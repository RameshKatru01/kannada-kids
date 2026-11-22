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
    val platform: String = "",
    /** End of the free Gold trial every family gets (no card, no purchase); 0 = unknown yet. */
    val trialUntil: Long = 0L
) {
    val paid: Boolean get() = status == "active" || status == "autorenew_off"
    val hasPremium: Boolean get() = paid || bonusUntil > currentTimeMillis() || trialUntil > currentTimeMillis()
    /** On the free trial (not paying, no admin gift). */
    val inTrial: Boolean get() = !paid && bonusUntil <= currentTimeMillis() && trialUntil > currentTimeMillis()
    /** Whole days left on the trial (1 on the last day). */
    val trialDaysLeft: Int get() = if (!inTrial) 0 else (((trialUntil - currentTimeMillis()) + 86_399_999L) / 86_400_000L).toInt()
    /** The trial has run out and the family hasn't chosen a plan. */
    val trialEnded: Boolean get() = !hasPremium && trialUntil in 1..currentTimeMillis()
}

/** Every family gets this many days of Gold free, with no card or purchase. */
const val FREE_TRIAL_DAYS = 15

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
        // Free trial: starts the first time this account uses the app (kept online, so reinstalling
        // doesn't restart it; the earliest start we know of wins).
        val trialPrefs = getSharedPreferences("SubscriptionTrial")
        val trialKey = "trial_start_${user.uid}"
        val trialStart = listOf(trialPrefs.getLong(trialKey, 0L), remote?.long("trialStart") ?: 0L)
            .filter { it > 0L }.minOrNull() ?: currentTimeMillis()
        if (trialPrefs.getLong(trialKey, 0L) != trialStart) trialPrefs.edit().putLong(trialKey, trialStart).apply()
        val trialUntil = trialStart + FREE_TRIAL_DAYS * 86_400_000L

        // Only bonusUntil can be trusted from the online record: the rules let just admins write it.
        // status / plan / renewsAt are written by the family's own app, so anyone signed in could
        // set them by hand; they are kept for the admin screens but never switch Premium on.
        val bonusUntil = remote?.long("bonusUntil") ?: _state.value.bonusUntil

        if (purchases == null) {
            // Store not reachable: keep what the store said last time (plus any admin free time).
            // Premium is never switched off here, so a paying family isn't locked out offline.
            _state.value = _state.value.copy(bonusUntil = bonusUntil, trialUntil = trialUntil)
            if (_state.value.hasPremium) applyEntitlement(user.email, true)
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
                platform = best.platform,
                trialUntil = trialUntil
            )
        } else {
            // No purchase in this device's store. A subscription bought on the other platform is
            // not trusted from the online record (see above) until a server checks receipts.
            val wasPaid = remote?.str("status").let { it == "active" || it == "autorenew_off" }
            SubscriptionState(
                plan = PlanPeriod.byKey(remote?.str("plan")),
                status = if (wasPaid) "expired" else "none",
                autoRenew = false,
                renewsAt = remote?.long("renewsAt") ?: 0L,
                bonusUntil = bonusUntil,
                platform = remote?.str("platform") ?: "",
                trialUntil = trialUntil
            )
        }
        _state.value = newState
        applyEntitlement(user.email, newState.hasPremium)

        // A record owned by the other platform's store is left for that device to keep up to date,
        // so the admin screens don't flip between "active" and "expired".
        val remotePlatform = remote?.str("platform").orEmpty()
        if (best == null && remotePlatform.isNotEmpty() && remotePlatform != platformName()) return@withLock

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
                "payments" to payments.takeLast(60),
                "trialStart" to trialStart
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

    suspend fun isCloudAdmin(): Boolean = AdminRoles.load() != null

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
