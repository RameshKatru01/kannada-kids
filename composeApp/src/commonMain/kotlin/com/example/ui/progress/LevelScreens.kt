package com.example.ui.progress

import com.example.ui.components.readableWidth
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.data.ScoreManager
import kotlinx.coroutines.delay

internal val ProgressInk = Color(0xFF0B3B66)
internal val ProgressInkSoft = Color(0xFF3A5F82)
internal val StarGold = Color(0xFFFFB300)
internal val StarEmpty = Color(0xFFC9D8E2)

private val starPoints = listOf(
    12f to 2f, 15f to 9f, 22f to 9.3f, 16.5f to 14f, 18.5f to 21f,
    12f to 17f, 5.5f to 21f, 7.5f to 14f, 2f to 9.3f, 9f to 9f
)

/** The rounded 5-point star used across the levels UI. */
@Composable
fun StarIcon(size: Dp, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 24f
        val path = Path().apply {
            starPoints.forEachIndexed { i, (x, y) ->
                if (i == 0) moveTo(x * s, y * s) else lineTo(x * s, y * s)
            }
            close()
        }
        drawPath(path, color)
    }
}

/** "★ 1,240 points" pill. */
@Composable
fun PointsPill(showWord: Boolean = true) {
    val total by ScoreManager.totalCoins.collectAsState()
    Row(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StarIcon(18.dp, StarGold)
        Text(
            text = if (showWord) "${formatPoints(total)} points" else formatPoints(total),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = ProgressInk
        )
    }
}

fun formatPoints(n: Int): String {
    val s = n.toString()
    val out = StringBuilder()
    s.forEachIndexed { i, c ->
        if (i > 0 && (s.length - i) % 3 == 0 && c.isDigit() && s[i - 1].isDigit()) out.append(',')
        out.append(c)
    }
    return out.toString()
}

@Composable
private fun RoundBackButton(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable { onBack() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ProgressInk)
    }
}

// ---------------------------------------------------------------------------------------------
// Level map
// ---------------------------------------------------------------------------------------------

/** Bubble positions (top-left of a 110-wide column) on a 350 x 600 design map, level 1 at the bottom. */
private val mapPositions = listOf(35f to 485f, 195f to 400f, 55f to 295f, 180f to 185f, 45f to 95f, 185f to 5f)

