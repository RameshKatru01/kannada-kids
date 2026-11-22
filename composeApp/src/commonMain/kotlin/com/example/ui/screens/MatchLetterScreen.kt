package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KannadaLetter
import com.example.data.LetterData
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.data.ActivityTracker
import com.example.ui.progress.GameProgress
import com.example.ui.progress.LevelResult
import com.example.ui.progress.LevelTally
import com.example.ui.reference.RefWord
import com.example.ui.reference.ReferenceInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * Match the Letter: letters on the left, pictures on the right. Tap a letter, then the picture whose
 * word starts with it; a coloured string joins them. 6 levels, 2 boards each, more pairs as it goes.
 */

private data class MatchLevel(val label: String, val pairs: Int, val pool: () -> List<KannadaLetter>)

private val matchLevels get() = listOf(
    MatchLevel("Vowels · 3 pairs", 3) { LetterData.vowels },
    MatchLevel("Vowels · 4 pairs", 4) { LetterData.vowels },
    MatchLevel(com.example.data.Lang.script("ಕ to ನ · 4 pairs"), 4) { LetterData.consonants.take(20) },
    MatchLevel(com.example.data.Lang.script("ಪ to ಹ · 4 pairs"), 4) { LetterData.consonants.drop(20) },
    MatchLevel("Mixed · 5 pairs", 5) { LetterData.vowels + LetterData.consonants },
    MatchLevel("Mixed · 6 pairs", 6) { LetterData.vowels + LetterData.consonants }
)

private const val BOARDS = 2

private data class MatchItem(val letter: KannadaLetter, val word: RefWord, val color: Color, val image: org.jetbrains.compose.resources.DrawableResource? = null)

