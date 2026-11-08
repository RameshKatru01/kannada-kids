package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * "Scrapbook" look for the Fun Videos and Games lists: slightly tilted photo prints with a strip
 * of coloured tape on top and a caption underneath.
 */

val ScrapbookPaper = Color(0xFFFFF3D6)
val ScrapbookInk = Color(0xFF0B3B66)
val ScrapbookInkSoft = Color(0xFF3A5F82)

private val tilts = listOf(-2.5f, 2f, 1.5f, -2f, 2.5f, -1.5f)
private val tapes = listOf(
    Color(0xD9FFC93C), Color(0xCC2EC4B6), Color(0xCCFF6B6B), Color(0xBF6C63FF), Color(0xCCE84393), Color(0xCC4DA8FF)
)

/** A tilted photo print. [picture] fills the photo area; [caption] goes underneath. */
@Composable
fun PolaroidCard(
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pictureHeight: Int = 124,
    picture: @Composable BoxScope.() -> Unit,
    caption: @Composable ColumnScope.() -> Unit
) {
    val tilt = tilts[index % tilts.size]
    val tape = tapes[(index * 5 + 1) % tapes.size]
    Box(modifier = modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .rotate(tilt)
                .shadow(6.dp, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .clickable { onClick() }
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pictureHeight.dp)
                    .clip(RoundedCornerShape(3.dp)),
                content = picture
            )
            caption()
        }
        // Tape strip
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-9).dp)
                .rotate(if (index % 2 == 0) -4f else 3f)
                .size(width = 56.dp, height = 18.dp)
                .background(tape)
        )
    }
}

/** Small coloured tag with a letter, placed on a photo. */
@Composable
fun BoxScope.LetterTag(glyph: String, color: Color, ink: Color) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(6.dp)
            .heightIn(min = 34.dp)
            .widthIn(min = 34.dp)
            .background(color, RoundedCornerShape(17.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
    }
}

/** Header used by the scrapbook screens: round back button + big title. */
@Composable
fun ScrapbookHeader(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ScrapbookInk)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ScrapbookInk,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

// ---------------------------------------------------------------------------------------------
// Drawn pictures for the games (no image files needed)
// ---------------------------------------------------------------------------------------------

enum class GameArtKind { Monster, Train, Paint, Balloons, Tiles, Grid, Jars, Runner, Letter }

private val ArtInk = Color(0xFF1E2A47)

