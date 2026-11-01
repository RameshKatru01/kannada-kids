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

/** The vowel signs for the Ka-kaa (Kannada) or Barakhadi (Hindi) grid. */
val kaKaaSigns: List<Pair<String, String>> get() = if (com.example.data.Lang.isHindi) barakhadiSigns else guninthakshariSigns

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
    
    val signs = kaKaaSigns
    val titleText = if (com.example.data.Lang.isHindi) "Barakhadi" else "Guninthakshari"
    
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
    val highestUnlocked = if (com.example.ui.data.FeatureFlags.UNLOCK_ALL) Int.MAX_VALUE else if (isGuest) 0 else if (subscriptionPlan == "Gold") Int.MAX_VALUE else guninthakshariUnlocked
    val learnVersion by com.example.ui.data.ActivityTracker.version.collectAsState()
    val sub by com.example.ui.data.SubscriptionManager.state.collectAsState()

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
                        val state = remember(learnVersion, sub, index) { com.example.ui.data.Access.letter("guninthakshari", index) }
                        ConsonantCard(consonant = consonant, state = state) {
                            when (state) {
                                com.example.ui.data.Access.State.Open -> {
                                    selectedConsonant = consonant
                                    selectedConsonantIndex = index
                                }
                                com.example.ui.data.Access.State.NeedsPremium -> com.example.ui.data.Access.askGrownUp("${consonant.character} and all the other ${com.example.data.Lang.islandName("guninthakshari")} letters are in the Gold pack.")
                                com.example.ui.data.Access.State.NeedsProgress -> {
                                    val nextChar = LetterData.consonants.getOrNull(com.example.ui.data.Access.nextIndex("guninthakshari"))?.character ?: ""
                                    com.example.ui.data.Access.showHint("Letters open one by one. Next up is $nextChar!")
                                }
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
                        val signState = com.example.ui.data.Access.kaKaaSign(selectedConsonantIndex ?: 0, signIndex)
                        val isUnlocked = signState == com.example.ui.data.Access.State.Open
                        val combinedChar = selectedConsonant!!.character + sign
                        val combinedTransliteration = selectedConsonant!!.transliteration.replace(Regex("a$"), "") + transliterationSuffix
                        
                        CombinationCard(
                            char = combinedChar,
                            transliteration = combinedTransliteration,
                            state = signState,
                            onClick = {
                                if (isUnlocked) {
                                    onNavigateToGameFlow(combinedChar, combinedTransliteration, selectedConsonantIndex ?: 0)
                                } else {
                                    com.example.ui.data.Access.askGrownUp("$combinedChar and all the other ${com.example.data.Lang.islandName("guninthakshari")} letters are in the Gold pack.")
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
fun ConsonantCard(consonant: KannadaLetter, state: com.example.ui.data.Access.State = com.example.ui.data.Access.State.Open, onClick: () -> Unit) {
    val isUnlocked = state == com.example.ui.data.Access.State.Open
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
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
                    color = if (isUnlocked) Color.White else Color(0xFF4E6276)
                )
                Text(
                    text = consonant.transliteration,
                    fontSize = 16.sp,
                    color = if (isUnlocked) Color.White.copy(alpha = 0.8f) else Color.Gray
                )
            }
            AccessBadge(state, Modifier.align(Alignment.TopEnd).padding(6.dp), size = 28.dp)
        }
    }
}

@Composable
fun CombinationCard(char: String, transliteration: String, state: com.example.ui.data.Access.State = com.example.ui.data.Access.State.Open, onClick: () -> Unit = {}) {
    val isUnlocked = state == com.example.ui.data.Access.State.Open
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
                    color = if (isUnlocked) BentoCard1Text else Color(0xFF4E6276)
                )
                Text(
                    text = transliteration,
                    fontSize = 16.sp,
                    color = if (isUnlocked) BentoCard1Text.copy(alpha = 0.7f) else Color.Gray
                )
            }
            AccessBadge(state, Modifier.align(Alignment.TopEnd).padding(6.dp), size = 28.dp)
        }
    }
}
