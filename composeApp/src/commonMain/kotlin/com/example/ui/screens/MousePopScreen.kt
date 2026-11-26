package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LetterData
import com.example.resources.Res
import com.example.resources.pic_ili
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.data.Access
import com.example.ui.data.ScoreManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/** Game id of Mouse Pop (see GameRegistry). */
const val MOUSE_POP_GAME_ID = 28

private data class PopLevel(val title: String, val letters: () -> List<String>)
private val popLevels = listOf(
    PopLevel("Vowels") { LetterData.vowels.map { it.character } },
    PopLevel("Consonants") { LetterData.consonants.map { it.character }.filter { it !in setOf("ಙ", "ಞ") } },
    PopLevel("Mixed") { (LetterData.vowels + LetterData.consonants).map { it.character }.filter { it !in setOf("ಙ", "ಞ") } },
)

private const val HOLES = 9               // a 3 × 3 field
private const val BONKS_PER_ROUND = 6
private const val ROUNDS = 5

/** One mouse in one hole. [serial] changes every time a new mouse pops up there. */
private data class Mole(val serial: Int, val letter: String, val up: Boolean, val hit: Boolean = false, val wrong: Boolean = false)

private val Grass = Color(0xFF8BD46E)
private val GrassDark = Color(0xFF6FBF57)
private val Sky = Color(0xFFBDE8FF)
private val Dirt = Color(0xFFB07A4F)
private val DirtDark = Color(0xFF7A4E2D)
private val Hole = Color(0xFF3B2414)
private val PopInk = Color(0xFF3B2414)
private val Sign = Color(0xFFFFF1C9)

/**
 * Mouse Pop: little mice pop out of holes in a sunny garden, each holding a letter sign. The app says
 * a letter ("ಕ!") and the child bonks every mouse holding it. Six bonks finish a round; each round
 * brings a new letter and quicker mice. Vowels are free; Consonants and Mixed are in Gold.
 */
