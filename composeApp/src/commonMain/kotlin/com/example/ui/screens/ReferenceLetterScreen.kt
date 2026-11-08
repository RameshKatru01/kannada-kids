package com.example.ui.screens

import com.example.ui.components.readableWidth
import com.example.platform.*

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KannadaLetter
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.components.ScrapbookInk
import com.example.ui.components.ScrapbookInkSoft
import com.example.ui.components.ScrapbookPaper
import com.example.ui.reference.ReferenceInfo
import com.example.ui.reference.ReferenceProgress
import com.example.ui.reference.RefTab
import kotlinx.coroutines.delay

private val Coral = Color(0xFFFF6B6B)
private val Sun = Color(0xFFFFC93C)
private val Violet = Color(0xFF6C63FF)

/** One letter: how to say it, an example word, the writing video, and tracing practice. */
@Composable
fun ReferenceLetterScreen(tab: Int, startIndex: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val refTab = RefTab.entries[tab.coerceIn(0, RefTab.entries.size - 1)]
    val letters = remember(refTab) { refTab.letters() }
    if (letters.isEmpty()) return
    var index by remember { mutableIntStateOf(startIndex.coerceIn(0, letters.size - 1)) }
    val letter = letters[index]
    val tint = RefTints[index % RefTints.size]
    val word = ReferenceInfo.word(letter)
    val videoUrl = getVideoUrlForLetter(letter).takeIf { refTab == RefTab.Vowels || refTab == RefTab.Consonants }
    var tracing by remember { mutableStateOf(false) }
    // Print worksheet: grown-up sum first, then a preview with Print.
    var showPrintGate by remember { mutableStateOf(false) }
    var showPrintPreview by remember { mutableStateOf(false) }
    val worksheet = remember(refTab, index) { worksheetFor(refTab, index) }

    fun hear() = NativeAudioPlayer.playPhonics(context, letter.character)
    fun hearWord() { word?.let { NativeAudioPlayer.playPhonics(context, it.kannada) } }

    LaunchedEffect(index) {
        delay(400)
        hear()
        delay(2600)
        ReferenceProgress.markPractised(letter)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookPaper)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .readableWidth(680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top bar
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onClick = onBack)
                Text(
                    "${refTab.english} · ${index + 1} of ${letters.size}",
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInkSoft
                )
                RoundIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous letter", enabled = index > 0) { index -= 1 }
                RoundIconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next letter", enabled = index < letters.size - 1) { index += 1 }
            }

            // The letter, how to say it, and an example word
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(tint.second)
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(lerpTint(tint.second))
                        .clickable { hear() }
                        .testTag("ref_big_letter"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        letter.character,
                        fontSize = if (letter.character.length > 1) 58.sp else 72.sp,
                        fontWeight = FontWeight.ExtraBold, color = ScrapbookInk
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (refTab == RefTab.Numbers) letter.transliteration else ReferenceInfo.sound(letter),
                        fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk, lineHeight = 34.sp
                    )
                    Text(ReferenceInfo.tip(letter, refTab), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
                    if (word != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White)
                                .clickable { hearWord() }
                                .padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(word.picture, fontSize = 20.sp)
                            Text(word.kannada, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                            Text(word.english, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
                        }
                    }
                }
            }

            // Writing video
            if (videoUrl != null) {
                key(videoUrl) { WritingVideoCard(videoUrl, letter) }
            }

            // Big buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton("Hear it", Icons.AutoMirrored.Filled.VolumeUp, Coral, Color.White, Modifier.weight(1f)) { hear() }
                BigButton("Trace it", Icons.Default.Create, Sun, ScrapbookInk, Modifier.weight(1f)) { tracing = true }
                if (word != null) {
                    BigButton("The word", Icons.Default.Image, Violet, Color.White, Modifier.weight(1f)) { hearWord() }
                }
            }

            // Printable worksheet for this letter (Ka-kaa forms share their consonant's family sheets): one slim line
            if (worksheet != null) {
                val (_, sheet) = worksheet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .clickable { showPrintGate = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("ref_print_worksheet"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🖨️", fontSize = 16.sp)
                    Text("Practice on paper", modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk, maxLines = 1)
                    if (!com.example.ui.data.Worksheets.isOpen(sheet)) {
                        AccessBadge(com.example.ui.data.Access.State.NeedsPremium, size = 20.dp)
                    }
                    Text("Print ›", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E86DE))
                }
            }

            // Nearby letters
            val from = (index - 2).coerceIn(0, maxOf(0, letters.size - 5))
            val nearby = letters.subList(from, minOf(letters.size, from + 5))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                nearby.forEachIndexed { k, l ->
                    val i = from + k
                    val on = i == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (on) ScrapbookInk else RefTints[i % RefTints.size].first)
                            .clickable { index = i },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            l.character,
                            fontSize = if (l.character.length > 1) 18.sp else 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (on) Color.White else ScrapbookInk
                        )
                    }
                }
            }
        }

        if (showPrintGate) {
            ParentGate(
                purpose = "Answer to print the worksheet",
                onPass = {
                    showPrintGate = false
                    showPrintPreview = true
                },
                onCancel = { showPrintGate = false }
            )
        }
        if (showPrintPreview && worksheet != null) {
            SingleWorksheetPreview(worksheet.first, worksheet.second, onClose = { showPrintPreview = false })
        }

        if (tracing) {
            TracePanel(
                letter = letter,
                onHear = { hear() },
                onDone = {
                    tracing = false
                    SoundEffectsHelper.playSuccess(context)
                    ReferenceProgress.markPractised(letter)
                },
                onClose = { tracing = false }
            )
        }
    }
}

