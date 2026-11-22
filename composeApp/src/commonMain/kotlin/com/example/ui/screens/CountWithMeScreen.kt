package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ThemeWords
import com.example.resources.Res
import com.example.resources.tap_mango
import com.example.resources.tap_apple
import com.example.resources.tap_banana
import com.example.resources.tap_orange
import com.example.resources.tap_strawberry
import com.example.resources.tap_watermelon
import com.example.resources.tap_potato
import com.example.resources.tap_brinjal
import com.example.resources.pic_aane
import com.example.resources.pic_karadi
import com.example.resources.pic_onte
import com.example.resources.pic_navilu
import com.example.resources.pic_hamsa
import com.example.resources.pic_ili
import com.example.resources.pic_tagaru
import com.example.resources.pic_dana
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.data.Access
import com.example.ui.data.ScoreManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Game id of Count With Me (see GameRegistry). */
const val COUNT_GAME_ID = 27

/** ಒಂದು … ಹತ್ತು (voice clips for these are already in the app). */
private val numberNames = listOf("ಒಂದು", "ಎರಡು", "ಮೂರು", "ನಾಲ್ಕು", "ಐದು", "ಆರು", "ಏಳು", "ಎಂಟು", "ಒಂಬತ್ತು", "ಹತ್ತು")
private val numberEnglish = listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")

/** What to count: a picture (from the app's videos) and its English name, one and many (for the answer card). */
private data class CountThing(val image: DrawableResource, val one: String, val many: String)

private val countThings = listOf(
    CountThing(Res.drawable.tap_mango, "mango", "mangoes"), CountThing(Res.drawable.tap_apple, "apple", "apples"),
    CountThing(Res.drawable.tap_banana, "banana bunch", "banana bunches"), CountThing(Res.drawable.tap_orange, "orange", "oranges"),
    CountThing(Res.drawable.tap_strawberry, "strawberry", "strawberries"), CountThing(Res.drawable.tap_watermelon, "watermelon", "watermelons"),
    CountThing(Res.drawable.tap_potato, "potato", "potatoes"), CountThing(Res.drawable.tap_brinjal, "brinjal", "brinjals"),
    CountThing(Res.drawable.pic_aane, "elephant", "elephants"), CountThing(Res.drawable.pic_karadi, "bear", "bears"),
    CountThing(Res.drawable.pic_onte, "camel", "camels"), CountThing(Res.drawable.pic_navilu, "peacock", "peacocks"),
    CountThing(Res.drawable.pic_hamsa, "swan", "swans"), CountThing(Res.drawable.pic_ili, "mouse", "mice"),
    CountThing(Res.drawable.pic_tagaru, "ram", "rams"), CountThing(Res.drawable.pic_dana, "cow", "cows"),
)

/** Easy: 1–5 with count-along. Medium: 1–10 with count-along. Hard: 1–10, count in your head. */
private data class CountLevel(val title: String, val max: Int, val countAlong: Boolean)
private val countLevels = listOf(CountLevel("Easy", 5, true), CountLevel("Medium", 10, true), CountLevel("Hard", 10, false))

private const val ROUNDS = 10
private val CInk = Color(0xFF633806)
private val CSoft = Color(0xFF854F0B)
private val CBg = Color(0xFFFAEEDA)
private val CGold = Color(0xFFFAC775)
private val CGood = Color(0xFF3B6D11)
private val CGoodBg = Color(0xFFEAF3DE)

private enum class CountPhase { Counting, Answer, Right, Finished }

/**
 * Count With Me: tap each picture and the app counts aloud in Kannada (ಒಂದು, ಎರಡು, ಮೂರು…), then pick
 * how many from three number names. A wrong answer counts the pictures again, one by one, so the
 * child sees why. 10 rounds a game; Easy is free, Medium and Hard are in Gold.
 */
