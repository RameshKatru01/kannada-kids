package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.audio.BalloonMusic
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.data.ActivityTracker
import com.example.ui.data.ScoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.random.Random

/*
 * Musical Balloon Pop (endless): balloons float up, each with a letter. Pop the one the bird asks for.
 * Background music plays all the time and gets faster as the game gets harder.
 * - Popping a wrong balloon, or letting the right one fly away, costs a life (3 lives, hearts give more).
 * - Every 30 seconds: next stage (faster balloons, more of them, more letters, look-alike letters).
 * - Special balloons: ❤️ extra life, ⭐ bonus points, 🐌 slows everything for a few seconds.
 */

private enum class BalloonKind { Letter, Heart, Star, Snail }

private data class Balloon(
    val id: Int,
    val kind: BalloonKind,
    val text: String,
    val x: Float,          // 0..1 across the screen
    val y: Float,          // 0 = top, 1 = bottom (balloons rise, so y goes down)
    val color: Color,
    val sway: Float,       // phase for side-to-side drift
    val popped: Boolean = false,
    val popAge: Float = 0f
)

private val balloonPalette = listOf(
    Color(0xFFFF6B6B), Color(0xFFFFC93C), Color(0xFF2EC4B6), Color(0xFF6C63FF),
    Color(0xFFFF9F1C), Color(0xFFE84393), Color(0xFF4DA8FF), Color(0xFF59C173)
)

private val popStages get() = listOf(
    listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ"),
    listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ", "ಊ", "ಎ", "ಏ", "ಐ", "ಒ", "ಓ", "ಔ"),
    listOf("ಕ", "ಖ", "ಗ", "ಘ", "ಚ", "ಛ", "ಜ", "ಝ", "ಅ", "ಆ", "ಇ", "ಈ"),
    listOf("ಟ", "ಠ", "ಡ", "ಢ", "ತ", "ಥ", "ದ", "ಧ", "ನ", "ಕ", "ಗ", "ಜ"),
    listOf("ಪ", "ಫ", "ಬ", "ಭ", "ಮ", "ಯ", "ರ", "ಲ", "ವ", "ಶ", "ಷ", "ಸ", "ಹ", "ಳ"),
    listOf("ಕ", "ಕಾ", "ಕಿ", "ಕೀ", "ಕು", "ಕೂ", "ಕೆ", "ಕೇ", "ಕೊ", "ಕೋ", "೧", "೨", "೩", "೪", "೫", "೬", "೭", "೮", "೯")
).map { com.example.data.Lang.letters(it) }
private val popStageNames get() = listOf("Warm up", "All vowels", "First consonants", com.example.data.Lang.script("ಟ to ನ"), com.example.data.Lang.script("ಪ to ಳ").replace("to ल", "to ह"), "${com.example.data.Lang.islandName("guninthakshari")} & numbers")
private const val POP_STAGE_SECONDS = 30f
private const val POP_MAX_LIVES = 5