private fun lerpTint(c: Color): Color = Color(
    c.red + (1 - c.red) * 0.55f, c.green + (1 - c.green) * 0.55f, c.blue + (1 - c.blue) * 0.55f, 1f
)

@Composable
private fun RoundIconButton(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = ScrapbookInk.copy(alpha = if (enabled) 1f else 0.35f))
    }
}

@Composable
private fun BigButton(label: String, icon: ImageVector, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        Text(label, color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
    }
}

@Composable
private fun WritingVideoCard(url: String, letter: KannadaLetter) {
    // Plays once; tap (or the play button) to watch it again
    val player = rememberVideoPlayer(url = url, loop = false, autoPlay = true)
    val playing by player.isPlaying
    val buffering by player.isBuffering
    var slow by remember { mutableStateOf(true) }
    LaunchedEffect(slow, player) { player.setSpeed(if (slow) 0.6f else 1f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(2.dp, Color(0xFFF2E6C4), RoundedCornerShape(18.dp))
                .background(Color.White)
        ) {
            VideoSurface(controller = player, showNativeControls = false, modifier = Modifier.fillMaxSize(), whiteBackground = true)
            // Tap layer (play / pause)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (playing) player.pause() else player.play()
                    }
                    .testTag("ref_video_tap"),
                contentAlignment = Alignment.Center
            ) {
                when {
                    buffering -> CircularProgressIndicator(color = Coral)
                    !playing -> Box(
                        Modifier.size(64.dp).background(ScrapbookInk.copy(alpha = 0.85f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play writing video", tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                    else -> Unit
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("How to write ${letter.character}", modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
            Row(
                modifier = Modifier.background(Color(0xFFEAF6FF), RoundedCornerShape(16.dp)).padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(true to "Slow", false to "Normal").forEach { (isSlow, label) ->
                    val on = slow == isSlow
                    Text(
                        label,
                        modifier = Modifier
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (on) ScrapbookInk else Color.Transparent)
                            .clickable { slow = isSlow; if (!playing) player.play() }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        color = if (on) Color.White else ScrapbookInk,
                        fontWeight = FontWeight.ExtraBold, fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/** Full-screen tracing: a faint letter to draw over with a finger. */
@Composable
private fun TracePanel(letter: KannadaLetter, onHear: () -> Unit, onDone: () -> Unit, onClose: () -> Unit) {
    var strokes by remember(letter.id) { mutableStateOf(listOf<List<Offset>>()) }
    var current by remember(letter.id) { mutableStateOf(listOf<Offset>()) }
    val ink = Color(0xFFFF6B6B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookPaper)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            .safeDrawingPadding()
            .readableWidth(760.dp)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Close tracing", onClick = onClose)
            Text("Trace ${letter.character}", modifier = Modifier.weight(1f), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
            RoundIconButton(Icons.AutoMirrored.Filled.VolumeUp, "Hear the letter", onClick = onHear)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .pointerInput(letter.id) {
                    detectDragGestures(
                        onDragStart = { current = listOf(it) },
                        onDrag = { change, _ -> change.consume(); current = current + change.position },
                        onDragEnd = { if (current.isNotEmpty()) strokes = strokes + listOf(current); current = emptyList() },
                        onDragCancel = { current = emptyList() }
                    )
                }
                .testTag("ref_trace_canvas"),
            contentAlignment = Alignment.Center
        ) {
            // The grey letter, as big as fits (sized by its real shape)
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val measurer = androidx.compose.ui.text.rememberTextMeasurer()
                val density = androidx.compose.ui.platform.LocalDensity.current
                val direction = androidx.compose.ui.platform.LocalLayoutDirection.current
                val wPx = with(density) { maxWidth.toPx() }
                val hPx = with(density) { maxHeight.toPx() }
                val fitted = remember(letter.character, wPx, hPx) { fitLetter(measurer, letter.character, wPx, hPx, density, direction) }
                Canvas(Modifier.fillMaxSize()) {
                    drawText(fitted.first, color = Color(0xFFE3EEF8), topLeft = fitted.second)
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                val style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                (strokes + listOf(current)).forEach { s ->
                    if (s.size > 1) {
                        val p = Path().apply {
                            moveTo(s.first().x, s.first().y)
                            s.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(p, ink, style = style)
                    } else if (s.size == 1) {
                        drawCircle(ink, 11.dp.toPx(), s.first())
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(2.dp, ScrapbookInk, RoundedCornerShape(28.dp))
                    .clickable { strokes = emptyList(); current = emptyList() },
                contentAlignment = Alignment.Center
            ) {
                Text("Clear", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = ScrapbookInk)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (strokes.isEmpty()) Color(0xFFB8C7D4) else Color(0xFF2EC4B6))
                    .clickable(enabled = strokes.isNotEmpty()) { onDone() }
                    .testTag("ref_trace_done"),
                contentAlignment = Alignment.Center
            ) {
                Text("I did it!", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = ScrapbookInk)
            }
        }
    }
}
