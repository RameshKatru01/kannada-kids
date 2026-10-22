package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.data.ActivityTracker
import com.example.ui.data.ScoreManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/*
 * Letter Dash: an endless runner. The monster runs down a 3-lane road; catch the letter it asks for,
 * dodge rocks and wrong letters. No time limit: every 30 seconds the stage goes up, the road gets
 * faster, more letters join in and the wrong letters look more alike. 3 lives (hearts can be found).
 * (The original Letter Runner stays as it is.)
 */

private enum class DashKind { Letter, Rock, Heart, Shield }

private data class DashItem(
    val id: Int,
    val kind: DashKind,
    val text: String,
    val lane: Int,
    val y: Float,
    val color: Color
)

/** Letters by stage: starts small, adds groups as the child goes further. */
private val dashStages = listOf(
    listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ"),
    listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ", "ಊ", "ಎ", "ಏ", "ಐ", "ಒ", "ಓ", "ಔ"),
    listOf("ಕ", "ಖ", "ಗ", "ಘ", "ಚ", "ಛ", "ಜ", "ಝ", "ಅ", "ಆ", "ಇ", "ಈ"),
    listOf("ಕ", "ಖ", "ಗ", "ಘ", "ಚ", "ಛ", "ಜ", "ಝ", "ಟ", "ಠ", "ಡ", "ಢ", "ತ", "ಥ", "ದ", "ಧ", "ನ"),
    listOf("ಪ", "ಫ", "ಬ", "ಭ", "ಮ", "ಯ", "ರ", "ಲ", "ವ", "ಶ", "ಷ", "ಸ", "ಹ", "ಳ", "ಕ", "ಗ", "ತ", "ದ"),
    listOf("ಕ", "ಕಾ", "ಕಿ", "ಕೀ", "ಕು", "ಕೂ", "ಕೆ", "ಕೇ", "ಕೊ", "ಕೋ", "೧", "೨", "೩", "೪", "೫", "೬", "೭", "೮", "೯")
)

private const val STAGE_SECONDS = 30f
private const val RUNNER_Y = 0.82f
private const val MAX_LIVES = 5
private val stageNames = listOf("Warm up", "All vowels", "First consonants", "More consonants", "ಪ to ಳ", "Ka-kaa & numbers")

