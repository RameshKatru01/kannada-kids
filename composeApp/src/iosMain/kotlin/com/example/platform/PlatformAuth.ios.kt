package com.example.platform

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults
import kotlin.coroutines.resume

/**
 * iOS sign-in.
 *  - E-mail: Firebase Authentication REST API, when FIREBASE_API_KEY is set in Info.plist.
 *  - Google: GoogleSignIn SDK through [IosPlatformBridge] (see iOSApp.swift), then Firebase when configured.
 * The signed-in user is remembered in NSUserDefaults.
 */
actual object PlatformAuth {
    private const val USER_KEY = "kk_auth_user"
    private val httpClient by lazy { createHttpClient() }

    private val apiKey: String?
        get() {
            val key = NSBundle.mainBundle.objectForInfoDictionaryKey("FIREBASE_API_KEY") as? String
            return key?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("$(") && it != "YOUR_FIREBASE_WEB_API_KEY" }
        }

    actual fun isAvailable(): Boolean = apiKey != null

    actual fun currentUser(): AuthUser? {
        val raw = NSUserDefaults.standardUserDefaults.stringForKey(USER_KEY) ?: return null
        return try {
            val obj = Json.parseToJsonElement(raw).jsonObject
            AuthUser(
                uid = obj["uid"]?.jsonPrimitive?.contentOrNull ?: return null,
                email = obj["email"]?.jsonPrimitive?.contentOrNull,
                displayName = obj["displayName"]?.jsonPrimitive?.contentOrNull
            )
        } catch (e: Exception) {
            null
        }
    }

    private const val TOKEN_KEY = "kk_auth_id_token"
    private const val REFRESH_KEY = "kk_auth_refresh_token"
    private const val EXPIRES_KEY = "kk_auth_token_expires"

    // ---- Token storage ----
    // The Firebase tokens live in the Keychain (IosPlatformBridge.secureStore), not in NSUserDefaults,
    // which is a plain file that is also copied into device backups. Tokens saved by older versions
    // in NSUserDefaults are moved over the first time they are read.

    private fun readSecret(key: String): String? {
        val d = NSUserDefaults.standardUserDefaults
        val store = IosPlatformBridge.secureStore ?: return d.stringForKey(key)
        store.read(key)?.let { return it }
        val old = d.stringForKey(key) ?: return null
        store.write(key, old)
        d.removeObjectForKey(key)
        return old
    }

    private fun writeSecret(key: String, value: String) {
        val store = IosPlatformBridge.secureStore
        if (store == null) {
            NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
        } else {
            store.write(key, value)
            NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
        }
    }

    private fun removeSecret(key: String) {
        IosPlatformBridge.secureStore?.remove(key)
        NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
    }

    /** Keeps the Firebase tokens from a sign-in / refresh response. */
    private fun saveTokens(idToken: String?, refreshToken: String?, expiresInSeconds: Long?) {
        if (idToken != null) writeSecret(TOKEN_KEY, idToken)
        if (refreshToken != null) writeSecret(REFRESH_KEY, refreshToken)
        val expires = currentTimeMillis() + ((expiresInSeconds ?: 3600L) - 120L) * 1000L
        writeSecret(EXPIRES_KEY, expires.toString())
    }

    private fun JsonObject.saveTokensFromSignIn() = saveTokens(
        this["idToken"]?.jsonPrimitive?.contentOrNull,
        this["refreshToken"]?.jsonPrimitive?.contentOrNull,
        this["expiresIn"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
    )

    actual suspend fun idToken(): String? {
        // Keychain items outlive an uninstall, so a reinstalled app (signed out) must not reuse them
        if (currentUser() == null) return null
        val token = readSecret(TOKEN_KEY)
        val expires = readSecret(EXPIRES_KEY)?.toLongOrNull() ?: 0L
        if (token != null && currentTimeMillis() < expires) return token
        val refresh = readSecret(REFRESH_KEY) ?: return null
        val key = apiKey ?: return null
        return try {
            val response = httpClient.post("https://securetoken.googleapis.com/v1/token?key=$key") {
                contentType(ContentType.Application.FormUrlEncoded)
                NSBundle.mainBundle.bundleIdentifier?.let { headers.append("X-Ios-Bundle-Identifier", it) }
                setBody("grant_type=refresh_token&refresh_token=$refresh")
            }
            if (!response.status.isSuccess()) return null
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val newToken = json["id_token"]?.jsonPrimitive?.contentOrNull
            saveTokens(newToken, json["refresh_token"]?.jsonPrimitive?.contentOrNull, json["expires_in"]?.jsonPrimitive?.contentOrNull?.toLongOrNull())
            newToken
        } catch (e: Exception) {
            null
        }
    }

    private const val PROVIDER_KEY = "kk_auth_provider"

    private fun remember(user: AuthUser, provider: String = "google.com"): AuthUser {
        NSUserDefaults.standardUserDefaults.setObject(provider, forKey = PROVIDER_KEY)
        val json = buildJsonObject {
            put("uid", user.uid)
            put("email", user.email)
            put("displayName", user.displayName)
        }
        NSUserDefaults.standardUserDefaults.setObject(json.toString(), forKey = USER_KEY)
        return user
    }

    private suspend fun firebaseCall(endpoint: String, body: JsonObject): JsonObject {
        val key = apiKey ?: throw AuthException("Firebase unavailable")
        val response = httpClient.post("https://identitytoolkit.googleapis.com/v1/accounts:$endpoint?key=$key") {
            contentType(ContentType.Application.Json)
            NSBundle.mainBundle.bundleIdentifier?.let { headers.append("X-Ios-Bundle-Identifier", it) }
            setBody(body.toString())
        }
        val text = response.bodyAsText()
        val json = try {
            Json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            throw AuthException("Unexpected response from sign-in server")
        }
        if (!response.status.isSuccess()) {
            val message = (json["error"] as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull
            throw AuthException(message ?: "Sign in failed (${response.status.value})")
        }
        return json
    }

    private fun JsonObject.toAuthUser(fallbackEmail: String?): AuthUser = AuthUser(
        uid = this["localId"]?.jsonPrimitive?.contentOrNull ?: randomId(),
        email = this["email"]?.jsonPrimitive?.contentOrNull ?: fallbackEmail,
        displayName = this["displayName"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    )

    actual suspend fun signInWithEmail(email: String, password: String): AuthUser {
        val json = firebaseCall("signInWithPassword", buildJsonObject {
            put("email", email)
            put("password", password)
            put("returnSecureToken", true)
        })
        json.saveTokensFromSignIn()
        return remember(json.toAuthUser(email), provider = "password")
    }

    actual suspend fun createUserWithEmail(email: String, password: String): AuthUser {
        val json = firebaseCall("signUp", buildJsonObject {
            put("email", email)
            put("password", password)
            put("returnSecureToken", true)
        })
        json.saveTokensFromSignIn()
        return remember(json.toAuthUser(email), provider = "password")
    }

    private data class GoogleResult(
        val email: String?,
        val displayName: String?,
        val idToken: String?,
        val error: String?
    )

    actual suspend fun signInWithGoogle(context: Context): AuthUser {
        val handler = IosPlatformBridge.googleSignIn
            ?: throw AuthException(
                "Google sign-in is not set up in the iPhone app yet. You can explore as a guest instead!"
            )

        val result = suspendCancellableCoroutine<GoogleResult> { cont ->
            handler.signIn(object : GoogleSignInCallback {
                override fun onResult(email: String?, displayName: String?, idToken: String?, errorMessage: String?) {
                    if (cont.isActive) cont.resume(GoogleResult(email, displayName, idToken, errorMessage))
                }
            })
        }

        if (result.error != null || result.email.isNullOrBlank()) {
            throw AuthException(result.error ?: "Google sign in failed")
        }
        val email = result.email

        val token = result.idToken
        if (token != null && apiKey != null) {
            try {
                val json = firebaseCall("signInWithIdp", buildJsonObject {
                    put("postBody", "id_token=$token&providerId=google.com")
                    put("requestUri", "http://localhost")
                    put("returnIdpCredential", true)
                    put("returnSecureToken", true)
                })
                json.saveTokensFromSignIn()
                val user = json.toAuthUser(email)
                return remember(user.copy(displayName = user.displayName ?: result.displayName))
            } catch (e: Exception) {
                println("PlatformAuth: Firebase Google sign-in failed, using local profile - ${e.message}")
            }
        }

        // Local authenticated profile (same fallback the Android app uses without Firebase)
        return remember(
            AuthUser(
                uid = "google_${email.hashCode()}",
                email = email,
                displayName = result.displayName ?: email.substringBefore("@")
            )
        )
    }

    // ---- Sign in with Apple ----

    actual fun appleSignInSupported(): Boolean = IosPlatformBridge.appleSignIn != null

    private data class AppleResult(
        val idToken: String?,
        val rawNonce: String?,
        val authorizationCode: String?,
        val email: String?,
        val fullName: String?,
        val error: String?
    )

    private suspend fun appleCredential(): AppleResult {
        val handler = IosPlatformBridge.appleSignIn
            ?: throw AuthException("Sign in with Apple is not available on this device.")
        val result = suspendCancellableCoroutine<AppleResult> { cont ->
            handler.signIn(object : AppleSignInCallback {
                override fun onResult(idToken: String?, rawNonce: String?, authorizationCode: String?, email: String?, fullName: String?, errorMessage: String?) {
                    if (cont.isActive) cont.resume(AppleResult(idToken, rawNonce, authorizationCode, email, fullName, errorMessage))
                }
            })
        }
        if (result.error != null || result.idToken.isNullOrBlank() || result.rawNonce.isNullOrBlank()) {
            throw AuthException(result.error ?: "Sign in with Apple failed")
        }
        return result
    }

    /** Signs in to Firebase with an Apple credential. */
    private suspend fun firebaseApple(apple: AppleResult): JsonObject = firebaseCall("signInWithIdp", buildJsonObject {
        put("postBody", "id_token=${apple.idToken}&providerId=apple.com&nonce=${apple.rawNonce}")
        put("requestUri", "http://localhost")
        put("returnIdpCredential", true)
        put("returnSecureToken", true)
    })

    actual suspend fun signInWithApple(context: Context): AuthUser {
        if (apiKey == null) throw AuthException("Sign in is not set up in this build yet.")
        val apple = appleCredential()
        val json = firebaseApple(apple)
        json.saveTokensFromSignIn()
        val user = json.toAuthUser(apple.email)
        val name = user.displayName ?: apple.fullName?.takeIf { it.isNotBlank() }
        return remember(user.copy(displayName = name ?: "Little learner"), provider = "apple.com")
    }

    // ---- Delete account ----

    actual suspend fun deleteAccount(context: Context) {
        val current = currentUser() ?: throw AuthException("Not signed in")
        val provider = NSUserDefaults.standardUserDefaults.stringForKey(PROVIDER_KEY) ?: "google.com"
        if (provider == "apple.com") {
            // Apple: sign in again (fresh login + an authorization code), revoke the Apple tokens, delete.
            val apple = appleCredential()
            val json = firebaseApple(apple)
            val uid = json["localId"]?.jsonPrimitive?.contentOrNull
            if (uid != current.uid) throw AuthException("That was a different Apple ID. Please use the same one to delete it.")
            json.saveTokensFromSignIn()
            val idToken = json["idToken"]?.jsonPrimitive?.contentOrNull ?: throw AuthException("Sign in with Apple failed")
            apple.authorizationCode?.let { code ->
                try {
                    revokeAppleToken(code, idToken)
                } catch (e: Exception) {
                    println("PlatformAuth: Apple token revoke failed - ${e.message}")
                }
            }
            firebaseCall("delete", buildJsonObject { put("idToken", idToken) })
        } else {
            val token = idToken() ?: throw AuthException("Please sign in again and then delete the account.", needsRecentLogin = true)
            try {
                firebaseCall("delete", buildJsonObject { put("idToken", token) })
            } catch (e: AuthException) {
                if (e.message?.contains("CREDENTIAL_TOO_OLD") != true && e.message?.contains("TOKEN_EXPIRED") != true) throw e
                // Too long since sign-in: sign in with Google again, then delete
                val again = signInWithGoogle(context)
                if (again.uid != current.uid) throw AuthException("That was a different Google account. Please choose the same account to delete it.")
                val fresh = idToken() ?: throw AuthException("Please sign in again and then delete the account.", needsRecentLogin = true)
                firebaseCall("delete", buildJsonObject { put("idToken", fresh) })
            }
        }
        signOut()
    }

    /** Firebase revokes the Apple refresh token (needs the Apple provider set up in Firebase with a key). */
    private suspend fun revokeAppleToken(authorizationCode: String, idToken: String) {
        val key = apiKey ?: return
        val response = httpClient.post("https://identitytoolkit.googleapis.com/v2/accounts:revokeToken?key=$key") {
            contentType(ContentType.Application.Json)
            NSBundle.mainBundle.bundleIdentifier?.let { headers.append("X-Ios-Bundle-Identifier", it) }
            setBody(buildJsonObject {
                put("providerId", "apple.com")
                put("tokenType", "CODE")
                put("token", authorizationCode)
                put("idToken", idToken)
            }.toString())
        }
        if (!response.status.isSuccess()) println("PlatformAuth: revokeToken ${response.status.value} ${response.bodyAsText()}")
    }

    actual fun signOut() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(PROVIDER_KEY)
        NSUserDefaults.standardUserDefaults.removeObjectForKey(USER_KEY)
        removeSecret(TOKEN_KEY)
        removeSecret(REFRESH_KEY)
        removeSecret(EXPIRES_KEY)
        try {
            IosPlatformBridge.googleSignIn?.signOut()
        } catch (_: Throwable) {
        }
    }
}
