package com.example.ui.classroom

import com.example.platform.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KannadaLetter
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.components.ScrapbookInk
import com.example.ui.components.ScrapbookInkSoft
import com.example.ui.progress.GameProgress
import com.example.ui.progress.GameRegistry
import com.example.ui.progress.LevelResult
import com.example.ui.screens.BackHeader
import com.example.ui.screens.TraceExercise
import com.example.ui.screens.WhiteCard
import kotlinx.coroutines.launch

/** The task the child has just gone off to do (checked when they come back to the homework list). */
internal object HomeworkSession {
    data class Pending(
        val code: String,
        val hw: Homework,
        val index: Int,
        val startedAt: Long,
        val levelSeen: LevelResult?,
        val before: TaskResult
    )

    var pending: Pending? = null
}

/** Games stay locked until every video and tracing task of that homework is done (if the teacher chose so). */
internal fun isLocked(hw: Homework, results: Map<Int, TaskResult>, index: Int): Boolean {
    if (!hw.lockGames || !hw.tasks[index].isGame) return false
    return hw.tasks.withIndex().any { (i, t) -> !t.isGame && results[i]?.done != true }
}

/** The route that plays one task. */
internal fun routeFor(task: HwTask): String = when {
    task.isVideo -> "video_player/${Uri.encode(task.ref)}"
    task.isTrace -> CLASS_TRACE
    else -> {
        val g = GameRegistry.byId(task.ref.toIntOrNull() ?: -1)
        if (g != null && g.hasLevels) "game/${g.id}?level=${GameProgress.nextLevel(g)}" else "game/${task.ref}"
    }
}

/** Works out whether the video / game the child just came back from counts as done, and saves it. */
private suspend fun finishPending(): Pair<String, TaskResult>? {
    val p = HomeworkSession.pending ?: return null
    HomeworkSession.pending = null
    val task = p.hw.tasks.getOrNull(p.index) ?: return null
    if (task.isTrace) return null // the tracing screen saves its own result
    val secs = ((currentTimeMillis() - p.startedAt) / 1000).toInt().coerceIn(0, 3600)
    var done = false
    var stars = 0
    if (task.isVideo) {
        done = secs >= 30
    } else {
        val lr = GameProgress.lastLevel.value
        if (lr != null && lr !== p.levelSeen && lr.gameId.toString() == task.ref) {
            done = true
            stars = lr.stars
        } else {
            done = secs >= task.minutes * 60
        }
    }
    val old = p.before
    val new = TaskResult(
        done = old.done || done,
        accuracy = old.accuracy,
        stars = maxOf(old.stars, stars),
        tries = old.tries + 1,
        seconds = old.seconds + secs,
        at = currentTimeMillis()
    )
    Classroom.saveResult(p.code, p.hw.id, p.index, new)
    return p.hw.id to new
}

