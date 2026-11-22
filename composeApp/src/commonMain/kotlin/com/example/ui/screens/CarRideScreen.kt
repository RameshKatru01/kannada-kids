package com.example.ui.screens

import com.example.platform.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LetterData
import com.example.data.ThemeWords
import com.example.ui.audio.SpeechBank
import com.example.ui.data.Access
import kotlinx.coroutines.delay

/** One thing the car-ride player says, and how long to wait after it (time to hear it and repeat it). */
private data class RideStep(val chapter: Int, val say: String, val waitMs: Long, val picture: String = "", val english: String = "", val isPause: Boolean = false)

private data class RideChapter(val title: String, val kannada: String, val emoji: String, val free: Boolean)

private val rideChapters = listOf(
    RideChapter("Fruits", "ಹಣ್ಣುಗಳು", "🍎", free = true),
    RideChapter("Vegetables", "ತರಕಾರಿಗಳು", "🥕", free = false),
    RideChapter("My body", "ದೇಹದ ಭಾಗಗಳು", "🙂", free = false),
    RideChapter("My family", "ನನ್ನ ಕುಟುಂಬ", "👨‍👩‍👧", free = false),
    RideChapter("Counting", "ಎಣಿಕೆ", "🔢", free = false),
    RideChapter("Vowels", "ಸ್ವರಗಳು", "ಅ", free = false),
)

private val rideNumbers = listOf("ಒಂದು", "ಎರಡು", "ಮೂರು", "ನಾಲ್ಕು", "ಐದು", "ಆರು", "ಏಳು", "ಎಂಟು", "ಒಂಬತ್ತು", "ಹತ್ತು")
private val rideNumbersEn = listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")

/**
 * The whole listening mix (about 8 minutes): for each word, the word, "ನೀನೂ ಹೇಳು!" (you say it too),
 * a pause for the child to repeat it, the word again and a short sentence. Then counting 1 to 10
 * and the vowels. Uses the voice clips already in the app.
 */
private fun ridePlaylist(): List<RideStep> = buildList {
    val themes = listOf(ThemeWords.fruits, ThemeWords.vegetables, ThemeWords.bodyParts, ThemeWords.family)
    themes.forEachIndexed { ch, theme ->
        add(RideStep(ch, theme.kannada, 2200, rideChapters[ch].emoji, rideChapters[ch].title))
        theme.words.forEach { w ->
            add(RideStep(ch, w.kannada, 1700, w.picture, w.english))
            add(RideStep(ch, ThemeWords.SAY_IT, 1500, w.picture, w.english))
            add(RideStep(ch, "", 2300, w.picture, w.english, isPause = true))
            add(RideStep(ch, w.kannada, 1600, w.picture, w.english))
            add(RideStep(ch, w.sentence, 2700, w.picture, w.english))
        }
    }
    val count = 4
    add(RideStep(count, "", 1200, "🔢", "Counting"))
    repeat(2) {
        rideNumbers.forEachIndexed { i, n -> add(RideStep(count, n, 1300, "${i + 1}", rideNumbersEn[i])) }
        add(RideStep(count, ThemeWords.SAY_IT, 1600, "🔢", "Your turn: count with me!"))
    }
    val vowels = 5
    add(RideStep(vowels, "ಸ್ವರ", 1800, "ಅ", "Vowels"))
    repeat(2) {
        LetterData.kannadaVowels.forEach { v -> add(RideStep(vowels, v.character, 1400, v.character, v.transliteration)) }
    }
    add(RideStep(vowels, ThemeWords.WELL_DONE, 2000, "🎉", "Well done!"))
}

private val RideBg = Color(0xFF14213D)
private val RideCard = Color(0xFF1F2E52)
private val RideText = Color(0xFFF1EFE8)
private val RideSoft = Color(0xFFB4B2A9)
private val RideGold = Color(0xFFFAC775)

/**
 * Car-ride listening: an audio mix of Kannada words, counting and vowels to listen to (and repeat)
 * in the car, with no need to look at the screen. Dark, calm screen; it stays on while playing.
 * Fruits is free; the other chapters are in Gold.
 */