@Composable
fun LetterDashScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { getSharedPreferences("GamePrefs") }
    val user = remember { getSharedPreferences("AppProgress").getString("last_active_user", "guest") ?: "guest" }
    val bestKey = "${user}_letter_dash_best"

    var game by remember { mutableIntStateOf(0) }          // bumps on "play again"
    var items by remember { mutableStateOf(listOf<DashItem>()) }
    var lane by remember { mutableIntStateOf(1) }
    var lives by remember { mutableIntStateOf(3) }
    var score by remember { mutableIntStateOf(0) }
    var caught by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var elapsed by remember { mutableFloatStateOf(0f) }
    var roadOffset by remember { mutableFloatStateOf(0f) }
    var target by remember { mutableStateOf(dashStages[0].random()) }
    var shieldLeft by remember { mutableFloatStateOf(0f) }
    var paused by remember { mutableStateOf(false) }
    var over by remember { mutableStateOf(false) }
    var best by remember { mutableIntStateOf(prefs.getInt(bestKey, 0)) }
    var newBest by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf<String?>("Catch the letter the monster asks for!") }
    var mood by remember { mutableStateOf(MonsterMood.Hungry) }
    var hurtFlash by remember { mutableIntStateOf(0) }

    // Read the clock every time (the game loop must not use values from an old frame)
    fun stageNow() = (elapsed / STAGE_SECONDS).toInt().coerceAtMost(dashStages.size - 1)
    fun boostNow() = ((elapsed / STAGE_SECONDS).toInt() - (dashStages.size - 1)).coerceAtLeast(0)
    val stage = stageNow()
    val endlessBoost = boostNow()

    fun sayTarget() = NativeAudioPlayer.playPhonics(context, target)

    fun nextTarget() {
        val pool = dashStages[stageNow()]
        target = pool.filter { it != target }.random()
        scope.launch { delay(250); sayTarget() }
    }

    fun loseLife(reason: String) {
        if (shieldLeft > 0f) return
        lives -= 1
        combo = 0
        hurtFlash += 1
        mood = MonsterMood.Yuck
        SoundEffectsHelper.playOops(context)
        banner = reason
        scope.launch { delay(700); if (!over) mood = MonsterMood.Hungry }
        if (lives <= 0) {
            over = true
            if (score > best) {
                best = score
                newBest = true
                prefs.edit().putInt(bestKey, score).apply()
            }
            val coins = caught
            if (coins > 0) try { ScoreManager.addCoins(context, "letter_dash", coins) } catch (e: Exception) {}
            SoundEffectsHelper.playGameCompletion(context)
        }
    }

    fun restart() {
        items = emptyList(); lane = 1; lives = 3; score = 0; caught = 0; combo = 0; elapsed = 0f
        shieldLeft = 0f; paused = false; over = false; newBest = false; mood = MonsterMood.Hungry
        target = dashStages[0].random()
        banner = "Catch the letter the monster asks for!"
        game += 1
    }

    // Say the first letter
    LaunchedEffect(game) { delay(600); sayTarget() }

    // Stage-up banner
    var lastStage by remember { mutableIntStateOf(0) }
    LaunchedEffect(game) { lastStage = 0 }
    LaunchedEffect(stage, endlessBoost) {
        val level = stage + endlessBoost
        if (level > lastStage) {
            lastStage = level
            banner = if (endlessBoost > 0) "Faster! Stage ${level + 1}" else "Stage ${level + 1}: ${stageNames[stage]}"
            SoundEffectsHelper.playSparkle()
            if (target !in dashStages[stage]) nextTarget()
        }
    }
    LaunchedEffect(banner) { if (banner != null) { delay(2200); banner = null } }

    // The game loop
    LaunchedEffect(game, paused, over) {
        if (paused || over) return@LaunchedEffect
        var last = 0L
        var spawnTimer = 0.4f
        var nextId = items.maxOfOrNull { it.id + 1 } ?: 0
        while (isActive) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
                elapsed += dt
                if (shieldLeft > 0f) shieldLeft = (shieldLeft - dt).coerceAtLeast(0f)

                val stageIndex = stageNow()
                val level = stageIndex + boostNow()
                val speed = (0.26f + 0.045f * level).coerceAtMost(0.75f)        // screen heights per second
                roadOffset = (roadOffset + speed * dt) % 1f

                // Spawn a new row
                spawnTimer -= dt
                if (spawnTimer <= 0f) {
                    spawnTimer = (1.25f - 0.09f * level).coerceAtLeast(0.5f)
                    val pool = dashStages[stageIndex]
                    val lanes = (0..2).shuffled()
                    val row = mutableListOf<DashItem>()
                    val needTarget = items.none { it.kind == DashKind.Letter && it.text == target && it.y < RUNNER_Y }
                    val letter = if (needTarget || Random.nextFloat() < 0.45f) target else {
                        // Later stages pick decoys that look/sound close to the target
                        val i = pool.indexOf(target)
                        val near = pool.filter { it != target }.sortedBy { kotlin.math.abs(pool.indexOf(it) - i) }
                        if (level >= 3) near.take(3).random() else near.random()
                    }
                    row += DashItem(nextId++, DashKind.Letter, letter, lanes[0], -0.08f, CandyColors[nextId % CandyColors.size])
                    if (Random.nextFloat() < (0.12f + 0.07f * level).coerceAtMost(0.6f)) {
                        row += DashItem(nextId++, DashKind.Rock, listOf("🪨", "🌵", "🚧", "🪵").random(), lanes[1], -0.08f, Color.Transparent)
                    }
                    if (level >= 2 && Random.nextFloat() < 0.35f) {
                        val decoy = pool.filter { it != target }.random()
                        row += DashItem(nextId++, DashKind.Letter, decoy, lanes[2], -0.08f, CandyColors[nextId % CandyColors.size])
                    } else if (lives < MAX_LIVES && Random.nextFloat() < 0.05f) {
                        row += DashItem(nextId++, DashKind.Heart, "❤️", lanes[2], -0.08f, Color.Transparent)
                    } else if (Random.nextFloat() < 0.04f) {
                        row += DashItem(nextId++, DashKind.Shield, "🛡️", lanes[2], -0.08f, Color.Transparent)
                    }
                    items = items + row
                }

                // Move and check collisions
                val keep = mutableListOf<DashItem>()
                for (it in items) {
                    val moved = it.copy(y = it.y + speed * dt)
                    val hit = moved.lane == lane && moved.y >= RUNNER_Y - 0.05f && it.y < RUNNER_Y - 0.05f
                    if (hit) {
                        when (moved.kind) {
                            DashKind.Letter -> if (moved.text == target) {
                                caught += 1
                                combo += 1
                                score += 10 + 5 * (combo / 3)
                                mood = MonsterMood.Chomp
                                ActivityTracker.recordAnswer(target, true)
                                SoundEffectsHelper.playSuccess(context)
                                if (combo % 5 == 0) banner = "$combo in a row! ⭐"
                                scope.launch { delay(450); if (!over) mood = MonsterMood.Hungry }
                                nextTarget()
                            } else {
                                ActivityTracker.recordAnswer(target, false)
                                loseLife("Oops! That was ${moved.text}, not $target")
                            }
                            DashKind.Rock -> loseLife("Bump! Watch out for rocks")
                            DashKind.Heart -> { lives = (lives + 1).coerceAtMost(MAX_LIVES); SoundEffectsHelper.playSparkle(); banner = "Extra life! ❤️" }
                            DashKind.Shield -> { shieldLeft = 6f; SoundEffectsHelper.playSparkle(); banner = "Shield on for 6 seconds! 🛡️" }
                        }
                        continue
                    }
                    if (moved.y < 1.1f) keep += moved
                }
                items = keep
            }
            if (over) break
        }
    }

    val hurt = rememberShake(hurtFlash)

    Box(Modifier.fillMaxSize().background(Color(0xFF8FD694))) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .pointerInput(game) {
                    var dragX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragX = 0f },
                        onDragEnd = {
                            if (dragX > 40f && lane < 2) lane++ else if (dragX < -40f && lane > 0) lane--
                        }
                    ) { change, amount -> change.consume(); dragX += amount }
                }
                .pointerInput(game, "tap") {
                    detectTapGestures { p ->
                        // Tap a side of the road to move that way
                        val third = size.width / 3f
                        val tapped = (p.x / third).toInt().coerceIn(0, 2)
                        lane = when {
                            tapped < lane -> lane - 1
                            tapped > lane -> lane + 1
                            else -> lane
                        }
                    }
                }
        ) {
            val roadW = minOf(maxWidth, 560.dp)
            val roadLeft = (maxWidth - roadW) / 2
            val laneW = roadW / 3
            val h = maxHeight

            // Road
            Canvas(Modifier.fillMaxSize()) {
                val left = roadLeft.toPx()
                val w = roadW.toPx()
                drawRect(Color(0xFF6E7B8B), Offset(left, 0f), Size(w, size.height))
                drawRect(Color(0xFFFFC93C), Offset(left - 10f, 0f), Size(10f, size.height))
                drawRect(Color(0xFFFFC93C), Offset(left + w, 0f), Size(10f, size.height))
                val dash = size.height / 8f
                for (i in 1..2) {
                    val x = left + w / 3f * i
                    var y = -dash + (roadOffset * size.height) % dash
                    while (y < size.height) {
                        drawRect(Color.White.copy(alpha = 0.85f), Offset(x - 4f, y), Size(8f, dash * 0.5f))
                        y += dash
                    }
                }
                // grass tufts scrolling on the sides
                for (k in 0 until 8) {
                    val y = ((k / 8f + roadOffset) % 1f) * size.height
                    drawCircle(Color(0xFF59C173), 14f, Offset(left / 2f, y))
                    drawCircle(Color(0xFF59C173), 14f, Offset(left + w + (size.width - left - w) / 2f, (y + size.height / 16f) % size.height))
                }
            }

            // Items
            items.forEach { item ->
                val x = roadLeft + laneW * item.lane + laneW / 2
                val y = h * item.y
                Box(
                    Modifier
                        .offset(x = x - 34.dp, y = y - 34.dp)
                        .size(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.kind == DashKind.Letter) {
                        Box(
                            Modifier.size(64.dp).shadow(4.dp, CircleShape).clip(CircleShape)
                                .background(item.color).border(4.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.text, fontSize = if (item.text.length > 1) 24.sp else 30.sp, fontWeight = FontWeight.ExtraBold, color = GameInk)
                        }
                    } else {
                        Text(item.text, fontSize = 44.sp)
                    }
                }
            }

            // Runner (the monster)
            val rx = roadLeft + laneW * lane + laneW / 2
            val animX by animateDpAsState(rx, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium), label = "lane")
            val bounce = rememberInfiniteTransition(label = "run")
            val step by bounce.animateFloat(0f, -10f, infiniteRepeatable(tween(220), RepeatMode.Reverse), label = "step")
            Box(
                Modifier
                    .offset(x = animX - 48.dp, y = h * RUNNER_Y - 48.dp)
                    .size(96.dp)
                    .graphicsLayer { translationY = if (paused || over) 0f else step * density; translationX = hurt * density }
            ) {
                if (shieldLeft > 0f) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(Color(0x554DA8FF)).border(4.dp, Color(0xFF4DA8FF), CircleShape))
                }
                Monster(
                    style = MonsterStyle("", Color(0xFFFF9F1C), Color(0xFFD97E00), Color(0xFFFFE2B8), Color.Transparent, "letter_dash"),
                    mood = mood,
                    mouthWide = mood == MonsterMood.Chomp,
                    growth = 0,
                    modifier = Modifier.fillMaxSize().padding(6.dp)
                )
            }

            // Top bar: back, target, score, lives
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoundButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(Color.White).clickable { sayTarget() }
                        .padding(horizontal = 12.dp, vertical = 4.dp).testTag("dash_target"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Catch", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = GameInk)
                    Text(target, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD94848))
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear the letter", tint = GameInk)
                }
                Column(
                    Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xCC1E2A47)).padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⭐ $score", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("❤️".repeat(lives.coerceAtLeast(0)), fontSize = 12.sp)
                }
                RoundButton({ paused = !paused }) {
                    Icon(if (paused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = if (paused) "Resume" else "Pause", tint = Color.White)
                }
            }

            // Stage + banner
            Column(Modifier.align(Alignment.TopCenter).padding(top = 72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Stage ${stage + endlessBoost + 1} · ${elapsed.toInt() / 60}:${(elapsed.toInt() % 60).toString().padStart(2, '0')}",
                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    modifier = Modifier.background(Color(0x991E2A47), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 3.dp)
                )
                banner?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = GameInk, textAlign = TextAlign.Center,
                        modifier = Modifier.background(Color(0xFFFFF6E0), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            if (paused && !over) {
                Box(Modifier.fillMaxSize().background(Color(0x991E2A47)), contentAlignment = Alignment.Center) {
                    DashCard("Paused", "Take a breath!", "Keep running", { paused = false }, "Back to games", onBack)
                }
            }
        }

        if (over) {
            Box(Modifier.fillMaxSize().background(Color(0xCC1E2A47)).safeDrawingPadding(), contentAlignment = Alignment.Center) {
                DashCard(
                    title = if (newBest) "New best score! 🏆" else "Great run!",
                    body = "⭐ $score points\n🔤 $caught letters caught\n⏱️ ${elapsed.toInt() / 60}:${(elapsed.toInt() % 60).toString().padStart(2, '0')} · stage ${stage + endlessBoost + 1}\n\nBest: $best",
                    primary = "Run again",
                    onPrimary = { restart() },
                    secondary = "Back to games",
                    onSecondary = onBack
                )
            }
        }
    }
}

@Composable
private fun RoundButton(onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xCC1E2A47)).clickable { onClick() }, contentAlignment = Alignment.Center) { icon() }
}

@Composable
private fun DashCard(title: String, body: String, primary: String, onPrimary: () -> Unit, secondary: String, onSecondary: () -> Unit) {
    Column(
        Modifier.padding(24.dp).widthIn(max = 380.dp).fillMaxWidth().background(Color.White, RoundedCornerShape(30.dp)).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = GameInk, textAlign = TextAlign.Center)
        Text(body, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GameInk, textAlign = TextAlign.Center, lineHeight = 24.sp)
        Box(
            Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).background(Color(0xFFFF6B6B)).clickable { onPrimary() }.testTag("dash_primary"),
            contentAlignment = Alignment.Center
        ) { Text(primary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
        Box(
            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).border(2.dp, GameInk, RoundedCornerShape(24.dp)).clickable { onSecondary() },
            contentAlignment = Alignment.Center
        ) { Text(secondary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GameInk) }
    }
}
