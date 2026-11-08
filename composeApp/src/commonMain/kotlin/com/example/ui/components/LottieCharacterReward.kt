package com.example.ui.components

import com.example.platform.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

/**
 * Delightful Lottie Animation Reward component that celebrates when a child
 * correctly identifies a Kannada character in any interactive game.
 */
@Composable
fun LottieCharacterRewardOverlay(
    visible: Boolean,
    character: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "ಅದ್ಭುತ! (Awesome!)",
    subtitle: String = "ಸರಿಯಾದ ಅಕ್ಷರ! • Correct Letter!",
    durationMillis: Long = 1800L
) {
    if (!visible || character.isEmpty()) return

    LaunchedEffect(character, visible) {
        if (visible) {
            delay(durationMillis)
            onDismiss()
        }
    }

    // Star-burst celebration (drawn with Compose so it works on Android and iOS)
    val burst = remember(character) { Animatable(0f) }
    LaunchedEffect(character, visible) {
        burst.snapTo(0f)
        burst.animateTo(1f, animationSpec = tween(durationMillis = 1300, easing = LinearOutSlowInEasing))
    }
    val progress = burst.value

    // Pulsing scale animation for the Kannada character badge
    val infiniteTransition = rememberInfiniteTransition(label = "reward_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(150f)
            .background(Color.Black.copy(alpha = 0.40f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        // Main Reward Card
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.85f)
                .wrapContentHeight()
                .shadow(24.dp, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(4.dp, Color(0xFFFFD54F)) // Golden amber border
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFFDE7), Color.White, Color(0xFFF0FDF4))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header badge
                Surface(
                    color = Color(0xFFFFE082),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, Color(0xFFFFB300))
                ) {
                    Text(
                        text = "⭐ SUPER STAR! ⭐",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Lottie Animation Container with Kannada Character Overlay
                Box(
                    modifier = Modifier
                        .size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Lottie Motion Graphic Celebration
                    StarBurst(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Big Glowing Badge displaying the identified Kannada character
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(92.dp)
                            .shadow(12.dp, CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
                                ),
                                shape = CircleShape
                            )
                            .border(4.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = character,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title Praise
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle
                Text(
                    text = subtitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF388E3C),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tap to continue hint
                Text(
                    text = "👉 Tap anywhere to continue",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}


/** Expanding ring of sparkling stars, used in place of the Lottie "star_sparkle" animation. */
@Composable
private fun StarBurst(progress: () -> Float, modifier: Modifier = Modifier) {
    val colors = remember {
        listOf(Color(0xFFFFD54F), Color(0xFFFF9800), Color(0xFFFF4081), Color(0xFF40C4FF), Color(0xFF69F0AE))
    }
    Canvas(modifier = modifier) {
        val p = progress()
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f
        val alpha = (1f - p).coerceIn(0f, 1f)
        val rings = 2
        for (ring in 0 until rings) {
            val count = 10 + ring * 4
            val ringRadius = maxRadius * (0.35f + 0.65f * p) * (1f - ring * 0.25f)
            for (i in 0 until count) {
                val angle = (2.0 * PI * i / count + ring * 0.3 + p * 0.8).toFloat()
                val pos = Offset(center.x + cos(angle) * ringRadius, center.y + sin(angle) * ringRadius)
                val starSize = maxRadius * (0.09f - ring * 0.025f) * (0.6f + 0.4f * (1f - p))
                drawStar(pos, starSize, colors[(i + ring) % colors.size].copy(alpha = alpha))
            }
        }
        // Soft glow behind the letter badge
        drawCircle(
            color = Color(0xFFFFF59D).copy(alpha = 0.45f * alpha),
            radius = maxRadius * (0.45f + 0.35f * p),
            center = center
        )
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    val points = 5
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val angle = (PI / points * i - PI / 2).toFloat()
        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}