@Composable
fun MousePopScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { getSharedPreferences("GamePrefs") }
    val user = remember { getSharedPreferences("AppProgress").getString("last_active_user", "guest") ?: "guest" }
    val sub by com.example.ui.data.SubscriptionManager.state.collectAsState()
    val premium = remember(sub) { Access.isPremium() }

    var levelIndex by remember { mutableIntStateOf(0) }
    var game by remember { mutableIntStateOf(0) }       // bumps to start a new game
    var playing by remember { mutableStateOf(false) }
    var round by remember { mutableIntStateOf(0) }
    var bonks by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var stars by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf("") }
    var roundBanner by remember { mutableStateOf(false) }
    val moles = remember { mutableStateListOf<Mole?>().apply { repeat(HOLES) { add(null) } } }
    var serial by remember { mutableIntStateOf(0) }
    var bestVersion by remember { mutableIntStateOf(0) }
    fun bestKey(i: Int) = "${user}_mouse_pop_L${i + 1}_best"
    var music by remember { mutableStateOf<Any?>(null) }

    fun say(text: String) = NativeAudioPlayer.playPhonics(context, text)

    // Speed for this round: mice stay up a shorter time and come out more often as rounds go on
    fun upTime() = (1900L - round * 220L).coerceAtLeast(900L)
    fun spawnGap() = (900L - round * 110L).coerceAtLeast(380L)

    fun startRound() {
        val pool = popLevels[levelIndex].letters()
        target = pool.filter { it != target }.random()
        bonks = 0
        roundBanner = true
        scope.launch {
            say(target)
            delay(1400)
            roundBanner = false
            say(target)
        }
    }

    fun finishGame() {
        playing = false
        for (i in 0 until HOLES) moles[i] = null
        stars = when {
            misses <= 2 -> 3
            misses <= 6 -> 2
            else -> 1
        }
        SoundEffectsHelper.playApplause()
        if (stars > prefs.getInt(bestKey(levelIndex), 0)) { prefs.edit().putInt(bestKey(levelIndex), stars).apply(); bestVersion++ }
        try { ScoreManager.addCoins(context, "mouse_pop", ROUNDS * 3 + stars * 2) } catch (e: Exception) {}
    }

    fun bonk(i: Int) {
        val m = moles[i] ?: return
        if (!m.up || m.hit || m.wrong || !playing || roundBanner || bonks >= BONKS_PER_ROUND) return
        if (m.letter == target) {
            SoundEffectsHelper.playPop()
            moles[i] = m.copy(hit = true)
            bonks++
            val roundDone = bonks == BONKS_PER_ROUND
            scope.launch {
                delay(420)
                if (moles[i]?.serial == m.serial) moles[i] = moles[i]?.copy(up = false)
                if (roundDone) {
                    SoundEffectsHelper.playSuccess()
                    if (round + 1 >= ROUNDS) finishGame() else { round++; startRound() }
                }
            }
        } else {
            SoundEffectsHelper.playOops()
            misses++
            moles[i] = m.copy(wrong = true)
            say(m.letter)
            scope.launch {
                delay(500)
                if (moles[i]?.serial == m.serial) moles[i] = moles[i]?.copy(up = false)
            }
        }
    }

    // The game loop: pop mice out at random free holes
    LaunchedEffect(playing, game) {
        if (!playing) return@LaunchedEffect
        val pool = popLevels[levelIndex].letters()
        while (isActive && playing) {
            delay(spawnGap())
            if (roundBanner) continue
            val free = (0 until HOLES).filter { moles[it]?.up != true }
            if (free.isEmpty() || (0 until HOLES).count { moles[it]?.up == true } >= 3) continue
            val hole = free.random()
            // About 45% of mice carry the letter we're looking for
            val letter = if ((0..99).random() < 45) target else pool.filter { it != target }.random()
            serial++
            val mine = serial
            moles[hole] = Mole(mine, letter, up = true)
            scope.launch {
                delay(upTime())
                val now = moles[hole]
                if (now?.serial == mine && !now.hit && !now.wrong) moles[hole] = now.copy(up = false)
            }
        }
    }

    // Music: the Balloon Pop tune, a little quicker each round
    LaunchedEffect(playing, round) {
        PlatformAudio.stopLoop(music)
        music = null
        if (playing) {
            val samples = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                com.example.ui.audio.BalloonMusic.track(110 + round * 8)
            }
            music = PlatformAudio.startLoop(samples, com.example.ui.audio.BalloonMusic.SAMPLE_RATE)
        }
    }
    DisposableEffect(Unit) { onDispose { PlatformAudio.stopLoop(music) } }

    fun startGame() {
        SoundEffectsHelper.playButtonTap()
        round = 0; misses = 0; stars = 0; target = ""
        for (i in 0 until HOLES) moles[i] = null
        playing = true
        game++
        startRound()
    }

    Column(Modifier.fillMaxSize().background(Sky).safeDrawingPadding()) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable { playing = false; onBack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PopInk) }
            Column(Modifier.weight(1f)) {
                Text("Mouse Pop", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = PopInk)
                Text(if (playing) "Round ${round + 1} of $ROUNDS" else "Bonk the mice with the right letter!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B4A2E))
            }
            if (playing) {
                Text("🔨 $bonks/$BONKS_PER_ROUND", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PopInk)
            }
        }

        if (playing && target.isNotEmpty()) {
            // What to bonk, big, with a speaker to hear it again
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White)
                    .border(3.dp, Color(0xFFFFC93C), RoundedCornerShape(22.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Bonk", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = PopInk)
                Text(target, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE8572A), modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFFFC93C)).clickable { say(target) },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Say it again", tint = PopInk) }
            }
        }

        // The garden
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(12.dp)) {
            val fieldW = maxWidth
            val fieldH = maxHeight
            Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))) {
                // Sunny garden: grass with stripes, a few flowers and a sun peeking in
                drawRect(Grass)
                val stripe = size.height / 9
                for (k in 0 until 9 step 2) drawRect(GrassDark.copy(alpha = 0.35f), Offset(0f, k * stripe), Size(size.width, stripe))
                drawCircle(Color(0xFFFFE066), size.minDimension * 0.12f, Offset(size.width * 0.92f, size.height * 0.04f))
                listOf(0.06f to 0.12f, 0.5f to 0.06f, 0.94f to 0.55f, 0.04f to 0.62f, 0.55f to 0.97f).forEach { (fx, fy) ->
                    val c = Offset(size.width * fx, size.height * fy)
                    val r = size.minDimension * 0.022f
                    for (p in 0 until 5) {
                        val a = p * 72.0 * kotlin.math.PI / 180.0
                        drawCircle(Color(0xFFFF8FB1), r, Offset(c.x + (kotlin.math.cos(a) * r * 1.3).toFloat(), c.y + (kotlin.math.sin(a) * r * 1.3).toFloat()))
                    }
                    drawCircle(Color(0xFFFFD43B), r * 0.9f, c)
                }
            }
            val cellW = fieldW / 3
            val cellH = fieldH / 3
            Column(Modifier.fillMaxSize()) {
                for (r in 0 until 3) {
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        for (c in 0 until 3) {
                            val i = r * 3 + c
                            MoleHole(
                                mole = moles[i],
                                modifier = Modifier.weight(1f).fillMaxHeight().testTag("mouse_hole_$i"),
                                onBonk = { bonk(i) }
                            )
                        }
                    }
                }
            }

            if (playing && roundBanner) {
                Column(
                    Modifier.align(Alignment.Center).clip(RoundedCornerShape(26.dp)).background(Color.White.copy(alpha = 0.95f)).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Round ${round + 1}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6B4A2E))
                    Text("Find", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B4A2E))
                    Text(target, fontSize = 72.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE8572A))
                }
            }

            if (!playing) {
                // Start / finished card with the level picker
                Column(
                    Modifier.align(Alignment.Center).padding(16.dp).clip(RoundedCornerShape(28.dp)).background(Color.White).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painterResource(Res.drawable.pic_ili), contentDescription = "Mouse", contentScale = ContentScale.Crop,
                        modifier = Modifier.size(92.dp).clip(CircleShape).border(4.dp, Color(0xFFFFC93C), CircleShape)
                    )
                    if (stars > 0) {
                        Text("You bonked them all! 🎉", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = PopInk, textAlign = TextAlign.Center)
                        Text("★".repeat(stars) + "☆".repeat(3 - stars), fontSize = 30.sp, color = Color(0xFFEF9F27))
                    } else {
                        Text("Bonk the mice holding the letter you hear!", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = PopInk, textAlign = TextAlign.Center)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        popLevels.forEachIndexed { i, l ->
                            val locked = i > 0 && !premium
                            val best = remember(bestVersion, i) { prefs.getInt(bestKey(i), 0) }
                            val on = i == levelIndex
                            Column(
                                Modifier.clip(RoundedCornerShape(14.dp)).background(if (on) PopInk else Color(0xFFFFF1C9))
                                    .clickable {
                                        SoundEffectsHelper.playButtonTap()
                                        if (locked) Access.askGrownUp("Consonants and Mixed Mouse Pop are in the Gold pack.")
                                        else { levelIndex = i; stars = 0 }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("mouse_level_${i + 1}"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text((if (locked) "👑 " else "") + l.title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (on) Color.White else PopInk)
                                Text("★".repeat(best) + "☆".repeat(3 - best), fontSize = 11.sp, color = Color(0xFFEF9F27))
                            }
                        }
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(26.dp)).background(Color(0xFFE8572A)).clickable { startGame() }
                            .padding(horizontal = 30.dp, vertical = 12.dp).testTag("mouse_start"),
                        contentAlignment = Alignment.Center
                    ) { Text(if (stars > 0) "🔁 Play again" else "▶ Play", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
                }
            }
        }
    }
}

