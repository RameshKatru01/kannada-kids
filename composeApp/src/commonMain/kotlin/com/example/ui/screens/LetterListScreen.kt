package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KannadaLetter
import com.example.data.LetterData
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.BrightGreen
import com.example.ui.theme.RainbowColors
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.AuthState
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterListScreen(
    type: String,
    onBack: () -> Unit,
    onNavigateToGameFlow: (String, Int) -> Unit,
    authViewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val isVowels = type == "vowels"
    val items = when(type) { "vowels" -> LetterData.vowels; "consonants" -> LetterData.consonants; "numbers" -> LetterData.numbers; else -> emptyList() }
    val title = when(type) { "vowels" -> "Vowels"; "consonants" -> "Consonants"; "numbers" -> "Numbers"; else -> "" }
    val topBarColor = when(type) { "vowels" -> BrightCyan; "consonants" -> BrightGreen; "numbers" -> Color(0xFFFF9800); else -> BrightCyan }
    
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    
    val authState by authViewModel.authState.collectAsState()
    val userEmail = (authState as? AuthState.Authenticated)?.email ?: "guest"
    val prefs = getSharedPreferences("AppProgress")
    val currentLanguage by com.example.ui.data.LanguageManager.currentLanguage.collectAsState()
    val langCode = currentLanguage.code
    val subscriptionPlan = prefs.getString("${langCode}_subscriptionPlan_$userEmail", "Free") ?: "Free"
    
    val progressUnlocked = when(type) {
        "vowels" -> prefs.getInt("${userEmail}_vowels_unlocked", 0)
        "consonants" -> prefs.getInt("${userEmail}_consonants_unlocked", 0)
        "numbers" -> prefs.getInt("${userEmail}_numbers_unlocked", 0)
        else -> 0
    }
    
    val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)
    val highestUnlocked = if (isGuest) 0 else if (subscriptionPlan == "Gold") items.size else progressUnlocked
    
    DisposableEffect(context) {
        var textToSpeech: TextToSpeech? = null
        val attrContext = context
        textToSpeech = TextToSpeech(attrContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.Builder().setLanguage("kn").setRegion("IN").build()
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor)
            )
        }
    ) { paddingValues ->
        LetterAdventureMap(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            items = items,
            highestUnlocked = highestUnlocked,
            onLetterClick = { index, letter ->
                com.example.ui.audio.NativeAudioPlayer.playPhonics(context, letter.character)
                onNavigateToGameFlow(type, index)
            }
        )
    }
}

@Composable
fun LetterAdventureMap(
    modifier: Modifier = Modifier,
    items: List<KannadaLetter>,
    highestUnlocked: Int,
    onLetterClick: (Int, KannadaLetter) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "path_animation")
    val pathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "pathPhase"
    )

    // A colorful background based on modulo math
    val bgColors = listOf(Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFFFD54F), Color(0xFFBA68C8))

    BoxWithConstraints(modifier = modifier.fillMaxWidth().background(bgColors[items.size % bgColors.size].copy(alpha = 0.5f))) {
        val mapWidth = maxWidth
        val stepHeight = 160.dp
        val totalHeight = (stepHeight * (items.size + 1)).coerceAtLeast(maxHeight)
        
        // Scroll state needs to be scrolled to the bottom (first letter)
        val scrollState = rememberScrollState(
            initial = Int.MAX_VALUE
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .height(totalHeight)
        ) {
            // Draw vibrant background features
            Canvas(modifier = Modifier.fillMaxSize()) {
                val riverCount = (size.height / 1000f).toInt() + 1
                for(i in 0..riverCount) {
                    val riverY = i * 1000f
                    drawPath(
                        path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, riverY)
                            quadraticTo(size.width * 0.5f, riverY + 300f, size.width, riverY - 100f)
                            lineTo(size.width, riverY + 300f)
                            quadraticTo(size.width * 0.5f, riverY + 700f, 0f, riverY + 200f)
                            close()
                        },
                        color = Color(0xFF4FC3F7).copy(alpha = 0.4f)
                    )
                }
            }

            // Calculate positions from BOTTOM to TOP
            val positions = items.mapIndexed { index, _ ->
                val yOffset = totalHeight - (stepHeight * (index + 1)) - 80.dp
                val xSine = kotlin.math.sin(index * 1.5)
                val xOffset = (mapWidth / 2) + (xSine * (mapWidth.value * 0.25)).dp - 40.dp
                Pair(xOffset, yOffset)
            }

            // Decorate with emojis
            val decorations = listOf("🌲", "🌳", "🌺", "🌸", "🦋", "🍄", "🌷", "🌻", "🍎", "🦜")
            positions.forEachIndexed { i, pos ->
                val x = pos.first
                val y = pos.second
                // Randomly place decorations around the path
                val side = if (i % 2 == 0) -80.dp else 120.dp
                Text(
                    text = decorations[i % decorations.size],
                    fontSize = 48.sp,
                    modifier = Modifier.offset(x = x + side, y = y + 20.dp)
                )
            }

            // Draw connected paths
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0 until positions.size - 1) {
                    val start = positions[i]
                    val end = positions[i + 1]
                    
                    val startX = start.first.toPx() + 40.dp.toPx()
                    val startY = start.second.toPx() + 40.dp.toPx()
                    val endX = end.first.toPx() + 40.dp.toPx()
                    val endY = end.second.toPx() + 40.dp.toPx()
                    
                    val controlX1 = startX
                    val controlY1 = startY + (endY - startY) / 2
                    val controlX2 = endX
                    val controlY2 = startY + (endY - startY) / 2
                    
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(startX, startY)
                        cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)
                    }
                    
                    val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(30f, 20f), phase = pathPhase * 2f)
                    val isUnlocked = i < highestUnlocked
                    val pathColor = if (isUnlocked) Color(0xFF795548) else Color.LightGray
                    val pathInnerColor = if (isUnlocked) RainbowColors[i % RainbowColors.size] else Color.Gray

                    drawPath(
                        path = path,
                        color = pathColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 24.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = pathEffect
                        )
                    )
                    drawPath(
                        path = path,
                        color = pathInnerColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 16.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = pathEffect
                        )
                    )
                }
            }

            // Draw nodes and decorations
            positions.forEachIndexed { index, (x, y) ->
                val letter = items[index]
                val isUnlocked = index <= highestUnlocked
                val rainbowColor = if (isUnlocked) RainbowColors[index % RainbowColors.size] else Color.Gray

                val mapContext = LocalContext.current
                Box(
                    modifier = Modifier
                        .offset(x = x, y = y)
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(rainbowColor)
                        .clickable {
                            if (isUnlocked) {
                                onLetterClick(index, letter)
                            } else {
                                Toast.makeText(mapContext, "🔒 Locked: Sign in with Gmail to unlock all letters!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .border(4.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isUnlocked) {
                        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.DarkGray, modifier = Modifier.size(32.dp))
                    } else {
                        Text(
                            text = letter.character,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )
                    }
                }
                
                if (isUnlocked) {
                    Text(
                        text = letter.transliteration,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .offset(x = x + 10.dp, y = y + 90.dp)
                            .background(Color(0xFF37474F).copy(alpha=0.8f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
