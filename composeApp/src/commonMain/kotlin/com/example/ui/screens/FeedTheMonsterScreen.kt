package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.KannadaLetter
import com.example.data.LetterData
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.components.PauseMenuDialog
import com.example.ui.progress.GameProgress
import com.example.ui.progress.LEVELS_PER_GAME
import com.example.ui.progress.LevelCompleteOverlay
import com.example.ui.progress.LevelResult
import com.example.ui.progress.LevelTally
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Feed the Monster
 * ----------------
 * The monster asks (out loud) for a letter. The child taps the right letter cookie, or drags it
 * into the monster's mouth. Right answer: the monster chomps and grows. Wrong answer: "yuck!".
 * Feed it 5 times to finish a level. Replaces the four Path Builder games:
 *   mode 1 = vowels, 2 = consonants, 3 = guninthakshari, 4 = numbers.
 */

internal enum class MonsterMood { Hungry, Chomp, Yuck, Happy }

internal data class MonsterStyle(
    val title: String,
    val body: Color,
    val bodyDark: Color,
    val belly: Color,
    val background: Color,
    val gameId: String
)

private fun monsterStyleFor(mode: Int): MonsterStyle = when (mode) {
    1 -> MonsterStyle("Vowels", Color(0xFF2EC4B6), Color(0xFF168F84), Color(0xFFB9F0E9), Color(0xFFE8FBF8), "monster_vowels")
    2 -> MonsterStyle("Consonants", Color(0xFF6C63FF), Color(0xFF4A42D6), Color(0xFFD3D0FF), Color(0xFFEFEEFF), "monster_consonants")
    3 -> MonsterStyle("Guninthakshari", Color(0xFFFF6B6B), Color(0xFFD94848), Color(0xFFFFD2D2), Color(0xFFFFF0F0), "monster_guninthakshari")
    else -> MonsterStyle("Numbers", Color(0xFFFF9F1C), Color(0xFFD97E00), Color(0xFFFFE2B8), Color(0xFFFFF6E8), "monster_numbers")
}

private val CookieColors = listOf(
    Color(0xFFFFC93C), Color(0xFF7FD6C9), Color(0xFFFFB3A7), Color(0xFFB9B3FF), Color(0xFFA8E6A1)
)
internal val MonsterInk = Color(0xFF1E2A47)

private const val MEALS_PER_LEVEL = 5

/**
 * Letters the monster asks for on [level] (0-based, 6 levels): levels 1-5 walk through the
 * alphabet in small groups, level 6 mixes everything.
 */