@Composable
fun LevelMapScreen(gameId: Int, onPlayLevel: (Int) -> Unit, onBack: () -> Unit) {
    val game = GameRegistry.byId(gameId) ?: return
    val version by GameProgress.version.collectAsState()
    val stars = remember(version, gameId) { GameProgress.starsList(game) }
    val next = remember(version, gameId) { GameProgress.nextLevel(game) }
    val allDone = stars.all { it > 0 }
    val background = lerpColor(game.color, Color.White, 0.86f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .safeDrawingPadding()
            .readableWidth(600.dp)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundBackButton(onBack)
            Column(modifier = Modifier.weight(1f)) {
                Text(game.name, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = ProgressInk, lineHeight = 26.sp)
                Text(
                    "${game.subtitle} · ${stars.sum()} of ${game.maxStars} stars",
                    fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ProgressInkSoft
                )
            }
            PointsPill(showWord = false)
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val scale = minOf(maxWidth.value / 350f, maxHeight.value / 600f)
            val mapW = (350f * scale).dp
            val mapH = (600f * scale).dp
            val pathColor = lerpColor(game.color, Color.White, 0.45f)
            Box(modifier = Modifier.size(mapW, mapH)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val k = this.size.width / 350f
                    val p = Path().apply {
                        moveTo(90 * k, 520 * k)
                        cubicTo(160 * k, 500 * k, 240 * k, 480 * k, 250 * k, 435 * k)
                        cubicTo(260 * k, 390 * k, 110 * k, 380 * k, 110 * k, 330 * k)
                        cubicTo(110 * k, 280 * k, 236 * k, 280 * k, 236 * k, 230 * k)
                        cubicTo(236 * k, 180 * k, 100 * k, 180 * k, 100 * k, 130 * k)
                        cubicTo(100 * k, 80 * k, 230 * k, 80 * k, 240 * k, 40 * k)
                    }
                    drawPath(
                        p, pathColor,
                        style = Stroke(
                            width = 10 * k,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2 * k, 18 * k))
                        )
                    )
                }
                for (i in 0 until game.levels.coerceAtMost(mapPositions.size)) {
                    val level = i + 1
                    val (x, y) = mapPositions[i]
                    val access = com.example.ui.data.Access.level(game, level)
                    val unlocked = access == com.example.ui.data.Access.State.Open
                    Box(
                        modifier = Modifier
                            .offset(x = (x * scale).dp, y = (y * scale).dp)
                            .width((110f * scale).dp.coerceAtLeast(96.dp))
                    ) {
                        LevelBubble(
                            level = level,
                            stars = stars[i],
                            current = unlocked && level == next && !(allDone && stars[i] > 0),
                            locked = !unlocked,
                            crown = access == com.example.ui.data.Access.State.NeedsPremium,
                            game = game,
                            onClick = {
                                when (access) {
                                    com.example.ui.data.Access.State.Open -> onPlayLevel(level)
                                    com.example.ui.data.Access.State.NeedsPremium ->
                                        com.example.ui.data.Access.askGrownUp("Level $level and more fun levels are in the Gold pack.")
                                    com.example.ui.data.Access.State.NeedsProgress ->
                                        com.example.ui.data.Access.showHint("Get a star on level ${level - 1} first, then level $level opens!")
                                }
                            }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(ProgressInk)
                .clickable { onPlayLevel(next) }
                .testTag("play_next_level"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(
                if (allDone) "Play again for 3 stars" else "Play level $next",
                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun LevelBubble(level: Int, stars: Int, current: Boolean, locked: Boolean, crown: Boolean = false, game: GameInfo, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val ring by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring"
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when {
            current -> {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(104.dp)
                            .scale(ring)
                            .background(Color(0x40FF6B6B), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF6B6B))
                            .border(6.dp, Color.White, CircleShape)
                            .clickable { onClick() }
                            .testTag("level_bubble_$level"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$level", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp)
                    }
                }
                Text(
                    if (stars > 0) "Play again!" else "Play next!",
                    color = Color(0xFFD94848), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp
                )
            }
            locked -> {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (crown) Color(0xFFFFF1C4) else Color(0xFFD7E3EA))
                        .border(4.dp, if (crown) Color(0xFFFFC93C) else Color.White, CircleShape)
                        .clickable { onClick() }
                        .testTag("level_bubble_$level"),
                    contentAlignment = Alignment.Center
                ) {
                    if (crown) Text("👑", fontSize = 28.sp)
                    else Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFF6A8297))
                }
                Text("Level $level", color = Color(0xFF6A8297), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            else -> {
                val done = stars > 0
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(if (done) game.color else Color.White)
                        .border(4.dp, if (done) Color.White else game.color, CircleShape)
                        .clickable { onClick() }
                        .testTag("level_bubble_$level"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$level",
                        color = if (done) game.ink else ProgressInk,
                        fontWeight = FontWeight.ExtraBold, fontSize = 28.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(3) { k -> StarIcon(20.dp, if (k < stars) StarGold else StarEmpty) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Level complete card (shown on top of a game)
// ---------------------------------------------------------------------------------------------

@Composable
fun LevelCompleteOverlay(
    result: LevelResult,
    onNextLevel: (() -> Unit)?,
    onPlayAgain: () -> Unit,
    onLevelMap: () -> Unit
) {
    val game = GameRegistry.byId(result.gameId)
    // Stars pop in one after another
    val shown = remember(result) { List(3) { Animatable(0f) } }
    LaunchedEffect(result) {
        delay(250)
        for (i in 0 until 3) {
            if (i < result.stars) {
                shown[i].animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
            } else {
                shown[i].snapTo(1f)
            }
        }
    }

    // A happy face that bounces, and a big cheer that matches the stars
    val bounce = rememberInfiniteTransition(label = "cheer")
    val hop by bounce.animateFloat(0f, 1f, infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "hop")
    val (face, cheer) = when (result.stars) {
        3 -> "🏆" to "Super star!"
        2 -> "🥳" to "Hooray!"
        else -> "👏" to "Yay, you did it!"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE62EC4B6))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            .safeDrawingPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .background(Color(0xFFFFFBEF), RoundedCornerShape(36.dp))
                .border(4.dp, Color(0xFFFFC93C), RoundedCornerShape(36.dp))
                .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                face,
                fontSize = 64.sp,
                modifier = Modifier.offset(y = (-10 * hop).dp).scale(1f + 0.06f * hop)
            )
            Text(
                cheer,
                fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF6B6B),
                lineHeight = 40.sp, textAlign = TextAlign.Center
            )
            Text(
                "${game?.name ?: "Level"} · level ${result.level} done!",
                modifier = Modifier.background(Color(0xFFE0F7F4), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 5.dp),
                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF168F84), textAlign = TextAlign.Center
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0 until 3) {
                    val big = i == 1
                    StarIcon(
                        size = if (big) 84.dp else 62.dp,
                        color = if (i < result.stars) StarGold else StarEmpty,
                        modifier = Modifier
                            .padding(bottom = if (big) 14.dp else 0.dp)
                            .scale(shown[i].value)
                    )
                }
            }
            Text(result.message, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = ProgressInk, textAlign = TextAlign.Center)

            // One big, simple reward instead of a table of points
            Row(
                modifier = Modifier
                    .background(Color(0xFFFFF0C2), RoundedCornerShape(24.dp))
                    .border(2.dp, Color(0xFFFFC93C), RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StarIcon(26.dp, StarGold)
                Text("+${result.pointsEarned} points", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB86E00))
            }

            Spacer(Modifier.height(2.dp))
            if (onNextLevel != null) {
                PrimaryPill("Next level", Color(0xFFFF6B6B), Modifier.testTag("next_level"), onNextLevel)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinePill("🔁 Play again", Modifier.weight(1f), onPlayAgain)
                    OutlinePill("🗺️ Levels", Modifier.weight(1f), onLevelMap)
                }
            } else {
                Text(
                    "You finished every level! 🎉",
                    fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF168F84)
                )
                PrimaryPill("Levels", Color(0xFFFF6B6B), Modifier, onLevelMap)
                OutlinePill("🔁 Play again", Modifier.fillMaxWidth(), onPlayAgain)
            }
        }
    }
}

@Composable
private fun PrimaryPill(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(color)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
    }
}

@Composable
private fun OutlinePill(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .border(2.dp, ProgressInk, RoundedCornerShape(25.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = ProgressInk, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

internal fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f
)
