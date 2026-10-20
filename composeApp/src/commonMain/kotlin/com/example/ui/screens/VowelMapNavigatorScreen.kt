package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightCyan
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

val vowelClues = mapOf(
    "ಅ" to "🍚", // Anna
    "ಆ" to "🐘", // Aane
    "ಇ" to "🐁", // Ili
    "ಈ" to "🌴", // Eechalu
    "ಉ" to "🎁", // Udugore
    "ಊ" to "🍲", // Oota
    "ಋ" to "🧘", // Rushi
    "ಎ" to "🍃", // Ele
    "ಏ" to "🪜", // Eni
    "ಐ" to "🖐️", // Aidu
    "ಒ" to "🐪", // Onte
    "ಓ" to "🏃", // Oota
    "ಔ" to "💊", // Aushadha
    "ಅಂ" to "👕", // Angi
    "ಅಃ" to "✨",
    "ಕ" to "🪷",
    "ಖ" to "🐦",
    "ಗ" to "🦜",
    "ಘ" to "🕰️",
    "ಙ" to "🐚",
    "ಚ" to "🥄",
    "ಛ" to "☂️",
    "ಜ" to "🕷️",
    "ಝ" to "🐟",
    "ಞ" to "🎭",
    "ಟ" to "🍅",
    "ಠ" to "🥶",
    "ಡ" to "🥁",
    "ಢ" to "🛡️",
    "ಣ" to "🏹",
    "ತ" to "⚖️",
    "ಥ" to "🍽️",
    "ದ" to "🐄",
    "ಧ" to "💰",
    "ನ" to "🦚",
    "ಪ" to "🕊️",
    "ಫ" to "🍎",
    "ಬ" to "🦆",
    "ಭ" to "🐻",
    "ಮ" to "🐒",
    "ಯ" to "⚙️",
    "ರ" to "🐎",
    "ಲ" to "🍋",
    "ವ" to "💎",
    "ಶ" to "🐇",
    "ಷ" to "🐝",
    "ಸ" to "🦁",
    "ಹ" to "🦢",
    "ಳ" to "🔒",
    "ಕ್ಷ" to "⚔️"
)

val mapLevels = listOf(
    listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ"),
    listOf("ಊ", "ಋ", "ಎ", "ಏ", "ಐ"),
    listOf("ಒ", "ಓ", "ಔ", "ಅಂ", "ಅಃ"),
    listOf("ಕ", "ಖ", "ಗ", "ಘ", "ಙ"),
    listOf("ಚ", "ಛ", "ಜ", "ಝ", "ಞ"),
    listOf("ಟ", "ಠ", "ಡ", "ಢ", "ಣ"),
    listOf("ತ", "ಥ", "ದ", "ಧ", "ನ"),
    listOf("ಪ", "ಫ", "ಬ", "ಭ", "ಮ"),
    listOf("ಯ", "ರ", "ಲ", "ವ", "ಶ"),
    listOf("ಷ", "ಸ", "ಹ", "ಳ", "ಕ್ಷ"),
    listOf("ಅ", "ಕ", "ಚ", "ಟ", "ಪ"),
    listOf("ಆ", "ಖ", "ಛ", "ಠ", "ಫ"),
    listOf("ಇ", "ಗ", "ಜ", "ಡ", "ಬ")
)

