package com.example.ui.screens

import com.example.platform.*

import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KannadaLetter
import com.example.data.LetterData
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import com.example.ui.theme.BentoTitle
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.BrightGreen
import com.example.ui.theme.BrightOrange
import com.example.ui.theme.Pink
import com.example.ui.theme.BentoBackground
import com.example.ui.theme.BrightYellow
import com.example.ui.theme.DeepBlue
import androidx.compose.material.icons.filled.VolumeUp
import kotlinx.coroutines.delay
import kotlin.random.Random

import com.example.data.TraceRepository

@Composable
fun TraceGame(
    letter: KannadaLetter,
    onSpeakText: (String) -> Unit = {},
    onComplete: () -> Unit
) {
    val baseAssets = "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/main"
    val vowelVideoUrl = letter.videoUrl ?: when (letter.character) {
        "ಅ" -> "$baseAssets/Final_A.mp4"
        "ಆ" -> "$baseAssets/Final%20AA.mp4"
        "ಇ" -> "$baseAssets/Final_E.mp4"
        "ಈ" -> "$baseAssets/Final_EE.mp4"
        "ಉ" -> "$baseAssets/Final_u.mp4"
        "ಊ" -> "$baseAssets/Final_uu.mp4"
        "ಋ" -> "$baseAssets/Final_RU.mp4"
        "ಎ" -> "$baseAssets/Final_ye.mp4"
        "ಏ" -> "$baseAssets/Final_yeee.mp4"
        "ಐ" -> "$baseAssets/Final_I.mp4"
        "ಒ" -> "$baseAssets/Final_O.mp4"
        "ಓ" -> "$baseAssets/Final_OO.mp4"
        "ಔ" -> "$baseAssets/Final_ouh.mp4"
        "ಅಂ" -> "$baseAssets/Final_AUM.mp4"
        "ಅಃ" -> "$baseAssets/Final_AHA.mp4"
        "ಕ" -> "$baseAssets/Vowel_KA.mp4"
        "ಖ" -> "$baseAssets/Vowel_KHA.mp4"
        "ಗ" -> "$baseAssets/Vowel_GA.mp4"
        "ಘ" -> "$baseAssets/Vowel_GHHHA.mp4"
        "ಙ" -> "$baseAssets/Vowle_Inya.mp4"
        "ಚ" -> "$baseAssets/Vowel_cha.mp4"
        "ಛ" -> "$baseAssets/Vowel_CHHHA.mp4"
        "ಜ" -> "$baseAssets/Vowel_JA.mp4"
        "ಝ" -> "$baseAssets/Vowel_JJJAA.mp4"
        "ಞ" -> "$baseAssets/Vowel_INI.mp4"
        "ಟ" -> "$baseAssets/Vowel_Ta.mp4"
        "ಠ" -> "$baseAssets/Vowel_tttta.mp4"
        "ಡ" -> "$baseAssets/Vowle_Da.mp4"
        "ಢ" -> "$baseAssets/Vowel_DDDAA.mp4"
        "ಣ" -> "$baseAssets/Vowel_ANA.mp4"
        "ತ" -> "$baseAssets/Vowel_Tha.mp4"
        "ಥ" -> "$baseAssets/Vowel_THA2.mp4"
        "ದ" -> "$baseAssets/Vowel_Dha.mp4"
        "ಧ" -> "$baseAssets/Vowel_DHA2.mp4"
        "ನ" -> "$baseAssets/Vowels_Na.mp4"
        "ಪ" -> "$baseAssets/Vowel_Pa.mp4"
        "ಫ" -> "$baseAssets/Vowel_Pha2.mp4"
        "ಬ" -> "$baseAssets/Vowel_Ba.mp4"
        "ಭ" -> "$baseAssets/Vowel_Bha2.mp4"
        "ಮ" -> "$baseAssets/Vowel_ma.mp4"
        "ಯ" -> "$baseAssets/Vowel_Ya.mp4"
        "ರ" -> "$baseAssets/Vowel_Ra.mp4"
        "ಲ" -> "$baseAssets/Vowel_La.mp4"
        "ವ" -> "$baseAssets/Vowel_va.mp4"
        "ಶ" -> "$baseAssets/Vowel%20SSA.mp4"
        "ಷ" -> "$baseAssets/Vowel_Sha.mp4"
        "ಸ" -> "$baseAssets/Vowel_Sa.mp4"
        "ಹ" -> "$baseAssets/Vowel_ha.mp4"
        "ಳ" -> "$baseAssets/Vowel_lha.mp4"
        else -> null
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (vowelVideoUrl != null) {
            // Video on top (smaller), big tracing area below
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f)
                    .padding(bottom = 6.dp)
            ) {
                com.example.ui.components.LetterTraceVideoPlayer(
                    modifier = Modifier.fillMaxSize(),
                    letterCharacter = letter.character,
                    letterName = letter.transliteration,
                    videoUrl = vowelVideoUrl
                )
            }
        }
        TraceExercise(
            letter = letter,
            onSpeakText = onSpeakText,
            onComplete = onComplete,
            modifier = Modifier.fillMaxWidth().weight(if (vowelVideoUrl != null) 1.5f else 1f)
        )
    }
}

