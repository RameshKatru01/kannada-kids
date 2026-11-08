package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.components.PauseMenuDialog
import com.example.ui.data.LanguageManager
import com.example.ui.progress.GameProgress
import com.example.ui.progress.LevelCompleteOverlay
import com.example.ui.progress.LevelResult
import com.example.ui.progress.LevelTally
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Paint the Monster
 * -----------------
 * The monster asks to be painted a colour, said out loud ("Paint me ಹಸಿರು!"). The child taps a
 * paint pot (or drags it onto the monster). Right colour: splash, the monster turns that colour and
 * jumps for joy. Wrong colour: it turns that colour for a moment, sneezes it off and says which
 * colour it was, then asks again. 5 paint jobs finish a level.
 */

/** A colour with its name in each language the app will support. */
internal data class KidColor(
    val id: String,
    val color: Color,
    val names: Map<String, String>
) {
    fun name(langCode: String): String = names[langCode] ?: names["en"] ?: id
}

internal val kidColors = listOf(
    KidColor("red", Color(0xFFE53935), mapOf("kn" to "ಕೆಂಪು", "hi" to "लाल", "en" to "Red")),
    KidColor("blue", Color(0xFF1E88E5), mapOf("kn" to "ನೀಲಿ", "hi" to "नीला", "en" to "Blue")),
    KidColor("yellow", Color(0xFFFDD835), mapOf("kn" to "ಹಳದಿ", "hi" to "पीला", "en" to "Yellow")),
    KidColor("green", Color(0xFF43A047), mapOf("kn" to "ಹಸಿರು", "hi" to "हरा", "en" to "Green")),
    KidColor("orange", Color(0xFFFB8C00), mapOf("kn" to "ಕಿತ್ತಳೆ", "hi" to "नारंगी", "en" to "Orange")),
    KidColor("purple", Color(0xFF8E24AA), mapOf("kn" to "ನೇರಳೆ", "hi" to "बैंगनी", "en" to "Purple")),
    KidColor("pink", Color(0xFFF06292), mapOf("kn" to "ಗುಲಾಬಿ", "hi" to "गुलाबी", "en" to "Pink")),
    KidColor("brown", Color(0xFF795548), mapOf("kn" to "ಕಂದು", "hi" to "भूरा", "en" to "Brown")),
    KidColor("black", Color(0xFF263238), mapOf("kn" to "ಕಪ್ಪು", "hi" to "काला", "en" to "Black")),
    KidColor("white", Color(0xFFFAFAFA), mapOf("kn" to "ಬಿಳಿ", "hi" to "सफ़ेद", "en" to "White"))
)

private fun colorById(id: String) = kidColors.first { it.id == id }

/** 6 levels: new colours, a mix of those, more new colours, a mix, then all ten. */
private val paintLevels: List<List<String>> = listOf(
    listOf("red", "blue", "yellow"),
    listOf("green", "orange", "purple"),
    listOf("red", "blue", "yellow", "green", "orange", "purple"),
    listOf("pink", "brown", "black", "white"),
    listOf("pink", "brown", "black", "white", "green", "purple"),
    kidColors.map { it.id }
)

private val Unpainted = Color(0xFFE3E7EE)
private val PaintBackground = Color(0xFFF4F1FF)
private const val PAINT_JOBS_PER_LEVEL = 5

