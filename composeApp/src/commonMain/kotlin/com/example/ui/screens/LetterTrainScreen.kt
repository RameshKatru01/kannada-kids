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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.KannadaLetter
import com.example.data.LetterData
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.components.PauseMenuDialog
import com.example.ui.data.ScoreManager
import com.example.ui.data.UserManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Letter Train
 * ------------
 * A train pulls in with its carriages carrying letters in alphabet order, but some carriages are
 * empty. The child puts the missing letters back (tap a letter: it goes into the first empty
 * carriage if it belongs there; or drag it onto any carriage). When the train is complete it
 * reads the letters out loud, whistles and drives off. Every 3 trains earns a sticker.
 */

private data class TrainMode(val key: String, val label: String, val accent: Color)

private val trainModes = listOf(
    TrainMode("vowels", "Vowels", Color(0xFF2EC4B6)),
    TrainMode("consonants", "Consonants", Color(0xFF6C63FF)),
    TrainMode("guninthakshari", "ಕ ಕಾ ಕಿ", Color(0xFFFF6B6B)),
    TrainMode("numbers", "Numbers", Color(0xFFFF9F1C))
)

private val CarriageColors = listOf(
    Color(0xFFFF6B6B), Color(0xFFFFC93C), Color(0xFF2EC4B6), Color(0xFF6C63FF), Color(0xFFFF9F1C)
)
private val TrainInk = Color(0xFF1E2A47)
private val Sky = Color(0xFFE6F4FF)
private val Grass = Color(0xFF9BE09B)

private const val CARRIAGES = 5
private const val TRAINS_PER_STICKER = 3

private fun lettersFor(modeKey: String): List<KannadaLetter> = when (modeKey) {
    "vowels" -> LetterData.vowels
    "consonants" -> LetterData.consonants
    "guninthakshari" -> LetterData.kaGuninthakshari
    else -> LetterData.numbers
}

/** Train number [index] carries [CARRIAGES] letters in order; windows overlap a little so every letter appears. */
private fun trainWindow(pool: List<KannadaLetter>, index: Int): List<KannadaLetter> {
    if (pool.size <= CARRIAGES) return pool
    val step = CARRIAGES - 1
    val start = (index * step).coerceAtMost(pool.size - CARRIAGES)
    return pool.subList(start, start + CARRIAGES)
}

private fun trainCount(pool: List<KannadaLetter>): Int {
    if (pool.size <= CARRIAGES) return 1
    val step = CARRIAGES - 1
    return 1 + (pool.size - CARRIAGES + step - 1) / step
}

