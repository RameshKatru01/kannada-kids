package com.example.data

import com.example.platform.currentTimeMillis
import com.example.platform.formatDate

/**
 * One word a day for the home-screen widget: goes through every Fun Videos theme word (fruits,
 * vegetables, body parts, family), a new one each day (by the phone's local date).
 */
object WordOfDay {
    private val words: List<ThemeWord> by lazy { ThemeWords.all.flatMap { it.words } }

    fun today(millis: Long = currentTimeMillis()): ThemeWord {
        val day = formatDate(millis, "yyyy").toInt() * 366 + formatDate(millis, "D").toInt()
        return words[day.mod(words.size)]
    }
}
