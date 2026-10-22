package com.example.ui.screens

import com.example.platform.*

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.data.KannadaLetter
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.BrightGreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.AuthState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.data.AppLanguage
import com.example.ui.data.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterGameFlowScreen(
    type: String,
    index: Int,
    char: String = "",
    translit: String = "",
    onBack: () -> Unit,
    onComplete: () -> Unit,
    authViewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val items = when (type) {
        "vowels" -> com.example.data.LetterData.vowels
        "consonants" -> com.example.data.LetterData.consonants
        "numbers" -> com.example.data.LetterData.numbers
        else -> emptyList()
    }
    
    val letter = if (type == "guninthakshari") {
        KannadaLetter(id = char.hashCode(), character = char, transliteration = translit, isVowel = false)
    } else {
        items.getOrNull(index) ?: return
    }
    
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    val authState by authViewModel.authState.collectAsState()
    val userEmail = (authState as? AuthState.Authenticated)?.email ?: "guest"
    val prefs = getSharedPreferences("AppProgress")
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val langCode = currentLanguage.code
    val highestUnlocked = prefs.getInt("${userEmail}_${langCode}_${type}_unlocked", 0)

    DisposableEffect(context) {
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

    var currentStep by remember { mutableStateOf(0) }
    var showSurpriseSticker by remember { mutableStateOf(false) }
    
    val speakText = { text: String ->
        com.example.ui.audio.NativeAudioPlayer.playPhonics(context, text)
        Unit
    }

    val topBarColor = when (type) {
        "vowels" -> BrightCyan
        "consonants" -> BrightGreen
        "numbers" -> Color(0xFFFF9800)
        else -> Color(0xFFBA68C8) // Guninthakshari color
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Learn ${letter.character}", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (currentStep) {
                0 -> TraceGame(
                    letter = letter,
                    onSpeakText = speakText,
                    onComplete = { currentStep++ }
                )
                1 -> BubbleGame(
                    targetLetter = letter,
                    onSpeakText = speakText,
                    onComplete = { currentStep++ }
                )
                2 -> CatchGame(
                    targetLetter = letter,
                    onSpeakText = speakText,
                    onComplete = {
                        com.example.ui.data.ActivityTracker.markLearned(type, index)
                        val maxItems = if (type == "guninthakshari") com.example.data.LetterData.consonants.size else items.size
                        val previousBadges = com.example.ui.data.BadgeManager.badges.count { it.isUnlocked(prefs, userEmail, langCode) }
                        
                        if (index == highestUnlocked && index < maxItems - 1) {
                            prefs.edit().putInt("${userEmail}_${langCode}_${type}_unlocked", highestUnlocked + 1).apply()
                        }
                        
                        val newBadges = com.example.ui.data.BadgeManager.badges.count { it.isUnlocked(prefs, userEmail, langCode) }
                        
                        if (newBadges > previousBadges) {
                            com.example.ui.data.AudioFeedbackManager.playBadgeUnlocked(context)
                        } else {
                            com.example.ui.data.AudioFeedbackManager.playLevelComplete(context)
                        }
                        showSurpriseSticker = true
                    }
                )
            }

            if (showSurpriseSticker) {
                com.example.ui.components.SurpriseStickerDialog(
                    userEmail = userEmail,
                    levelName = "${type.replaceFirstChar { it.uppercase() }} - Level ${index + 1} (${letter.character})",
                    seedModifier = index + letter.character.hashCode(),
                    onClose = {
                        showSurpriseSticker = false
                        onComplete()
                    },
                    onClaimOrNext = {
                        showSurpriseSticker = false
                        onComplete()
                    }
                )
            }
        }
    }
}
