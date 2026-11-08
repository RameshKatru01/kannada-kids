package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
    SortLevel(6, 4, 3, "6 colours, 3 empty jars")
)

/**
 * Six clearly different colours (pink was too close to red). Each also gets its own white symbol,
 * so children (and colour-blind players) can tell them apart.
 */
private val sortColourIds = listOf("red", "blue", "yellow", "green", "purple", "orange")
private val sortSymbols = listOf("●", "▲", "■", "★", "♥", "◆")

/**
 * Finds a way to finish from [jars] (depth-first search with memory). Returns the first pour
 * (from, to), or null if the jars can no longer be sorted. Used to check new puzzles and for hints.
 */
private fun solveFirstMove(jars: List<List<Int>>, cap: Int, colours: Int, limit: Int = 80_000): Pair<Int, Int>? {
    fun done(s: List<List<Int>>) = s.count { it.size == cap && it.distinct().size == 1 } == colours
    if (done(jars)) return null
    fun key(s: List<List<Int>>) = s.map { it.joinToString(",") }.sorted().joinToString("|")
    val seen = HashSet<String>()
    data class Frame(val state: List<List<Int>>, val first: Pair<Int, Int>?)
    val stack = ArrayDeque<Frame>()
    stack.addLast(Frame(jars, null))
    seen += key(jars)
    while (stack.isNotEmpty() && seen.size < limit) {
        val (state, first) = stack.removeLast()
        val moves = mutableListOf<Pair<Pair<Int, Int>, Int>>()   // move to priority (higher = tried first)
        for (a in state.indices) {
            val src = state[a]
            if (src.isEmpty()) continue
            if (src.size == cap && src.distinct().size == 1) continue
            val c = src.last()
            val run = src.reversed().takeWhile { it == c }.size
            for (b in state.indices) {
                if (a == b) continue
                val dst = state[b]
                val room = cap - dst.size
                if (room == 0 || (dst.isNotEmpty() && dst.last() != c)) continue
                if (dst.isEmpty() && run == src.size) continue          // moving a whole one-colour pile to an empty jar is pointless
                moves += (a to b) to (if (dst.isNotEmpty()) 2 else 1)
            }
        }
        // Push lower-priority moves first so better moves are explored first
        for ((mv, _) in moves.sortedBy { it.second }) {
            val (a, b) = mv
            val c = state[a].last()
            val n = minOf(cap - state[b].size, state[a].reversed().takeWhile { it == c }.size)
            val next = state.mapIndexed { i, j -> when (i) { a -> j.dropLast(n); b -> j + List(n) { c }; else -> j } }
            val f = first ?: mv
            if (done(next)) return f
            if (seen.add(key(next))) stack.addLast(Frame(next, f))
        }
    }
    return null
}

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
        if (sortedJars == 0 && solveFirstMove(jars, spec.capacity, spec.colours) != null) {
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
    var hint by remember(level, round) { mutableStateOf<Pair<Int, Int>?>(null) }
    var hintMessage by remember(level, round) { mutableStateOf<String?>(null) }

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
        hint = null
        hintMessage = null
        val sel = selected
        when {
            sel == null -> if (jars[i].isNotEmpty() && !isComplete(jars[i])) { selected = i; SoundEffectsHelper.playClick() }
            sel == i -> selected = null
            else -> { pour(sel, i); selected = null }
        }
    }

    val shake = rememberShake(shakeCount)

    fun symbolFor(colourIndex: Int): String =
        sortSymbols.getOrElse(sortColourIds.indexOf(palette[colourIndex].id)) { "●" }

    fun showHint() {
        if (finished) return
        selected = null
        scope.launch {
            val snapshot = jars
            val move = withContext(Dispatchers.Default) { solveFirstMove(snapshot, spec.capacity, spec.colours) }
            if (snapshot != jars) return@launch
            if (move == null) {
                hint = null
                hintMessage = "Oh no, stuck! Tap Undo a few times and try another way."
            } else {
                hint = move
                hintMessage = "Pour from the bouncing jar into the one with the arrow ⬇"
                delay(3000)
                if (hint == move) { hint = null; hintMessage = null }
            }
        }
    }

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
        Spacer(Modifier.height(8.dp))

        // Jars: up to 2 rows, sized to fit the screen so every jar can be reached
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val n = jars.size
            val rows = if (n <= 5) 1 else 2
            val perRow = (n + rows - 1) / rows
            val gap = 10.dp
            val jarW = minOf(66.dp, (maxWidth - gap * (perRow - 1)) / perRow)
            val rowH = (maxHeight - 18.dp * (rows - 1)) / rows
            val jarH = minOf(rowH - 50.dp, 52.dp * spec.capacity + 18.dp)
            val scoop = ((jarH - 18.dp) / spec.capacity).coerceAtLeast(22.dp)
            Column(verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                jars.indices.chunked(perRow).forEach { rowIdx ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        rowIdx.forEach { i ->
                            val jar = jars[i]
                            val lifted = selected == i
                            val hinted = hint?.let { it.first == i || it.second == i } == true
                            val lift by animateFloatAsState(if (lifted || hint?.first == i) -18f else 0f, spring(dampingRatio = 0.5f), label = "lift")
                            val complete = isComplete(jar)
                            val shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 26.dp, bottomEnd = 26.dp)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                                    if (complete) Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(24.dp))
                                    else if (hint?.second == i) Text("⬇", fontSize = 20.sp, color = Color(0xFFFF9F1C), fontWeight = FontWeight.ExtraBold)
                                }
                                Column(
                                    Modifier
                                        .graphicsLayer {
                                            translationY = lift * density
                                            translationX = if (shakeJar == i) shake * density else 0f
                                        }
                                        .width(jarW)
                                        .height(scoop * spec.capacity + 18.dp)
                                        .clip(shape)
                                        .background(Color.White.copy(alpha = 0.85f))
                                        .border(
                                            4.dp,
                                            when {
                                                lifted -> Color(0xFFFFC93C)
                                                hinted -> Color(0xFFFF9F1C)
                                                complete -> palette[jar.first()].color
                                                else -> Color(0xFFB8C7D4)
                                            },
                                            shape
                                        )
                                        .clickable { tapJar(i) }
                                        .padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
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
                                                    if (isBottom) RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp, topStart = 4.dp, topEnd = 4.dp)
                                                    else RoundedCornerShape(4.dp)
                                                )
                                                .background(palette[colourIndex].color),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(symbolFor(colourIndex), fontSize = (scoop.value * 0.42f).sp, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (complete) palette[jar.first()].name(lang) else " ",
                                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = GameInk, maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        hintMessage?.let {
            Text(it, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB55F00))
            Spacer(Modifier.height(6.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (history.isEmpty() || finished) Color(0xFFE3E7EE) else Color.White)
                    .clickable(enabled = history.isNotEmpty() && !finished) {
                        jars = history.last()
                        history = history.dropLast(1)
                        selected = null
                        hint = null
                        hintMessage = null
                    }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .testTag("sort_undo"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = GameInk)
                Text("Undo", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = GameInk)
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (finished) Color(0xFFE3E7EE) else Color(0xFFFFF0C2))
                    .clickable(enabled = !finished) { showHint() }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .testTag("sort_hint"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("💡", fontSize = 16.sp)
                Text("Hint", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = GameInk)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
