package com.example.ui.data

import com.example.data.LetterData
import com.example.platform.*
import com.example.ui.progress.GameInfo
import com.example.ui.progress.GameProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One place that decides what a child can open.
 *
 * Two kinds of lock, never shown together:
 *  - Progress (🔒): not reached yet. Letters open one by one; Consonants opens after all vowels,
 *    Ka-kaa after all consonants; a game level needs a star on the level before.
 *  - Plan (👑): not in the free plan. Free = the 1st letter of each island, Feed the Monster
 *    level 1, one run a day of Balloon Pop and Letter Dash, the Vowels videos and the Letter Box.
 *
 * Premium (Gold) = an active store subscription, free time given in Admin, a Gold plan set by
 * an admin, or being an admin. Every family gets a 15-day free Gold trial with no card (see SubscriptionManager.FREE_TRIAL_DAYS).
 *
 * Admins never see either lock: every island, letter, game, level and video is open for them.
 *
 * Parents can change the order in Parent corner → Learning path.
 */
object Access {

    enum class State { Open, NeedsProgress, NeedsPremium }

    // ---- Who is this, and are they Gold? ----

    private fun prefs() = getSharedPreferences("AppProgress")
    private fun user(): String = prefs().getString("last_active_user", "guest") ?: "guest"

    /** Signed in as an admin (checked against both the saved user and the signed-in account). */
    fun isAdmin(): Boolean =
        UserManager.isAdmin(user()) || UserManager.isAdmin(PlatformAuth.currentUser()?.email)

    /**
     * Test accounts that always have Gold (for trying the full app without buying).
     * Remove an address here when it is no longer needed.
     */
    private val testGoldEmails = setOf("neeraja.katru@gmail.com")

    fun isTestGold(): Boolean {
        val signedIn = PlatformAuth.currentUser()?.email?.trim()?.lowercase()
        return signedIn != null && signedIn in testGoldEmails
    }

    fun isPremium(): Boolean {
        if (SubscriptionManager.state.value.hasPremium) return true
        if (isAdmin()) return true
        if (isTestGold()) return true
        val email = user().trim().lowercase()
        val p = prefs()
        return listOf("kn", "en", "hi").any { (p.getString("${it}_subscriptionPlan_$email", "") ?: "").equals("Gold", true) }
    }

    // ---- Parent settings (Parent corner → Learning path) ----

    private val _version = MutableStateFlow(0)
    /** Bumps when a parent setting changes, so screens redraw. */
    val version: StateFlow<Int> = _version.asStateFlow()

    /** "Everything open": no progress locks at all (plan locks still apply). */
    var everythingOpen: Boolean
        get() = prefs().getBoolean("${user()}_path_open", false)
        set(v) { prefs().edit().putBoolean("${user()}_path_open", v).apply(); _version.value += 1 }

    /** 0 = from the beginning, 1 = from Consonants, 2 = from Ka-kaa, 3 = from Ottakshara (earlier islands open). */
    var startFrom: Int
        get() = prefs().getInt("${user()}_${com.example.data.Lang.keyPrefix}path_start", 0)
        set(v) { prefs().edit().putInt("${user()}_${com.example.data.Lang.keyPrefix}path_start", v.coerceIn(0, 3)).apply(); _version.value += 1 }

    /** Game levels need a star on the level before. */
    var levelsNeedStar: Boolean
        get() = prefs().getBoolean("${user()}_path_level_star", true)
        set(v) { prefs().edit().putBoolean("${user()}_path_level_star", v).apply(); _version.value += 1 }

    // ---- Islands and letters ----

    val islandOrder = listOf("vowels", "consonants", "guninthakshari", "numbers", "ottakshara")

    fun total(type: String): Int = when (type) {
        "vowels" -> LetterData.vowels.size
        "consonants", "guninthakshari" -> LetterData.consonants.size
        "numbers" -> LetterData.numbers.size
        "ottakshara" -> com.example.data.OttaksharaData.letters.size
        else -> 0
    }

    private fun allLearned(type: String): Boolean {
        val n = total(type)
        val done = ActivityTracker.learned(type)
        return n > 0 && (0 until n).all { it in done }
    }

    /** Whole island skipped by "Start from" (all its letters count as open). */
    private fun skipped(type: String): Boolean = when (type) {
        "vowels" -> startFrom >= 1
        "consonants" -> startFrom >= 2
        "guninthakshari" -> startFrom >= 3
        else -> false
    }

    /** Progress only (ignores the plan). */
    fun islandReached(type: String): Boolean = everythingOpen || when (type) {
        "vowels", "numbers" -> true
        "consonants" -> startFrom >= 1 || allLearned("vowels")
        "guninthakshari" -> startFrom >= 2 || allLearned("consonants")
        "ottakshara" -> startFrom >= 3 || allLearned("guninthakshari")
        else -> true
    }

    /** What the island needs before it opens, for the "locked" bubble. */
    fun islandHint(type: String): String = when (type) {
        "consonants" -> {
            val left = total("vowels") - ActivityTracker.learned("vowels").count { it < total("vowels") }
            "Learn $left more ${if (left == 1) "vowel" else "vowels"} to open Consonants!"
        }
        "guninthakshari" -> {
            val left = total("consonants") - ActivityTracker.learned("consonants").count { it < total("consonants") }
            "Learn $left more ${if (left == 1) "consonant" else "consonants"} to open ${com.example.data.Lang.islandName("guninthakshari")}!"
        }
        "ottakshara" -> {
            val left = total("guninthakshari") - ActivityTracker.learned("guninthakshari").count { it < total("guninthakshari") }
            "Finish $left more ${com.example.data.Lang.islandName("guninthakshari")} ${if (left == 1) "row" else "rows"} to open ${com.example.data.Lang.islandName("ottakshara")}!"
        }
        else -> ""
    }

