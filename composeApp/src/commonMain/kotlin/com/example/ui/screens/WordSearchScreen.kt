
package com.example.ui.screens

import com.example.platform.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightCyan
import kotlinx.coroutines.delay

data class WordSearchTarget(
    val word: List<String>,
    val emoji: String,
    val meaning: String
)

data class WordSearchLevel(
    val grid: List<List<String>>,
    val targetWords: List<WordSearchTarget>
)

val wordSearchLevels = listOf(
    WordSearchLevel(
        grid = listOf(
            listOf("ಅ", "ರ", "ಸ", "ಕ", "ಮ"),
            listOf("ಆ", "ನೆ", "ಟ", "ಚ", "ರ"),
            listOf("ಪ", "ಮ", "ನೆ", "ಗ", "ಬ"),
            listOf("ಹ", "ಡ", "ಗು", "ಲ", "ವ"),
            listOf("ಇ", "ಲಿ", "ಶ", "ಷ", "ಸ")
        ),
        targetWords = listOf(
            WordSearchTarget(listOf("ಅ", "ರ", "ಸ"), "👑", "Arasa (King)"),
            WordSearchTarget(listOf("ಆ", "ನೆ"), "🐘", "Aane (Elephant)"),
            WordSearchTarget(listOf("ಮ", "ನೆ"), "🏠", "Mane (House)"),
            WordSearchTarget(listOf("ಹ", "ಡ", "ಗು"), "🚢", "Hadagu (Ship)"),
            WordSearchTarget(listOf("ಇ", "ಲಿ"), "🐁", "Ili (Rat)")
        )
    ),
    WordSearchLevel(
        grid = listOf(
            listOf("ಕ", "ಮ", "ಲ", "ಯ", "ರ"),
            listOf("ಬ", "ಸ", "ವ", "ಲ", "ವ"),
            listOf("ಮ", "ರ", "ಶ", "ಷ", "ಸ"),
            listOf("ಹ", "ಬ", "ಬ", "ಗ", "ಘ"),
            listOf("ಪ", "ದ", "ಫ", "ಬ", "ಭ")
        ),
        targetWords = listOf(
            WordSearchTarget(listOf("ಕ", "ಮ", "ಲ"), "🪷", "Kamala (Lotus)"),
            WordSearchTarget(listOf("ಬ", "ಸ", "ವ"), "🐂", "Basava (Ox)"),
            WordSearchTarget(listOf("ಮ", "ರ"), "🌳", "Mara (Tree)")
        )
    ),
    WordSearchLevel(
        grid = listOf(
            listOf("ಹ", "ಸು", "ಸ", "ಕ", "ಮ"),
            listOf("ಗಿ", "ಳಿ", "ಟ", "ಚ", "ರ"),
            listOf("ಪ", "ಮ", "ಕ", "ರ", "ಡಿ"),
            listOf("ಹ", "ಡ", "ಗು", "ಲ", "ವ"),
            listOf("ಇ", "ಲಿ", "ಶ", "ಷ", "ಸ")
        ),
        targetWords = listOf(
            WordSearchTarget(listOf("ಹ", "ಸು"), "🐄", "Hasu (Cow)"),
            WordSearchTarget(listOf("ಗಿ", "ಳಿ"), "🦜", "Gili (Parrot)"),
            WordSearchTarget(listOf("ಕ", "ರ", "ಡಿ"), "🐻", "Karadi (Bear)")
        )
    ),
    WordSearchLevel(
        grid = listOf(
            listOf("ನಾ", "ಯಿ", "ಸ", "ಕ", "ಮ"),
            listOf("ಬೆ", "ಕ್ಕು", "ಟ", "ಚ", "ರ"),
            listOf("ಪ", "ಮ", "ಕು", "ರಿ", "ಬ"),
            listOf("ಹ", "ಡ", "ಗು", "ಲ", "ವ"),
            listOf("ಇ", "ಲಿ", "ಶ", "ಷ", "ಸ")
        ),
        targetWords = listOf(
            WordSearchTarget(listOf("ನಾ", "ಯಿ"), "🐕", "Naayi (Dog)"),
            WordSearchTarget(listOf("ಬೆ", "ಕ್ಕು"), "🐈", "Bekku (Cat)"),
            WordSearchTarget(listOf("ಕು", "ರಿ"), "🐑", "Kuri (Sheep)")
        )
    ),
    WordSearchLevel(
        grid = listOf(
            listOf("ಹು", "ಲಿ", "ಸ", "ಕ", "ಮ"),
            listOf("ಸಿಂ", "ಹ", "ಟ", "ಚ", "ರ"),
            listOf("ಪ", "ಮ", "ಆ", "ಮೆ", "ಬ"),
            listOf("ಹ", "ಡ", "ಗು", "ಲ", "ವ"),
            listOf("ಇ", "ಲಿ", "ಶ", "ಷ", "ಸ")
        ),
        targetWords = listOf(
            WordSearchTarget(listOf("ಹು", "ಲಿ"), "🐅", "Huli (Tiger)"),
            WordSearchTarget(listOf("ಸಿಂ", "ಹ"), "🦁", "Simha (Lion)"),
            WordSearchTarget(listOf("ಆ", "ಮೆ"), "🐢", "Aame (Turtle)")
        )
    ),
    WordSearchLevel(
        grid = listOf(
            listOf("ಕಾ", "ಗೆ", "ಸ", "ಕ", "ಮ"),
            listOf("ಗೂ", "ಬೆ", "ಟ", "ಚ", "ರ"),
            listOf("ನ", "ವಿ", "ಲು", "ಗ", "ಬ"),
            listOf("ಹ", "ಡ", "ಗು", "ಲ", "ವ"),
            listOf("ಇ", "ಲಿ", "ಶ", "ಷ", "ಸ")
        ),
        targetWords = listOf(
            WordSearchTarget(listOf("ಕಾ", "ಗೆ"), "🐦‍⬛", "Kaage (Crow)"),
            WordSearchTarget(listOf("ಗೂ", "ಬೆ"), "🦉", "Goobe (Owl)"),
            WordSearchTarget(listOf("ನ", "ವಿ", "ಲು"), "🦚", "Navilu (Peacock)")
        )
    ),
    WordSearchLevel(
        grid = listOf(
            listOf("ಕ", "ಣ್ಣು", "ಸ", "ಕ", "ಮ"),
            listOf("ಕಿ", "ವಿ", "ಟ", "ಚ", "ರ"),
            listOf("ಮೂ", "ಗು", "ನೆ", "ಗ", "ಬ"),
            listOf("ಹ", "ಡ", "ಗು", "ಲ", "ವ"),
            listOf("ಇ", "ಲಿ", "ಶ", "ಷ", "ಸ")
        ),
        targetWords = listOf(
            WordSearchTarget(listOf("ಕ", "ಣ್ಣು"), "👁️", "Kannu (Eye)"),
            WordSearchTarget(listOf("ಕಿ", "ವಿ"), "👂", "Kivi (Ear)"),
            WordSearchTarget(listOf("ಮೂ", "ಗು"), "👃", "Moogu (Nose)")
        )
    ),
    WordSearchLevel(
        grid = listOf(
            listOf("ಕಾ", "ಲು", "ಸ", "ಕ", "ಮ"),
            listOf("ಕೈ", "ನೆ", "ಟ", "ಚ", "ರ"),
            listOf("ತ", "ಲೆ", "ನೆ", "ಗ", "ಬ"),
            listOf("ಹ", "ಡ", "ಗು", "ಲ", "ವ"),
            listOf("ಇ", "ಲಿ", "ಶ", "ಷ", "ಸ")
        ),
        targetWords = listOf(
            WordSearchTarget(listOf("ಕಾ", "ಲು"), "🦵", "Kaalu (Leg)"),
            WordSearchTarget(listOf("ಕೈ"), "✋", "Kai (Hand)"),
            WordSearchTarget(listOf("ತ", "ಲೆ"), "🧑", "Tale (Head)")
        )
    ),
    WordSearchLevel(
        grid = listOf(
            listOf("ಸೂ", "ರ್ಯ", "ಸ", "ಕ", "ಮ"),
            listOf("ಚಂ", "ದ್ರ", "ಟ", "ಚ", "ರ"),
            listOf("ಭೂ", "ಮಿ", "ನೆ", "ಗ", "ಬ"),
            listOf("ಹ", "ಡ", "ಗು", "ಲ", "ವ"),
            listOf("ಇ", "ಲಿ", "ಶ", "ಷ", "ಸ")
        ),
        targetWords = listOf(
            WordSearchTarget(listOf("ಸೂ", "ರ್ಯ"), "☀️", "Soorya (Sun)"),
            WordSearchTarget(listOf("ಚಂ", "ದ್ರ"), "🌙", "Chandra (Moon)"),
            WordSearchTarget(listOf("ಭೂ", "ಮಿ"), "🌍", "Bhoomi (Earth)")
        )
    ),
    WordSearchLevel(
        grid = listOf(
            listOf("ಸೇ", "ಬು", "ಸ", "ಕ", "ಮ"),
            listOf("ಬಾ", "ಳೆ", "ಟ", "ಚ", "ರ"),
            listOf("ದ್ರಾ", "ಕ್ಷಿ", "ನೆ", "ಗ", "ಬ"),
            listOf("ಹ", "ಡ", "ಗು", "ಲ", "ವ"),
            listOf("ಇ", "ಲಿ", "ಶ", "ಷ", "ಸ")
        ),
        targetWords = listOf(
            WordSearchTarget(listOf("ಸೇ", "ಬು"), "🍎", "Sebu (Apple)"),
            WordSearchTarget(listOf("ಬಾ", "ಳೆ"), "🍌", "Baale (Banana)"),
            WordSearchTarget(listOf("ದ್ರಾ", "ಕ್ಷಿ"), "🍇", "Drakshi (Grapes)")
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordSearchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var currentLevelIndex by remember { mutableIntStateOf(0) }
    val level = wordSearchLevels[currentLevelIndex]

    var foundWords by remember(currentLevelIndex) { mutableStateOf(setOf<WordSearchTarget>()) }
    var selectedCells by remember(currentLevelIndex) { mutableStateOf(listOf<Pair<Int, Int>>()) }
    var foundCells by remember(currentLevelIndex) { mutableStateOf(setOf<Pair<Int, Int>>()) }

    var showSuccessMessage by remember { mutableStateOf(false) }

    LaunchedEffect(foundWords) {
        if (foundWords.size == level.targetWords.size && level.targetWords.isNotEmpty()) {
                            try { com.example.ui.data.ScoreManager.addCoins(context, "word_search", 15) } catch (e: Exception) {}
            showSuccessMessage = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Word Search Puzzles", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrightCyan)
            )
        },
        containerColor = Color(0xFFFFF3E0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Level ${currentLevelIndex + 1} of ${wordSearchLevels.size}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Picture Clues
            Text(
                text = "Find these pictures!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100)
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                level.targetWords.forEach { target ->
                    val isFound = foundWords.contains(target)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .background(if (isFound) Color(0xFFC8E6C9) else Color.White, RoundedCornerShape(12.dp))
                                .border(
                                    2.dp,
                                    if (isFound) Color(0xFF4CAF50) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = target.emoji, fontSize = 32.sp)
                        }
                        if (isFound) {
                            Text(
                                text = "Found!",
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Grid
            Column(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                level.grid.forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEachIndexed { colIndex, cellString ->
                            val cellPos = Pair(rowIndex, colIndex)
                            val isSelected = selectedCells.contains(cellPos)
                            val isFoundCell = foundCells.contains(cellPos)

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isFoundCell -> Color(0xFF81C784) // Green for found
                                            isSelected -> Color(0xFF64B5F6) // Blue for selecting
                                            else -> Color(0xFFF5F5F5) // Default gray
                                        }
                                    )
                                    .clickable {
                                        if (isFoundCell) return@clickable

                                        if (selectedCells.isNotEmpty() && selectedCells.last() == cellPos) {
                                            // Undo last selection
                                            selectedCells = selectedCells.dropLast(1)
                                            return@clickable
                                        }

                                        val newSelection = if (selectedCells.isEmpty()) {
                                            listOf(cellPos)
                                        } else {
                                            val lastPos = selectedCells.last()
                                            val rDiff = kotlin.math.abs(lastPos.first - rowIndex)
                                            val cDiff = kotlin.math.abs(lastPos.second - colIndex)
                                            // Must be adjacent and not already selected
                                            if (rDiff <= 1 && cDiff <= 1 && !selectedCells.contains(cellPos)) {
                                                selectedCells + cellPos
                                            } else {
                                                // If not adjacent, start new selection
                                                listOf(cellPos)
                                            }
                                        }

                                        // Check if newSelection matches prefix of any unfound word
                                        val currentChars = newSelection.map { level.grid[it.first][it.second] }
                                        val unfoundWords = level.targetWords - foundWords
                                        
                                        var matchedWord: WordSearchTarget? = null
                                        val isPrefix = unfoundWords.any { target ->
                                            if (target.word.size >= currentChars.size && target.word.take(currentChars.size) == currentChars) {
                                                if (target.word.size == currentChars.size) {
                                                    matchedWord = target
                                                }
                                                true
                                            } else {
                                                false
                                            }
                                        }

                                        if (matchedWord != null) {
                                            // Found a word!
                                            foundWords = foundWords + matchedWord!!
                                            foundCells = foundCells + newSelection
                                            selectedCells = emptyList()
                                        } else if (isPrefix) {
                                            // Still a valid prefix, continue selecting
                                            selectedCells = newSelection
                                        } else {
                                            // Not a prefix, start fresh with this cell if it's a valid starting letter
                                            val isStartChar = unfoundWords.any { it.word.firstOrNull() == cellString }
                                            selectedCells = if (isStartChar) listOf(cellPos) else emptyList()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cellString,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFoundCell || isSelected) Color.White else Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Clear Selection Button (optional, but good for UX)
            if (selectedCells.isNotEmpty()) {
                Button(
                    onClick = { selectedCells = emptyList() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                ) {
                    Text("Clear Selection", color = Color.White)
                }
            }

            if (showSuccessMessage) {
                val authPrefs = getSharedPreferences("AppProgress")
                val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
                com.example.ui.components.SurpriseStickerDialog(
                    userEmail = userEmail,
                    levelName = "Word Search Level ${currentLevelIndex + 1}",
                    seedModifier = currentLevelIndex,
                    onClose = {
                        showSuccessMessage = false
                    },
                    onClaimOrNext = {
                        val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)
                        if (isGuest) {
                            Toast.makeText(context, "🔒 Level 1 Complete! Sign in with Gmail to play all word search puzzles!", Toast.LENGTH_LONG).show()
                            onBack()
                        } else if (currentLevelIndex < wordSearchLevels.size - 1) {
                            currentLevelIndex++
                            showSuccessMessage = false
                            foundWords = emptySet()
                            foundCells = emptySet()
                            selectedCells = emptyList()
                        } else {
                            onBack()
                        }
                    }
                )
            }
        }
    }
}
