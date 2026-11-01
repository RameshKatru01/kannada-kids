package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KannadaLetter
import com.example.data.OttaLetter
import com.example.data.OttaksharaData
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.data.Access
import com.example.ui.data.ActivityTracker
import com.example.ui.components.readableWidth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * Ottakshara island (ಒತ್ತಕ್ಷರ): the list of 30 joined letters in 4 groups, and a 4-step lesson for
 * each: Build it (ಕ + ಕ = ಕ್ಕ), Trace it, Read the word, Spot it (pick the right spelling).
 */

private val OttaColor = Color(0xFF14A098)
private val OttaTint = Color(0xFFD3F4EF)
private val OttaInk = Color(0xFF0B3B66)
private val OttaInkSoft = Color(0xFF3A5F82)
private val OttaBg = Color(0xFFEAF6FF)
private const val TYPE = "ottakshara"

@Composable
private fun OttaHeader(title: String, subtitle: String?, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OttaInk) }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = OttaInk, maxLines = 1)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = OttaInkSoft)
        }
        trailing()
    }
}

// ---------------------------------------------------------------------------------------------
// The island: groups of tiles
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OttaksharaScreen(onBack: () -> Unit, onOpenLesson: (Int) -> Unit) {
    val context = LocalContext.current
    val version by ActivityTracker.version.collectAsState()
    val pathVersion by Access.version.collectAsState()
    val sub by com.example.ui.data.SubscriptionManager.state.collectAsState()
    val letters = OttaksharaData.letters
    val learned = remember(version) { ActivityTracker.learned(TYPE) }
    val states = remember(version, pathVersion, sub) { letters.indices.map { Access.letter(TYPE, it) } }
    val next = remember(version) { Access.nextIndex(TYPE) }

    Column(
        Modifier
            .fillMaxSize()
            .background(OttaBg)
            .safeDrawingPadding()
            .readableWidth(720.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OttaHeader("${com.example.data.Lang.islandName("ottakshara")} · ${com.example.data.Lang.islandNative("ottakshara")}", "${learned.count { it in letters.indices }} of ${letters.size} learned", onBack)

        // How it works
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(com.example.data.Lang.script("ಕ + ಕ = ಕ್ಕ"), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = OttaInk)
            Text(
                "The second letter sits small, under the first. Say them together!",
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OttaInkSoft, lineHeight = 15.sp
            )
        }

        OttaksharaData.groups.forEachIndexed { g, group ->
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(group.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = OttaInk)
                Text(group.kannada, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OttaInkSoft)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 6
            ) {
                letters.forEachIndexed { i, l ->
                    if (l.group != g) return@forEachIndexed
                    val state = states[i]
                    val isLearned = i in learned
                    val isNext = state == Access.State.Open && i == next && !isLearned
                    OttaTile(
                        glyph = l.glyph, state = state, learned = isLearned, next = isNext,
                        modifier = Modifier.weight(1f).testTag("otta_tile_$i")
                    ) {
                        when (state) {
                            Access.State.Open -> {
                                NativeAudioPlayer.playPhonics(context, l.glyph)
                                onOpenLesson(i)
                            }
                            Access.State.NeedsPremium -> Access.askGrownUp("${l.glyph} and all the other ottaksharas are in the Gold pack.")
                            Access.State.NeedsProgress -> Access.showHint(
                                if (!Access.islandReached(TYPE)) Access.islandHint(TYPE)
                                else "Ottaksharas open one by one. Next up is ${letters.getOrNull(next)?.glyph ?: ""}!"
                            )
                        }
                    }
                }
                // Keep the last row's tiles the same size
                val inGroup = letters.count { it.group == g }
                repeat((6 - inGroup % 6) % 6) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun OttaTile(glyph: String, state: Access.State, learned: Boolean, next: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val locked = state != Access.State.Open
    val pulse = rememberInfiniteTransition(label = "next")
    val grow by pulse.animateFloat(1f, 1.07f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "grow")
    val shape = RoundedCornerShape(16.dp)
    Box(modifier.height(58.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .scale(if (next) grow else 1f)
                .clip(shape)
                .background(
                    when {
                        next -> OttaColor
                        learned -> OttaTint
                        locked -> Color(0xFFE9EEF3)
                        else -> Color.White
                    }
                )
                .border(
                    2.dp,
                    when {
                        next -> Color.White
                        state == Access.State.NeedsPremium -> Color(0xFFF2C94C)
                        locked -> Color(0xFFB8C4CF)
                        else -> Color.Transparent
                    },
                    shape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(glyph, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = if (next) Color.White else if (locked) Color(0xFF4E6276) else OttaInk)
        }
        when {
            learned && !locked -> Box(
                Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).size(20.dp).background(OttaColor, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("✓", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
            locked -> AccessBadge(state, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp), size = 22.dp)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// A lesson: Build it → Trace it → Read the word → Spot it
// ---------------------------------------------------------------------------------------------

private val stepNames = listOf("Build it", "Trace it", "Read it", "Spot it")

@Composable
fun OttaksharaLessonScreen(index: Int, onBack: () -> Unit) {
    val letter = OttaksharaData.letters.getOrNull(index) ?: return
    val context = LocalContext.current
    var step by remember(index) { mutableIntStateOf(0) }
    var finished by remember(index) { mutableStateOf(false) }
    val speak: (String) -> Unit = { NativeAudioPlayer.playPhonics(context, it) }

    Box(Modifier.fillMaxSize().background(OttaBg)) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .readableWidth(720.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OttaHeader("Learn ${letter.glyph}", null, onBack) {
                Text("${step + 1} · ${stepNames[step]}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = OttaInkSoft)
            }
            // Step dots
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                stepNames.indices.forEach { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (i == step) 22.dp else 8.dp, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (i <= step) OttaColor else Color(0xFFCFE0E8))
                    )
                }
            }
            when (step) {
                0 -> BuildStep(letter, speak) { step = 1 }
                1 -> TraceExercise(
                    letter = KannadaLetter(id = 9000 + index, character = letter.glyph, transliteration = "", isVowel = false),
                    onSpeakText = speak,
                    onComplete = { step = 2 },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                2 -> ReadStep(letter, speak) { step = 3 }
                else -> SpotStep(letter, speak) {
                    ActivityTracker.markLearned(TYPE, index)
                    finished = true
                }
            }
        }
        if (finished) {
            ConfettiAnimation()
            LaunchedEffect(Unit) {
                SoundEffectsHelper.playGameCompletion(context)
                delay(1800)
                onBack()
            }
        }
    }
}

@Composable
private fun ColumnScope.BuildStep(letter: OttaLetter, speak: (String) -> Unit, onNext: () -> Unit) {
    LaunchedEffect(letter.glyph) { delay(400); speak(letter.glyph) }
    // ಕ + ಕ → ಕ್ಕ
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Color.White).padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PartCard(letter.first, "first", Color(0xFFFFF3D6)) { speak(letter.first) }
        Text(" + ", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = OttaColor)
        PartCard(letter.second, "second", Color(0xFFFFF3D6)) { speak(letter.second) }
        Text(" → ", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = OttaColor)
        PartCard(letter.glyph, "together", OttaTint) { speak(letter.glyph) }
    }
    // The second letter shrinks and slides under the first, over and over
    val anim = rememberInfiniteTransition(label = "build")
    val t by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "t")
    val slide = (t / 0.6f).coerceIn(0f, 1f)          // 0..1 while moving
    val showJoined = t > 0.7f
    Column(
        Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(26.dp)).background(Color.White).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            if (showJoined) {
                Text(letter.glyph, fontSize = 110.sp, fontWeight = FontWeight.ExtraBold, color = OttaInk)
            } else {
                Text(letter.first, fontSize = 110.sp, fontWeight = FontWeight.ExtraBold, color = OttaInk,
                    modifier = Modifier.graphicsLayer { translationX = -40.dp.toPx() * (1f - slide) })
                Text(
                    letter.second, fontSize = 110.sp, fontWeight = FontWeight.ExtraBold, color = OttaColor,
                    modifier = Modifier.graphicsLayer {
                        val s = 1f - 0.6f * slide
                        scaleX = s; scaleY = s
                        translationX = 60.dp.toPx() * (1f - slide)
                        translationY = 55.dp.toPx() * slide
                        alpha = 1f - 0.2f * slide
                    }
                )
            }
        }
        Text(
            "The second ${letter.second} becomes small and slides underneath",
            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = OttaInk, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        HearButton("Hear it: ${letter.glyph}") { speak(letter.glyph) }
    }
    OttaNextButton("Next: trace it", onNext)
}

@Composable
private fun PartCard(glyph: String, caption: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.size(width = 74.dp, height = 84.dp).clip(RoundedCornerShape(20.dp)).background(color).clickable { onClick() },
            contentAlignment = Alignment.Center
        ) { Text(glyph, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = OttaInk) }
        Text(caption, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = OttaInkSoft)
    }
}

