package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.data.ActivityTracker
import com.example.ui.progress.GameProgress
import com.example.ui.progress.LevelResult
import com.example.ui.progress.LevelTally
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * Scrambled Words: a picture and its syllables are jumbled up; tap them in the right order to
 * build the word. 6 levels, 5 words each:
 *   1: 2-syllable words            2: 2-syllable + 1 extra tile
 *   3: 3-syllable words            4: 3-syllable + 1 extra tile
 *   5: mixed words + 2 extra tiles 6: harder words (ಒತ್ತಕ್ಷರ) + 2 extra tiles
 */

internal val CandyColors = listOf(
    Color(0xFFFF6B6B), Color(0xFFFFC93C), Color(0xFF2EC4B6), Color(0xFF6C63FF),
    Color(0xFFFF9F1C), Color(0xFFE84393), Color(0xFF4DA8FF), Color(0xFF59C173)
)

private data class WordLevel(val label: String, val extraTiles: Int, val pick: (PictureWord) -> Boolean)

private val wordLevels = listOf(
    WordLevel("Two-part words", 0) { it.syllables.size == 2 && !it.hasConjunct },
    WordLevel("Two-part words + a tricky tile", 1) { it.syllables.size == 2 && !it.hasConjunct },
    WordLevel("Three-part words", 0) { it.syllables.size == 3 && !it.hasConjunct },
    WordLevel("Three-part words + a tricky tile", 1) { it.syllables.size == 3 },
    WordLevel("Mixed words + 2 tricky tiles", 2) { it.syllables.size >= 2 },
    WordLevel("Hard words + 2 tricky tiles", 2) { it.hasConjunct || it.syllables.size == 3 }
)

private const val WORDS_PER_LEVEL = 5
private val WordBg = Color(0xFFFFF4E6)

