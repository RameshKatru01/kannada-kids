package com.example.platform

/**
 * Hooks that the Swift side of the iOS app can fill in (see iosApp/iosApp/iOSApp.swift).
 */
object IosPlatformBridge {
    /** Set from Swift when the GoogleSignIn package has been added to the Xcode project. */
    var googleSignIn: IosGoogleSignInHandler? = null

    /** Set from Swift (StoreKit) for subscriptions. */
    var billing: IosBillingHandler? = null
}

/** Implemented in Swift with the GoogleSignIn SDK. */
interface IosGoogleSignInHandler {
    fun signIn(callback: GoogleSignInCallback)
    fun signOut()
}

/** Swift calls this exactly once with either the account details or an error message. */
interface GoogleSignInCallback {
    fun onResult(email: String?, displayName: String?, idToken: String?, errorMessage: String?)
}

/**
 * Implemented in Swift with StoreKit 2 (see iosApp/iosApp/StoreKitBridge.swift).
 * Results come back as small JSON strings so no complex types cross the Swift/Kotlin boundary.
 */
interface IosBillingHandler {
    /** json: [{"id":"premium_monthly","price":"₹99"}, …] */
    fun loadProducts(productIds: List<String>, callback: IosJsonCallback)

    /** json: one purchase object, or null when cancelled. */
    fun purchase(productId: String, callback: IosJsonCallback)

    /** json: [{"id":…,"orderId":…,"purchased":ms,"expires":ms,"autoRenew":true}, …] */
    fun activePurchases(callback: IosJsonCallback)
}

interface IosJsonCallback {
    fun onResult(json: String?, errorMessage: String?)
}
