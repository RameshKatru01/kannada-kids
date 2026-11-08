package com.example.ui.data

import com.example.platform.*

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {
    private const val PREFS_NAME = "AppThemePrefs"
    private const val THEME_KEY = "selected_theme"

    private val _currentTheme = MutableStateFlow(AppTheme.DEFAULT)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    /** The theme picker was taken out of Profile, so everyone uses the default look again. */
    fun init(context: Context) {
        getSharedPreferences(PREFS_NAME).edit().putString(THEME_KEY, AppTheme.DEFAULT.name).apply()
        _currentTheme.value = AppTheme.DEFAULT
    }

    fun setTheme(context: Context, theme: AppTheme) {
        val prefs = getSharedPreferences(PREFS_NAME)
        prefs.edit().putString(THEME_KEY, theme.name).apply()
        _currentTheme.value = theme
    }
}

enum class AppTheme(val displayName: String) {
    DEFAULT("Default"),
    OCEAN("Ocean Blue"),
    FOREST("Forest Green"),
    SUNSET("Sunset Orange")
}
