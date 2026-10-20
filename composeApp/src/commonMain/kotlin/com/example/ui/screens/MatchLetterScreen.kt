package com.example.ui.screens

import com.example.platform.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightCyan
import kotlin.math.roundToInt

data class MatchPair(
    val id: Int,
    val letter: String,
    val emoji: String
)

val matchLevels = listOf(
    listOf(
        MatchPair(1, "ಅ", "👑"),
        MatchPair(2, "ಆ", "🐘"),
        MatchPair(3, "ಇ", "🐁"),
        MatchPair(4, "ಈ", "🧅"),
        MatchPair(5, "ಉ", "🎁")
    ),
    listOf(
        MatchPair(6, "ಊ", "🧶"),
        MatchPair(7, "ಋ", "🐂"),
        MatchPair(8, "ಎ", "🍃"),
        MatchPair(9, "ಏ", "🦀"),
        MatchPair(10, "ಐ", "👁️")
    ),
    listOf(
        MatchPair(11, "ಒ", "🐪"),
        MatchPair(12, "ಓ", "📖"),
        MatchPair(13, "ಔ", "💊"),
        MatchPair(14, "ಅಂ", "🍇"),
        MatchPair(15, "ಅಃ", "🌅")
    ),
    listOf(
        MatchPair(16, "ಕ", "🪷"),
        MatchPair(17, "ಖ", "🐦"),
        MatchPair(18, "ಗ", "🦜"),
        MatchPair(19, "ಘ", "🕰️"),
        MatchPair(20, "ಙ", "🐚")
    ),
    listOf(
        MatchPair(21, "ಚ", "🥄"),
        MatchPair(22, "ಛ", "☂️"),
        MatchPair(23, "ಜ", "🕷️"),
        MatchPair(24, "ಝ", "🐟"),
        MatchPair(25, "ಞ", "🎭")
    ),
    listOf(
        MatchPair(26, "ಟ", "🍅"),
        MatchPair(27, "ಠ", "🥶"),
        MatchPair(28, "ಡ", "🥁"),
        MatchPair(29, "ಢ", "🛡️"),
        MatchPair(30, "ಣ", "🏹")
    ),
    listOf(
        MatchPair(31, "ತ", "⚖️"),
        MatchPair(32, "ಥ", "🍽️"),
        MatchPair(33, "ದ", "🐄"),
        MatchPair(34, "ಧ", "💰"),
        MatchPair(35, "ನ", "🦚")
    ),
    listOf(
        MatchPair(36, "ಪ", "🕊️"),
        MatchPair(37, "ಫ", "🍎"),
        MatchPair(38, "ಬ", "🦆"),
        MatchPair(39, "ಭ", "🐻"),
        MatchPair(40, "ಮ", "🐒")
    ),
    listOf(
        MatchPair(41, "ಯ", "⚙️"),
        MatchPair(42, "ರ", "🐎"),
        MatchPair(43, "ಲ", "🍋"),
        MatchPair(44, "ವ", "💎"),
        MatchPair(45, "ಶ", "🐇")
    ),
    listOf(
        MatchPair(46, "ಷ", "🐝"),
        MatchPair(47, "ಸ", "🦁"),
        MatchPair(48, "ಹ", "🦢"),
        MatchPair(49, "ಳ", "🔒"),
        MatchPair(50, "ಕ್ಷ", "⚔️")
    )
)