private fun feedLevelLetters(pool: List<KannadaLetter>, level: Int): List<KannadaLetter> {
    if (pool.size <= 3) return pool
    val groups = LEVELS_PER_GAME - 1
    if (level >= groups) return pool
    val start = level * pool.size / groups
    val end = maxOf((level + 1) * pool.size / groups, start + 3).coerceAtMost(pool.size)
    return pool.subList(maxOf(0, end - maxOf(end - start, 3)), end)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedTheMonsterScreen(mode: Int, firstLevel: Int = 1, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val style = remember(mode) { monsterStyleFor(mode) }
    val pool: List<KannadaLetter> = remember(mode) {
        when (mode) {
            1 -> LetterData.vowels
            2 -> LetterData.consonants
            3 -> LetterData.kaGuninthakshari
            else -> LetterData.numbers
        }
    }
    val levelCount = LEVELS_PER_GAME
    val tally = remember { LevelTally() }
    var levelResult by remember { mutableStateOf<LevelResult?>(null) }

    var level by remember { mutableIntStateOf((firstLevel - 1).coerceIn(0, levelCount - 1)) }
    var fed by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf<KannadaLetter?>(null) }
    var choices by remember { mutableStateOf(listOf<KannadaLetter>()) }
    var mood by remember { mutableStateOf(MonsterMood.Hungry) }
    var wrongId by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    var draggingId by remember { mutableStateOf<Int?>(null) }
    var showConfetti by remember { mutableStateOf(false) }
    var showPauseMenu by remember { mutableStateOf(false) }
    var monsterBounds by remember { mutableStateOf(Rect.Zero) }

    val levelLetters = remember(level, pool) {
        feedLevelLetters(pool, level)
    }

    fun speakTarget() {
        target?.let { NativeAudioPlayer.playPhonics(context, it.character) }
    }

    fun newRound(previous: KannadaLetter?) {
        val candidates = levelLetters.filter { it.id != previous?.id }.ifEmpty { levelLetters }
        val next = candidates.random()
        val choiceCount = if (level < 2) 3 else 4
        val sameLevel = levelLetters.filter { it.id != next.id }.shuffled()
        val others = pool.filter { it.id != next.id && it !in levelLetters }.shuffled()
        val distractors = (sameLevel + others).take(choiceCount - 1)
        target = next
        choices = (distractors + next).shuffled()
        mood = MonsterMood.Hungry
        wrongId = null
        scope.launch {
            delay(450)
            speakTarget()
        }
    }

    fun startLevel() {
        fed = 0
        tally.reset()
        levelResult = null
        showConfetti = false
        busy = false
        newRound(null)
    }

    LaunchedEffect(level) { startLevel() }

    fun feed(letter: KannadaLetter) {
        val wanted = target ?: return
        if (busy) return
        busy = true
        scope.launch {
            if (letter.id == wanted.id) {
                mood = MonsterMood.Chomp
                tally.right()
                SoundEffectsHelper.playSuccess(context)
                NativeAudioPlayer.playPhonics(context, letter.character)
                choices = choices.filter { it.id != letter.id }
                delay(900)
                fed += 1
                if (fed >= MEALS_PER_LEVEL) {
                    mood = MonsterMood.Happy
                    showConfetti = true
                    SoundEffectsHelper.playGameCompletion(context)
                    val result = GameProgress.recordLevel(context, mode, level + 1, tally.mistakes, tally.streaks)
                    delay(1200)
                    showConfetti = false
                    levelResult = result
                } else {
                    newRound(letter)
                    busy = false
                }
            } else {
                mood = MonsterMood.Yuck
                tally.wrong()
                wrongId = letter.id
                SoundEffectsHelper.playOops(context)
                delay(900)
                mood = MonsterMood.Hungry
                wrongId = null
                busy = false
                speakTarget()
            }
        }
    }

    Scaffold(
        containerColor = style.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Feed the Monster", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text(
                            "${style.title} · Level ${level + 1}/$levelCount",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showPauseMenu = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = style.bodyDark)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tummy meter: one star per meal
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(MEALS_PER_LEVEL) { i ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i < fed) Color(0xFFFFB300) else Color(0x33000000),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Speech bubble: "Feed me ಕ!" + hear-again button
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (mood) {
                                MonsterMood.Yuck -> "Yuck! Not that one!"
                                MonsterMood.Happy -> "Yum! I'm full!"
                                MonsterMood.Chomp -> "Nom nom nom!"
                                MonsterMood.Hungry -> "Feed me"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MonsterInk
                        )
                        if (mood == MonsterMood.Hungry && target != null) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = target?.character ?: "",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = style.bodyDark
                            )
                        }
                        IconButton(
                            onClick = { speakTarget() },
                            modifier = Modifier.testTag("monster_hear_again")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Hear the letter again",
                                tint = style.bodyDark
                            )
                        }
                    }
                }

                // The monster
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Biggest square that fits; leaves room for the monster to grow as it eats.
                    val side = minOf(maxWidth * 0.8f, maxHeight * 0.85f, 320.dp)
                    Monster(
                        style = style,
                        mood = mood,
                        mouthWide = draggingId != null,
                        growth = fed,
                        modifier = Modifier
                            .size(side)
                            .onGloballyPositioned { monsterBounds = it.boundsInRoot() }
                            .testTag("monster")
                    )
                }

                // Letter cookies
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(2f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    choices.forEachIndexed { index, letter ->
                        LetterCookie(
                            letter = letter,
                            color = CookieColors[(letter.id + index) % CookieColors.size],
                            shaking = wrongId == letter.id,
                            enabled = !busy && mood == MonsterMood.Hungry,
                            monsterBounds = { monsterBounds },
                            onDragStateChange = { dragging -> draggingId = if (dragging) letter.id else null },
                            onFeed = { feed(letter) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap a cookie, or drag it to the monster",
                    fontSize = 13.sp,
                    color = MonsterInk.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            ConfettiAnimation(show = showConfetti)

            if (showPauseMenu) {
                PauseMenuDialog(
                    onDismiss = { showPauseMenu = false },
                    onRestart = {
                        showPauseMenu = false
                        startLevel()
                    },
                    onMainMenu = {
                        showPauseMenu = false
                        onBack()
                    }
                )
            }

            levelResult?.let { result ->
                LevelCompleteOverlay(
                    result = result,
                    onNextLevel = if (level < levelCount - 1) ({ levelResult = null; level += 1 }) else null,
                    onPlayAgain = { startLevel() },
                    onLevelMap = onBack
                )
            }
        }
    }
}

/** A round "cookie" with a letter on it. Tap to feed, or drag it into the monster. */
@Composable
private fun LetterCookie(
    letter: KannadaLetter,
    color: Color,
    shaking: Boolean,
    enabled: Boolean,
    monsterBounds: () -> Rect,
    onDragStateChange: (Boolean) -> Unit,
    onFeed: () -> Unit
) {
    var dragOffset by remember(letter.id) { mutableStateOf(Offset.Zero) }
    var cookieBounds by remember { mutableStateOf(Rect.Zero) }
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnFeed by rememberUpdatedState(onFeed)

    val shake = rememberInfiniteTransition(label = "cookie_shake")
    val wobble by shake.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(70, easing = LinearEasing), RepeatMode.Reverse),
        label = "wobble"
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .graphicsLayer {
                translationX = if (shaking) wobble * 10f else 0f
                val dragging = dragOffset != Offset.Zero
                scaleX = if (dragging) 1.12f else 1f
                scaleY = if (dragging) 1.12f else 1f
            }
            .size(76.dp)
            .shadow(6.dp, CircleShape)
            .background(color, CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .onGloballyPositioned { cookieBounds = it.boundsInRoot() }
            .clickable(enabled = enabled) { onFeed() }
            .pointerInput(letter.id) {
                detectDragGestures(
                    onDragStart = {
                        if (currentEnabled) onDragStateChange(true)
                    },
                    onDrag = { change, amount ->
                        if (currentEnabled) {
                            change.consume()
                            dragOffset += amount
                        }
                    },
                    onDragEnd = {
                        val center = cookieBounds.center
                        val target = monsterBounds()
                        val inside = target != Rect.Zero && target.inflate(24f).contains(center)
                        onDragStateChange(false)
                        dragOffset = Offset.Zero
                        if (inside && currentEnabled) currentOnFeed()
                    },
                    onDragCancel = {
                        onDragStateChange(false)
                        dragOffset = Offset.Zero
                    }
                )
            }
            .testTag("cookie_${letter.id}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.character,
            fontSize = if (letter.character.length > 1) 26.sp else 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MonsterInk
        )
    }
}

