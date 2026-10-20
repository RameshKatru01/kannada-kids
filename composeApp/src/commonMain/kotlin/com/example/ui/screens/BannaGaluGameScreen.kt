package com.example.ui.screens

import com.example.platform.*


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Menu
import com.example.ui.components.PauseMenuDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class LiquidColor(val color: Color, val kannadaName: String, val englishName: String) {
    RED(Color.Red, "Kempu", "Red"),
    BLUE(Color.Blue, "Neeli", "Blue"),
    GREEN(Color.Green, "Hasiru", "Green"),
    YELLOW(Color.Yellow, "Haladi", "Yellow"),
    ORANGE(Color(0xFFFFA500), "Kiththale", "Orange"),
    PURPLE(Color(0xFF800080), "Nerale", "Purple"),
    PINK(Color(0xFFFFC0CB), "Gulabi", "Pink"),
    BROWN(Color(0xFFA52A2A), "Kandu", "Brown")
}

data class GameLevel(val id: Int, val initialTumblers: List<List<LiquidColor>>)

val colorsLevels = listOf(
    // Level 1
    GameLevel(1, listOf(
        listOf(LiquidColor.RED, LiquidColor.BLUE, LiquidColor.RED, LiquidColor.BLUE),
        listOf(LiquidColor.BLUE, LiquidColor.RED, LiquidColor.BLUE, LiquidColor.RED),
        emptyList()
    )),
    // Level 2
    GameLevel(2, listOf(
        listOf(LiquidColor.GREEN, LiquidColor.YELLOW, LiquidColor.YELLOW, LiquidColor.GREEN),
        listOf(LiquidColor.YELLOW, LiquidColor.GREEN, LiquidColor.GREEN, LiquidColor.YELLOW),
        emptyList()
    )),
    // Level 3
    GameLevel(3, listOf(
        listOf(LiquidColor.RED, LiquidColor.GREEN, LiquidColor.BLUE, LiquidColor.RED),
        listOf(LiquidColor.GREEN, LiquidColor.BLUE, LiquidColor.RED, LiquidColor.GREEN),
        listOf(LiquidColor.BLUE, LiquidColor.RED, LiquidColor.GREEN, LiquidColor.BLUE),
        emptyList()
    )),
    // Level 4
    GameLevel(4, listOf(
        listOf(LiquidColor.PURPLE, LiquidColor.PINK, LiquidColor.ORANGE, LiquidColor.PURPLE),
        listOf(LiquidColor.PINK, LiquidColor.ORANGE, LiquidColor.PURPLE, LiquidColor.PINK),
        listOf(LiquidColor.ORANGE, LiquidColor.PURPLE, LiquidColor.PINK, LiquidColor.ORANGE),
        emptyList()
    )),
    // Level 5
    GameLevel(5, listOf(
        listOf(LiquidColor.RED, LiquidColor.RED, LiquidColor.BLUE, LiquidColor.GREEN),
        listOf(LiquidColor.BLUE, LiquidColor.GREEN, LiquidColor.RED, LiquidColor.BLUE),
        listOf(LiquidColor.GREEN, LiquidColor.BLUE, LiquidColor.GREEN, LiquidColor.RED),
        emptyList()
    )),
    // Level 6
    GameLevel(6, listOf(
        listOf(LiquidColor.RED, LiquidColor.BLUE, LiquidColor.GREEN, LiquidColor.YELLOW),
        listOf(LiquidColor.YELLOW, LiquidColor.RED, LiquidColor.BLUE, LiquidColor.GREEN),
        listOf(LiquidColor.GREEN, LiquidColor.YELLOW, LiquidColor.RED, LiquidColor.BLUE),
        listOf(LiquidColor.BLUE, LiquidColor.GREEN, LiquidColor.YELLOW, LiquidColor.RED),
        emptyList(),
        emptyList()
    )),
    // Level 7
    GameLevel(7, listOf(
        listOf(LiquidColor.PINK, LiquidColor.ORANGE, LiquidColor.PURPLE, LiquidColor.BROWN),
        listOf(LiquidColor.BROWN, LiquidColor.PINK, LiquidColor.ORANGE, LiquidColor.PURPLE),
        listOf(LiquidColor.PURPLE, LiquidColor.BROWN, LiquidColor.PINK, LiquidColor.ORANGE),
        listOf(LiquidColor.ORANGE, LiquidColor.PURPLE, LiquidColor.BROWN, LiquidColor.PINK),
        emptyList(),
        emptyList()
    )),
    // Level 8
    GameLevel(8, listOf(
        listOf(LiquidColor.RED, LiquidColor.GREEN, LiquidColor.RED, LiquidColor.YELLOW),
        listOf(LiquidColor.BLUE, LiquidColor.BLUE, LiquidColor.YELLOW, LiquidColor.GREEN),
        listOf(LiquidColor.YELLOW, LiquidColor.RED, LiquidColor.GREEN, LiquidColor.BLUE),
        listOf(LiquidColor.GREEN, LiquidColor.YELLOW, LiquidColor.BLUE, LiquidColor.RED),
        emptyList(),
        emptyList()
    )),
    // Level 9
    GameLevel(9, listOf(
        listOf(LiquidColor.RED, LiquidColor.BLUE, LiquidColor.GREEN, LiquidColor.YELLOW),
        listOf(LiquidColor.PURPLE, LiquidColor.RED, LiquidColor.BLUE, LiquidColor.GREEN),
        listOf(LiquidColor.YELLOW, LiquidColor.PURPLE, LiquidColor.RED, LiquidColor.BLUE),
        listOf(LiquidColor.GREEN, LiquidColor.YELLOW, LiquidColor.PURPLE, LiquidColor.RED),
        listOf(LiquidColor.BLUE, LiquidColor.GREEN, LiquidColor.YELLOW, LiquidColor.PURPLE),
        emptyList(),
        emptyList()
    )),
    // Level 10
    GameLevel(10, listOf(
        listOf(LiquidColor.PINK, LiquidColor.ORANGE, LiquidColor.BROWN, LiquidColor.RED),
        listOf(LiquidColor.BLUE, LiquidColor.PINK, LiquidColor.ORANGE, LiquidColor.BROWN),
        listOf(LiquidColor.RED, LiquidColor.BLUE, LiquidColor.PINK, LiquidColor.ORANGE),
        listOf(LiquidColor.BROWN, LiquidColor.RED, LiquidColor.BLUE, LiquidColor.PINK),
        listOf(LiquidColor.ORANGE, LiquidColor.BROWN, LiquidColor.RED, LiquidColor.BLUE),
        emptyList(),
        emptyList()
    ))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannaGaluGameScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    
    DisposableEffect(context) {
        val attrContext = context
        val textToSpeech = TextToSpeech(attrContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale.US)
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    val capacity = 4
    var currentLevelIndex by remember { mutableIntStateOf(0) }
    var tumblers by remember { mutableStateOf(colorsLevels[currentLevelIndex].initialTumblers) }

    var selectedTumblerIndex by remember { mutableStateOf<Int?>(null) }
    var showWinMessage by remember { mutableStateOf(false) }
    var lastActionColor by remember { mutableStateOf<LiquidColor?>(null) }

    // Check win condition
    LaunchedEffect(tumblers) {
        val allColorsSorted = tumblers.filter { it.isNotEmpty() }.all { tumbler ->
            tumbler.size == capacity && tumbler.all { it == tumbler.first() }
        }
        if (allColorsSorted && tumblers.any { it.isNotEmpty() }) {
            showWinMessage = true
            com.example.ui.audio.SoundEffectsHelper.playGameCompletion(context)
        }
    }

    fun handleTumblerClick(index: Int) {
        com.example.ui.audio.SoundEffectsHelper.playButtonTap(context)
        if (selectedTumblerIndex == null) {
            // Select if not empty
            if (tumblers[index].isNotEmpty()) {
                val isCompleted = tumblers[index].size == capacity && tumblers[index].all { it == tumblers[index].first() }
                if (!isCompleted) {
                    selectedTumblerIndex = index
                }
            }
        } else {
            val fromIndex = selectedTumblerIndex!!
            val toIndex = index

            if (fromIndex == toIndex) {
                // Deselect
                selectedTumblerIndex = null
                return
            }

            val fromTumbler = tumblers[fromIndex]
            val toTumbler = tumblers[toIndex]

            if (fromTumbler.isNotEmpty() && toTumbler.size < capacity) {
                val colorToMove = fromTumbler.last()
                val canPour = toTumbler.isEmpty() || toTumbler.last() == colorToMove

                if (canPour) {
                    // Pour all contiguous identical color blocks that will fit
                    var blocksToMove = 0
                    for (i in fromTumbler.indices.reversed()) {
                        if (fromTumbler[i] == colorToMove) blocksToMove++ else break
                    }
                    val emptySpace = capacity - toTumbler.size
                    val actualBlocksToMove = minOf(blocksToMove, emptySpace)

                    val newTumblers = tumblers.toMutableList()
                    newTumblers[fromIndex] = fromTumbler.dropLast(actualBlocksToMove)
                    val addedBlocks = List(actualBlocksToMove) { colorToMove }
                    newTumblers[toIndex] = toTumbler + addedBlocks
                    tumblers = newTumblers

                    val isNowFullAndSameColor = newTumblers[toIndex].size == capacity && newTumblers[toIndex].all { it == colorToMove }

                    lastActionColor = colorToMove

                    if (isNowFullAndSameColor) {
                        // Celebration
                        com.example.ui.audio.SoundEffectsHelper.playSuccess(context)
                        
                        com.example.ui.audio.NativeAudioPlayer.playPhonics(context, "Look, this is ${colorToMove.englishName}!")
                    } else {
                        com.example.ui.audio.NativeAudioPlayer.playPhonics(context, "${colorToMove.englishName}!")
                    }
                }
            }
            selectedTumblerIndex = null
        }
    }

    var showPauseMenu by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Colors Sort") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        text = "Level ${currentLevelIndex + 1}/${colorsLevels.size}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF0F4F8)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Pour the colors to sort them!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )

            if (lastActionColor != null) {
                Text(
                    text = "${lastActionColor!!.kannadaName} - ${lastActionColor!!.englishName}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = lastActionColor!!.color,
                    modifier = Modifier.padding(bottom = 32.dp),
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black,
                            blurRadius = 2f
                        )
                    )
                )
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }

            val chunkedTumblers = tumblers.chunked(4)
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                chunkedTumblers.forEachIndexed { rowIndex, rowTumblers ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        rowTumblers.forEachIndexed { colIndex, tumbler ->
                            val actualIndex = rowIndex * 4 + colIndex
                            TumblerView(
                                colors = tumbler,
                                capacity = capacity,
                                isSelected = selectedTumblerIndex == actualIndex,
                                onClick = { handleTumblerClick(actualIndex) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            if (showWinMessage) {
                Text(
                    text = "Congratulations! You sorted all colors!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            tumblers = colorsLevels[currentLevelIndex].initialTumblers
                            showWinMessage = false
                            lastActionColor = null
                        }
                    ) {
                        Text("Restart Level", fontSize = 16.sp)
                    }
                    val authPrefs = getSharedPreferences("AppProgress")
                    val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
                    val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)

                    if (currentLevelIndex < colorsLevels.size - 1) {
                        Button(
                            onClick = {
                                if (isGuest) {
                                    Toast.makeText(context, "🔒 Level 1 Complete! Sign in with Gmail to play all color sorting levels!", Toast.LENGTH_LONG).show()
                                    onBack()
                                } else {
                                    currentLevelIndex++
                                    tumblers = colorsLevels[currentLevelIndex].initialTumblers
                                    showWinMessage = false
                                    lastActionColor = null
                                }
                            }
                        ) {
                            Text(if (isGuest) "Finish (Guest Preview)" else "Next Level", fontSize = 16.sp)
                        }
                    }
                }

                val authPrefs = getSharedPreferences("AppProgress")
                val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
                val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)
                com.example.ui.components.SurpriseStickerDialog(
                    userEmail = userEmail,
                    levelName = "Colors Sort Level ${currentLevelIndex + 1}",
                    seedModifier = currentLevelIndex,
                    onClose = {
                        showWinMessage = false
                        if (isGuest) {
                            onBack()
                        }
                    },
                    onClaimOrNext = {
                        showWinMessage = false
                        if (isGuest) {
                            Toast.makeText(context, "🔒 Level 1 Complete! Sign in with Gmail to play all color sorting levels!", Toast.LENGTH_LONG).show()
                            onBack()
                        } else if (currentLevelIndex < colorsLevels.size - 1) {
                            currentLevelIndex++
                            tumblers = colorsLevels[currentLevelIndex].initialTumblers
                            lastActionColor = null
                        } else {
                            onBack()
                        }
                    }
                )
            } else {
                Button(
                    onClick = {
                        tumblers = colorsLevels[currentLevelIndex].initialTumblers
                        lastActionColor = null
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Restart Level", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun TumblerView(
    colors: List<LiquidColor>,
    capacity: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tumblerHeight = 150.dp
    val tumblerWidth = 60.dp
    val borderWidth = 4.dp
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(32.dp)) // Maintain height
        }
        
        Box(
            modifier = Modifier
                .width(tumblerWidth)
                .height(tumblerHeight)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .border(
                    width = borderWidth,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray,
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                )
                .background(Color.White)
                .padding(borderWidth)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ) {
                val emptySpaces = capacity - colors.size
                repeat(emptySpaces) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Transparent))
                }
                colors.reversed().forEach { color ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(color.color)
                    )
                }
            }
        }
    }
}
