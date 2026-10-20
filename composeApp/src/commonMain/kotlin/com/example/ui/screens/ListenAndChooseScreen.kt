package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.components.ConfettiEffect
import com.example.ui.components.PauseMenuDialog
import com.example.ui.components.SurpriseStickerDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ListenLevel(
    val levelNumber: Int,
    val title: String,
    val subtitleKannada: String,
    val letterPool: List<String>,
    val targetRounds: Int = 5
)

val listenAndChooseLevels = listOf(
    ListenLevel(
        levelNumber = 1,
        title = "Short Vowels",
        subtitleKannada = "ಹ್ರಸ್ವ ಸ್ವರಗಳು",
        letterPool = listOf("ಅ", "ಇ", "ಉ", "ಋ", "ಎ", "ಒ")
    ),
    ListenLevel(
        levelNumber = 2,
        title = "Long Vowels",
        subtitleKannada = "ದೀರ್ಘ ಸ್ವರಗಳು",
        letterPool = listOf("ಆ", "ಈ", "ಊ", "ಏ", "ಐ", "ಓ", "ಔ")
    ),
    ListenLevel(
        levelNumber = 3,
        title = "Yogavahagalu & Vowels",
        subtitleKannada = "ಯೋಗವಾಹಗಳು & ಸ್ವರಗಳು",
        letterPool = listOf("ಅ", "ಆ", "ಅಂ", "ಅಃ", "ಉ", "ಊ")
    ),
    ListenLevel(
        levelNumber = 4,
        title = "Ka-Varga Consonants",
        subtitleKannada = "ಕ-ವರ್ಗ ವ್ಯಂಜನಗಳು",
        letterPool = listOf("ಕ", "ಖ", "ಗ", "ಘ", "ಙ")
    ),
    ListenLevel(
        levelNumber = 5,
        title = "Cha-Varga Consonants",
        subtitleKannada = "ಚ-ವರ್ಗ ವ್ಯಂಜನಗಳು",
        letterPool = listOf("ಚ", "ಛ", "ಜ", "ಝ", "ಞ")
    ),
    ListenLevel(
        levelNumber = 6,
        title = "Ta-Varga Consonants",
        subtitleKannada = "ಟ-ವರ್ಗ ವ್ಯಂಜನಗಳು",
        letterPool = listOf("ಟ", "ಠ", "ಡ", "ಢ", "ಣ")
    ),
    ListenLevel(
        levelNumber = 7,
        title = "Tha-Varga Consonants",
        subtitleKannada = "ತ-ವರ್ಗ ವ್ಯಂಜನಗಳು",
        letterPool = listOf("ತ", "ಥ", "ದ", "ಧ", "ನ")
    ),
    ListenLevel(
        levelNumber = 8,
        title = "Pa-Varga Consonants",
        subtitleKannada = "ಪ-ವರ್ಗ ವ್ಯಂಜನಗಳು",
        letterPool = listOf("ಪ", "ಫ", "ಬ", "ಭ", "ಮ")
    ),
    ListenLevel(
        levelNumber = 9,
        title = "Semi-vowels & Sibilants",
        subtitleKannada = "ಅವರ್ಗೀಯ ವ್ಯಂಜನಗಳು",
        letterPool = listOf("ಯ", "ರ", "ಲ", "ವ", "ಶ", "ಷ", "ಸ", "ಹ", "ಳ")
    ),
    ListenLevel(
        levelNumber = 10,
        title = "Grand Master Challenge",
        subtitleKannada = "ಸಂಪೂರ್ಣ ಕನ್ನಡ ಪರೀಕ್ಷೆ",
        letterPool = listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ", "ಕ", "ಗ", "ಚ", "ಜ", "ಟ", "ತ", "ದ", "ಪ", "ಬ", "ಮ", "ಯ", "ರ", "ಲ")
    ),
    ListenLevel(
        levelNumber = 11,
        title = "Guninthakshari - Ka Series",
        subtitleKannada = "ಕ ಗುಣಿತಾಕ್ಷರಗಳು",
        letterPool = listOf("ಕ", "ಕಾ", "ಕಿ", "ಕೀ", "ಕು", "ಕೂ")
    ),
    ListenLevel(
        levelNumber = 12,
        title = "Guninthakshari - Ke Series",
        subtitleKannada = "ಕೆ ಗುಣಿತಾಕ್ಷರಗಳು",
        letterPool = listOf("ಕೃ", "ಕೆ", "ಕೇ", "ಕೈ", "ಕೊ", "ಕೋ", "ಕೌ", "ಕಂ", "ಕಃ")
    ),
    ListenLevel(
        levelNumber = 13,
        title = "Guninthakshari - Ga Series",
        subtitleKannada = "ಗ ಗುಣಿತಾಕ್ಷರಗಳು",
        letterPool = listOf("ಗ", "ಗಾ", "ಗಿ", "ಗೀ", "ಗು", "ಗೂ", "ಗೆ", "ಗೇ", "ಗೈ")
    ),
    ListenLevel(
        levelNumber = 14,
        title = "Kannada Numbers 1 to 5",
        subtitleKannada = "ಸಂಖ್ಯೆಗಳು ೧ ರಿಂದ ೫",
        letterPool = listOf("೧", "೨", "೩", "೪", "೫")
    ),
    ListenLevel(
        levelNumber = 15,
        title = "Kannada Numbers 6 to 10",
        subtitleKannada = "ಸಂಖ್ಯೆಗಳು ೬ ರಿಂದ ೧೦",
        letterPool = listOf("೬", "೭", "೮", "೯", "೧೦")
    ),
    ListenLevel(
        levelNumber = 16,
        title = "Mahaprana Consonants",
        subtitleKannada = "ಮಹಾಪ್ರಾಣ ವ್ಯಂಜನಗಳು",
        letterPool = listOf("ಖ", "ಘ", "ಛ", "ಝ", "ಠ", "ಢ", "ಥ", "ಧ", "ಫ", "ಭ")
    ),
    ListenLevel(
        levelNumber = 17,
        title = "Alpaprana Consonants",
        subtitleKannada = "ಅಲ್ಪಪ್ರಾಣ ವ್ಯಂಜನಗಳು",
        letterPool = listOf("ಕ", "ಗ", "ಚ", "ಜ", "ಟ", "ಡ", "ತ", "ದ", "ಪ", "ಬ")
    ),
    ListenLevel(
        levelNumber = 18,
        title = "Anunasika Consonants",
        subtitleKannada = "ಅನುನಾಸಿಕಗಳು",
        letterPool = listOf("ಙ", "ಞ", "ಣ", "ನ", "ಮ")
    ),
    ListenLevel(
        levelNumber = 19,
        title = "Common Words",
        subtitleKannada = "ಸರಳ ಕನ್ನಡ ಪದಗಳು",
        letterPool = listOf("ಅಮ್ಮ", "ಅಪ್ಪ", "ಆನೆ", "ಗಿಳಿ", "ಮರ", "ಹೂವು", "ಹಾಲು", "ಬಾಳೆ")
    ),
    ListenLevel(
        levelNumber = 20,
        title = "Ultimate Kannada Master",
        subtitleKannada = "ಅಲ್ಟಿಮೇಟ್ ಕನ್ನಡ ವಿದ್ವಾಂಸ",
        letterPool = listOf("ಅ", "ಕ", "ಗ", "ಚ", "ಟ", "ತ", "ಪ", "ಯ", "ರ", "ಲ", "ವ", "ಶ", "ಸ", "ಹ", "ಳ", "೧", "೫", "೧೦")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenAndChooseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { getSharedPreferences("GameScores") }
    var score by remember { mutableIntStateOf(sharedPrefs.getInt("listen_and_choose_score", 0)) }
    val coroutineScope = rememberCoroutineScope()

    var currentLevelIndex by remember { mutableIntStateOf(0) }
    val currentLevel = listenAndChooseLevels[currentLevelIndex]

    var currentRoundInLevel by remember { mutableIntStateOf(1) }
    val totalRoundsInLevel = currentLevel.targetRounds

    var correctLetter by remember { mutableStateOf("") }
    var options by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSuccess by remember { mutableStateOf(false) }
    var rewardCharacter by remember { mutableStateOf<String?>(null) }
    var showLevelComplete by remember { mutableStateOf(false) }
    var showLevelPicker by remember { mutableStateOf(false) }
    var showPauseMenu by remember { mutableStateOf(false) }
    var wrongIndex by remember { mutableIntStateOf(-1) }
    var ttsInitialized by remember { mutableStateOf(false) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

    DisposableEffect(Unit) {
        onDispose {
            try { toneGen.release() } catch (e: Exception) {}
        }
    }

    // Play letter sound safely via NativeAudioPlayer + TTS
    fun playLetterSound(letter: String) {
        if (letter.isEmpty()) return
        NativeAudioPlayer.playPhonics(context, letter)
        tts?.speak(letter, TextToSpeech.QUEUE_FLUSH, null, "letter_audio")
    }

    fun generateRound() {
        val pool = currentLevel.letterPool.shuffled()
        val chosen = pool[0]
        correctLetter = chosen

        // 3 options including correct one
        val wrongOptions = pool.filter { it != chosen }.shuffled().take(2)
        val allOptions = (wrongOptions + chosen).shuffled()
        options = allOptions

        showSuccess = false
        wrongIndex = -1

        // Play letter audio with slight delay
        coroutineScope.launch {
            delay(350)
            playLetterSound(chosen)
        }
    }

    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("kn", "IN")
                ttsInitialized = true
            }
        }
        tts = textToSpeech
        onDispose {
            try { textToSpeech.stop(); textToSpeech.shutdown() } catch (e: Exception) {}
        }
    }

    // Generate first round on launch and whenever level changes
    LaunchedEffect(currentLevelIndex) {
        currentRoundInLevel = 1
        showLevelComplete = false
        generateRound()
    }

    // Pulse animation on speaker button
    val infiniteTransition = rememberInfiniteTransition(label = "speaker_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    if (showPauseMenu) {
        PauseMenuDialog(
            onDismiss = { showPauseMenu = false },
            onRestart = {
                showPauseMenu = false
                currentRoundInLevel = 1
                generateRound()
            },
            onMainMenu = onBack
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Level ${currentLevel.levelNumber}/10: ${currentLevel.title}",
                            fontSize = 17.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentLevel.subtitleKannada,
                            fontSize = 12.sp,
                            color = Color(0xFFD1C4E9)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showLevelPicker = true }) {
                        Icon(Icons.Default.List, contentDescription = "Select Level", tint = Color.White)
                    }
                    IconButton(onClick = { showPauseMenu = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF512DA8))
            )
        },
        containerColor = Color(0xFFEDE7F6)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (showSuccess) {
                ConfettiEffect(modifier = Modifier.zIndex(10f))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Progress & Score Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question $currentRoundInLevel of $totalRoundsInLevel",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF512DA8)
                            )
                            Surface(
                                color = Color(0xFFEDE7F6),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "🪙 Total Score: $score",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF673AB7),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { (currentRoundInLevel.toFloat() / totalRoundsInLevel.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF673AB7),
                            trackColor = Color(0xFFE0E0E0)
                        )
                    }
                }

                // Middle: Big Interactive Speaker Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF7E57C2), Color(0xFF4527A0))
                                )
                            )
                            .border(4.dp, Color.White, CircleShape)
                            .clickable {
                                playLetterSound(correctLetter)
                            }
                            .testTag("speaker_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Listen",
                            tint = Color.White,
                            modifier = Modifier.size(68.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Listen carefully & tap the letter!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF311B92),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "ಕೇಳಿ ಮತ್ತು ಸರಿಯಾದ ಅಕ್ಷರವನ್ನು ಆರಿಸಿ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF7E57C2)
                    )
                }

                // Options Row: 3 Big Letter Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    options.forEachIndexed { index, letter ->
                        val isError = (wrongIndex == index)
                        val isCorrect = (showSuccess && letter == correctLetter)

                        val bgColor by animateColorAsState(
                            targetValue = when {
                                isCorrect -> Color(0xFF4CAF50)
                                isError -> Color(0xFFE53935)
                                else -> Color.White
                            },
                            label = "bgColor"
                        )

                        val textColor by animateColorAsState(
                            targetValue = if (isCorrect || isError) Color.White else Color(0xFF311B92),
                            label = "textColor"
                        )

                        Card(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    width = 3.dp,
                                    color = if (isError) Color.Red else Color(0xFF7E57C2),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable(enabled = !showSuccess) {
                                    if (letter == correctLetter) {
                                        showSuccess = true
                                        rewardCharacter = letter
                                        com.example.ui.audio.SoundEffectsHelper.playSuccess(context)
                                        try { com.example.ui.data.ScoreManager.addCoins(context, "listen_and_choose", 10) } catch (e: Exception) {}
                                        score += 1
                                        sharedPrefs.edit().putInt("listen_and_choose_score", score).apply()

                                        coroutineScope.launch {
                                            delay(1400)
                                            if (currentRoundInLevel < totalRoundsInLevel) {
                                                currentRoundInLevel++
                                                generateRound()
                                            } else {
                                                // Completed all 5 rounds of this level!
                                                showLevelComplete = true
                                            }
                                        }
                                    } else {
                                        wrongIndex = index
                                        com.example.ui.audio.SoundEffectsHelper.playOops(context)
                                        coroutineScope.launch {
                                            delay(500)
                                            if (wrongIndex == index) wrongIndex = -1
                                        }
                                    }
                                }
                                .testTag("option_$letter"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = letter,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            // Google Pay Style Surprise Sticker Dialog on completing level!
            if (showLevelComplete) {
                val authPrefs = getSharedPreferences("AppProgress")
                val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
                val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)
                SurpriseStickerDialog(
                    userEmail = userEmail,
                    levelName = "Listen & Choose Level ${currentLevel.levelNumber}",
                    seedModifier = currentLevelIndex * 7,
                    onClose = {
                        showLevelComplete = false
                        if (isGuest) {
                            onBack()
                        } else if (currentLevelIndex < listenAndChooseLevels.size - 1) {
                            currentLevelIndex++
                        }
                    },
                    onClaimOrNext = {
                        showLevelComplete = false
                        if (isGuest) {
                            Toast.makeText(context, "🔒 Level 1 Complete! Sign in with Gmail to play all levels!", Toast.LENGTH_LONG).show()
                            onBack()
                        } else if (currentLevelIndex < listenAndChooseLevels.size - 1) {
                            currentLevelIndex++
                        } else {
                            onBack()
                        }
                    }
                )
            }

            // Level Selection Sheet
            if (showLevelPicker) {
                val authPrefs = getSharedPreferences("AppProgress")
                val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
                val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)

                AlertDialog(
                    onDismissRequest = { showLevelPicker = false },
                    title = { Text("Choose Level (1 - ${listenAndChooseLevels.size})", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            listenAndChooseLevels.forEachIndexed { idx, lvl ->
                                val isLevelLocked = isGuest && idx > 0
                                Surface(
                                    color = if (isLevelLocked) Color(0xFFEEEEEE) else if (idx == currentLevelIndex) Color(0xFFD1C4E9) else Color(0xFFF5F5F5),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isLevelLocked) {
                                                Toast.makeText(context, "🔒 Locked: Sign in with Gmail to unlock all levels!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                currentLevelIndex = idx
                                                showLevelPicker = false
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Level ${lvl.levelNumber}: ${lvl.title}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                if (isLevelLocked) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Text(lvl.subtitleKannada, fontSize = 12.sp, color = Color.Gray)
                                        }
                                        if (idx == currentLevelIndex) {
                                            Text("Active", color = Color(0xFF512DA8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLevelPicker = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Lottie Animation Reward whenever child correctly identifies a Kannada character
            com.example.ui.components.LottieCharacterRewardOverlay(
                visible = rewardCharacter != null && !showLevelComplete,
                character = rewardCharacter ?: "",
                onDismiss = { rewardCharacter = null }
            )
        }
    }
}
