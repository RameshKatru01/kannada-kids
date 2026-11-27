package com.example.ui.classroom

import com.example.platform.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LetterData
import com.example.ui.components.ScrapbookInk
import com.example.ui.components.ScrapbookInkSoft
import com.example.ui.data.LearnerName
import com.example.ui.progress.GameRegistry
import com.example.ui.screens.BackHeader
import com.example.ui.screens.ParentGate
import com.example.ui.screens.WhiteCard
import kotlinx.coroutines.launch

/** Video folders a teacher can set as homework. */
internal val homeworkVideoFolders = listOf(
    "Vowels", "Consonants", "Birds", "Animals", "Colours", "Fruits", "Vegetables", "Body Parts",
    "Family", "Numbers", "Letter Quiz", "Picture Quiz", "Number Quiz", "How Many?", "Play Time"
)

/** Games that work well as homework (letter games first). */
internal val homeworkGameIds = listOf(11, 19, 1, 2, 28, 12, 25, 27, 24, 21, 4)

private fun shareOnWhatsApp(uri: androidx.compose.ui.platform.UriHandler, text: String) {
    runCatching { uri.openUri("https://wa.me/?text=" + Uri.encode(text)) }
}

// ---------------------------------------------------------------------------------------------
// Classroom hub: join a class (families) or make one (teachers). Behind the grown-up check.
// ---------------------------------------------------------------------------------------------

