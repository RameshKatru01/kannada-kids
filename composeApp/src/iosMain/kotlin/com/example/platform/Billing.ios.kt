package com.example.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual object PlatformBilling {
    private val handler: IosBillingHandler? get() = IosPlatformBridge.billing

    actual fun isSupported(): Boolean = handler != null

    actual fun storeName(): String = "App Store"

    private suspend fun call(block: (IosBillingHandler, IosJsonCallback) -> Unit): String? {
        val h = handler ?: throw BillingException("The App Store isn't connected in this build.")
        return suspendCancellableCoroutine { cont ->
            block(h, object : IosJsonCallback {
                override fun onResult(json: String?, errorMessage: String?) {
                    if (!cont.isActive) return
                    if (errorMessage != null) cont.resumeWithException(BillingException(errorMessage))
                    else cont.resume(json)
                }
            })
        }
    }

    private fun JsonObject.toPurchase(): StorePurchase? {
        val period = PlanPeriod.byProductId(this["id"]?.jsonPrimitive?.contentOrNull) ?: return null
        return StorePurchase(
            period = period,
            orderId = this["orderId"]?.jsonPrimitive?.contentOrNull ?: "",
            purchaseTimeMillis = this["purchased"]?.jsonPrimitive?.longOrNull ?: 0L,
            expiresAtMillis = this["expires"]?.jsonPrimitive?.longOrNull,
            autoRenewing = this["autoRenew"]?.jsonPrimitive?.booleanOrNull ?: true,
            platform = "ios"
        )
    }

    actual suspend fun loadProducts(): List<StoreProduct> = try {
        val json = call { h, cb -> h.loadProducts(PlanPeriod.entries.map { it.productId }, cb) } ?: "[]"
        Json.parseToJsonElement(json).jsonArray.mapNotNull { e ->
            val o = e.jsonObject
            val p = PlanPeriod.byProductId(o["id"]?.jsonPrimitive?.contentOrNull) ?: return@mapNotNull null
            StoreProduct(p, o["price"]?.jsonPrimitive?.contentOrNull ?: p.listPrice)
        }
    } catch (e: Exception) {
        emptyList()
    }

    actual suspend fun purchase(context: Context, period: PlanPeriod, accountId: String): StorePurchase? {
        val json = call { h, cb -> h.purchase(period.productId, cb) } ?: return null
        return Json.parseToJsonElement(json).jsonObject.toPurchase()
    }

    actual suspend fun activePurchases(): List<StorePurchase> {
        val json = call { h, cb -> h.activePurchases(cb) } ?: "[]"
        return Json.parseToJsonElement(json).jsonArray.mapNotNull { it.jsonObject.toPurchase() }
    }

    actual fun manageUrl(): String = "https://apps.apple.com/account/subscriptions"
}
