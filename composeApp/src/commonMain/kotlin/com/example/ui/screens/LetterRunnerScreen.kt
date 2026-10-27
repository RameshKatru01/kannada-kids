package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.auth.AuthState
import com.example.ui.auth.AuthViewModel
import com.example.ui.components.ConfettiEffect
import com.example.ui.components.drawTextCentered
import com.example.ui.theme.BrightCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Things that come down the road. */
private enum class RunnerKind { LETTER, ROCK, STAR, SHIELD, HEART }

private data class RunnerItem(
    val id: Int,
    val kind: RunnerKind,
    val text: String,
    val lane: Int,
    val y: Float,
    val tint: Int
)

/** A short-lived burst of sparkles where something was caught. */
private data class RunnerBurst(val x: Float, val y: Float, val color: Color, val age: Float = 0f)

/** "+10", "Oops!" and friends, floating up from the runner. */
private data class RunnerFloater(val text: String, val x: Float, val y: Float, val color: Color, val age: Float = 0f)

private enum class RunnerPhase { COUNTDOWN, RUNNING, LEVEL_DONE, GAME_OVER, WON }

private val BubbleColors = listOf(
    Color(0xFFFF6B6B), Color(0xFFFFA94D), Color(0xFF4DABF7),
    Color(0xFF9775FA), Color(0xFF20C997), Color(0xFFF06595)
)
private val RunnerNavy = Color(0xFF0B3A66)
private val SideDecor = listOf("🌳", "🌷", "🌼", "🌲", "🍄", "🌻", "🦋", "🌳")
private val Rocks = listOf("🪨", "🌵", "🚧")

private const val LANES = 3
private const val RUNNER_Y = 0.78f
private const val HIT_RANGE = 0.06f
/** How far the road moves between rows (in screen heights). */
private const val ROW_GAP = 0.40f
/** Where new rows appear (game units); they are drawn at the horizon. */
private const val SPAWN_Y = -0.08f
/** The far end of the path, as a fraction of the screen height (sky above it). */
private const val HORIZON = 0.14f
/** How long the runner cheers after catching the right letter (seconds). */
private const val CHEER_TIME = 0.55f
/** Top running speed (screen heights per second); at this speed the cape flies flat out. */
private const val MAX_SPEED = 0.48f

class BGMPlayer {
    private var handle: Any? = null

    fun play() {
        if (handle != null) return
        handle = PlatformAudio.startLoop(melody, SAMPLE_RATE)
    }

    fun stop() {
        PlatformAudio.stopLoop(handle)
        handle = null
    }

    companion object {
        private const val SAMPLE_RATE = 44100

        /** One loop of the cheerful background tune (same notes as the original Android version). */
        private val melody: ShortArray by lazy {
            val notes = listOf(261.63, 329.63, 392.00, 523.25, 392.00, 329.63)
            val samplesPerNote = SAMPLE_RATE / 4
            val buffer = ShortArray(samplesPerNote * notes.size)
            var t = 0.0
            var i = 0
            for (freq in notes) {
                for (sampleCount in 0 until samplesPerNote) {
                    val envelope = 1.0 - (sampleCount.toDouble() / samplesPerNote)
                    val sample = sin(2.0 * PI * freq * t) * envelope * 6000
                    buffer[i++] = sample.toInt().toShort()
                    t += 1.0 / SAMPLE_RATE
                }
            }
            buffer
        }
    }
}

