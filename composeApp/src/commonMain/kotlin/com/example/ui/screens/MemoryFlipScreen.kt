package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.data.KannadaLetter
import com.example.data.LetterData
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.data.Access
import com.example.ui.data.ScoreManager
import com.example.ui.reference.ReferenceInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Game id of Memory Flip (see GameRegistry). */
const val MEMORY_FLIP_GAME_ID = 25

private val MemInk = Color(0xFF26215C)
private val MemSoft = Color(0xFF534AB7)
private val MemBack = Color(0xFF7F77DD)
private val MemBg = Color(0xFFEEEDFE)
private val MemGood = Color(0xFF3B6D11)
private val MemGoodBg = Color(0xFFEAF3DE)

/** One card: a letter, or the picture of a word that starts with it. Cards with the same [pair] match. */
private data class MemCard(val id: Int, val pair: String, val isLetter: Boolean, val face: String, val caption: String, val speak: String, val image: org.jetbrains.compose.resources.DrawableResource? = null)

/** Easy: 3 pairs of vowels. Medium: 4 pairs of vowels and consonants. Hard: 6 pairs of consonants. */
private data class MemLevel(val title: String, val pairs: Int, val pool: () -> List<KannadaLetter>)

private val memLevels = listOf(
    MemLevel("Easy", 3) { LetterData.vowels },
    MemLevel("Medium", 4) { LetterData.vowels + LetterData.consonants },
    MemLevel("Hard", 6) { LetterData.consonants },
)

private fun dealCards(level: MemLevel): List<MemCard> {
    // Pictures from the letter videos (Kannada); emoji words otherwise
    val withPictures = level.pool().mapNotNull { l -> gamePicture(l.character)?.let { l to it } }
    val picked = if (withPictures.size >= level.pairs) {
        withPictures.shuffled().take(level.pairs).map { (l, p) -> Triple(l, p.word, p.image) }
    } else {
        level.pool().mapNotNull { l -> ReferenceInfo.word(l)?.let { l to it } }.distinctBy { it.second.picture }
            .shuffled().take(level.pairs).map { (l, w) -> Triple(l, w.kannada, null) }
    }
    var id = 0
    return picked.flatMap { (letter, word, image) ->
        val emoji = if (image == null) ReferenceInfo.word(letter)?.picture ?: "" else ""
        listOf(
            MemCard(id++, letter.character, true, letter.character, "", letter.character),
            MemCard(id++, letter.character, false, emoji, word, word, image)
        )
    }.shuffled()
}

/**
 * Memory Flip: turn over two cards at a time to find a letter and the picture of a word that starts
 * with it (ಸ and 🍎 ಸೇಬು). Every card says its letter or word when it turns over. Easy is free;
 * Medium and Hard are in Gold. Best stars per level are kept on the phone.
 */
