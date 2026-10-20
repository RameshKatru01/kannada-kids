package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.rememberTextMeasurer
import com.example.ui.components.drawTextCentered
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightCyan
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.AuthState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin

data class RunnerEntity(
    val id: Int,
    val type: EntityType,
    val text: String,
    val lane: Int,
    var y: Float,
    var caught: Boolean = false
)

enum class EntityType {
    LETTER, OBSTACLE
}

class BGMPlayer {
    private var handle: Any? = null

    fun play() {
        if (handle != null) return
        handle = PlatformAudio.startLoop(melody, SAMPLE_RATE)
    }

    fun stop() {
        PlatformAudio.stopLoop(handle)
        handle = null
    }

    companion object {
        private const val SAMPLE_RATE = 44100

        /** One loop of the cheerful background tune (same notes as the original Android version). */
        private val melody: ShortArray by lazy {
            val notes = listOf(261.63, 329.63, 392.00, 523.25, 392.00, 329.63)
            val samplesPerNote = SAMPLE_RATE / 4
            val buffer = ShortArray(samplesPerNote * notes.size)
            var t = 0.0
            var i = 0
            for (freq in notes) {
                for (sampleCount in 0 until samplesPerNote) {
                    val envelope = 1.0 - (sampleCount.toDouble() / samplesPerNote)
                    val sample = sin(2.0 * PI * freq * t) * envelope * 6000
                    buffer[i++] = sample.toInt().toShort()
                    t += 1.0 / SAMPLE_RATE
                }
            }
            buffer
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterRunnerScreen(onBack: () -> Unit, authViewModel: AuthViewModel = viewModel { AuthViewModel() }) {
    val bgm = remember { BGMPlayer() }
    DisposableEffect(Unit) {
        bgm.play()
        onDispose {
            bgm.stop()
        }
    }
    
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val userEmail = (authState as? AuthState.Authenticated)?.email ?: "guest"
    val prefs = remember { getSharedPreferences("GamePrefs") }
    var highScore by remember { mutableIntStateOf(prefs.getInt("${userEmail}_runner_high_score", 0)) }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }

    val runnerLevels = listOf(
        listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ", "ಊ", "ಋ", "ಎ", "ಏ", "ಐ", "ಒ", "ಓ", "ಔ", "ಅಂ", "ಅಃ"), // Vowels
        listOf("ಕ", "ಖ", "ಗ", "ಘ", "ಙ", "ಚ", "ಛ", "ಜ", "ಝ", "ಞ", "ಟ", "ಠ", "ಡ", "ಢ", "ಣ", "ತ", "ಥ", "ದ", "ಧ", "ನ", "ಪ", "ಫ", "ಬ", "ಭ", "ಮ", "ಯ", "ರ", "ಲ", "ವ", "ಶ", "ಷ", "ಸ", "ಹ", "ಳ"), // Consonants
        listOf("ಕ", "ಕಾ", "ಕಿ", "ಕೀ", "ಕು", "ಕೂ", "ಕೃ", "ಕೆ", "ಕೇ", "ಕೈ", "ಕೊ", "ಕೋ", "ಕೌ", "ಕಂ", "ಕಃ"), // Guninthakshari
        listOf("೧", "೨", "೩", "೪", "೫", "೬", "೭", "೮", "೯", "೧೦") // Numbers
    )
    var currentLevelIndex by remember { mutableIntStateOf(0) }
    val currentSequence = runnerLevels[currentLevelIndex]

    var currentTargetIndex by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var gameWon by remember { mutableStateOf(false) }
    var levelComplete by remember { mutableStateOf(false) }
    var showSurpriseSticker by remember { mutableStateOf(false) }

    var boyLane by remember { mutableIntStateOf(1) } // 0: left, 1: center, 2: right

    var entities by remember { mutableStateOf(listOf<RunnerEntity>()) }
    var distance by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isGameOver, gameWon, levelComplete, currentLevelIndex) {
        if (isGameOver || gameWon || levelComplete) return@LaunchedEffect
        
        var lastSpawnDistance = 0f
        var entityIdCounter = 0
        
        while(isActive) {
            withFrameNanos { time ->
                distance += 0.005f 
                
                if (distance - lastSpawnDistance > 0.5f) { 
                    lastSpawnDistance = distance
                    
                    val letterIdx = currentTargetIndex + entities.count { it.type == EntityType.LETTER && !it.caught }
                    if (letterIdx < currentSequence.size) {
                        val lane = (0..2).random()
                        
                        if (kotlin.random.Random.nextDouble() > 0.7) {
                            val obsLane = (0..2).filter { it != lane }.random()
                            entities = entities + RunnerEntity(
                                id = entityIdCounter++,
                                type = EntityType.OBSTACLE,
                                text = listOf("🌲", "🪨", "🚧").random(),
                                lane = obsLane,
                                y = -0.1f
                            )
                        }
                        
                        entities = entities + RunnerEntity(
                            id = entityIdCounter++,
                            type = EntityType.LETTER,
                            text = currentSequence[letterIdx],
                            lane = lane,
                            y = -0.1f
                        )
                    }
                }
                
                val newEntities = entities.map { it.copy(y = it.y + 0.005f) }.toMutableList()
                val boyY = 0.8f 
                
                val iterator = newEntities.iterator()
                while (iterator.hasNext()) {
                    val entity = iterator.next()
                    if (entity.y > 1.1f) {
                        iterator.remove()
                        continue
                    }
                    
                    if (kotlin.math.abs(entity.y - boyY) < 0.08f) {
                        if (entity.lane == boyLane && !entity.caught) {
                            if (entity.type == EntityType.OBSTACLE) {
                                entity.caught = true
                                lives--
                                if (lives <= 0) isGameOver = true
                            } else if (entity.type == EntityType.LETTER) {
                                entity.caught = true
                                score += 10
                                if (score > highScore) {
                                    highScore = score
                                    prefs.edit().putInt("${userEmail}_runner_high_score", highScore).apply()
                                }
                                currentTargetIndex++
                                if (currentTargetIndex >= currentSequence.size) {
                                    showSurpriseSticker = true
                                    if (currentLevelIndex < runnerLevels.size - 1) {
                                        levelComplete = true
                                    } else {
                                        gameWon = true
                                    }
                                }
                            }
                        }
                    } else if (entity.y > boyY + 0.08f) { // passed boy position
                         if (entity.type == EntityType.LETTER && !entity.caught) {
                            entity.caught = true
                            lives--
                            if (lives <= 0) isGameOver = true
                         }
                    }
                }
                
                entities = newEntities
            }
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF81C784))
                .pointerInput(Unit) {
                    var totalDragX = 0f
                    detectDragGestures(
                        onDragStart = { 
                            totalDragX = 0f 
                        },
                        onDragEnd = {
                            if (totalDragX > 50 && boyLane < 2) boyLane++
                            else if (totalDragX < -50 && boyLane > 0) boyLane--
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                    }
                }
        ) {
            val textMeasurer = rememberTextMeasurer()
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                
                val laneWidth = width * 0.8f / 3
                val roadStartX = width * 0.1f
                
                val turnOffset = sin(distance * 1.5f) * (width * 0.15f)
                
                translate(left = turnOffset) {
                    drawRect(
                        color = Color(0xFF9E9E9E),
                        topLeft = Offset(roadStartX, 0f),
                        size = androidx.compose.ui.geometry.Size(width * 0.8f, height)
                    )
                    
                    for (i in 1..2) {
                        val lineX = roadStartX + laneWidth * i
                        var yOffset = (distance * height) % 100f
                        while (yOffset < height + 100f) {
                            drawLine(
                                color = Color.White,
                                start = Offset(lineX, yOffset - 100f),
                                end = Offset(lineX, yOffset - 50f),
                                strokeWidth = 10f
                            )
                            yOffset += 100f
                        }
                    }

                    entities.forEach { entity ->
                        if (!entity.caught) {
                            val ex = roadStartX + (entity.lane + 0.5f) * laneWidth
                            val ey = entity.y * height
                            
                            if (entity.type == EntityType.LETTER) {
                                drawOval(
                                    color = Color(0x64000000),
                                    topLeft = Offset(ex - 40f, ey + 20f),
                                    size = androidx.compose.ui.geometry.Size(80f, 20f)
                                )
                                drawTextCentered(textMeasurer, entity.text, ex, ey - 40f, 120f, Color.Black)
                            } else {
                                drawTextCentered(textMeasurer, entity.text, ex, ey + 40f, 120f, Color.Black)
                            }
                        }
                    }
                    
                    val bx = roadStartX + (boyLane + 0.5f) * laneWidth
                    val by = 0.8f * height
                    
                    drawOval(
                        color = Color(0x64000000),
                        topLeft = Offset(bx - 50f, by + 20f),
                        size = androidx.compose.ui.geometry.Size(100f, 20f)
                    )
                    drawTextCentered(textMeasurer, "🏃", bx, by + 40f, 140f, Color.Black)
                }
            }
            
            
            // Minimal back button overlaid
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(16.dp)
                    .background(Color(0x88000000), androidx.compose.foundation.shape.CircleShape)
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 72.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0x88000000), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val levelNames = listOf("Vowels", "Consonants", "Guninthakshari", "Numbers")
                        Text(text = "L${currentLevelIndex + 1}: ${levelNames[currentLevelIndex]}", fontSize = 12.sp, color = Color.White)
                        val target = currentSequence.getOrNull(currentTargetIndex) ?: "End"
                        Text(
                            text = "Find: $target",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(Color(0x88000000), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "⭐ $score", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = "❤️".repeat(lives.coerceAtLeast(0)), fontSize = 12.sp, color = Color.White)
                    }
                }
            }
            
            if (isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Game Over!", fontSize = 48.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                isGameOver = false
                                currentLevelIndex = 0
                                currentTargetIndex = 0
                                distance = 0f
                                boyLane = 1
                                score = 0
                                lives = 3
                                entities = emptyList()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrightCyan),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(60.dp).padding(horizontal = 32.dp)
                        ) {
                            Text("Try Again", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            if (levelComplete) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Level Complete! 🎉", fontSize = 48.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))
                        val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)
                        Button(
                            onClick = {
                                if (isGuest) {
                                    Toast.makeText(context, "🔒 Level 1 Complete! Sign in with Gmail to unlock all runner levels!", Toast.LENGTH_LONG).show()
                                    onBack()
                                } else {
                                    levelComplete = false
                                    currentLevelIndex++
                                    currentTargetIndex = 0
                                    distance = 0f
                                    boyLane = 1
                                    entities = emptyList()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrightCyan),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(60.dp).padding(horizontal = 32.dp)
                        ) {
                            Text(if (isGuest) "Finish (Guest Preview)" else "Next Level", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (gameWon) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("You Won! 🎉", fontSize = 48.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = BrightCyan),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(60.dp).padding(horizontal = 32.dp)
                        ) {
                            Text("Awesome!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (showSurpriseSticker) {
                com.example.ui.components.SurpriseStickerDialog(
                    userEmail = userEmail,
                    levelName = "Runner Level ${currentLevelIndex + 1}",
                    seedModifier = currentLevelIndex,
                    onClose = {
                        showSurpriseSticker = false
                    },
                    onClaimOrNext = {
                        showSurpriseSticker = false
                        if (currentLevelIndex < runnerLevels.size - 1) {
                            levelComplete = false
                            currentLevelIndex++
                            currentTargetIndex = 0
                            distance = 0f
                            boyLane = 1
                            entities = emptyList()
                        } else {
                            onBack()
                        }
                    }
                )
            }
        }
    }
}
