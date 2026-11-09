package com.example.ui.data

import com.example.platform.*

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The languages the app can teach. [available] = false hides a language that isn't ready yet
 * (shown as "Coming soon"); set it to true to switch it on.
 */
enum class AppLanguage(val code: String, val displayName: String, val available: Boolean = true) {
    KANNADA("kn", "Kannada"),
    HINDI("hi", "Hindi", available = false)
}

object LanguageManager {
    private val _currentLanguage = MutableStateFlow(AppLanguage.KANNADA)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun init(context: Context) {
        val prefs = getSharedPreferences("AppProgress")
        val savedLangCode = prefs.getString("selected_language", "kn") ?: "kn"
        // A language that has been switched off falls back to Kannada (progress in it is kept)
        val lang = AppLanguage.values().find { it.code == savedLangCode && it.available } ?: AppLanguage.KANNADA
        _currentLanguage.value = lang
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        if (!language.available) return
        _currentLanguage.value = language
        val prefs = getSharedPreferences("AppProgress")
        prefs.edit().putString("selected_language", language.code).apply()
    }
}
