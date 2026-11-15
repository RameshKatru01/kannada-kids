package com.example.platform

/**
 * The three auto-renewing plans. [productId] must match the subscription IDs created in the
 * Google Play Console and App Store Connect.
 */
enum class PlanPeriod(
    val productId: String,
    val title: String,
    val months: Int,
    val listPrice: String,
    val rupees: Int,
    val perText: String
) {
    Monthly("premium_monthly", "Monthly", 1, "₹99", 99, "every month"),
    HalfYearly("premium_halfyearly", "Half-yearly", 6, "₹499", 499, "every 6 months"),
    Yearly("premium_yearly", "Yearly", 12, "₹899", 899, "every year");

    /** Saving compared with paying monthly, in %. */
    val savingPercent: Int get() = if (months == 1) 0 else kotlin.math.round((1.0 - rupees.toDouble() / (99.0 * months)) * 100).toInt()

    companion object {
        fun byProductId(id: String?): PlanPeriod? = entries.firstOrNull { it.productId == id }
        fun byKey(key: String?): PlanPeriod? = entries.firstOrNull { it.name.equals(key, ignoreCase = true) || it.productId == key }
    }
}

/**
 * A plan as the store sells it, in the currency of the parent's store country
 * (their Google Play / Apple ID country, not where the phone is right now).
 * [micros] is the price × 1,000,000 and [currency] the ISO code ("INR", "USD"); 0 / "" when unknown.
 */
data class StoreProduct(val period: PlanPeriod, val price: String, val micros: Long = 0, val currency: String = "")

/** "About $3.33 a month" for a longer plan, in the store's currency; null when the store didn't say. */
fun List<StoreProduct>.perMonthPrice(period: PlanPeriod): String? {
    val p = firstOrNull { it.period == period } ?: return null
    if (p.micros <= 0 || p.currency.isBlank()) return null
    return formatMoney(p.micros / period.months, p.currency)
}

/** Saving compared with paying monthly, using the store's prices when both are known in the same currency. */
fun List<StoreProduct>.savingPercent(period: PlanPeriod): Int {
    if (period.months == 1) return 0
    val p = firstOrNull { it.period == period }
    val m = firstOrNull { it.period == PlanPeriod.Monthly }
    if (p == null || m == null || p.micros <= 0 || m.micros <= 0 || p.currency != m.currency) return period.savingPercent
    return kotlin.math.round((1.0 - p.micros.toDouble() / (m.micros.toDouble() * period.months)) * 100).toInt().coerceAtLeast(0)
}

/** An active subscription the store knows about on this device's account. */
data class StorePurchase(
    val period: PlanPeriod,
    val orderId: String,
    val purchaseTimeMillis: Long,
    /** When it renews or ends; null when the store doesn't say (Android works it out from the period). */
    val expiresAtMillis: Long?,
    val autoRenewing: Boolean,
    val platform: String
)

class BillingException(message: String) : Exception(message)

/**
 * Google Play Billing on Android, StoreKit on iOS. The stores take the payment and renew it.
 */
expect object PlatformBilling {
    /** True when the store can be used on this device / build. */
    fun isSupported(): Boolean

    /** Store name to show to parents ("Google Play" / "App Store"). */
    fun storeName(): String

    suspend fun loadProducts(): List<StoreProduct>

    /** Shows the store's purchase sheet. Returns the purchase, or null if the parent cancelled. */
    suspend fun purchase(context: Context, period: PlanPeriod, accountId: String): StorePurchase?

    /** Active subscriptions for the store account on this device. */
    suspend fun activePurchases(): List<StorePurchase>

    /** Where parents manage or cancel their subscription. */
    fun manageUrl(): String
}
