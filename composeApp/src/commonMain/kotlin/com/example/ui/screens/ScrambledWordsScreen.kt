package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightCyan
import kotlinx.coroutines.delay

data class ScrambledWord(val syllables: List<String>, val emoji: String, val meaning: String)

val scrambledWordsLevel = listOf(
    ScrambledWord(listOf("ಅ", "ರ", "ಸ"), "👑", "Arasa (King)"),
    ScrambledWord(listOf("ಆ", "ನೆ"), "🐘", "Aane (Elephant)"),
    ScrambledWord(listOf("ಇ", "ಲಿ"), "🐁", "Ili (Rat)"),
    ScrambledWord(listOf("ಮ", "ರ"), "🌳", "Mara (Tree)"),
    ScrambledWord(listOf("ಮ", "ನೆ"), "🏠", "Mane (House)"),
    ScrambledWord(listOf("ಕ", "ಮ", "ಲ"), "🪷", "Kamala (Lotus)"),
    ScrambledWord(listOf("ಹ", "ಡ", "ಗು"), "🚢", "Hadagu (Ship)"),
    ScrambledWord(listOf("ಬ", "ಸ", "ವ"), "🐂", "Basava (Ox)"),
    ScrambledWord(listOf("ಹ", "ಸು"), "🐄", "Hasu (Cow)"),
    ScrambledWord(listOf("ಗಿ", "ಳಿ"), "🦜", "Gili (Parrot)"),
    ScrambledWord(listOf("ಕ", "ರ", "ಡಿ"), "🐻", "Karadi (Bear)"),
    ScrambledWord(listOf("ನಾ", "ಯಿ"), "🐕", "Naayi (Dog)"),
    ScrambledWord(listOf("ಬೆ", "ಕ್ಕು"), "🐈", "Bekku (Cat)"),
    ScrambledWord(listOf("ಕು", "ರಿ"), "🐑", "Kuri (Sheep)"),
    ScrambledWord(listOf("ಹು", "ಲಿ"), "🐅", "Huli (Tiger)"),
    ScrambledWord(listOf("ಸಿಂ", "ಹ"), "🦁", "Simha (Lion)"),
    ScrambledWord(listOf("ಆ", "ಮೆ"), "🐢", "Aame (Turtle)"),
    ScrambledWord(listOf("ಕಾ", "ಗೆ"), "🐦‍⬛", "Kaage (Crow)"),
    ScrambledWord(listOf("ಗೂ", "ಬೆ"), "🦉", "Goobe (Owl)"),
    ScrambledWord(listOf("ನ", "ವಿ", "ಲು"), "🦚", "Navilu (Peacock)"),
    ScrambledWord(listOf("ಕ", "ಣ್ಣು"), "👁️", "Kannu (Eye)"),
    ScrambledWord(listOf("ಕಿ", "ವಿ"), "👂", "Kivi (Ear)"),
    ScrambledWord(listOf("ಮೂ", "ಗು"), "👃", "Moogu (Nose)"),
    ScrambledWord(listOf("ಬಾ", "ಯಿ"), "👄", "Baayi (Mouth)"),
    ScrambledWord(listOf("ಕಾ", "ಲು"), "🦵", "Kaalu (Leg)"),
    ScrambledWord(listOf("ಕೈ"), "✋", "Kai (Hand)"),
    ScrambledWord(listOf("ತ", "ಲೆ"), "🧑", "Tale (Head)"),
    ScrambledWord(listOf("ಕೂ", "ದ", "ಲು"), "💇", "Koodalu (Hair)"),
    ScrambledWord(listOf("ಸೂ", "ರ್ಯ"), "☀️", "Soorya (Sun)"),
    ScrambledWord(listOf("ಚಂ", "ದ್ರ"), "🌙", "Chandra (Moon)"),
    ScrambledWord(listOf("ಭೂ", "ಮಿ"), "🌍", "Bhoomi (Earth)"),
    ScrambledWord(listOf("ನ", "ಕ್ಷ", "ತ್ರ"), "⭐", "Nakshatra (Star)"),
    ScrambledWord(listOf("ಸೇ", "ಬು"), "🍎", "Sebu (Apple)"),
    ScrambledWord(listOf("ಬಾ", "ಳೆ"), "🍌", "Baale (Banana)"),
    ScrambledWord(listOf("ದ್ರಾ", "ಕ್ಷಿ"), "🍇", "Drakshi (Grapes)"),
    ScrambledWord(listOf("ಕಿ", "ತ್ತ", "ಳೆ"), "🍊", "Kittale (Orange)"),
    ScrambledWord(listOf("ಮಾ", "ವು"), "🥭", "Maavu (Mango)"),
    ScrambledWord(listOf("ಈ", "ರು", "ಳ್ಳಿ"), "🧅", "Eerulli (Onion)"),
    ScrambledWord(listOf("ಟೊ", "ಮೆ", "ಟೊ"), "🍅", "Tomato"),
    ScrambledWord(listOf("ಬ", "ದ", "ನೆ"), "🍆", "Badane (Brinjal)"),
    ScrambledWord(listOf("ಹೂ", "ವು"), "🌸", "Hoovu (Flower)"),
    ScrambledWord(listOf("ಬೆ", "ಳ", "ಕು"), "💡", "Belaku (Light)"),
    ScrambledWord(listOf("ಪು", "ಸ್ತ", "ಕ"), "📖", "Pustaka (Book)"),
    ScrambledWord(listOf("ಲೇ", "ಖ", "ನಿ"), "🖊️", "Lekhani (Pen)"),
    ScrambledWord(listOf("ಶಾ", "ಲೆ"), "🏫", "Shaale (School)"),
    ScrambledWord(listOf("ರ", "ಸ್ತೆ"), "🛣️", "Raste (Road)"),
    ScrambledWord(listOf("ಗಾ", "ಡಿ"), "🚗", "Gaadi (Car)"),
    ScrambledWord(listOf("ವಿ", "ಮಾ", "ನ"), "✈️", "Vimaana (Airplane)"),
    ScrambledWord(listOf("ರೈ", "ಲು"), "🚆", "Railu (Train)"),
    ScrambledWord(listOf("ಕ", "ಡ", "ಲು"), "🌊", "Kadalu (Sea)")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrambledWordsScreen(onBack: () -> Unit) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentWord = scrambledWordsLevel[currentIndex]
    
    // We need to keep track of available options and filled slots
    // To handle multiple identical letters (if any), we can use data class with unique ID
    data class SyllableItem(val id: Int, val text: String)
    
    var options by remember(currentIndex) { 
        mutableStateOf(currentWord.syllables.mapIndexed { index, s -> SyllableItem(index, s) }.shuffled())
    }
    
    var filledSlots by remember(currentIndex) {
        mutableStateOf(List<SyllableItem?>(currentWord.syllables.size) { null })
    }
    
    var isError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(filledSlots) {
        if (filledSlots.all { it != null }) {
            val formedWord = filledSlots.joinToString("") { it!!.text }
            val correctWord = currentWord.syllables.joinToString("")
            if (formedWord == correctWord) {
                isSuccess = true
                isError = false
            } else {
                isError = true
                delay(1000)
                isError = false
            }
        } else {
            isError = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scrambled Words", color = Color.White, fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Level ${currentIndex + 1} of ${scrambledWordsLevel.size}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Picture Hint
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .background(Color.White, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = currentWord.emoji, fontSize = 80.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(text = currentWord.meaning, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Slots
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filledSlots.forEachIndexed { index, item ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSuccess -> Color(0xFF81C784)
                                    isError -> Color(0xFFE57373)
                                    else -> Color(0xFFE0E0E0)
                                }
                            )
                            .clickable(enabled = item != null && !isSuccess) {
                                // Remove from slot and add back to options
                                if (item != null) {
                                    val newSlots = filledSlots.toMutableList()
                                    newSlots[index] = null
                                    filledSlots = newSlots
                                    
                                    options = options + item
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (item != null) {
                            Text(text = item.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Options Rack
            if (!isSuccess) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    options.forEach { item ->
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .clickable {
                                    // Move to first empty slot
                                    val emptyIndex = filledSlots.indexOfFirst { it == null }
                                    if (emptyIndex != -1) {
                                        val newSlots = filledSlots.toMutableList()
                                        newSlots[emptyIndex] = item
                                        filledSlots = newSlots
                                        
                                        options = options.filter { it.id != item.id }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = item.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Great Job! 🎉", fontSize = 32.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    val context = LocalContext.current
                    val authPrefs = remember { getSharedPreferences("AppProgress") }
                    val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
                    val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)

                    Button(
                        onClick = {
                            if (isGuest && currentIndex >= 0) {
                                Toast.makeText(context, "🔒 Level 1 Complete! Sign in with Gmail to play all scrambled words!", Toast.LENGTH_LONG).show()
                                onBack()
                            } else if (currentIndex < scrambledWordsLevel.size - 1) {
                                currentIndex++
                                isSuccess = false
                            } else {
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrightCyan),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(60.dp).padding(horizontal = 32.dp)
                    ) {
                        Text(if (isGuest) "Finish (Guest Preview)" else if (currentIndex < scrambledWordsLevel.size - 1) "Next Word" else "Finish", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