private data class Tile(val id: Int, val text: String, val color: Color)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScrambledWordsScreen(firstLevel: Int = 1, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var level by remember { mutableIntStateOf((firstLevel - 1).coerceIn(0, wordLevels.size - 1)) }
    val spec = wordLevels[level]
    var round by remember { mutableIntStateOf(0) }
    var done by remember { mutableIntStateOf(0) }
    val tally = remember { LevelTally() }
    var levelResult by remember { mutableStateOf<LevelResult?>(null) }
    var showConfetti by remember { mutableStateOf(false) }

    val words = remember(level, round) {
        val pool = pictureWords.filter(spec.pick).ifEmpty { pictureWords }
        pool.shuffled().take(WORDS_PER_LEVEL).let { if (it.size < WORDS_PER_LEVEL) (it + pool.shuffled()).take(WORDS_PER_LEVEL) else it }
    }
    val word = words[done.coerceIn(0, words.size - 1)]
    var placed by remember(level, round, done) { mutableIntStateOf(0) }
    var shakeId by remember { mutableStateOf<Int?>(null) }
    var shakeCount by remember { mutableIntStateOf(0) }
    var celebrating by remember(level, round, done) { mutableStateOf(false) }

    val tiles = remember(level, round, done) {
        val extras = pictureWords.flatMap { it.syllables }.distinct()
            .filter { s -> s !in word.syllables }.shuffled().take(spec.extraTiles)
        (word.syllables + extras).mapIndexed { i, s -> Tile(i, s, CandyColors[(i + done * 3) % CandyColors.size]) }.shuffled()
    }
    var usedIds by remember(level, round, done) { mutableStateOf(setOf<Int>()) }

    fun startLevel(newLevel: Int) {
        tally.reset()
        levelResult = null
        showConfetti = false
        done = 0
        level = newLevel
        round += 1
    }

    LaunchedEffect(level, round, done) {
        delay(500)
        NativeAudioPlayer.playPhonics(context, word.text)
    }

    fun tap(tile: Tile) {
        if (celebrating || tile.id in usedIds) return
        val needed = word.syllables.getOrNull(placed) ?: return
        if (tile.text == needed) {
            usedIds = usedIds + tile.id
            placed += 1
            tally.right()
            SoundEffectsHelper.playPop()
            NativeAudioPlayer.playPhonics(context, tile.text)
            if (placed == word.syllables.size) {
                celebrating = true
                ActivityTracker.recordAnswer(word.text, true)
                scope.launch {
                    delay(500)
                    SoundEffectsHelper.playSuccess(context)
                    NativeAudioPlayer.playPhonics(context, word.text)
                    delay(1500)
                    if (done + 1 >= WORDS_PER_LEVEL) {
                        done += 1
                        showConfetti = true
                        SoundEffectsHelper.playGameCompletion(context)
                        val result = GameProgress.recordLevel(context, 8, level + 1, tally.mistakes, tally.streaks)
                        delay(1200)
                        showConfetti = false
                        levelResult = result
                    } else {
                        done += 1
                    }
                }
            }
        } else {
            tally.wrong()
            ActivityTracker.recordAnswer(word.text, false)
            SoundEffectsHelper.playOops(context)
            shakeId = tile.id
            shakeCount += 1
        }
    }
    val shake = rememberShake(shakeCount)

    LevelGameScaffold(
        title = "Scrambled Words",
        subtitle = "Level ${level + 1}/${wordLevels.size} · ${spec.label}",
        headerColor = Color(0xFFFF9F1C),
        background = WordBg,
        rounds = WORDS_PER_LEVEL,
        roundsDone = done,
        showConfetti = showConfetti,
        levelResult = levelResult,
        hasNextLevel = level < wordLevels.size - 1,
        onNextLevel = { startLevel(level + 1) },
        onPlayAgain = { startLevel(level) },
        onBack = onBack
    ) {
        // Picture card
        val bounce = rememberInfiniteTransition(label = "pic")
        val bob by bounce.animateFloat(0f, -8f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
        Box(
            Modifier
                .size(170.dp)
                .graphicsLayer { translationY = bob * density }
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(CandyColors[(done + 2) % CandyColors.size].copy(alpha = 0.25f))
                .background(Color.White.copy(alpha = 0.55f))
                .clickable { NativeAudioPlayer.playPhonics(context, word.text) }
                .testTag("word_picture"),
            contentAlignment = Alignment.Center
        ) { Text(word.picture, fontSize = 92.sp) }
        Spacer(Modifier.height(8.dp))
        Text(
            if (celebrating) "${word.text}!  ${word.english}" else word.english,
            fontSize = if (celebrating) 26.sp else 18.sp, fontWeight = FontWeight.ExtraBold,
            color = if (celebrating) Color(0xFF168F84) else GameInk
        )
        Text("Tap the pieces in order to make the word", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GameInk.copy(alpha = 0.6f))

        Spacer(Modifier.height(18.dp))

        // Slots
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            word.syllables.forEachIndexed { i, s ->
                val filled = i < placed
                val pop by animateFloatAsState(if (filled) 1f else 0.9f, spring(dampingRatio = 0.35f), label = "slot")
                Box(
                    Modifier
                        .size(76.dp)
                        .graphicsLayer { scaleX = pop; scaleY = pop }
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (filled) CandyColors[(i + done * 3) % CandyColors.size] else Color.White)
                        .border(3.dp, if (filled) Color.White else GameInk.copy(alpha = 0.2f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (filled) Text(s, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = GameInk)
                    else Text("${i + 1}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = GameInk.copy(alpha = 0.25f))
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Tiles
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            tiles.forEach { t ->
                val used = t.id in usedIds
                val wobble by bounce.animateFloat(-4f, 4f, infiniteRepeatable(tween(700 + t.id * 90, easing = LinearEasing), RepeatMode.Reverse), label = "tile${t.id}")
                Box(
                    Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            rotationZ = if (used) 0f else wobble
                            translationX = if (shakeId == t.id) shake * density else 0f
                            alpha = if (used) 0.15f else 1f
                        }
                        .shadow(if (used) 0.dp else 6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(t.color)
                        .border(4.dp, Color.White, CircleShape)
                        .clickable(enabled = !used) { tap(t) }
                        .testTag("tile_${t.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(t.text, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = GameInk)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
