package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.readableWidth
import com.example.ui.progress.LevelCompleteOverlay
import com.example.ui.progress.LevelResult

internal val GameInk = Color(0xFF1E2A47)

/**
 * Shared frame for the levelled games: coloured header (title, level, restart), a row of
 * progress stars, the game itself, confetti and the "Level complete" card.
 */
@Composable
internal fun LevelGameScaffold(
    title: String,
    subtitle: String,
    headerColor: Color,
    background: Color,
    rounds: Int,
    roundsDone: Int,
    showConfetti: Boolean,
    levelResult: LevelResult?,
    hasNextLevel: Boolean,
    onNextLevel: () -> Unit,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.fillMaxSize().background(background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RoundHeaderButton({ Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }, onBack)
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                    Text(subtitle, color = Color.White.copy(alpha = 0.92f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                RoundHeaderButton({ Icon(Icons.Default.Refresh, contentDescription = "Start this level again", tint = Color.White) }, onPlayAgain)
            }
            Column(
                Modifier
                    .weight(1f)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .readableWidth(820.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Progress stars
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(rounds) { i ->
                        val done = i < roundsDone
                        val scale by animateFloatAsState(if (done) 1.15f else 1f, spring(dampingRatio = 0.4f), label = "star")
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (done) Color(0xFFFFB300) else Color(0x33000000),
                            modifier = Modifier.size(28.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                content()
            }
        }
        ConfettiAnimation(show = showConfetti)
        levelResult?.let {
            LevelCompleteOverlay(
                result = it,
                onNextLevel = if (hasNextLevel) onNextLevel else null,
                onPlayAgain = onPlayAgain,
                onLevelMap = onBack
            )
        }
    }
}

@Composable
private fun RoundHeaderButton(icon: @Composable () -> Unit, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { icon() }
}

/** Little left-right shake used for wrong answers (0 → no shake). */
@Composable
internal fun rememberShake(trigger: Int): Float {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            for (x in listOf(14f, -12f, 9f, -6f, 3f, 0f)) shake.animateTo(x, tween(45))
        }
    }
    return shake.value
}