@Composable
fun ClassroomHubScreen(onBack: () -> Unit, onOpenHomework: () -> Unit, onOpenClass: (String) -> Unit) {
    var passed by remember { mutableStateOf(false) }
    if (!passed) {
        ParentGate(onPass = { passed = true }, onCancel = onBack, purpose = "Answer to open Classroom")
        return
    }
    val scope = rememberCoroutineScope()
    val membership by Classroom.membership.collectAsState()
    var code by remember { mutableStateOf("") }
    var childName by remember { mutableStateOf(LearnerName.name.value) }
    var joinError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var classes by remember { mutableStateOf<List<ClassInfo>?>(null) }
    var newClass by remember { mutableStateOf("") }
    var teachError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { Classroom.loadMembership() }
        classes = runCatching { Classroom.myClasses() }.getOrElse { teachError = friendly(it); emptyList() }
    }

    ClassPage {
        BackHeader("Classroom", "Homework from school, inside the app", onBack)

        WhiteCard {
            Heading("Your child's class")
            val m = membership
            if (m != null) {
                Text("${m.childName} · ${m.className}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                if (m.approved) {
                    Badge("Joined", ClassGreenSoft, ClassGreenInk)
                    BigButton("Open homework") { onOpenHomework() }
                } else {
                    Badge("Waiting for ${m.teacherName.ifBlank { "the teacher" }} to approve", ClassAmberSoft, ClassAmberInk)
                    Muted("The teacher sees your request in the app and lets ${m.childName} in.")
                }
                SoftButton("Leave this class", color = ClassRed) {
                    scope.launch { runCatching { Classroom.leave() }.onFailure { joinError = friendly(it) } }
                }
            } else {
                Muted("Got a class code from the teacher? Type it here.")
                OutlinedTextField(
                    value = code, onValueChange = { code = it.uppercase().take(10); joinError = null },
                    label = { Text("Class code, like KAN-4821") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = childName, onValueChange = { childName = it.take(20); joinError = null },
                    label = { Text("Child's first name") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Muted("Only the first name is shared with the teacher.")
                BigButton(if (busy) "Joining…" else "Join class", ClassViolet) {
                    if (busy) return@BigButton
                    busy = true
                    scope.launch {
                        try { Classroom.join(code, childName) } catch (e: Exception) { joinError = friendly(e) }
                        busy = false
                    }
                }
                ErrorLine(joinError)
            }
        }

        WhiteCard {
            Heading("For teachers")
            Muted("Make a class, share the code with parents, give homework and see how each child does.")
            val list = classes
            when {
                list == null -> Muted("Loading…")
                list.isEmpty() -> {}
                else -> list.forEach { c ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ClassVioletSoft)
                            .clickable { onOpenClass(c.code) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(c.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ClassVioletInk)
                            Text("Code ${c.code}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ClassViolet)
                        }
                        Text("Open ›", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ClassViolet)
                    }
                }
            }
            OutlinedTextField(
                value = newClass, onValueChange = { newClass = it.take(30); teachError = null },
                label = { Text("New class name, like UKG – B") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            SoftButton("Create class") {
                scope.launch {
                    try {
                        val c = Classroom.createClass(newClass)
                        newClass = ""
                        classes = (classes.orEmpty()) + c
                        onOpenClass(c.code)
                    } catch (e: Exception) {
                        teachError = friendly(e)
                    }
                }
            }
            ErrorLine(teachError)
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// One class: code to share, join requests, homework list
// ---------------------------------------------------------------------------------------------

@Composable
fun TeacherClassScreen(code: String, onBack: () -> Unit, onNewHomework: () -> Unit, onOpenHomework: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    var info by remember { mutableStateOf<ClassInfo?>(null) }
    var students by remember { mutableStateOf(listOf<Student>()) }
    var homework by remember { mutableStateOf(listOf<Homework>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    suspend fun load() {
        loading = true
        try {
            info = Classroom.findClass(code)
            students = Classroom.students(code)
            homework = Classroom.homework(code)
            error = null
        } catch (e: Exception) {
            error = friendly(e)
        }
        loading = false
    }
    LaunchedEffect(code) { load() }

    val waiting = students.filter { !it.approved }
    val joined = students.filter { it.approved }

    ClassPage {
        BackHeader(info?.name ?: "Class", "${joined.size} children", onBack)

        WhiteCard {
            Muted("Class code")
            Text(code, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = ClassViolet)
            Muted("Parents open Fun Learn ಕನ್ನಡ › Class, and type this code.")
            SoftButton("Share code on WhatsApp") {
                shareOnWhatsApp(
                    uri,
                    "Our class ${info?.name ?: ""} is using Fun Learn ಕನ್ನಡ for Kannada homework. " +
                        "Install the app, tap “Class” on the home screen and enter the code $code. " +
                        com.example.ui.screens.STORE_URL
                )
            }
        }

        if (waiting.isNotEmpty()) {
            WhiteCard {
                Heading("Waiting to join (${waiting.size})")
                waiting.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(s.childName, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                        SoftButton("Let in", color = ClassTeal) {
                            scope.launch { runCatching { Classroom.approve(code, s.uid) }.onFailure { error = friendly(it) }; load() }
                        }
                        SoftButton("Remove", color = ClassRed) {
                            scope.launch { runCatching { Classroom.removeStudent(code, s.uid) }.onFailure { error = friendly(it) }; load() }
                        }
                    }
                }
            }
        }

        BigButton("+  New homework", ClassViolet) { onNewHomework() }

        Heading("Homework")
        if (!loading && homework.isEmpty()) Muted("No homework yet. Tap “New homework” to give the first one.")
        homework.forEach { h ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)
                    .clickable { onOpenHomework(h.id) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(h.title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                    Muted("${h.tasks.size} tasks · ${Classroom.dueText(h.dueAt)}")
                }
                Text("Progress ›", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ClassViolet)
            }
        }

        if (joined.isNotEmpty()) {
            Heading("Children")
            WhiteCard {
                joined.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.childName, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ScrapbookInk)
                        Text(
                            "Remove", Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                                scope.launch { runCatching { Classroom.removeStudent(code, s.uid) }.onFailure { error = friendly(it) }; load() }
                            }.padding(6.dp),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft
                        )
                    }
                }
            }
        }
        ErrorLine(error)
        Spacer(Modifier.height(20.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Progress of one homework: numbers, child × task grid, who needs help
// ---------------------------------------------------------------------------------------------

private data class Cell(val text: String, val bg: Color, val fg: Color)

private fun cellFor(t: HwTask, r: TaskResult?): Cell = when {
    r == null || (!r.done && r.tries == 0) -> Cell("—", Color.Transparent, Color(0xFFB4B2A9))
    !r.done -> Cell("Tried", ClassAmberSoft, ClassAmberInk)
    t.isTrace && r.accuracy >= 0 -> if (r.accuracy < 60) Cell("${r.accuracy}%", ClassAmberSoft, ClassAmberInk) else Cell("${r.accuracy}%", ClassGreenSoft, ClassGreenInk)
    t.isGame && r.stars > 0 -> Cell("★ ${r.stars}", ClassAmberSoft, ClassAmberInk)
    else -> Cell("✓", ClassGreenSoft, ClassGreenInk)
}

@Composable
fun HomeworkProgressScreen(code: String, hwId: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    var hw by remember { mutableStateOf<Homework?>(null) }
    var students by remember { mutableStateOf(listOf<Student>()) }
    var results by remember { mutableStateOf(mapOf<String, Map<Int, TaskResult>>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    suspend fun load() {
        try {
            hw = Classroom.homework(code).firstOrNull { it.id == hwId }
            students = Classroom.students(code).filter { it.approved }
            results = Classroom.results(code, hwId)
            error = null
        } catch (e: Exception) {
            error = friendly(e)
        }
    }
    LaunchedEffect(hwId) { load() }

    val h = hw
    ClassPage {
        BackHeader(h?.title ?: "Homework", h?.let { Classroom.dueText(it.dueAt) }, onBack)
        if (h == null) {
            Muted(if (error == null) "Loading…" else "")
            ErrorLine(error)
            return@ClassPage
        }
        val n = h.tasks.size
        fun doneCount(uid: String) = (0 until n).count { results[uid]?.get(it)?.done == true }
        val finished = students.count { doneCount(it.uid) == n }
        val notStarted = students.filter { s -> results[s.uid].orEmpty().values.none { it.done || it.tries > 0 } }
        val started = students.size - finished - notStarted.size
        val traceScores = results.values.flatMap { m -> m.entries.filter { h.tasks.getOrNull(it.key)?.isTrace == true && it.value.accuracy >= 0 }.map { it.value.accuracy } }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Finished", "$finished / ${students.size}", Modifier.weight(1f))
            Metric("Started", "$started", Modifier.weight(1f))
            Metric("Not started", "${notStarted.size}", Modifier.weight(1f))
            Metric("Tracing", if (traceScores.isEmpty()) "—" else "${traceScores.average().toInt()}%", Modifier.weight(1f))
        }

        // Grid: children down, tasks across
        WhiteCard {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Child", Modifier.width(96.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
                        h.tasks.forEach { t ->
                            Text(
                                t.label.substringAfter(": "), Modifier.width(78.dp).padding(horizontal = 2.dp),
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft,
                                maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (students.isEmpty()) Muted("No children in the class yet.")
                    students.forEach { s ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(s.childName, Modifier.width(96.dp), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            h.tasks.forEachIndexed { i, t ->
                                val c = cellFor(t, results[s.uid]?.get(i))
                                Box(Modifier.width(78.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        c.text,
                                        Modifier.background(c.bg, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.fg
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Muted("Tracing shows how close the lines were to the letter. ★ = stars in a game level.")
        }

        // Who needs help
        val lowTracing = students.mapNotNull { s ->
            val weak = h.tasks.withIndex().filter { (i, t) -> t.isTrace && (results[s.uid]?.get(i)?.accuracy ?: 100) in 0..59 }
            if (weak.isEmpty()) null else "${s.childName} · tracing ${weak.joinToString { it.value.ref }} below 60%"
        }
        if (lowTracing.isNotEmpty() || notStarted.isNotEmpty()) {
            WhiteCard {
                Heading("Needs help")
                lowTracing.forEach { Text(it, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ScrapbookInk) }
                if (notStarted.isNotEmpty()) {
                    Text("Not started: ${notStarted.joinToString { it.childName }}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ScrapbookInk)
                    SoftButton("Remind parents on WhatsApp") {
                        shareOnWhatsApp(uri, "Reminder: today's Kannada homework “${h.title}” is waiting in Fun Learn ಕನ್ನಡ. Tap Homework on the home screen. ${Classroom.dueText(h.dueAt)}.")
                    }
                }
            }
        }

        WhiteCard {
            Heading("Tasks")
            h.tasks.forEachIndexed { i, t -> Text("${i + 1}. ${t.label}  ·  ${taskKind(t)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ScrapbookInk) }
            if (h.note.isNotBlank()) Muted("Note: ${h.note}")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftButton("Refresh") { scope.launch { load() } }
            SoftButton(if (confirmDelete) "Tap again to delete" else "Delete homework", color = ClassRed) {
                if (!confirmDelete) confirmDelete = true
                else scope.launch {
                    runCatching { Classroom.deleteHomework(code, hwId) }.onSuccess { onBack() }.onFailure { error = friendly(it) }
                }
            }
        }
        ErrorLine(error)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White, RoundedCornerShape(16.dp)).padding(10.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft, maxLines = 1)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------
// New homework: pick videos, letters to trace and games; order them; due date; note
// ---------------------------------------------------------------------------------------------

@Composable
fun NewHomeworkScreen(code: String, onBack: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(1) }
    var tasks by remember { mutableStateOf(listOf<HwTask>()) }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var due by remember { mutableStateOf(1) }
    var lockGames by remember { mutableStateOf(true) }
    var reps by remember { mutableStateOf(2) }
    var minutes by remember { mutableStateOf(2) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    fun add(t: HwTask) {
        if (tasks.size >= 8) { error = "8 tasks at most. Kindergarten homework works best with 3 to 5."; return }
        // Suggested order: videos, then tracing, then games
        val rank = { x: HwTask -> if (x.isVideo) 0 else if (x.isTrace) 1 else 2 }
        val at = tasks.indexOfFirst { rank(it) > rank(t) }.let { if (it < 0) tasks.size else it }
        tasks = tasks.toMutableList().apply { add(at, t) }
        error = null
    }

    fun move(i: Int, d: Int) {
        val j = i + d
        if (j !in tasks.indices) return
        tasks = tasks.toMutableList().apply { val x = this[i]; this[i] = this[j]; this[j] = x }
    }

    val tracedLetters = tasks.filter { it.isTrace }.map { it.ref }
    val autoTitle = if (tracedLetters.isNotEmpty()) tracedLetters.joinToString(" and ") else "Homework"

    ClassPage {
        BackHeader("New homework", "Pick tasks in the order the child should do them", onBack)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Videos", "Tracing", "Games").forEachIndexed { i, s -> Chip(s, tab == i) { tab = i } }
        }

        WhiteCard {
            when (tab) {
                0 -> {
                    Muted("Tap a video set to add it.")
                    FlowRowSimple(homeworkVideoFolders) { f ->
                        Chip(f, tasks.any { it.isVideo && it.ref == f }) { add(HwTask(HwTask.TYPE_VIDEO, f, "Watch: $f")) }
                    }
                }
                1 -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Muted("Trace each letter")
                        listOf(1, 2, 3, 5).forEach { r -> Chip("$r×", reps == r) { reps = r } }
                    }
                    val letters = LetterData.getVowels("kn") + LetterData.getConsonants("kn") + LetterData.getNumbers("kn")
                    FlowRowSimple(letters.map { it.character }) { ch ->
                        Chip(ch, tasks.any { it.isTrace && it.ref == ch }, big = true) {
                            add(HwTask(HwTask.TYPE_TRACE, ch, "Trace: $ch", reps = reps))
                        }
                    }
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Muted("Play for")
                        listOf(2, 3, 5).forEach { m -> Chip("$m min", minutes == m) { minutes = m } }
                    }
                    Muted("Games with levels count as done when the child finishes a level.")
                    homeworkGameIds.mapNotNull { GameRegistry.byId(it) }.forEach { g ->
                        val added = tasks.any { it.isGame && it.ref == g.id.toString() }
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(if (added) ClassTealSoft else Color(0xFFF7FAFD))
                                .clickable { add(HwTask(HwTask.TYPE_GAME, g.id.toString(), "Play: ${g.name}", minutes = minutes)) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(Modifier.size(38.dp).background(g.color, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Text(g.glyph, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = g.ink)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(g.name, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                                Text(g.subtitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
                            }
                            Text(if (added) "Added" else "Add", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ClassTeal)
                        }
                    }
                }
            }
        }

        Heading("This homework (${tasks.size})")
        if (tasks.isEmpty()) Muted("Nothing added yet. 3 to 5 tasks is about 10–15 minutes.")
        tasks.forEachIndexed { i, t ->
            val (bg, ink) = taskColors(t)
            Row(
                Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("${i + 1}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInkSoft)
                Box(Modifier.size(36.dp).background(bg, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Text(taskGlyph(t), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ink)
                }
                Column(Modifier.weight(1f)) {
                    Text(t.label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(taskKind(t), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
                }
                Icon(Icons.Filled.KeyboardArrowUp, "Move up", tint = ScrapbookInkSoft, modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).clickable { move(i, -1) })
                Icon(Icons.Filled.KeyboardArrowDown, "Move down", tint = ScrapbookInkSoft, modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).clickable { move(i, 1) })
                Icon(Icons.Filled.Close, "Remove", tint = ClassRed, modifier = Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).clickable { tasks = tasks.filterIndexed { k, _ -> k != i } })
            }
        }

        WhiteCard {
            OutlinedTextField(
                value = title, onValueChange = { title = it.take(40) },
                label = { Text("Title (optional)") }, placeholder = { Text(autoTitle) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note, onValueChange = { note = it.take(200) },
                label = { Text("Note for the child (optional)") }, placeholder = { Text("Practise ಅ and ಆ today!") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Muted("Due")
                listOf(0 to "Today", 1 to "Tomorrow", 2 to "In 2 days", 7 to "In a week").forEach { (d, s) -> Chip(s, due == d) { due = d } }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lock games until the rest is done", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ScrapbookInk)
                    Muted("So children don't skip to the fun part.")
                }
                Switch(checked = lockGames, onCheckedChange = { lockGames = it }, colors = SwitchDefaults.colors(checkedTrackColor = ClassTeal))
            }
        }

        BigButton(if (saving) "Saving…" else "Give homework", ClassViolet) {
            if (saving) return@BigButton
            saving = true
            scope.launch {
                try {
                    Classroom.createHomework(code, title.ifBlank { autoTitle }, note, due, lockGames, tasks)
                    onSaved()
                } catch (e: Exception) {
                    error = friendly(e)
                }
                saving = false
            }
        }
        ErrorLine(error)
        Spacer(Modifier.height(24.dp))
    }
}

/** Wraps chips onto as many rows as needed. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun <T> FlowRowSimple(items: List<T>, item: @Composable (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item(it) }
    }
}
