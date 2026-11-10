package com.example.ui.data

import com.example.platform.*

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ScoreManager {
    private const val PREFS_NAME = "GameScores"
    private const val TOTAL_COINS_KEY = "total_coins"

    private val _totalCoins = MutableStateFlow(0)
    val totalCoins: StateFlow<Int> = _totalCoins.asStateFlow()

    fun init(context: Context) {
        val prefs = getSharedPreferences(PREFS_NAME)
        _totalCoins.value = prefs.getInt(TOTAL_COINS_KEY, 0)
    }

    /** Re-reads the points total (after ProgressSync brought in points from another device). */
    fun reload() {
        _totalCoins.value = getSharedPreferences(PREFS_NAME).getInt(TOTAL_COINS_KEY, 0)
    }

    fun addCoins(context: Context, gameId: String, amount: Int = 1) {
        val prefs = getSharedPreferences(PREFS_NAME)
        val currentTotal = prefs.getInt(TOTAL_COINS_KEY, 0)
        val currentParam = prefs.getInt("score_$gameId", 0)

        val newTotal = currentTotal + amount
        val newParam = currentParam + amount

        prefs.edit()
            .putInt(TOTAL_COINS_KEY, newTotal)
            .putInt("score_$gameId", newParam)
            .apply()

        _totalCoins.value = newTotal
    }

    fun getGameScore(context: Context, gameId: String): Int {
        val prefs = getSharedPreferences(PREFS_NAME)
        return prefs.getInt("score_$gameId", 0)
    }

    fun getAllScores(context: Context): Map<String, Int> {
        val prefs = getSharedPreferences(PREFS_NAME)
        val scores = mutableMapOf<String, Int>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("score_") && value is Int) {
                scores[key.removePrefix("score_")] = value
            }
        }
        return scores
    }
}
