package com.example.platform

/** A signed-in account returned by the platform sign-in code. */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?
)

class AuthException(
    message: String,
    val noAccountOnDevice: Boolean = false,
    /** The account can only be deleted right after signing in again. */
    val needsRecentLogin: Boolean = false
) : Exception(message)

/**
 * Platform sign-in layer.
 *
 * Android: Firebase Authentication + Google Credential Manager (same as the original app).
 * iOS: Firebase Authentication REST API for e-mail sign-in (when a Firebase API key is
 *      configured) and the GoogleSignIn SDK for "Continue with Google" (when it is added in Xcode).
 *
 * All functions throw [AuthException] (or another exception) on failure.
 */
expect object PlatformAuth {
    /** True when a Firebase backend is configured and usable on this device. */
    fun isAvailable(): Boolean

    fun currentUser(): AuthUser?

    suspend fun signInWithGoogle(context: Context): AuthUser

    /** "Sign in with Apple" is offered on this device (iPhone / iPad). */
    fun appleSignInSupported(): Boolean

    suspend fun signInWithApple(context: Context): AuthUser

    /**
     * Permanently deletes the signed-in account from Firebase Authentication. If the sign-in is
     * too old, signs in again with the same provider (Google / Apple) first. For Apple accounts it
     * also revokes the app's Apple tokens, as Apple requires.
     */
    suspend fun deleteAccount(context: Context)

    suspend fun signInWithEmail(email: String, password: String): AuthUser

    suspend fun createUserWithEmail(email: String, password: String): AuthUser

    fun signOut()

    /** A fresh Firebase ID token for the signed-in user (used for the online database), or null. */
    suspend fun idToken(): String?
}
