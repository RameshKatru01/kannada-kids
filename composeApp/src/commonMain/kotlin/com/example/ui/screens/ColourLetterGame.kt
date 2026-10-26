package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KannadaLetter
import com.example.ui.audio.SoundEffectsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/*
 * "Colour the letter": the second step of every letter lesson, right after tracing.
 *
 *  - The letter is a big outline. The child picks a crayon colour and paints with a finger;
 *    paint only shows inside the letter, so it never looks messy.
 *  - A bar shows how much of the letter is coloured. At about 70% the letter sparkles,
 *    a cheer plays and the Next button appears (the child can keep colouring first).
 *  - Undo removes the last stroke; the refresh button starts again.
 */

private val ColInk = Color(0xFF0B3B66)
private val ColInkSoft = Color(0xFF3A5F82)
private val ColPaper = Color(0xFFFFFBF0)
private val ColGuide = Color(0xFFD6CCB4)
private val ColLetterFill = Color(0xFFF2EBDA)
private val ColDone = Color(0xFF2EC4B6)

/** Crayon colours, bright and easy to tell apart. */
private val Crayons = listOf(
    Color(0xFFFF4D4D), // red
    Color(0xFFFF9F1C), // orange
    Color(0xFFFFD23F), // yellow
    Color(0xFF3DDC84), // green
    Color(0xFF2EC4B6), // teal
    Color(0xFF4D8BFF), // blue
    Color(0xFF9B5DE5), // purple
    Color(0xFFFF6FB5), // pink
)

/** How much of the letter has to be coloured before the lesson moves on. */
private const val COLOURED_ENOUGH = 0.7f

private class PaintStroke(val color: Color, val points: List<Offset>)

@Composable
fun ColourLetterGame(
    letter: KannadaLetter,
    onSpeakText: (String) -> Unit = {},
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var strokes by remember(letter.character) { mutableStateOf(listOf<PaintStroke>()) }
    var colour by remember { mutableStateOf(Crayons[0]) }
    var coloured by remember(letter.character) { mutableFloatStateOf(0f) }
    var finished by remember(letter.character) { mutableStateOf(false) }
    var measure by remember { mutableStateOf<((List<List<Offset>>) -> Float)?>(null) }

    LaunchedEffect(letter.character) {
        delay(400)
        onSpeakText(letter.character)
    }

    fun recount() {
        val m = measure ?: return
        coloured = m(strokes.map { it.points })
        if (!finished && coloured >= COLOURED_ENOUGH) {
            finished = true
            SoundEffectsHelper.playGameCompletion(context)
            scope.launch {
                delay(500)
                onSpeakText(letter.character)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            if (finished) "Beautiful! ⭐" else "Colour ${letter.character}!",
            fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = ColInk
        )
        Text(
            if (finished) "Keep colouring, or tap Next when you are ready."
            else "Pick a crayon and colour in the letter.",
            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ColInkSoft
        )

        ColourProgress(coloured)

        ColourPad(
            letter = letter.character,
            strokes = strokes,
            colour = colour,
            finished = finished,
            onStroke = { s ->
                strokes = strokes + s
                recount()
            },
            onMeasure = { measure = it; recount() },
            onSpeak = { onSpeakText(letter.character) },
            onUndo = {
                if (strokes.isNotEmpty()) {
                    strokes = strokes.dropLast(1)
                    recount()
                }
            },
            onClear = {
                strokes = emptyList()
                coloured = 0f
            },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        // Crayon box
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Crayons.forEachIndexed { i, c ->
                val on = c == colour
                Box(
                    Modifier
                        .size(if (on) 46.dp else 38.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(if (on) 4.dp else 2.dp, if (on) ColInk else Color.White, CircleShape)
                        .clickable {
                            colour = c
                            SoundEffectsHelper.playClick(context)
                        }
                        .testTag("crayon_$i")
                )
            }
        }

        AnimatedVisibility(visible = finished, enter = fadeIn() + scaleIn()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ColDone)
                    .clickable { onComplete() }
                    .testTag("colour_next"),
                contentAlignment = Alignment.Center
            ) {
                Text("Next ▶", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

/** "Coloured so far" bar, turning teal once it is enough. */
@Composable
private fun ColourProgress(coloured: Float) {
    val shown by animateFloatAsState(min(1f, coloured / COLOURED_ENOUGH), label = "coloured")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("🖍️", fontSize = 20.sp)
        Box(
            Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFFE3EEF8))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(shown)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (shown >= 1f) ColDone else Color(0xFFFFB300))
            )
        }
        Text(if (shown >= 1f) "⭐" else "", fontSize = 20.sp)
    }
}