/**
 * Letter Runner: the runner races down a three-lane path. Each row brings the letter to find
 * together with look-alike decoys (and rocks, stars, shields and hearts). Catch the right letter
 * to hear it and build a combo; a wrong letter or a rock costs a heart. Letters come in alphabet
 * order, one level per Letter Box tab. Tap the left or right side (or swipe) to change lanes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterRunnerScreen(onBack: () -> Unit, authViewModel: AuthViewModel = viewModel { AuthViewModel() }) {
    val bgm = remember { BGMPlayer() }
    DisposableEffect(Unit) {
        bgm.play()
        onDispose { bgm.stop() }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authState by authViewModel.authState.collectAsState()
    val userEmail = (authState as? AuthState.Authenticated)?.email ?: "guest"
    val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)
    val prefs = remember { getSharedPreferences("GamePrefs") }
    var highScore by remember { mutableIntStateOf(prefs.getInt("${userEmail}_runner_high_score", 0)) }

    val runnerLevels = remember {
        listOf(
            listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ", "ಊ", "ಋ", "ಎ", "ಏ", "ಐ", "ಒ", "ಓ", "ಔ", "ಅಂ", "ಅಃ"), // Vowels
            listOf("ಕ", "ಖ", "ಗ", "ಘ", "ಙ", "ಚ", "ಛ", "ಜ", "ಝ", "ಞ", "ಟ", "ಠ", "ಡ", "ಢ", "ಣ", "ತ", "ಥ", "ದ", "ಧ", "ನ", "ಪ", "ಫ", "ಬ", "ಭ", "ಮ", "ಯ", "ರ", "ಲ", "ವ", "ಶ", "ಷ", "ಸ", "ಹ", "ಳ"), // Consonants
            listOf("ಕ", "ಕಾ", "ಕಿ", "ಕೀ", "ಕು", "ಕೂ", "ಕೃ", "ಕೆ", "ಕೇ", "ಕೈ", "ಕೊ", "ಕೋ", "ಕೌ", "ಕಂ", "ಕಃ"), // Guninthakshari
            listOf("೧", "೨", "೩", "೪", "೫", "೬", "೭", "೮", "೯", "೧೦") // Numbers
        )
    }
    val levelNames = listOf("Vowels", "Consonants", "Ka-kaa", "Numbers")

    var levelIndex by remember { mutableIntStateOf(0) }
    val sequence = runnerLevels[levelIndex]
    var targetIndex by remember { mutableIntStateOf(0) }
    val target = sequence.getOrNull(targetIndex)

    var phase by remember { mutableStateOf(RunnerPhase.COUNTDOWN) }
    var countdown by remember { mutableIntStateOf(3) }
    var showSurpriseSticker by remember { mutableStateOf(false) }

    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var combo by remember { mutableIntStateOf(0) }

    // Lane the runner is heading to, and where it is drawn (slides smoothly between lanes).
    var runnerLane by remember { mutableIntStateOf(1) }
    var runnerX by remember { mutableFloatStateOf(1f) }

    var items by remember { mutableStateOf(listOf<RunnerItem>()) }
    var bursts by remember { mutableStateOf(listOf<RunnerBurst>()) }
    var floaters by remember { mutableStateOf(listOf<RunnerFloater>()) }
    var distance by remember { mutableFloatStateOf(0f) }
    var shake by remember { mutableFloatStateOf(0f) }
    var hurtTime by remember { mutableFloatStateOf(0f) }
    var shieldTime by remember { mutableFloatStateOf(0f) }
    var cheerTime by remember { mutableFloatStateOf(0f) }
    // How far the cape is up in the air (0 = hanging down, 1 = flying flat out); follows the speed.
    var capeLift by remember { mutableFloatStateOf(0f) }

    fun speak(text: String?) {
        if (text != null) NativeAudioPlayer.playPhonics(context, text)
    }

    fun moveLane(delta: Int) {
        if (phase != RunnerPhase.RUNNING) return
        val next = (runnerLane + delta).coerceIn(0, LANES - 1)
        if (next != runnerLane) {
            runnerLane = next
            SoundEffectsHelper.playClick()
        }
    }

    fun resetRun() {
        targetIndex = 0
        runnerLane = 1
        runnerX = 1f
        items = emptyList()
        bursts = emptyList()
        floaters = emptyList()
        distance = 0f
        shake = 0f
        hurtTime = 0f
        shieldTime = 0f
        cheerTime = 0f
        capeLift = 0f
        combo = 0
        phase = RunnerPhase.COUNTDOWN
    }

    fun nextLevel() {
        showSurpriseSticker = false
        when {
            levelIndex >= runnerLevels.size - 1 -> onBack()
            isGuest -> {
                Toast.makeText(context, "🔒 Level 1 complete! Sign in with Gmail to unlock all runner levels!", Toast.LENGTH_LONG).show()
                onBack()
            }
            else -> {
                levelIndex++
                resetRun()
            }
        }
    }

    // Big friendly 3-2-1-Go before each level; the first letter is spoken on "Go".
    LaunchedEffect(phase, levelIndex) {
        if (phase != RunnerPhase.COUNTDOWN) return@LaunchedEffect
        for (n in 3 downTo 1) {
            countdown = n
            SoundEffectsHelper.playPop()
            delay(650)
        }
        countdown = 0
        speak(runnerLevels[levelIndex].firstOrNull())
        delay(450)
        phase = RunnerPhase.RUNNING
    }

    // The game loop.
    LaunchedEffect(phase, levelIndex) {
        if (phase != RunnerPhase.RUNNING) return@LaunchedEffect
        var nextId = 0
        var sinceRow = ROW_GAP - 0.05f
        var last = withFrameNanos { it }

        /** Lays out one row: the target, decoys, and sometimes a rock or a bonus. */
        fun spawnRow() {
            val want = sequence.getOrNull(targetIndex) ?: return
            val lanes = (0 until LANES).shuffled()
            val decoys = sequence.filter { it != want }.shuffled()
            // Gentle start: the very first letters of the first level come with just one decoy.
            val easy = levelIndex == 0 && targetIndex < 3
            val row = mutableListOf(RunnerItem(nextId++, RunnerKind.LETTER, want, lanes[0], SPAWN_Y, Random.nextInt(BubbleColors.size)))
            decoys.getOrNull(0)?.let { row += RunnerItem(nextId++, RunnerKind.LETTER, it, lanes[1], SPAWN_Y, Random.nextInt(BubbleColors.size)) }
            val roll = Random.nextFloat()
            val third = when {
                easy -> if (roll < 0.5f) RunnerKind.STAR else null
                roll < 0.40f -> RunnerKind.LETTER
                roll < 0.65f -> RunnerKind.ROCK
                roll < 0.82f -> RunnerKind.STAR
                roll < 0.90f && shieldTime <= 0f -> RunnerKind.SHIELD
                roll < 0.96f && lives < 3 -> RunnerKind.HEART
                else -> null
            }
            when (third) {
                RunnerKind.LETTER -> decoys.getOrNull(1)?.let { row += RunnerItem(nextId++, RunnerKind.LETTER, it, lanes[2], SPAWN_Y, Random.nextInt(BubbleColors.size)) }
                RunnerKind.ROCK -> row += RunnerItem(nextId++, RunnerKind.ROCK, Rocks.random(), lanes[2], SPAWN_Y, 0)
                RunnerKind.STAR -> row += RunnerItem(nextId++, RunnerKind.STAR, "⭐", lanes[2], SPAWN_Y, 0)
                RunnerKind.SHIELD -> row += RunnerItem(nextId++, RunnerKind.SHIELD, "🛡️", lanes[2], SPAWN_Y, 0)
                RunnerKind.HEART -> row += RunnerItem(nextId++, RunnerKind.HEART, "💖", lanes[2], SPAWN_Y, 0)
                null -> {}
            }
            items = items + row
        }

        fun loseHeart(laneX: Float, label: String) {
            combo = 0
            SoundEffectsHelper.playOops()
            if (shieldTime > 0f) {
                // The shield takes the knock instead.
                shieldTime = 0f
                floaters = floaters + RunnerFloater("🛡️ Saved!", laneX, RUNNER_Y - 0.06f, Color(0xFF1C7ED6))
                return
            }
            lives--
            shake = 1f
            hurtTime = 1.2f
            floaters = floaters + RunnerFloater(label, laneX, RUNNER_Y - 0.06f, Color(0xFFE03131))
            if (lives <= 0) phase = RunnerPhase.GAME_OVER
        }

        while (isActive && phase == RunnerPhase.RUNNING) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
            last = now

            // A little faster with every letter, and with every level.
            val speed = (0.26f + levelIndex * 0.03f + targetIndex * 0.008f).coerceAtMost(MAX_SPEED)
            val step = speed * dt
            distance += step
            sinceRow += step
            if (sinceRow >= ROW_GAP) {
                sinceRow = 0f
                spawnRow()
            }

            runnerX += (runnerLane - runnerX) * (dt * 16f).coerceAtMost(1f)
            shake = (shake - dt * 2.5f).coerceAtLeast(0f)
            hurtTime = (hurtTime - dt).coerceAtLeast(0f)
            shieldTime = (shieldTime - dt).coerceAtLeast(0f)
            cheerTime = (cheerTime - dt).coerceAtLeast(0f)
            // The cape rises (or sinks) smoothly towards where the current speed puts it.
            capeLift += ((speed / MAX_SPEED).coerceIn(0f, 1f) - capeLift) * (dt * 3f).coerceAtMost(1f)

            val moved = items.map { it.copy(y = it.y + step) }
            val keep = mutableListOf<RunnerItem>()
            var caughtTarget = false
            for (item in moved) {
                val hit = item.lane == runnerLane && abs(item.y - RUNNER_Y) < HIT_RANGE
                if (!hit) {
                    if (item.y < 1.15f) keep += item
                    continue
                }
                when (item.kind) {
                    RunnerKind.LETTER -> if (item.text == sequence.getOrNull(targetIndex) && !caughtTarget) {
                        caughtTarget = true
                        combo++
                        val points = 10 * combo.coerceAtMost(5)
                        score += points
                        SoundEffectsHelper.playSuccess()
                        speak(item.text)
                        bursts = bursts + RunnerBurst(item.lane.toFloat(), RUNNER_Y, BubbleColors[item.tint])
                        floaters = floaters + RunnerFloater(
                            if (combo >= 2) "+$points  x$combo!" else "+$points",
                            item.lane.toFloat(), RUNNER_Y - 0.06f, Color(0xFF2B8A3E)
                        )
                        targetIndex++
                        cheerTime = CHEER_TIME
                    } else if (hurtTime <= 0f) {
                        loseHeart(item.lane.toFloat(), "Oops!")
                        bursts = bursts + RunnerBurst(item.lane.toFloat(), RUNNER_Y, Color.Gray)
                        // Say the letter we're looking for, so the child learns which one it was.
                        val want = sequence.getOrNull(targetIndex)
                        scope.launch { delay(700); speak(want) }
                    }
                    RunnerKind.ROCK -> {
                        if (hurtTime <= 0f) loseHeart(item.lane.toFloat(), "Bump!") else keep += item
                    }
                    RunnerKind.STAR -> {
                        score += 5
                        SoundEffectsHelper.playSparkle()
                        bursts = bursts + RunnerBurst(item.lane.toFloat(), RUNNER_Y, Color(0xFFFFD43B))
                        floaters = floaters + RunnerFloater("+5 ⭐", item.lane.toFloat(), RUNNER_Y - 0.06f, Color(0xFFE67700))
                    }
                    RunnerKind.SHIELD -> {
                        shieldTime = 8f
                        SoundEffectsHelper.playSparkle()
                        floaters = floaters + RunnerFloater("Shield!", item.lane.toFloat(), RUNNER_Y - 0.06f, Color(0xFF1C7ED6))
                    }
                    RunnerKind.HEART -> {
                        lives = (lives + 1).coerceAtMost(3)
                        SoundEffectsHelper.playSparkle()
                        floaters = floaters + RunnerFloater("+1 💖", item.lane.toFloat(), RUNNER_Y - 0.06f, Color(0xFFD6336C))
                    }
                }
            }
            items = if (caughtTarget) {
                // The letters still coming were for the old target: pop them, and bring the next row soon.
                keep.filter { it.kind != RunnerKind.LETTER || it.y > RUNNER_Y }.also {
                    keep.filter { k -> k.kind == RunnerKind.LETTER && k.y <= RUNNER_Y }.forEach { k ->
                        bursts = bursts + RunnerBurst(k.lane.toFloat(), k.y, Color.White)
                    }
                    sinceRow = ROW_GAP - 0.10f
                }
            } else keep

            bursts = bursts.map { it.copy(age = it.age + dt) }.filter { it.age < 0.6f }
            floaters = floaters.map { it.copy(age = it.age + dt) }.filter { it.age < 0.9f }

            if (score > highScore) {
                highScore = score
                prefs.edit().putInt("${userEmail}_runner_high_score", highScore).apply()
            }

            if (targetIndex >= sequence.size) {
                SoundEffectsHelper.playGameCompletion(context)
                phase = if (levelIndex < runnerLevels.size - 1) RunnerPhase.LEVEL_DONE else RunnerPhase.WON
                showSurpriseSticker = true
            }
        }
    }

    // The "find" card bounces when the letter changes.
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(targetIndex, levelIndex) {
        pulse.snapTo(1.35f)
        pulse.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 300f))
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF8CD47E))
                .pointerInput(Unit) {
                    // Tap the left or right half to step that way; or swipe (the lane changes mid-swipe).
                    val swipe = 28.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var swiped = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val dx = change.position.x - down.position.x
                            if (!swiped && abs(dx) > swipe) {
                                swiped = true
                                moveLane(if (dx > 0) 1 else -1)
                            }
                        }
                        if (!swiped) moveLane(if (down.position.x < size.width / 2f) -1 else 1)
                    }
                }
                .testTag("runner_play_area")
        ) {
            val textMeasurer = rememberTextMeasurer()
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val horizon = HORIZON * h
                val cx0 = w / 2f
                // Lane width right at the bottom of the screen; it shrinks towards the horizon.
                val laneW = w * 0.80f / LANES
                val roadHalf = laneW * LANES / 2f

                /** Where a road position ([y] in game units) is drawn: slow far away, faster up close. */
                fun sy(y: Float): Float {
                    val t = ((y - SPAWN_Y) / (RUNNER_Y - SPAWN_Y)).coerceAtLeast(0f)
                    return horizon + (RUNNER_Y * h - horizon) * t.pow(1.6f)
                }
                /** How big things are at screen height [screenY]: small at the horizon, full size at the bottom. */
                fun scaleAt(screenY: Float) = (0.18f + 0.82f * ((screenY - horizon) / (h - horizon))).coerceAtLeast(0.18f)
                fun laneX(lane: Float, screenY: Float) = cx0 + (lane - 1f) * laneW * scaleAt(screenY)

                val shakeX = sin(distance * 900f) * shake * w * 0.025f

                translate(left = shakeX) {
                    // Sky, clouds and hills at the far end of the path.
                    drawRect(Color(0xFFBDE7FF), Offset(-40f, 0f), Size(w + 80f, horizon + 4f))
                    for (c in 0 until 3) {
                        val cxCloud = ((c * 0.38f + distance * 0.05f) % 1.3f - 0.15f) * w
                        val cyCloud = horizon * (0.35f + 0.2f * c)
                        val r = w * 0.035f
                        drawCircle(Color.White, r, Offset(cxCloud, cyCloud))
                        drawCircle(Color.White, r * 1.3f, Offset(cxCloud + r * 1.2f, cyCloud - r * 0.3f))
                        drawCircle(Color.White, r, Offset(cxCloud + r * 2.4f, cyCloud))
                    }
                    drawCircle(Color(0xFF6FBF5F), w * 0.32f, Offset(w * 0.12f, horizon + w * 0.27f))
                    drawCircle(Color(0xFF79C96A), w * 0.38f, Offset(w * 0.9f, horizon + w * 0.33f))
                    drawRect(Color(0xFF8CD47E), Offset(-40f, horizon), Size(w + 80f, h - horizon + 40f))

                    // Grass stripes that rush past.
                    val band = 0.12f
                    var gy = SPAWN_Y - band * 2 + (distance % (band * 2))
                    while (gy < 1.3f) {
                        val top = sy(gy)
                        val bottom = sy(gy + band)
                        if (bottom > horizon) drawRect(Color(0xFF7CC96D), Offset(-40f, top.coerceAtLeast(horizon)), Size(w + 80f, bottom - top.coerceAtLeast(horizon)))
                        gy += band * 2
                    }

                    // The sandy path, narrowing to the horizon, with a darker edge.
                    fun road(extra: Float, color: Color) {
                        val topY = horizon
                        val bottomY = h + 20f
                        val path = Path().apply {
                            moveTo(cx0 - (roadHalf + extra) * scaleAt(topY), topY)
                            lineTo(cx0 + (roadHalf + extra) * scaleAt(topY), topY)
                            lineTo(cx0 + (roadHalf + extra) * scaleAt(bottomY), bottomY)
                            lineTo(cx0 - (roadHalf + extra) * scaleAt(bottomY), bottomY)
                            close()
                        }
                        drawPath(path, color)
                    }
                    road(14f, Color(0xFFE2B66E))
                    road(0f, Color(0xFFF6DDA8))

                    // Dashed lane lines.
                    val dash = 0.06f
                    for (i in 1 until LANES) {
                        val offset = i - 0.5f - 1f
                        var dy = SPAWN_Y - dash * 2 + (distance % (dash * 2))
                        while (dy < 1.3f) {
                            val y1 = sy(dy)
                            val y2 = sy(dy + dash)
                            drawLine(
                                Color.White.copy(alpha = 0.9f),
                                Offset(cx0 + offset * laneW * scaleAt(y1), y1),
                                Offset(cx0 + offset * laneW * scaleAt(y2), y2),
                                strokeWidth = 9f * scaleAt(y2)
                            )
                            dy += dash * 2
                        }
                    }

                    // Trees and flowers on both sides, growing as they come closer.
                    for (i in SideDecor.indices) {
                        val wy = ((i * 0.19f + distance) % 1.5f) + SPAWN_Y
                        val y = sy(wy)
                        val s = scaleAt(y)
                        val side = if (i % 2 == 0) -1f else 1f
                        drawTextCentered(
                            textMeasurer, SideDecor[i],
                            cx0 + side * (roadHalf + laneW * 0.45f) * s, y, w * 0.11f * s
                        )
                    }

                    // Letters, rocks and bonuses (farther ones first, so nearer ones draw on top).
                    items.sortedBy { it.y }.forEach { item ->
                        val cy = sy(item.y)
                        val s = scaleAt(cy)
                        val cx = laneX(item.lane.toFloat(), cy)
                        // Letter bubbles nearly fill their lane (0.5 would touch the next lane); bonuses stay a bit smaller.
                        val r = laneW * (if (item.kind == RunnerKind.LETTER) 0.46f else 0.38f) * s
                        drawOval(Color(0x33000000), Offset(cx - r * 0.8f, cy + r * 0.85f), Size(r * 1.6f, r * 0.4f))
                        when (item.kind) {
                            RunnerKind.LETTER -> {
                                drawCircle(Color.White, r, Offset(cx, cy))
                                drawCircle(BubbleColors[item.tint], r * 0.86f, Offset(cx, cy))
                                drawCircle(Color.White.copy(alpha = 0.35f), r * 0.28f, Offset(cx - r * 0.35f, cy - r * 0.4f))
                                val textSize = if (item.text.length > 1) r * 0.95f else r * 1.15f
                                drawTextCentered(textMeasurer, item.text, cx, cy + textSize * 0.36f, textSize, Color.White, bold = true)
                            }
                            else -> {
                                // Bonuses bob gently so they look alive.
                                val bob = if (item.kind == RunnerKind.ROCK) 0f else sin(distance * 40f + item.id) * r * 0.12f
                                val textSize = r * 1.4f
                                drawTextCentered(textMeasurer, item.text, cx, cy + textSize * 0.36f + bob, textSize)
                            }
                        }
                    }

                    // The runner, seen from behind, running up the path towards the letters.
                    val ry = RUNNER_Y * h
                    val rs = scaleAt(ry)
                    val rx = cx0 + (runnerX - 1f) * laneW * rs
                    val kidSize = laneW * 0.95f * rs
                    if (shieldTime > 0f) {
                        val glow = 0.22f + 0.12f * sin(distance * 60f)
                        drawCircle(Color(0xFF74C0FC).copy(alpha = glow), kidSize * 0.55f, Offset(rx, ry - kidSize * 0.1f))
                        drawCircle(Color(0xFF1C7ED6), kidSize * 0.55f, Offset(rx, ry - kidSize * 0.1f), style = Stroke(5f))
                    }
                    val blinkHidden = hurtTime > 0f && ((hurtTime * 10).toInt() % 2 == 0)
                    if (!blinkHidden) {
                        drawRunnerKid(
                            groundX = rx,
                            groundY = ry + kidSize * 0.38f,
                            s = kidSize,
                            stride = distance * 70f,
                            running = phase == RunnerPhase.RUNNING,
                            cheer = (cheerTime / CHEER_TIME).coerceIn(0f, 1f),
                            dizzy = hurtTime > 0f,
                            capeLift = if (phase == RunnerPhase.RUNNING) capeLift else 0f,
                            lean = (runnerLane - runnerX).coerceIn(-1f, 1f) * 16f
                        )
                    }

                    // Sparkle bursts.
                    bursts.forEach { b ->
                        val t = b.age / 0.6f
                        val alpha = (1f - t).coerceIn(0f, 1f)
                        val cy = sy(b.y)
                        val s = scaleAt(cy)
                        val cx = laneX(b.x, cy)
                        for (k in 0 until 10) {
                            val angle = k * (2 * PI / 10).toFloat()
                            val dist = (laneW * 0.25f + t * laneW * 0.6f) * s
                            drawCircle(
                                b.color.copy(alpha = alpha),
                                ((1f - t) * laneW * 0.07f + 3f) * s,
                                Offset(cx + cos(angle) * dist, cy + sin(angle) * dist)
                            )
                        }
                    }

                    // Floating "+10" labels.
                    floaters.forEach { f ->
                        val t = f.age / 0.9f
                        val fy = sy(f.y) - t * 0.08f * h
                        drawTextCentered(
                            textMeasurer, f.text, laneX(f.x, fy), fy,
                            laneW * 0.24f, f.color.copy(alpha = (1f - t).coerceIn(0f, 1f)), bold = true
                        )
                    }
                }
            }

            // ---- Heads-up display ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.White, CircleShape).size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RunnerNavy)
                }

                // What to find: tap it to hear the letter again.
                Column(
                    modifier = Modifier
                        .scale(pulse.value)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .clickable { speak(target) }
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                        .testTag("runner_target"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Find 🔊", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = RunnerNavy.copy(alpha = 0.7f))
                    Text(target ?: "🎉", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = RunnerNavy)
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("⭐ $score", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = RunnerNavy)
                    Text("❤️".repeat(lives.coerceAtLeast(0)) + "🤍".repeat((3 - lives).coerceIn(0, 3)), fontSize = 13.sp)
                    if (combo >= 2) {
                        Text("🔥 x$combo", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE8590C))
                    }
                }
            }

            // Level progress: how many letters of this level are found.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Level ${levelIndex + 1} · ${levelNames[levelIndex]}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = RunnerNavy)
                    Text("${targetIndex.coerceAtMost(sequence.size)} / ${sequence.size}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = RunnerNavy)
                }
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFE3EEF8))) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(targetIndex.coerceAtMost(sequence.size).toFloat() / sequence.size)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF2EC4B6))
                    )
                }
            }

            if (phase == RunnerPhase.COUNTDOWN) {
                Box(Modifier.fillMaxSize().background(Color(0x55000000)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (countdown > 0) "$countdown" else "Go!",
                            fontSize = 96.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
                        )
                        Text(
                            "Catch ${sequence.first()} … in order!",
                            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
                        )
                        Text(
                            "Tap left or right to move",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            if (phase == RunnerPhase.LEVEL_DONE || phase == RunnerPhase.WON) {
                ConfettiEffect()
            }

            if (phase == RunnerPhase.GAME_OVER) {
                RunnerOverlay(
                    title = "Oh no! 🙈",
                    subtitle = "You found ${targetIndex} of ${sequence.size} · ⭐ $score · Best $highScore",
                    button = "Try again",
                    onClick = {
                        score = 0
                        lives = 3
                        resetRun()
                    }
                )
            }

            if (phase == RunnerPhase.LEVEL_DONE && !showSurpriseSticker) {
                RunnerOverlay(
                    title = "Level complete! 🎉",
                    subtitle = "All the ${levelNames[levelIndex].lowercase()} found · ⭐ $score",
                    button = if (isGuest) "Finish (Guest Preview)" else "Next level",
                    onClick = { nextLevel() }
                )
            }

            if (phase == RunnerPhase.WON && !showSurpriseSticker) {
                RunnerOverlay(
                    title = "You won! 🏆",
                    subtitle = "Every level done · ⭐ $score · Best $highScore",
                    button = "Awesome!",
                    onClick = onBack
                )
            }

            if (showSurpriseSticker) {
                com.example.ui.components.SurpriseStickerDialog(
                    userEmail = userEmail,
                    levelName = "Runner Level ${levelIndex + 1}",
                    seedModifier = levelIndex,
                    onClose = { showSurpriseSticker = false },
                    onClaimOrNext = { nextLevel() }
                )
            }
        }
    }
}

