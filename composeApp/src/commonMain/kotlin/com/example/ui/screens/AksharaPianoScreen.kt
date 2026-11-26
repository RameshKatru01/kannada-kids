package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.data.ScoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/** Game id of Akshara Piano (see GameRegistry). */
const val AKSHARA_PIANO_GAME_ID = 29

/** The 15 vowels as two rows of keys: the first row is one octave (C to C), the second the octave above. */
private val pianoLetters = listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ", "ಊ", "ಋ", "ಎ", "ಏ", "ಐ", "ಒ", "ಓ", "ಔ", "ಅಂ", "ಅಃ")
/** Semitones above middle C for each key (a major scale over two octaves). */
private val pianoSemitones = listOf(0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24)
private val keyColors = listOf(
    Color(0xFFFF6B6B), Color(0xFFFF9F43), Color(0xFFFFD43B), Color(0xFF8BD46E), Color(0xFF38D9A9),
    Color(0xFF4DABF7), Color(0xFF748FFC), Color(0xFFDA77F2), Color(0xFFFF6B6B), Color(0xFFFF9F43),
    Color(0xFFFFD43B), Color(0xFF8BD46E), Color(0xFF38D9A9), Color(0xFF4DABF7), Color(0xFF748FFC)
)

/** Play-along tunes, as key numbers (0 = ಅ = C). */
private data class PianoSong(val title: String, val notes: List<Int>)
private val pianoSongs = listOf(
    PianoSong("Twinkle Twinkle", listOf(0, 0, 4, 4, 5, 5, 4, 3, 3, 2, 2, 1, 1, 0)),
    PianoSong("Happy Song", listOf(2, 2, 3, 4, 4, 3, 2, 1, 0, 0, 1, 2, 2, 1, 1)),
    PianoSong("Up the Ladder", listOf(0, 1, 2, 3, 4, 5, 6, 7, 7, 6, 5, 4, 3, 2, 1, 0)),
)

private const val PIANO_RATE = 22050

/** A soft piano-like note: a few harmonics with a quick attack and a gentle fade. */
private fun pianoTone(semitone: Int): ShortArray {
    val freq = 261.63 * 2.0.pow(semitone / 12.0)
    val n = (0.75 * PIANO_RATE).toInt()
    return ShortArray(n) { i ->
        val t = i.toDouble() / PIANO_RATE
        val env = (if (t < 0.01) t / 0.01 else 1.0) * exp(-t * 4.2)
        val v = sin(2 * PI * freq * t) + 0.45 * sin(4 * PI * freq * t) + 0.18 * sin(6 * PI * freq * t)
        (v / 1.63 * env * 0.55 * Short.MAX_VALUE).toInt().toShort()
    }
}

private val PianoBg = Color(0xFF2B2D5C)
private val PianoInk = Color(0xFF1E1F3D)

/**
 * Akshara Piano: every key is a Kannada vowel. Tap a key to hear a note and the letter (the letter
 * voice can be switched off for pure music). "Play along" lights up the next key of a simple tune;
 * finishing a tune brings applause. Free for everyone.
 */