/** The monster, drawn with Compose so it looks the same on Android and iOS. */
@Composable
internal fun Monster(
    style: MonsterStyle,
    mood: MonsterMood,
    mouthWide: Boolean,
    growth: Int,
    modifier: Modifier = Modifier
) {
    val idle = rememberInfiniteTransition(label = "monster_idle")
    val breathe by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val chompPhase by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(180, easing = LinearEasing), RepeatMode.Reverse),
        label = "chomp"
    )
    val shakePhase by idle.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(260, easing = LinearEasing), RepeatMode.Restart),
        label = "shake"
    )

    val restingMouth = when {
        mood == MonsterMood.Yuck -> 0.15f
        mood == MonsterMood.Happy -> 0.35f
        mouthWide -> 1f
        else -> 0.45f
    }
    val animatedMouth by animateFloatAsState(restingMouth, tween(220), label = "mouth")
    val mouthOpen = if (mood == MonsterMood.Chomp) chompPhase * 0.9f else animatedMouth
    val growScale by animateFloatAsState(1f + growth * 0.04f, spring(dampingRatio = 0.4f), label = "grow")
    val jump = if (mood == MonsterMood.Happy) breathe * 18f else 0f

    Canvas(
        modifier = modifier.graphicsLayer {
            val s = growScale * (1f + breathe * 0.025f)
            scaleX = s
            scaleY = s
            translationX = if (mood == MonsterMood.Yuck) sin(shakePhase) * 12f else 0f
            translationY = -jump
        }
    ) {
        drawMonster(style, mood, mouthOpen)
    }
}

