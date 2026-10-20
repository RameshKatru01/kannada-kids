package com.example.ui.screens

import com.example.platform.*


import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Menu
import com.example.ui.components.PauseMenuDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import com.example.ui.components.drawTextCentered
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ConfettiEffect
import com.example.data.KannadaLetter
import com.example.data.LetterData
import com.example.ui.audio.NativeAudioPlayer
import kotlinx.coroutines.delay

data class ConstellationLevel(
    val letters: List<KannadaLetter>,
    val instruction: String,
    val completeEmoji: String,
    val points: List<Offset>
)

fun getConstellationLevels(): List<ConstellationLevel> {
    val v = LetterData.vowels
    val c = LetterData.consonants
    val n = LetterData.numbers
    
    val levels = mutableListOf<ConstellationLevel>()
    
    // Vowels
    if (v.size >= 5) levels.add(ConstellationLevel(v.subList(0, 5), "Connect ${v[0].character} to ${v[4].character}", "🏠", listOf(Offset(0.2f, 0.8f), Offset(0.2f, 0.4f), Offset(0.5f, 0.1f), Offset(0.8f, 0.4f), Offset(0.8f, 0.8f))))
    if (v.size >= 10) levels.add(ConstellationLevel(v.subList(5, 10), "Connect ${v[5].character} to ${v[9].character}", "👑", listOf(Offset(0.2f, 0.3f), Offset(0.3f, 0.7f), Offset(0.5f, 0.4f), Offset(0.7f, 0.7f), Offset(0.8f, 0.3f))))
    if (v.size >= 15) levels.add(ConstellationLevel(v.subList(10, 15), "Connect ${v[10].character} to ${v[14].character}", "💎", listOf(Offset(0.5f, 0.2f), Offset(0.8f, 0.4f), Offset(0.7f, 0.8f), Offset(0.3f, 0.8f), Offset(0.2f, 0.4f))))
    else if (v.size > 10) levels.add(ConstellationLevel(v.subList(10, v.size), "Connect ${v[10].character} to ${v[v.size-1].character}", "💎", listOf(Offset(0.5f, 0.2f), Offset(0.8f, 0.4f), Offset(0.7f, 0.8f), Offset(0.3f, 0.8f), Offset(0.2f, 0.4f)).subList(0, v.size - 10)))
    
    // Consonants
    if (c.size >= 5) levels.add(ConstellationLevel(c.subList(0, 5), "Connect ${c[0].character} to ${c[4].character}", "⛰️", listOf(Offset(0.1f, 0.8f), Offset(0.3f, 0.3f), Offset(0.5f, 0.6f), Offset(0.7f, 0.2f), Offset(0.9f, 0.8f))))
    if (c.size >= 10) levels.add(ConstellationLevel(c.subList(5, 10), "Connect ${c[5].character} to ${c[9].character}", "🎀", listOf(Offset(0.2f, 0.3f), Offset(0.2f, 0.7f), Offset(0.8f, 0.3f), Offset(0.8f, 0.7f), Offset(0.5f, 0.5f))))
    if (c.size >= 15) levels.add(ConstellationLevel(c.subList(10, 15), "Connect ${c[10].character} to ${c[14].character}", "☕", listOf(Offset(0.2f, 0.3f), Offset(0.3f, 0.7f), Offset(0.7f, 0.7f), Offset(0.8f, 0.3f), Offset(0.9f, 0.5f))))
    if (c.size >= 20) levels.add(ConstellationLevel(c.subList(15, 20), "Connect ${c[15].character} to ${c[19].character}", "⚡", listOf(Offset(0.5f, 0.1f), Offset(0.3f, 0.5f), Offset(0.7f, 0.5f), Offset(0.4f, 0.9f), Offset(0.45f, 0.85f))))
    if (c.size >= 25) levels.add(ConstellationLevel(c.subList(20, 25), "Connect ${c[20].character} to ${c[24].character}", "☂️", listOf(Offset(0.2f, 0.6f), Offset(0.5f, 0.3f), Offset(0.8f, 0.6f), Offset(0.5f, 0.6f), Offset(0.5f, 0.9f))))
    if (c.size >= 30) levels.add(ConstellationLevel(c.subList(25, 30), "Connect ${c[25].character} to ${c[29].character}", "🐟", listOf(Offset(0.8f, 0.8f), Offset(0.5f, 0.2f), Offset(0.2f, 0.5f), Offset(0.5f, 0.8f), Offset(0.8f, 0.2f))))
    if (c.size >= 34) levels.add(ConstellationLevel(c.subList(30, 34), "Connect ${c[30].character} to ${c[33].character}", "🟦", listOf(Offset(0.3f, 0.3f), Offset(0.7f, 0.3f), Offset(0.7f, 0.7f), Offset(0.3f, 0.7f))))
    
    // Numbers
    if (n.size >= 5) levels.add(ConstellationLevel(n.subList(0, 5), "Connect ${n[0].character} to ${n[4].character}", "⭐", listOf(Offset(0.5f, 0.1f), Offset(0.8f, 0.9f), Offset(0.1f, 0.4f), Offset(0.9f, 0.4f), Offset(0.2f, 0.9f))))
    if (n.size >= 10) levels.add(ConstellationLevel(n.subList(5, 10), "Connect ${n[5].character} to ${n[9].character}", "🌲", listOf(Offset(0.5f, 0.1f), Offset(0.3f, 0.6f), Offset(0.7f, 0.6f), Offset(0.5f, 0.6f), Offset(0.5f, 0.9f))))
    
    return levels
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstellationPathGameScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    val langCode = com.example.ui.data.LanguageManager.currentLanguage.value.code
    val constellationLevels = remember(langCode) { getConstellationLevels() }
    
    var currentLevelIndex by remember { mutableIntStateOf(0) }
    // Ensure index is within bounds if language changes
    if (currentLevelIndex >= constellationLevels.size) {
        currentLevelIndex = 0
    }
    val currentLevel = constellationLevels[currentLevelIndex]
    val letters = currentLevel.letters
    
    var connectedIndices by remember { mutableStateOf(emptyList<Int>()) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    var isComplete by remember { mutableStateOf(false) }
    var rewardCharacter by remember(currentLevelIndex) { mutableStateOf<String?>(null) }
    var showSurpriseSticker by remember { mutableStateOf(false) }
    


    // A pulsing glow for the next expected star
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LaunchedEffect(currentLevelIndex) {
        NativeAudioPlayer.playPhonics(context, currentLevel.instruction)
    }

    LaunchedEffect(connectedIndices.size) {
        if (connectedIndices.size == letters.size) {
            try { com.example.ui.data.ScoreManager.addCoins(context, "constellation_path", 20) } catch (e: Exception) {}
            isComplete = true
            showSurpriseSticker = true
            com.example.ui.audio.SoundEffectsHelper.playGameCompletion(context)
        }
    }

    var showPauseMenu by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Level ${currentLevelIndex + 1}: ${currentLevel.instruction}", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F2027))
            )
        },
        containerColor = Color(0xFF0F2027)
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF0F2027))
        ) {
            val canvasWidth = maxWidth.value * LocalDensity.current.density
            val canvasHeight = maxHeight.value * LocalDensity.current.density
            
            val starPositions = remember(canvasWidth, canvasHeight, currentLevelIndex) {
                currentLevel.points.map {
                    Offset(it.x * canvasWidth, it.y * canvasHeight)
                }
            }

            val textMeasurer = rememberTextMeasurer()

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentLevelIndex) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            var currentPos = down.position
                            
                            fun checkCollision(pos: Offset, density: Float) {
                                val nextIndex = connectedIndices.size
                                if (nextIndex < letters.size) {
                                    val targetPos = starPositions[nextIndex]
                                    val hitRadius = 80f * density // Scaled hit radius
                                    
                                    // Also check line intersection between last connected point and target point
                                    // But distance check is usually enough if hitRadius is large enough.
                                    if ((pos - targetPos).getDistance() < hitRadius) {
                                        connectedIndices = connectedIndices + nextIndex
                                        rewardCharacter = letters[nextIndex].character
                                        com.example.ui.audio.SoundEffectsHelper.playSuccess(context)
                                        NativeAudioPlayer.playPhonics(context, letters[nextIndex].character)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            }
                            
                            val currentDensity = density
                            checkCollision(currentPos, currentDensity)
                            dragPosition = currentPos
                            
                            do {
                                val event = awaitPointerEvent()
                                val anyPressed = event.changes.any { it.pressed }
                                if (anyPressed) {
                                    val newPos = event.changes.first().position
                                    dragPosition = newPos
                                    
                                    // Check intermediate points in case of fast dragging
                                    val steps = 5
                                    for (i in 1..steps) {
                                        val interpPos = Offset(
                                            currentPos.x + (newPos.x - currentPos.x) * (i.toFloat() / steps),
                                            currentPos.y + (newPos.y - currentPos.y) * (i.toFloat() / steps)
                                        )
                                        checkCollision(interpPos, currentDensity)
                                    }
                                    
                                    currentPos = newPos
                                }
                            } while (anyPressed)
                            
                            dragPosition = null
                        }
                    }
            ) {
                // Draw background stars
                val random = kotlin.random.Random(42)
                for (i in 0 until 50) {
                    val rx = random.nextFloat() * size.width
                    val ry = random.nextFloat() * size.height
                    val rSize = random.nextFloat() * 4f + 2f
                    drawCircle(Color.White.copy(alpha = random.nextFloat() * 0.5f + 0.2f), radius = rSize, center = Offset(rx, ry))
                }

                // Draw completed paths
                if (connectedIndices.isNotEmpty()) {
                    val path = Path()
                    path.moveTo(starPositions[connectedIndices[0]].x, starPositions[connectedIndices[0]].y)
                    for (i in 1 until connectedIndices.size) {
                        path.lineTo(starPositions[connectedIndices[i]].x, starPositions[connectedIndices[i]].y)
                    }
                    // Draw drag line to current finger position
                    if (dragPosition != null && connectedIndices.size < letters.size) {
                        path.lineTo(dragPosition!!.x, dragPosition!!.y)
                    }

                    // Glow effect
                    drawPath(
                        path = path,
                        color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                        style = Stroke(width = 30f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Core laser
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Draw stars
                starPositions.forEachIndexed { index, pos ->
                    val isConnected = connectedIndices.contains(index)
                    val isNext = index == connectedIndices.size
                    
                    val starColor = if (isConnected) Color(0xFFFFD54F) else Color(0xFF90CAF9)
                    val starRadius = if (isNext) 40f + (10f * pulseAlpha) else 30f
                    
                    // Star glow
                    if (isNext || isConnected) {
                        drawCircle(
                            color = starColor.copy(alpha = 0.3f),
                            radius = starRadius * 1.5f,
                            center = pos
                        )
                    }
                    
                    drawCircle(
                        color = starColor,
                        radius = starRadius,
                        center = pos
                    )
                    
                    // The letter
                    drawTextCentered(
                        textMeasurer = textMeasurer,
                        text = letters[index].character,
                        centerX = pos.x,
                        baselineY = pos.y + 20f,
                        textSizePx = 60f,
                        color = if (isConnected) Color.Black else Color.White,
                        bold = true
                    )
                }

                // Draw spaceship at drag position
                if (dragPosition != null && connectedIndices.size < letters.size) {
                    drawTextCentered(
                        textMeasurer = textMeasurer,
                        text = "🚀",
                        centerX = dragPosition!!.x,
                        baselineY = dragPosition!!.y + 30f,
                        textSizePx = 80f
                    )
                }
            }

            if (isComplete) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentLevel.completeEmoji,
                            fontSize = 150.sp,
                            modifier = Modifier.offset(y = (-20).dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        val authPrefs = getSharedPreferences("AppProgress")
                        val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
                        val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)

                        Button(
                            onClick = {
                                if (isGuest) {
                                    Toast.makeText(context, "🔒 Level 1 Complete! Sign in with Gmail to play all constellation levels!", Toast.LENGTH_LONG).show()
                                    onBack()
                                } else if (currentLevelIndex < constellationLevels.size - 1) {
                                    currentLevelIndex++
                                    connectedIndices = emptyList()
                                    isComplete = false
                                } else {
                                    onBack()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                        ) {
                            Text(
                                text = if (isGuest) "Finish (Guest Preview)" else if (currentLevelIndex < constellationLevels.size - 1) "Next Level" else "Finish",
                                color = Color.Black,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }

            if (showSurpriseSticker) {
                val authPrefs = getSharedPreferences("AppProgress")
                val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
                val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)
                com.example.ui.components.SurpriseStickerDialog(
                    userEmail = userEmail,
                    levelName = "Constellation Level ${currentLevelIndex + 1}",
                    seedModifier = currentLevelIndex,
                    onClose = {
                        showSurpriseSticker = false
                        if (isGuest) {
                            onBack()
                        }
                    },
                    onClaimOrNext = {
                        showSurpriseSticker = false
                        if (isGuest) {
                            Toast.makeText(context, "🔒 Level 1 Complete! Sign in with Gmail to play all constellation levels!", Toast.LENGTH_LONG).show()
                            onBack()
                        } else if (currentLevelIndex < constellationLevels.size - 1) {
                            currentLevelIndex++
                            connectedIndices = emptyList()
                            isComplete = false
                        } else {
                            onBack()
                        }
                    }
                )
            }

            // Lottie Animation Reward whenever child correctly identifies a Kannada character
            com.example.ui.components.LottieCharacterRewardOverlay(
                visible = rewardCharacter != null && !isComplete,
                character = rewardCharacter ?: "",
                onDismiss = { rewardCharacter = null }
            )
        }
    }
}
