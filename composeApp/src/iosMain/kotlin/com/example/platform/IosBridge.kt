package com.example.platform

/**
 * Hooks that the Swift side of the iOS app can fill in (see iosApp/iosApp/iOSApp.swift).
 */
object IosPlatformBridge {
    /** Set from Swift when the GoogleSignIn package has been added to the Xcode project. */
    var googleSignIn: IosGoogleSignInHandler? = null

    /** Set from Swift (AuthenticationServices) for "Sign in with Apple". */
    var appleSignIn: IosAppleSignInHandler? = null

    /** Set from Swift (StoreKit) for subscriptions. */
    var billing: IosBillingHandler? = null

    /** Set from Swift (Keychain) for the sign-in tokens. */
    var secureStore: IosSecureStore? = null
}

/**
 * Small secret store, implemented in Swift with the Keychain (see iosApp/iosApp/KeychainStore.swift).
 * Kept on this device only: not in backups and not moved to a new phone.
 */
interface IosSecureStore {
    fun read(key: String): String?
    fun write(key: String, value: String)
    fun remove(key: String)
}

/** Implemented in Swift with AuthenticationServices (see iosApp/iosApp/AppleSignInBridge.swift). */
interface IosAppleSignInHandler {
    fun signIn(callback: AppleSignInCallback)
}

/**
 * Swift calls this once. [idToken] + [rawNonce] go to Firebase; [authorizationCode] is used to
 * revoke Apple tokens when the account is deleted. [fullName] / [email] only come the first time.
 */
interface AppleSignInCallback {
    fun onResult(idToken: String?, rawNonce: String?, authorizationCode: String?, email: String?, fullName: String?, errorMessage: String?)
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