val mapSpotPositions = listOf(
    Offset(0.2f, 0.2f),
    Offset(0.7f, 0.3f),
    Offset(0.8f, 0.7f),
    Offset(0.4f, 0.8f),
    Offset(0.2f, 0.6f)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VowelMapNavigatorScreen(onBack: () -> Unit) {
    var currentLevel by remember { mutableIntStateOf(0) }
    var unlockedSpots by remember { mutableStateOf(setOf<String>()) }
    var showLevelComplete by remember { mutableStateOf(false) }

    val currentVowels = mapLevels[currentLevel]
    val shuffledOptions = remember(currentLevel) { currentVowels.shuffled() }

    var draggingVowel by remember { mutableStateOf<String?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }

    // Store global positions of the drop targets and rack items
    val spotGlobalPositions = remember { mutableMapOf<String, Offset>() }
    val rackItemPositions = remember { mutableMapOf<String, Offset>() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = { Text("Vowel Map Navigator", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrightCyan)
            )
        },
        containerColor = Color(0xFFFFF9C4) // Parchment / Map color
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // The Map Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .background(Color(0xFFFFECB3), RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    // Draw map path
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        val path = Path()
                        mapSpotPositions.forEachIndexed { index, relPos ->
                            val px = relPos.x * width
                            val py = relPos.y * height
                            if (index == 0) {
                                path.moveTo(px, py)
                            } else {
                                path.lineTo(px, py)
                            }
                        }

                        drawPath(
                            path = path,
                            color = Color(0xFF8D6E63),
                            style = Stroke(width = 8.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 20f)))
                        )
                    }
                    
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val width = maxWidth
                        val height = maxHeight
                        
                        currentVowels.forEachIndexed { index, vowel ->
                            val isUnlocked = unlockedSpots.contains(vowel)
                            val relPos = mapSpotPositions[index]
                            
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = width * relPos.x - 40.dp,
                                        y = height * relPos.y - 40.dp
                                    )
                                    .size(80.dp)
                                    .onGloballyPositioned { coordinates ->
                                        spotGlobalPositions[vowel] = coordinates.boundsInRoot().center
                                    }
                                    .shadow(if (isUnlocked) 8.dp else 2.dp, CircleShape)
                                    .background(if (isUnlocked) Color.White else Color(0xFFE0E0E0), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUnlocked) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = vowelClues[vowel] ?: "❓", fontSize = 28.sp)
                                        Text(text = vowel, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
                                    }
                                } else {
                                    Text(
                                        text = vowel,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }

                // The Options Rack
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFF8D6E63), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        shuffledOptions.forEach { vowel ->
                            val isUnlocked = unlockedSpots.contains(vowel)
                            
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .onGloballyPositioned { coordinates ->
                                        // store starting center of rack item
                                        if (!isUnlocked) {
                                            rackItemPositions[vowel] = coordinates.boundsInRoot().center
                                        }
                                    }
                                    .background(if (isUnlocked) Color.Transparent else Color.White, RoundedCornerShape(12.dp))
                                    .pointerInput(isUnlocked) {
                                        if (isUnlocked) return@pointerInput
                                        detectDragGestures(
                                            onDragStart = { _ ->
                                                draggingVowel = vowel
                                                dragPosition = rackItemPositions[vowel] ?: Offset.Zero
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragPosition += dragAmount
                                            },
                                            onDragEnd = {
                                                val targetPos = spotGlobalPositions[vowel]
                                                if (targetPos != null) {
                                                    // distance check between dragging center and target center
                                                    val dist = sqrt((dragPosition.x - targetPos.x).pow(2) + (dragPosition.y - targetPos.y).pow(2))
                                                    if (dist < 150f) { // 150px threshold
                                                        unlockedSpots = unlockedSpots + vowel
                                                        if (unlockedSpots.size == currentVowels.size) {
                                                            showLevelComplete = true
                                                        }
                                                    }
                                                }
                                                draggingVowel = null
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!isUnlocked && draggingVowel != vowel) {
                                    Text(
                                        text = vowel,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        } // Close Scaffold lambda

        // Dragging overlay (drawn on top of Scaffold in root coordinates)
        if (draggingVowel != null) {
            // Box size is 60.dp, so we offset by -30.dp to center it on the dragPosition
            Box(
                modifier = Modifier
                    .offset { 
                        IntOffset(
                            (dragPosition.x - 30.dp.toPx()).roundToInt(), 
                            (dragPosition.y - 30.dp.toPx()).roundToInt()
                        ) 
                    }
                    .size(60.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .background(Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = draggingVowel!!,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        if (showLevelComplete) {
            val localContext = LocalContext.current
            val authPrefs = getSharedPreferences("AppProgress")
            val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
            com.example.ui.components.SurpriseStickerDialog(
                userEmail = userEmail,
                levelName = "Map Level ${currentLevel + 1}",
                seedModifier = currentLevel,
                onClose = {
                    showLevelComplete = false
                },
                onClaimOrNext = {
                    showLevelComplete = false
                    val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)
                    if (isGuest) {
                        Toast.makeText(localContext, "🔒 Level 1 Complete! Sign in with Gmail to unlock all map levels!", Toast.LENGTH_LONG).show()
                        onBack()
                    } else if (currentLevel < mapLevels.size - 1) {
                        currentLevel++
                        unlockedSpots = emptySet()
                    } else {
                        onBack()
                    }
                }
            )
        }
    }
}
