package com.example.ui.data

import com.example.platform.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What the app knows about the signed-in family's subscription. */
data class SubscriptionState(
    val plan: PlanPeriod? = null,
    /** "active", "autorenew_off", "expired", "none" */
    val status: String = "none",
    val autoRenew: Boolean = false,
    /** Next renewal (or end date when auto-renew is off). */
    val renewsAt: Long = 0L,
    /** Free time given by the admin, valid until this time. */
    val bonusUntil: Long = 0L,
    val platform: String = ""
) {
    val hasPremium: Boolean get() = status == "active" || status == "autorenew_off" || bonusUntil > currentTimeMillis()
}

/**
 * Keeps the subscription in sync between the store (Google Play / App Store), the online database
 * (subscriptions/{uid}, read by the admin screens) and the app's "premium" switch.
 */
object SubscriptionManager {
    private val _state = MutableStateFlow(SubscriptionState())
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    private val _products = MutableStateFlow<List<StoreProduct>>(emptyList())
    val products: StateFlow<List<StoreProduct>> = _products.asStateFlow()

    private val lock = Mutex()

    fun priceOf(period: PlanPeriod): String =
        _products.value.firstOrNull { it.period == period }?.price ?: period.listPrice

    suspend fun loadProducts() {
        if (!PlatformBilling.isSupported()) return
        val list = PlatformBilling.loadProducts()
        if (list.isNotEmpty()) _products.value = list
    }

    /** Next renewal for an Android purchase: the first period boundary after now. */
    private fun nextRenewal(p: StorePurchase): Long {
        p.expiresAtMillis?.let { return it }
        var t = p.purchaseTimeMillis
        val now = currentTimeMillis()
        var guard = 0
        while (t <= now && guard < 600) {
            t = addToDate(t, months = p.period.months)
            guard++
        }
        return t
    }

    /** Reads the store + online record and updates everything. Safe to call often. */
    suspend fun refresh() = lock.withLock {
        val user = PlatformAuth.currentUser() ?: return@withLock
        val purchases = try {
            if (PlatformBilling.isSupported()) PlatformBilling.activePurchases() else emptyList()
        } catch (e: Exception) {
            null // store not reachable: keep what we have
        }
        val remote = try {
            CloudStore.get("subscriptions/${user.uid}")
        } catch (e: Exception) {
            null
        }
        val bonusUntil = remote?.long("bonusUntil") ?: 0L

        if (purchases == null) {
            // Offline: trust the last known online record
            if (remote != null) {
                _state.value = SubscriptionState(
                    plan = PlanPeriod.byKey(remote.str("plan")),
                    status = remote.str("status") ?: "none",
                    autoRenew = remote.bool("autoRenew"),
                    renewsAt = remote.long("renewsAt"),
                    bonusUntil = bonusUntil,
                    platform = remote.str("platform") ?: ""
                )
            }
            applyEntitlement(user.email, _state.value.hasPremium)
            return@withLock
        }

        val best = purchases.maxByOrNull { nextRenewal(it) }
        val now = currentTimeMillis()
        val newState = if (best != null) {
            SubscriptionState(
                plan = best.period,
                status = if (best.autoRenewing) "active" else "autorenew_off",
                autoRenew = best.autoRenewing,
                renewsAt = nextRenewal(best),
                bonusUntil = bonusUntil,
                platform = best.platform
            )
        } else {
            val wasPaid = remote?.str("status").let { it == "active" || it == "autorenew_off" }
            // The other platform may hold the subscription (same account signed in on iPhone and Android)
            val otherPlatform = remote?.str("platform")?.let { it != platformName() } == true &&
                remote.long("renewsAt") > now
            if (otherPlatform && wasPaid) {
                SubscriptionState(
                    plan = PlanPeriod.byKey(remote?.str("plan")),
                    status = remote?.str("status") ?: "none",
                    autoRenew = remote?.bool("autoRenew") ?: false,
                    renewsAt = remote?.long("renewsAt") ?: 0L,
                    bonusUntil = bonusUntil,
                    platform = remote?.str("platform") ?: ""
                )
            } else {
                SubscriptionState(
                    plan = PlanPeriod.byKey(remote?.str("plan")),
                    status = if (wasPaid) "expired" else (remote?.str("status") ?: "none"),
                    autoRenew = false,
                    renewsAt = remote?.long("renewsAt") ?: 0L,
                    bonusUntil = bonusUntil,
                    platform = remote?.str("platform") ?: ""
                )
            }
        }
        _state.value = newState
        applyEntitlement(user.email, newState.hasPremium)

        // Save to the online record (admin screens read it)
        try {
            @Suppress("UNCHECKED_CAST")
            val payments = (remote?.get("payments") as? List<Map<String, Any?>>).orEmpty().toMutableList()
            if (best != null && payments.none { it.str("orderId") == best.orderId }) {
                payments += mapOf(
                    "orderId" to best.orderId,
                    "date" to now,
                    "plan" to best.period.name,
                    "amount" to priceOf(best.period)
                )
            }
            val fields = mutableMapOf<String, Any?>(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "name" to (user.displayName ?: user.email?.substringBefore("@") ?: ""),
                "plan" to (newState.plan?.name ?: ""),
                "status" to newState.status,
                "autoRenew" to newState.autoRenew,
                "renewsAt" to newState.renewsAt,
                "platform" to newState.platform,
                "updatedAt" to now,
                "payments" to payments.takeLast(60)
            )
            if (remote == null) fields["joinedAt"] = now
            if (best != null && (remote == null || remote.long("startedAt") == 0L)) {
                fields["startedAt"] = best.purchaseTimeMillis
            }
            val changed = remote == null || fields.any { (k, v) -> k != "updatedAt" && remote[k] != v }
            if (changed) CloudStore.update("subscriptions/${user.uid}", fields)
        } catch (e: Exception) {
            Log.w("SubscriptionManager", "Couldn't save subscription online", e)
        }
    }

