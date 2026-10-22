package com.example.ui.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.platform.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Records what the Parent corner shows: time spent in the app each day, and right / wrong
 * answers per letter. Everything is stored on the device, per player.
 */
object ActivityTracker {
    private const val PREFS = "ActivityStats"
    private const val TICK_SECONDS = 30

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    private fun prefs() = getSharedPreferences(PREFS)
    private fun user() = getSharedPreferences("AppProgress").getString("last_active_user", "guest") ?: "guest"
    private fun dayKey(millis: Long) = formatDate(millis, "yyyyMMdd")
    private fun secondsKey(user: String, day: String) = "${user}_secs_$day"

    // ---- Time ----

    fun addSeconds(seconds: Int) {
        val u = user()
        val k = secondsKey(u, dayKey(currentTimeMillis()))
        val p = prefs()
        p.edit().putInt(k, p.getInt(k, 0) + seconds).apply()
        _version.value += 1
    }

    data class DayMinutes(val label: String, val minutes: Int, val isToday: Boolean)

    /** Minutes in the app for each of the last 7 days, oldest first. */
    fun lastSevenDays(): List<DayMinutes> {
        val u = user()
        val now = currentTimeMillis()
        val p = prefs()
        return (6 downTo 0).map { back ->
            val t = addToDate(now, days = -back)
            val secs = p.getInt(secondsKey(u, dayKey(t)), 0)
            DayMinutes(formatDate(t, "EEE"), (secs + 30) / 60, back == 0)
        }
    }

    /** Days in a row (ending today, or yesterday if today hasn't started) with at least a minute of learning. */
    fun streakDays(): Int {
        val u = user()
        val now = currentTimeMillis()
        val p = prefs()
        fun active(back: Int) = p.getInt(secondsKey(u, dayKey(addToDate(now, days = -back))), 0) >= 60
        var back = if (active(0)) 0 else 1
        var streak = 0
        while (back < 366 && active(back)) {
            streak += 1
            back += 1
        }
        return streak
    }

    // ---- Letters learned (from the letter lessons) ----

    /** Remember that lesson [index] of [type] (vowels / consonants / guninthakshari / numbers) was finished. */
    fun markLearned(type: String, index: Int) {
        val u = user()
        val key = "${u}_learned_$type"
        val known = prefs().getStringSet(key, emptySet()) ?: emptySet()
        if (index.toString() in known) return
        prefs().edit().putStringSet(key, known + index.toString()).apply()
        _version.value += 1
    }

    /** Lesson indexes of [type] that have been finished. */
    fun learned(type: String): Set<Int> =
        (prefs().getStringSet("${user()}_learned_$type", emptySet()) ?: emptySet())
            .mapNotNull { it.toIntOrNull() }.toSet()

    // ---- Answers ----

    fun recordAnswer(letter: String, right: Boolean) {
        if (letter.isBlank()) return
        val u = user()
        val p = prefs()
        val k = if (right) "${u}_ok_$letter" else "${u}_no_$letter"
        val total = if (right) "${u}_ok_total" else "${u}_no_total"
        val known = p.getStringSet("${u}_letters", emptySet()) ?: emptySet()
        p.edit()
            .putInt(k, p.getInt(k, 0) + 1)
            .putInt(total, p.getInt(total, 0) + 1)
            .putStringSet("${u}_letters", known + letter)
            .apply()
        _version.value += 1
    }

    /** Percentage of right answers, or null before any answers. */
    fun accuracyPercent(): Int? {
        val u = user()
        val ok = prefs().getInt("${u}_ok_total", 0)
        val no = prefs().getInt("${u}_no_total", 0)
        return if (ok + no == 0) null else (ok * 100) / (ok + no)
    }

    private data class Score(val letter: String, val ok: Int, val no: Int)

    private fun scores(): List<Score> {
        val u = user()
        val p = prefs()
        val letters = p.getStringSet("${u}_letters", emptySet()) ?: emptySet()
        return letters.map { Score(it, p.getInt("${u}_ok_$it", 0), p.getInt("${u}_no_$it", 0)) }
    }

    /** Letters with the most mistakes (at least 2), worst first. */
    fun needsPractice(limit: Int = 6): List<String> =
        scores().filter { it.no >= 2 && it.no * 3 >= it.ok }
            .sortedWith(compareByDescending<Score> { it.no.toFloat() / (it.ok + it.no) }.thenByDescending { it.no })
            .take(limit).map { it.letter }

    /** Letters answered right at least 3 times with few mistakes. */
    fun strongLetters(limit: Int = 8): List<String> =
        scores().filter { it.ok >= 3 && it.no * 5 <= it.ok }
            .sortedByDescending { it.ok }
            .take(limit).map { it.letter }
}

/** Counts time while the app is on screen (put once at the top of the app). */
@Composable
fun TrackActiveTime() {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(30_000L)
                ActivityTracker.addSeconds(30)
            }
        }
    }
}