@Composable
fun MatchLetterScreen(firstLevel: Int = 1, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var level by remember { mutableIntStateOf((firstLevel - 1).coerceIn(0, matchLevels.size - 1)) }
    val spec = matchLevels[level]
    var round by remember { mutableIntStateOf(0) }
    var board by remember { mutableIntStateOf(0) }
    val tally = remember { LevelTally() }
    var levelResult by remember { mutableStateOf<LevelResult?>(null) }
    var showConfetti by remember { mutableStateOf(false) }

    val items = remember(level, round, board) {
        // Kannada: pictures from the letter videos (ಅ → ಅರಸ); Hindi: the emoji words
        val withPictures = spec.pool().mapNotNull { l -> gamePicture(l.character)?.let { l to it } }
        if (withPictures.size >= spec.pairs) {
            withPictures.shuffled().take(spec.pairs).mapIndexed { i, (l, p) ->
                MatchItem(l, RefWord(p.word, p.english, ""), CandyColors[(i + board * 2) % CandyColors.size], p.image)
            }
        } else {
            spec.pool().mapNotNull { l -> ReferenceInfo.word(l)?.let { l to it } }
                .shuffled().take(spec.pairs)
                .mapIndexed { i, (l, w) -> MatchItem(l, w, CandyColors[(i + board * 2) % CandyColors.size]) }
        }
    }
    val pictures = remember(items) { items.shuffled() }
    var matched by remember(items) { mutableStateOf(listOf<MatchItem>()) }
    var picked by remember(items) { mutableStateOf<MatchItem?>(null) }
    var shakeCount by remember { mutableIntStateOf(0) }
    var shakeTarget by remember { mutableStateOf<String?>(null) }
    val letterBounds = remember(items) { mutableStateMapOf<String, Rect>() }
    val pictureBounds = remember(items) { mutableStateMapOf<String, Rect>() }
    var areaOrigin by remember { mutableStateOf(Offset.Zero) }

    fun startLevel(newLevel: Int) {
        tally.reset()
        levelResult = null
        showConfetti = false
        board = 0
        level = newLevel
        round += 1
    }

    fun pickPicture(item: MatchItem) {
        val p = picked ?: run {
            NativeAudioPlayer.playPhonics(context, item.word.kannada)
            return
        }
        if (item in matched) return
        if (item.letter.id == p.letter.id) {
            matched = matched + item
            picked = null
            tally.right()
            ActivityTracker.recordAnswer(item.letter.character, true)
            SoundEffectsHelper.playSuccess(context)
            NativeAudioPlayer.playPhonics(context, item.word.kannada)
            if (matched.size == items.size) {
                scope.launch {
                    delay(1000)
                    if (board + 1 < BOARDS) {
                        SoundEffectsHelper.playSparkle()
                        board += 1
                    } else {
                        showConfetti = true
                        SoundEffectsHelper.playGameCompletion(context)
                        val result = GameProgress.recordLevel(context, 11, level + 1, tally.mistakes, tally.streaks)
                        delay(1200)
                        showConfetti = false
                        levelResult = result
                    }
                }
            }
        } else {
            tally.wrong()
            ActivityTracker.recordAnswer(p.letter.character, false)
            SoundEffectsHelper.playOops(context)
            shakeTarget = item.word.kannada
            shakeCount += 1
        }
    }
    val shake = rememberShake(shakeCount)
    val done = board * spec.pairs + matched.size

    LevelGameScaffold(
        title = "Match the Letter",
        subtitle = "Level ${level + 1}/${matchLevels.size} · ${spec.label} · board ${board + 1}/$BOARDS",
        headerColor = Color(0xFF4DA8FF),
        background = Color(0xFFEAF6FF),
        rounds = spec.pairs * BOARDS,
        roundsDone = done,
        showConfetti = showConfetti,
        levelResult = levelResult,
        hasNextLevel = level < matchLevels.size - 1,
        onNextLevel = { startLevel(level + 1) },
        onPlayAgain = { startLevel(level) },
        onBack = onBack
    ) {
        Text(
            if (picked == null) "Tap a letter, then its picture" else "Now find the picture for ${picked!!.letter.character}",
            fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GameInk
        )
        Spacer(Modifier.height(12.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .onGloballyPositioned { areaOrigin = it.boundsInRoot().topLeft }
        ) {
            // Strings between matched pairs (and from the picked letter)
            Canvas(Modifier.fillMaxSize()) {
                matched.forEach { m ->
                    val a = letterBounds[m.letter.character]
                    val b = pictureBounds[m.word.kannada]
                    if (a != null && b != null) {
                        drawLine(
                            m.color,
                            Offset(a.right, a.center.y) - areaOrigin,
                            Offset(b.left, b.center.y) - areaOrigin,
                            strokeWidth = 10.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Letters
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                    items.forEach { item ->
                        val isMatched = item in matched
                        val isPicked = picked == item
                        val lift by animateFloatAsState(if (isPicked) 1.12f else 1f, spring(dampingRatio = 0.4f), label = "lift")
                        Box(
                            Modifier
                                .size(if (items.size >= 6) 62.dp else 76.dp)
                                .graphicsLayer { scaleX = lift; scaleY = lift }
                                .onGloballyPositioned { letterBounds[item.letter.character] = it.boundsInRoot() }
                                .shadow(if (isPicked) 10.dp else 4.dp, RoundedCornerShape(22.dp))
                                .clip(RoundedCornerShape(22.dp))
                                .background(item.color)
                                .border(if (isPicked) 5.dp else 3.dp, if (isPicked) GameInk else Color.White, RoundedCornerShape(22.dp))
                                .clickable(enabled = !isMatched) {
                                    picked = if (isPicked) null else item
                                    NativeAudioPlayer.playPhonics(context, item.letter.character)
                                }
                                .testTag("match_letter_${item.letter.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.letter.character, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = GameInk)
                        }
                    }
                }
                // Pictures
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.End) {
                    pictures.forEach { item ->
                        val isMatched = item in matched
                        Row(
                            Modifier
                                .graphicsLayer { translationX = if (shakeTarget == item.word.kannada) shake * density else 0f }
                                .onGloballyPositioned { pictureBounds[item.word.kannada] = it.boundsInRoot() }
                                .shadow(4.dp, RoundedCornerShape(22.dp))
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (isMatched) item.color else Color.White)
                                .clickable(enabled = !isMatched) { pickPicture(item) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("match_picture_${item.letter.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val img = item.image
                            if (img != null) {
                                androidx.compose.foundation.Image(
                                    org.jetbrains.compose.resources.painterResource(img), contentDescription = item.word.english,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.size(if (items.size >= 6) 46.dp else 58.dp).clip(RoundedCornerShape(14.dp))
                                )
                            } else {
                                Text(item.word.picture, fontSize = if (items.size >= 6) 34.sp else 42.sp)
                            }
                            Column {
                                Text(
                                    if (isMatched) item.word.kannada else item.word.english,
                                    fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = GameInk
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
