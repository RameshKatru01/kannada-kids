package com.example.ui.progress

import androidx.compose.ui.graphics.Color
import com.example.platform.*
import com.example.ui.data.FeatureFlags
import com.example.ui.data.ScoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A game in the Games list. Games with [levels] > 0 use the shared levels / stars / points system. */
data class GameInfo(
    val id: Int,
    val key: String,
    val name: String,
    val subtitle: String,
    val glyph: String,
    val color: Color,
    val ink: Color,
    val levels: Int = 0
) {
    val hasLevels: Boolean get() = levels > 0
    val maxStars: Int get() = levels * 3
}

const val LEVELS_PER_GAME = 6

private val Teal = Color(0xFF2EC4B6)
private val Coral = Color(0xFFFF6B6B)
private val Violet = Color(0xFF6C63FF)
private val Sun = Color(0xFFFFC93C)
private val Tangerine = Color(0xFFFF9F1C)
private val Berry = Color(0xFFE84393)
private val Sky = Color(0xFF4DA8FF)
private val Leaf = Color(0xFF59C173)
private val Navy = Color(0xFF0B3B66)

object GameRegistry {
    val games: List<GameInfo> = listOf(
        GameInfo(1, "monster_vowels", "Feed the Monster", "Vowels", "ಅ", Teal, Navy, LEVELS_PER_GAME),
        GameInfo(2, "monster_consonants", "Feed the Monster", "Consonants", "ಕ", Violet, Color.White, LEVELS_PER_GAME),
        GameInfo(3, "monster_guninthakshari", "Feed the Monster", "Guninthakshari", "ಕಾ", Coral, Color.White, LEVELS_PER_GAME),
        GameInfo(4, "monster_numbers", "Feed the Monster", "Numbers", "೧", Tangerine, Navy, LEVELS_PER_GAME),
        GameInfo(21, "letter_train", "Letter Train", "Put the letters in order", "ರ", Sky, Color.White, LEVELS_PER_GAME),
        GameInfo(22, "paint_monster", "Paint the Monster", "Colours", "ಬ", Berry, Color.White, LEVELS_PER_GAME),
        GameInfo(8, "scrambled_words", "Scrambled Words", "Build the word", "ಪ", Tangerine, Navy, LEVELS_PER_GAME),
        GameInfo(9, "word_search", "Word Search", "Find hidden words", "ಮ", Violet, Color.White, LEVELS_PER_GAME),
        GameInfo(13, "colour_sort", "Colour Sort", "Pour the colours", "ಹ", Coral, Color.White, LEVELS_PER_GAME),
        GameInfo(19, "listen_choose", "Listen & Choose", "Hear it, find it", "ಆ", Teal, Navy, LEVELS_PER_GAME),
        GameInfo(11, "match_letter", "Match the Letter", "Letters and pictures", "ಇ", Sky, Color.White, LEVELS_PER_GAME),
        GameInfo(20, "swara_adventure", "Swara Adventure", "Find the missing ಸ್ವರ", "ಸ", Leaf, Navy, LEVELS_PER_GAME),
        // Older games: play freely (levels come in a later update)
        GameInfo(12, "balloon_pop", "Musical Balloon Pop", "Endless · pop to the music!", "ಉ", Berry, Color.White),
        GameInfo(23, "letter_dash", "Letter Dash", "Endless run · beat your best!", "ಡ", Tangerine, Navy),
        GameInfo(6, "letter_runner", "Letter Runner", "Just for fun", "ಎ", Leaf, Navy),
    )

    fun byId(id: Int): GameInfo? = games.firstOrNull { it.id == id }
}

/** One line of the points breakdown on the "Level complete" card. */
data class PointsLine(val label: String, val points: Int)

data class LevelResult(
    val gameId: Int,
    val level: Int,          // 1-based
    val stars: Int,          // 1..3
    val previousBest: Int,   // 0 if first clear
    val lines: List<PointsLine>,
    val pointsEarned: Int,
    val totalPoints: Int,
    val message: String
)