@Composable
fun CountWithMeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { getSharedPreferences("GamePrefs") }
    val user = remember { getSharedPreferences("AppProgress").getString("last_active_user", "guest") ?: "guest" }
    val sub by com.example.ui.data.SubscriptionManager.state.collectAsState()
    val premium = remember(sub) { Access.isPremium() }

    var levelIndex by remember { mutableIntStateOf(0) }
    var game by remember { mutableIntStateOf(0) }
    val level = countLevels[levelIndex]
    var round by remember(levelIndex, game) { mutableIntStateOf(0) }
    var mistakes by remember(levelIndex, game) { mutableIntStateOf(0) }
    var stars by remember(levelIndex, game) { mutableIntStateOf(0) }
    var bestVersion by remember { mutableIntStateOf(0) }
    fun bestKey(i: Int) = "${user}_count_L${i + 1}_best"

    // The current round
    val n = remember(levelIndex, game, round) { (1..level.max).random() }
    val thing = remember(levelIndex, game, round) { countThings.random() }
    val options = remember(levelIndex, game, round) {
        ((1..level.max).filter { it != n }.shuffled().take(2) + n).shuffled()
    }
    var counted by remember(levelIndex, game, round) { mutableIntStateOf(0) }
    var phase by remember(levelIndex, game, round) { mutableStateOf(if (level.countAlong) CountPhase.Counting else CountPhase.Answer) }
    var glow by remember(levelIndex, game, round) { mutableIntStateOf(-1) }   // picture lit up while counting again
    var wrong by remember(levelIndex, game, round) { mutableStateOf(setOf<Int>()) }
    var busy by remember(levelIndex, game, round) { mutableStateOf(false) }

    fun say(text: String) = NativeAudioPlayer.playPhonics(context, text)

    fun tapThing(i: Int) {
        if (phase != CountPhase.Counting || i != counted) return
        SoundEffectsHelper.playPop()
        counted += 1
        say(numberNames[counted - 1])
        if (counted == n) scope.launch { delay(700); phase = CountPhase.Answer }
    }

    fun answer(k: Int) {
        if (phase != CountPhase.Answer || busy || k in wrong) return
        say(numberNames[k - 1])
        if (k == n) {
            SoundEffectsHelper.playSuccess()
            phase = CountPhase.Right
            scope.launch { delay(700); say(ThemeWords.WELL_DONE) }
        } else {
            SoundEffectsHelper.playOops()
            mistakes++
            wrong = wrong + k
            busy = true
            // Count again, one picture at a time, so the child sees the answer
            scope.launch {
                delay(700)
                for (j in 0 until n) {
                    glow = j
                    say(numberNames[j])
                    delay(750)
                }
                glow = -1
                busy = false
            }
        }
    }

    fun nextRound() {
        SoundEffectsHelper.playButtonTap()
        if (round + 1 >= ROUNDS) {
            stars = when {
                mistakes == 0 -> 3
                mistakes <= 3 -> 2
                else -> 1
            }
            SoundEffectsHelper.playApplause()
            if (stars > prefs.getInt(bestKey(levelIndex), 0)) { prefs.edit().putInt(bestKey(levelIndex), stars).apply(); bestVersion++ }
            try { ScoreManager.addCoins(context, "count_with_me", ROUNDS + stars * 2) } catch (e: Exception) {}
        } else {
            round += 1
        }
    }

    Column(Modifier.fillMaxSize().background(CBg).safeDrawingPadding().padding(horizontal = 14.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CInk) }
            Column(Modifier.weight(1f)) {
                Text("Count with me", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CInk)
                Text("ಎಣಿಸೋಣ ಬಾ!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CSoft)
            }
            Text("⭐ ${round + if (phase == CountPhase.Right || stars > 0) 1 else 0}/$ROUNDS", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CInk)
        }

        // Levels (Medium and Hard need Gold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            countLevels.forEachIndexed { i, l ->
                val locked = i > 0 && !premium
                val best = remember(bestVersion, i) { prefs.getInt(bestKey(i), 0) }
                val on = i == levelIndex
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (on) CInk else Color.White)
                        .clickable {
                            SoundEffectsHelper.playButtonTap()
                            if (locked) Access.askGrownUp("Medium and Hard Count with me are in the Gold pack.")
                            else { levelIndex = i; game++ }
                        }
                        .padding(vertical = 6.dp)
                        .testTag("count_level_${i + 1}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text((if (locked) "👑 " else "") + l.title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (on) Color.White else CInk)
                    Text("★".repeat(best) + "☆".repeat(3 - best), fontSize = 12.sp, color = if (on) CGold else Color(0xFFEF9F27))
                }
            }
        }

        Text(
            when (phase) {
                CountPhase.Counting -> "Tap each ${thing.one} to count"
                CountPhase.Answer -> if (busy) "Let's count again…" else "How many ${thing.many}?"
                else -> ""
            },
            fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CInk, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
        )

        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(bottom = 12.dp)) {
            val boxWidth = maxWidth
            if (stars > 0) {
                // Game finished
                Column(
                    Modifier.align(Alignment.Center).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🎉", fontSize = 44.sp)
                    Text("You counted $ROUNDS times!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CInk, textAlign = TextAlign.Center)
                    Text("★".repeat(stars) + "☆".repeat(3 - stars), fontSize = 30.sp, color = Color(0xFFEF9F27))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CountPill("🔁 Again", Color(0xFFEF9F27)) { SoundEffectsHelper.playButtonTap(); game++ }
                        if (levelIndex < countLevels.size - 1) {
                            CountPill("Next ▶", CInk) {
                                SoundEffectsHelper.playButtonTap()
                                if (!premium) Access.askGrownUp("Medium and Hard Count with me are in the Gold pack.")
                                else { levelIndex++; game++ }
                            }
                        }
                    }
                }
            } else if (phase == CountPhase.Right) {
                Column(
                    Modifier.align(Alignment.Center).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("ಶಭಾಷ್!", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = CInk)
                    // The pictures again, small, 5 a row
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        (0 until n).chunked(5).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                row.forEach { _ ->
                                    Image(
                                        painterResource(thing.image), contentDescription = null, contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(if (n <= 5) 40.dp else 30.dp).clip(CircleShape)
                                    )
                                }
                            }
                        }
                    }
                    Text(numberNames[n - 1], fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = CGood)
                    Text("${numberEnglish[n - 1]} ${if (n == 1) thing.one else thing.many}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888780))
                    CountPill("Next ▶", Color(0xFFEF9F27)) { nextRound() }
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // The pictures, 5 a row
                    val perRow = 5
                    val rows = (n + perRow - 1) / perRow
                    val cell = minOf((boxWidth - 20.dp - 8.dp * (perRow - 1)) / perRow, 76.dp)
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (r in 0 until rows) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (c in 0 until minOf(perRow, n - r * perRow)) {
                                    val i = r * perRow + c
                                    val isCounted = i < counted
                                    val lit = i == glow
                                    val pop by animateFloatAsState(if (lit || (isCounted && i == counted - 1)) 1.2f else 1f, spring(dampingRatio = 0.4f), label = "pop")
                                    Column(
                                        Modifier.width(cell)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (lit) CGold.copy(alpha = 0.5f) else Color.Transparent)
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { tapThing(i) }
                                            .testTag("count_thing_$i"),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val faded = level.countAlong && phase == CountPhase.Counting && !isCounted
                                        Image(
                                            painterResource(thing.image), contentDescription = thing.one,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(cell * 0.86f).scale(pop).alpha(if (faded) 0.35f else 1f).clip(CircleShape)
                                        )
                                        val label = when {
                                            lit -> numberNames[i]
                                            isCounted && level.countAlong -> numberNames[i]
                                            else -> ""
                                        }
                                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = CGood, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    if (phase == CountPhase.Answer) {
                        options.forEach { k ->
                            val isWrong = k in wrong
                            Box(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(if (isWrong) Color(0xFFF1EFE8) else Color.White)
                                    .border(2.dp, if (isWrong) Color(0xFFD3D1C7) else CGold, RoundedCornerShape(16.dp))
                                    .clickable(enabled = !isWrong) { answer(k) }
                                    .padding(vertical = 12.dp)
                                    .testTag("count_answer_$k"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    numberNames[k - 1], fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                                    color = if (isWrong) Color(0xFFB4B2A9) else CInk
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountPill(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(22.dp)).background(color).clickable { onClick() }.padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
}
