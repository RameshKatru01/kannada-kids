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
import androidx.compose.ui.graphics.graphicsLayer
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
 *  - The letter fills the whole tracing area: it is sized by its real shape (not the font's line
 *    box), so every letter is as big as fits.
 *  - The grey letter fills with colour where the child's finger goes. Nothing is judged while
 *    drawing: the child taps Done when finished (Clear starts again).
 *  - On Done the trace passes with at least 80% of the letter coloured and at least 80% of the
 *    ink on the letter. Not enough coloured = the parts still to colour glow orange and the child
 *    carries on; too much ink off the letter = "Oops, stay on the grey letter" and it clears.
 *  - 5 stars across the top show how many good traces are done; "Trace ಅ 5 times" and
 *    "2 more to go!" say it in words, a pointing hand shows where to start, and the letter is
 *    spoken after each good trace.
 */

private val PadInk = Color(0xFF0B3B66)
private val PadInkSoft = Color(0xFF3A5F82)
private val PadPaper = Color(0xFFFFFBF0)
private val PadGuide = Color(0xFFD6CCB4)
private val PadStroke = Color(0xFF6C63FF)
private val PadFill = Color(0xFFFF5C8A)   // the child's colour (pink, so the orange "still to colour" parts stand out)
private val PadGood = Color(0xFF2EC4B6)
private val PadOops = Color(0xFFFF6B6B)
private val PadStar = Color(0xFFFFB300)
private val PadStarEmpty = Color(0xFFD5DEE6)

private const val REQUIRED_TRACES = 5

private enum class TraceFeedback { None, Good, NotYet, TryAgain }

/** The parts still to colour after Done (not enough coloured yet). */
private val PadMissing = Color(0xFFFFB25C)

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

    // Checked only when the child taps Done (nothing is judged while drawing)
    fun checkOnDone() {
        if (feedback == TraceFeedback.Good || feedback == TraceFeedback.TryAgain) return
        val score = checker?.invoke(strokes) ?: return
        when {
            score.passed -> success()
            score.accuracy < OK_ACCURACY -> tryAgain()
            else -> {
                // Not enough coloured: show the parts still to colour (orange) and let them carry on
                feedback = TraceFeedback.NotYet
                SoundEffectsHelper.playOops(context)
            }
        }
    }
    val canCheck = strokes.isNotEmpty() && feedback != TraceFeedback.Good && feedback != TraceFeedback.TryAgain

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
                        feedback == TraceFeedback.TryAgain -> "Oops! Stay on the grey letter"
                        feedback == TraceFeedback.NotYet -> "Almost! Colour the orange bits, then tap Done"
                        done == 0 -> "Colour the whole grey letter, then tap Done"
                        else -> "$left more to go! Tap Done when finished"
                    },
                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = when (feedback) {
                        TraceFeedback.Good -> PadGood
                        TraceFeedback.TryAgain -> PadOops
                        TraceFeedback.NotYet -> Color(0xFFE8711A)
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
            onStrokesChange = { strokes = it },
            onChecker = { checker = it },
            onSpeak = { onSpeakText(letter.character) },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Spacer(Modifier.height(8.dp))
        // Clear + Done: the trace is checked only when the child taps Done
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(2.dp, PadInk, RoundedCornerShape(26.dp))
                    .clickable(enabled = feedback != TraceFeedback.Good) {
                        strokes = emptyList()
                        feedback = TraceFeedback.None
                    }
                    .testTag("trace_clear"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = PadInk, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text("Clear", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = PadInk)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (canCheck) PadGood else Color(0xFFB8C7D4))
                    .clickable(enabled = canCheck) { checkOnDone() }
                    .testTag("trace_done"),
                contentAlignment = Alignment.Center
            ) {
                Text("✓ Done", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
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

/** How much of the letter must be coloured, and how much of the ink must be on the letter. */
private const val GOAL_COVERAGE = 0.80f
private const val OK_ACCURACY = 0.80f

/** How well a drawing matches the letter. */
data class TraceScore(val coverage: Float, val accuracy: Float, val inked: Boolean) {
    /** Most of the letter coloured, and most of the ink on the letter. */
    val passed: Boolean get() = inked && coverage >= GOAL_COVERAGE && accuracy >= OK_ACCURACY
}

@Composable
private fun TracePad(
    letter: String,
    strokes: List<List<Offset>>,
    feedback: TraceFeedback,
    showHint: Boolean,
    onStrokesChange: (List<List<Offset>>) -> Unit,
    onChecker: ((List<List<Offset>>) -> TraceScore) -> Unit,
    onSpeak: () -> Unit,
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
                    TraceFeedback.NotYet, TraceFeedback.None -> PadPaper
                }
            )
            .border(3.dp, PadGuide, RoundedCornerShape(24.dp))
    ) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }

        // The letter as big as it fits: sized by its real (drawn) shape, ~92% of the width and ~90%
        // of the height, and centred on that shape (a font's line box has lots of empty space)
        val fitted = remember(letter, wPx, hPx) { fitLetter(measurer, letter, wPx, hPx, density, direction) }
        val layout: TextLayoutResult = fitted.first
        val topLeft = fitted.second
        val inkBox = fitted.third

        // How far a finger line "reaches" grows with the letter (a bigger letter has thicker strokes)
        val reachPx = max(with(density) { 16.dp.toPx() }, inkBox.height * 0.065f)
        val brushPx = with(density) { 22.dp.toPx() }
        // Width of the colour that fills the letter (matches how far the check "reaches")
        val fillPx = reachPx * 2f
        LaunchedEffect(layout, topLeft, wPx, hPx) {
            val cellPx = with(density) { 4.dp.toPx() }
            val mask = letterMask(layout, topLeft, wPx, hPx, cellPx, density, direction)
            onChecker { s -> scoreTrace(mask, s, cellPx, reachPx, reachPx * 1.15f) }
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
                        },
                        onDragCancel = { current = emptyList() }
                    )
                }
        ) {
            // Guide letter (orange after Done when parts are still to colour)
            drawText(layout, color = if (feedback == TraceFeedback.NotYet) PadMissing else PadGuide, topLeft = topLeft)
            // The finger's line, faint (shows where it went off the letter)
            val line = if (feedback == TraceFeedback.TryAgain) PadOops else PadStroke.copy(alpha = 0.35f)
            (strokes + listOf(current)).forEach { stroke -> drawStroke(stroke, line, brushPx * 0.45f) }
        }
        // The letter filling with colour: wide strokes, kept only inside the letter's shape
        val fill = when (feedback) {
            TraceFeedback.Good -> PadGood
            TraceFeedback.TryAgain -> PadOops
            TraceFeedback.NotYet, TraceFeedback.None -> PadFill
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
        ) {
            (strokes + listOf(current)).forEach { stroke -> drawStroke(stroke, fill, fillPx) }
            drawText(layout, color = Color.Black, topLeft = topLeft, blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
        }

        // Pointing hand that slides across the letter until the child starts
        if (showHint && current.isEmpty()) {
            val handX = with(density) { (topLeft.x + inkBox.left + inkBox.width * (0.25f + 0.5f * hintMove)).toDp() }
            val handY = with(density) { (topLeft.y + inkBox.top + inkBox.height * 0.55f).toDp() }
            Text("👆", fontSize = 44.sp, modifier = Modifier.offset(x = handX - 16.dp, y = handY))
        }

        // Listen button (Clear is under the pad, next to Done)
        Row(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            PadButton(onClick = onSpeak) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear the letter", tint = PadInk)
            }
        }
    }
}

