package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.data.ActivityTracker
import com.example.ui.progress.GameProgress
import com.example.ui.progress.LevelResult
import com.example.ui.progress.LevelTally
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/*
 * Word Search: find the picture words hidden in a grid of letters by sliding a finger across them.
 * 6 levels: the grid grows (4×4 → 6×6), more words hide, and from level 5 words also run diagonally.
 * Topics: the words from the Fun Videos word charts (Fruits, Vegetables, Body Parts, Family, Mix) or
 * the general picture words. A found video word can be watched (▶) right away.
 */

private data class SearchLevel(val size: Int, val words: Int, val vertical: Boolean, val diagonal: Boolean, val label: String)

private val searchLevels = listOf(
    SearchLevel(4, 2, false, false, "Words go across →"),
    SearchLevel(4, 3, true, false, "Across → and down ↓"),
    SearchLevel(5, 3, true, false, "Bigger grid"),
    SearchLevel(5, 4, true, false, "4 hidden words"),
    SearchLevel(6, 4, true, true, "Diagonal words too ↘"),
    SearchLevel(6, 5, true, true, "Word Search champion")
)

private data class Cell(val r: Int, val c: Int)
private data class Placed(val word: PictureWord, val cells: List<Cell>, val color: Color)
private data class Puzzle(val grid: List<List<String>>, val placed: List<Placed>)

internal val fillerSyllables = listOf(
    "ಅ", "ಆ", "ಇ", "ಉ", "ಎ", "ಒ", "ಕ", "ಗ", "ಚ", "ಜ", "ಟ", "ಡ", "ತ", "ದ", "ನ", "ಪ", "ಬ", "ಮ", "ಯ", "ರ", "ಲ", "ವ", "ಸ", "ಹ",
    "ಕಾ", "ಮಾ", "ನಿ", "ರು", "ಲೆ", "ಬೆ", "ಗೆ", "ಹು", "ತೆ"
)

/** Filler letters for a topic: the topic's own letter blocks (so wrong answers look believable) plus a few common ones. */
private fun fillerFor(words: List<PictureWord>): List<String> {
    val own = words.flatMap { it.syllables }.distinct()
    return if (own.size >= 12) own + fillerSyllables.shuffled().take(6) else fillerSyllables + own
}

