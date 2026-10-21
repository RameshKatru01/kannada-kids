package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.data.LanguageManager
import com.example.ui.progress.GameProgress
import com.example.ui.progress.LevelResult
import com.example.ui.progress.LevelTally
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * Colour Sort: pour the coloured paint between the jars until every jar holds one colour.
 * When a jar is full of one colour, it says the colour's name in Kannada.
 * 6 levels: 2 colours → 6 colours, jars grow from 3 to 4 scoops.
 */

private data class SortLevel(val colours: Int, val capacity: Int, val empty: Int, val label: String)

private val sortLevels = listOf(
    SortLevel(2, 3, 1, "2 colours"),
    SortLevel(3, 3, 1, "3 colours"),
    SortLevel(3, 4, 2, "3 colours, taller jars"),
    SortLevel(4, 4, 2, "4 colours"),
    SortLevel(5, 4, 2, "5 colours"),
    SortLevel(6, 4, 2, "6 colours")
)

/** Bright, easy-to-tell-apart colours (from Paint the Monster's list, with names). */
private val sortColourIds = listOf("red", "blue", "yellow", "green", "orange", "purple", "pink")

/** Builds a puzzle that can always be solved, by scrambling a solved one with "backwards" pours. */
private fun makeJars(spec: SortLevel, colours: List<Int>): List<List<Int>> {
    repeat(40) {
        val jars = MutableList(spec.colours + spec.empty) { i ->
            if (i < spec.colours) MutableList(spec.capacity) { colours[i] } else mutableListOf()
        }
        var last: Pair<Int, Int>? = null
        repeat(spec.colours * spec.capacity * 12) {
            val moves = buildList {
                for (a in jars.indices) {
                    val src = jars[a]
                    if (src.isEmpty()) continue
                    val u = src.last()
                    val below = src.getOrNull(src.size - 2)
                    if (below != null && below != u) continue // the forward pour back would not be allowed
                    for (b in jars.indices) {
                        if (b != a && jars[b].size < spec.capacity && last != (b to a)) add(a to b)
                    }
                }
            }
            if (moves.isEmpty()) return@repeat
            val (a, b) = moves.random()
            jars[b].add(jars[a].removeAt(jars[a].size - 1))
            last = a to b
        }
        val sortedJars = jars.count { it.size == spec.capacity && it.distinct().size == 1 }
        if (sortedJars == 0) {
            // Keep the empty jars at the end so the puzzle looks tidy
            return jars.sortedBy { if (it.isEmpty()) 1 else 0 }
        }
    }
    // Very unlikely fallback: simple alternating jars
    return List(spec.colours + spec.empty) { i ->
        if (i < spec.colours) List(spec.capacity) { k -> colours[(i + k) % spec.colours] } else emptyList()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColourSortScreen(firstLevel: Int = 1, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang = LanguageManager.currentLanguage.collectAsState().value.code
    var level by remember { mutableIntStateOf((firstLevel - 1).coerceIn(0, sortLevels.size - 1)) }
    val spec = sortLevels[level]
    var round by remember { mutableIntStateOf(0) }
    val palette = remember(level, round) {
        sortColourIds.shuffled().take(spec.colours).mapNotNull { id -> kidColors.firstOrNull { it.id == id } }
    }
    var jars by remember(level, round) { mutableStateOf(makeJars(spec, palette.indices.toList())) }
    var history by remember(level, round) { mutableStateOf(listOf<List<List<Int>>>()) }
    var selected by remember(level, round) { mutableStateOf<Int?>(null) }
    var announced by remember(level, round) { mutableStateOf(setOf<Int>()) }
    var shakeJar by remember { mutableStateOf<Int?>(null) }
    var shakeCount by remember { mutableIntStateOf(0) }
    val tally = remember { LevelTally() }
    var levelResult by remember { mutableStateOf<LevelResult?>(null) }
    var showConfetti by remember { mutableStateOf(false) }
    var finished by remember(level, round) { mutableStateOf(false) }

    fun isComplete(j: List<Int>) = j.size == spec.capacity && j.distinct().size == 1
    val completeCount = jars.count { isComplete(it) }

    fun startLevel(newLevel: Int) {
        tally.reset()
        levelResult = null
        showConfetti = false
        level = newLevel
        round += 1
    }

    fun pour(from: Int, to: Int) {
        val src = jars[from]
        val dst = jars[to]
        val colour = src.last()
        val room = spec.capacity - dst.size
        val run = src.reversed().takeWhile { it == colour }.size
        if (room == 0 || (dst.isNotEmpty() && dst.last() != colour)) {
            tally.wrong()
            SoundEffectsHelper.playOops(context)
            shakeJar = to
            shakeCount += 1
            return
        }
        val n = minOf(room, run)
        history = history + listOf(jars)
        jars = jars.mapIndexed { i, j ->
            when (i) {
                from -> j.dropLast(n)
                to -> j + List(n) { colour }
                else -> j
            }
        }
        SoundEffectsHelper.playPop()
        val newJar = jars[to]
        if (isComplete(newJar) && colour !in announced) {
            announced = announced + colour
            tally.right()
            SoundEffectsHelper.playSuccess(context)
            NativeAudioPlayer.playPhonics(context, palette[colour].name(lang))
        }
        if (!finished && jars.count { isComplete(it) } == spec.colours) {
            finished = true
            scope.launch {
                delay(900)
                showConfetti = true
                SoundEffectsHelper.playGameCompletion(context)
                val result = GameProgress.recordLevel(context, 13, level + 1, tally.mistakes, tally.streaks)
                delay(1200)
                showConfetti = false
                levelResult = result
            }
        }
    }

    fun tapJar(i: Int) {
        if (finished) return
        val sel = selected
        when {
            sel == null -> if (jars[i].isNotEmpty() && !isComplete(jars[i])) { selected = i; SoundEffectsHelper.playClick() }
            sel == i -> selected = null
            else -> { pour(sel, i); selected = null }
        }
    }

    val shake = rememberShake(shakeCount)

    LevelGameScaffold(
        title = "Colour Sort",
        subtitle = "Level ${level + 1}/${sortLevels.size} · ${spec.label}",
        headerColor = Color(0xFFE84393),
        background = Color(0xFFFFF0F7),
        rounds = spec.colours,
        roundsDone = completeCount,
        showConfetti = showConfetti,
        levelResult = levelResult,
        hasNextLevel = level < sortLevels.size - 1,
        onNextLevel = { startLevel(level + 1) },
        onPlayAgain = { startLevel(level) },
        onBack = onBack
    ) {
        Text("Tap a jar, then tap where to pour", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = GameInk)
        Text("Make every jar one colour!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GameInk.copy(alpha = 0.6f))
        Spacer(Modifier.weight(1f))

        val scoop = if (spec.capacity == 3) 52.dp else 46.dp
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(26.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            jars.forEachIndexed { i, jar ->
                val lifted = selected == i
                val lift by animateFloatAsState(if (lifted) -22f else 0f, spring(dampingRatio = 0.5f), label = "lift")
                val complete = isComplete(jar)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(26.dp), contentAlignment = Alignment.Center) {
                        if (complete) Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(26.dp))
                    }
                    Column(
                        Modifier
                            .graphicsLayer {
                                translationY = lift * density
                                translationX = if (shakeJar == i) shake * density else 0f
                            }
                            .width(66.dp)
                            .height(scoop * spec.capacity + 18.dp)
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 30.dp, bottomEnd = 30.dp))
                            .background(Color.White.copy(alpha = 0.85f))
                            .border(
                                4.dp,
                                if (lifted) Color(0xFFFFC93C) else if (complete) palette[jar.first()].color else Color(0xFFB8C7D4),
                                RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 30.dp, bottomEnd = 30.dp)
                            )
                            .clickable { tapJar(i) }
                            .padding(start = 7.dp, end = 7.dp, bottom = 7.dp)
                            .testTag("jar_$i"),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        jar.reversed().forEachIndexed { k, colourIndex ->
                            val isBottom = k == jar.size - 1
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(scoop)
                                    .clip(
                                        if (isBottom) RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp, topStart = 4.dp, topEnd = 4.dp)
                                        else RoundedCornerShape(4.dp)
                                    )
                                    .background(palette[colourIndex].color)
                            ) {
                                // little shine
                                Box(Modifier.padding(start = 6.dp, top = 6.dp).size(8.dp, 16.dp).background(Color.White.copy(alpha = 0.35f), CircleShape))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (complete) palette[jar.first()].name(lang) else " ",
                        fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = GameInk
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(if (history.isEmpty() || finished) Color(0xFFE3E7EE) else Color.White)
                .clickable(enabled = history.isNotEmpty() && !finished) {
                    jars = history.last()
                    history = history.dropLast(1)
                    selected = null
                }
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .testTag("sort_undo"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = GameInk)
            Text("Undo", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = GameInk)
        }
        Spacer(Modifier.height(16.dp))
    }
}