private val KidSkin = Color(0xFFE8B07A)
private val KidHair = Color(0xFF3B2414)
private val KidShirt = Color(0xFF3B82F6)
private val KidShorts = Color(0xFF1E3A8A)
private val KidCape = Color(0xFFE63946)
private val KidCapeDark = Color(0xFFB5172A)
private val KidStar = Color(0xFFFFD43B)

/**
 * A cartoon superhero kid seen from behind (so it runs up the path, towards the letters), standing
 * on ([groundX], [groundY]) and about [s] tall, with a cape that flutters as it runs. Legs and
 * arms swing with [stride]; [capeLift] (0..1, from the speed) lifts the cape from hanging down to
 * streaming out behind; [cheer] (0..1)
 * is a hop with both arms up after a right letter; [dizzy] circles stars round the head after a
 * bump; [lean] (degrees) tilts the kid into a lane change.
 */
private fun DrawScope.drawRunnerKid(
    groundX: Float,
    groundY: Float,
    s: Float,
    stride: Float,
    running: Boolean,
    cheer: Float,
    dizzy: Boolean,
    capeLift: Float,
    lean: Float
) {
    val swing = if (running) sin(stride) else 0f
    // Hop up and down with every step; a big hop when cheering.
    val hop = if (cheer > 0f) sin(PI.toFloat() * (1f - cheer)) * s * 0.28f else 0f
    val bounce = (if (running) abs(sin(stride)) * s * 0.05f else 0f) + hop

    // Shadow stays on the ground and shrinks while the kid is in the air.
    val shadowW = s * 0.42f * (1f - hop / (s * 0.6f))
    drawOval(Color(0x44000000), Offset(groundX - shadowW / 2f, groundY - s * 0.04f), Size(shadowW, s * 0.1f))

    // Dust puffs kicked back down the path.
    if (running) {
        for (k in 0 until 3) {
            val t = ((stride / (2f * PI.toFloat())) + k / 3f) % 1f
            val side = if (k % 2 == 0) -1f else 1f
            drawCircle(
                Color(0xFFD9B98A).copy(alpha = 0.55f * (1f - t)),
                s * (0.035f + 0.06f * t),
                Offset(groundX + side * s * 0.1f, groundY + t * s * 0.35f)
            )
        }
    }

    rotate(lean, pivot = Offset(groundX, groundY)) {
        val hipY = groundY - s * 0.36f - bounce
        val shoulderY = hipY - s * 0.30f
        val headR = s * 0.15f
        val headY = shoulderY - headR * 0.85f

        // Legs: one foot pushes down while the other lifts (we see its sole).
        for (side in listOf(-1f, 1f)) {
            val legSwing = swing * side
            val lift = max(0f, legSwing) * s * 0.13f
            val footX = groundX + side * s * 0.09f
            val footY = groundY - s * 0.05f - lift - bounce * 0.6f
            drawLine(KidSkin, Offset(groundX + side * s * 0.08f, hipY), Offset(footX, footY - s * 0.03f), strokeWidth = s * 0.09f, cap = StrokeCap.Round)
            val lifted = legSwing > 0.2f
            drawOval(
                if (lifted) Color(0xFFDDE3EA) else Color.White,
                Offset(footX - s * 0.07f, footY - s * 0.04f),
                Size(s * 0.14f, s * (if (lifted) 0.09f else 0.07f))
            )
            drawLine(Color(0xFFE03131), Offset(footX - s * 0.05f, footY), Offset(footX + s * 0.05f, footY), strokeWidth = s * 0.018f)
        }

        // Shorts and shirt.
        drawRoundRect(KidShorts, Offset(groundX - s * 0.16f, hipY - s * 0.04f), Size(s * 0.32f, s * 0.13f), CornerRadius(s * 0.04f))
        drawRoundRect(KidShirt, Offset(groundX - s * 0.18f, shoulderY - s * 0.02f), Size(s * 0.36f, hipY - shoulderY + s * 0.02f), CornerRadius(s * 0.09f))

        // The cape. Seen from behind, a cape in the air streams back towards us: the faster the kid
        // runs, the longer and wider it gets and the harder it flaps. Standing or slow, it hangs
        // straight down the back. A cheer hop makes it billow out too.
        val up = (cheer * 3f).coerceAtMost(1f)
        val air = max(capeLift, up * 0.8f)
        val flare = s * (0.16f + 0.17f * air)
        val capeBottom = hipY + s * (0.02f + 0.22f * air)
        val flutter = s * (0.004f + 0.045f * air)
        val sway = sin(stride * 0.5f) * s * 0.03f * air
        val cape = Path().apply {
            moveTo(groundX - s * 0.13f, shoulderY)
            lineTo(groundX + s * 0.13f, shoulderY)
            lineTo(groundX + flare + sway, capeBottom + sin(stride * 2f) * flutter)
            // Wavy hem: three scallops that ripple faster and bigger with speed.
            val hem = 3
            for (i in hem - 1 downTo 0) {
                val x0 = groundX - flare + sway + (2f * flare) * i / hem
                val xMid = groundX - flare + sway + (2f * flare) * (i + 0.5f) / hem
                quadraticTo(
                    xMid, capeBottom + s * (0.02f + 0.04f * air) + sin(stride * 2f + i * 1.7f) * flutter,
                    x0, capeBottom + sin(stride * 2f + (i + 1) * 1.3f) * flutter
                )
            }
            close()
        }
        drawPath(cape, KidCape)
        // A fold down the middle and a collar, so it reads as cloth.
        drawLine(KidCapeDark.copy(alpha = 0.5f), Offset(groundX, shoulderY + s * 0.05f), Offset(groundX + sway, capeBottom), strokeWidth = s * 0.015f)
        drawRoundRect(KidCapeDark, Offset(groundX - s * 0.15f, shoulderY - s * 0.025f), Size(s * 0.30f, s * 0.05f), CornerRadius(s * 0.025f))
        drawStar(Offset(groundX, shoulderY + s * 0.15f), s * 0.07f, KidStar)

        // Arms swing opposite to the legs, or go up in a cheer (with a little wave).
        for (side in listOf(-1f, 1f)) {
            val shoulder = Offset(groundX + side * s * 0.16f, shoulderY + s * 0.05f)
            val runHand = Offset(shoulder.x + side * s * 0.07f, shoulderY + s * 0.24f + swing * side * s * 0.08f)
            val wave = sin(stride * 1.5f) * s * 0.03f
            val cheerHand = Offset(shoulder.x + side * s * 0.13f + wave, shoulderY - s * 0.24f)
            // Arms shoot up straight away and come down smoothly at the end of the cheer.
            val hand = Offset(runHand.x + (cheerHand.x - runHand.x) * up, runHand.y + (cheerHand.y - runHand.y) * up)
            drawLine(KidShirt, shoulder, Offset((shoulder.x + hand.x) / 2f, (shoulder.y + hand.y) / 2f), strokeWidth = s * 0.1f, cap = StrokeCap.Round)
            drawLine(KidSkin, Offset((shoulder.x + hand.x) / 2f, (shoulder.y + hand.y) / 2f), hand, strokeWidth = s * 0.08f, cap = StrokeCap.Round)
            drawCircle(KidSkin, s * 0.05f, hand)
        }

        // Head from behind: ears, then hair.
        drawCircle(KidSkin, headR * 0.28f, Offset(groundX - headR * 0.95f, headY + headR * 0.1f))
        drawCircle(KidSkin, headR * 0.28f, Offset(groundX + headR * 0.95f, headY + headR * 0.1f))
        drawCircle(KidHair, headR, Offset(groundX, headY))

        // Dizzy stars after a bump.
        if (dizzy) {
            for (k in 0 until 3) {
                val a = stride * 0.6f + k * (2f * PI.toFloat() / 3f)
                drawCircle(
                    Color(0xFFFFD43B),
                    s * 0.035f,
                    Offset(groundX + cos(a) * headR * 1.4f, headY - headR * 1.1f + sin(a) * headR * 0.4f)
                )
            }
        }
    }
}

/** A five-pointed star (the emblem on the cape). */
private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val a = -PI.toFloat() / 2f + i * PI.toFloat() / 5f
        val x = center.x + cos(a) * r
        val y = center.y + sin(a) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

@Composable
private fun RunnerOverlay(title: String, subtitle: String, button: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xAA000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = RunnerNavy)
            Text(subtitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RunnerNavy.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrightCyan),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(56.dp).testTag("runner_overlay_button")
            ) {
                Text(button, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