/**
 * The letter measured to fill a [wPx] x [hPx] area: the layout, where to draw it (top-left), and the
 * box of its drawn pixels inside the layout. Sized by the real drawn shape, so every letter is as big
 * as fits ([fillW] of the width, [fillH] of the height) and centred on that shape.
 */
internal fun fitLetter(
    measurer: androidx.compose.ui.text.TextMeasurer,
    letter: String,
    wPx: Float,
    hPx: Float,
    density: Density,
    direction: LayoutDirection,
    fillW: Float = 0.92f,
    fillH: Float = 0.90f
): Triple<TextLayoutResult, Offset, androidx.compose.ui.geometry.Rect> {
    val base = with(density) { 100.dp.toSp() }
    val style = TextStyle(fontSize = base, fontWeight = FontWeight.Bold)
    val first = measurer.measure(letter, style)
    val ink = inkBounds(first, density, direction)
    val k = min(wPx * fillW / max(1f, ink.width), hPx * fillH / max(1f, ink.height))
    val layout = measurer.measure(letter, style.copy(fontSize = base * k))
    // Same shape, scaled (no need to scan the big one again)
    val box = androidx.compose.ui.geometry.Rect(ink.left * k, ink.top * k, ink.right * k, ink.bottom * k)
    val topLeft = Offset((wPx - box.width) / 2f - box.left, (hPx - box.height) / 2f - box.top)
    return Triple(layout, topLeft, box)
}

/** Box of the letter's drawn pixels inside its text layout (the whole layout box if none are found). */
private fun inkBounds(layout: TextLayoutResult, density: Density, direction: LayoutDirection): androidx.compose.ui.geometry.Rect {
    val w = max(1, layout.size.width)
    val h = max(1, layout.size.height)
    val image = ImageBitmap(w, h)
    val canvas = androidx.compose.ui.graphics.Canvas(image)
    CanvasDrawScope().draw(density, direction, canvas, Size(w.toFloat(), h.toFloat())) {
        drawText(layout, color = Color.Black)
    }
    val pixels = IntArray(w * h)
    image.readPixels(pixels, 0, 0, w, h)
    var minX = w; var minY = h; var maxX = -1; var maxY = -1
    for (y in 0 until h) for (x in 0 until w) {
        if (((pixels[y * w + x] ushr 24) and 0xFF) > 40) {
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
    }
    return if (maxX < 0) androidx.compose.ui.geometry.Rect(0f, 0f, w.toFloat(), h.toFloat())
    else androidx.compose.ui.geometry.Rect(minX.toFloat(), minY.toFloat(), maxX + 1f, maxY + 1f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(stroke: List<Offset>, color: Color, width: Float) {
    if (stroke.size > 1) {
        val path = Path().apply {
            moveTo(stroke.first().x, stroke.first().y)
            for (i in 1 until stroke.size) lineTo(stroke[i].x, stroke[i].y)
        }
        drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    } else if (stroke.size == 1) {
        drawCircle(color, radius = width / 2, center = stroke.first())
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
