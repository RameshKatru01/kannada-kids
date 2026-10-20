package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.components.ConfettiEffect
import com.example.ui.components.SurpriseStickerDialog
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.BrightGreen
import com.example.ui.theme.BrightOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LevelType { MULTIPLE_CHOICE, DRAG_DROP, BALLOON_POP, BOSS }

data class AdventureLevel(
    val levelNumber: Int,
    val title: String,
    val type: LevelType,
    val wordPrefix: String,
    val wordSuffix: String,
    val correctVowel: String,
    val options: List<String>,
    val hasTimer: Boolean = false
)

val adventureLevels = listOf(
    AdventureLevel(1, "The First Step", LevelType.MULTIPLE_CHOICE, "", "ಟ (Ata)", "ಅ", listOf("ಅ", "ಆ")),
    AdventureLevel(2, "Long & Short", LevelType.MULTIPLE_CHOICE, "", "ಗ (Eega)", "ಈ", listOf("ಇ", "ಈ", "ಉ")),
    AdventureLevel(3, "The Forest of U", LevelType.DRAG_DROP, "", "ಟ (Uta)", "ಊ", listOf("ಉ", "ಊ", "ಋ")),
    AdventureLevel(4, "Curve & Twist", LevelType.MULTIPLE_CHOICE, "", "ಷಿ (Rushi)", "ಋ", listOf("ಋ", "ಎ", "ಏ"), hasTimer = true),
    AdventureLevel(5, "Midpoint Challenge", LevelType.MULTIPLE_CHOICE, "", "ಣಿ (Yeni)", "ಏ", listOf("ಎ", "ಏ", "ಐ")),
    AdventureLevel(6, "Circle Time", LevelType.BALLOON_POP, "", "ಂಟೆ (Onte)", "ಒ", listOf("ಒ", "ಓ", "ಔ")),
    AdventureLevel(7, "The Grand Vowel", LevelType.DRAG_DROP, "", "ಷಧ (Oushadha)", "ಔ", listOf("ಒ", "ಓ", "ಔ")),
    AdventureLevel(8, "Special Symbols", LevelType.MULTIPLE_CHOICE, "", "ಶ (Amsha)", "ಅಂ", listOf("ಅಂ", "ಅಃ", "ಅ")),
    AdventureLevel(9, "Speed Sprint", LevelType.BALLOON_POP, "", "ಕ (Eka)", "ಏ", listOf("ಎ", "ಏ", "ಐ"), hasTimer = true),
    AdventureLevel(10, "Master of Swaragalu", LevelType.BOSS, "", "", "", emptyList())
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwaraAdventureScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var currentLevelIndex by remember { mutableIntStateOf(0) }
    val level = adventureLevels[currentLevelIndex]

    val sfx = remember { SoundEffects() }
    var showSuccessBurst by remember { mutableStateOf(false) }
    var showSurpriseSticker by remember { mutableStateOf(false) }
    var rewardCharacter by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose { sfx.release() }
    }

    fun onLevelComplete() {
        showSuccessBurst = true
        sfx.playCorrectTile()
        if (level.correctVowel.isNotEmpty()) {
            rewardCharacter = level.correctVowel
            NativeAudioPlayer.playPhonics(context, level.correctVowel)
        }
        try { com.example.ui.data.ScoreManager.addCoins(context, "swara_adventure", 15) } catch (e: Exception) {}

        coroutineScope.launch {
            delay(1200)
            showSuccessBurst = false
            showSurpriseSticker = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Level ${level.levelNumber}: ${level.title}", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrightCyan)
            )
        },
        containerColor = Color(0xFFF1F8E9) // Clean light lime background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showSuccessBurst) {
                ConfettiEffect(modifier = Modifier.fillMaxSize())
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (level.type == LevelType.BOSS) {
                    BossLevelView(
                        onVictory = { onLevelComplete() },
                        onBack = onBack,
                        sfx = sfx
                    )
                } else {
                    GameLevelView(
                        level = level,
                        onCorrect = { onLevelComplete() },
                        sfx = sfx
                    )
                }
            }

            // Google Pay Style Surprise Sticker Dialog
            if (showSurpriseSticker) {
                val authPrefs = getSharedPreferences("AppProgress")
                val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
                val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)
                SurpriseStickerDialog(
                    userEmail = userEmail,
                    levelName = "Swara Adventure Level ${level.levelNumber}",
                    seedModifier = currentLevelIndex,
                    onClose = {
                        showSurpriseSticker = false
                        if (isGuest) {
                            onBack()
                        } else if (currentLevelIndex < adventureLevels.size - 1) {
                            currentLevelIndex++
                        }
                    },
                    onClaimOrNext = {
                        showSurpriseSticker = false
                        if (isGuest) {
                            Toast.makeText(context, "🔒 Level 1 Complete! Sign in with Gmail to play all adventure levels!", Toast.LENGTH_LONG).show()
                            onBack()
                        } else if (currentLevelIndex < adventureLevels.size - 1) {
                            currentLevelIndex++
                        } else {
                            onBack()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GameLevelView(level: AdventureLevel, onCorrect: () -> Unit, sfx: SoundEffects) {
    var timeLeft by remember(level) { mutableIntStateOf(12) }
    var isFailed by remember(level) { mutableStateOf(false) }
    var placedVowel by remember(level) { mutableStateOf<String?>(null) }

    val shakeAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    if (level.hasTimer && !isFailed && placedVowel == null) {
        LaunchedEffect(level) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            if (timeLeft == 0 && placedVowel == null) {
                isFailed = true
                sfx.playIncorrectTile()
            }
        }
    }

    fun handleWrong() {
        sfx.playIncorrectTile()
        coroutineScope.launch {
            shakeAnim.animateTo(20f, tween(50))
            shakeAnim.animateTo(-20f, tween(50))
            shakeAnim.animateTo(0f, spring())
        }
    }

    fun handleChoice(option: String) {
        if (placedVowel != null) return
        if (option == level.correctVowel) {
            placedVowel = option
            onCorrect()
        } else {
            handleWrong()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.offset(x = shakeAnim.value.dp)
    ) {
        if (level.hasTimer) {
            Surface(
                color = if (timeLeft <= 3) Color(0xFFFFEBEE) else Color(0xFFE0F7FA),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, if (timeLeft <= 3) Color.Red else BrightCyan)
            ) {
                Text(
                    text = "⏱️ $timeLeft s remaining",
                    fontSize = 18.sp,
                    color = if (timeLeft <= 3) Color.Red else Color(0xFF006064),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        if (isFailed) {
            Text("Time's Up!", fontSize = 36.sp, color = Color.Red, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    timeLeft = 12
                    isFailed = false
                    placedVowel = null
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrightOrange),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Try Again 🔄", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            return@Column
        }

        // Word Display Card with blank slot
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (level.wordPrefix.isNotEmpty()) {
                    Text(level.wordPrefix, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                }

                // Blank or Filled Slot
                val isFilled = placedVowel != null
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isFilled) BrightGreen else Color(0xFFECEFF1))
                        .border(3.dp, if (isFilled) BrightGreen else BrightCyan, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = placedVowel ?: "?",
                        fontSize = 42.sp,
                        color = if (isFilled) Color.White else BrightCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (level.wordSuffix.isNotEmpty()) {
                    Text(level.wordSuffix, fontSize = 52.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }

        Text(
            text = when (level.type) {
                LevelType.DRAG_DROP -> "Drag or tap the correct vowel to fill the blank:"
                LevelType.BALLOON_POP -> "Pop the floating balloon with the right vowel! 🎈"
                else -> "Choose the correct vowel to complete the word:"
            },
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF37474F),
            textAlign = TextAlign.Center
        )

        // Interaction Areas
        when (level.type) {
            LevelType.MULTIPLE_CHOICE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    level.options.forEach { option ->
                        Button(
                            onClick = { handleChoice(option) },
                            modifier = Modifier
                                .size(76.dp)
                                .testTag("option_$option"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrightOrange),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Text(option, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            LevelType.DRAG_DROP -> {
                DragDropArea(
                    options = level.options,
                    correctVowel = level.correctVowel,
                    onCorrect = onCorrect,
                    onWrong = ::handleWrong
                )
            }
            LevelType.BALLOON_POP -> {
                BalloonPopArea(
                    options = level.options,
                    correctVowel = level.correctVowel,
                    onCorrect = onCorrect,
                    onWrong = ::handleWrong
                )
            }
            else -> {}
        }
    }
}

@Composable
fun DragDropArea(
    options: List<String>,
    correctVowel: String,
    onCorrect: () -> Unit,
    onWrong: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.padding(top = 16.dp)
    ) {
        options.forEach { option ->
            key(option) {
                var dragOffset by remember { mutableStateOf(Offset.Zero) }
                var isDragging by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(dragOffset.x.toInt(), dragOffset.y.toInt()) }
                        .zIndex(if (isDragging) 10f else 1f)
                        .size(76.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                if (isDragging) listOf(Color(0xFFFFB74D), Color(0xFFFF9800))
                                else listOf(Color(0xFFFF9800), Color(0xFFF57C00))
                            )
                        )
                        .border(2.dp, Color.White, RoundedCornerShape(18.dp))
                        .pointerInput(option) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    dragOffset = Offset.Zero
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount
                                },
                                onDragEnd = {
                                    // If dragged upward towards target or released, check answer
                                    if (dragOffset.y < -50f || dragOffset.getDistance() > 30f) {
                                        if (option == correctVowel) {
                                            onCorrect()
                                        } else {
                                            onWrong()
                                        }
                                    } else {
                                        // Tap gesture
                                        if (option == correctVowel) onCorrect() else onWrong()
                                    }
                                    dragOffset = Offset.Zero
                                    isDragging = false
                                },
                                onDragCancel = {
                                    dragOffset = Offset.Zero
                                    isDragging = false
                                }
                            )
                        }
                        .clickable {
                            // Direct tap support so kids are never stuck
                            if (option == correctVowel) onCorrect() else onWrong()
                        }
                        .testTag("drag_option_$option"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(option, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun BalloonPopArea(
    options: List<String>,
    correctVowel: String,
    onCorrect: () -> Unit,
    onWrong: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "balloon_float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "balloon_anim"
    )

    val balloonColors = listOf(
        listOf(Color(0xFFFF8A80), Color(0xFFD50000)),
        listOf(Color(0xFF80D8FF), Color(0xFF0091EA)),
        listOf(Color(0xFFCCFF90), Color(0xFF64DD17))
    )

    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(top = 16.dp)) {
        options.forEachIndexed { index, option ->
            val colors = balloonColors[index % balloonColors.size]
            var isPopped by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .offset(y = (floatY * (if (index % 2 == 0) 1 else -1)).dp)
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(colors))
                    .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    .clickable {
                        if (!isPopped) {
                            if (option == correctVowel) {
                                isPopped = true
                                onCorrect()
                            } else {
                                onWrong()
                            }
                        }
                    }
                    .testTag("balloon_$option"),
                contentAlignment = Alignment.Center
            ) {
                Text(option, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun BossLevelView(
    onVictory: () -> Unit,
    onBack: () -> Unit,
    sfx: SoundEffects
) {
    // 3 rapid-fire boss quiz rounds
    val bossQuestions = remember {
        listOf(
            Triple("ಗ (Aaga)", "ಆ", listOf("ಅ", "ಆ", "ಇ")),
            Triple("ಲೆ (Ele)", "ಎ", listOf("ಎ", "ಏ", "ಐ")),
            Triple("ಂಟೆ (Onte)", "ಒ", listOf("ಒ", "ಓ", "ಔ"))
        )
    }

    var bossQuestionIndex by remember { mutableIntStateOf(0) }
    var isBossDefeated by remember { mutableStateOf(false) }

    val currentQ = bossQuestions[bossQuestionIndex]

    if (isBossDefeated) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(Color(0xFFFFF9C4), CircleShape)
                    .border(4.dp, Color(0xFFFFD54F), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("👑", fontSize = 72.sp)
            }

            Text(
                "Swara Master Champion! 🏆",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BrightGreen,
                textAlign = TextAlign.Center
            )

            Text(
                "You defeated the Swara Boss and completed all 10 adventure levels!",
                fontSize = 16.sp,
                color = Color(0xFF37474F),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = BrightCyan),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(54.dp)
            ) {
                Text("Back to Games Menu 🎮", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "👑 FINAL BOSS BATTLE 👑",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFD50000)
            )

            // Boss Health / Progress Bar
            Text(
                "Boss Challenge ${bossQuestionIndex + 1} of 3",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100)
            )

            LinearProgressIndicator(
                progress = { (bossQuestionIndex.toFloat() / 3f) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = Color(0xFFD50000),
                trackColor = Color(0xFFFFCDD2)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(14.dp))
                            .border(3.dp, Color(0xFFD50000), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", fontSize = 38.sp, color = Color(0xFFD50000), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(currentQ.first, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text("Select the missing vowel to strike the boss:", fontSize = 15.sp, color = Color(0xFF424242))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                currentQ.third.forEach { opt ->
                    Button(
                        onClick = {
                            if (opt == currentQ.second) {
                                sfx.playCorrectTile()
                                if (bossQuestionIndex < 2) {
                                    bossQuestionIndex++
                                } else {
                                    isBossDefeated = true
                                    onVictory()
                                }
                            } else {
                                sfx.playIncorrectTile()
                            }
                        },
                        modifier = Modifier.size(76.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000))
                    ) {
                        Text(opt, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