/** Builds the monster's colours from one paint colour. */
private fun paintedStyle(paint: Color): MonsterStyle = MonsterStyle(
    title = "",
    body = paint,
    bodyDark = lerp(paint, Color.Black, 0.25f),
    belly = lerp(paint, Color.White, 0.55f),
    background = PaintBackground,
    gameId = "paint_monster"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaintTheMonsterScreen(firstLevel: Int = 1, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val language by LanguageManager.currentLanguage.collectAsState()
    val lang = language.code

    val levelCount = paintLevels.size
    val tally = remember { LevelTally() }
    var levelResult by remember { mutableStateOf<LevelResult?>(null) }

    var level by remember { mutableIntStateOf((firstLevel - 1).coerceIn(0, levelCount - 1)) }
    var done by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf<KidColor?>(null) }
    var pots by remember { mutableStateOf(listOf<KidColor>()) }
    var paint by remember { mutableStateOf(Unpainted) }
    var mood by remember { mutableStateOf(MonsterMood.Hungry) }
    var message by remember { mutableStateOf<String?>(null) }
    var wrongId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var draggingPot by remember { mutableStateOf(false) }
    var splashColor by remember { mutableStateOf<Color?>(null) }
    var showConfetti by remember { mutableStateOf(false) }
    var showPauseMenu by remember { mutableStateOf(false) }
    var monsterBounds by remember { mutableStateOf(Rect.Zero) }
    val splash = remember { Animatable(0f) }

    val animatedPaint by animateColorAsState(paint, tween(450), label = "paint")
    val style = paintedStyle(animatedPaint)

    // Colours this level asks for, and colours already met (used as extra pots)
    val levelColors = remember(level) { paintLevels[level].map { colorById(it) } }
    val knownColors = remember(level) {
        paintLevels.take(level + 1).flatten().distinct().map { colorById(it) }
    }

    fun speakTarget() {
        target?.let { NativeAudioPlayer.playPhonics(context, it.name(lang)) }
    }

    fun newRound(previous: KidColor?) {
        val next = levelColors.filter { it.id != previous?.id }.ifEmpty { levelColors }.random()
        val potCount = if (level == 0) 3 else 4
        val extras = (levelColors + knownColors).distinct().filter { it.id != next.id }.shuffled()
        target = next
        pots = (extras.take(potCount - 1) + next).shuffled()
        mood = MonsterMood.Hungry
        message = null
        wrongId = null
        scope.launch {
            delay(450)
            speakTarget()
        }
    }

    fun startLevel() {
        done = 0
        tally.reset()
        levelResult = null
        paint = Unpainted
        showConfetti = false
        busy = false
        newRound(null)
    }

    LaunchedEffect(level) { startLevel() }

    fun playSplash(color: Color) {
        splashColor = color
        scope.launch {
            splash.snapTo(0f)
            splash.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
            splashColor = null
        }
    }

    fun usePot(pot: KidColor) {
        val wanted = target ?: return
        if (busy) return
        busy = true
        scope.launch {
            playSplash(pot.color)
            paint = pot.color
            if (pot.id == wanted.id) {
                mood = MonsterMood.Happy
                tally.right()
                message = "Yay! I'm ${pot.name(lang)}!"
                SoundEffectsHelper.playSuccess(context)
                NativeAudioPlayer.playPhonics(context, pot.name(lang))
                delay(1300)
                done += 1
                if (done >= PAINT_JOBS_PER_LEVEL) {
                    showConfetti = true
                    SoundEffectsHelper.playGameCompletion(context)
                    val result = GameProgress.recordLevel(context, 22, level + 1, tally.mistakes, tally.streaks)
                    delay(1200)
                    showConfetti = false
                    levelResult = result
                } else {
                    paint = Unpainted
                    newRound(wanted)
                    busy = false
                }
            } else {
                // Wrong colour: show it, name it, then sneeze it off
                mood = MonsterMood.Yuck
                tally.wrong()
                wrongId = pot.id
                message = "Achoo! That's ${pot.name(lang)}!"
                SoundEffectsHelper.playOops(context)
                NativeAudioPlayer.playPhonics(context, pot.name(lang))
                delay(1400)
                paint = Unpainted
                mood = MonsterMood.Hungry
                message = null
                wrongId = null
                busy = false
                delay(300)
                speakTarget()
            }
        }
    }

    Scaffold(
        containerColor = PaintBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Paint the Monster", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text(
                            "Colours · Level ${level + 1}/$levelCount",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF6C3FC5))
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
                // Progress stars
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(PAINT_JOBS_PER_LEVEL) { i ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i < done) Color(0xFFFFB300) else Color(0x33000000),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Speech bubble
                Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 3.dp) {
                    Row(
                        modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val wanted = target
                        if (message != null || wanted == null) {
                            Text(
                                text = message ?: "",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MonsterInk
                            )
                        } else {
                            Text("Paint me", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MonsterInk)
                            // Only the colour's NAME is shown (and spoken): the child has to know the colour.
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = wanted.name(lang),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MonsterInk
                                )
                                if (lang != "en") {
                                    Text(text = wanted.name("en"), fontSize = 12.sp, color = MonsterInk.copy(alpha = 0.65f))
                                }
                            }
                        }
                        IconButton(onClick = { speakTarget() }, modifier = Modifier.testTag("paint_hear_again")) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Hear the colour again",
                                tint = Color(0xFF6C3FC5)
                            )
                        }
                    }
                }

                // The monster + paint splash
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val side = minOf(maxWidth * 0.8f, maxHeight * 0.85f, 320.dp)
                    Monster(
                        style = style,
                        mood = mood,
                        mouthWide = draggingPot,
                        growth = 0,
                        modifier = Modifier
                            .size(side)
                            .onGloballyPositioned { monsterBounds = it.boundsInRoot() }
                            .testTag("paint_monster")
                    )
                    val sc = splashColor
                    if (sc != null) {
                        Canvas(modifier = Modifier.size(side * 1.2f)) {
                            val p = splash.value
                            val center = Offset(size.width / 2f, size.height / 2f)
                            for (i in 0 until 12) {
                                val angle = (2 * PI * i / 12).toFloat()
                                val dist = size.minDimension * (0.2f + 0.3f * p)
                                drawCircle(
                                    color = sc.copy(alpha = (1f - p).coerceIn(0f, 1f)),
                                    radius = size.minDimension * (0.05f - 0.025f * p),
                                    center = Offset(center.x + cos(angle) * dist, center.y + sin(angle) * dist)
                                )
                            }
                        }
                    }
                }

                // Paint pots
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(2f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
                ) {
                    pots.forEach { pot ->
                        PaintPot(
                            pot = pot,
                            shaking = wrongId == pot.id,
                            enabled = !busy,
                            monsterBounds = { monsterBounds },
                            onDragStateChange = { draggingPot = it },
                            onUse = { usePot(pot) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap a paint pot, or drag it to the monster",
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

/** A paint pot. Tap to paint, or drag it onto the monster. */
@Composable
private fun PaintPot(
    pot: KidColor,
    shaking: Boolean,
    enabled: Boolean,
    monsterBounds: () -> Rect,
    onDragStateChange: (Boolean) -> Unit,
    onUse: () -> Unit
) {
    var dragOffset by remember(pot.id) { mutableStateOf(Offset.Zero) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnUse by rememberUpdatedState(onUse)
    val wobble by rememberInfiniteTransition(label = "pot_shake").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(70, easing = LinearEasing), RepeatMode.Reverse),
        label = "wobble"
    )
    val isLight = pot.id == "white" || pot.id == "yellow"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .graphicsLayer {
                translationX = if (shaking) wobble * 10f else 0f
                val s = if (dragOffset != Offset.Zero) 1.12f else 1f
                scaleX = s
                scaleY = s
            }
    ) {
        Box(
            modifier = Modifier
                .size(width = 70.dp, height = 74.dp)
                .onGloballyPositioned { bounds = it.boundsInRoot() }
                .clickable(enabled = enabled) { onUse() }
                .pointerInput(pot.id) {
                    detectDragGestures(
                        onDragStart = { if (currentEnabled) onDragStateChange(true) },
                        onDrag = { change, amount ->
                            if (currentEnabled) {
                                change.consume()
                                dragOffset += amount
                            }
                        },
                        onDragEnd = {
                            val inside = monsterBounds().let { it != Rect.Zero && it.inflate(24f).contains(bounds.center) }
                            onDragStateChange(false)
                            dragOffset = Offset.Zero
                            if (inside && currentEnabled) currentOnUse()
                        },
                        onDragCancel = {
                            onDragStateChange(false)
                            dragOffset = Offset.Zero
                        }
                    )
                }
                .testTag("pot_${pot.id}"),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Pot body
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 56.dp)
                    .shadow(5.dp, RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp, topStart = 10.dp, topEnd = 10.dp))
                    .background(
                        Color(0xFFB0BEC5),
                        RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp, topStart = 10.dp, topEnd = 10.dp)
                    )
            )
            // Paint surface on top of the pot
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .size(width = 66.dp, height = 26.dp)
                    .background(pot.color, RoundedCornerShape(13.dp))
                    .border(2.dp, if (isLight) Color(0x33000000) else Color.White.copy(alpha = 0.6f), RoundedCornerShape(13.dp))
            )
            // Drip
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 14.dp, y = 6.dp)
                    .size(width = 10.dp, height = 18.dp)
                    .background(pot.color, RoundedCornerShape(5.dp))
            )
        }
    }
}
