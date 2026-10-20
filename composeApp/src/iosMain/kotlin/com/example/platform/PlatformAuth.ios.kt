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

    private fun remember(user: AuthUser): AuthUser {
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
        return remember(json.toAuthUser(email))
    }

    actual suspend fun createUserWithEmail(email: String, password: String): AuthUser {
        val json = firebaseCall("signUp", buildJsonObject {
            put("email", email)
            put("password", password)
            put("returnSecureToken", true)
        })
        return remember(json.toAuthUser(email))
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

    actual fun signOut() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(USER_KEY)
        try {
            IosPlatformBridge.googleSignIn?.signOut()
        } catch (_: Throwable) {
        }
    }
}
