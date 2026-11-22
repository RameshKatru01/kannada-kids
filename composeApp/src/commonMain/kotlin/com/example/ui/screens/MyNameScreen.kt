package com.example.ui.screens

import com.example.platform.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KannadaLetter
import com.example.ui.audio.NativeAudioPlayer
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.data.ScoreManager
import kotlinx.coroutines.delay

/** Game id of Write My Name (see GameRegistry). */
const val MY_NAME_GAME_ID = 26

private val NameInk = Color(0xFF0C447C)
private val NameSoft = Color(0xFF5F5E5A)
private val NameBg = Color(0xFFFFF8EC)
private val NameAccent = Color(0xFFEF9F27)
private val NameRainbow = listOf(
    Color(0xFF7F77DD), Color(0xFF378ADD), Color(0xFF1D9E75), Color(0xFF639922),
    Color(0xFFBA7517), Color(0xFFD85A30), Color(0xFFD4537E)
)

/**
 * Write My Name: a grown-up types the child's name in English (or straight in Kannada); the app writes
 * it in Kannada letters, which can be fixed by tapping a letter (ತ/ಟ, ದ/ಡ, ನ/ಣ, ಲ/ಳ, short/long
 * vowel). Then the child traces their own name, one letter at a time, and gets a big rainbow name.
 * Free for everyone. The Kannada spelling is saved on the phone.
 */
@Composable
fun MyNameScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { getSharedPreferences("AppProgress") }
    val user = remember { prefs.getString("last_active_user", "guest") ?: "guest" }
    val learner by com.example.ui.data.LearnerName.name.collectAsState()
    LaunchedEffect(Unit) { com.example.ui.data.LearnerName.load() }

    var typed by remember { mutableStateOf(prefs.getString("${user}_my_name_typed", "") ?: "") }
    LaunchedEffect(learner) { if (typed.isBlank() && learner.isNotBlank()) typed = learner.trim().substringBefore(" ") }
    // The Kannada letters (aksharas); a saved, hand-fixed spelling wins over the automatic one
    var letters by remember { mutableStateOf(NameKannada.aksharas(prefs.getString("${user}_my_name_kn", "") ?: "")) }
    var edited by remember { mutableStateOf(letters.isNotEmpty()) }
    LaunchedEffect(typed) { if (!edited || letters.isEmpty()) letters = NameKannada.aksharas(NameKannada.fromTyped(typed)) }

    var step by remember { mutableIntStateOf(-1) }   // -1 = set up the name; 0..n-1 = tracing; n = finished

    fun save() {
        prefs.edit()
            .putString("${user}_my_name_typed", typed)
            .putString("${user}_my_name_kn", letters.joinToString(""))
            .apply()
    }
    fun sayName() = NativeAudioPlayer.playPhonics(context, letters.joinToString(""))

    Column(Modifier.fillMaxSize().background(NameBg).safeDrawingPadding().padding(horizontal = 14.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.White)
                    .clickable { if (step >= 0) step = -1 else onBack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NameInk) }
            Column(Modifier.weight(1f)) {
                Text("My name in Kannada", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = NameInk)
                Text(
                    if (step < 0) "Grown-up: type the name" else if (step < letters.size) "Letter ${step + 1} of ${letters.size}" else "All done!",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NameSoft
                )
            }
        }

        when {
            step < 0 -> NameSetup(
                typed = typed,
                onTyped = { typed = it.take(24); edited = false },
                letters = letters,
                onCycle = { i ->
                    val opts = NameKannada.variants(letters[i])
                    val next = opts[(opts.indexOf(letters[i]) + 1) % opts.size]
                    letters = letters.toMutableList().also { it[i] = next }
                    edited = true
                    SoundEffectsHelper.playButtonTap()
                    NativeAudioPlayer.playPhonics(context, next)
                },
                onSay = { sayName() },
                onStart = {
                    if (letters.isNotEmpty()) { save(); SoundEffectsHelper.playButtonTap(); step = 0 }
                }
            )
            step < letters.size -> {
                NameProgress(letters, step)
                key(step) {
                    TraceExercise(
                        letter = KannadaLetter(id = 9500 + step, character = letters[step], transliteration = "", isVowel = false),
                        onSpeakText = { NativeAudioPlayer.playPhonics(context, it) },
                        onComplete = {
                            if (step + 1 >= letters.size) {
                                SoundEffectsHelper.playApplause()
                                try { ScoreManager.addCoins(context, "my_name", 5 + letters.size * 2) } catch (e: Exception) {}
                            }
                            step += 1
                        },
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 10.dp),
                        requiredTraces = 2
                    )
                }
            }
            else -> NameFinished(
                letters = letters,
                typed = typed,
                onSay = { sayName() },
                onAgain = { SoundEffectsHelper.playButtonTap(); step = 0 },
                onChange = { SoundEffectsHelper.playButtonTap(); step = -1 }
            )
        }
    }
}