/** A drawn picture of a game, on a soft [background], with an optional letter. */
@Composable
fun GameArt(kind: GameArtKind, color: Color, background: Color, glyph: String, modifier: Modifier = Modifier, ink: Color = Color.White) {
    Box(modifier = modifier.fillMaxSize().background(background), contentAlignment = Alignment.Center) {
        when (kind) {
            GameArtKind.Letter -> {
                Box(
                    modifier = Modifier.size(84.dp).background(color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(glyph, color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp)
                }
            }
            else -> {
                Box(modifier = Modifier.size(96.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val k = size.width / 100f
                        when (kind) {
                            GameArtKind.Monster -> drawMonsterArt(k, color)
                            GameArtKind.Train -> drawTrainArt(k)
                            GameArtKind.Paint -> drawPaintArt(k)
                            GameArtKind.Balloons -> drawBalloonArt(k)
                            GameArtKind.Tiles -> drawTilesArt(k)
                            GameArtKind.Grid -> drawGridArt(k)
                            GameArtKind.Jars -> drawJarsArt(k)
                            GameArtKind.Runner -> drawRunnerArt(k)
                            GameArtKind.Letter -> Unit
                        }
                    }
                    when (kind) {
                        GameArtKind.Monster -> ArtLetter(glyph, x = 72, y = 68, size = 26, bg = Color(0xFFFFC93C))
                        GameArtKind.Train -> {
                            ArtLetter("ಕ", x = 48, y = 42, size = 0, bg = Color.Transparent)
                            ArtLetter("ಖ", x = 74, y = 42, size = 0, bg = Color.Transparent)
                        }
                        GameArtKind.Balloons -> ArtLetter("ಉ", x = 40, y = 16, size = 0, bg = Color.Transparent, ink = Color.White)
                        GameArtKind.Runner -> ArtLetter("ಅ", x = 72, y = 4, size = 24, bg = Color(0xFFFFC93C))
                        GameArtKind.Tiles -> {
                            ArtLetter("ಆ", x = 6, y = 50, size = 0, bg = Color.Transparent)
                            ArtLetter("ನೆ", x = 38, y = 58, size = 0, bg = Color.Transparent)
                            ArtLetter("ಮ", x = 70, y = 50, size = 0, bg = Color.Transparent)
                        }
                        GameArtKind.Grid -> {
                            ArtLetter("ಮ", x = 9, y = 37, size = 0, bg = Color.Transparent)
                            ArtLetter("ನೆ", x = 38, y = 37, size = 0, bg = Color.Transparent)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

/** A letter placed on the 100x100 drawing grid. size > 0 draws a round badge behind it. */
@Composable
private fun BoxScope.ArtLetter(glyph: String, x: Int, y: Int, size: Int, bg: Color, ink: Color = ArtInk) {
    val box = if (size > 0) size else 24
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = (x * 0.96f).dp, y = (y * 0.96f).dp)
            .size(box.dp)
            .background(bg, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(com.example.data.Lang.script(glyph), color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
    }
}

private fun darker(c: Color, f: Float = 0.75f) = Color(c.red * f, c.green * f, c.blue * f, 1f)
private fun lighter(c: Color, t: Float = 0.6f) =
    Color(c.red + (1 - c.red) * t, c.green + (1 - c.green) * t, c.blue + (1 - c.blue) * t, 1f)

private fun DrawScope.drawMonsterArt(k: Float, body: Color) {
    val horns = Path().apply {
        moveTo(30 * k, 32 * k); lineTo(24 * k, 12 * k); lineTo(42 * k, 27 * k); close()
        moveTo(70 * k, 32 * k); lineTo(76 * k, 12 * k); lineTo(58 * k, 27 * k); close()
    }
    drawPath(horns, darker(body))
    drawOval(body, Offset(15 * k, 26 * k), Size(70 * k, 64 * k))
    drawOval(lighter(body), Offset(30 * k, 59 * k), Size(40 * k, 26 * k))
    drawCircle(Color.White, 9 * k, Offset(37 * k, 46 * k))
    drawCircle(Color.White, 9 * k, Offset(63 * k, 46 * k))
    drawCircle(ArtInk, 4 * k, Offset(39 * k, 47 * k))
    drawCircle(ArtInk, 4 * k, Offset(61 * k, 47 * k))
    val mouth = Path().apply {
        moveTo(35 * k, 60 * k); quadraticTo(50 * k, 80 * k, 65 * k, 60 * k); close()
    }
    drawPath(mouth, ArtInk)
}

private fun DrawScope.drawTrainArt(k: Float) {
    val coral = Color(0xFFFF6B6B)
    drawCircle(Color.White, 6 * k, Offset(16 * k, 16 * k))
    drawCircle(Color.White, 4 * k, Offset(24 * k, 10 * k))
    drawRoundRect(ArtInk, Offset(11 * k, 24 * k), Size(9 * k, 16 * k), CornerRadius(2 * k))
    drawRoundRect(coral, Offset(6 * k, 38 * k), Size(38 * k, 30 * k), CornerRadius(7 * k))
    drawRoundRect(coral, Offset(26 * k, 26 * k), Size(18 * k, 18 * k), CornerRadius(3 * k))
    drawRoundRect(Color.White, Offset(30 * k, 30 * k), Size(10 * k, 9 * k), CornerRadius(2 * k))
    drawRoundRect(Color(0xFFFFC93C), Offset(48 * k, 42 * k), Size(23 * k, 26 * k), CornerRadius(5 * k))
    drawRoundRect(Color(0xFF2EC4B6), Offset(74 * k, 42 * k), Size(22 * k, 26 * k), CornerRadius(5 * k))
    drawRoundRect(ArtInk, Offset(2 * k, 78 * k), Size(96 * k, 4 * k), CornerRadius(2 * k))
    drawCircle(ArtInk, 7 * k, Offset(16 * k, 72 * k))
    drawCircle(ArtInk, 7 * k, Offset(34 * k, 72 * k))
    drawCircle(ArtInk, 5 * k, Offset(59 * k, 72 * k))
    drawCircle(ArtInk, 5 * k, Offset(85 * k, 72 * k))
}

private fun DrawScope.drawPaintArt(k: Float) {
    val blob = Path().apply {
        moveTo(50 * k, 8 * k)
        cubicTo(66 * k, 6 * k, 72 * k, 20 * k, 84 * k, 22 * k)
        cubicTo(96 * k, 25 * k, 92 * k, 42 * k, 82 * k, 46 * k)
        cubicTo(74 * k, 50 * k, 78 * k, 60 * k, 64 * k, 60 * k)
        cubicTo(52 * k, 60 * k, 50 * k, 50 * k, 38 * k, 52 * k)
        cubicTo(24 * k, 55 * k, 12 * k, 46 * k, 16 * k, 34 * k)
        cubicTo(19 * k, 24 * k, 30 * k, 26 * k, 34 * k, 18 * k)
        cubicTo(38 * k, 10 * k, 42 * k, 9 * k, 50 * k, 8 * k)
        close()
    }
    drawPath(blob, Color(0xFFE84393))
    drawCircle(Color.White, 6 * k, Offset(44 * k, 30 * k))
    drawCircle(Color.White, 6 * k, Offset(62 * k, 30 * k))
    drawCircle(ArtInk, 3 * k, Offset(45 * k, 31 * k))
    drawCircle(ArtInk, 3 * k, Offset(61 * k, 31 * k))
    val smile = Path().apply { moveTo(44 * k, 42 * k); quadraticTo(53 * k, 50 * k, 62 * k, 42 * k) }
    drawPath(smile, ArtInk, style = Stroke(width = 3 * k))
    val pots = listOf(Color(0xFFE53935) to Color(0xFFB71C1C), Color(0xFF1E88E5) to Color(0xFF0D47A1), Color(0xFFFDD835) to Color(0xFFF9A825))
    pots.forEachIndexed { i, (c, rim) ->
        val x = (8 + i * 30) * k
        drawRoundRect(c, Offset(x, 68 * k), Size(24 * k, 24 * k), CornerRadius(6 * k))
        drawRoundRect(rim, Offset(x, 66 * k), Size(24 * k, 6 * k), CornerRadius(3 * k))
    }
}

private fun DrawScope.drawBalloonArt(k: Float) {
    val strings = Path().apply {
        moveTo(30 * k, 56 * k); quadraticTo(34 * k, 72 * k, 46 * k, 94 * k)
        moveTo(54 * k, 50 * k); quadraticTo(52 * k, 72 * k, 48 * k, 94 * k)
        moveTo(76 * k, 58 * k); quadraticTo(66 * k, 74 * k, 50 * k, 94 * k)
    }
    drawPath(strings, ArtInk, style = Stroke(width = 2 * k))
    drawOval(Color(0xFFFF6B6B), Offset(11 * k, 17 * k), Size(34 * k, 42 * k))
    drawOval(Color(0xFFFFC93C), Offset(57 * k, 19 * k), Size(34 * k, 42 * k))
    drawOval(Color(0xFF6C63FF), Offset(33 * k, 5 * k), Size(38 * k, 46 * k))
    drawOval(Color.White.copy(alpha = 0.5f), Offset(41 * k, 11 * k), Size(8 * k, 14 * k))
}

/** A child running along a track, with speed lines (Letter Runner). */
private fun DrawScope.drawRunnerArt(k: Float) {
    fun limb(color: Color, width: Float, vararg pts: Pair<Float, Float>) {
        val path = Path().apply {
            moveTo(pts[0].first * k, pts[0].second * k)
            for (i in 1 until pts.size) lineTo(pts[i].first * k, pts[i].second * k)
        }
        drawPath(path, color, style = Stroke(width = width * k, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
    val skin = Color(0xFFF5C08A)
    val shirt = Color(0xFFFF6B6B)
    // Track with lane marks
    drawRoundRect(Color(0xFF59C173), Offset(4 * k, 86 * k), Size(92 * k, 8 * k), CornerRadius(4 * k))
    listOf(10f, 40f, 70f).forEach { x -> limb(Color.White, 2f, x to 90f, (x + 12) to 90f) }
    // Speed lines
    limb(ArtInk, 2.5f, 4f to 40f, 16f to 40f)
    limb(ArtInk, 2.5f, 8f to 52f, 20f to 52f)
    limb(ArtInk, 2.5f, 2f to 64f, 12f to 64f)
    // Back arm and back leg
    limb(Color(0xFFE0A86E), 5f, 48f to 38f, 36f to 46f, 30f to 40f)
    limb(ArtInk, 7f, 46f to 58f, 36f to 68f, 24f to 68f)
    drawCircle(shirt, 4 * k, Offset(22 * k, 69 * k))
    // Body
    limb(shirt, 12f, 50f to 34f, 46f to 58f)
    // Front leg with shoe, front arm
    limb(ArtInk, 7f, 46f to 58f, 58f to 68f, 60f to 82f)
    drawOval(shirt, Offset(57 * k, 79.5f * k), Size(12 * k, 7 * k))
    limb(skin, 5f, 50f to 38f, 62f to 46f, 70f to 38f)
    // Head: face, hair, eye and smile
    drawCircle(skin, 10 * k, Offset(54 * k, 22 * k))
    drawArc(ArtInk, 189f, 162f, false, Offset(43.5f * k, 9.5f * k), Size(21 * k, 21 * k))
    drawCircle(ArtInk, 1.4f * k, Offset(58 * k, 23 * k))
    drawArc(ArtInk, 11f, 75f, false, Offset(54 * k, 23 * k), Size(6 * k, 6 * k), style = Stroke(1.5f * k, cap = androidx.compose.ui.graphics.StrokeCap.Round))
}

private fun DrawScope.drawTilesArt(k: Float) {
    drawCircle(Color(0xFFFFF4E6), 20 * k, Offset(50 * k, 24 * k))
    drawCircle(Color(0xFFFF9F1C), 15 * k, Offset(50 * k, 24 * k), style = Stroke(4 * k))
    listOf(Color(0xFFFF6B6B) to Offset(18 * k, 62 * k), Color(0xFF2EC4B6) to Offset(50 * k, 70 * k), Color(0xFF6C63FF) to Offset(82 * k, 62 * k))
        .forEach { (c, o) ->
            drawCircle(Color.White, 17 * k, o)
            drawCircle(c, 14 * k, o)
        }
}

private fun DrawScope.drawGridArt(k: Float) {
    val colors = listOf(Color(0xFFFFC93C), Color(0xFF4DA8FF), Color(0xFF59C173), Color(0xFFE84393))
    for (r in 0 until 3) for (c in 0 until 3) {
        val found = r == 1 && c < 2
        drawRoundRect(
            if (found) Color(0xFF6C63FF) else colors[(r + c) % colors.size].copy(alpha = 0.45f),
            Offset((6 + c * 30) * k, (8 + r * 30) * k), Size(26 * k, 26 * k), CornerRadius(6 * k)
        )
    }
}

private fun DrawScope.drawJarsArt(k: Float) {
    val stacks = listOf(
        listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFFE53935)),
        listOf(Color(0xFF1E88E5), Color(0xFFFDD835), Color(0xFF1E88E5)),
        listOf(Color(0xFFFDD835), Color(0xFFFDD835), Color(0xFFE53935))
    )
    stacks.forEachIndexed { j, stack ->
        val x = (8 + j * 31) * k
        drawRoundRect(ArtInk.copy(alpha = 0.25f), Offset(x, 14 * k), Size(24 * k, 78 * k), CornerRadius(10 * k), style = Stroke(3 * k))
        stack.forEachIndexed { i, c ->
            drawRoundRect(c, Offset(x + 3 * k, (68 - i * 24) * k), Size(18 * k, 21 * k), CornerRadius(5 * k))
        }
    }
}