    /**
     * Free users can always try the 1st letter of every island, so for them islands never show a
     * progress lock (everything after the 1st letter shows the crown instead).
     */
    fun island(type: String): State =
        if (isAdmin() || !isPremium() || islandReached(type)) State.Open else State.NeedsProgress

    /** First letter of [type] not learned yet (where "Next" goes). */
    fun nextIndex(type: String): Int {
        val done = ActivityTracker.learned(type)
        return (0 until total(type)).firstOrNull { it !in done } ?: 0
    }

    /** Progress only: letters open one by one (every letter before it is learned). */
    fun letterReached(type: String, index: Int): Boolean {
        if (everythingOpen || skipped(type)) return true
        if (!islandReached(type)) return false
        val done = ActivityTracker.learned(type)
        return index in done || index <= nextIndex(type)
    }

    fun letter(type: String, index: Int): State {
        if (isAdmin()) return State.Open
        if (!isPremium()) return if (index == 0) State.Open else State.NeedsPremium
        return if (letterReached(type, index)) State.Open else State.NeedsProgress
    }

    /** In Ka-kaa, only ಕ + the first sign is free. */
    fun kaKaaSign(consonantIndex: Int, signIndex: Int): State {
        val base = letter("guninthakshari", consonantIndex)
        if (base != State.Open) return base
        return if (isPremium() || signIndex == 0) State.Open else State.NeedsPremium
    }

    // ---- Games ----

    private val endless = setOf(12, 23)          // Musical Balloon Pop, Letter Dash
    private val monster = 1..4                   // Feed the Monster (all four)

    /** Can this game be opened at all (its level map, or the game itself for games without levels)? */
    fun game(game: GameInfo): State = when {
        isPremium() -> State.Open
        game.id in monster || game.id in endless -> State.Open
        // Tap & Learn opens for everyone; inside it the fruit market is free and the other scenes need Gold
        // Memory Flip: Easy is free (Medium / Hard ask for Gold inside). Write My Name is free for everyone.
        game.id == 24 || game.id == 25 || game.id == 26 || game.id == 27 -> State.Open
        else -> State.NeedsPremium
    }

    fun level(game: GameInfo, level: Int): State {
        if (isAdmin()) return State.Open
        if (level > 1 && levelsNeedStar && !everythingOpen && GameProgress.stars(game, level - 1) == 0) return State.NeedsProgress
        if (isPremium()) return State.Open
        return if (game.id in monster && level == 1) State.Open else State.NeedsPremium
    }

    private fun runKey(gameId: Int) = "${user()}_free_run_${gameId}_${formatDate(currentTimeMillis(), "yyyyMMdd")}"

    /** Free users get one run a day of the endless games. */
    fun endlessRunLeft(gameId: Int): Boolean =
        gameId !in endless || isPremium() || !prefs().getBoolean(runKey(gameId), false)

    fun useEndlessRun(gameId: Int) {
        if (gameId in endless && !isPremium()) prefs().edit().putBoolean(runKey(gameId), true).apply()
    }

    fun isEndless(gameId: Int) = gameId in endless

    // ---- Videos ----

    private val freeFolders = setOf("vowels")

    fun videoFolder(folder: String): State =
        if (isPremium() || folder.trim().lowercase() in freeFolders) State.Open else State.NeedsPremium

    // ---- Printable worksheets ----

    /**
     * A worksheet file covering the letters at [itemIndexes] of one section. Free: the cover /
     * starter kit (no letters) and the sheets for the first letter of each section, like the
     * first letter of each island.
     */
    fun worksheet(itemIndexes: List<Int>): State =
        if (isPremium() || itemIndexes.all { it == 0 }) State.Open else State.NeedsPremium

    // ---- Asking for things (shown by AppNavigation) ----

    data class Prompt(val title: String, val message: String, val premium: Boolean)

    private val _prompt = MutableStateFlow<Prompt?>(null)
    val prompt: StateFlow<Prompt?> = _prompt.asStateFlow()

    /** Crown tapped: "This is in the Gold pack. Ask a grown-up!" */
    fun askGrownUp(message: String = "Lots more letters, games and videos are waiting!") {
        _prompt.value = Prompt("This is in the Gold pack", message, premium = true)
    }

    /** Padlock tapped: say what to do next. */
    fun showHint(message: String) {
        _prompt.value = Prompt("Not yet!", message, premium = false)
    }

    fun dismissPrompt() { _prompt.value = null }

    // ---- "New island opened!" ----

    /** An island the child has just opened by learning (shown once). */
    fun newlyOpenedIsland(): String? {
        if (isAdmin() || !isPremium() || everythingOpen) return null
        val before = mapOf("consonants" to "vowels", "guninthakshari" to "consonants", "ottakshara" to "guninthakshari")
        return before.keys.firstOrNull { type ->
            !skipped(before.getValue(type)) &&
                islandReached(type) && !prefs().getBoolean("${user()}_celebrated_${com.example.data.Lang.keyPrefix}$type", false)
        }
    }

    fun markCelebrated(type: String) {
        prefs().edit().putBoolean("${user()}_celebrated_${com.example.data.Lang.keyPrefix}$type", true).apply()
        _version.value += 1
    }
}
