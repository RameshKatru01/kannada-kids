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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KannadaLetter
import com.example.data.TraceRepository
import com.example.ui.audio.SoundEffectsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * Letter tracing used at the start of every letter lesson.
 *
 *  - The letter fills the whole tracing area (as big as fits).
 *  - Each time the child lifts their finger, the drawing is checked against the letter: enough of
 *    the letter must be covered and most of the ink must be on the letter.
 *  - 5 stars across the top show how many good traces are done; "Trace ಅ 5 times" and
 *    "2 more to go!" say it in words, a pointing hand shows where to start, and the letter is
 *    spoken after each good trace.
 */

private val PadInk = Color(0xFF0B3B66)
private val PadInkSoft = Color(0xFF3A5F82)
private val PadPaper = Color(0xFFFFFBF0)
private val PadGuide = Color(0xFFD6CCB4)
private val PadStroke = Color(0xFF6C63FF)
private val PadGood = Color(0xFF2EC4B6)
private val PadOops = Color(0xFFFF6B6B)
private val PadStar = Color(0xFFFFB300)
private val PadStarEmpty = Color(0xFFD5DEE6)

private const val REQUIRED_TRACES = 5

private enum class TraceFeedback { None, Good, TryAgain }

/** The instructions, stars and buttons above/below the pad, plus the pad itself. */
@Composable
fun TraceExercise(
    letter: KannadaLetter,
    onSpeakText: (String) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var done by remember(letter.character) { mutableIntStateOf(0) }
    var strokes by remember(letter.character) { mutableStateOf(listOf<List<Offset>>()) }
    var feedback by remember(letter.character) { mutableStateOf(TraceFeedback.None) }
    var checkJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var checker by remember { mutableStateOf<((List<List<Offset>>) -> TraceScore)?>(null) }

    LaunchedEffect(letter.character) {
        delay(400)
        onSpeakText(letter.character)
    }

    fun success() {
        TraceRepository.addTrace(letter.character, strokes)
        done += 1
        feedback = TraceFeedback.Good
        if (done >= REQUIRED_TRACES) SoundEffectsHelper.playGameCompletion(context) else SoundEffectsHelper.playSuccess(context)
        scope.launch {
            delay(450)
            onSpeakText(letter.character)
            delay(650)
            strokes = emptyList()
            feedback = TraceFeedback.None
            if (done >= REQUIRED_TRACES) {
                delay(300)
                onComplete()
            }
        }
    }

    fun tryAgain() {
        feedback = TraceFeedback.TryAgain
        SoundEffectsHelper.playOops(context)
        scope.launch {
            delay(1300)
            strokes = emptyList()
            feedback = TraceFeedback.None
        }
    }

    // Called when the finger lifts: pass straight away if it's good; otherwise give the child a
    // moment to add more strokes (many letters need 2–3), then say "try again".
    fun onStrokeFinished() {
        checkJob?.cancel()
        if (feedback != TraceFeedback.None) return
        val score = checker?.invoke(strokes) ?: return
        if (score.passed) {
            success()
        } else {
            checkJob = scope.launch {
                delay(2200)
                val again = checker?.invoke(strokes) ?: return@launch
                if (again.passed) success() else if (again.inked) tryAgain()
            }
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Instruction + progress
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Trace ${letter.character} $REQUIRED_TRACES times",
                    fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = PadInk
                )
                val left = REQUIRED_TRACES - done
                Text(
                    when {
                        feedback == TraceFeedback.Good && left == 0 -> "All $REQUIRED_TRACES done! 🎉"
                        feedback == TraceFeedback.Good -> "Great! $left more to go"
                        feedback == TraceFeedback.TryAgain -> "Oops! Follow the grey letter"
                        done == 0 -> "Use your finger to colour the grey letter"
                        else -> "$left more to go!"
                    },
                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = when (feedback) {
                        TraceFeedback.Good -> PadGood
                        TraceFeedback.TryAgain -> PadOops
                        TraceFeedback.None -> PadInkSoft
                    }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        TraceStars(done = done, celebrate = feedback == TraceFeedback.Good)
        Spacer(Modifier.height(8.dp))

        TracePad(
            letter = letter.character,
            strokes = strokes,
            feedback = feedback,
            showHint = done == 0 && strokes.isEmpty(),
            onStrokesChange = {
                checkJob?.cancel()
                strokes = it
            },
            onStrokeFinished = { onStrokeFinished() },
            onChecker = { checker = it },
            onSpeak = { onSpeakText(letter.character) },
            onClear = {
                checkJob?.cancel()
                strokes = emptyList()
                feedback = TraceFeedback.None
            },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}

/** Five stars, one per good trace; the next one to earn gently pulses. */
@Composable
private fun TraceStars(done: Int, celebrate: Boolean) {
    val pulse = rememberInfiniteTransition(label = "stars")
    val beat by pulse.animateFloat(1f, 1.18f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "beat")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(REQUIRED_TRACES) { i ->
            val earned = i < done
            val isNext = i == done
            val pop by animateFloatAsState(if (earned && celebrate && i == done - 1) 1.35f else 1f, spring(dampingRatio = 0.35f), label = "pop")
            Box(
                Modifier
                    .size(40.dp)
                    .scale(if (isNext) beat else pop)
                    .background(if (earned) Color(0xFFFFF1C4) else Color.White, CircleShape)
                    .border(3.dp, if (earned) PadStar else if (isNext) PadInk.copy(alpha = 0.35f) else PadStarEmpty, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (earned) {
                    com.example.ui.progress.StarIcon(26.dp, PadStar)
                } else {
                    Text("${i + 1}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (isNext) PadInk else PadStarEmpty)
                }
            }
        }
    }
}

/** How well a drawing matches the letter. */
data class TraceScore(val coverage: Float, val accuracy: Float, val inked: Boolean) {
    /** Kind thresholds for small fingers: half the letter covered and most ink on the letter. */
    val passed: Boolean get() = inked && coverage >= 0.5f && accuracy >= 0.7f
}

@Composable
private fun TracePad(
    letter: String,
    strokes: List<List<Offset>>,
    feedback: TraceFeedback,
    showHint: Boolean,
    onStrokesChange: (List<List<Offset>>) -> Unit,
    onStrokeFinished: () -> Unit,
    onChecker: ((List<List<Offset>>) -> TraceScore) -> Unit,
    onSpeak: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    var current by remember { mutableStateOf(listOf<Offset>()) }
    val latestStrokes by rememberUpdatedState(strokes)

    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                when (feedback) {
                    TraceFeedback.Good -> Color(0xFFE3FAF6)
                    TraceFeedback.TryAgain -> Color(0xFFFFECEC)
                    TraceFeedback.None -> PadPaper
                }
            )
            .border(3.dp, PadGuide, RoundedCornerShape(24.dp))
    ) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }

        // Measure the letter as big as it fits (about 90% of the width, 85% of the height)
        val layout: TextLayoutResult = remember(letter, wPx, hPx) {
            val base = with(density) { (min(maxWidth.value, maxHeight.value) * 0.8f).dp.toSp() }
            val style = TextStyle(fontSize = base, fontWeight = FontWeight.Bold)
            val first = measurer.measure(letter, style)
            val k = min(1f, min(wPx * 0.9f / max(1, first.size.width), hPx * 0.85f / max(1, first.size.height)))
            if (k < 0.999f) measurer.measure(letter, style.copy(fontSize = base * k)) else first
        }
        val topLeft = Offset((wPx - layout.size.width) / 2f, (hPx - layout.size.height) / 2f)

        // The checker compares strokes against a small mask of the letter
        val brushPx = with(density) { 22.dp.toPx() }
        LaunchedEffect(layout, wPx, hPx) {
            val cellPx = with(density) { 4.dp.toPx() }
            val mask = letterMask(layout, topLeft, wPx, hPx, cellPx, density, direction)
            onChecker { s -> scoreTrace(mask, s, cellPx, with(density) { 16.dp.toPx() }, with(density) { 18.dp.toPx() }) }
        }

        val hint = rememberInfiniteTransition(label = "hint")
        val hintMove by hint.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "hintMove")

        Canvas(
            Modifier
                .fillMaxSize()
                .testTag("trace_pad")
                .pointerInput(letter) {
                    detectDragGestures(
                        onDragStart = { current = listOf(it) },
                        onDrag = { change, _ ->
                            change.consume()
                            current = current + change.position
                        },
                        onDragEnd = {
                            if (current.isNotEmpty()) onStrokesChange(latestStrokes + listOf(current))
                            current = emptyList()
                            onStrokeFinished()
                        },
                        onDragCancel = { current = emptyList() }
                    )
                }
        ) {
            // Guide letter
            drawText(layout, color = PadGuide, topLeft = topLeft)
            val ink = when (feedback) {
                TraceFeedback.Good -> PadGood
                TraceFeedback.TryAgain -> PadOops
                TraceFeedback.None -> PadStroke
            }
            (strokes + listOf(current)).forEach { stroke ->
                if (stroke.size > 1) {
                    val path = Path().apply {
                        moveTo(stroke.first().x, stroke.first().y)
                        for (i in 1 until stroke.size) lineTo(stroke[i].x, stroke[i].y)
                    }
                    drawPath(path, ink, style = Stroke(width = brushPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                } else if (stroke.size == 1) {
                    drawCircle(ink, radius = brushPx / 2, center = stroke.first())
                }
            }
        }

        // Pointing hand that slides across the letter until the child starts
        if (showHint && current.isEmpty()) {
            val handX = with(density) { (topLeft.x + layout.size.width * (0.25f + 0.5f * hintMove)).toDp() }
            val handY = with(density) { (topLeft.y + layout.size.height * 0.55f).toDp() }
            Text("👆", fontSize = 44.sp, modifier = Modifier.offset(x = handX - 16.dp, y = handY))
        }

        // Listen + clear buttons
        Row(Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PadButton(onClick = onSpeak) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear the letter", tint = PadInk)
            }
            PadButton(onClick = onClear) {
                Icon(Icons.Default.Refresh, contentDescription = "Start again", tint = PadInk)
            }
        }
    }
}

