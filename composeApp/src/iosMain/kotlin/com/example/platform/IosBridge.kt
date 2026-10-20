package com.example.platform

/**
 * Hooks that the Swift side of the iOS app can fill in (see iosApp/iosApp/iOSApp.swift).
 */
object IosPlatformBridge {
    /** Set from Swift when the GoogleSignIn package has been added to the Xcode project. */
    var googleSignIn: IosGoogleSignInHandler? = null
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