@Composable
fun MusicalBalloonPopScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { getSharedPreferences("GamePrefs") }
    val user = remember { getSharedPreferences("AppProgress").getString("last_active_user", "guest") ?: "guest" }
    val bestKey = "${user}_balloon_pop_best"

    var game by remember { mutableIntStateOf(0) }
    var balloons by remember { mutableStateOf(listOf<Balloon>()) }
    var lives by remember { mutableIntStateOf(3) }
    var score by remember { mutableIntStateOf(0) }
    var popped by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var elapsed by remember { mutableFloatStateOf(0f) }
    var slowLeft by remember { mutableFloatStateOf(0f) }
    var target by remember { mutableStateOf(popStages[0].random()) }
    var paused by remember { mutableStateOf(false) }
    var over by remember { mutableStateOf(false) }
    var best by remember { mutableIntStateOf(prefs.getInt(bestKey, 0)) }
    var newBest by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf<String?>("Pop the balloon the bird asks for!") }
    // The best score is saved the moment it is beaten (so it is kept even if the child stops early)
    LaunchedEffect(score) {
        if (score > best) {
            if (best > 0 && !newBest) { banner = "New best score! 🏆"; SoundEffectsHelper.playSparkle() }
            best = score
            newBest = true
            prefs.edit().putInt(bestKey, score).apply()
        }
    }
    var musicOn by remember { mutableStateOf(prefs.getBoolean("${user}_balloon_music", true)) }
    var hurt by remember { mutableIntStateOf(0) }
    var birdHappy by remember { mutableStateOf(false) }

    fun stageNow() = (elapsed / POP_STAGE_SECONDS).toInt().coerceAtMost(popStages.size - 1)
    fun boostNow() = ((elapsed / POP_STAGE_SECONDS).toInt() - (popStages.size - 1)).coerceAtLeast(0)
    val stage = stageNow()
    val level = stage + boostNow()

    // ---- Background music: tempo rises with the level ----
    val bpm = (108 + level * 6).coerceAtMost(150)
    var musicHandle by remember { mutableStateOf<Any?>(null) }
    LaunchedEffect(bpm, musicOn, paused, over) {
        PlatformAudio.stopLoop(musicHandle)
        musicHandle = null
        if (musicOn && !paused && !over) {
            val samples = withContext(Dispatchers.Default) { BalloonMusic.track(bpm) }
            musicHandle = PlatformAudio.startLoop(samples, BalloonMusic.SAMPLE_RATE)
        }
    }
    DisposableEffect(Unit) { onDispose { PlatformAudio.stopLoop(musicHandle) } }

    fun sayTarget() = NativeAudioPlayer.playPhonics(context, target)

    fun nextTarget() {
        target = popStages[stageNow()].filter { it != target }.random()
        scope.launch { delay(250); sayTarget() }
    }

    fun loseLife(reason: String) {
        lives -= 1
        combo = 0
        hurt += 1
        SoundEffectsHelper.playOops(context)
        banner = reason
        if (lives <= 0) {
            over = true
            if (score > best) {
                best = score; newBest = true
                prefs.edit().putInt(bestKey, score).apply()
            }
            if (popped > 0) try { ScoreManager.addCoins(context, "balloon_pop", popped) } catch (e: Exception) {}
            SoundEffectsHelper.playGameCompletion(context)
        }
    }

    fun restart() {
        balloons = emptyList(); lives = 3; score = 0; popped = 0; combo = 0; elapsed = 0f; slowLeft = 0f
        paused = false; over = false; newBest = false
        target = popStages[0].random()
        banner = "Pop the balloon the bird asks for!"
        game += 1
    }

    LaunchedEffect(game) { delay(700); sayTarget() }

    var lastLevel by remember { mutableIntStateOf(0) }
    LaunchedEffect(game) { lastLevel = 0 }
    LaunchedEffect(level) {
        if (level > lastLevel) {
            lastLevel = level
            banner = if (level >= popStages.size) "Faster! Stage ${level + 1} 🎵" else "Stage ${level + 1}: ${popStageNames[stage]} 🎵"
            SoundEffectsHelper.playSparkle()
            if (target !in popStages[stage]) nextTarget()
        }
    }
    LaunchedEffect(banner) { if (banner != null) { delay(2200); banner = null } }

    fun pop(b: Balloon) {
        if (b.popped || over || paused) return
        balloons = balloons.map { if (it.id == b.id) it.copy(popped = true, popAge = 0f) else it }
        SoundEffectsHelper.playPop()
        when (b.kind) {
            BalloonKind.Letter -> if (b.text == target) {
                popped += 1
                combo += 1
                score += 10 + 5 * (combo / 3)
                birdHappy = true
                scope.launch { delay(600); birdHappy = false }
                ActivityTracker.recordAnswer(target, true)
                SoundEffectsHelper.playSuccess(context)
                if (combo % 5 == 0) banner = "$combo in a row! ⭐"
                nextTarget()
            } else {
                ActivityTracker.recordAnswer(target, false)
                loseLife("Oops! That was ${b.text}. Find $target")
            }
            BalloonKind.Heart -> { lives = (lives + 1).coerceAtMost(POP_MAX_LIVES); SoundEffectsHelper.playSparkle(); banner = "Extra life! ❤️" }
            BalloonKind.Star -> { score += 25; SoundEffectsHelper.playSparkle(); banner = "Bonus +25 ⭐" }
            BalloonKind.Snail -> { slowLeft = 6f; SoundEffectsHelper.playSparkle(); banner = "Slow motion! 🐌" }
        }
    }

    // ---- Game loop ----
    LaunchedEffect(game, paused, over) {
        if (paused || over) return@LaunchedEffect
        var last = 0L
        var spawn = 0.3f
        var nextId = (balloons.maxOfOrNull { it.id } ?: 0) + 1
        while (isActive && !over) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
                elapsed += dt
                if (slowLeft > 0f) slowLeft = (slowLeft - dt).coerceAtLeast(0f)
                val lv = stageNow() + boostNow()
                val slow = if (slowLeft > 0f) 0.45f else 1f
                val rise = (0.10f + 0.022f * lv).coerceAtMost(0.34f) * slow   // screen heights per second

                spawn -= dt * slow
                if (spawn <= 0f) {
                    spawn = (1.35f - 0.1f * lv).coerceAtLeast(0.45f)
                    val pool = popStages[stageNow()]
                    val targetOnScreen = balloons.any { !it.popped && it.kind == BalloonKind.Letter && it.text == target && it.y > 0.05f }
                    val roll = Random.nextFloat()
                    val (kind, text) = when {
                        !targetOnScreen || roll < 0.38f -> BalloonKind.Letter to target
                        roll < 0.42f && lives < POP_MAX_LIVES -> BalloonKind.Heart to "❤️"
                        roll < 0.46f -> BalloonKind.Star to "⭐"
                        roll < 0.49f -> BalloonKind.Snail to "🐌"
                        else -> {
                            val i = pool.indexOf(target)
                            val near = pool.filter { it != target }.sortedBy { kotlin.math.abs(pool.indexOf(it) - i) }
                            BalloonKind.Letter to (if (lv >= 3) near.take(3).random() else near.random())
                        }
                    }
                    // Keep balloons from starting on top of each other
                    var x = Random.nextFloat() * 0.76f + 0.12f
                    repeat(6) {
                        if (balloons.none { b -> !b.popped && b.y > 0.85f && kotlin.math.abs(b.x - x) < 0.16f }) return@repeat
                        x = Random.nextFloat() * 0.76f + 0.12f
                    }
                    balloons = balloons + Balloon(nextId++, kind, text, x, 1.12f, balloonPalette[nextId % balloonPalette.size], Random.nextFloat() * 6f)
                }

                val keep = mutableListOf<Balloon>()
                for (b in balloons) {
                    if (b.popped) {
                        val aged = b.copy(popAge = b.popAge + dt)
                        if (aged.popAge < 0.35f) keep += aged
                        continue
                    }
                    val moved = b.copy(y = b.y - rise * dt, sway = b.sway + dt * (1.2f + 0.15f * lv))
                    if (moved.y < -0.15f) {
                        if (moved.kind == BalloonKind.Letter && moved.text == target) {
                            loseLife("The $target balloon flew away!")
                            nextTarget()
                        }
                        continue
                    }
                    keep += moved
                }
                balloons = keep
            }
        }
    }

    val shake = rememberShake(hurt)

    Box(Modifier.fillMaxSize().background(Color(0xFFBDE6FF))) {
        // Sky: sun, drifting clouds
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0xFFFFE27A), size.minDimension * 0.12f, Offset(size.width * 0.85f, size.height * 0.12f))
            for (i in 0 until 4) {
                val cx = ((i * 0.31f + elapsed * 0.012f * (1 + i % 2)) % 1.3f - 0.15f) * size.width
                val cy = size.height * (0.18f + 0.2f * i)
                val r = size.minDimension * 0.06f
                drawCircle(Color.White.copy(alpha = 0.85f), r, Offset(cx, cy))
                drawCircle(Color.White.copy(alpha = 0.85f), r * 1.3f, Offset(cx + r * 1.2f, cy - r * 0.3f))
                drawCircle(Color.White.copy(alpha = 0.85f), r, Offset(cx + r * 2.4f, cy))
            }
            drawRect(Color(0xFF8FD694), Offset(0f, size.height * 0.93f), Size(size.width, size.height * 0.07f))
        }

        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val w = maxWidth
            val h = maxHeight
            val bw: Dp = minOf(w * 0.2f, 92.dp)
            balloons.forEach { b ->
                val dx = sin(b.sway) * 0.025f
                val cx = w * (b.x + dx)
                val cy = h * b.y
                BalloonView(
                    b, bw,
                    Modifier
                        .offset(x = cx - bw / 2, y = cy - bw * 0.6f)
                        .graphicsLayer {
                            if (b.popped) {
                                val s = 1f + b.popAge * 2.5f
                                scaleX = s; scaleY = s; alpha = (1f - b.popAge / 0.35f).coerceIn(0f, 1f)
                            }
                        }
                ) { pop(b) }
            }

            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PopRoundButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                Row(
                    Modifier.weight(1f).graphicsLayer { translationX = shake * density }
                        .clip(RoundedCornerShape(22.dp)).background(Color.White).clickable { sayTarget() }
                        .padding(horizontal = 10.dp, vertical = 2.dp).testTag("pop_target"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(if (birdHappy) "🐦🎶" else "🐦", fontSize = 24.sp)
                    Text("Pop", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = GameInk)
                    Text(target, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD94848))
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear the letter", tint = GameInk)
                }
                Column(
                    Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xCC1E2A47)).padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⭐ $score", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("🏆 $best", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = if (newBest) Color(0xFFFFC93C) else Color(0xFFCFE3F5))
                    Text("❤️".repeat(lives.coerceAtLeast(0)), fontSize = 12.sp)
                }
            }
            Row(
                Modifier.align(Alignment.TopEnd).padding(top = 64.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PopRoundButton({
                    musicOn = !musicOn
                    prefs.edit().putBoolean("${user}_balloon_music", musicOn).apply()
                }) { Icon(if (musicOn) Icons.Default.MusicNote else Icons.Default.MusicOff, contentDescription = if (musicOn) "Music off" else "Music on", tint = Color.White) }
                PopRoundButton({ paused = !paused }) {
                    Icon(if (paused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = if (paused) "Resume" else "Pause", tint = Color.White)
                }
            }

            Column(Modifier.align(Alignment.TopCenter).padding(top = 66.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Stage ${level + 1} · ${elapsed.toInt() / 60}:${(elapsed.toInt() % 60).toString().padStart(2, '0')}" + if (slowLeft > 0f) " · 🐌" else "",
                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    modifier = Modifier.background(Color(0x991E2A47), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 3.dp)
                )
                banner?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = GameInk, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp).background(Color(0xFFFFF6E0), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            if (paused && !over) {
                Box(
                    Modifier.fillMaxSize().background(Color(0x991E2A47))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                    contentAlignment = Alignment.Center
                ) {
                    PopCard("Paused", "The balloons are waiting!", "Keep popping", { paused = false }, "Back to games", onBack)
                }
            }
        }

        if (over) {
            Box(
                Modifier.fillMaxSize().background(Color(0xCC1E2A47)).safeDrawingPadding()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center
            ) {
                PopCard(
                    title = if (newBest) "New best score! 🏆" else "Great popping!",
                    body = "⭐ $score points\n🎈 $popped balloons popped\n⏱️ ${elapsed.toInt() / 60}:${(elapsed.toInt() % 60).toString().padStart(2, '0')} · stage ${level + 1}\n\n🏆 Best: $best" + (if (newBest) "\nYou beat your best!" else "\nCan you beat it next time?"),
                    primary = "Play again",
                    onPrimary = { restart() },
                    secondary = "Back to games",
                    onSecondary = onBack
                )
            }
        }
    }
}