@Composable
fun ParentalGate(
    letter: KannadaLetter,
    onApproved: () -> Unit,
    onRejected: () -> Unit
) {
    val context = LocalContext.current
    val onSpeakText: (String) -> Unit = { text -> 
        com.example.ui.audio.NativeAudioPlayer.playPhonics(context, text) 
    }
    var showMathChallenge by remember { mutableStateOf(false) }
    var mathAnswer by remember { mutableStateOf("") }
    val num1 by remember { mutableStateOf((1..10).random()) }
    val num2 by remember { mutableStateOf((1..10).random()) }
    var isFreeWriteMode by remember { mutableStateOf(false) }
    var freeWriteStrokes by remember { mutableStateOf(listOf<List<Offset>>()) }
    var currentStroke by remember { mutableStateOf(listOf<Offset>()) }

    if (isFreeWriteMode) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Write '${letter.character}' without help!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BentoTitle,
                modifier = Modifier.padding(16.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentStroke = listOf(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentStroke = currentStroke + change.position
                            },
                            onDragEnd = {
                                if (currentStroke.isNotEmpty()) {
                                    freeWriteStrokes = freeWriteStrokes + listOf(currentStroke)
                                    currentStroke = emptyList()
                                }
                            },
                            onDragCancel = {
                                currentStroke = emptyList()
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeColor = BentoTitle
                    val strokeWidth = 24f

                    freeWriteStrokes.forEach { stroke ->
                        if (stroke.size > 1) {
                            val path = Path().apply {
                                moveTo(stroke.first().x, stroke.first().y)
                                for (i in 1 until stroke.size) {
                                    lineTo(stroke[i].x, stroke[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = strokeColor,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        } else if (stroke.size == 1) {
                            drawCircle(color = strokeColor, radius = strokeWidth / 2, center = stroke.first())
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
                            color = strokeColor,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    } else if (currentStroke.size == 1) {
                        drawCircle(color = strokeColor, radius = strokeWidth / 2, center = currentStroke.first())
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { freeWriteStrokes = emptyList() },
                    colors = ButtonDefaults.buttonColors(containerColor = Pink)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Clear")
                    Spacer(Modifier.width(8.dp))
                    Text("Clear")
                }
                Button(
                    onClick = {
                        if (freeWriteStrokes.isNotEmpty()) {
                            TraceRepository.addTrace(letter.character, freeWriteStrokes)
                        }
                        isFreeWriteMode = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightGreen)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Done")
                    Spacer(Modifier.width(8.dp))
                    Text("Done")
                }
            }
        }
        return
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier.fillMaxSize().imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Parental Verification",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = BentoTitle,
            modifier = Modifier.padding(16.dp)
        )
        Text(
            text = "Did the child trace '${letter.character}' correctly?",
            fontSize = 18.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
        ) {
            val traces = TraceRepository.userTraces[letter.character]
            Text(
                text = letter.character,
                fontSize = 180.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable { onSpeakText(letter.character) }
            )
            IconButton(
                onClick = { onSpeakText(letter.character) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.White.copy(alpha = 0.8f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play sound", tint = BentoTitle)
            }

            if (!traces.isNullOrEmpty()) {
                val lastTrace = traces.last()
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeColor = BentoTitle
                    val strokeWidth = 24f
    
                    lastTrace.forEach { stroke ->
                        if (stroke.size > 1) {
                            val path = Path().apply {
                                moveTo(stroke.first().x, stroke.first().y)
                                for (i in 1 until stroke.size) {
                                    lineTo(stroke[i].x, stroke[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = strokeColor,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        } else if (stroke.size == 1) {
                            drawCircle(color = strokeColor, radius = strokeWidth / 2, center = stroke.first())
                        }
                    }
                }
            }
        }

        if (showMathChallenge) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "For Parents: What is $num1 + $num2?", 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                OutlinedTextField(
                    value = mathAnswer,
                    onValueChange = { mathAnswer = it },
                    label = { Text("Answer") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            if (mathAnswer.trim() == (num1 + num2).toString()) {
                                onApproved()
                            } else {
                                mathAnswer = ""
                            }
                        }
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Button(
                    onClick = {
                        keyboardController?.hide()
                        if (mathAnswer.trim() == (num1 + num2).toString()) {
                            onApproved()
                        } else {
                            mathAnswer = ""
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrightGreen)
                ) {
                    Text("Confirm", color = Color.White)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onRejected,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("No, Try Again")
                }
                Button(
                    onClick = { isFreeWriteMode = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightOrange)
                ) {
                    Text("Ask to Write")
                }
                Button(
                    onClick = { showMathChallenge = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightGreen)
                ) {
                    Text("Looks Good")
                }
            }
        }
    }
}

data class Bubble(
    val id: Int,
    var x: Float,
    var y: Float,
    val letter: String,
    val isTarget: Boolean,
    var popped: Boolean = false,
    val color: Color
)

@Composable
fun BubbleGame(
    targetLetter: KannadaLetter,
    onSpeakText: (String) -> Unit,
    onComplete: () -> Unit
) {
    var score by remember { mutableStateOf(0) }
    var bubbles by remember { mutableStateOf(listOf<Bubble>()) }
    var nextBubbleId by remember { mutableStateOf(0) }
    var showConfetti by remember { mutableStateOf(false) }
    
    val targetScore = 20
    val bubbleColors = listOf(
        Color(0xFFFFB3BA), // Pastel Red
        Color(0xFFFFDFBA), // Pastel Orange
        Color(0xFFFFFFBA), // Pastel Yellow
        Color(0xFFBAFFC9), // Pastel Green
        Color(0xFFBAE1FF), // Pastel Blue
        Color(0xFFE6B3FF)  // Pastel Purple
    )

    LaunchedEffect(Unit) {
        onSpeakText(targetLetter.character)
        while (score < targetScore) {
            delay(1200) // Spawns bubbles a bit slower since we need 20
            val isTarget = Random.nextFloat() > 0.4f
            val charToUse = if (isTarget) targetLetter.character else LetterData.allLetters.random().character
            val newBubble = Bubble(
                id = nextBubbleId++,
                x = Random.nextFloat(), // 0.1 to 0.9 later when drawing
                y = -0.2f, // start above screen
                letter = charToUse,
                isTarget = isTarget,
                color = bubbleColors.random()
            )
            bubbles = bubbles + newBubble
        }
    }

    LaunchedEffect(Unit) {
        while (score < targetScore) {
            delay(32)
            bubbles = bubbles.mapNotNull {
                if (it.popped) null
                else if (it.y > 1.2f) null
                else it.copy(y = it.y + 0.005f) // fall down slower
            }
        }
    }

    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            delay(2500)
            onComplete()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFE0F7FA)), // Light sky blue background
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Pop '${targetLetter.character}' bubbles!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = BentoTitle,
            modifier = Modifier.padding(16.dp)
        )
        Text(
            text = "Score: $score / $targetScore",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = BrightOrange
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .background(Color(0xFF87CEEB).copy(alpha = 0.3f), RoundedCornerShape(16.dp)) // Sky blue theme
                .clip(RoundedCornerShape(16.dp))
        ) {
            val widthPx = maxWidth.value
            val heightPx = maxHeight.value

            bubbles.forEach { bubble ->
                val xPos = bubble.x * (widthPx - 80f) + 40f
                val yPos = bubble.y * heightPx
                
                Box(
                    modifier = Modifier
                        .offset(x = xPos.dp - 40.dp, y = yPos.dp - 40.dp)
                        .size(80.dp) // Increased bubble size
                        .background(bubble.color.copy(alpha = 0.85f), CircleShape)
                        .clickable {
                            if (!bubble.popped) {
                                if (bubble.isTarget) {
                                    onSpeakText(bubble.letter)
                                    score++
                                    if (score >= targetScore && !showConfetti) {
                                        showConfetti = true
                                    }
                                }
                                bubbles = bubbles.map { if (it.id == bubble.id) it.copy(popped = true) else it }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bubble.letter,
                        fontSize = 40.sp, // Increased letter size
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }
            }
        }
        if (showConfetti) {
            ConfettiAnimation()
        }
    }
}

data class FallingLetter(
    val id: Int,
    var x: Float,
    var y: Float,
    val letter: String,
    val isTarget: Boolean,
    var caught: Boolean = false
)

@Composable
fun CatchGame(
    targetLetter: KannadaLetter,
    onSpeakText: (String) -> Unit,
    onComplete: () -> Unit
) {
    var score by remember { mutableStateOf(0) }
    var fallingLetters by remember { mutableStateOf(listOf<FallingLetter>()) }
    var nextId by remember { mutableStateOf(0) }
    var basketX by remember { mutableStateOf(0.5f) }
    var showConfetti by remember { mutableStateOf(false) }
    
    val targetScore = 20

    LaunchedEffect(Unit) {
        onSpeakText(targetLetter.character)
        while (score < targetScore) {
            delay(1500)
            val isTarget = Random.nextFloat() > 0.4f
            val charToUse = if (isTarget) targetLetter.character else LetterData.allLetters.random().character
            val newLetter = FallingLetter(
                id = nextId++,
                x = Random.nextFloat() * 0.8f + 0.1f,
                y = -0.1f,
                letter = charToUse,
                isTarget = isTarget
            )
            fallingLetters = fallingLetters + newLetter
        }
    }

    LaunchedEffect(Unit) {
        while (score < targetScore) {
            delay(32)
            fallingLetters = fallingLetters.mapNotNull {
                if (it.caught) null
                else if (it.y > 1.1f) null
                else {
                    val nextY = it.y + 0.008f
                    // Check collision with basket
                    if (nextY > 0.85f && nextY < 1.0f && kotlin.math.abs(it.x - basketX) < 0.2f) {
                        if (it.isTarget) {
                            onSpeakText(it.letter)
                            score++
                            if (score >= targetScore && !showConfetti) {
                                showConfetti = true
                            }
                        }
                        it.copy(caught = true)
                    } else {
                        it.copy(y = nextY)
                    }
                }
            }
        }
    }

    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            delay(2500)
            onComplete()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFE0F7FA)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Catch '${targetLetter.character}'!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = BentoTitle,
            modifier = Modifier.padding(16.dp)
        )
        Text(
            text = "Score: $score / $targetScore",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = BrightOrange
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .background(Pink.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        basketX = (basketX + dragAmount.x / size.width).coerceIn(0.1f, 0.9f)
                    }
                }
        ) {
            val widthPx = maxWidth.value
            val heightPx = maxHeight.value

            fallingLetters.forEach { falling ->
                val xPos = falling.x * widthPx
                val yPos = falling.y * heightPx
                
                Box(
                    modifier = Modifier
                        .offset(x = xPos.dp - 40.dp, y = yPos.dp - 40.dp)
                        .size(80.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = falling.letter,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTitle
                    )
                }
            }

            // Basket
            Box(
                modifier = Modifier
                    .offset(x = (basketX * widthPx).dp - 60.dp, y = (0.9f * heightPx).dp)
                    .size(width = 120.dp, height = 60.dp)
                    .background(BrightOrange, RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            )
        }
        if (showConfetti) {
            ConfettiAnimation()
        }
    }
}

@Composable
fun WordsActivity(
    targetLetter: KannadaLetter,
    onSpeakText: (String) -> Unit,
    onComplete: () -> Unit
) {
    val wordsMap = mapOf(
        "ಅ" to Pair("ಅಮ್ಮ", "Mother (Amma)"),
        "ಆ" to Pair("ಆನೆ", "Elephant (Aane)"),
        "ಇ" to Pair("ಇಲಿ", "Rat (Ili)"),
        "ಈ" to Pair("ಈರುಳ್ಳಿ", "Onion (Eerulli)"),
        "ಉ" to Pair("ಉಂಗುರ", "Ring (Ungura)"),
        "ಊ" to Pair("ಊಟ", "Food (Oota)"),
        "ಎ" to Pair("ಎಲೆ", "Leaf (Ele)"),
        "ಏ" to Pair("ಏಡಿ", "Crab (Aedi)"),
        "ಕ" to Pair("ಕಮಲ", "Lotus (Kamala)"),
        "ಚ" to Pair("ಚಮಚ", "Spoon (Chamacha)"),
        "ಟ" to Pair("ಟೊಮೇಟೊ", "Tomato (Tomato)"),
        "ತ" to Pair("ತಬಲ", "Tabla (Tabala)"),
        "ಪ" to Pair("ಪಟ", "Kite (Pata)"),
        "ಬ" to Pair("ಬೆಕ್ಕು", "Cat (Bekku)"),
        "ಮ" to Pair("ಮರ", "Tree (Mara)"),
        "ರ" to Pair("ರಥ", "Chariot (Ratha)")
    )

    val wordInfo = wordsMap[targetLetter.character] ?: Pair("${targetLetter.character} ಪದ", "Word with ${targetLetter.character}")

    LaunchedEffect(Unit) {
        onSpeakText(wordInfo.first)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFE0F7FA)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Word Activity",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = BentoTitle,
            modifier = Modifier.padding(16.dp)
        )
        
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BrightCyan.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = wordInfo.first,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BentoTitle
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = wordInfo.second,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrightOrange
                )
            }
        }
        
        Button(
            onClick = onComplete,
            colors = ButtonDefaults.buttonColors(containerColor = BrightGreen),
            modifier = Modifier.padding(16.dp).height(56.dp).fillMaxWidth(0.8f)
        ) {
            Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Finish", fontSize = 20.sp)
        }
    }
}

