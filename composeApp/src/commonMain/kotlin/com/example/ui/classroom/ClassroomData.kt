package com.example.ui.classroom

import com.example.platform.*
import com.example.ui.data.CloudStore
import com.example.ui.data.bool
import com.example.ui.data.long
import com.example.ui.data.str
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * Classroom homework: data kept online in Firestore.
 *
 *   classes/{code}                               name, teacherUid, teacherName, createdAt
 *   classes/{code}/students/{uid}                childName, approved, joinedAt
 *   classes/{code}/homework/{hwId}               title, note, createdAt, dueAt, lockGames, tasks[]
 *   classes/{code}/homework/{hwId}/results/{uid} childName, updatedAt, t0, t1, … (one map per task)
 *   teachers/{uid}                               classes[] (codes this account teaches)
 *   classMembers/{uid}                           code, childName (the class this family's child is in)
 *
 * One child per signed-in account: the student id is the account's uid.
 */

/** One homework item. [type] is "video" (ref = video folder), "trace" (ref = letter) or "game" (ref = game id). */
data class HwTask(
    val type: String,
    val ref: String,
    val label: String,
    /** Tracing: good traces needed. */
    val reps: Int = 2,
    /** Games without levels: minutes of play that count as done. */
    val minutes: Int = 2
) {
    val isGame: Boolean get() = type == TYPE_GAME
    val isTrace: Boolean get() = type == TYPE_TRACE
    val isVideo: Boolean get() = type == TYPE_VIDEO

    fun toMap(): Map<String, Any?> = mapOf(
        "type" to type, "ref" to ref, "label" to label, "reps" to reps.toLong(), "minutes" to minutes.toLong()
    )

    companion object {
        const val TYPE_VIDEO = "video"
        const val TYPE_TRACE = "trace"
        const val TYPE_GAME = "game"

        fun from(m: Map<String, Any?>) = HwTask(
            type = m.str("type") ?: TYPE_VIDEO,
            ref = m.str("ref") ?: "",
            label = m.str("label") ?: "",
            reps = m.long("reps").toInt().takeIf { it > 0 } ?: 2,
            minutes = m.long("minutes").toInt().takeIf { it > 0 } ?: 2
        )
    }
}

/** How a child did on one task. [accuracy] is 0..100 for tracing (-1 when not traced). */
data class TaskResult(
    val done: Boolean = false,
    val accuracy: Int = -1,
    val stars: Int = 0,
    val tries: Int = 0,
    val seconds: Int = 0,
    val at: Long = 0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "done" to done, "accuracy" to accuracy.toLong(), "stars" to stars.toLong(),
        "tries" to tries.toLong(), "seconds" to seconds.toLong(), "at" to at
    )

    companion object {
        fun from(m: Map<String, Any?>) = TaskResult(
            done = m.bool("done"),
            accuracy = (m["accuracy"] as? Long)?.toInt() ?: -1,
            stars = m.long("stars").toInt(),
            tries = m.long("tries").toInt(),
            seconds = m.long("seconds").toInt(),
            at = m.long("at")
        )
    }
}

data class Homework(
    val id: String,
    val title: String,
    val note: String,
    val createdAt: Long,
    val dueAt: Long,
    val lockGames: Boolean,
    val tasks: List<HwTask>
)

data class ClassInfo(val code: String, val name: String, val teacherName: String, val teacherUid: String)

data class Student(val uid: String, val childName: String, val approved: Boolean, val joinedAt: Long)

/** The class this account's child belongs to. */
data class Membership(val code: String, val className: String, val teacherName: String, val childName: String, val approved: Boolean)

object Classroom {
    private const val DAY = 24L * 60 * 60 * 1000

    private val _membership = MutableStateFlow<Membership?>(null)
    val membership: StateFlow<Membership?> = _membership.asStateFlow()

    /** Not-yet-done tasks in the newest homework (for the Home button). */
    private val _todo = MutableStateFlow(0)
    val todo: StateFlow<Int> = _todo.asStateFlow()