/** A cheerful "toot toot" train whistle. */
private val whistleSamples: ShortArray by lazy {
    val rate = 22050
    val toot = 0.22
    val gap = 0.08
    val total = ((toot * 2 + gap) * rate).toInt()
    ShortArray(total) { i ->
        val t = i.toDouble() / rate
        val inFirst = t < toot
        val inSecond = t > toot + gap
        if (!inFirst && !inSecond) return@ShortArray 0
        val local = if (inFirst) t else t - toot - gap
        val env = (1.0 - exp(-local * 60)) * exp(-local * 3)
        val v = sin(2 * PI * 523.25 * t) + 0.8 * sin(2 * PI * 659.25 * t) + 0.6 * sin(2 * PI * 783.99 * t)
        (v * env * 7000).toInt().coerceIn(-32767, 32767).toShort()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterTrainScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val authPrefs = remember { getSharedPreferences("AppProgress") }
    val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
    val isGuest = UserManager.isGuest(userEmail)
    val gamePrefs = remember { getSharedPreferences("GamePrefs") }

    var mode by remember { mutableStateOf(trainModes.first()) }
    val pool = remember(mode) { lettersFor(mode.key) }
    val totalTrains = remember(pool) { trainCount(pool) }
    fun progressKey(m: TrainMode) = "${userEmail}_letter_train_${m.key}"

    var trainIndex by remember(mode) {
        mutableIntStateOf(if (isGuest) 0 else gamePrefs.getInt(progressKey(mode), 0).coerceIn(0, trainCount(lettersFor(mode.key)) - 1))
    }
    var trainsSinceSticker by remember { mutableIntStateOf(0) }

    val window = remember(pool, trainIndex) { trainWindow(pool, trainIndex) }
    val missingCount = if (trainIndex == 0) 2 else 3
    var filled by remember(window) { mutableStateOf(List(window.size) { true }) }
    var tiles by remember(window) { mutableStateOf(listOf<KannadaLetter>()) }
    var wrongTileId by remember { mutableStateOf<Int?>(null) }
    var highlighted by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(true) }
    var showConfetti by remember { mutableStateOf(false) }
    var showSticker by remember { mutableStateOf(false) }
    var showPauseMenu by remember { mutableStateOf(false) }
    val carriageBounds = remember { mutableStateMapOf<Int, Rect>() }

    val trainX = remember { Animatable(0f) }
    var screenWidthPx by remember { mutableStateOf(1000f) }
    val moving = trainX.isRunning

    fun setUpTrain() {
        val missing = window.indices.shuffled().take(missingCount.coerceAtMost(window.size)).toSet()
        filled = window.indices.map { it !in missing }
        val answers = missing.map { window[it] }
        val distractor = if (trainIndex > 0) {
            pool.filter { candidate -> window.none { it.id == candidate.id } }.shuffled().take(1)
        } else emptyList()
        tiles = (answers + distractor).shuffled()
        wrongTileId = null
        highlighted = null
    }

    suspend fun arrive() {
        busy = true
        trainX.snapTo(screenWidthPx)
        trainX.animateTo(0f, tween(1300, easing = LinearOutSlowInEasing))
        busy = false
    }

    LaunchedEffect(window) {
        setUpTrain()
        arrive()
    }

    fun speak(letter: KannadaLetter) = NativeAudioPlayer.playPhonics(context, letter.character)

    fun goToTrain(index: Int) {
        showSticker = false
        when {
            isGuest && index > 0 -> {
                Toast.makeText(context, "🔒 Train 1 Complete! Sign in with Gmail to ride all trains!", Toast.LENGTH_LONG).show()
                scope.launch { setUpTrain(); arrive() }
            }
            index >= totalTrains -> {
                Toast.makeText(context, "🎉 You finished every train! Starting again.", Toast.LENGTH_LONG).show()
                if (trainIndex == 0) scope.launch { setUpTrain(); arrive() } else trainIndex = 0
            }
            else -> trainIndex = index
        }
    }

    fun completeTrain() {
        busy = true
        scope.launch {
            // Read the whole train out loud, carriage by carriage
            delay(500)
            for (i in window.indices) {
                highlighted = i
                speak(window[i])
                delay(750)
            }
            highlighted = null
            PlatformAudio.playPcm16(whistleSamples, 22050)
            showConfetti = true
            try { ScoreManager.addCoins(context, "letter_train", 5) } catch (e: Exception) {}
            trainX.animateTo(-screenWidthPx * 1.3f, tween(1500, easing = FastOutLinearInEasing))
            showConfetti = false

            val next = trainIndex + 1
            if (!isGuest && next < totalTrains && next > gamePrefs.getInt(progressKey(mode), 0)) {
                gamePrefs.edit().putInt(progressKey(mode), next).apply()
            }
            trainsSinceSticker += 1
            if (trainsSinceSticker >= TRAINS_PER_STICKER || next >= totalTrains) {
                trainsSinceSticker = 0
                SoundEffectsHelper.playGameCompletion(context)
                showSticker = true
            } else {
                goToTrain(next)
            }
        }
    }

    fun place(letter: KannadaLetter, slot: Int) {
        val newFilled = filled.toMutableList()
        newFilled[slot] = true
        filled = newFilled
        tiles = tiles.filter { it.id != letter.id }
        SoundEffectsHelper.playSuccess(context)
        speak(letter)
        if (newFilled.all { it }) completeTrain()
    }

    fun wrong(letter: KannadaLetter) {
        SoundEffectsHelper.playOops(context)
        wrongTileId = letter.id
        scope.launch {
            delay(600)
            if (wrongTileId == letter.id) wrongTileId = null
        }
    }

    /** Tap: the letter goes into the first empty carriage, if that is its place. */
    fun onTileTap(letter: KannadaLetter) {
        if (busy) return
        val firstEmpty = filled.indexOfFirst { !it }
        if (firstEmpty >= 0 && window[firstEmpty].id == letter.id) place(letter, firstEmpty) else wrong(letter)
    }

    /** Drag: the letter goes into the carriage it was dropped on, if that is its place. */
    fun onTileDrop(letter: KannadaLetter, dropCenter: Offset) {
        if (busy) return
        val slot = carriageBounds.entries
            .filter { (i, rect) -> !filled.getOrElse(i) { true } && rect.inflate(16f).contains(dropCenter) }
            .map { it.key }
            .firstOrNull()
        when {
            slot == null -> Unit // dropped somewhere else: tile just springs back
            window[slot].id == letter.id -> place(letter, slot)
            else -> wrong(letter)
        }
    }

    Scaffold(
        containerColor = Sky,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Letter Train", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text(
                            "Train ${trainIndex + 1}/$totalTrains",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TrainInk)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .onGloballyPositioned { screenWidthPx = it.size.width.toFloat() }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Choose what the train carries
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    trainModes.forEach { m ->
                        val selected = m.key == mode.key
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .background(if (selected) m.accent else Color.White, RoundedCornerShape(20.dp))
                                .border(2.dp, m.accent, RoundedCornerShape(20.dp))
                                .clickable(enabled = !busy || !selected) {
                                    if (!selected) {
                                        trainsSinceSticker = 0
                                        mode = m
                                    }
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = m.label,
                                color = if (selected) Color.White else TrainInk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Text(
                    text = "Put the letters back in order!",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TrainInk
                )

                // Track scene
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                  Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.BottomCenter
                  ) {
                    // A couple of clouds
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 36.dp, top = 8.dp)
                            .size(width = 90.dp, height = 34.dp)
                            .background(Color.White, RoundedCornerShape(17.dp))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 48.dp, top = 40.dp)
                            .size(width = 70.dp, height = 26.dp)
                            .background(Color.White, RoundedCornerShape(13.dp))
                    )

                    val engineWidth = 84.dp
                    val gap = 6.dp
                    val carriageSize: Dp = minOf(
                        (maxWidth - engineWidth - gap * (CARRIAGES + 2)) / CARRIAGES,
                        72.dp
                    )

                    // Ground and rails
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Color(0xFF8D6E63))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(Grass)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .padding(bottom = 54.dp)
                            .offset { IntOffset(trainX.value.roundToInt(), 0) },
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        Engine(width = engineWidth, moving = moving)
                        window.forEachIndexed { i, letter ->
                            Carriage(
                                letter = letter,
                                filled = filled.getOrElse(i) { true },
                                size = carriageSize,
                                color = CarriageColors[i % CarriageColors.size],
                                highlighted = highlighted == i,
                                moving = moving,
                                onPositioned = { carriageBounds[i] = it }
                            )
                        }
                    }
                  }
                }

                // Letter tiles to put back
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .zIndex(2f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    tiles.forEach { letter ->
                        LetterTile(
                            letter = letter,
                            shaking = wrongTileId == letter.id,
                            enabled = !busy,
                            onTap = { onTileTap(letter) },
                            onDrop = { center -> onTileDrop(letter, center) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap a letter, or drag it onto its carriage",
                        fontSize = 13.sp,
                        color = TrainInk.copy(alpha = 0.7f)
                    )
                    IconButton(
                        onClick = {
                            val firstEmpty = filled.indexOfFirst { !it }
                            if (firstEmpty > 0) speak(window[firstEmpty - 1])
                        },
                        modifier = Modifier.testTag("train_hint_sound")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Hear the letter before the empty carriage",
                            tint = TrainInk
                        )
                    }
                }
            }

            ConfettiAnimation(show = showConfetti)

            if (showPauseMenu) {
                PauseMenuDialog(
                    onDismiss = { showPauseMenu = false },
                    onRestart = {
                        showPauseMenu = false
                        scope.launch { setUpTrain(); arrive() }
                    },
                    onMainMenu = {
                        showPauseMenu = false
                        onBack()
                    }
                )
            }

            if (showSticker) {
                com.example.ui.components.SurpriseStickerDialog(
                    userEmail = userEmail,
                    levelName = "Train ${trainIndex + 1}",
                    seedModifier = trainIndex + 100,
                    onClose = { goToTrain(trainIndex + 1) },
                    onClaimOrNext = { goToTrain(trainIndex + 1) }
                )
            }
        }
    }
}