data class ConfettiParticle(
    val x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val color: Color,
    val size: Float,
    var rotation: Float,
    val rotationSpeed: Float
)

@Composable
fun ConfettiAnimation() {
    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta, Color.Cyan, BrightOrange, Pink)
    var particles by remember {
        mutableStateOf(List(100) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -1f, // start above screen
                speedX = (Random.nextFloat() - 0.5f) * 0.01f,
                speedY = Random.nextFloat() * 0.02f + 0.01f,
                color = colors.random(),
                size = Random.nextFloat() * 15f + 10f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 10f - 5f
            )
        })
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            particles = particles.map {
                it.copy(
                    x = it.x + it.speedX,
                    y = it.y + it.speedY,
                    rotation = it.rotation + it.rotationSpeed
                )
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = maxWidth.value
        val height = maxHeight.value

        particles.forEach { particle ->
            if (particle.y < 1.2f) { // only draw if on or above screen
                Box(
                    modifier = Modifier
                        .offset(x = (particle.x * width).dp, y = (particle.y * height).dp)
                        .size(particle.size.dp)
                        .background(particle.color, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
fun QuizMode(
    targetLetter: KannadaLetter,
    onSpeakText: (String) -> Unit,
    onComplete: () -> Unit
) {
    val allLetters = remember { com.example.data.LetterData.vowels + com.example.data.LetterData.consonants }
    var options by remember { mutableStateOf(listOf<KannadaLetter>()) }
    var resultMessage by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        val distractors = allLetters.filter { it.id != targetLetter.id }.shuffled().take(3)
        options = (distractors + targetLetter).shuffled()
        onSpeakText(targetLetter.character)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BentoBackground).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Listen and select the correct letter!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = BentoTitle,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { onSpeakText(targetLetter.character) },
            colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
            modifier = Modifier.size(80.dp),
            shape = androidx.compose.foundation.shape.CircleShape
        ) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play sound", tint = Color.White, modifier = Modifier.size(40.dp))
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.take(2).forEach { option ->
                LetterOptionCard(letter = option, onClick = {
                    if (option.id == targetLetter.id) {
                        resultMessage = "Correct!"
                        onComplete()
                    } else {
                        resultMessage = "Try again!"
                        onSpeakText(targetLetter.character)
                    }
                })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.drop(2).forEach { option ->
                LetterOptionCard(letter = option, onClick = {
                    if (option.id == targetLetter.id) {
                        resultMessage = "Correct!"
                        onComplete()
                    } else {
                        resultMessage = "Try again!"
                        onSpeakText(targetLetter.character)
                    }
                })
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = resultMessage,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (resultMessage == "Correct!") BrightGreen else Color.Red
        )
    }
}

@Composable
fun LetterOptionCard(letter: KannadaLetter, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = BrightYellow),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = letter.character,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = BentoTitle
            )
        }
    }
}

