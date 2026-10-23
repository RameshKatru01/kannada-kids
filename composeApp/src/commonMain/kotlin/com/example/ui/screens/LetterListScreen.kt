package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KannadaLetter
import com.example.data.LetterData
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.BrightGreen
import com.example.ui.theme.RainbowColors
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.AuthState
import kotlin.random.Random

/*
 * The letters of one island (Vowels / Consonants / Numbers) as big tiles:
 * learned (✓), next (highlighted), not reached yet (🔒, letter still shown) or Gold pack (👑).
 * Tapping a locked tile says what to do; the card on top shows the way to the next island.
 */
@Composable
fun LetterListScreen(
    type: String,
    onBack: () -> Unit,
    onNavigateToGameFlow: (String, Int) -> Unit,
    authViewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val letters = when (type) { "vowels" -> LetterData.vowels; "consonants" -> LetterData.consonants; "numbers" -> LetterData.numbers; else -> emptyList() }
    val title = when (type) { "vowels" -> "Vowels · ಸ್ವರಗಳು"; "consonants" -> "Consonants · ವ್ಯಂಜನಗಳು"; "numbers" -> "Numbers · ಅಂಕೆಗಳು"; else -> "" }
    val accent = when (type) { "vowels" -> Color(0xFF2EC4B6); "consonants" -> Color(0xFF6C63FF); else -> Color(0xFFFF9F1C) }
    val tint = when (type) { "vowels" -> Color(0xFFD9F6F2); "consonants" -> Color(0xFFE6E4FF); else -> Color(0xFFFFE6CC) }
    val ink = Color(0xFF0B3B66)
    val inkSoft = Color(0xFF3A5F82)
    val context = LocalContext.current

    val version by com.example.ui.data.ActivityTracker.version.collectAsState()
    val pathVersion by com.example.ui.data.Access.version.collectAsState()
    val sub by com.example.ui.data.SubscriptionManager.state.collectAsState()
    val learned = remember(version, type) { com.example.ui.data.ActivityTracker.learned(type) }
    val states = remember(version, pathVersion, sub, type, letters.size) {
        letters.indices.map { com.example.ui.data.Access.letter(type, it) }
    }
    val premium = remember(sub, pathVersion) { com.example.ui.data.Access.isPremium() }
    val next = remember(version, type) { com.example.ui.data.Access.nextIndex(type) }
    val done = learned.count { it in letters.indices }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFEAF6FF))
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ink) }
            Column {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ink, maxLines = 1)
                Text("$done of ${letters.size} learned", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = inkSoft)
            }
        }

        // Goal card
        val (goal, goalFraction) = when {
            !premium -> "Try ${letters.firstOrNull()?.character ?: ""} for free. Gold 👑 opens every letter!" to (done.toFloat() / letters.size.coerceAtLeast(1))
            type == "vowels" && done < letters.size -> "Learn all ${letters.size} vowels to open the Consonants island 🏝️" to (done.toFloat() / letters.size)
            type == "vowels" -> "All vowels learned! The Consonants island is open 🎉" to 1f
            type == "consonants" && done < letters.size -> "Learn all ${letters.size} consonants to open the Ka-kaa island 🏝️" to (done.toFloat() / letters.size)
            type == "consonants" -> "All consonants learned! The Ka-kaa island is open 🎉" to 1f
            else -> "$done of ${letters.size} numbers learned" to (done.toFloat() / letters.size.coerceAtLeast(1))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .clickable(enabled = !premium) { com.example.ui.data.Access.askGrownUp() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(46.dp).background(if (premium) tint else Color(0xFFFFF1C4), CircleShape), contentAlignment = Alignment.Center) {
                if (premium) Icon(Icons.Default.Lock, contentDescription = null, tint = accent) else Text("👑", fontSize = 22.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(goal, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ink, lineHeight = 18.sp)
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFE6ECF1))) {
                    Box(Modifier.fillMaxWidth(goalFraction.coerceIn(0f, 1f)).fillMaxHeight().background(accent))
                }
            }
        }

        // Letter tiles
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(78.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(letters.size) { index ->
                val letter = letters[index]
                val state = states[index]
                val isLearned = index in learned
                val isNext = state == com.example.ui.data.Access.State.Open && index == next && !isLearned
                LetterTile(
                    glyph = letter.character,
                    state = state,
                    learned = isLearned,
                    next = isNext,
                    accent = accent,
                    tint = tint,
                    onClick = {
                        when (state) {
                            com.example.ui.data.Access.State.Open -> {
                                com.example.ui.audio.NativeAudioPlayer.playPhonics(context, letter.character)
                                onNavigateToGameFlow(type, index)
                            }
                            com.example.ui.data.Access.State.NeedsPremium ->
                                com.example.ui.data.Access.askGrownUp("${letter.character} and all the other letters are in the Gold pack.")
                            com.example.ui.data.Access.State.NeedsProgress -> {
                                val first = letters.getOrNull(next)?.character ?: ""
                                com.example.ui.data.Access.showHint(
                                    if (index == next + 1) "Finish $first first, then ${letter.character} opens!"
                                    else "Letters open one by one. Next up is $first!"
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LetterTile(
    glyph: String,
    state: com.example.ui.data.Access.State,
    learned: Boolean,
    next: Boolean,
    accent: Color,
    tint: Color,
    onClick: () -> Unit
) {
    val ink = Color(0xFF0B3B66)
    val locked = state != com.example.ui.data.Access.State.Open
    val pulse = rememberInfiniteTransition(label = "next")
    val grow by pulse.animateFloat(1f, 1.06f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "grow")
    val shape = RoundedCornerShape(22.dp)
    Box(Modifier.fillMaxWidth().aspectRatio(0.95f)) {
        Box(
            Modifier
                .fillMaxSize()
                .scale(if (next) grow else 1f)
                .clip(shape)
                .background(
                    when {
                        next -> accent
                        learned -> tint
                        locked -> Color(0xFFE9EEF3)
                        else -> Color.White
                    }
                )
                .border(
                    if (next) 4.dp else 2.dp,
                    when {
                        next -> Color.White
                        state == com.example.ui.data.Access.State.NeedsPremium -> Color(0xFFF2C94C)
                        locked -> Color(0xFFB8C4CF)
                        else -> Color.Transparent
                    },
                    shape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                glyph,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (locked) Color(0xFF4E6276) else ink
            )
        }
        when {
            learned && !locked -> Box(
                Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).size(24.dp).background(accent, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("✓", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
            locked -> AccessBadge(state, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp), size = 26.dp)
        }
        if (next) {
            Text(
                "Next!",
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 8.dp)
                    .background(Color(0xFFFF6B6B), RoundedCornerShape(10.dp))
                    .padding(horizontal = 9.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun LetterAdventureMap(
    modifier: Modifier = Modifier,
    items: List<KannadaLetter>,
    highestUnlocked: Int,
    onLetterClick: (Int, KannadaLetter) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "path_animation")
    val pathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "pathPhase"
    )

    // A colorful background based on modulo math
    val bgColors = listOf(Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFFFD54F), Color(0xFFBA68C8))

    BoxWithConstraints(modifier = modifier.fillMaxWidth().background(bgColors[items.size % bgColors.size].copy(alpha = 0.5f))) {
        val mapWidth = maxWidth
        val stepHeight = 160.dp
        val totalHeight = (stepHeight * (items.size + 1)).coerceAtLeast(maxHeight)
        
        // Scroll state needs to be scrolled to the bottom (first letter)
        val scrollState = rememberScrollState(
            initial = Int.MAX_VALUE
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .height(totalHeight)
        ) {
            // Draw vibrant background features
            Canvas(modifier = Modifier.fillMaxSize()) {
                val riverCount = (size.height / 1000f).toInt() + 1
                for(i in 0..riverCount) {
                    val riverY = i * 1000f
                    drawPath(
                        path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, riverY)
                            quadraticTo(size.width * 0.5f, riverY + 300f, size.width, riverY - 100f)
                            lineTo(size.width, riverY + 300f)
                            quadraticTo(size.width * 0.5f, riverY + 700f, 0f, riverY + 200f)
                            close()
                        },
                        color = Color(0xFF4FC3F7).copy(alpha = 0.4f)
                    )
                }
            }

            // Calculate positions from BOTTOM to TOP
            val positions = items.mapIndexed { index, _ ->
                val yOffset = totalHeight - (stepHeight * (index + 1)) - 80.dp
                val xSine = kotlin.math.sin(index * 1.5)
                val xOffset = (mapWidth / 2) + (xSine * (mapWidth.value * 0.25)).dp - 40.dp
                Pair(xOffset, yOffset)
            }

            // Decorate with emojis
            val decorations = listOf("🌲", "🌳", "🌺", "🌸", "🦋", "🍄", "🌷", "🌻", "🍎", "🦜")
            positions.forEachIndexed { i, pos ->
                val x = pos.first
                val y = pos.second
                // Randomly place decorations around the path
                val side = if (i % 2 == 0) -80.dp else 120.dp
                Text(
                    text = decorations[i % decorations.size],
                    fontSize = 48.sp,
                    modifier = Modifier.offset(x = x + side, y = y + 20.dp)
                )
            }

            // Draw connected paths
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0 until positions.size - 1) {
                    val start = positions[i]
                    val end = positions[i + 1]
                    
                    val startX = start.first.toPx() + 40.dp.toPx()
                    val startY = start.second.toPx() + 40.dp.toPx()
                    val endX = end.first.toPx() + 40.dp.toPx()
                    val endY = end.second.toPx() + 40.dp.toPx()
                    
                    val controlX1 = startX
                    val controlY1 = startY + (endY - startY) / 2
                    val controlX2 = endX
                    val controlY2 = startY + (endY - startY) / 2
                    
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(startX, startY)
                        cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)
                    }
                    
                    val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(30f, 20f), phase = pathPhase * 2f)
                    val isUnlocked = i < highestUnlocked
                    val pathColor = if (isUnlocked) Color(0xFF795548) else Color.LightGray
                    val pathInnerColor = if (isUnlocked) RainbowColors[i % RainbowColors.size] else Color.Gray

                    drawPath(
                        path = path,
                        color = pathColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 24.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = pathEffect
                        )
                    )
                    drawPath(
                        path = path,
                        color = pathInnerColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 16.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = pathEffect
                        )
                    )
                }
            }

            // Draw nodes and decorations
            positions.forEachIndexed { index, (x, y) ->
                val letter = items[index]
                val isUnlocked = index <= highestUnlocked
                val rainbowColor = if (isUnlocked) RainbowColors[index % RainbowColors.size] else Color.Gray

                val mapContext = LocalContext.current
                Box(
                    modifier = Modifier
                        .offset(x = x, y = y)
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(rainbowColor)
                        .clickable {
                            if (isUnlocked) {
                                onLetterClick(index, letter)
                            } else {
                                Toast.makeText(mapContext, "🔒 Locked: Sign in with Gmail to unlock all letters!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .border(4.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isUnlocked) {
                        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.DarkGray, modifier = Modifier.size(32.dp))
                    } else {
                        Text(
                            text = letter.character,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )
                    }
                }
                
                if (isUnlocked) {
                    Text(
                        text = letter.transliteration,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .offset(x = x + 10.dp, y = y + 90.dp)
                            .background(Color(0xFF37474F).copy(alpha=0.8f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
