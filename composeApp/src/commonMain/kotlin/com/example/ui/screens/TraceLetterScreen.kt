package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TraceRepository
import com.example.ui.components.ConfettiEffect
import com.example.ui.components.LetterTraceVideoPlayer
import com.example.ui.theme.BrightCyan

data class TraceableLetter(
    val character: String,
    val transliteration: String,
    val isVowel: Boolean,
    val hasVideo: Boolean = false,
    val videoUrl: String = "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/main/Final_A.mp4"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraceLetterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("kn", "IN")
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    val letters = remember {
        val base = "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/main"
        listOf(
            // Vowels (Swaragalu) - All 15 vowels fetch reference video directly from git!
            TraceableLetter(
                "ಅ", "a", isVowel = true, hasVideo = true,
                videoUrl = "$base/Final_A.mp4"
            ),
            TraceableLetter(
                "ಆ", "aa", isVowel = true, hasVideo = true,
                videoUrl = "$base/Final%20AA.mp4"
            ),
            TraceableLetter(
                "ಇ", "i", isVowel = true, hasVideo = true,
                videoUrl = "$base/Final_E.mp4"
            ),
            TraceableLetter(
                "ಈ", "ee", isVowel = true, hasVideo = true,
                videoUrl = "$base/Final_EE.mp4"
            ),
            TraceableLetter(
                "ಉ", "u", isVowel = true, hasVideo = true,
                videoUrl = "$base/Final_u.mp4"
            ),
            TraceableLetter(
                "ಊ", "oo", isVowel = true, hasVideo = true,
                videoUrl = "$base/Final_uu.mp4"
            ),
            TraceableLetter(
                "ಋ", "ru", isVowel = true, hasVideo = true,
                videoUrl = "$base/Final_RU.mp4"
            ),
            TraceableLetter(
                "ಎ", "e", isVowel = true, hasVideo = true,
                videoUrl = "$base/Final_ye.mp4"
            ),
            TraceableLetter(
                "ಏ", "ea", isVowel = true, hasVideo = true,
                videoUrl = "$base/Final_yeee.mp4"
            ),
            TraceableLetter(
                "ಐ", "ai", isVowel = true, hasVideo = true,
                videoUrl = "$base/Final_I.mp4"
            ),
            TraceableLetter(
                "ಒ", "o", isVowel = true, hasVideo = true,
                videoUrl = "$base/Final_O.mp4"
            ),
            TraceableLetter(
                "ಓ", "oa", isVowel = true, hasVideo = true,
                videoUrl = "$base/Final_OO.mp4"
            ),
            TraceableLetter(
                "ಔ", "au", isVowel = true, hasVideo = true,
                videoUrl = "$base/Final_ouh.mp4"
            ),
            TraceableLetter(
                "ಅಂ", "am", isVowel = true, hasVideo = true,
                videoUrl = "$base/Final_AUM.mp4"
            ),
            TraceableLetter(
                "ಅಃ", "aha", isVowel = true, hasVideo = true,
                videoUrl = "$base/Final_AHA.mp4"
            ),
            // Consonants (Vyanjanagalu)
            TraceableLetter("ಕ", "ka", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_KA.mp4"),
            TraceableLetter("ಖ", "kha", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_KHA.mp4"),
            TraceableLetter("ಗ", "ga", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_GA.mp4"),
            TraceableLetter("ಘ", "gha", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_GHHHA.mp4"),
            TraceableLetter("ಙ", "nga", isVowel = false, hasVideo = true, videoUrl = "$base/Vowle_Inya.mp4"),
            TraceableLetter("ಚ", "cha", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_cha.mp4"),
            TraceableLetter("ಛ", "chha", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_CHHHA.mp4"),
            TraceableLetter("ಜ", "ja", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_JA.mp4"),
            TraceableLetter("ಝ", "jha", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_JJJAA.mp4"),
            TraceableLetter("ಞ", "nya", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_INI.mp4"),
            TraceableLetter("ಟ", "ta", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_Ta.mp4"),
            TraceableLetter("ಠ", "tha", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_tttta.mp4"),
            TraceableLetter("ಡ", "da", isVowel = false, hasVideo = true, videoUrl = "$base/Vowle_Da.mp4"),
            TraceableLetter("ಢ", "dha", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_DDDAA.mp4"),
            TraceableLetter("ಣ", "na", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_ANA.mp4"),
            TraceableLetter("ತ", "ta", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_Tha.mp4"),
            TraceableLetter("ಥ", "tha", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_THA2.mp4"),
            TraceableLetter("ದ", "da", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_Dha.mp4"),
            TraceableLetter("ಧ", "dha", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_DHA2.mp4"),
            TraceableLetter("ನ", "na", isVowel = false, hasVideo = true, videoUrl = "$base/Vowels_Na.mp4"),
            TraceableLetter("ಪ", "pa", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_Pa.mp4"),
            TraceableLetter("ಫ", "pha", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_Pha2.mp4"),
            TraceableLetter("ಬ", "ba", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_Ba.mp4"),
            TraceableLetter("ಭ", "bha", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_Bha2.mp4"),
            TraceableLetter("ಮ", "ma", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_ma.mp4"),
            TraceableLetter("ಯ", "ya", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_Ya.mp4"),
            TraceableLetter("ರ", "ra", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_Ra.mp4"),
            TraceableLetter("ಲ", "la", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_La.mp4"),
            TraceableLetter("ವ", "va", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_va.mp4"),
            TraceableLetter("ಶ", "sha", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel%20SSA.mp4"),
            TraceableLetter("ಷ", "ssa", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_Sha.mp4"),
            TraceableLetter("ಸ", "sa", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_Sa.mp4"),
            TraceableLetter("ಹ", "ha", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_ha.mp4"),
            TraceableLetter("ಳ", "la", isVowel = false, hasVideo = true, videoUrl = "$base/Vowel_lha.mp4")
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentLetter = letters[currentIndex]

    // Tracing stroke data
    var strokes by remember { mutableStateOf(listOf<List<Offset>>()) }
    var currentStroke by remember { mutableStateOf(listOf<Offset>()) }
    var showConfetti by remember { mutableStateOf(false) }

    fun clearTracing() {
        strokes = emptyList()
        currentStroke = emptyList()
    }

    fun speakCurrentLetter() {
        tts?.speak(currentLetter.character, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    val authPrefs = remember { getSharedPreferences("AppProgress") }
    val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
    val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)

    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem(currentIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Trace the Letter",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (currentLetter.hasVideo) "Watch video & trace below" else "Trace the letter easily",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { speakCurrentLetter() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Pronounce",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { clearTracing() }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrightCyan)
            )
        },
        containerColor = Color(0xFFFFFDE7) // Soft playful warm background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top horizontal selector for Kannada letters
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(letters) { index, item ->
                        val isSelected = index == currentIndex
                        val isLocked = isGuest && index > 0
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isLocked) Color(0xFFEEEEEE) else if (isSelected) Color(0xFFFF6F00) else Color.White,
                            border = BorderStroke(
                                1.5.dp,
                                if (isLocked) Color.LightGray else if (isSelected) Color(0xFFE65100) else Color(0xFFFFE082)
                            ),
                            shadowElevation = if (isSelected) 4.dp else 1.dp,
                            modifier = Modifier.clickable {
                                if (isLocked) {
                                    Toast.makeText(context, "🔒 Locked: Sign in with Gmail to trace all letters!", Toast.LENGTH_SHORT).show()
                                } else if (currentIndex != index) {
                                    clearTracing()
                                    currentIndex = index
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else if (item.hasVideo) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Video available",
                                        tint = if (isSelected) Color.White else Color(0xFFFF7043),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "${item.character} (${item.transliteration})",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (isLocked) Color.Gray else if (isSelected) Color.White else Color(0xFF5D4037)
                                )
                            }
                        }
                    }
                }

                // If letter 'a' (hasVideo is true), split screen into 2 horizontal sections!
                if (currentLetter.hasVideo) {
                    // SECTION 1 (ABOVE): Video Displayed
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 6.dp)
                    ) {
                        LetterTraceVideoPlayer(
                            modifier = Modifier.fillMaxSize(),
                            letterCharacter = currentLetter.character,
                            letterName = currentLetter.transliteration,
                            videoUrl = currentLetter.videoUrl
                        )
                    }

                    // Guidance Banner between sections
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE8F5E9),
                        border = BorderStroke(1.dp, Color(0xFFA5D6A7))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "✍️ Trace '${currentLetter.character}' below to practice",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    // SECTION 2 (BELOW): Tracing Exercise Displayed
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.05f)
                    ) {
                        TracingExerciseCard(
                            letter = currentLetter,
                            strokes = strokes,
                            currentStroke = currentStroke,
                            onStrokeChange = { newCurrent -> currentStroke = newCurrent },
                            onStrokeCompleted = { newStroke ->
                                strokes = strokes + listOf(newStroke)
                                currentStroke = emptyList()
                                if (strokes.size >= 2) {
                                    showConfetti = true
                                }
                            },
                            onClear = { 
                                com.example.ui.audio.SoundEffectsHelper.playButtonTap(context)
                                clearTracing() 
                            },
                            onSpeak = { 
                                com.example.ui.audio.SoundEffectsHelper.playButtonTap(context)
                                speakCurrentLetter() 
                            },
                            onNext = {
                                com.example.ui.audio.SoundEffectsHelper.playButtonTap(context)
                                if (isGuest) {
                                    Toast.makeText(context, "🔒 Locked: Sign in with Gmail to trace all letters!", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (strokes.isNotEmpty()) {
                                        TraceRepository.addTrace(currentLetter.character, strokes)
                                    }
                                    clearTracing()
                                    currentIndex = (currentIndex + 1) % letters.size
                                }
                            },
                            onPrevious = {
                                com.example.ui.audio.SoundEffectsHelper.playButtonTap(context)
                                clearTracing()
                                currentIndex = if (currentIndex > 0) currentIndex - 1 else letters.size - 1
                            }
                        )
                    }
                } else {
                    // Other letters without attached video: single expanded tracing exercise section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        TracingExerciseCard(
                            letter = currentLetter,
                            strokes = strokes,
                            currentStroke = currentStroke,
                            onStrokeChange = { newCurrent -> currentStroke = newCurrent },
                            onStrokeCompleted = { newStroke ->
                                strokes = strokes + listOf(newStroke)
                                currentStroke = emptyList()
                                if (strokes.size >= 2) {
                                    showConfetti = true
                                }
                            },
                            onClear = { clearTracing() },
                            onSpeak = { speakCurrentLetter() },
                            onNext = {
                                if (isGuest) {
                                    Toast.makeText(context, "🔒 Locked: Sign in with Gmail to trace all letters!", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (strokes.isNotEmpty()) {
                                        TraceRepository.addTrace(currentLetter.character, strokes)
                                    }
                                    clearTracing()
                                    currentIndex = (currentIndex + 1) % letters.size
                                }
                            },
                            onPrevious = {
                                clearTracing()
                                currentIndex = if (currentIndex > 0) currentIndex - 1 else letters.size - 1
                            }
                        )
                    }
                }
            }

            // Confetti effect celebration on completing tracing
            if (showConfetti) {
                LaunchedEffect(Unit) {
                    com.example.ui.audio.SoundEffectsHelper.playGameCompletion(context)
                    kotlinx.coroutines.delay(2500)
                    showConfetti = false
                }
                ConfettiEffect(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun TracingExerciseCard(
    letter: TraceableLetter,
    strokes: List<List<Offset>>,
    currentStroke: List<Offset>,
    onStrokeChange: (List<Offset>) -> Unit,
    onStrokeCompleted: (List<Offset>) -> Unit,
    onClear: () -> Unit,
    onSpeak: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Interactive Tracing Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFAFAFA))
                    .pointerInput(letter.character) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                onStrokeChange(listOf(offset))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                onStrokeChange(currentStroke + change.position)
                            },
                            onDragEnd = {
                                if (currentStroke.isNotEmpty()) {
                                    onStrokeCompleted(currentStroke)
                                }
                            },
                            onDragCancel = {
                                onStrokeChange(emptyList())
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Background Faint Letter Reference
                Text(
                    text = letter.character,
                    fontSize = 170.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE0E0E0)
                )

                // Canvas drawing user strokes
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val traceColor = Color(0xFF43A047) // Vibrant Green
                    val strokeWidth = 32f

                    strokes.forEach { stroke ->
                        if (stroke.size > 1) {
                            val path = Path().apply {
                                moveTo(stroke.first().x, stroke.first().y)
                                for (i in 1 until stroke.size) {
                                    lineTo(stroke[i].x, stroke[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = traceColor,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        } else if (stroke.size == 1) {
                            drawCircle(
                                color = traceColor,
                                radius = strokeWidth / 2,
                                center = stroke.first()
                            )
                        }
                    }

                    if (currentStroke.size > 1) {
                        val path = Path().apply {
                            moveTo(currentStroke.first().x, currentStroke.first().y)
                            for (i in 1 until currentStroke.size) {
                                lineTo(currentStroke[i].x, currentStroke[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFF2E7D32),
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    } else if (currentStroke.size == 1) {
                        drawCircle(
                            color = Color(0xFF2E7D32),
                            radius = strokeWidth / 2,
                            center = currentStroke.first()
                        )
                    }
                }

                // Audio button in top-end corner
                IconButton(
                    onClick = onSpeak,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                        .shadow(2.dp, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speak letter",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Indicator if no strokes drawn yet
                if (strokes.isEmpty() && currentStroke.isEmpty()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "Trace with finger",
                            fontSize = 11.sp,
                            color = Color(0xFF616161),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons Row: Previous, Clear, Next
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous letter button
                OutlinedButton(
                    onClick = onPrevious,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(42.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF757575))
                ) {
                    Icon(
                        Icons.Default.NavigateBefore,
                        contentDescription = "Previous letter",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Clear button
                Button(
                    onClick = onClear,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .weight(1f)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                // Next letter button
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = BrightCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .weight(1f)
                ) {
                    Text("Next", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.NavigateNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
