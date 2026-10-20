package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightCyan

data class FlashcardData(val english: String, val kannada: String, val pronunciation: String, val imageUrl: String)

val animalFlashcards = emptyList<FlashcardData>()

val colorFlashcards = listOf(
    FlashcardData("Red", "ಕೆಂಪು", "Kempu", "https://placehold.co/600x400/FF0000/FFFFFF?text=Red"),
    FlashcardData("Blue", "ನೀಲಿ", "Neeli", "https://placehold.co/600x400/0000FF/FFFFFF?text=Blue"),
    FlashcardData("Green", "ಹಸಿರು", "Hasiru", "https://placehold.co/600x400/008000/FFFFFF?text=Green"),
    FlashcardData("Yellow", "ಹಳದಿ", "Haladi", "https://placehold.co/600x400/FFFF00/000000?text=Yellow"),
    FlashcardData("Black", "ಕಪ್ಪು", "Kappu", "https://placehold.co/600x400/000000/FFFFFF?text=Black"),
    FlashcardData("White", "ಬಿಳಿ", "Bili", "https://placehold.co/600x400/F0F0F0/000000?text=White"),
    FlashcardData("Orange", "ಕಿತ್ತಳೆ", "Kittale", "https://placehold.co/600x400/FFA500/000000?text=Orange"),
    FlashcardData("Pink", "ಗುಲಾಬಿ", "Gulabi", "https://placehold.co/600x400/FFC0CB/000000?text=Pink"),
    FlashcardData("Purple", "ನೇರಳೆ", "Nerale", "https://placehold.co/600x400/800080/FFFFFF?text=Purple"),
    FlashcardData("Brown", "ಕಂದು", "Kandu", "https://placehold.co/600x400/A52A2A/FFFFFF?text=Brown")
)

val numberFlashcards = listOf(
    FlashcardData("One (1)", "ಒಂದು", "Ondu", "https://placehold.co/600x400/EEEEEE/333333?text=1"),
    FlashcardData("Two (2)", "ಎರಡು", "Eradu", "https://placehold.co/600x400/EEEEEE/333333?text=2"),
    FlashcardData("Three (3)", "ಮೂರು", "Mooru", "https://placehold.co/600x400/EEEEEE/333333?text=3"),
    FlashcardData("Four (4)", "ನಾಲ್ಕು", "Naalku", "https://placehold.co/600x400/EEEEEE/333333?text=4"),
    FlashcardData("Five (5)", "ಐದು", "Aidu", "https://placehold.co/600x400/EEEEEE/333333?text=5"),
    FlashcardData("Six (6)", "ಆರು", "Aaru", "https://placehold.co/600x400/EEEEEE/333333?text=6"),
    FlashcardData("Seven (7)", "ಏಳು", "Eelu", "https://placehold.co/600x400/EEEEEE/333333?text=7"),
    FlashcardData("Eight (8)", "ಎಂಟು", "Entu", "https://placehold.co/600x400/EEEEEE/333333?text=8"),
    FlashcardData("Nine (9)", "ಒಂಬತ್ತು", "Ombattu", "https://placehold.co/600x400/EEEEEE/333333?text=9"),
    FlashcardData("Ten (10)", "ಹತ್ತು", "Hattu", "https://placehold.co/600x400/EEEEEE/333333?text=10")
)

val birdFlashcards = emptyList<FlashcardData>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    category: String,
    onBack: () -> Unit
) {
    val flashcards = when (category.lowercase()) {
        "birds" -> birdFlashcards
        "animals" -> animalFlashcards
        "colours", "colors" -> colorFlashcards
        "numbers" -> numberFlashcards
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$category Flashcards", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrightCyan)
            )
        },
        containerColor = Color(0xFFE1F5FE)
    ) { paddingValues ->
        if (flashcards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = if (category.lowercase() == "animals") "🐾" else if (category.lowercase() == "birds") "🐦" else "📂",
                        fontSize = 56.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No content in $category",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF006064),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Content in this folder has been cleared.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = BrightCyan),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Back to Folders ↩", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { flashcards.size })
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val context = LocalContext.current
                val authPrefs = remember { getSharedPreferences("AppProgress") }
                val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
                val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)

                Text(
                    text = "${pagerState.currentPage + 1} / ${flashcards.size}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    val isLocked = isGuest && page > 0
                    if (!isLocked) {
                        FlashcardItem(card = flashcards[page])
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4f),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color(0xFFE91E63),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "🔒 Guest Preview",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF263238),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Only the 1st card in each folder is unlocked.\nSign in with Gmail to view all cards!",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                
                Text(
                    text = if (isGuest) "🔒 Guest mode: 1st card preview" else "Tap card to flip • Swipe for next",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun FlashcardItem(card: FlashcardData) {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "flip"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable { flipped = !flipped },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (rotation <= 90f) {
                    // Front
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = card.imageUrl,
                            contentDescription = card.english,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = card.english,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrightCyan,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Back
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }, // Keep text readable
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = card.kannada,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFE91E63),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = card.pronunciation,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