@Composable
fun CarRideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sub by com.example.ui.data.SubscriptionManager.state.collectAsState()
    val premium = remember(sub) { Access.isPremium() }
    val steps = remember { ridePlaylist() }
    var index by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    fun allowed(chapter: Int) = premium || rideChapters[chapter].free
    fun firstStep(chapter: Int) = steps.indexOfFirst { it.chapter == chapter }.coerceAtLeast(0)
    val step = steps[index.coerceIn(0, steps.size - 1)]

    if (playing) KeepScreenOn()

    LaunchedEffect(playing) {
        while (playing && index < steps.size) {
            val s = steps[index]
            if (!allowed(s.chapter)) {
                // Free plan: stop after the free chapter
                playing = false
                finished = true
                break
            }
            if (!s.isPause && s.say.isNotBlank()) SpeechBank.play(context, s.say)
            delay(s.waitMs)
            if (!playing) break
            if (index + 1 >= steps.size) { playing = false; finished = true; index = 0; break }
            index += 1
        }
    }

    Column(
        Modifier.fillMaxSize().background(RideBg).safeDrawingPadding().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(RideCard).clickable { playing = false; onBack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RideText) }
            Column(Modifier.weight(1f)) {
                Text("🚗 Car-ride listening", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = RideText)
                Text("Listen and say it back · about 8 minutes", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RideSoft)
            }
        }

        // Chapters
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rideChapters.forEachIndexed { i, c ->
                val on = step.chapter == i
                val locked = !allowed(i)
                Text(
                    (if (locked) "👑 " else "${c.emoji} ") + c.title,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                        .background(if (on) RideGold else RideCard)
                        .clickable {
                            if (locked) { playing = false; Access.askGrownUp("All the car-ride chapters are in the Gold pack.") }
                            else { index = firstStep(i); finished = false }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("ride_chapter_$i"),
                    fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (on) RideBg else RideText
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // What is being said now (big, for a quick glance)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(RideCard).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (finished && !playing) {
                Text("🎉", fontSize = 56.sp)
                Text(
                    if (premium) "All done! Great listening!" else "That was the free Fruits chapter! Ask a grown-up about Gold for all 6 chapters.",
                    fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = RideText, textAlign = TextAlign.Center
                )
            } else {
                Text(step.picture.ifBlank { rideChapters[step.chapter].emoji }, fontSize = 64.sp, color = RideText)
                Text(
                    when {
                        step.isPause -> "Your turn! Say it 🗣️"
                        step.say == ThemeWords.SAY_IT -> "Say it with me!"
                        else -> step.say.ifBlank { rideChapters[step.chapter].kannada }
                    },
                    fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = if (step.isPause) RideGold else RideText, textAlign = TextAlign.Center
                )
                if (step.english.isNotBlank()) Text(step.english, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RideSoft)
            }
        }

        Spacer(Modifier.weight(1f))

        // Controls: previous chapter, play/pause, next chapter
        Row(
            Modifier.padding(bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            RideButton(56, { Icon(Icons.Default.SkipPrevious, contentDescription = "Previous chapter", tint = RideText, modifier = Modifier.size(30.dp)) }) {
                val ch = (step.chapter - 1).coerceAtLeast(0)
                index = firstStep(ch); finished = false
            }
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(RideGold)
                    .border(4.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .clickable {
                        if (!playing && !allowed(step.chapter)) {
                            Access.askGrownUp("All the car-ride chapters are in the Gold pack.")
                        } else {
                            if (finished) { finished = false; index = 0 }
                            playing = !playing
                        }
                    }
                    .testTag("ride_play"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = RideBg, modifier = Modifier.size(52.dp)
                )
            }
            RideButton(56, { Icon(Icons.Default.SkipNext, contentDescription = "Next chapter", tint = RideText, modifier = Modifier.size(30.dp)) }) {
                val ch = (step.chapter + 1).coerceAtMost(rideChapters.size - 1)
                if (allowed(ch)) { index = firstStep(ch); finished = false }
                else { playing = false; Access.askGrownUp("All the car-ride chapters are in the Gold pack.") }
            }
        }
        Text(
            "The screen stays on while playing. Turn the phone's volume up!",
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RideSoft, textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun RideButton(size: Int, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(RideCard).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { icon() }
}