/** One hole: a dirt mound with a mouse that slides up holding a letter sign, and squashes when bonked. */
@Composable
private fun MoleHole(mole: Mole?, modifier: Modifier, onBonk: () -> Unit) {
    val rise by animateFloatAsState(if (mole?.up == true) 0f else 1f, spring(dampingRatio = 0.55f, stiffness = 420f), label = "rise")
    val squash = remember { Animatable(1f) }
    LaunchedEffect(mole?.hit, mole?.serial) {
        if (mole?.hit == true) {
            squash.snapTo(1f)
            squash.animateTo(0.55f, tween(90))
            squash.animateTo(1f, spring(dampingRatio = 0.35f))
        }
    }
    val wiggle = remember { Animatable(0f) }
    LaunchedEffect(mole?.wrong, mole?.serial) {
        if (mole?.wrong == true) for (x in listOf(8f, -8f, 6f, -6f, 0f)) wiggle.animateTo(x, tween(50))
    }

    BoxWithConstraints(
        modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBonk() },
        contentAlignment = Alignment.BottomCenter
    ) {
        val w = maxWidth
        val h = maxHeight
        val moundH = h * 0.30f
        val mouseSize = minOf(w * 0.74f, h * 0.62f)

        // The hole (dark) behind the mouse
        Canvas(Modifier.fillMaxWidth().height(moundH)) {
            drawOval(Hole, Offset(size.width * 0.14f, size.height * 0.02f), Size(size.width * 0.72f, size.height * 0.5f))
        }
        // The mouse, clipped so it seems to come out of the hole
        Box(
            Modifier.fillMaxWidth().fillMaxHeight().padding(bottom = moundH * 0.55f).clipToBounds(),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (mole != null) {
                Column(
                    Modifier.graphicsLayer {
                        translationY = rise * (mouseSize.toPx() * 1.5f)
                        scaleY = squash.value
                        scaleX = 2f - squash.value
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                        translationX = wiggle.value * density
                    },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Letter sign held up by the mouse
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(if (mole.hit) Color(0xFFD7F5C8) else if (mole.wrong) Color(0xFFFFD6D6) else Sign)
                            .border(2.5.dp, DirtDark, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(mole.letter, fontSize = (mouseSize.value * 0.34f).sp, fontWeight = FontWeight.ExtraBold, color = PopInk)
                    }
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painterResource(Res.drawable.pic_ili), contentDescription = "Mouse", contentScale = ContentScale.Crop,
                            modifier = Modifier.size(mouseSize * 0.78f).clip(CircleShape).border(3.dp, Color.White, CircleShape)
                        )
                        if (mole.hit) Text("💫", fontSize = (mouseSize.value * 0.3f).sp, modifier = Modifier.align(Alignment.TopEnd))
                        if (mole.wrong) Text("😜", fontSize = (mouseSize.value * 0.26f).sp, modifier = Modifier.align(Alignment.TopEnd))
                    }
                }
            }
        }
        // The dirt mound in front
        Canvas(Modifier.fillMaxWidth().height(moundH * 0.62f)) {
            drawRoundRect(DirtDark, Offset(size.width * 0.06f, size.height * 0.18f), Size(size.width * 0.88f, size.height * 0.82f), CornerRadius(size.height, size.height))
            drawRoundRect(Dirt, Offset(size.width * 0.06f, 0f), Size(size.width * 0.88f, size.height * 0.8f), CornerRadius(size.height, size.height))
            drawCircle(DirtDark.copy(alpha = 0.4f), size.height * 0.08f, Offset(size.width * 0.3f, size.height * 0.4f))
            drawCircle(DirtDark.copy(alpha = 0.4f), size.height * 0.06f, Offset(size.width * 0.68f, size.height * 0.3f))
        }
    }
}