/** A shiny balloon with a string; letter, heart, star or snail inside. */
@Composable
private fun BalloonView(b: Balloon, width: Dp, modifier: Modifier, onTap: () -> Unit) {
    val special = b.kind != BalloonKind.Letter
    val fill = when (b.kind) {
        BalloonKind.Heart -> Color(0xFFFF8FB1)
        BalloonKind.Star -> Color(0xFFFFD54F)
        BalloonKind.Snail -> Color(0xFFB8E986)
        else -> b.color
    }
    Box(
        modifier
            .size(width, width * 1.55f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTap() }
            .testTag("balloon_${b.id}"),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val bw = size.width
            val bh = bw * 1.15f
            // string
            val string = Path().apply {
                moveTo(bw / 2, bh)
                cubicTo(bw * 0.35f, bh + size.height * 0.12f, bw * 0.65f, bh + size.height * 0.2f, bw / 2, size.height)
            }
            drawPath(string, Color(0x99475569), style = Stroke(width = 3f))
            // knot
            val knot = Path().apply {
                moveTo(bw / 2, bh - 4f); lineTo(bw / 2 - 9f, bh + 10f); lineTo(bw / 2 + 9f, bh + 10f); close()
            }
            drawPath(knot, fill)
            // body
            drawOval(fill, Offset(0f, 0f), Size(bw, bh))
            if (special) drawOval(Color.White, Offset(0f, 0f), Size(bw, bh), style = Stroke(width = 6f))
            // shine
            drawOval(Color.White.copy(alpha = 0.45f), Offset(bw * 0.18f, bh * 0.12f), Size(bw * 0.18f, bh * 0.28f))
        }
        Box(Modifier.size(width, width * 1.15f), contentAlignment = Alignment.Center) {
            Text(
                b.text,
                fontSize = (width.value * (if (special) 0.42f else if (b.text.length > 1) 0.34f else 0.44f)).sp,
                fontWeight = FontWeight.ExtraBold,
                color = GameInk
            )
        }
    }
}

@Composable
private fun PopRoundButton(onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xCC1E2A47)).clickable { onClick() }, contentAlignment = Alignment.Center) { icon() }
}

@Composable
private fun PopCard(title: String, body: String, primary: String, onPrimary: () -> Unit, secondary: String, onSecondary: () -> Unit) {
    Column(
        Modifier.padding(24.dp).widthIn(max = 380.dp).fillMaxWidth().background(Color.White, RoundedCornerShape(30.dp)).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = GameInk, textAlign = TextAlign.Center)
        Text(body, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GameInk, textAlign = TextAlign.Center, lineHeight = 24.sp)
        Box(
            Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).background(Color(0xFFE84393)).clickable { onPrimary() }.testTag("pop_primary"),
            contentAlignment = Alignment.Center
        ) { Text(primary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
        Box(
            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).border(2.dp, GameInk, RoundedCornerShape(24.dp)).clickable { onSecondary() },
            contentAlignment = Alignment.Center
        ) { Text(secondary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GameInk) }
    }
}