@Composable
private fun Engine(width: Dp, moving: Boolean) {
    val anim = rememberInfiniteTransition(label = "engine")
    val puff by anim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (moving) 500 else 1400, easing = LinearEasing)),
        label = "puff"
    )
    val wheelTurn by anim.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing)),
        label = "wheels"
    )
    Canvas(
        modifier = Modifier
            .width(width)
            .height(width * 1.15f)
    ) {
        val w = size.width
        val h = size.height
        val wheelR = w * 0.13f
        val bodyBottom = h - wheelR * 1.2f

        // Smoke
        for (k in 0..2) {
            val p = (puff + k / 3f) % 1f
            drawCircle(
                color = Color.White.copy(alpha = (1f - p) * 0.9f),
                radius = w * (0.07f + 0.08f * p),
                center = Offset(w * 0.22f - p * w * 0.1f, h * 0.18f - p * h * 0.2f)
            )
        }
        // Chimney
        drawRect(TrainInk, topLeft = Offset(w * 0.14f, h * 0.2f), size = Size(w * 0.16f, h * 0.22f))
        // Boiler (front, facing left)
        drawRoundRect(
            color = Color(0xFFE53935),
            topLeft = Offset(w * 0.02f, h * 0.38f),
            size = Size(w * 0.62f, bodyBottom - h * 0.38f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
        )
        // Cab
        drawRoundRect(
            color = Color(0xFF1E88E5),
            topLeft = Offset(w * 0.56f, h * 0.12f),
            size = Size(w * 0.42f, bodyBottom - h * 0.12f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        // Cab roof
        drawRoundRect(
            color = TrainInk,
            topLeft = Offset(w * 0.52f, h * 0.08f),
            size = Size(w * 0.5f, h * 0.08f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
        // Window
        drawRoundRect(
            color = Color(0xFFBDE7FF),
            topLeft = Offset(w * 0.64f, h * 0.22f),
            size = Size(w * 0.26f, h * 0.2f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
        // Headlight
        drawCircle(Color(0xFFFFE066), radius = w * 0.06f, center = Offset(w * 0.08f, h * 0.5f))
        // Wheels
        drawWheel(Offset(w * 0.2f, h - wheelR), wheelR, if (moving) wheelTurn else 0f)
        drawWheel(Offset(w * 0.47f, h - wheelR), wheelR, if (moving) wheelTurn else 0f)
        drawWheel(Offset(w * 0.78f, h - wheelR), wheelR, if (moving) wheelTurn else 0f)
    }
}

private fun DrawScope.drawWheel(center: Offset, radius: Float, angle: Float) {
    drawCircle(TrainInk, radius = radius, center = center)
    drawCircle(Color(0xFFB0BEC5), radius = radius * 0.55f, center = center)
    rotate(angle, pivot = center) {
        drawLine(TrainInk, center - Offset(radius * 0.55f, 0f), center + Offset(radius * 0.55f, 0f), strokeWidth = radius * 0.18f)
        drawLine(TrainInk, center - Offset(0f, radius * 0.55f), center + Offset(0f, radius * 0.55f), strokeWidth = radius * 0.18f)
    }
}

@Composable
private fun Carriage(
    letter: KannadaLetter,
    filled: Boolean,
    size: Dp,
    color: Color,
    highlighted: Boolean,
    moving: Boolean,
    onPositioned: (Rect) -> Unit
) {
    val bounce by animateFloatAsState(if (highlighted) -14f else 0f, spring(dampingRatio = 0.35f), label = "bounce")
    val wheelTurn by rememberInfiniteTransition(label = "carriage_wheels").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing)),
        label = "turn"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { translationY = bounce }
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .onGloballyPositioned { onPositioned(it.boundsInRoot()) }
                .background(if (filled) color else Color.White, RoundedCornerShape(12.dp))
                .border(
                    width = 3.dp,
                    color = if (filled) TrainInk.copy(alpha = 0.25f) else color,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (filled) letter.character else "?",
                fontSize = if (letter.character.length > 1) (size.value * 0.36f).sp else (size.value * 0.48f).sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (filled) TrainInk else color
            )
        }
        Canvas(
            modifier = Modifier
                .width(size)
                .height(size * 0.32f)
        ) {
            val r = this.size.height * 0.5f
            drawWheel(Offset(this.size.width * 0.25f, r), r * 0.95f, if (moving) wheelTurn else 0f)
            drawWheel(Offset(this.size.width * 0.75f, r), r * 0.95f, if (moving) wheelTurn else 0f)
        }
    }
}

@Composable
private fun LetterTile(
    letter: KannadaLetter,
    shaking: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
    onDrop: (Offset) -> Unit
) {
    var dragOffset by remember(letter.id) { mutableStateOf(Offset.Zero) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnDrop by rememberUpdatedState(onDrop)
    val wobble by rememberInfiniteTransition(label = "tile_shake").animateFloat(
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
                val s = if (dragOffset != Offset.Zero) 1.1f else 1f
                scaleX = s
                scaleY = s
            }
            .size(68.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(3.dp, TrainInk, RoundedCornerShape(18.dp))
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .clickable(enabled = enabled) { onTap() }
            .pointerInput(letter.id) {
                detectDragGestures(
                    onDrag = { change, amount ->
                        if (currentEnabled) {
                            change.consume()
                            dragOffset += amount
                        }
                    },
                    onDragEnd = {
                        val center = bounds.center
                        dragOffset = Offset.Zero
                        if (currentEnabled) currentOnDrop(center)
                    },
                    onDragCancel = { dragOffset = Offset.Zero }
                )
            }
            .testTag("train_tile_${letter.id}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.character,
            fontSize = if (letter.character.length > 1) 24.sp else 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TrainInk
        )
    }
}