/** The child's homework: the newest homework open at the top with one big "Start" button, older ones below. */
@Composable
fun StudentHomeworkScreen(onBack: () -> Unit, onOpenRoute: (String) -> Unit, onOpenClassroom: () -> Unit) {
    val scope = rememberCoroutineScope()
    val membership by Classroom.membership.collectAsState()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var list by remember { mutableStateOf(listOf<Homework>()) }
    var results by remember { mutableStateOf(mapOf<String, Map<Int, TaskResult>>()) }
    var openId by remember { mutableStateOf<String?>(null) }
    var celebrate by remember { mutableStateOf(false) }

    suspend fun load() {
        loading = true
        error = null
        try {
            val justDone = runCatching { finishPending() }.getOrNull()
            val m = Classroom.loadMembership()
            if (m != null && m.approved) {
                val hws = Classroom.homework(m.code).take(6)
                val res = hws.associate { h -> h.id to runCatching { Classroom.myResults(m.code, h.id) }.getOrDefault(emptyMap()) }
                list = hws
                results = res
                if (openId == null) openId = hws.firstOrNull()?.id
                hws.firstOrNull()?.let { h -> Classroom.setTodo(h.tasks.indices.count { res[h.id]?.get(it)?.done != true }) }
                if (justDone != null) {
                    val h = hws.firstOrNull { it.id == justDone.first }
                    if (h != null && h.tasks.indices.all { res[h.id]?.get(it)?.done == true }) {
                        celebrate = true
                        SoundEffectsHelper.playApplause()
                    }
                }
            }
        } catch (e: Exception) {
            error = friendly(e)
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    fun start(m: Membership, hw: Homework, index: Int) {
        HomeworkSession.pending = HomeworkSession.Pending(
            code = m.code, hw = hw, index = index, startedAt = currentTimeMillis(),
            levelSeen = GameProgress.lastLevel.value,
            before = results[hw.id]?.get(index) ?: TaskResult()
        )
        onOpenRoute(routeFor(hw.tasks[index]))
    }

    Box(Modifier.fillMaxSize()) {
        ClassPage {
            val m = membership
            BackHeader("Homework", m?.let { "${it.childName} · ${it.className}" }, onBack)
            when {
                loading && list.isEmpty() -> Muted("Loading…")
                m == null -> WhiteCard {
                    Heading("Not in a class yet")
                    Muted("Ask a grown-up to join your teacher's class with the class code.")
                    BigButton("Join a class", ClassViolet) { onOpenClassroom() }
                }
                !m.approved -> WhiteCard {
                    Heading("Waiting for the teacher")
                    Muted("${m.teacherName.ifBlank { "The teacher" }} needs to let ${m.childName} into ${m.className}. Check again later.")
                    SoftButton("Check again") { scope.launch { load() } }
                }
                list.isEmpty() -> WhiteCard {
                    Heading("No homework yet")
                    Muted("When the teacher gives homework, it shows here.")
                    SoftButton("Check again") { scope.launch { load() } }
                }
                else -> list.forEachIndexed { n, hw ->
                    val res = results[hw.id].orEmpty()
                    if (hw.id == openId) {
                        HomeworkCard(m, hw, res, newest = n == 0, onStart = { i -> start(m, hw, i) })
                    } else {
                        val done = hw.tasks.indices.count { res[it]?.done == true }
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)
                                .clickable { openId = hw.id }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(hw.title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                                Muted(Classroom.dueText(hw.dueAt))
                            }
                            Badge(
                                if (done == hw.tasks.size) "All done" else "$done of ${hw.tasks.size}",
                                if (done == hw.tasks.size) ClassGreenSoft else ClassAmberSoft,
                                if (done == hw.tasks.size) ClassGreenInk else ClassAmberInk
                            )
                        }
                    }
                }
            }
            ErrorLine(error)
            Spacer(Modifier.height(20.dp))
        }

        if (celebrate) {
            Box(
                Modifier.fillMaxSize().background(Color(0xCC0B3B66)).clickable { celebrate = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier.padding(24.dp).widthIn(max = 380.dp).fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(28.dp)).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🏆", fontSize = 64.sp)
                    Text("Super, ${membership?.childName ?: "star"}!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk, textAlign = TextAlign.Center)
                    Text("Homework done", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
                    Row { repeat(4) { Icon(Icons.Filled.Star, null, tint = ClassAmber, modifier = Modifier.size(34.dp)) } }
                    Badge("Sent to ${membership?.teacherName?.ifBlank { "your teacher" } ?: "your teacher"}", ClassVioletSoft, ClassVioletInk)
                    BigButton("Yay!") { celebrate = false }
                }
            }
        }
    }
}

@Composable
private fun HomeworkCard(m: Membership, hw: Homework, res: Map<Int, TaskResult>, newest: Boolean, onStart: (Int) -> Unit) {
    val doneCount = hw.tasks.indices.count { res[it]?.done == true }
    val next = hw.tasks.indices.firstOrNull { res[it]?.done != true && !isLocked(hw, res, it) }

    Column(
        Modifier.fillMaxWidth().background(ClassVioletSoft, RoundedCornerShape(22.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(if (newest) "Today's homework" else hw.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = ClassVioletInk)
        Text(
            listOf(m.teacherName.ifBlank { null }?.let { "From $it" }, Classroom.dueText(hw.dueAt).ifBlank { null }).filterNotNull().joinToString(" · "),
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ClassViolet
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            hw.tasks.indices.forEach { i ->
                Icon(Icons.Filled.Star, null, tint = if (res[i]?.done == true) ClassAmber else Color(0xFFCECBF6), modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("$doneCount of ${hw.tasks.size} done", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ClassViolet)
        }
        if (hw.note.isNotBlank()) {
            Text(
                "“${hw.note}”",
                Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(10.dp),
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ScrapbookInk
            )
        }
    }

    hw.tasks.forEachIndexed { i, t ->
        val r = res[i]
        val done = r?.done == true
        val locked = !done && isLocked(hw, res, i)
        val isNext = i == next
        val (bg, ink) = taskColors(t)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(if (isNext) ClassTealSoft else Color.White)
                .border(if (isNext) 2.dp else 0.dp, if (isNext) ClassTeal else Color.Transparent, RoundedCornerShape(18.dp))
                .clickable(enabled = !locked) { onStart(i) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(48.dp).background(if (isNext) Color.White else bg, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text(taskGlyph(t), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = ink)
            }
            Column(Modifier.weight(1f)) {
                Text(t.label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (locked) ScrapbookInkSoft else ScrapbookInk)
                Text(
                    when {
                        locked -> "Unlocks after the tracing"
                        isNext -> "Up next"
                        r != null && r.done && r.accuracy >= 0 -> "Done · ${r.accuracy}%"
                        else -> taskKind(t)
                    },
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isNext) ClassTeal else ScrapbookInkSoft
                )
            }
            when {
                done -> Icon(Icons.Filled.CheckCircle, "Done", tint = ClassTeal, modifier = Modifier.size(28.dp))
                locked -> Icon(Icons.Filled.Lock, "Locked", tint = Color(0xFFB4B2A9), modifier = Modifier.size(24.dp))
                else -> Icon(Icons.Filled.PlayArrow, "Start", tint = if (isNext) ClassTeal else ScrapbookInkSoft, modifier = Modifier.size(28.dp))
            }
        }
    }

    if (next != null) {
        BigButton("▶  Start ${hw.tasks[next].label.substringAfter(": ")}") { onStart(next) }
    } else if (doneCount == hw.tasks.size) {
        Text("All done! Well done ⭐", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ClassTeal)
    }
}

/** Traces one homework letter, then saves how well it went. */
@Composable
fun HomeworkTraceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val p = remember { HomeworkSession.pending }
    val task = p?.let { it.hw.tasks.getOrNull(it.index) }
    if (p == null || task == null || !task.isTrace) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val checks = remember { mutableListOf<Pair<Float, Boolean>>() }
    var saved by remember { mutableStateOf(false) }

    fun save(done: Boolean) {
        if (saved) return
        saved = true
        HomeworkSession.pending = null
        val secs = ((currentTimeMillis() - p.startedAt) / 1000).toInt().coerceIn(0, 3600)
        val passed = checks.filter { it.second }.map { it.first }
        val acc = if (passed.isNotEmpty()) (passed.average() * 100).toInt()
            else if (checks.isNotEmpty()) (checks.map { it.first }.average() * 100).toInt() else -1
        val old = p.before
        val new = TaskResult(
            done = old.done || done,
            accuracy = if (done || old.accuracy < 0) acc else old.accuracy,
            stars = old.stars,
            tries = old.tries + checks.size,
            seconds = old.seconds + secs,
            at = currentTimeMillis()
        )
        if (checks.isNotEmpty() || done) {
            classroomScope.launch { runCatching { Classroom.saveResult(p.code, p.hw.id, p.index, new) } }
        }
    }

    ClassPage {
        BackHeader("Trace ${task.ref}", "Homework · trace it ${task.reps} times", { save(false); onBack() })
        TraceExercise(
            letter = KannadaLetter(id = 9700 + p.index, character = task.ref, transliteration = "", isVowel = false),
            onSpeakText = { NativeAudioPlayer.playPhonics(context, it) },
            onComplete = { save(true); onBack() },
            requiredTraces = task.reps,
            onChecked = { acc, ok -> checks += acc to ok }
        )
    }
}

/** The small button on Home: "Homework · 2 to do" for children in a class, "Class" for everyone else. */
@Composable
fun HomeworkHomeButton(modifier: Modifier = Modifier, onOpenHomework: () -> Unit, onOpenClassroom: () -> Unit) {
    val membership by Classroom.membership.collectAsState()
    val todo by Classroom.todo.collectAsState()
    LaunchedEffect(Unit) {
        runCatching {
            val m = Classroom.loadMembership()
            if (m != null && m.approved) {
                val h = Classroom.homework(m.code).firstOrNull()
                if (h != null) {
                    val r = Classroom.myResults(m.code, h.id)
                    Classroom.setTodo(h.tasks.indices.count { r[it]?.done != true })
                } else Classroom.setTodo(0)
            }
        }
    }
    val m = membership
    val inClass = m != null && m.approved
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (inClass && todo > 0) ClassViolet else Color.White)
            .border(1.dp, ClassLine, RoundedCornerShape(999.dp))
            .clickable { if (m != null) onOpenHomework() else onOpenClassroom() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("📒", fontSize = 18.sp)
        Text(
            when {
                m == null -> "Class"
                !m.approved -> "Homework · waiting"
                todo > 0 -> "Homework · $todo to do"
                else -> "Homework ✓"
            },
            fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
            color = if (inClass && todo > 0) Color.White else ScrapbookInk
        )
        if (inClass && todo > 0) Box(Modifier.size(8.dp).background(Color(0xFFFAC775), CircleShape))
    }
}
