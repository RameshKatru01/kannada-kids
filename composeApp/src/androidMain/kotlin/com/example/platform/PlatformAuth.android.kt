package com.example.platform

import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual object PlatformAuth {
    private const val FALLBACK_WEB_CLIENT_ID =
        "9146775799-bbh5cob9boe2vj81j5l81of6tu941apc.apps.googleusercontent.com"

    private val auth: FirebaseAuth? by lazy {
        try {
            Firebase.auth
        } catch (e: Exception) {
            android.util.Log.e("PlatformAuth", "Firebase is not initialized", e)
            null
        }
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(uid = uid, email = email, displayName = displayName)

    actual fun isAvailable(): Boolean = auth != null

    actual fun currentUser(): AuthUser? = try {
        auth?.currentUser?.toAuthUser()
    } catch (e: Exception) {
        null
    }

    actual suspend fun signInWithGoogle(context: Context): AuthUser {
        // Resolve Web Client ID from BuildConfig, R.string.default_web_client_id, or fallback
        val webClientId = try {
            val configured = BuildConfig.WEB_CLIENT_ID
            if (configured.isNotBlank() && configured != "dummy_client_id") {
                configured
            } else {
                context.getString(R.string.default_web_client_id)
            }
        } catch (e: Exception) {
            FALLBACK_WEB_CLIENT_ID
        }

        if (webClientId.isBlank()) {
            throw AuthException("Web Client ID is not configured. You can explore as a guest instead.")
        }

        val credentialManager = CredentialManager.create(context)

        // Generate a secure nonce
        val rawNonce = UUID.randomUUID().toString()
        val digest = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray())
        val hashedNonce = digest.joinToString("") { "%02x".format(it) }

        val signInOption = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        val result = try {
            credentialManager.getCredential(request = request, context = context)
        } catch (e: GetCredentialException) {
            android.util.Log.e("PlatformAuth", "Google Sign in failed", e)
            val noAccount = e is NoCredentialException ||
                e.message?.contains("no credentials", ignoreCase = true) == true ||
                e.message?.contains("no accounts", ignoreCase = true) == true
            throw AuthException(e.message ?: "Google sign in failed", noAccountOnDevice = noAccount)
        }

        val credential = result.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw AuthException("Unexpected credential type received")
        }
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

        val firebaseAuth = auth
        if (firebaseAuth == null) {
            // Fallback to local authenticated profile if Firebase instance is unavailable
            val email = googleIdTokenCredential.id
            return AuthUser(
                uid = "google_${email.hashCode()}",
                email = email,
                displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")
            )
        }

        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
        return suspendCancellableCoroutine { cont ->
            firebaseAuth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        if (user != null) {
                            cont.resume(user.toAuthUser().copy(email = user.email ?: "user@gmail.com"))
                        } else {
                            cont.resumeWithException(AuthException("User session is empty after login"))
                        }
                    } else {
                        android.util.Log.e("PlatformAuth", "Firebase sign in failed", task.exception)
                        cont.resumeWithException(AuthException(task.exception?.message ?: "Firebase sign in failed"))
                    }
                }
        }
    }

    actual suspend fun signInWithEmail(email: String, password: String): AuthUser {
        val firebaseAuth = auth ?: throw AuthException("Firebase unavailable")
        return suspendCancellableCoroutine { cont ->
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    cont.resume(
                        AuthUser(
                            uid = user?.uid ?: UUID.randomUUID().toString(),
                            email = user?.email ?: email,
                            displayName = user?.displayName
                        )
                    )
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }

    actual suspend fun createUserWithEmail(email: String, password: String): AuthUser {
        val firebaseAuth = auth ?: throw AuthException("Firebase unavailable")
        return suspendCancellableCoroutine { cont ->
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    cont.resume(
                        AuthUser(
                            uid = user?.uid ?: UUID.randomUUID().toString(),
                            email = user?.email ?: email,
                            displayName = user?.displayName
                        )
                    )
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }

    actual fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            android.util.Log.e("PlatformAuth", "Firebase sign out error", e)
        }
    }
}
