package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ThemeWord
import com.example.data.ThemeWords
import com.example.data.WordTheme
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.data.Access
import com.example.ui.data.ScoreManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Game id of Tap & Learn (see GameRegistry). */
const val TAP_LEARN_GAME_ID = 24

/** One picture scene: a theme's 10 words drawn on a backdrop. */
private data class TapScene(
    val theme: WordTheme,
    val title: String,
    val emoji: String,
    val sky: Color,
    val accent: Color,
    val free: Boolean
)

private val tapScenes = listOf(
    TapScene(ThemeWords.fruits, "Fruit market", "🍎", Color(0xFFFFF1D6), Color(0xFFFF6B6B), free = true),
    TapScene(ThemeWords.vegetables, "Vegetable shop", "🥕", Color(0xFFE8F8E4), Color(0xFF59C173), free = false),
    TapScene(ThemeWords.bodyParts, "My body", "🙂", Color(0xFFE6F2FF), Color(0xFF4DA8FF), free = false),
    TapScene(ThemeWords.family, "My family", "👨‍👩‍👧", Color(0xFFFDEBF3), Color(0xFFE84393), free = false),
)

/** Where the 10 pictures sit in a scene (fractions of the scene's width and height). */
private val spots = listOf(
    0.16f to 0.22f, 0.39f to 0.20f, 0.62f to 0.22f, 0.85f to 0.20f,
    0.16f to 0.50f, 0.39f to 0.48f, 0.62f to 0.50f, 0.85f to 0.48f,
    0.30f to 0.78f, 0.70f to 0.78f,
)

private val TapInk = Color(0xFF0B3B66)
private val TapSoft = Color(0xFF5B6B85)
private const val FINDS_PER_ROUND = 10

/**
 * Tap & Learn: pick a scene (fruit market, vegetable shop, my body, my family). In "Explore", tap any
 * picture to hear its Kannada name; in "Find it!", the app says a word and the child taps that picture.
 * Uses the theme words and voice clips of the Fun Videos (data/ThemeWords.kt), so no new audio is needed.
 * The fruit market is free; the other scenes are in Gold.
 */
@Composable
fun TapLearnScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sub by com.example.ui.data.SubscriptionManager.state.collectAsState()
    val premium = remember(sub) { Access.isPremium() }
    var scene by remember { mutableStateOf<TapScene?>(null) }

    Column(Modifier.fillMaxSize().background(Color(0xFFFFF8EC)).safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.White)
                    .clickable { if (scene != null) scene = null else onBack() }
                    .testTag("tap_learn_back"),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TapInk) }
            Column(Modifier.weight(1f)) {
                Text(scene?.title ?: "Tap & Learn", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TapInk)
                Text(scene?.theme?.kannada ?: "Tap the pictures, hear the words", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TapSoft)
            }
        }

        val current = scene
        if (current == null) {
            ScenePicker(premium) { picked ->
                SoundEffectsHelper.playButtonTap()
                if (picked.free || premium) {
                    scene = picked
                    NativeAudioPlayer.playPhonics(context, picked.theme.kannada)
                } else {
                    Access.askGrownUp("The ${picked.title} scene and all the other picture scenes are in the Gold pack.")
                }
            }
        } else {
            key(current) { ScenePlay(current, onPickAnother = { scene = null }) }
        }
    }
}