    private fun uid(): String =
        PlatformAuth.currentUser()?.uid ?: throw IllegalStateException("Please sign in first.")

    private fun now() = currentTimeMillis()

    fun cleanCode(code: String) = code.trim().uppercase().replace(" ", "")

    // ---------------- Family side ----------------

    /** Reads which class this account's child is in (and whether the teacher has let them in). */
    suspend fun loadMembership(): Membership? {
        val me = PlatformAuth.currentUser()?.uid ?: run { _membership.value = null; return null }
        val link = runCatching { CloudStore.get("classMembers/$me") }.getOrNull()
        val code = link?.str("code")
        if (code.isNullOrBlank()) { _membership.value = null; return null }
        val cls = runCatching { CloudStore.get("classes/$code") }.getOrNull()
        val st = runCatching { CloudStore.get("classes/$code/students/$me") }.getOrNull()
        if (cls == null || st == null) {
            // Class deleted or child removed by the teacher
            runCatching { CloudStore.delete("classMembers/$me") }
            _membership.value = null
            return null
        }
        val m = Membership(
            code = code,
            className = cls.str("name") ?: code,
            teacherName = cls.str("teacherName") ?: "",
            childName = st.str("childName") ?: link.str("childName") ?: "",
            approved = st.bool("approved")
        )
        _membership.value = m
        return m
    }

    suspend fun findClass(code: String): ClassInfo? {
        val c = cleanCode(code)
        if (c.length < 4) return null
        val d = CloudStore.get("classes/$c") ?: return null
        return ClassInfo(c, d.str("name") ?: c, d.str("teacherName") ?: "", d.str("teacherUid") ?: "")
    }

    suspend fun join(code: String, childName: String): Membership {
        val c = cleanCode(code)
        val name = childName.trim().take(20)
        require(name.isNotEmpty()) { "Please type your child's name." }
        val cls = findClass(c) ?: throw IllegalStateException("No class has the code $c. Check it with the teacher.")
        val me = uid()
        CloudStore.update("classes/$c/students/$me", mapOf("childName" to name, "approved" to false, "joinedAt" to now()))
        CloudStore.update("classMembers/$me", mapOf("code" to c, "childName" to name))
        val m = Membership(c, cls.name, cls.teacherName, name, false)
        _membership.value = m
        return m
    }

    suspend fun leave() {
        val m = _membership.value ?: return
        val me = uid()
        runCatching { CloudStore.delete("classes/${m.code}/students/$me") }
        CloudStore.delete("classMembers/$me")
        _membership.value = null
        _todo.value = 0
    }

    // ---------------- Homework ----------------

    suspend fun homework(code: String): List<Homework> =
        CloudStore.list("classes/$code/homework").map { (id, m) ->
            @Suppress("UNCHECKED_CAST")
            val tasks = (m["tasks"] as? List<Any?>).orEmpty().mapNotNull { (it as? Map<String, Any?>)?.let { m2 -> HwTask.from(m2) } }
            Homework(
                id = id,
                title = m.str("title") ?: "Homework",
                note = m.str("note") ?: "",
                createdAt = m.long("createdAt"),
                dueAt = m.long("dueAt"),
                lockGames = m["lockGames"] as? Boolean ?: true,
                tasks = tasks
            )
        }.sortedByDescending { it.createdAt }

    private fun parseResults(m: Map<String, Any?>?): Map<Int, TaskResult> {
        if (m == null) return emptyMap()
        return m.entries.mapNotNull { (k, v) ->
            val i = k.removePrefix("t").toIntOrNull() ?: return@mapNotNull null
            if (!k.startsWith("t")) return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            (v as? Map<String, Any?>)?.let { i to TaskResult.from(it) }
        }.toMap()
    }

    suspend fun myResults(code: String, hwId: String): Map<Int, TaskResult> =
        parseResults(CloudStore.get("classes/$code/homework/$hwId/results/${uid()}"))

