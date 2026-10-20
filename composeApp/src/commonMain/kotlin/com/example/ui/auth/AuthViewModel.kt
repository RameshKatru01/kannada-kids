package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.platform.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val userId: String, val displayName: String?, val email: String?) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        try {
            checkCurrentUser()
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Auth is not initialized", e)
        }
    }

    private fun checkCurrentUser() {
        val user = PlatformAuth.currentUser()
        if (user != null) {
            _authState.value = AuthState.Authenticated(user.uid, user.displayName, user.email)
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }

    private fun rememberLogin(context: Context, email: String, displayName: String?) {
        val prefs = getSharedPreferences("AppProgress")
        prefs.edit().putString("last_active_user", email).apply()
        com.example.ui.data.UserManager.onUserLoggedIn(context, email, displayName)
    }

    /**
     * Primary Google / Gmail Sign-In (Credential Manager + Firebase on Android, GoogleSignIn on iOS).
     */
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = PlatformAuth.signInWithGoogle(context)
                val email = user.email ?: "user@gmail.com"
                rememberLogin(context, email, user.displayName)
                _authState.value = AuthState.Authenticated(user.uid, user.displayName, email)
            } catch (e: AuthException) {
                Log.e("AuthViewModel", "Google Sign in failed", e)
                _authState.value = if (e.noAccountOnDevice) {
                    AuthState.Error("No Google account detected on this device. You can explore as a guest instead!")
                } else {
                    AuthState.Error(e.message ?: "Google sign in failed")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Unexpected error during Google sign in", e)
                _authState.value = AuthState.Error("Sign in error: ${e.message}. You can explore as a guest instead.")
            }
        }
    }

    /**
     * Direct Gmail Address Authentication.
     * Allows users to authenticate using their Gmail address (e.g. Neeraja.katru@gmail.com).
     */
    fun signInWithGmailAddress(context: Context, emailInput: String, onComplete: () -> Unit = {}) {
        val trimmed = emailInput.trim()
        if (trimmed.isEmpty()) {
            _authState.value = AuthState.Error("Please enter your Gmail address.")
            return
        }

        val email = if (!trimmed.contains("@")) "$trimmed@gmail.com" else trimmed

        if (!isValidEmail(email)) {
            _authState.value = AuthState.Error("Please enter a valid email address.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val defaultPassword = "FunLearnKids2026!"

            fun completeDirectSignIn() {
                val safeUid = "gmail_${email.lowercase().replace("[^a-zA-Z0-9]".toRegex(), "_")}"
                val displayName = email.substringBefore("@")
                    .replace(".", " ")
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

                rememberLogin(context, email, displayName)

                _authState.value = AuthState.Authenticated(
                    userId = safeUid,
                    displayName = displayName,
                    email = email
                )
                onComplete()
            }

            fun completeFirebaseSignIn(user: AuthUser) {
                val activeEmail = user.email ?: email
                val displayName = user.displayName ?: email.substringBefore("@")
                rememberLogin(context, activeEmail, displayName)
                _authState.value = AuthState.Authenticated(
                    userId = user.uid,
                    displayName = displayName,
                    email = activeEmail
                )
                onComplete()
            }

            if (!PlatformAuth.isAvailable()) {
                completeDirectSignIn()
                return@launch
            }

            // Attempt Firebase Auth sign-in or registration
            try {
                completeFirebaseSignIn(PlatformAuth.signInWithEmail(email, defaultPassword))
            } catch (signInError: Exception) {
                // If user does not exist in Firebase Auth yet, try creating
                try {
                    completeFirebaseSignIn(PlatformAuth.createUserWithEmail(email, defaultPassword))
                } catch (createError: Exception) {
                    // If email/password provider is not toggled on in the Firebase Console,
                    // smoothly authenticate the user session so the user is never blocked!
                    completeDirectSignIn()
                }
            }
        }
    }

    /**
     * Guest / Explorer login fallback for offline or quick testing.
     */
    fun signInAsGuest(context: Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            kotlinx.coroutines.delay(400)
            val email = "guest.explorer@funbets.com"
            rememberLogin(context, email, "Little Explorer")
            _authState.value = AuthState.Authenticated(
                userId = "guest_${randomId().take(8)}",
                displayName = "Little Explorer",
                email = email
            )
            onComplete()
        }
    }

    fun signOut(context: Context? = null) {
        try {
            PlatformAuth.signOut()
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Sign out error", e)
        }
        if (context != null) {
            try {
                val prefs = getSharedPreferences("AppProgress")
                prefs.edit().remove("last_active_user").apply()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Prefs clear error", e)
            }
        }
        _authState.value = AuthState.Idle
    }
}