@Composable
private fun ScenePicker(premium: Boolean, onPick: (TapScene) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pick a place to explore", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TapInk)
        tapScenes.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { s ->
                    val locked = !s.free && !premium
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(Color.White)
                            .border(3.dp, s.accent.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                            .clickable { onPick(s) }
                            .padding(12.dp)
                            .testTag("tap_scene_${s.theme.folder}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1.3f).clip(RoundedCornerShape(16.dp)).background(s.sky),
                            contentAlignment = Alignment.Center
                        ) {
                            // A little collage of the scene's pictures
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(s.theme.words.take(3).joinToString(" ") { it.picture }, fontSize = 26.sp)
                                Text(s.theme.words.drop(3).take(3).joinToString(" ") { it.picture }, fontSize = 26.sp)
                            }
                            if (locked) {
                                Text(
                                    "👑 Gold",
                                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                                        .background(Color(0xFFFFC93C), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF5A3A00)
                                )
                            }
                        }
                        Text(s.title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TapInk, textAlign = TextAlign.Center)
                        Text(s.theme.kannada, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = s.accent, textAlign = TextAlign.Center)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ScenePlay(scene: TapScene, onPickAnother: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val words = scene.theme.words.take(spots.size)
    var findMode by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf<ThemeWord?>(null) }
    var target by remember { mutableStateOf<ThemeWord?>(null) }
    var found by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val bounce = remember(scene) { words.map { Animatable(1f) } }
    val shake = remember(scene) { words.map { Animatable(0f) } }

    fun say(text: String) = NativeAudioPlayer.playPhonics(context, text)
    fun nextTarget() {
        val pick = words.filter { it != target }.random()
        target = pick
        say(pick.kannada)
    }
    fun startFind() {
        findMode = true; found = 0; mistakes = 0; done = false; shown = null
        nextTarget()
    }

    fun onTap(i: Int) {
        val w = words[i]
        scope.launch {
            bounce[i].snapTo(1f)
            bounce[i].animateTo(1.35f, tween(120))
            bounce[i].animateTo(1f, spring(dampingRatio = 0.4f))
        }
        if (!findMode) {
            SoundEffectsHelper.playPop()
            shown = w
            say(w.kannada)
            return
        }
        if (busy || done) return
        if (w == target) {
            SoundEffectsHelper.playSuccess()
            shown = w
            found++
            busy = true
            scope.launch {
                say(ThemeWords.WELL_DONE)
                delay(1100)
                if (found >= FINDS_PER_ROUND) {
                    done = true
                    SoundEffectsHelper.playApplause()
                    try { ScoreManager.addCoins(context, "tap_learn", (FINDS_PER_ROUND - mistakes).coerceAtLeast(3)) } catch (e: Exception) {}
                } else {
                    nextTarget()
                }
                busy = false
            }
        } else {
            SoundEffectsHelper.playOops()
            mistakes++
            say(w.kannada)
            scope.launch {
                for (x in listOf(10f, -10f, 7f, -7f, 0f)) shake[i].animateTo(x, tween(45))
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Mode switch
        Row(
            Modifier.padding(horizontal = 14.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ModeChip("👆 Explore", !findMode, Modifier.weight(1f)) { if (findMode) { findMode = false; target = null; done = false } }
            ModeChip("🔍 Find it!", findMode, Modifier.weight(1f)) { if (!findMode) startFind() }
        }

        // Prompt line
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp).fillMaxWidth().heightIn(min = 52.dp)
                .clip(RoundedCornerShape(16.dp)).background(scene.accent.copy(alpha = 0.14f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val t = target
            val s = shown
            when {
                findMode && t != null && !done -> {
                    Text("Find:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TapSoft)
                    Text(t.kannada, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TapInk, modifier = Modifier.weight(1f))
                    Text("⭐ $found/$FINDS_PER_ROUND", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TapInk)
                    SpeakButton(scene.accent) { say(t.kannada) }
                }
                s != null && !findMode -> {
                    Text(s.picture, fontSize = 26.sp)
                    Column(Modifier.weight(1f)) {
                        Text(s.kannada, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TapInk)
                        Text(s.english, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TapSoft)
                    }
                    SpeakButton(scene.accent) { say(s.sentence) }
                }
                else -> Text(
                    if (findMode) "Well done!" else "Tap a picture to hear its name",
                    fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TapInk
                )
            }
        }

        // The scene
        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 14.dp)
                .clip(RoundedCornerShape(24.dp)).background(scene.sky)
        ) {
            val w = maxWidth
            val h = maxHeight
            val pic = minOf(w / 5.2f, h / 4.6f)
            Canvas(Modifier.fillMaxSize()) { drawBackdrop(scene) }
            words.forEachIndexed { i, word ->
                val (fx, fy) = spots[i]
                Box(
                    Modifier
                        .offset(x = w * fx - pic / 2, y = h * fy - pic / 2)
                        .size(pic)
                        .graphicsLayer {
                            scaleX = bounce[i].value; scaleY = bounce[i].value
                            translationX = shake[i].value * density
                        }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.55f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTap(i) }
                        .testTag("tap_item_$i"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(word.picture, fontSize = (pic.value * 0.58f).sp)
                }
            }

            if (done) {
                Column(
                    Modifier.align(Alignment.Center).padding(20.dp).clip(RoundedCornerShape(24.dp)).background(Color.White)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🎉", fontSize = 44.sp)
                    Text("You found them all!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TapInk, textAlign = TextAlign.Center)
                    Text("ಶಭಾಷ್! ⭐ $FINDS_PER_ROUND", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = scene.accent)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BigPill("🔁 Again", scene.accent) { SoundEffectsHelper.playButtonTap(); startFind() }
                        BigPill("🗺️ New place", TapInk) { SoundEffectsHelper.playButtonTap(); onPickAnother() }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(if (on) TapInk else Color.Transparent)
            .clickable { SoundEffectsHelper.playButtonTap(); onClick() }.padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (on) Color.White else TapInk)
    }
}

@Composable
private fun SpeakButton(color: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(color).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Say it again", tint = Color.White) }
}

@Composable
private fun BigPill(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(22.dp)).background(color).clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
}

/** Simple flat backdrops: a market stall, a shop, a mirror wall, a living room. */
private fun DrawScope.drawBackdrop(scene: TapScene) {
    val w = size.width
    val h = size.height
    when (scene.theme.folder) {
        "Fruits", "Vegetables" -> {
            // Striped awning along the top
            val stripes = 8
            val sw = w / stripes
            for (i in 0 until stripes) {
                drawRect(if (i % 2 == 0) scene.accent else Color.White, Offset(i * sw, 0f), Size(sw, h * 0.07f))
                drawCircle(if (i % 2 == 0) scene.accent else Color.White, sw / 2, Offset(i * sw + sw / 2, h * 0.07f))
            }
            // Wooden shelves under each row of pictures
            val wood = Color(0xFFC98B55)
            val woodDark = Color(0xFFA86E3D)
            listOf(0.31f, 0.59f, 0.87f).forEach { y ->
                drawRoundRect(woodDark, Offset(w * 0.04f, h * y + 6f), Size(w * 0.92f, h * 0.045f), CornerRadius(12f, 12f))
                drawRoundRect(wood, Offset(w * 0.04f, h * y), Size(w * 0.92f, h * 0.04f), CornerRadius(12f, 12f))
            }
        }
        "Body Parts" -> {
            // A big round mirror on a soft wall
            drawCircle(Color.White.copy(alpha = 0.7f), minOf(w, h) * 0.46f, Offset(w / 2, h / 2))
            drawCircle(scene.accent.copy(alpha = 0.25f), minOf(w, h) * 0.46f, Offset(w / 2, h / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(14f))
        }
        else -> {
            // Living room: a rug and a sofa at the bottom, two picture frames on the wall
            drawRoundRect(Color(0xFFF9C6DA), Offset(w * 0.08f, h * 0.88f), Size(w * 0.84f, h * 0.1f), CornerRadius(40f, 40f))
            drawRoundRect(scene.accent.copy(alpha = 0.35f), Offset(w * 0.1f, h * 0.64f), Size(w * 0.8f, h * 0.2f), CornerRadius(30f, 30f))
            drawRoundRect(Color(0xFFFFE08A), Offset(w * 0.08f, h * 0.05f), Size(w * 0.14f, h * 0.08f), CornerRadius(8f, 8f))
            drawRoundRect(Color(0xFFBDE3FF), Offset(w * 0.78f, h * 0.05f), Size(w * 0.14f, h * 0.08f), CornerRadius(8f, 8f))
        }
    }
}