@Composable
private fun ColourPad(
    letter: String,
    strokes: List<PaintStroke>,
    colour: Color,
    finished: Boolean,
    onStroke: (PaintStroke) -> Unit,
    onMeasure: ((List<List<Offset>>) -> Float) -> Unit,
    onSpeak: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    var current by remember { mutableStateOf(listOf<Offset>()) }
    val latestColour by rememberUpdatedState(colour)

    // A gentle pulse when the letter is done
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(1f, 1.03f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "glow")

    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(ColPaper)
            .border(3.dp, if (finished) ColDone else ColGuide, RoundedCornerShape(24.dp))
    ) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }

        // The letter as big as it fits, like the tracing pad
        val layout: TextLayoutResult = remember(letter, wPx, hPx) {
            val base = with(density) { (min(maxWidth.value, maxHeight.value) * 0.8f).dp.toSp() }
            val style = TextStyle(fontSize = base, fontWeight = FontWeight.Bold)
            val first = measurer.measure(letter, style)
            val k = min(1f, min(wPx * 0.9f / max(1, first.size.width), hPx * 0.85f / max(1, first.size.height)))
            if (k < 0.999f) measurer.measure(letter, style.copy(fontSize = base * k)) else first
        }
        val topLeft = Offset((wPx - layout.size.width) / 2f, (hPx - layout.size.height) / 2f)
        val brushPx = with(density) { 34.dp.toPx() }
        val outlinePx = with(density) { 3.dp.toPx() }

        // The letter drawn once into an image, used as a stencil so paint can never show outside
        // it. (Blending with an image works the same on iOS and Android; blending text does not.)
        val stencil: ImageBitmap? = remember(layout, wPx, hPx) {
            val w = wPx.toInt()
            val h = hPx.toInt()
            if (w <= 0 || h <= 0) null else ImageBitmap(w, h).also { img ->
                CanvasDrawScope().draw(density, direction, androidx.compose.ui.graphics.Canvas(img), Size(w.toFloat(), h.toFloat())) {
                    drawText(layout, color = Color.Black, topLeft = topLeft)
                }
            }
        }

        // Share of the letter's cells under the paint
        LaunchedEffect(layout, wPx, hPx) {
            val cellPx = with(density) { 4.dp.toPx() }
            val mask = letterMask(layout, topLeft, wPx, hPx, cellPx, density, direction)
            onMeasure { s -> scoreTrace(mask, s, cellPx, brushPx / 2f, brushPx).coverage }
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .scale(if (finished) glow else 1f)
                // Its own layer, so the paint can be cut to the letter's shape below
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .testTag("colour_pad")
                .pointerInput(letter) {
                    detectTapGestures { p -> onStroke(PaintStroke(latestColour, listOf(p))) }
                }
                .pointerInput(letter) {
                    detectDragGestures(
                        onDragStart = { current = listOf(it) },
                        onDrag = { change, _ ->
                            change.consume()
                            current = current + change.position
                        },
                        onDragEnd = {
                            if (current.isNotEmpty()) onStroke(PaintStroke(latestColour, current))
                            current = emptyList()
                        },
                        onDragCancel = { current = emptyList() }
                    )
                }
        ) {
            // 1. The child's paint
            (strokes + PaintStroke(colour, current)).forEach { s ->
                val pts = s.points
                if (pts.size > 1) {
                    val path = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                    }
                    drawPath(path, s.color, style = Stroke(width = brushPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                } else if (pts.size == 1) {
                    drawCircle(s.color, radius = brushPx / 2, center = pts.first())
                }
            }
            if (stencil != null) {
                // 2. Keep the paint only where the letter is
                drawImage(stencil, blendMode = BlendMode.DstIn)
                // 3. A soft fill behind the paint, so the uncoloured part of the letter still shows
                drawImage(stencil, colorFilter = ColorFilter.tint(ColLetterFill), blendMode = BlendMode.DstOver)
            }
            // 4. The outline on top
            drawText(layout, color = ColInk, topLeft = topLeft, drawStyle = Stroke(width = outlinePx, join = StrokeJoin.Round))
        }

        Row(Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ColourPadButton(onClick = onSpeak, tag = "colour_hear") {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear the letter", tint = ColInk)
            }
            ColourPadButton(onClick = onUndo, tag = "colour_undo") {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = ColInk)
            }
            ColourPadButton(onClick = onClear, tag = "colour_clear") {
                Icon(Icons.Default.Refresh, contentDescription = "Start again", tint = ColInk)
            }
        }
    }
}

@Composable
private fun ColourPadButton(onClick: () -> Unit, tag: String, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(2.dp, ColGuide, CircleShape)
            .clickable { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) { content() }
}