@Composable
fun AksharaPianoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tones by remember { mutableStateOf<List<ShortArray>>(emptyList()) }
    LaunchedEffect(Unit) { tones = withContext(Dispatchers.Default) { pianoSemitones.map { pianoTone(it) } } }

    var sayLetters by remember { mutableStateOf(true) }
    var song by remember { mutableStateOf<PianoSong?>(null) }
    var step by remember { mutableIntStateOf(0) }
    var songDone by remember { mutableStateOf(false) }
    var lastLetter by remember { mutableStateOf("") }
    val bounce = remember { pianoLetters.map { Animatable(1f) } }

    fun play(i: Int) {
        tones.getOrNull(i)?.let { PlatformAudio.playPcm16(it, PIANO_RATE) }
        lastLetter = pianoLetters[i]
        scope.launch {
            bounce[i].snapTo(0.9f)
            bounce[i].animateTo(1f, spring(dampingRatio = 0.35f))
        }
        if (sayLetters) scope.launch { delay(160); NativeAudioPlayer.playPhonics(context, pianoLetters[i]) }
        val s = song ?: return
        if (songDone) return
        if (i == s.notes[step]) {
            if (step + 1 >= s.notes.size) {
                songDone = true
                scope.launch {
                    delay(700)
                    SoundEffectsHelper.playApplause()
                    try { ScoreManager.addCoins(context, "akshara_piano", 10) } catch (e: Exception) {}
                }
            } else step++
        }
    }

    Column(Modifier.fillMaxSize().background(PianoBg).safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PianoInk) }
            Column(Modifier.weight(1f)) {
                Text("Akshara Piano 🎹", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("Every key sings a Kannada vowel", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC5C7F5))
            }
            // Letter voice on / off
            Box(
                Modifier.clip(RoundedCornerShape(16.dp)).background(if (sayLetters) Color(0xFFFFD43B) else Color(0xFF45478A))
                    .clickable { SoundEffectsHelper.playButtonTap(); sayLetters = !sayLetters }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("piano_say_letters"),
                contentAlignment = Alignment.Center
            ) {
                Text(if (sayLetters) "🗣️ ಅ on" else "🎵 Music", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (sayLetters) PianoInk else Color.White)
            }
        }

        // Mode chips: free play or a tune to play along
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PianoChip("🎹 Free play", song == null, Modifier.weight(1f)) { song = null; songDone = false }
            pianoSongs.forEach { s ->
                PianoChip("⭐ " + s.title, song == s, Modifier.weight(1f)) { song = s; step = 0; songDone = false }
            }
        }

        // What's happening: the letter just played, or the tune's progress
        Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
            val s = song
            when {
                s != null && songDone -> Text("🎉 You played ${s.title}!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD43B))
                s != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tap the glowing key", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC5C7F5))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        s.notes.forEachIndexed { k, n ->
                            Text(
                                pianoLetters[n], fontSize = if (k == step) 24.sp else 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when { k < step -> Color(0xFF8BD46E); k == step -> Color(0xFFFFD43B); else -> Color(0xFF8E90D9) }
                            )
                        }
                    }
                }
                lastLetter.isNotEmpty() -> Text(lastLetter, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                else -> Text("Tap any key! 🎶", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }

        // The keyboard: two rows of tall rainbow keys
        val glowing = song?.let { if (songDone) null else it.notes[step] }
        Column(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(0 until 8, 8 until 15).forEach { range ->
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (i in range) {
                        PianoKey(
                            letter = pianoLetters[i],
                            color = keyColors[i],
                            glow = glowing == i,
                            scale = bounce[i].value,
                            modifier = Modifier.weight(1f).fillMaxHeight().testTag("piano_key_$i"),
                            onPress = { play(i) }
                        )
                    }
                    if (range.count() < 8) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PianoKey(letter: String, color: Color, glow: Boolean, scale: Float, modifier: Modifier, onPress: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "pulse"
    )
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 6.dp, topEnd = 6.dp))
            .background(Color.White)
            .border(
                if (glow) 4.dp else 0.dp,
                if (glow) Color(0xFFFFD43B).copy(alpha = pulse) else Color.Transparent,
                RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 6.dp, topEnd = 6.dp)
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPress() },
        contentAlignment = Alignment.BottomCenter
    ) {
        // Coloured cap at the top, the letter near the bottom (where little fingers tap)
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().fillMaxHeight(0.22f).background(color))
        Text(
            letter, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = PianoInk, textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 14.dp)
        )
        if (glow) Text("👇", fontSize = 18.sp, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun PianoChip(label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(if (on) Color(0xFFFFD43B) else Color(0xFF45478A))
            .clickable { SoundEffectsHelper.playButtonTap(); onClick() }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (on) PianoInk else Color.White, textAlign = TextAlign.Center, maxLines = 2)
    }
}