internal fun DrawScope.drawMonster(style: MonsterStyle, mood: MonsterMood, mouthOpen: Float) {
    val w = size.width
    val cx = w / 2f
    val r = w * 0.38f
    val cy = size.height * 0.56f

    // Horns
    for (side in listOf(-1f, 1f)) {
        val horn = Path().apply {
            moveTo(cx + side * r * 0.35f, cy - r * 0.85f)
            lineTo(cx + side * r * 0.72f, cy - r * 1.32f)
            lineTo(cx + side * r * 0.78f, cy - r * 0.62f)
            close()
        }
        drawPath(horn, style.bodyDark)
    }

    // Feet
    for (side in listOf(-1f, 1f)) {
        drawOval(
            color = style.bodyDark,
            topLeft = Offset(cx + side * r * 0.55f - r * 0.3f, cy + r * 0.82f),
            size = Size(r * 0.6f, r * 0.3f)
        )
    }

    // Body
    drawRoundRect(
        color = style.body,
        topLeft = Offset(cx - r, cy - r),
        size = Size(r * 2f, r * 2f),
        cornerRadius = CornerRadius(r * 0.9f, r * 0.9f)
    )
    // Belly
    drawOval(
        color = style.belly,
        topLeft = Offset(cx - r * 0.6f, cy + r * 0.05f),
        size = Size(r * 1.2f, r * 0.85f)
    )

    // Cheeks
    for (side in listOf(-1f, 1f)) {
        drawCircle(Color(0x55FF5A8A), radius = r * 0.12f, center = Offset(cx + side * r * 0.62f, cy - r * 0.05f))
    }

    // Eyes
    val eyeY = cy - r * 0.42f
    for (side in listOf(-1f, 1f)) {
        val eyeCenter = Offset(cx + side * r * 0.36f, eyeY)
        if (mood == MonsterMood.Yuck) {
            // Squeezed-shut eyes: > <
            val dx = r * 0.14f
            val dy = r * 0.1f
            val path = Path().apply {
                moveTo(eyeCenter.x - side * dx, eyeCenter.y - dy)
                lineTo(eyeCenter.x + side * dx, eyeCenter.y)
                lineTo(eyeCenter.x - side * dx, eyeCenter.y + dy)
            }
            drawPath(path, MonsterInk, style = Stroke(width = r * 0.06f, cap = StrokeCap.Round))
        } else if (mood == MonsterMood.Happy) {
            // Happy closed eyes: ^ ^
            drawArc(
                color = MonsterInk,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(eyeCenter.x - r * 0.15f, eyeCenter.y - r * 0.08f),
                size = Size(r * 0.3f, r * 0.3f),
                style = Stroke(width = r * 0.06f, cap = StrokeCap.Round)
            )
        } else {
            drawCircle(Color.White, radius = r * 0.22f, center = eyeCenter)
            // Pupils look down at the cookies
            drawCircle(MonsterInk, radius = r * 0.1f, center = eyeCenter + Offset(0f, r * 0.07f))
            drawCircle(Color.White, radius = r * 0.035f, center = eyeCenter + Offset(r * 0.03f, r * 0.03f))
        }
    }

    // Mouth
    val mouthCenterY = cy + r * 0.08f
    val mouthWidth = r * (0.7f + 0.35f * mouthOpen)
    val mouthHeight = r * (0.06f + 0.6f * mouthOpen)
    if (mouthOpen < 0.08f || mood == MonsterMood.Yuck) {
        // Wobbly closed mouth, tongue out when yucky
        drawArc(
            color = MonsterInk,
            startAngle = if (mood == MonsterMood.Yuck) 200f else 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - r * 0.3f, mouthCenterY - r * 0.12f),
            size = Size(r * 0.6f, r * 0.3f),
            style = Stroke(width = r * 0.06f, cap = StrokeCap.Round)
        )
        if (mood == MonsterMood.Yuck) {
            drawOval(
                color = Color(0xFFFF7A9C),
                topLeft = Offset(cx - r * 0.1f, mouthCenterY - r * 0.04f),
                size = Size(r * 0.2f, r * 0.26f)
            )
        }
    } else {
        val topLeft = Offset(cx - mouthWidth / 2f, mouthCenterY - mouthHeight / 2f)
        drawOval(color = Color(0xFF5A1020), topLeft = topLeft, size = Size(mouthWidth, mouthHeight))
        // Tongue
        if (mouthOpen > 0.3f) {
            drawOval(
                color = Color(0xFFFF7A9C),
                topLeft = Offset(cx - mouthWidth * 0.25f, mouthCenterY + mouthHeight * 0.05f),
                size = Size(mouthWidth * 0.5f, mouthHeight * 0.42f)
            )
        }
        // Two teeth
        for (side in listOf(-1f, 1f)) {
            val toothX = cx + side * mouthWidth * 0.2f
            val tooth = Path().apply {
                moveTo(toothX - r * 0.07f, mouthCenterY - mouthHeight / 2f + r * 0.01f)
                lineTo(toothX + r * 0.07f, mouthCenterY - mouthHeight / 2f + r * 0.01f)
                lineTo(toothX, mouthCenterY - mouthHeight / 2f + r * 0.13f)
                close()
            }
            drawPath(tooth, Color.White)
        }
    }
}
