package com.example.ui.data

import com.example.platform.*

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val code: String, val displayName: String) {
    KANNADA("kn", "Kannada"),
    HINDI("hi", "Hindi")
}

object LanguageManager {
    private val _currentLanguage = MutableStateFlow(AppLanguage.KANNADA)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun init(context: Context) {
        val prefs = getSharedPreferences("AppProgress")
        val savedLangCode = prefs.getString("selected_language", "kn") ?: "kn"
        val lang = AppLanguage.values().find { it.code == savedLangCode } ?: AppLanguage.KANNADA
        _currentLanguage.value = lang
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        _currentLanguage.value = language
        val prefs = getSharedPreferences("AppProgress")
        prefs.edit().putString("selected_language", language.code).apply()
    }
}
