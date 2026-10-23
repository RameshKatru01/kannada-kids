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
 * Swara Adventure: a little explorer hops along stepping stones to a treasure chest.
 * Each stone is a picture word with its first vowel missing (_ನೆ 🐘); pick the right ಸ್ವರ to hop on.
 * 6 levels: short/long vowel pairs first, then more choices, and finally "listen only" (picture hidden).
 */

internal data class VowelWord(val vowel: String, val word: String, val picture: String, val english: String)

internal val vowelWords = listOf(
    VowelWord("ಅ", "ಅಮ್ಮ", "👩‍👧", "mother"), VowelWord("ಅ", "ಅರಸ", "👑", "king"),
    VowelWord("ಆ", "ಆನೆ", "🐘", "elephant"), VowelWord("ಆ", "ಆಮೆ", "🐢", "turtle"),
    VowelWord("ಇ", "ಇಲಿ", "🐭", "mouse"), VowelWord("ಇ", "ಇರುವೆ", "🐜", "ant"),
    VowelWord("ಈ", "ಈರುಳ್ಳಿ", "🧅", "onion"), VowelWord("ಈ", "ಈಜು", "🏊", "swim"),
    VowelWord("ಉ", "ಉಗಿಬಂಡಿ", "🚂", "steam train"), VowelWord("ಉ", "ಉಪ್ಪು", "🧂", "salt"),
    VowelWord("ಊ", "ಊಟ", "🍛", "meal"),
    VowelWord("ಋ", "ಋಷಿ", "🧘", "sage"),
    VowelWord("ಎ", "ಎಲೆ", "🍃", "leaf"), VowelWord("ಎ", "ಎತ್ತು", "🐂", "ox"),
    VowelWord("ಏ", "ಏಣಿ", "🪜", "ladder"),
    VowelWord("ಐ", "ಐದು", "🖐️", "five"),
    VowelWord("ಒ", "ಒಂಟೆ", "🐫", "camel"),
    VowelWord("ಓ", "ಓಟ", "🏃", "race"),
    VowelWord("ಔ", "ಔಷಧ", "💊", "medicine"),
    VowelWord("ಅಂ", "ಅಂಗಿ", "👕", "shirt")
)

private data class SwaraLevel(val label: String, val vowels: List<String>, val options: Int, val listenOnly: Boolean = false)

private val swaraLevels = listOf(
    SwaraLevel("ಅ ಆ ಇ ಈ", listOf("ಅ", "ಆ", "ಇ", "ಈ"), 2),
    SwaraLevel("ಉ ಊ ಎ ಏ", listOf("ಉ", "ಊ", "ಎ", "ಏ"), 2),
    SwaraLevel("ಋ ಐ ಒ ಓ ಔ ಅಂ", listOf("ಋ", "ಐ", "ಒ", "ಓ", "ಔ", "ಅಂ"), 3),
    SwaraLevel("All the ಸ್ವರಗಳು", vowelWords.map { it.vowel }.distinct(), 3),
    SwaraLevel("More choices", vowelWords.map { it.vowel }.distinct(), 4),
    SwaraLevel("Listen only (no picture)", vowelWords.map { it.vowel }.distinct(), 4, listenOnly = true)
)

/** Short/long partners: the trickiest wrong answers. */
private val partners = mapOf(
    "ಅ" to "ಆ", "ಆ" to "ಅ", "ಇ" to "ಈ", "ಈ" to "ಇ", "ಉ" to "ಊ", "ಊ" to "ಉ", "ಎ" to "ಏ", "ಏ" to "ಎ",
    "ಒ" to "ಓ", "ಓ" to "ಒ", "ಐ" to "ಎ", "ಔ" to "ಒ", "ಋ" to "ಉ", "ಅಂ" to "ಅ"
)

private const val STONES = 5