    suspend fun saveResult(code: String, hwId: String, index: Int, result: TaskResult) {
        CloudStore.update(
            "classes/$code/homework/$hwId/results/${uid()}",
            mapOf(
                "t$index" to result.toMap(),
                "childName" to (_membership.value?.childName ?: ""),
                "updatedAt" to now()
            )
        )
    }

    fun setTodo(count: Int) { _todo.value = count }

    // ---------------- Teacher side ----------------

    suspend fun createClass(name: String): ClassInfo {
        val clean = name.trim().take(30)
        require(clean.isNotEmpty()) { "Please type a class name, like UKG – B." }
        val me = PlatformAuth.currentUser() ?: throw IllegalStateException("Please sign in first.")
        var code: String
        var tries = 0
        do {
            code = "KAN-" + (1000 + (now() / 7 + tries * 7919).mod(9000L)).toString()
            tries++
        } while (CloudStore.get("classes/$code") != null && tries < 20)
        val teacherName = me.displayName?.substringBefore(' ')?.ifBlank { null } ?: "Teacher"
        CloudStore.update(
            "classes/$code",
            mapOf("name" to clean, "teacherUid" to me.uid, "teacherName" to teacherName, "createdAt" to now())
        )
        val existing = runCatching { CloudStore.get("teachers/${me.uid}") }.getOrNull()
        @Suppress("UNCHECKED_CAST")
        val codes = ((existing?.get("classes") as? List<Any?>).orEmpty().mapNotNull { it as? String }) + code
        CloudStore.update("teachers/${me.uid}", mapOf("classes" to codes.distinct()))
        return ClassInfo(code, clean, teacherName, me.uid)
    }

    suspend fun myClasses(): List<ClassInfo> {
        val me = uid()
        val doc = CloudStore.get("teachers/$me") ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val codes = (doc["classes"] as? List<Any?>).orEmpty().mapNotNull { it as? String }
        return codes.mapNotNull { c -> runCatching { findClass(c) }.getOrNull() }
    }

    suspend fun students(code: String): List<Student> =
        CloudStore.list("classes/$code/students").map { (id, m) ->
            Student(id, m.str("childName") ?: "Child", m.bool("approved"), m.long("joinedAt"))
        }.sortedBy { it.childName.lowercase() }

    suspend fun approve(code: String, studentUid: String) =
        CloudStore.update("classes/$code/students/$studentUid", mapOf("approved" to true))

    suspend fun removeStudent(code: String, studentUid: String) =
        CloudStore.delete("classes/$code/students/$studentUid")

    suspend fun createHomework(code: String, title: String, note: String, dueInDays: Int, lockGames: Boolean, tasks: List<HwTask>) {
        require(tasks.isNotEmpty()) { "Add at least one task." }
        val t = now()
        CloudStore.update(
            "classes/$code/homework/hw$t",
            mapOf(
                "title" to title.trim().ifBlank { "Homework" }.take(40),
                "note" to note.trim().take(200),
                "createdAt" to t,
                "dueAt" to t + dueInDays * DAY,
                "lockGames" to lockGames,
                "tasks" to tasks.map { it.toMap() }
            )
        )
    }

    suspend fun deleteHomework(code: String, hwId: String) = CloudStore.delete("classes/$code/homework/$hwId")

    /** Everyone's results for one homework: student uid -> (task index -> result). */
    suspend fun results(code: String, hwId: String): Map<String, Map<Int, TaskResult>> =
        CloudStore.list("classes/$code/homework/$hwId/results").mapValues { (_, m) -> parseResults(m) }

    /** "Due today", "Due tomorrow", "Due in 3 days", "Was due 2 days ago". */
    fun dueText(dueAt: Long): String {
        if (dueAt <= 0) return ""
        val left = dueAt - now()
        if (left <= 0) {
            val ago = (-left / DAY).toInt()
            return if (ago == 0) "Due today" else "Was due $ago day${if (ago == 1) "" else "s"} ago"
        }
        val days = ((left + DAY / 2) / DAY).toInt()
        return when {
            days == 0 -> "Due today"
            days == 1 -> "Due tomorrow"
            else -> "Due in $days days"
        }
    }
}