@Composable
fun MemoryFlipScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { getSharedPreferences("GamePrefs") }
    val user = remember { getSharedPreferences("AppProgress").getString("last_active_user", "guest") ?: "guest" }
    val sub by com.example.ui.data.SubscriptionManager.state.collectAsState()
    val premium = remember(sub) { Access.isPremium() }

    var levelIndex by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(0) }
    val level = memLevels[levelIndex]
    val cards = remember(levelIndex, round) { dealCards(level) }
    var faceUp by remember(levelIndex, round) { mutableStateOf(setOf<Int>()) }
    var matched by remember(levelIndex, round) { mutableStateOf(setOf<Int>()) }
    var first by remember(levelIndex, round) { mutableStateOf<Int?>(null) }
    var busy by remember(levelIndex, round) { mutableStateOf(false) }
    var mistakes by remember(levelIndex, round) { mutableIntStateOf(0) }
    var stars by remember(levelIndex, round) { mutableIntStateOf(0) }
    var hint by remember(levelIndex, round) { mutableStateOf("Turn over two cards to find a pair") }
    fun bestKey(i: Int) = "${user}_memory_flip_L${i + 1}_best"
    var bestVersion by remember { mutableIntStateOf(0) }

    fun say(text: String) = NativeAudioPlayer.playPhonics(context, text)

    fun onTap(i: Int) {
        val card = cards[i]
        if (busy || card.id in matched || card.id in faceUp || stars > 0) return
        SoundEffectsHelper.playPop()
        faceUp = faceUp + card.id
        say(card.speak)
        val f = first
        if (f == null) {
            first = i
            hint = if (card.isLetter) "Now find a picture that starts with ${card.face}" else "Now find the letter ${card.pair}"
            return
        }
        first = null
        val other = cards[f]
        if (other.pair == card.pair) {
            matched = matched + other.id + card.id
            busy = true
            scope.launch {
                delay(500)
                SoundEffectsHelper.playSuccess()
                hint = "${card.pair} · ${if (card.isLetter) other.caption else card.caption} 🎉"
                busy = false
                if (matched.size == cards.size) {
                    delay(600)
                    stars = when {
                        mistakes == 0 -> 3
                        mistakes <= level.pairs -> 2
                        else -> 1
                    }
                    SoundEffectsHelper.playApplause()
                    val best = prefs.getInt(bestKey(levelIndex), 0)
                    if (stars > best) { prefs.edit().putInt(bestKey(levelIndex), stars).apply(); bestVersion++ }
                    try { ScoreManager.addCoins(context, "memory_flip", level.pairs * 2 + stars) } catch (e: Exception) {}
                }
            }
        } else {
            mistakes++
            busy = true
            hint = "Not a pair. Try again!"
            scope.launch {
                delay(1100)
                faceUp = faceUp - other.id - card.id
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize().background(MemBg).safeDrawingPadding().padding(horizontal = 14.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MemInk) }
            Column(Modifier.weight(1f)) {
                Text("Memory Flip", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MemInk)
                Text("Find the letter and its picture", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MemSoft)
            }
            Text("⭐ ${matched.size / 2}/${level.pairs}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MemInk)
        }

        // Level chips (Medium and Hard need Gold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            memLevels.forEachIndexed { i, l ->
                val locked = i > 0 && !premium
                val best = remember(bestVersion, i) { prefs.getInt(bestKey(i), 0) }
                val on = i == levelIndex
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (on) MemInk else Color.White)
                        .clickable {
                            SoundEffectsHelper.playButtonTap()
                            if (locked) Access.askGrownUp("Medium and Hard Memory Flip are in the Gold pack.")
                            else { levelIndex = i; round++ }
                        }
                        .padding(vertical = 6.dp)
                        .testTag("memory_level_${i + 1}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text((if (locked) "👑 " else "") + l.title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (on) Color.White else MemInk)
                    Text("★".repeat(best) + "☆".repeat(3 - best), fontSize = 12.sp, color = if (on) Color(0xFFFFC93C) else Color(0xFFEF9F27))
                }
            }
        }

        Text(
            hint, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MemSoft, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
        )

        // The cards: 3 a row for Easy, 4 a row otherwise; sized to fit the screen
        val perRow = if (cards.size <= 6) 3 else 4
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(bottom = 12.dp)) {
            val rows = (cards.size + perRow - 1) / perRow
            val gap = 10.dp
            val cardW = minOf((maxWidth - gap * (perRow - 1)) / perRow, ((maxHeight - gap * (rows - 1)) / rows) * 0.78f)
            Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(gap)) {
                cards.indices.chunked(perRow).forEach { rowIdx ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        rowIdx.forEach { i ->
                            val c = cards[i]
                            FlipCard(
                                card = c,
                                up = c.id in faceUp || c.id in matched,
                                matched = c.id in matched,
                                modifier = Modifier.width(cardW).height(cardW / 0.78f).testTag("memory_card_$i")
                            ) { onTap(i) }
                        }
                    }
                }
            }

            if (stars > 0) {
                Column(
                    Modifier.align(Alignment.Center).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🎉", fontSize = 44.sp)
                    Text("All pairs found!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MemInk)
                    Text("★".repeat(stars) + "☆".repeat(3 - stars), fontSize = 30.sp, color = Color(0xFFEF9F27))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MemPill("🔁 Again", MemBack) { SoundEffectsHelper.playButtonTap(); round++ }
                        if (levelIndex < memLevels.size - 1) {
                            MemPill("Next ▶", MemInk) {
                                SoundEffectsHelper.playButtonTap()
                                if (!premium) Access.askGrownUp("Medium and Hard Memory Flip are in the Gold pack.")
                                else { levelIndex++; round++ }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipCard(card: MemCard, up: Boolean, matched: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val turn by animateFloatAsState(if (up) 180f else 0f, tween(260), label = "flip")
    val showFace = turn > 90f
    Box(
        modifier
            .graphicsLayer { rotationY = turn; cameraDistance = 12f * density }
            .clip(RoundedCornerShape(14.dp))
            .background(if (!showFace) MemBack else if (matched) MemGoodBg else Color.White)
            .border(2.dp, if (showFace && matched) MemGood else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (!showFace) {
            Text("?", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MemBg)
        } else {
            // Mirror the face back so it reads the right way round
            Column(Modifier.graphicsLayer { rotationY = 180f }, horizontalAlignment = Alignment.CenterHorizontally) {
                if (card.isLetter) {
                    Text(card.face, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = if (matched) MemGood else MemInk)
                } else {
                    if (card.image != null) {
                        androidx.compose.foundation.Image(
                            org.jetbrains.compose.resources.painterResource(card.image), contentDescription = card.caption,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth(0.86f).aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Text(card.face, fontSize = 30.sp)
                    }
                    Text(card.caption, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (matched) MemGood else MemSoft, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun MemPill(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(22.dp)).background(color).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
}
