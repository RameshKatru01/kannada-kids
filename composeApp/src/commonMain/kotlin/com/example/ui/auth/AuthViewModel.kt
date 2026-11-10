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

    /** Brings in this account's progress from the cloud (waits a few seconds at most). */
    private suspend fun restoreProgress() {
        try {
            kotlinx.coroutines.withTimeoutOrNull(8_000L) { com.example.ui.data.ProgressSync.sync() }
        } catch (e: Exception) {
            Log.w("AuthViewModel", "Progress sync at sign-in failed", e)
        }
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
                restoreProgress()
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

    /** "Sign in with Apple" (iPhone / iPad). */
    fun signInWithApple(context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = PlatformAuth.signInWithApple(context)
                // Apple may hide the e-mail; the Firebase user id keeps progress separate per account
                val email = user.email ?: "apple_${user.uid}@privaterelay.appleid.com"
                rememberLogin(context, email, user.displayName)
                restoreProgress()
                _authState.value = AuthState.Authenticated(user.uid, user.displayName, email)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Apple sign in failed", e)
                _authState.value = AuthState.Error(e.message ?: "Sign in with Apple failed")
            }
        }
    }

    sealed class DeleteResult {
        object Done : DeleteResult()
        data class Failed(val message: String) : DeleteResult()
    }

    /**
     * Permanently deletes the account: the online subscription record and progress, the sign-in account and
     * everything this app saved on this device for it. Store subscriptions are not cancelled by
     * this (the parent does that in Google Play / the App Store).
     */
    suspend fun deleteAccount(context: Context): DeleteResult {
        val user = PlatformAuth.currentUser() ?: return DeleteResult.Failed("You are not signed in with an account.")
        val email = (authState.value as? AuthState.Authenticated)?.email ?: user.email
        // 1. Online data (must happen while still signed in)
        try {
            com.example.ui.data.CloudStore.delete("subscriptions/${user.uid}")
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Could not delete subscription record", e)
        }
        try {
            com.example.ui.data.CloudStore.delete("progress/${user.uid}")
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Could not delete online progress", e)
        }
        com.example.ui.data.ProgressSync.forget(user.uid)
        // 2. The sign-in account
        try {
            PlatformAuth.deleteAccount(context)
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Account delete failed", e)
            return DeleteResult.Failed(e.message ?: "Could not delete the account. Please try again.")
        }
        // 3. Everything saved on this device for this account
        try {
            clearLocalData(context, email, user.uid)
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Local data clear failed", e)
        }
        signOut(context)
        return DeleteResult.Done
    }

    private fun clearLocalData(context: Context, email: String?, uid: String) {
        val keys = listOfNotNull(email?.trim()?.lowercase()?.takeIf { it.isNotBlank() }, email?.trim()?.takeIf { it.isNotBlank() }, uid)
        // Stores whose keys include the account's e-mail (progress, stars, letters learned, settings…)
        listOf("AppProgress", "GameProgress", "ReferenceProgress", "ActivityStats", "GamePrefs").forEach { name ->
            val prefs = getSharedPreferences(name)
            val editor = prefs.edit()
            prefs.all.keys.filter { k -> keys.any { k.contains(it) } }.forEach { editor.remove(it) }
            editor.apply()
        }
        // Points are kept per device, not per account: reset them too
        getSharedPreferences("GameScores").edit().clear().apply()
        com.example.ui.data.ScoreManager.init(context)
        if (email != null) com.example.ui.data.UserManager.deleteUser(context, email)
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