@Composable
fun SwaraAdventureScreen(firstLevel: Int = 1, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var level by remember { mutableIntStateOf((firstLevel - 1).coerceIn(0, swaraLevels.size - 1)) }
    val spec = swaraLevels[level]
    var round by remember { mutableIntStateOf(0) }
    var done by remember { mutableIntStateOf(0) }
    val tally = remember { LevelTally() }
    var levelResult by remember { mutableStateOf<LevelResult?>(null) }
    var showConfetti by remember { mutableStateOf(false) }

    val questions = remember(level, round) {
        val pool = vowelWords.filter { it.vowel in spec.vowels }.shuffled()
        List(STONES) { pool[it % pool.size] }
    }
    val q = questions[done.coerceIn(0, STONES - 1)]
    val options = remember(level, round, done) {
        val others = vowelWords.map { it.vowel }.distinct().filter { it != q.vowel }
        val partner = partners[q.vowel]?.takeIf { it in others }
        val rest = (spec.vowels.filter { it != q.vowel && it != partner } + others).distinct().shuffled()
        (listOfNotNull(partner) + rest).take(spec.options - 1).plus(q.vowel).shuffled()
    }
    var answered by remember(level, round, done) { mutableStateOf(false) }
    var wrong by remember(level, round, done) { mutableStateOf(setOf<String>()) }
    var shakeCount by remember { mutableIntStateOf(0) }
    var shakeTarget by remember { mutableStateOf<String?>(null) }

    val hop = remember { Animatable(0f) }

    fun startLevel(newLevel: Int) {
        tally.reset()
        levelResult = null
        showConfetti = false
        done = 0
        level = newLevel
        round += 1
    }

    LaunchedEffect(level, round, done) {
        if (done < STONES) {
            delay(500)
            NativeAudioPlayer.playPhonics(context, q.word)
        }
    }

    fun choose(v: String) {
        if (answered || v in wrong) return
        if (v == q.vowel) {
            answered = true
            tally.right()
            ActivityTracker.recordAnswer(q.vowel, true)
            SoundEffectsHelper.playSuccess(context)
            NativeAudioPlayer.playPhonics(context, q.word)
            scope.launch {
                hop.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
                hop.snapTo(0f)
                delay(500)
                if (done + 1 >= STONES) {
                    done += 1
                    showConfetti = true
                    SoundEffectsHelper.playGameCompletion(context)
                    val result = GameProgress.recordLevel(context, 20, level + 1, tally.mistakes, tally.streaks)
                    delay(1200)
                    showConfetti = false
                    levelResult = result
                } else {
                    done += 1
                }
            }
        } else {
            wrong = wrong + v
            tally.wrong()
            ActivityTracker.recordAnswer(q.vowel, false)
            SoundEffectsHelper.playOops(context)
            NativeAudioPlayer.playPhonics(context, v)
            shakeTarget = v
            shakeCount += 1
        }
    }
    val shake = rememberShake(shakeCount)

    LevelGameScaffold(
        title = "Swara Adventure",
        subtitle = "Level ${level + 1}/${swaraLevels.size} · ${spec.label}",
        headerColor = Color(0xFF59C173),
        background = Color(0xFFEAF8E6),
        rounds = STONES,
        roundsDone = done,
        showConfetti = showConfetti,
        levelResult = levelResult,
        hasNextLevel = level < swaraLevels.size - 1,
        onNextLevel = { startLevel(level + 1) },
        onPlayAgain = { startLevel(level) },
        onBack = onBack
    ) {
        // Stepping-stone path to the treasure
        BoxWithConstraints(Modifier.fillMaxWidth().height(96.dp)) {
            val step = maxWidth / (STONES + 1)
            // river
            Box(Modifier.align(Alignment.Center).fillMaxWidth().height(34.dp).clip(RoundedCornerShape(17.dp)).background(Color(0xFF9ED8FF)))
            for (i in 0 until STONES) {
                Box(
                    Modifier
                        .offset(x = step * i + step / 2 - 22.dp, y = 30.dp)
                        .size(44.dp, 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (i < done) Color(0xFFFFC93C) else Color(0xFFB0A89A))
                )
            }
            Text("🎁", fontSize = 40.sp, modifier = Modifier.offset(x = step * STONES + step / 2 - 22.dp, y = 22.dp))
            // explorer
            val pos = done.coerceAtMost(STONES - 1).toFloat() + hop.value
            Text(
                "🦊",
                fontSize = 40.sp,
                modifier = Modifier
                    .offset(x = step * pos + step / 2 - 22.dp, y = 4.dp)
                    .graphicsLayer { translationY = -kotlin.math.sin(hop.value * kotlin.math.PI).toFloat() * 26f * density }
            )
        }

        Spacer(Modifier.height(10.dp))

        // Picture + word with a missing vowel
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .clickable { NativeAudioPlayer.playPhonics(context, q.word) }
                .padding(vertical = 16.dp)
                .testTag("swara_word"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (spec.listenOnly && !answered) "👂" else q.picture, fontSize = 76.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (answered) Color(0xFF59C173) else Color(0xFFFFF0C2))
                        .border(3.dp, if (answered) Color.White else Color(0xFFFFC93C), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (answered) q.vowel else "?", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = GameInk)
                }
                Text(q.word.removePrefix(q.vowel), fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = GameInk, modifier = Modifier.padding(start = 4.dp))
            }
            Text(
                if (answered) "${q.word} · ${q.english}" else "Tap to hear the word",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GameInk.copy(alpha = 0.65f)
            )
        }

        Spacer(Modifier.weight(1f))
        Text("Which ಸ್ವರ is missing?", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GameInk)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEachIndexed { i, v ->
                val isWrong = v in wrong
                val isRight = answered && v == q.vowel
                Box(
                    Modifier
                        .size(if (options.size >= 4) 72.dp else 82.dp)
                        .graphicsLayer {
                            translationX = if (shakeTarget == v) shake * density else 0f
                            alpha = if (isWrong) 0.35f else 1f
                        }
                        .shadow(if (isWrong) 0.dp else 6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(if (isRight) Color(0xFF59C173) else CandyColors[(i * 2 + done) % CandyColors.size])
                        .border(5.dp, Color.White, CircleShape)
                        .clickable(enabled = !answered && !isWrong) { choose(v) }
                        .testTag("vowel_$i"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(v, fontSize = if (v.length > 1) 30.sp else 38.sp, fontWeight = FontWeight.ExtraBold, color = GameInk)
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