    /** Buy a plan. Returns a message for the parent. */
    suspend fun buy(context: Context, period: PlanPeriod): String {
        val user = PlatformAuth.currentUser()
            ?: return "Please sign in with Google first, so the subscription stays with your account."
        return try {
            val purchase = PlatformBilling.purchase(context, period, user.uid)
            if (purchase == null) {
                "No problem, nothing was charged."
            } else {
                refresh()
                "Thank you! Premium is now on."
            }
        } catch (e: Exception) {
            e.message ?: "The purchase didn't go through."
        }
    }

    suspend fun restore(): String {
        refresh()
        return if (_state.value.hasPremium) "Your subscription is restored." else
            "No active subscription found for this ${PlatformBilling.storeName()} account."
    }

    private fun platformName() = if (PlatformBilling.storeName() == "App Store") "ios" else "android"

    /** Switches the app's premium content on or off for this player. */
    private fun applyEntitlement(email: String?, premium: Boolean) {
        val e = email?.trim()?.lowercase() ?: return
        val prefs = getSharedPreferences("AppProgress")
        val grantedKey = "store_premium_granted_$e"
        val grantedByStore = prefs.getBoolean(grantedKey, false)
        val editor = prefs.edit()
        if (premium) {
            listOf("kn", "hi", "en").forEach { editor.putString("${it}_subscriptionPlan_$e", "Gold") }
            editor.putBoolean(grantedKey, true)
        } else if (grantedByStore) {
            // Only switch off what the store switched on (keeps plans the admin gave by hand)
            listOf("kn", "hi", "en").forEach { editor.putString("${it}_subscriptionPlan_$e", "Free") }
            editor.putBoolean(grantedKey, false)
        }
        editor.apply()
    }

    // ---- Admin ----

    data class Subscriber(
        val uid: String,
        val email: String,
        val name: String,
        val plan: PlanPeriod?,
        val status: String,
        val autoRenew: Boolean,
        val renewsAt: Long,
        val bonusUntil: Long,
        val bonusNote: String,
        val platform: String,
        val joinedAt: Long,
        val payments: List<Map<String, Any?>>
    ) {
        /** Status as the admin sees it, worked out from the dates. */
        val effectiveStatus: String
            get() {
                val now = currentTimeMillis()
                return when {
                    (status == "active" || status == "autorenew_off") && renewsAt in 1 until now - 2 * 86_400_000L ->
                        if (autoRenew) "overdue" else "expired"
                    status == "active" -> "active"
                    status == "autorenew_off" -> "autorenew_off"
                    bonusUntil > now -> "free_time"
                    status == "expired" -> "expired"
                    else -> "none"
                }
            }
    }

    private fun Map<String, Any?>.toSubscriber(id: String): Subscriber {
        @Suppress("UNCHECKED_CAST")
        return Subscriber(
            uid = id,
            email = str("email") ?: "",
            name = str("name") ?: (str("email")?.substringBefore("@") ?: id.take(6)),
            plan = PlanPeriod.byKey(str("plan")),
            status = str("status") ?: "none",
            autoRenew = bool("autoRenew"),
            renewsAt = long("renewsAt"),
            bonusUntil = long("bonusUntil"),
            bonusNote = str("bonusNote") ?: "",
            platform = str("platform") ?: "",
            joinedAt = long("joinedAt"),
            payments = (this["payments"] as? List<Map<String, Any?>>).orEmpty()
        )
    }

    suspend fun isCloudAdmin(): Boolean {
        val email = PlatformAuth.currentUser()?.email?.trim()?.lowercase() ?: return false
        return try {
            CloudStore.get("admins/$email") != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun allSubscribers(): List<Subscriber> =
        CloudStore.list("subscriptions").map { (id, m) -> m.toSubscriber(id) }
            .sortedWith(compareBy<Subscriber> { if (it.plan == null) 1 else 0 }.thenBy { it.name.lowercase() })

    suspend fun subscriber(uid: String): Subscriber? = CloudStore.get("subscriptions/$uid")?.toSubscriber(uid)

    /** Admin: give (or with days = 0, remove) free premium time. */
    suspend fun giveFreeTime(uid: String, days: Int, note: String): Subscriber? {
        val current = subscriber(uid)
        val now = currentTimeMillis()
        val until = if (days <= 0) 0L else maxOf(now, current?.bonusUntil ?: 0L) + days * 86_400_000L
        CloudStore.update(
            "subscriptions/$uid",
            mapOf("bonusUntil" to until, "bonusNote" to note, "bonusBy" to (PlatformAuth.currentUser()?.email ?: ""))
        )
        return subscriber(uid)
    }
}
