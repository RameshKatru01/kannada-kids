package com.example.ui.data

import com.example.platform.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The child's own name, typed in Profile ("Learner name") and shown on Home.
 * Kept on the phone for each signed-in account.
 */
object LearnerName {
    const val MAX_LENGTH = 20

    private fun prefs() = getSharedPreferences("AppProgress")
    private fun user(): String = prefs().getString("last_active_user", "guest") ?: "guest"
    private fun key() = "${user()}_learner_name"

    private val _name = MutableStateFlow("")
    /** The current account's learner name ("" when none is set). */
    val name: StateFlow<String> = _name.asStateFlow()

    /** Reads the name for the account that is signed in now. */
    fun load(): String = (prefs().getString(key(), "") ?: "").also { _name.value = it }

    fun set(value: String) {
        val clean = value.replace("\n", " ").take(MAX_LENGTH)
        prefs().edit().putString(key(), clean.trim()).apply()
        _name.value = clean.trim()
    }
}