@Composable
private fun HearButton(label: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(22.dp)).background(OttaTint).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = OttaInk)
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = OttaInk)
    }
}

@Composable
private fun OttaNextButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).background(OttaColor).clickable { onClick() }.testTag("otta_next"),
        contentAlignment = Alignment.Center
    ) { Text("$label  ▶", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
}

@Composable
private fun ColumnScope.ReadStep(letter: OttaLetter, speak: (String) -> Unit, onNext: () -> Unit) {
    LaunchedEffect(letter.word) { delay(400); speak(letter.word) }
    Column(
        Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(26.dp)).background(Color.White).clickable { speak(letter.word) }.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
    ) {
        Text(letter.picture, fontSize = 96.sp)
        Text(letter.word, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = OttaInk)
        Text(letter.english, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OttaInkSoft)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Can you find", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = OttaInk)
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(OttaTint).padding(horizontal = 10.dp, vertical = 2.dp)) {
                Text(letter.glyph, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = OttaInk)
            }
            Text("in the word?", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = OttaInk)
        }
        HearButton("Hear the word") { speak(letter.word) }
    }
    OttaNextButton("Next: spot it", onNext)
}

@Composable
private fun ColumnScope.SpotStep(letter: OttaLetter, speak: (String) -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // The right word, the word without the ottakshara, and the word with another ottakshara
    val options = remember(letter) {
        val other = OttaksharaData.letters.filter { it.glyph != letter.glyph && it.group == letter.group }.random()
        listOf(letter.word, letter.withoutOtta, letter.word.replaceFirst(letter.glyph, other.glyph))
            .distinct().shuffled()
    }
    var wrong by remember(letter) { mutableStateOf(setOf<String>()) }
    var right by remember(letter) { mutableStateOf(false) }
    var shakeCount by remember { mutableIntStateOf(0) }
    var lastWrong by remember(letter) { mutableStateOf<String?>(null) }
    val shake = rememberShake(shakeCount)
    LaunchedEffect(letter.word) { delay(400); speak(letter.word) }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Color.White).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(letter.picture, fontSize = 64.sp)
        HearButton("Listen again") { speak(letter.word) }
        Text("Which one says the word you heard?", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = OttaInk, textAlign = TextAlign.Center)
    }
    options.forEach { w ->
        val isRight = right && w == letter.word
        val isWrong = w in wrong
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = if (w == lastWrong) shake * density else 0f }
                .clip(RoundedCornerShape(22.dp))
                .background(if (isRight) OttaTint else if (isWrong) Color(0xFFFFECEC) else Color.White)
                .border(3.dp, if (isRight) OttaColor else if (isWrong) Color(0xFFFF6B6B) else Color(0xFFE6ECF1), RoundedCornerShape(22.dp))
                .clickable(enabled = !right && !isWrong) {
                    if (w == letter.word) {
                        right = true
                        SoundEffectsHelper.playSuccess(context)
                        ActivityTracker.recordAnswer(letter.glyph, true)
                        scope.launch { delay(1400); onDone() }
                    } else {
                        wrong = wrong + w
                        lastWrong = w
                        shakeCount += 1
                        SoundEffectsHelper.playOops(context)
                        ActivityTracker.recordAnswer(letter.glyph, false)
                        scope.launch { delay(500); speak(letter.word) }
                    }
                }
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .testTag("otta_option"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(w, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = OttaInk, modifier = Modifier.weight(1f))
            if (isRight) Text("✓", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = OttaColor)
            if (isWrong) Text("✕", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF6B6B))
        }
    }
    if (right) {
        Text(
            "Yes! ${letter.word} has ${letter.glyph}. Listen for it when you read!",
            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F6B62),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(OttaTint).padding(12.dp)
        )
    }
}
