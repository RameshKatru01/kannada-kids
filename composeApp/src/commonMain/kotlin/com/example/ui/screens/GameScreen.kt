package com.example.ui.screens

import com.example.platform.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KannadaLetter
import com.example.data.LetterData
import com.example.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.AuthState
import androidx.compose.material.icons.filled.Lock
import com.example.ui.data.AppLanguage
import com.example.ui.data.LanguageManager

val guninthakshariSigns = listOf(
    "" to "a",         // ಅ
    "\u0CBE" to "aa",  // ಆ
    "\u0CBF" to "i",   // ಇ
    "\u0CC0" to "ee",  // ಈ
    "\u0CC1" to "u",   // ಉ
    "\u0CC2" to "oo",  // ಊ
    "\u0CC3" to "ru",  // ಋ
    "\u0CC6" to "e",   // ಎ
    "\u0CC7" to "ea",  // ಏ
    "\u0CC8" to "ai",  // ಐ
    "\u0CCA" to "o",   // ಒ
    "\u0CCB" to "oa",  // ಓ
    "\u0CCC" to "au",  // ಔ
    "\u0C82" to "am",  // ಅಂ
    "\u0C83" to "aha"  // ಅಃ
)

val barakhadiSigns = listOf(
    "" to "a",         // अ
    "ा" to "aa",      // आ
    "ि" to "i",       // इ
    "ी" to "ee",      // ई
    "ु" to "u",       // उ
    "ू" to "oo",      // ऊ
    "ृ" to "ri",      // ऋ
    "े" to "e",       // ए
    "ै" to "ai",      // ऐ
    "ो" to "o",       // ओ
    "ौ" to "au",      // औ
    "ं" to "am",      // अं
    "ः" to "aha"      // अः
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(onBack: () -> Unit, onNavigateToGameFlow: (String, String, Int) -> Unit = { _, _, _ -> }, authViewModel: AuthViewModel = viewModel { AuthViewModel() }) {
    val context = LocalContext.current
    var tts by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<TextToSpeech?>(null) }
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    
    val signs = guninthakshariSigns
    val titleText = "Guninthakshari"
    
    androidx.compose.runtime.DisposableEffect(context, currentLanguage) {
        var textToSpeech: TextToSpeech? = null
        val attrContext = context
        textToSpeech = TextToSpeech(attrContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val localeStr = "kn"
                textToSpeech?.language = Locale.Builder().setLanguage(localeStr).setRegion("IN").build()
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
    }

    var selectedConsonant by remember { mutableStateOf<KannadaLetter?>(null) }
    var selectedConsonantIndex by remember { mutableStateOf<Int?>(null) }
    
    val authState by authViewModel.authState.collectAsState()
    val userEmail = (authState as? AuthState.Authenticated)?.email ?: "guest"
    val prefs = getSharedPreferences("AppProgress")
    
    val langPrefix = currentLanguage.code
    val subscriptionPlan = prefs.getString("${langPrefix}_subscriptionPlan_$userEmail", "Free") ?: "Free"
    val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)
    val guninthakshariUnlocked = prefs.getInt("${userEmail}_${langPrefix}_guninthakshari_unlocked", 0)
    val highestUnlocked = if (isGuest) 0 else if (subscriptionPlan == "Gold") Int.MAX_VALUE else guninthakshariUnlocked

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedConsonant == null) titleText else "${selectedConsonant?.character} $titleText", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedConsonant != null) {
                            selectedConsonant = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BentoTitle)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BentoBackground)
                .padding(paddingValues)
        ) {
            if (selectedConsonant == null) {
                // Show list of consonants
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(LetterData.consonants.size) { index ->
                        val consonant = LetterData.consonants[index]
                        val isUnlocked = index <= highestUnlocked
                        ConsonantCard(consonant = consonant, isUnlocked = isUnlocked) {
                            if (isUnlocked) {
                                selectedConsonant = consonant
                                selectedConsonantIndex = index
                            } else {
                                Toast.makeText(context, "🔒 Locked: Sign in with Gmail to unlock all consonants!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                // Show combinations for selected consonant
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(signs.size) { signIndex ->
                        val (sign, transliterationSuffix) = signs[signIndex]
                        val isUnlocked = !isGuest || signIndex == 0
                        val combinedChar = selectedConsonant!!.character + sign
                        val combinedTransliteration = selectedConsonant!!.transliteration.replace(Regex("a$"), "") + transliterationSuffix
                        
                        CombinationCard(
                            char = combinedChar,
                            transliteration = combinedTransliteration,
                            isUnlocked = isUnlocked,
                            onClick = {
                                if (isUnlocked) {
                                    onNavigateToGameFlow(combinedChar, combinedTransliteration, selectedConsonantIndex ?: 0)
                                } else {
                                    Toast.makeText(context, "🔒 Locked: Sign in with Gmail to unlock all Gunintakshari combinations!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConsonantCard(consonant: KannadaLetter, isUnlocked: Boolean = true, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(enabled = isUnlocked, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (isUnlocked) BentoCard3Bg else Color.LightGray),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = consonant.character,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Color.White else Color.Gray
                )
                Text(
                    text = consonant.transliteration,
                    fontSize = 16.sp,
                    color = if (isUnlocked) Color.White.copy(alpha = 0.8f) else Color.Gray
                )
            }
            if (!isUnlocked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.Gray,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CombinationCard(char: String, transliteration: String, isUnlocked: Boolean = true, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (isUnlocked) BentoCard1Bg else Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = char,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) BentoCard1Text else Color.Gray
                )
                Text(
                    text = transliteration,
                    fontSize = 16.sp,
                    color = if (isUnlocked) BentoCard1Text.copy(alpha = 0.7f) else Color.Gray
                )
            }
            if (!isUnlocked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.Gray,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp)
                )
            }
        }
    }
}
