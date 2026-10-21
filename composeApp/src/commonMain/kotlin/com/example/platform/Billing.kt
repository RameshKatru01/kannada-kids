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

/** A plan as the store sells it (price in the shopper's currency). */
data class StoreProduct(val period: PlanPeriod, val price: String)

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
