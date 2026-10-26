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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
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

    // Print worksheet for this letter: a grown-up answers the sum, then sees a preview with Print.
    val worksheetSection = com.example.ui.data.Worksheets.Section.fromLearnedType(type)
    val worksheetSheet = worksheetSection?.let { com.example.ui.data.Worksheets.item(it, index) }
    val canPrint = worksheetSheet != null
    var showPrintGate by remember { mutableStateOf(false) }
    var showPrintPreview by remember { mutableStateOf(false) }
    // What to do once the grown-up has answered: open the preview, or print straight away.
    var previewAfterGate by remember { mutableStateOf(true) }
    val printScope = rememberCoroutineScope()
    
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

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        bottomBar = {
            if (canPrint && worksheetSheet != null) {
                PracticeOnPaperBar(
                    letter = letter.character,
                    pages = worksheetSheet.pages,
                    locked = !com.example.ui.data.Worksheets.isOpen(worksheetSheet),
                    onPreview = { previewAfterGate = true; showPrintGate = true },
                    onPrint = { previewAfterGate = false; showPrintGate = true }
                )
            }
        },
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
                // Right after tracing: colour the letter in
                1 -> ColourLetterGame(
                    letter = letter,
                    onSpeakText = speakText,
                    onComplete = { currentStep++ }
                )
                2 -> BubbleGame(
                    targetLetter = letter,
                    onSpeakText = speakText,
                    onComplete = { currentStep++ }
                )
                3 -> CatchGame(
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

    // Practice on paper: the grown-up sum first, then the preview or the print sheet.
    if (showPrintGate) {
        ParentGate(
            purpose = "Answer to print the ${letter.character} worksheet",
            onPass = {
                showPrintGate = false
                val section = worksheetSection
                val sheet = worksheetSheet
                if (previewAfterGate) {
                    showPrintPreview = true
                } else if (section != null && sheet != null) {
                    if (!com.example.ui.data.Worksheets.isOpen(sheet)) {
                        com.example.ui.data.Worksheets.askForGold(section)
                    } else {
                        printScope.launch {
                            if (!com.example.ui.data.Worksheets.print(context, listOf(sheet), "Kannada ${section.plural}")) {
                                Toast.makeText(context, "Could not open the worksheet. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            },
            onCancel = { showPrintGate = false }
        )
    }
    if (showPrintPreview && worksheetSection != null && worksheetSheet != null) {
        SingleWorksheetPreview(worksheetSection, worksheetSheet, onClose = { showPrintPreview = false })
    }
    }
}

/**
 * Bottom bar on a letter lesson: "Practice on paper". Preview shows the worksheet pages first;
 * Print goes straight to the print sheet. Both ask the grown-up sum first.
 */
@Composable
private fun PracticeOnPaperBar(
    letter: String,
    pages: Int,
    locked: Boolean,
    onPreview: () -> Unit,
    onPrint: () -> Unit
) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("practice_on_paper_bar"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🖨️", fontSize = 24.sp)
            Column(Modifier.weight(1f)) {
                Text(
                    "Practice on paper" + if (locked) "  👑" else "",
                    fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1F2A44)
                )
                Text(
                    "Want more practice? Print the $letter worksheet ($pages pages).",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5B6B82), maxLines = 2
                )
            }
            // Preview and Print share one solid style, so neither looks greyed out.
            Button(
                onClick = onPreview,
                enabled = true,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E86DE), contentColor = Color.White),
                modifier = Modifier.testTag("practice_preview")
            ) {
                Text("Preview", fontWeight = FontWeight.ExtraBold)
            }
            Button(
                onClick = onPrint,
                enabled = true,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E86DE), contentColor = Color.White),
                modifier = Modifier.testTag("practice_print")
            ) {
                Text("Print", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