@Composable
private fun PadButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(2.dp, PadGuide, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

// ---------------------------------------------------------------------------------------------
// Checking a trace
// ---------------------------------------------------------------------------------------------

/** The letter as a grid of on/off cells ([cellPx] pixels each). */
class LetterMask(val cols: Int, val rows: Int, val on: BooleanArray) {
    val count: Int = on.count { it }
}

internal fun letterMask(
    layout: TextLayoutResult,
    topLeft: Offset,
    wPx: Float,
    hPx: Float,
    cellPx: Float,
    density: Density,
    direction: LayoutDirection
): LetterMask {
    val cols = max(1, ceil(wPx / cellPx).toInt())
    val rows = max(1, ceil(hPx / cellPx).toInt())
    val image = ImageBitmap(cols, rows)
    val canvas = androidx.compose.ui.graphics.Canvas(image)
    CanvasDrawScope().draw(density, direction, canvas, Size(cols.toFloat(), rows.toFloat())) {
        scale(1f / cellPx, pivot = Offset.Zero) {
            drawText(layout, color = Color.Black, topLeft = topLeft)
        }
    }
    val pixels = IntArray(cols * rows)
    image.readPixels(pixels, 0, 0, cols, rows)
    val on = BooleanArray(cols * rows) { ((pixels[it] ushr 24) and 0xFF) > 70 }
    return LetterMask(cols, rows, on)
}

/**
 * coverage = share of the letter's cells within [reachPx] of the child's lines;
 * accuracy = share of the child's line points within [tolerancePx] of the letter.
 */
fun scoreTrace(mask: LetterMask, strokes: List<List<Offset>>, cellPx: Float, reachPx: Float, tolerancePx: Float): TraceScore {
    if (mask.count == 0) return TraceScore(1f, 1f, strokes.isNotEmpty())
    // Points along the lines, about one per cell
    val points = ArrayList<Offset>()
    strokes.forEach { s ->
        if (s.size == 1) points.add(s[0])
        for (i in 1 until s.size) {
            val a = s[i - 1]
            val b = s[i]
            val d = (b - a).getDistance()
            val n = max(1, (d / cellPx).roundToInt())
            for (k in 0 until n) points.add(a + (b - a) * (k.toFloat() / n))
            if (i == s.size - 1) points.add(b)
        }
    }
    var length = 0f
    for (i in 1 until points.size) length += (points[i] - points[i - 1]).getDistance()
    val inked = length > cellPx * 30   // ignore taps and tiny scribbles
    if (points.isEmpty()) return TraceScore(0f, 0f, false)

    // Cells reached by the child's brush
    val reached = BooleanArray(mask.cols * mask.rows)
    val r = ceil(reachPx / cellPx).toInt()
    val r2 = (reachPx / cellPx) * (reachPx / cellPx)
    points.forEach { p ->
        val cx = (p.x / cellPx).toInt()
        val cy = (p.y / cellPx).toInt()
        for (dy in -r..r) for (dx in -r..r) {
            if (dx * dx + dy * dy > r2) continue
            val x = cx + dx
            val y = cy + dy
            if (x in 0 until mask.cols && y in 0 until mask.rows) reached[y * mask.cols + x] = true
        }
    }
    var covered = 0
    for (i in mask.on.indices) if (mask.on[i] && reached[i]) covered++

    // Ink near the letter
    val t = ceil(tolerancePx / cellPx).toInt()
    val t2 = (tolerancePx / cellPx) * (tolerancePx / cellPx)
    var onLetter = 0
    points.forEach { p ->
        val cx = (p.x / cellPx).toInt()
        val cy = (p.y / cellPx).toInt()
        var hit = false
        var dy = -t
        while (dy <= t && !hit) {
            var dx = -t
            while (dx <= t && !hit) {
                if (dx * dx + dy * dy <= t2) {
                    val x = cx + dx
                    val y = cy + dy
                    if (x in 0 until mask.cols && y in 0 until mask.rows && mask.on[y * mask.cols + x]) hit = true
                }
                dx++
            }
            dy++
        }
        if (hit) onLetter++
    }
    return TraceScore(
        coverage = covered.toFloat() / mask.count,
        accuracy = onLetter.toFloat() / points.size,
        inked = inked
    )
}
