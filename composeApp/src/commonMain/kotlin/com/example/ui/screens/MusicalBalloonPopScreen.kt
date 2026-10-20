package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

val balloonColors = listOf(Color.Red, Color.Blue, Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFF9C27B0), Color(0xFFFF9800), Color.Cyan, Color.Magenta)
val kannadaVowels = listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ", "ಊ", "ಋ", "ಎ", "ಏ", "ಐ", "ಒ", "ಓ", "ಔ", "ಅಂ", "ಅಃ")

class BalloonData(
    val id: Int,
    letter: String,
    color: Color,
    xPercent: Float,
    yPercent: Float,
    speed: Float
) {
    var letter by mutableStateOf(letter)
    var color by mutableStateOf(color)
    var xPercent by mutableFloatStateOf(xPercent)
    var yPercent by mutableFloatStateOf(yPercent)
    var speed by mutableFloatStateOf(speed)
    var isPopped by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicalBalloonPopScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var toneGenerator by remember { mutableStateOf<ToneGenerator?>(null) }
    
    val textMeasurer = rememberTextMeasurer()

    var score by remember { mutableIntStateOf(0) }
    var targetLetter by remember { mutableStateOf("") }
    var rewardCharacter by remember { mutableStateOf<String?>(null) }
    var showSurpriseSticker by remember { mutableStateOf(false) }
    
    // Create initial balloons
    val balloons = remember {
        val columns = listOf(0.2f, 0.5f, 0.8f)
        List(8) { i ->
            BalloonData(
                id = i,
                letter = kannadaVowels.random(),
                color = balloonColors.random(),
                xPercent = columns[i % columns.size],
                yPercent = 1.0f + (i * 0.2f),
                speed = Random.nextFloat() * 0.0015f + 0.0015f
            )
        }
    }
    
    DisposableEffect(context) {
        val attrContext = context
        val textToSpeech = TextToSpeech(attrContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("kn", "IN")
            }
        }
        tts = textToSpeech
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        
        targetLetter = balloons.random().letter
        
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
            toneGenerator?.release()
        }
    }
    
    // Periodically call out the target letter
    LaunchedEffect(targetLetter) {
        if (targetLetter.isNotEmpty()) {
            delay(500)
            com.example.ui.audio.NativeAudioPlayer.playPhonics(context, targetLetter)
            
            while(isActive) {
                delay(6000) // Increase delay a bit for native audio
                com.example.ui.audio.NativeAudioPlayer.playPhonics(context, targetLetter)
            }
        }
    }
    
    // Animation loop using withFrameNanos for smooth 60fps
    LaunchedEffect(Unit) {
        while(isActive) {
            withFrameNanos { time ->
                for (b in balloons) {
                    if (!b.isPopped) {
                        b.yPercent -= b.speed
                        
                        // If balloon goes off top
                        if (b.yPercent < -0.2f) {
                            if (b.letter == targetLetter) {
                                score = (score - 5).coerceAtLeast(0)
                            }
                            b.yPercent = 1.2f
                            b.xPercent = listOf(0.2f, 0.5f, 0.8f).random()
                            b.speed = Random.nextFloat() * 0.0015f + 0.0015f
                            b.letter = kannadaVowels.random()
                            b.color = balloonColors.random()
                            // Ensure the target letter is always present
                            if (Random.nextFloat() < 0.25f) {
                                b.letter = targetLetter
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Musical Balloon Pop", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Text(
                        text = "Score: $score",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2196F3))
            )
        },
        containerColor = Color(0xFFE3F2FD) // Light blue sky
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Target Indicator
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Pop the balloon with:", fontSize = 18.sp, color = Color.Gray)
                    Text(
                        text = targetLetter, 
                        fontSize = 48.sp, 
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }
            }
            
            // Balloons Canvas
            val density = LocalDensity.current
            
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(targetLetter) { // Re-bind when target changes
                        detectTapGestures { tapOffset ->
                            val width = size.width
                            val height = size.height
                            val balloonRadius = 55.dp.toPx()
                            
                            var tappedBalloon: BalloonData? = null
                            
                            for (i in balloons.indices.reversed()) {
                                val b = balloons[i]
                                if (!b.isPopped) {
                                    val bx = b.xPercent * width
                                    val by = b.yPercent * height
                                    
                                    val dist = kotlin.math.hypot((tapOffset.x - bx).toDouble(), (tapOffset.y - by).toDouble())
                                    if (dist <= balloonRadius * 1.5f) { // Slightly larger hit area
                                        tappedBalloon = b
                                        break
                                    }
                                }
                            }
                            
                            tappedBalloon?.let { b ->
                                if (b.letter == targetLetter) {
                                    // Correct!
                                    score += 10
                                    try { com.example.ui.data.ScoreManager.addCoins(context, "musical_balloon", 5) } catch (e: Exception) {}
                                    b.isPopped = true
                                    rewardCharacter = b.letter
                                    com.example.ui.audio.SoundEffectsHelper.playPop(context)
                                    if (score >= 50 && !showSurpriseSticker) {
                                        com.example.ui.audio.SoundEffectsHelper.playGameCompletion(context)
                                        showSurpriseSticker = true
                                    } else if (score % 50 == 0) {
                                        com.example.ui.audio.SoundEffectsHelper.playGameCompletion(context)
                                    } else {
                                        com.example.ui.audio.SoundEffectsHelper.playSuccess(context)
                                    }
                                    
                                    // Respawn the popped balloon somewhere at the bottom later
                                    b.yPercent = 1.2f
                                    b.xPercent = listOf(0.2f, 0.5f, 0.8f).random()
                                    b.speed = Random.nextFloat() * 0.0015f + 0.0015f
                                    b.letter = kannadaVowels.random()
                                    b.color = balloonColors.random()
                                    b.isPopped = false
                                    
                                    // Make sure new target is on a balloon
                                    val unpopped = balloons.filter { !it.isPopped }
                                    if (unpopped.isNotEmpty()) {
                                        targetLetter = unpopped.random().letter
                                    } else {
                                        targetLetter = kannadaVowels.random()
                                    }
                                    
                                    com.example.ui.audio.NativeAudioPlayer.playPhonics(context, "ಸರಿ")
                                    
                                } else {
                                    // Wrong! Boing sound
                                    score = (score - 5).coerceAtLeast(0)
                                    com.example.ui.audio.SoundEffectsHelper.playOops(context)
                                    // TTS can say "Boing"
                                    com.example.ui.audio.NativeAudioPlayer.playPhonics(context, "ಬೋಯಿಂಗ್")
                                }
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val balloonRadius = 55.dp.toPx()
                
                for (b in balloons) {
                    if (!b.isPopped) {
                        val cx = b.xPercent * width
                        val cy = b.yPercent * height
                        
                        // Draw string
                        val stringPath = Path().apply {
                            moveTo(cx, cy + balloonRadius)
                            quadraticBezierTo(
                                cx - 10.dp.toPx(), cy + balloonRadius + 20.dp.toPx(),
                                cx + 10.dp.toPx(), cy + balloonRadius + 40.dp.toPx()
                            )
                            quadraticBezierTo(
                                cx - 10.dp.toPx(), cy + balloonRadius + 60.dp.toPx(),
                                cx, cy + balloonRadius + 80.dp.toPx()
                            )
                        }
                        drawPath(stringPath, Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                        
                        // Draw balloon (oval shape slightly)
                        drawCircle(
                            color = b.color,
                            radius = balloonRadius,
                            center = Offset(cx, cy)
                        )
                        
                        // Draw highlight for 3D effect
                        drawCircle(
                            color = Color.White.copy(alpha = 0.4f),
                            radius = balloonRadius * 0.3f,
                            center = Offset(cx - balloonRadius * 0.4f, cy - balloonRadius * 0.4f)
                        )
                        
                        // Draw text
                        val textLayoutResult = textMeasurer.measure(
                            text = b.letter,
                            style = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(
                                cx - textLayoutResult.size.width / 2,
                                cy - textLayoutResult.size.height / 2
                            )
                        )
                    }
                }
            }

            // Lottie Animation Reward whenever child correctly identifies a Kannada character
            com.example.ui.components.LottieCharacterRewardOverlay(
                visible = rewardCharacter != null,
                character = rewardCharacter ?: "",
                onDismiss = { rewardCharacter = null }
            )

            if (showSurpriseSticker) {
                val authPrefs = getSharedPreferences("AppProgress")
                val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
                val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)
                com.example.ui.components.SurpriseStickerDialog(
                    userEmail = userEmail,
                    levelName = "Musical Balloon Pop Level 1",
                    seedModifier = 12,
                    onClose = {
                        showSurpriseSticker = false
                        if (isGuest) {
                            onBack()
                        }
                    },
                    onClaimOrNext = {
                        showSurpriseSticker = false
                        if (isGuest) {
                            Toast.makeText(context, "🔒 Level 1 Complete! Sign in with Gmail to play all balloon pop levels!", Toast.LENGTH_LONG).show()
                            onBack()
                        }
                    }
                )
            }
        }
    }
}