@Composable
private fun NameSetup(
    typed: String,
    onTyped: (String) -> Unit,
    letters: List<String>,
    onCycle: (Int) -> Unit,
    onSay: () -> Unit,
    onStart: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = typed,
            onValueChange = onTyped,
            singleLine = true,
            label = { Text("Child's name (English or ಕನ್ನಡ)") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = NameInk, unfocusedTextColor = NameInk,
                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF378ADD), unfocusedBorderColor = Color(0xFFB5D4F4),
                focusedLabelColor = Color(0xFF378ADD), unfocusedLabelColor = NameSoft
            ),
            modifier = Modifier.fillMaxWidth().testTag("my_name_input")
        )

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (letters.isEmpty()) {
                Text("The name in Kannada shows here", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NameSoft)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    letters.forEachIndexed { i, a ->
                        val alts = NameKannada.variants(a).size > 1
                        Box(
                            Modifier.clip(RoundedCornerShape(12.dp))
                                .background(NameRainbow[i % NameRainbow.size].copy(alpha = 0.12f))
                                .border(2.dp, if (alts) NameRainbow[i % NameRainbow.size].copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable(enabled = alts) { onCycle(i) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("my_name_letter_$i"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(a, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = NameRainbow[i % NameRainbow.size])
                        }
                    }
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(NameAccent).clickable { onSay() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Say the name", tint = Color.White) }
                }
                Text(
                    "Not quite right? Tap a letter with a coloured border to change it (for example ತ ↔ ಟ, ನ ↔ ಣ, ಲ ↔ ಳ, or a short ↔ long sound).",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NameSoft, textAlign = TextAlign.Center
                )
            }
        }

        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(if (letters.isEmpty()) Color(0xFFD3D1C7) else Color(0xFFFAC775))
                .clickable(enabled = letters.isNotEmpty()) { onStart() }
                .padding(vertical = 14.dp)
                .testTag("my_name_start"),
            contentAlignment = Alignment.Center
        ) { Text("✏️ Start tracing", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF412402)) }
        Text(
            "The child traces each letter of their name twice. The name stays on this phone only.",
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NameSoft, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NameProgress(letters: List<String>, step: Int) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        letters.forEachIndexed { i, a ->
            val done = i < step
            val now = i == step
            Box(
                Modifier.clip(RoundedCornerShape(10.dp))
                    .background(if (done) Color(0xFFEAF3DE) else Color.White)
                    .border(2.dp, if (done) Color(0xFF639922) else if (now) NameAccent else Color(0xFFD3D1C7), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(a, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = if (done) Color(0xFF3B6D11) else if (now) NameInk else Color(0xFFB4B2A9))
            }
        }
    }
}