/**
 * Per-player level progress: best stars for each level of each game.
 *
 * Stars: no mistakes = 3, 1-2 mistakes = 2, more = 1 (finishing always earns at least 1 star).
 * Points (added to the same coin total shown on Home / Leaderboard / Dashboard):
 *   first time a level is finished: +10, +5 per star, +2 per streak (3 right answers in a row)
 *   playing it again: +5 for each star better than before, +2 practice bonus.
 */
object GameProgress {
    private const val PREFS = "GameProgress"

    private val _version = MutableStateFlow(0)
    /** Changes whenever progress is saved, so screens can refresh. */
    val version: StateFlow<Int> = _version.asStateFlow()

    fun currentUser(): String =
        getSharedPreferences("AppProgress").getString("last_active_user", "guest") ?: "guest"

    private fun key(user: String, gameKey: String, level: Int) = "${user}_${gameKey}_L${level}_stars"

    fun stars(game: GameInfo, level: Int, user: String = currentUser()): Int =
        getSharedPreferences(PREFS).getInt(key(user, game.key, level), 0)

    fun starsList(game: GameInfo, user: String = currentUser()): List<Int> =
        (1..game.levels).map { stars(game, it, user) }

    fun totalStars(game: GameInfo, user: String = currentUser()): Int = starsList(game, user).sum()

    fun levelsDone(game: GameInfo, user: String = currentUser()): Int = starsList(game, user).count { it > 0 }

    /** The level the child should play next (1-based): the first level without stars, or the last level. */
    fun nextLevel(game: GameInfo, user: String = currentUser()): Int {
        val list = starsList(game, user)
        val firstOpen = list.indexOfFirst { it == 0 }
        return if (firstOpen < 0) game.levels else firstOpen + 1
    }

    fun isUnlocked(game: GameInfo, level: Int, user: String = currentUser()): Boolean =
        com.example.ui.data.Access.level(game, level) == com.example.ui.data.Access.State.Open

    fun starsFor(mistakes: Int): Int = when {
        mistakes <= 0 -> 3
        mistakes <= 2 -> 2
        else -> 1
    }

    /** Saves a finished level and awards points. Call once when the child finishes a level. */
    fun recordLevel(context: Context, gameId: Int, level: Int, mistakes: Int, streaks: Int): LevelResult {
        val game = GameRegistry.byId(gameId)
        val user = currentUser()
        val earnedStars = starsFor(mistakes)
        val previous = if (game != null) stars(game, level, user) else 0

        val lines = mutableListOf<PointsLine>()
        if (previous == 0) {
            lines += PointsLine("Level finished", 10)
            lines += PointsLine(if (earnedStars == 1) "1 star" else "$earnedStars stars", 5 * earnedStars)
            if (streaks > 0) lines += PointsLine(if (streaks == 1) "1 streak" else "$streaks streaks", 2 * streaks)
        } else {
            if (earnedStars > previous) {
                val more = earnedStars - previous
                lines += PointsLine(if (more == 1) "1 more star" else "$more more stars", 5 * more)
            }
            lines += PointsLine("Practice bonus", 2)
        }
        val earned = lines.sumOf { it.points }

        if (game != null && earnedStars > previous) {
            getSharedPreferences(PREFS).edit().putInt(key(user, game.key, level), earnedStars).apply()
        }
        try {
            ScoreManager.addCoins(context, game?.key ?: "game_$gameId", earned)
        } catch (e: Exception) {
            Log.w("GameProgress", "Could not add points", e)
        }
        _version.value += 1

        val message = when (earnedStars) {
            3 -> "No mistakes, super star!"
            2 -> "Great job! Almost perfect!"
            else -> "You did it! Play again for more stars."
        }
        return LevelResult(
            gameId = gameId,
            level = level,
            stars = earnedStars,
            previousBest = previous,
            lines = lines,
            pointsEarned = earned,
            totalPoints = ScoreManager.totalCoins.value,
            message = message
        )
    }
}

/** Counts mistakes and streaks (3 right answers in a row) during one level. */
class LevelTally {
    var mistakes = 0
        private set
    var streaks = 0
        private set
    private var inARow = 0

    fun right() {
        inARow += 1
        if (inARow % 3 == 0) streaks += 1
    }

    fun wrong() {
        mistakes += 1
        inARow = 0
    }

    fun reset() {
        mistakes = 0
        streaks = 0
        inARow = 0
    }
}
