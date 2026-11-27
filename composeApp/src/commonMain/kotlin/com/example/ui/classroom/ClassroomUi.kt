package com.example.ui.classroom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ScrapbookInk
import com.example.ui.components.ScrapbookInkSoft
import com.example.ui.components.ScrapbookPaper
import com.example.ui.components.readableWidth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Saves that must finish even when the child leaves the screen. */
internal val classroomScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

internal val ClassTeal = Color(0xFF1D9E75)
internal val ClassTealSoft = Color(0xFFE1F5EE)
internal val ClassTealInk = Color(0xFF085041)
internal val ClassViolet = Color(0xFF534AB7)
internal val ClassVioletSoft = Color(0xFFEEEDFE)
internal val ClassVioletInk = Color(0xFF3C3489)
internal val ClassAmber = Color(0xFFBA7517)
internal val ClassAmberSoft = Color(0xFFFAEEDA)
internal val ClassAmberInk = Color(0xFF633806)
internal val ClassGreenSoft = Color(0xFFEAF3DE)
internal val ClassGreenInk = Color(0xFF27500A)
internal val ClassRed = Color(0xFFD94848)
internal val ClassRedSoft = Color(0xFFFCEBEB)
internal val ClassLine = Color(0xFFE3EEF8)

/** The scrolling page every classroom screen sits on. */
@Composable
internal fun ClassPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookPaper)
            .safeDrawingPadding()
            .readableWidth(720.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
internal fun BigButton(text: String, color: Color = ClassTeal, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(color)
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun SoftButton(text: String, modifier: Modifier = Modifier, color: Color = ScrapbookInk, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.5.dp, color.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(vertical = 9.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
internal fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, big: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) ClassTealSoft else Color.White)
            .border(if (selected) 2.dp else 1.dp, if (selected) ClassTeal else ClassLine, RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(vertical = if (big) 6.dp else 7.dp, horizontal = if (big) 14.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) ClassTealInk else ScrapbookInk,
            fontSize = if (big) 20.sp else 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun Badge(text: String, bg: Color, fg: Color) {
    Text(
        text,
        modifier = Modifier.background(bg, RoundedCornerShape(999.dp)).padding(horizontal = 9.dp, vertical = 3.dp),
        color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun Muted(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
}

@Composable
internal fun Heading(text: String) {
    Text(text, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
}

@Composable
internal fun ErrorLine(text: String?) {
    if (!text.isNullOrBlank()) Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ClassRed)
}

/** Short message for a failed online call. */
internal fun friendly(e: Throwable): String {
    val m = e.message.orEmpty()
    return when {
        m.contains("PERMISSION_DENIED", true) || m.contains("permission", true) ->
            "Not allowed. (Has the classroom rules update been published in Firebase?)"
        m.contains("Unable to resolve", true) || m.contains("timeout", true) || m.contains("network", true) ->
            "No internet. Check the connection and try again."
        m.isNotBlank() -> m
        else -> "Something went wrong. Try again."
    }
}

/** Task icon text: shown as a big glyph on the task row. */
internal fun taskGlyph(t: HwTask): String = when {
    t.isTrace -> t.ref
    t.isVideo -> "▶"
    else -> com.example.ui.progress.GameRegistry.byId(t.ref.toIntOrNull() ?: -1)?.glyph ?: "★"
}

internal fun taskKind(t: HwTask): String = when {
    t.isTrace -> "Tracing · ${t.reps} times"
    t.isVideo -> "Video"
    else -> {
        val g = com.example.ui.progress.GameRegistry.byId(t.ref.toIntOrNull() ?: -1)
        if (g?.hasLevels == true) "Game · finish a level" else "Game · ${t.minutes} min"
    }
}

internal fun taskColors(t: HwTask): Pair<Color, Color> = when {
    t.isTrace -> ClassTealSoft to ClassTealInk
    t.isVideo -> Color(0xFFE6F1FB) to Color(0xFF185FA5)
    else -> Color(0xFFFBEAF0) to Color(0xFF993556)
}
