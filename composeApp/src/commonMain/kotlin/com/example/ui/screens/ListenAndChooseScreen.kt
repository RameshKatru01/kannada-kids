package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * Listen & Choose: a parrot says a letter, the child taps the matching letter card.
 * 6 levels, 6 questions each. Wrong choices come from letters that sound alike, so it gets harder.
 */

private data class ListenLevel(val label: String, val pool: List<String>, val options: Int)

private val listenLevels get() = listOf(
    ListenLevel("Short vowels", listOf("ಅ", "ಇ", "ಉ", "ಎ", "ಒ"), 3),
    ListenLevel("All vowels", listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ", "ಊ", "ಋ", "ಎ", "ಏ", "ಐ", "ಒ", "ಓ", "ಔ", "ಅಂ", "ಅಃ"), 3),
    ListenLevel("ಕ to ಞ", listOf("ಕ", "ಖ", "ಗ", "ಘ", "ಙ", "ಚ", "ಛ", "ಜ", "ಝ", "ಞ"), 4),
    ListenLevel("ಟ to ಮ", listOf("ಟ", "ಠ", "ಡ", "ಢ", "ಣ", "ತ", "ಥ", "ದ", "ಧ", "ನ", "ಪ", "ಫ", "ಬ", "ಭ", "ಮ"), 4),
    ListenLevel("ಯ to ಳ and numbers", listOf("ಯ", "ರ", "ಲ", "ವ", "ಶ", "ಷ", "ಸ", "ಹ", "ಳ", "೧", "೨", "೩", "೪", "೫", "೬", "೭", "೮", "೯"), 4),
    ListenLevel("Sound-alike letters", listOf("ಕ", "ಖ", "ಗ", "ಘ", "ಟ", "ಠ", "ತ", "ಥ", "ದ", "ಧ", "ಡ", "ಢ", "ಬ", "ಭ", "ಪ", "ಫ", "ಕಾ", "ಕಿ", "ಕೀ", "ಕು", "ಕೂ"), 4)
).map { it.copy(label = com.example.data.Lang.script(it.label), pool = com.example.data.Lang.letters(it.pool)) }

private const val QUESTIONS = 6

@Composable
fun ListenAndChooseScreen(firstLevel: Int = 1, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var level by remember { mutableIntStateOf((firstLevel - 1).coerceIn(0, listenLevels.size - 1)) }
    val spec = listenLevels[level]
    var round by remember { mutableIntStateOf(0) }
    var done by remember { mutableIntStateOf(0) }
    val tally = remember { LevelTally() }
    var levelResult by remember { mutableStateOf<LevelResult?>(null) }
    var showConfetti by remember { mutableStateOf(false) }

    val targets = remember(level, round) {
        val shuffled = spec.pool.shuffled()
        List(QUESTIONS) { shuffled[it % shuffled.size] }
    }
    val target = targets[done.coerceIn(0, QUESTIONS - 1)]
    val options = remember(level, round, done) {
        // Neighbours in the list sound most alike, so pick distractors close to the target
        val i = spec.pool.indexOf(target)
        val near = spec.pool.filter { it != target }.sortedBy { kotlin.math.abs(spec.pool.indexOf(it) - i) }
        (near.take(spec.options + 1).shuffled().take(spec.options - 1) + target).shuffled()
    }
    var wrong by remember(level, round, done) { mutableStateOf(setOf<String>()) }
    var picked by remember(level, round, done) { mutableStateOf<String?>(null) }
    var shakeCount by remember { mutableIntStateOf(0) }
    var shakeTarget by remember { mutableStateOf<String?>(null) }
    var talking by remember { mutableStateOf(false) }

    fun say() {
        NativeAudioPlayer.playPhonics(context, target)
        scope.launch { talking = true; delay(900); talking = false }
    }

    fun startLevel(newLevel: Int) {
        tally.reset()
        levelResult = null
        showConfetti = false
        done = 0
        level = newLevel
        round += 1
    }

    LaunchedEffect(level, round, done) {
        if (done < QUESTIONS) {
            delay(600)
            say()
        }
    }

    fun choose(option: String) {
        if (picked != null || option in wrong) return
        if (option == target) {
            picked = option
            tally.right()
            ActivityTracker.recordAnswer(target, true)
            SoundEffectsHelper.playSuccess(context)
            scope.launch {
                delay(1100)
                if (done + 1 >= QUESTIONS) {
                    done += 1
                    showConfetti = true
                    SoundEffectsHelper.playGameCompletion(context)
                    val result = GameProgress.recordLevel(context, 19, level + 1, tally.mistakes, tally.streaks)
                    delay(1200)
                    showConfetti = false
                    levelResult = result
                } else {
                    done += 1
                }
            }
        } else {
            wrong = wrong + option
            tally.wrong()
            ActivityTracker.recordAnswer(target, false)
            SoundEffectsHelper.playOops(context)
            shakeTarget = option
            shakeCount += 1
            scope.launch { delay(700); say() }
        }
    }
    val shake = rememberShake(shakeCount)

    LevelGameScaffold(
        title = "Listen & Choose",
        subtitle = "Level ${level + 1}/${listenLevels.size} · ${spec.label}",
        headerColor = Color(0xFF2EC4B6),
        background = Color(0xFFE8FBF8),
        rounds = QUESTIONS,
        roundsDone = done,
        showConfetti = showConfetti,
        levelResult = levelResult,
        hasNextLevel = level < listenLevels.size - 1,
        onNextLevel = { startLevel(level + 1) },
        onPlayAgain = { startLevel(level) },
        onBack = onBack
    ) {
        // Parrot + speech bubble
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Parrot(talking = talking, happy = picked != null, modifier = Modifier.size(130.dp).clickable { say() })
            Column(
                Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .clickable { say() }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("listen_again"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (picked != null) "Yes! $target" else if (wrong.isNotEmpty()) "Listen again!" else "Which one is it?",
                    fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = GameInk
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear it again", tint = Color(0xFF168F84))
                    Text("Tap to hear", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF168F84))
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Letter cards
        val columns = if (options.size == 3) 3 else 2
        options.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 7.dp)) {
                row.forEach { option ->
                    val i = options.indexOf(option)
                    val isRight = picked == option
                    val isWrong = option in wrong
                    val pop by animateFloatAsState(if (isRight) 1.15f else 1f, spring(dampingRatio = 0.35f), label = "pop")
                    Box(
                        Modifier
                            .size(if (columns == 3) 104.dp else 130.dp)
                            .graphicsLayer {
                                scaleX = pop; scaleY = pop
                                translationX = if (shakeTarget == option) shake * density else 0f
                                alpha = if (isWrong) 0.35f else 1f
                            }
                            .shadow(if (isWrong) 0.dp else 6.dp, RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .background(if (isRight) Color(0xFF59C173) else CandyColors[(i + done) % CandyColors.size])
                            .border(5.dp, Color.White, RoundedCornerShape(28.dp))
                            .clickable(enabled = !isWrong && picked == null) { choose(option) }
                            .testTag("option_$i"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(option, fontSize = if (option.length > 1) 40.sp else 52.sp, fontWeight = FontWeight.ExtraBold, color = GameInk)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

/** A friendly green parrot drawn with shapes; its beak opens while it "talks". */
@Composable
private fun Parrot(talking: Boolean, happy: Boolean, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "parrot")
    val beak by t.animateFloat(0f, 1f, infiniteRepeatable(tween(160), RepeatMode.Reverse), label = "beak")
    val bob by t.animateFloat(0f, -5f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val open = if (talking) beak else 0f
    Canvas(modifier.graphicsLayer { translationY = bob * density; rotationZ = if (happy) -6f else 0f }) {
        val w = size.width
        val k = w / 100f
        // tail + body
        drawOval(Color(0xFF168F84), Offset(28 * k, 62 * k), androidx.compose.ui.geometry.Size(22 * k, 34 * k))
        drawOval(Color(0xFF59C173), Offset(22 * k, 30 * k), androidx.compose.ui.geometry.Size(52 * k, 58 * k))
        drawOval(Color(0xFFFFE082), Offset(34 * k, 52 * k), androidx.compose.ui.geometry.Size(28 * k, 30 * k))
        // wing
        drawOval(Color(0xFF2EC4B6), Offset(18 * k, 46 * k), androidx.compose.ui.geometry.Size(22 * k, 32 * k))
        // head
        drawCircle(Color(0xFF59C173), 22 * k, Offset(56 * k, 30 * k))
        drawCircle(Color(0xFFFF6B6B), 6 * k, Offset(44 * k, 14 * k))
        drawCircle(Color.White, 7 * k, Offset(60 * k, 26 * k))
        drawCircle(GameInk, 3.5f * k, Offset(61 * k, 26 * k))
        // beak (upper + lower that opens)
        val upper = androidx.compose.ui.graphics.Path().apply {
            moveTo(72 * k, 28 * k); lineTo(90 * k, 34 * k); lineTo(72 * k, 40 * k); close()
        }
        drawPath(upper, Color(0xFFFF9F1C))
        val lower = androidx.compose.ui.graphics.Path().apply {
            moveTo(72 * k, 40 * k); lineTo((84 + 2 * open) * k, (42 + 8 * open) * k); lineTo(72 * k, (44 + 4 * open) * k); close()
        }
        drawPath(lower, Color(0xFFE07F00))
        // feet
        drawLine(Color(0xFFFF9F1C), Offset(42 * k, 88 * k), Offset(42 * k, 96 * k), strokeWidth = 4 * k)
        drawLine(Color(0xFFFF9F1C), Offset(54 * k, 88 * k), Offset(54 * k, 96 * k), strokeWidth = 4 * k)
        if (talking) {
            drawArc(Color(0xFF2EC4B6), -40f, 80f, false, Offset(88 * k, 22 * k), androidx.compose.ui.geometry.Size(10 * k, 24 * k), style = Stroke(3 * k))
        }
    }
}
