
package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LetterData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

data class PoppableLetter(
    val id: Int,
    val character: String,
    val x: Float,
    val y: Float,
    var isPopped: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuguriSpinScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var spinSpeed by remember { mutableStateOf(0f) }
    var score by remember { mutableStateOf(0) }
    var showSurpriseSticker by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var poppableLetters by remember { mutableStateOf(listOf<PoppableLetter>()) }

    // Spin animation
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween((1000 / (spinSpeed.coerceAtLeast(0.1f) / 10f).coerceAtLeast(0.1f)).toInt(), easing = LinearEasing)
        ),
        label = "rotation"
    )

    // Decrease spin speed over time
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            if (spinSpeed > 0f) {
                spinSpeed -= 1f
                if (spinSpeed < 0f) spinSpeed = 0f
            } else if (spinSpeed == 0f) {
                // Clear unpopped letters when stopped
                if (poppableLetters.any { !it.isPopped }) {
                    poppableLetters = emptyList()
                }
            }
        }
    }

    // Generate random letters when spinning fast enough
    LaunchedEffect(spinSpeed) {
        if (spinSpeed > 20f) {
            if (Random.nextFloat() > 0.7f && poppableLetters.filter { !it.isPopped }.size < 5) {
                val letter = LetterData.allLetters.random()
                val newLetter = PoppableLetter(
                    id = Random.nextInt(),
                    character = letter.character,
                    x = Random.nextFloat(),
                    y = Random.nextFloat()
                )
                poppableLetters = poppableLetters + newLetter
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buguri Spin", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF44336))
            )
        },
        containerColor = Color(0xFFFFEB3B) // Bright yellow background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            // Can add additional logic if needed
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        // Calculate drag speed and apply to spin
                        val dragSpeed = abs(dragAmount.x) + abs(dragAmount.y)
                        spinSpeed = (spinSpeed + dragSpeed / 2f).coerceAtMost(100f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Background instructional text
            if (spinSpeed == 0f) {
                Text(
                    text = "Swipe fast to spin the Buguri!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.offset(y = (-150).dp)
                )
            }

            // The Buguri (Spinning Top)
            Canvas(modifier = Modifier.size(150.dp)) {
                val actualRotation = if (spinSpeed > 0) rotation else 0f
                rotate(degrees = actualRotation) {
                    val width = size.width
                    val height = size.height

                    // Draw Top (Cone shape)
                    val topPath = Path().apply {
                        moveTo(width / 2, height) // Tip
                        lineTo(width * 0.9f, height * 0.4f)
                        lineTo(width * 0.1f, height * 0.4f)
                        close()
                    }
                    drawPath(topPath, Color(0xFFE91E63))

                    // Draw upper dome
                    val domePath = Path().apply {
                        moveTo(width * 0.1f, height * 0.4f)
                        quadraticBezierTo(width / 2, 0f, width * 0.9f, height * 0.4f)
                        close()
                    }
                    drawPath(domePath, Color(0xFF4CAF50))

                    // Draw peg (bottom tip)
                    drawCircle(Color.Gray, radius = 5f, center = Offset(width / 2, height))

                    // Draw stripes for visual spinning effect
                    val stripePath1 = Path().apply {
                        moveTo(width * 0.3f, height * 0.4f)
                        lineTo(width * 0.4f, height * 0.7f)
                    }
                    drawPath(stripePath1, Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))

                    val stripePath2 = Path().apply {
                        moveTo(width * 0.7f, height * 0.4f)
                        lineTo(width * 0.6f, height * 0.7f)
                    }
                    drawPath(stripePath2, Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                }
            }

            // Poppable Letters
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val boxWidth = maxWidth
                val boxHeight = maxHeight
                
                poppableLetters.filter { !it.isPopped }.forEach { letter ->
                    val xOffset = letter.x * boxWidth.value * 0.8f + boxWidth.value * 0.1f
                    val yOffset = letter.y * boxHeight.value * 0.8f + boxHeight.value * 0.1f
                    
                    Box(
                        modifier = Modifier
                            .offset(x = xOffset.dp, y = yOffset.dp)
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2196F3))
                            .clickable {
                                poppableLetters = poppableLetters.map { 
                                    if (it.id == letter.id) it.copy(isPopped = true) else it 
                                }
                                 score += 10
                                try { com.example.ui.data.ScoreManager.addCoins(context, "buguri_spin", 2) } catch (e: Exception) {}
                                if (score >= 50 && !showSurpriseSticker) {
                                    showSurpriseSticker = true
                                    com.example.ui.audio.SoundEffectsHelper.playGameCompletion(context)
                                }
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter.character,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Score Display
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        text = "Score: $score",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            if (showSurpriseSticker) {
                val authPrefs = getSharedPreferences("AppProgress")
                val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
                val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)
                com.example.ui.components.SurpriseStickerDialog(
                    userEmail = userEmail,
                    levelName = "Buguri Spin Level 1",
                    seedModifier = 15,
                    onClose = {
                        showSurpriseSticker = false
                        if (isGuest) {
                            onBack()
                        }
                    },
                    onClaimOrNext = {
                        showSurpriseSticker = false
                        if (isGuest) {
                            Toast.makeText(context, "🔒 Level 1 Complete! Sign in with Gmail to play all spin levels!", Toast.LENGTH_LONG).show()
                            onBack()
                        }
                    }
                )
            }
        }
    }
}