private fun makePuzzle(spec: SearchLevel, words: List<PictureWord>, filler: List<String>): Puzzle {
    val dirs = buildList {
        add(0 to 1)
        if (spec.vertical) add(1 to 0)
        if (spec.diagonal) add(1 to 1)
    }
    repeat(60) {
        val grid = Array(spec.size) { arrayOfNulls<String>(spec.size) }
        val chosen = words.filter { it.syllables.size in 2..spec.size }.shuffled()
        val placed = mutableListOf<Placed>()
        for (w in chosen) {
            if (placed.size == spec.words) break
            // Skip a word that is part of another chosen word (ಮಗ in ಮಗಳು) or the same word
            if (placed.any { it.word.text.contains(w.text) || w.text.contains(it.word.text) }) continue
            var ok = false
            for (attempt in 0 until 80) {
                val (dr, dc) = dirs.random()
                val len = w.syllables.size
                val r0 = Random.nextInt(0, spec.size - dr * (len - 1))
                val c0 = Random.nextInt(0, spec.size - dc * (len - 1))
                val cells = (0 until len).map { Cell(r0 + dr * it, c0 + dc * it) }
                if (cells.withIndex().all { (i, cell) -> grid[cell.r][cell.c].let { it == null || it == w.syllables[i] } }) {
                    cells.forEachIndexed { i, cell -> grid[cell.r][cell.c] = w.syllables[i] }
                    placed += Placed(w, cells, CandyColors[placed.size % CandyColors.size])
                    ok = true
                    break
                }
            }
            if (!ok) continue
        }
        if (placed.size == spec.words) {
            val filled = grid.map { row -> row.map { it ?: filler.random() } }
            return Puzzle(filled, placed)
        }
    }
    // Fallback: simple rows
    val fallback = words.filter { it.syllables.size == 2 }.ifEmpty { pictureWords.filter { it.syllables.size == 2 } }
        .distinctBy { it.text }.shuffled().take(spec.words)
    val grid = Array(spec.size) { r -> Array(spec.size) { filler.random() } }
    val placed = fallback.mapIndexed { i, w ->
        val cells = w.syllables.indices.map { Cell(i, it) }
        cells.forEachIndexed { k, cell -> grid[cell.r][cell.c] = w.syllables[k] }
        Placed(w, cells, CandyColors[i % CandyColors.size])
    }
    return Puzzle(grid.map { it.toList() }, placed)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordSearchScreen(firstLevel: Int = 1, topic: String? = null, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { getSharedPreferences("AppSettings") }
    // The chosen topic: from the video folder that opened the game, else the last one played
    var topicId by remember {
        mutableStateOf(
            topic?.takeIf { t -> wordTopics.any { it.id == t } }
                ?: settings.getString("word_search_topic", null)?.takeIf { t -> wordTopics.any { it.id == t } }
                ?: "mix"
        )
    }
    var level by remember { mutableIntStateOf((firstLevel - 1).coerceIn(0, searchLevels.size - 1)) }
    val spec = searchLevels[level]
    var round by remember { mutableIntStateOf(0) }
    val topicWords = remember(topicId) { wordsForTopic(topicId) }
    val filler = remember(topicId) { fillerFor(topicWords) }
    val puzzle = remember(level, round, topicId) { makePuzzle(spec, topicWords, filler) }
    var watching by remember { mutableStateOf<PictureWord?>(null) }
    var found by remember(level, round, topicId) { mutableStateOf(listOf<Placed>()) }
    var selection by remember(level, round, topicId) { mutableStateOf(listOf<Cell>()) }
    val tally = remember { LevelTally() }
    var levelResult by remember { mutableStateOf<LevelResult?>(null) }
    var showConfetti by remember { mutableStateOf(false) }
    var gridPx by remember { mutableStateOf(1f) }
    var justFound by remember { mutableStateOf<Placed?>(null) }

    fun startLevel(newLevel: Int) {
        tally.reset()
        levelResult = null
        showConfetti = false
        justFound = null
        level = newLevel
        round += 1
    }

    fun cellAt(p: Offset): Cell? {
        val size = gridPx / spec.size
        val r = (p.y / size).toInt()
        val c = (p.x / size).toInt()
        return if (r in 0 until spec.size && c in 0 until spec.size) Cell(r, c) else null
    }

    fun line(a: Cell, b: Cell): List<Cell> {
        val dr = b.r - a.r
        val dc = b.c - a.c
        val straight = dr == 0 || dc == 0 || (spec.diagonal && abs(dr) == abs(dc))
        if (!straight) return listOf(a)
        val n = max(abs(dr), abs(dc))
        val sr = if (dr == 0) 0 else dr / abs(dr)
        val sc = if (dc == 0) 0 else dc / abs(dc)
        return (0..n).map { Cell(a.r + sr * it, a.c + sc * it) }
    }

    fun finishSelection() {
        val sel = selection
        selection = emptyList()
        if (sel.size < 2) return
        val match = puzzle.placed.firstOrNull { p -> p !in found && (p.cells == sel || p.cells == sel.reversed()) }
        if (match != null) {
            found = found + match
            justFound = match
            tally.right()
            ActivityTracker.recordAnswer(match.word.text, true)
            SoundEffectsHelper.playSuccess(context)
            NativeAudioPlayer.playPhonics(context, match.word.text)
            if (found.size == puzzle.placed.size) {
                scope.launch {
                    delay(900)
                    showConfetti = true
                    SoundEffectsHelper.playGameCompletion(context)
                    val result = GameProgress.recordLevel(context, 9, level + 1, tally.mistakes, tally.streaks)
                    delay(1200)
                    showConfetti = false
                    levelResult = result
                }
            }
        } else {
            tally.wrong()
            SoundEffectsHelper.playOops(context)
        }
    }

    LevelGameScaffold(
        title = "Word Search",
        subtitle = "Level ${level + 1}/${searchLevels.size} · ${spec.label}",
        headerColor = Color(0xFF6C63FF),
        background = Color(0xFFF0EEFF),
        rounds = puzzle.placed.size,
        roundsDone = found.size,
        showConfetti = showConfetti,
        levelResult = levelResult,
        hasNextLevel = level < searchLevels.size - 1,
        onNextLevel = { startLevel(level + 1) },
        onPlayAgain = { startLevel(level) },
        onBack = onBack
    ) {
        // Topic: the words from a Fun Videos folder, all of them (Mix) or the general picture words
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            wordTopics.forEach { t ->
                val on = t.id == topicId
                Text(
                    "${t.icon} ${t.label}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (on) Color.White else GameInk,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (on) Color(0xFF6C63FF) else Color.White)
                        .clickable {
                            if (!on) {
                                topicId = t.id
                                settings.edit().putString("word_search_topic", t.id).apply()
                                justFound = null
                                tally.reset()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("ws_topic_${t.id}")
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        // Words to find (tap to hear; a found video word can be watched)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            puzzle.placed.forEach { p ->
                val isFound = p in found
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isFound) p.color else Color.White)
                        .border(2.dp, p.color, RoundedCornerShape(20.dp))
                        .clickable {
                            if (isFound && p.word.video != null) watching = p.word
                            else NativeAudioPlayer.playPhonics(context, p.word.text)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(p.word.picture, fontSize = 22.sp)
                    Text(
                        if (isFound) p.word.text else p.word.english,
                        fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = GameInk
                    )
                    if (isFound) Text(if (p.word.video != null) "▶" else "✓", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = GameInk)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(if (found.any { it.word.video != null }) "Slide across the letters · tap a found word ▶ to watch" else "Slide your finger across the letters", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GameInk.copy(alpha = 0.6f))
        Spacer(Modifier.height(12.dp))

        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight, 560.dp)
            val cellDp = side / spec.size
            Box(
                Modifier
                    .size(side)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .pointerInput(level, round, topicId) {
                        gridPx = size.width.toFloat()
                        var start: Cell? = null
                        detectDragGestures(
                            onDragStart = { p ->
                                start = cellAt(p)
                                selection = listOfNotNull(start)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val s = start ?: return@detectDragGestures
                                val c = cellAt(change.position) ?: return@detectDragGestures
                                selection = line(s, c)
                            },
                            onDragEnd = { finishSelection() },
                            onDragCancel = { selection = emptyList() }
                        )
                    }
                    .pointerInput(level, round, topicId, "tap") {
                        detectTapGestures { p ->
                            cellAt(p)?.let { c -> NativeAudioPlayer.playPhonics(context, puzzle.grid[c.r][c.c]) }
                        }
                    }
                    .testTag("word_search_grid")
            ) {
                Column {
                    puzzle.grid.forEachIndexed { r, row ->
                        Row {
                            row.forEachIndexed { c, syl ->
                                val cell = Cell(r, c)
                                val owner = found.lastOrNull { cell in it.cells }
                                val selected = cell in selection
                                val pop by animateFloatAsState(if (owner != null && owner == justFound) 1.12f else 1f, spring(dampingRatio = 0.3f), label = "cell")
                                Box(
                                    Modifier
                                        .size(cellDp)
                                        .padding(3.dp)
                                        .graphicsLayer { scaleX = pop; scaleY = pop }
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            when {
                                                selected -> Color(0xFFFFC93C)
                                                owner != null -> owner.color
                                                else -> CandyColors[(r * 3 + c) % CandyColors.size].copy(alpha = 0.12f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        syl,
                                        fontSize = (cellDp.value * 0.38f).sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GameInk
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    watching?.let { w -> WordVideoDialog(w, onClose = { watching = null }) }
}

/** Plays a found word's video from the Fun Videos word charts. */
@Composable
private fun WordVideoDialog(word: PictureWord, onClose: () -> Unit) {
    val url = word.video ?: return
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val player = rememberVideoPlayer(url = url, loop = false, autoPlay = true)
        val playing by player.isPlaying
        val ended by player.isEnded
        Column(
            Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black)
                    .clickable { if (playing) player.pause() else player.play() },
                contentAlignment = Alignment.Center
            ) {
                VideoSurface(controller = player, showNativeControls = false, modifier = Modifier.fillMaxSize())
                if (!playing) Text(if (ended) "↻" else "▶", fontSize = 48.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Close ✕",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF6C63FF))
                    .clickable { onClose() }
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .testTag("ws_video_close")
            )
        }
    }
}