@Composable
private fun NameFinished(letters: List<String>, typed: String, onSay: () -> Unit, onAgain: () -> Unit, onChange: () -> Unit) {
    LaunchedEffect(Unit) { delay(600); onSay() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("🎉", fontSize = 52.sp)
        Text("You wrote your name!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = NameInk, textAlign = TextAlign.Center)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White)
                .border(3.dp, Color(0xFFFAC775), RoundedCornerShape(24.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("⭐ ನನ್ನ ಹೆಸರು ⭐", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = NameAccent)
            Row {
                letters.forEachIndexed { i, a ->
                    Text(a, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = NameRainbow[i % NameRainbow.size])
                }
            }
            if (typed.isNotBlank()) Text(typed, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NameSoft)
            Text("Fun Learn ಕನ್ನಡ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB4B2A9))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NamePill("🔊 Hear it", NameAccent, onSay)
            NamePill("🔁 Trace again", Color(0xFF7F77DD), onAgain)
        }
        NamePill("✏️ Change name", NameInk, onChange)
        Text(
            "Tip for grown-ups: take a screenshot of the name card to share it with family!",
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NameSoft, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NamePill(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(22.dp)).background(color).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
}

/**
 * Writes a name typed in English letters in Kannada, the way Indian names are usually spelled
 * (Ananya → ಅನನ್ಯ, Rohit → ರೋಹಿತ್, Priya → ಪ್ರಿಯ, Deepa → ದೀಪ). Text already in Kannada is kept.
 * It can't always guess (t can be ತ or ಟ), so [variants] lists the other spellings of a letter.
 */
object NameKannada {
    private const val VIRAMA = "್"
    private const val ANUSVARA = "ಂ"

    // Longest first, so "chh" wins over "ch" and "c"
    private val consonants = listOf(
        "ksh" to "ಕ್ಷ", "chh" to "ಛ", "shr" to "ಶ್ರ", "sh" to "ಶ", "ch" to "ಚ", "kh" to "ಖ", "gh" to "ಘ",
        "jh" to "ಝ", "th" to "ತ", "dh" to "ಧ", "ph" to "ಫ", "bh" to "ಭ", "gn" to "ಜ್ಞ",
        "k" to "ಕ", "g" to "ಗ", "c" to "ಕ", "j" to "ಜ", "t" to "ತ", "d" to "ದ", "n" to "ನ", "p" to "ಪ",
        "f" to "ಫ", "b" to "ಬ", "m" to "ಮ", "y" to "ಯ", "r" to "ರ", "l" to "ಲ", "v" to "ವ", "w" to "ವ",
        "s" to "ಸ", "h" to "ಹ", "z" to "ಜ", "q" to "ಕ", "x" to "ಕ್ಸ"
    )
    // (letter on its own, sign after a consonant)
    private val vowels = listOf(
        "aa" to ("ಆ" to "ಾ"), "ai" to ("ಐ" to "ೈ"), "au" to ("ಔ" to "ೌ"), "ee" to ("ಈ" to "ೀ"),
        "ii" to ("ಈ" to "ೀ"), "oo" to ("ಊ" to "ೂ"), "uu" to ("ಊ" to "ೂ"), "a" to ("ಅ" to ""),
        "i" to ("ಇ" to "ಿ"), "u" to ("ಉ" to "ು"), "e" to ("ಏ" to "ೇ"), "o" to ("ಓ" to "ೋ")
    )

    private fun isKannada(s: String) = s.any { it in 'ಀ'..'೿' }

    /** The whole name in Kannada (every word is kept). */
    fun fromTyped(typed: String): String {
        val t = typed.trim()
        if (t.isEmpty()) return ""
        if (isKannada(t)) return t.filter { it in 'ಀ'..'೿' }
        return t.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
            .joinToString(" ") { word(it.filter { c -> c in 'a'..'z' }) }.trim()
    }

    private fun word(w: String): String {
        val out = StringBuilder()
        var i = 0
        var pendingConsonant = false   // the last thing written is a consonant still waiting for its vowel
        while (i < w.length) {
            val v = vowels.firstOrNull { w.startsWith(it.first, i) }
            if (v != null) {
                out.append(if (pendingConsonant) v.second.second else v.second.first)
                pendingConsonant = false
                i += v.first.length
                continue
            }
            val c = consonants.firstOrNull { w.startsWith(it.first, i) }
            if (c == null) { i++; continue }
            val next = i + c.first.length
            val nextIsConsonant = next < w.length && vowels.none { w.startsWith(it.first, next) }
            // n / m before another consonant is usually written as ಂ (Ananth → ಅನಂತ್, Ambika → ಅಂಬಿಕ)
            val nasal = (c.first == "n" || c.first == "m") && nextIsConsonant && !pendingConsonant &&
                out.isNotEmpty() && listOf("y", "n", "m", "v", "r", "l", "h").none { w.startsWith(it, next) } &&
                (c.first == "n" || w.startsWith("b", next) || w.startsWith("p", next))
            if (nasal) {
                out.append(ANUSVARA)
                i = next
                continue
            }
            if (pendingConsonant) out.append(VIRAMA)
            out.append(c.second)
            pendingConsonant = true
            i = next
        }
        if (pendingConsonant) out.append(VIRAMA)   // Rohit → ರೋಹಿತ್
        return out.toString()
    }

    /** Splits Kannada text into letters to trace: a consonant cluster with its vowel sign, or a vowel. */
    fun aksharas(kn: String): List<String> {
        val s = kn.filter { it in 'ಀ'..'೿' }
        val out = mutableListOf<String>()
        var i = 0
        fun isConsonant(ch: Char) = ch in 'ಕ'..'ಹ' || ch == 'ೞ'
        fun isSign(ch: Char) = ch in 'ಾ'..'ೌ' || ch == 'ೢ' || ch == 'ೣ'
        while (i < s.length) {
            val start = i
            val ch = s[i]
            i++
            if (isConsonant(ch)) {
                // consonant + ್ + consonant ... (a joined letter)
                while (i + 1 < s.length && s[i] == '್' && isConsonant(s[i + 1])) i += 2
                if (i < s.length && (s[i] == '್' || isSign(s[i]))) i++
            }
            while (i < s.length && (s[i] == 'ಂ' || s[i] == 'ಃ')) i++   // ಂ ಃ
            val part = s.substring(start, i)
            // A lone sign or ಂ joins the letter before it
            if (out.isNotEmpty() && !isConsonant(part[0]) && part[0] !in 'ಅ'..'ಔ') out[out.size - 1] = out.last() + part
            else out += part
        }
        return out
    }

    private val consonantSwaps = listOf("ತ" to "ಟ", "ದ" to "ಡ", "ನ" to "ಣ", "ಲ" to "ಳ", "ಶ" to "ಷ", "ಥ" to "ಠ", "ಧ" to "ಢ")
    private val signSwaps = listOf("" to "ಾ", "ಿ" to "ೀ", "ು" to "ೂ", "ೇ" to "ೆ", "ೋ" to "ೊ")
    private val vowelSwaps = listOf("ಅ" to "ಆ", "ಇ" to "ಈ", "ಉ" to "ಊ", "ಏ" to "ಎ", "ಓ" to "ಒ")

    /** Other spellings of one letter, for the grown-up to pick (always includes [a] itself, first). */
    fun variants(a: String): List<String> {
        if (a.isEmpty()) return listOf(a)
        val out = linkedSetOf(a)
        // Independent vowel at the start
        vowelSwaps.firstOrNull { it.first == a.take(1) || it.second == a.take(1) }?.let { (x, y) ->
            out += (if (a.startsWith(x)) y else x) + a.drop(1)
        }
        // First consonant: dental ↔ retroflex and similar
        val first = a.take(1)
        val swapped = consonantSwaps.firstOrNull { it.first == first || it.second == first }
            ?.let { (x, y) -> (if (first == x) y else x) + a.drop(1) }
        // Vowel sign after the consonant cluster: short ↔ long (no sign ↔ ಾ)
        fun swapSign(s: String): String? {
            if (s.endsWith("್")) return null
            val tail = s.dropLastWhile { it == 'ಂ' || it == 'ಃ' }
            val extra = s.drop(tail.length)
            val last = tail.lastOrNull() ?: return null
            val lastStr = last.toString()
            val pair = signSwaps.firstOrNull { it.first == lastStr || it.second == lastStr }
            return when {
                pair != null && pair.first.isNotEmpty() ->
                    tail.dropLast(1) + (if (lastStr == pair.first) pair.second else pair.first) + extra
                lastStr == "ಾ" -> tail.dropLast(1) + extra
                last in 'ಕ'..'ಹ' -> tail + "ಾ" + extra
                else -> null
            }
        }
        swapSign(a)?.let { out += it }
        if (swapped != null) {
            out += swapped
            swapSign(swapped)?.let { out += it }
        }
        return out.toList()
    }
}
