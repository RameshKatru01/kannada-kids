package com.example.platform

import android.app.Activity
import android.content.ContextWrapper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.MessageDigest

actual object PlatformBilling {
    private var client: BillingClient? = null
    private val connectLock = Mutex()
    private var pendingPurchase: CompletableDeferred<Pair<BillingResult, List<Purchase>?>>? = null
    private var details: Map<String, ProductDetails> = emptyMap()

    private val listener = PurchasesUpdatedListener { result, purchases ->
        pendingPurchase?.complete(result to purchases)
    }

    actual fun isSupported(): Boolean = true

    actual fun storeName(): String = "Google Play"

    private suspend fun ready(): BillingClient = connectLock.withLock {
        val existing = client
        if (existing != null && existing.isReady) return existing
        val c = existing ?: BillingClient.newBuilder(AppContextHolder.context)
            .setListener(listener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        client = c
        val ok = suspendCancellableCoroutine<Boolean> { cont ->
            c.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (cont.isActive) cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) cont.resume(false)
                }
            })
        }
        if (!ok) throw BillingException("Google Play isn't available right now. Please check that you're signed in to the Play Store.")
        c
    }

    private suspend fun loadDetails(): Map<String, ProductDetails> {
        if (details.isNotEmpty()) return details
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(PlanPeriod.entries.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it.productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            })
            .build()
        val c = ready()
        // Billing 8: the callback gives a QueryProductDetailsResult; products Play can't find are
        // listed separately (unfetchedProductList) instead of being left out.
        val found = suspendCancellableCoroutine<List<ProductDetails>> { cont ->
            c.queryProductDetailsAsync(params) { result, query ->
                val list = if (result.responseCode == BillingClient.BillingResponseCode.OK) query.productDetailsList else emptyList()
                if (cont.isActive) cont.resume(list)
            }
        }
        details = found.associateBy { it.productId }
        return details
    }

    /** The base plan offer (no free-trial / promo offer) of a subscription. */
    private fun ProductDetails.baseOffer(): ProductDetails.SubscriptionOfferDetails? =
        subscriptionOfferDetails?.firstOrNull { it.offerId == null } ?: subscriptionOfferDetails?.firstOrNull()

    actual suspend fun loadProducts(): List<StoreProduct> = try {
        val d = loadDetails()
        PlanPeriod.entries.mapNotNull { p ->
            val phase = d[p.productId]?.baseOffer()?.pricingPhases?.pricingPhaseList?.lastOrNull()
            phase?.let { StoreProduct(p, it.formattedPrice, it.priceAmountMicros, it.priceCurrencyCode) }
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun Purchase.toStorePurchase(): StorePurchase? {
        val period = PlanPeriod.byProductId(products.firstOrNull()) ?: return null
        return StorePurchase(
            period = period,
            orderId = orderId ?: purchaseToken.take(24),
            purchaseTimeMillis = purchaseTime,
            expiresAtMillis = null,
            autoRenewing = isAutoRenewing,
            platform = "android"
        )
    }

    private suspend fun acknowledge(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            ready().acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            )
        }
    }

    private fun Context.findActivity(): Activity? {
        var c: android.content.Context? = this
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }

    actual suspend fun purchase(context: Context, period: PlanPeriod, accountId: String): StorePurchase? {
        val activity = context.findActivity() ?: throw BillingException("Can't open Google Play from here.")
        val product = loadDetails()[period.productId]
            ?: throw BillingException("The ${period.title} plan isn't available in Google Play yet.")
        val offer = product.baseOffer() ?: throw BillingException("The ${period.title} plan has no price set in Google Play.")
        val hashedAccount = MessageDigest.getInstance("SHA-256").digest(accountId.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(64)

        val c = ready()
        val deferred = CompletableDeferred<Pair<BillingResult, List<Purchase>?>>()
        pendingPurchase = deferred
        val launch = c.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(product)
                            .setOfferToken(offer.offerToken)
                            .build()
                    )
                )
                .setObfuscatedAccountId(hashedAccount)
                .build()
        )
        if (launch.responseCode != BillingClient.BillingResponseCode.OK) {
            pendingPurchase = null
            throw BillingException(launch.debugMessage.ifBlank { "Couldn't start the purchase (${launch.responseCode})." })
        }
        val (result, purchases) = deferred.await()
        pendingPurchase = null
        return when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val p = purchases?.firstOrNull { it.products.contains(period.productId) } ?: purchases?.firstOrNull()
                if (p == null) null else {
                    acknowledge(p)
                    if (p.purchaseState == Purchase.PurchaseState.PENDING) {
                        throw BillingException("Payment is pending. Premium starts as soon as Google Play confirms it.")
                    }
                    p.toStorePurchase()
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> null
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> activePurchases().firstOrNull()
            else -> throw BillingException(result.debugMessage.ifBlank { "Purchase failed (${result.responseCode})." })
        }
    }

    actual suspend fun activePurchases(): List<StorePurchase> {
        val c = ready()
        val result = c.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        )
        val purchased = result.purchasesList.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        purchased.forEach { runCatching { acknowledge(it) } }
        return purchased.mapNotNull { it.toStorePurchase() }
    }

    actual fun manageUrl(): String =
        "https://play.google.com/store/account/subscriptions?package=${AppContextHolder.context.packageName}"
}
