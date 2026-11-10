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
    val hasPremium: Boolean get() {
        val now = currentTimeMillis()
        return ((status == "active" || status == "autorenew_off") && renewsAt > now) || bonusUntil > now
    }
}

/**
 * Keeps the subscription in sync between the store (Google Play / App Store), the online database
 * (subscriptions/{uid}, written only by the verifyPurchases Cloud Function and read by the admin
 * screens) and the app's "premium" switch.
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

    /** A paid plan that hasn't run out yet. */
    private fun SubscriptionState.paidNow(): Boolean =
        (status == "active" || status == "autorenew_off") && renewsAt > currentTimeMillis()

    private fun Map<String, Any?>.toState() = SubscriptionState(
        plan = PlanPeriod.byKey(str("plan")),
        status = str("status") ?: "none",
        autoRenew = bool("autoRenew"),
        renewsAt = long("renewsAt"),
        bonusUntil = long("bonusUntil"),
        platform = str("platform") ?: ""
    )

    /**
     * Works out whether this family has Gold. Safe to call often.
     *
     * The store's purchase proofs go to the verifyPurchases Cloud Function, which checks them with
     * Google Play / the App Store and is the only thing allowed to write subscriptions/{uid}
     * (see firestore.rules). The app never writes its own subscription status.
     */
    suspend fun refresh() = lock.withLock {
        val user = PlatformAuth.currentUser() ?: return@withLock
        val purchases = try {
            if (PlatformBilling.isSupported()) PlatformBilling.activePurchases() else emptyList()
        } catch (e: Exception) {
            null // store not reachable
        }

        // 1. Ask the server to check the purchases and update the family's record.
        val verified = try {
            CloudStore.callFunction(
                "verifyPurchases",
                mapOf("purchases" to purchases.orEmpty().filter { it.proof.isNotEmpty() }.map {
                    mapOf("platform" to it.platform, "proof" to it.proof, "autoRenew" to it.autoRenewing)
                })
            )
        } catch (e: Exception) {
            Log.w("SubscriptionManager", "Couldn't check the subscription online", e)
            null
        }
        // 2. Server not reachable: read the record it wrote last time.
        val remote = verified ?: try {
            CloudStore.get("subscriptions/${user.uid}")
        } catch (e: Exception) {
            null
        }

        if (remote == null && purchases == null) {
            // Fully offline: keep what we had.
            applyEntitlement(user.email, _state.value.hasPremium)
            return@withLock
        }

        val fromServer = remote?.toState() ?: SubscriptionState()
        // 3. A subscription the store itself reports on this phone counts straight away
        //    (e.g. just bought, before the server has caught up).
        val best = purchases?.maxByOrNull { nextRenewal(it) }
        val newState = if (best != null && !fromServer.paidNow()) {
            SubscriptionState(
                plan = best.period,
                status = if (best.autoRenewing) "active" else "autorenew_off",
                autoRenew = best.autoRenewing,
                renewsAt = nextRenewal(best),
                bonusUntil = fromServer.bonusUntil,
                platform = best.platform
            )
        } else {
            fromServer
        }
        _state.value = newState
        applyEntitlement(user.email, newState.hasPremium)
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