fun getWordForLetter(letter: String): String {
    return when(letter) {
        "ಅ" -> "ಅರಸ"
        "ಆ" -> "ಆನೆ"
        "ಇ" -> "ಇಲಿ"
        "ಈ" -> "ಈರುಳ್ಳಿ"
        "ಉ" -> "ಉಡುಗೊರೆ"
        "ಊ" -> "ಉಣ್ಣೆ"
        "ಋ" -> "ಋಷಭ"
        "ಎ" -> "ಎಲೆ"
        "ಏ" -> "ಏಡಿ"
        "ಐ" -> "ಐನೋಟ"
        "ಒ" -> "ಒಂಟೆ"
        "ಓ" -> "ಓದು"
        "ಔ" -> "ಔಷಧ"
        "ಅಂ" -> "ಅಂಗೂರು"
        "ಕ" -> "ಕಮಲ"
        "ಖ" -> "ಖಗ"
        "ಗ" -> "ಗಿಣಿ"
        "ಘ" -> "ಘಂಟೆ"
        "ಚ" -> "ಚಮಚ"
        "ಛ" -> "ಛತ್ರಿ"
        "ಜ" -> "ಜೇಡ"
        "ಝ" -> "ಝಷ"
        "ಟ" -> "ಟೊಮೇಟೊ"
        "ಠ" -> "ಠಳ"
        "ಡ" -> "ಡಮರು"
        "ಢ" -> "ಢಾಲು"
        "ಣ" -> "ಬಾಣ"
        "ತ" -> "ತಕ್ಕಡಿ"
        "ಥ" -> "ಥಟ್ಟೆ"
        "ದ" -> "ದನ"
        "ಧ" -> "ಧನ"
        "ನ" -> "ನವಿಲು"
        "ಪ" -> "ಪಾರಿವಾಳ"
        "ಫ" -> "ಫಲ"
        "ಬ" -> "ಬಾತುಕೋಳಿ"
        "ಭ" -> "ಭಲ್ಲೂಕ"
        "ಮ" -> "ಮಂಗ"
        "ಯ" -> "ಯಂತ್ರ"
        "ರ" -> "ರಥ"
        "ಲ" -> "ಲೆಮನ್"
        "ವ" -> "ವಜ್ರ"
        "ಶ" -> "ಶಶ"
        "ಷ" -> "ಷಟ್ಪದ"
        "ಸ" -> "ಸಿಂಹ"
        "ಹ" -> "ಹಂಸ"
        "ಳ" -> "ತಾಳ"
        "ಕ್ಷ" -> "ಕ್ಷತ್ರಿಯ"
        else -> letter
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchLetterScreen(onBack: () -> Unit) {
    var level by remember { mutableIntStateOf(1) }
    
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    
    DisposableEffect(context) {
        val attrContext = context
        val textToSpeech = TextToSpeech(attrContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("kn", "IN")
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }
    
    // 5 items per level
    val currentPairs = remember(level) {
        val index = (level - 1) % matchLevels.size
        matchLevels[index].shuffled().take(5)
    }
    
    val shuffledPictures = remember(currentPairs) { currentPairs.shuffled() }
    val shuffledLetters = remember(currentPairs) { currentPairs.shuffled() }
    
    var matchedIds by remember(currentPairs) { mutableStateOf(setOf<Int>()) }
    var targetRects by remember(currentPairs) { mutableStateOf(mapOf<Int, Rect>()) }
    var dragOffsets by remember(currentPairs) { mutableStateOf(mapOf<Int, Offset>()) }
    
    var showSuccess by remember { mutableStateOf(false) }
    var rewardCharacter by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(matchedIds) {
        if (matchedIds.size == currentPairs.size && currentPairs.isNotEmpty()) {
            showSuccess = true
            com.example.ui.audio.SoundEffectsHelper.playGameCompletion(context)
            try { com.example.ui.data.ScoreManager.addCoins(context, "match_letter", 10) } catch (e: Exception) {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match the Letter", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrightCyan)
            )
        },
        containerColor = Color(0xFFE8F5E9)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Drag the letter to its picture!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (showSuccess) {
                val authPrefs = getSharedPreferences("AppProgress")
                val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
                val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)
                com.example.ui.components.SurpriseStickerDialog(
                    userEmail = userEmail,
                    levelName = "Match Level $level",
                    seedModifier = level,
                    onClose = {
                        showSuccess = false
                        if (isGuest) {
                            onBack()
                        }
                    },
                    onClaimOrNext = {
                        showSuccess = false
                        if (isGuest) {
                            Toast.makeText(context, "🔒 Level 1 Complete! Sign in with Gmail to play all matching levels!", Toast.LENGTH_LONG).show()
                            onBack()
                        } else {
                            level++
                        }
                    }
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Pictures (Targets)
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        shuffledPictures.forEach { pair ->
                            val isMatched = matchedIds.contains(pair.id)
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .shadow(4.dp, RoundedCornerShape(16.dp))
                                    .background(if (isMatched) Color(0xFFC8E6C9) else Color.White, RoundedCornerShape(16.dp))
                                    .border(2.dp, if (isMatched) Color(0xFF4CAF50) else Color.LightGray, RoundedCornerShape(16.dp))
                                    .clickable {
                                        val word = getWordForLetter(pair.letter)
                                        com.example.ui.audio.NativeAudioPlayer.playPhonics(context, word)
                                    }
                                    .onGloballyPositioned { coords ->
                                        val bounds = coords.boundsInRoot()
                                        if (targetRects[pair.id] != bounds) {
                                            targetRects = targetRects + (pair.id to bounds)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isMatched) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(pair.emoji, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(pair.letter, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    }
                                } else {
                                    Text(pair.emoji, fontSize = 48.sp)
                                }
                            }
                        }
                    }
                    
                    // Spacer
                    Spacer(modifier = Modifier.width(32.dp))
                    
                    // Letters (Draggables)
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        shuffledLetters.forEach { pair ->
                            val isMatched = matchedIds.contains(pair.id)
                            val offset = dragOffsets[pair.id] ?: Offset.Zero
                            var localLayoutRect by remember { mutableStateOf(Rect.Zero) }
                            
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .zIndex(if (offset != Offset.Zero) 1f else 0f)
                                    .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                                    .alpha(if (isMatched) 0f else 1f)
                                    .shadow(if (isMatched) 0.dp else 8.dp, RoundedCornerShape(36.dp))
                                    .background(if (isMatched) Color.Transparent else Color(0xFFFFCC80), RoundedCornerShape(36.dp))
                                    .onGloballyPositioned { coords ->
                                        localLayoutRect = coords.boundsInRoot()
                                    }
                                    .pointerInput(isMatched) {
                                        if (isMatched) return@pointerInput
                                        detectTapGestures(
                                            onTap = {
                                                com.example.ui.audio.SoundEffectsHelper.playButtonTap(context)
                                                com.example.ui.audio.NativeAudioPlayer.playPhonics(context, pair.letter)
                                            }
                                        )
                                    }
                                    .pointerInput(isMatched) {
                                        if (isMatched) return@pointerInput
                                        detectDragGestures(
                                            onDragStart = {
                                                com.example.ui.audio.NativeAudioPlayer.playPhonics(context, pair.letter)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val current = dragOffsets[pair.id] ?: Offset.Zero
                                                dragOffsets = dragOffsets + (pair.id to (current + dragAmount))
                                            },
                                            onDragEnd = {
                                                val dropCenter = localLayoutRect.center
                                                val targetMatch = targetRects.entries.find { it.value.contains(dropCenter) }
                                                
                                                if (targetMatch?.key == pair.id) {
                                                    matchedIds = matchedIds + pair.id
                                                    rewardCharacter = pair.letter
                                                    com.example.ui.data.ActivityTracker.recordAnswer(pair.letter, true)
                                                    com.example.ui.audio.SoundEffectsHelper.playSuccess(context)
                                                } else if (targetMatch != null) {
                                                    com.example.ui.data.ActivityTracker.recordAnswer(pair.letter, false)
                                                    com.example.ui.audio.SoundEffectsHelper.playOops(context)
                                                }
                                                // Reset offset
                                                dragOffsets = dragOffsets - pair.id
                                            },
                                            onDragCancel = {
                                                dragOffsets = dragOffsets - pair.id
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!isMatched) {
                                    Text(pair.letter, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                }
                            }
                        }
                    }
                }
            }

            // Lottie Animation Reward whenever child correctly identifies a Kannada character
            com.example.ui.components.LottieCharacterRewardOverlay(
                visible = rewardCharacter != null,
                character = rewardCharacter ?: "",
                onDismiss = { rewardCharacter = null }
            )
        }
    }
}
