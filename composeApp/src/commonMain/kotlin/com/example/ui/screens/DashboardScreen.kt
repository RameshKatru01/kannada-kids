package com.example.ui.screens

import com.example.ui.components.readableWidth
import com.example.platform.*

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.LetterData
import com.example.ui.auth.AuthState
import com.example.ui.auth.AuthViewModel
import com.example.ui.components.ScrapbookInk
import com.example.ui.components.ScrapbookInkSoft
import com.example.ui.components.ScrapbookPaper
import com.example.ui.data.ActivityTracker
import com.example.ui.data.BadgeManager
import com.example.ui.data.LanguageManager
import com.example.ui.data.ScoreManager
import com.example.ui.progress.GameProgress
import com.example.ui.progress.GameRegistry
import com.example.ui.progress.StarGold
import com.example.ui.progress.StarIcon
import com.example.ui.progress.formatPoints

internal val DashTeal = Color(0xFF2EC4B6)
internal val DashCoral = Color(0xFFFF6B6B)
internal val DashViolet = Color(0xFF6C63FF)
internal val DashSun = Color(0xFFFFC93C)
internal val DashTangerine = Color(0xFFFF9F1C)
internal val DashBerry = Color(0xFFE84393)

/** How far the child is in each learning section (letters finished). */
internal data class ModuleProgress(
    val key: String,
    val title: String,
    val glyph: String,
    val learned: Int,
    val total: Int,
    val color: Color,
    val tint: Color,
    val route: String
)

internal fun learningProgress(userEmail: String, langCode: String): List<ModuleProgress> {
    val prefs = getSharedPreferences("AppProgress")
    fun unlocked(key: String) = maxOf(
        prefs.getInt("${userEmail}_${langCode}_${key}_unlocked", 0),
        prefs.getInt("${userEmail}_${key}_unlocked", 0)
    )
    return listOf(
        ModuleProgress("vowels", "Vowels", com.example.data.Lang.script("ಅ"), unlocked("vowels").coerceIn(0, LetterData.vowels.size), LetterData.vowels.size, DashTeal, Color(0xFFD9F6F2), "list/vowels"),
        ModuleProgress("consonants", "Consonants", com.example.data.Lang.script("ಕ"), unlocked("consonants").coerceIn(0, LetterData.consonants.size), LetterData.consonants.size, DashViolet, Color(0xFFE6E4FF), "list/consonants"),
        ModuleProgress("guninthakshari", com.example.data.Lang.islandName("guninthakshari"), com.example.data.Lang.script("ಕಾ"), unlocked("guninthakshari").coerceIn(0, LetterData.consonants.size), LetterData.consonants.size, DashCoral, Color(0xFFFFE1E1), "guninthakshari"),
        ModuleProgress("numbers", "Numbers", com.example.data.Lang.script("೧"), unlocked("numbers").coerceIn(0, 10), 10, DashTangerine, Color(0xFFFFE6CC), "list/numbers"),
        com.example.data.OttaksharaData.letters.size.let { n ->
            ModuleProgress(
                "ottakshara", com.example.data.Lang.islandName("ottakshara"), if (com.example.data.Lang.isHindi) "क्ष" else "ಕ್ಕ",
                ActivityTracker.learned("ottakshara").count { it in 0 until n }, n, Color(0xFF14A098), Color(0xFFD3F4EF), "ottakshara"
            )
        }
    )
}

/** The child's dashboard on one screen: overall progress, what to do next, sections, game stars and badges. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel = viewModel { AuthViewModel() },
    onSignOut: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    val authState by authViewModel.authState.collectAsState()
    val userEmail = (authState as? AuthState.Authenticated)?.email ?: "guest"
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val langCode = currentLanguage.code
    val prefs = getSharedPreferences("AppProgress")
    val totalPoints by ScoreManager.totalCoins.collectAsState()
    val activityVersion by ActivityTracker.version.collectAsState()
    val gameVersion by GameProgress.version.collectAsState()

    val modules = remember(userEmail, langCode, activityVersion) { learningProgress(userEmail, langCode) }
    val learned = modules.sumOf { it.learned }
    val total = modules.sumOf { it.total }
    val percent = if (total == 0) 0 else learned * 100 / total
    val streak = remember(activityVersion) { ActivityTracker.streakDays() }
    val levelled = GameRegistry.games.filter { it.hasLevels }
    val gameStars = remember(gameVersion) { levelled.sumOf { GameProgress.totalStars(it) } }
    val maxStars = levelled.sumOf { it.maxStars }
    val badges = remember(userEmail, langCode) {
        BadgeManager.badges.sortedByDescending { it.isUnlocked(prefs, userEmail, langCode) }
            .map { it to it.isUnlocked(prefs, userEmail, langCode) }
    }
    var showGate by remember { mutableStateOf(false) }

    // What to do next: the first section that isn't finished (vowels → consonants → numbers → ka-kaa)
    val next = remember(modules) {
        val order = listOf("vowels", "consonants", "numbers")
        val m = order.mapNotNull { k -> modules.firstOrNull { it.key == k } }.firstOrNull { it.learned < it.total }
        if (m != null) {
            val list = when (m.key) {
                "vowels" -> LetterData.vowels
                "consonants" -> LetterData.consonants
                else -> LetterData.numbers
            }
            val letter = list.getOrNull(m.learned)
            if (letter != null) Triple(
                m,
                letter.character,
                "letter_flow/${m.key}/${m.learned}?char=${Uri.encode(letter.character)}&translit=${Uri.encode(letter.transliteration)}"
            ) else null
        } else null
    }

    Box(Modifier.fillMaxSize().background(ScrapbookPaper)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .readableWidth(720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("My Dashboard", modifier = Modifier.weight(1f), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .clickable { showGate = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("parent_corner_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ScrapbookInkSoft, modifier = Modifier.size(16.dp))
                    Text("Parents", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInkSoft)
                }
            }

            // Summary card: overall progress, greeting, points, streak and game stars
            val learnerName by com.example.ui.data.LearnerName.name.collectAsState()
            LaunchedEffect(Unit) { com.example.ui.data.LearnerName.load() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(ScrapbookInk)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ProgressRing(percent / 100f, 84.dp, 10.dp, DashSun, Color.White.copy(alpha = 0.18f)) {
                    Text("$percent%", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 22.sp)
                    Text("of all letters", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (learnerName.isBlank()) "Hi, little learner!" else "Hi, $learnerName!",
                        fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 21.sp,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text("$learned of $total letters learned", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCFE3F5))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DarkChip { StarIcon(13.dp, StarGold); Text(formatPoints(totalPoints), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }
                        DarkChip {
                            FlameIcon(13.dp)
                            Text(if (streak == 1) "1 day" else "$streak days", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                        DarkChip(onClick = { onNavigate("games") }) {
                            Text("🏅", fontSize = 11.sp)
                            Text("$gameStars/$maxStars stars", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Keep going
            if (next != null) {
                val (m, glyph, route) = next
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .clickable { onNavigate(route) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("dashboard_keep_going"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(m.color)
                            .padding(bottom = 4.dp).clip(RoundedCornerShape(14.dp)).background(m.tint),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(glyph, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("KEEP GOING", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInkSoft, letterSpacing = 0.4.sp)
                        Text("Next letter: $glyph", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk, lineHeight = 17.sp)
                        Text("${m.title} · ${m.learned + 1} of ${m.total}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
                    }
                    Box(Modifier.size(40.dp).background(DashCoral, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = Color.White)
                    }
                }
            }

            // Sections: all five in one row, each with a small progress ring
            Text("My letters", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                modules.forEach { m ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .clickable { onNavigate(m.route) }
                            .padding(top = 7.dp, bottom = 6.dp)
                            .testTag("dashboard_section_${m.key}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val fraction = if (m.total == 0) 0f else m.learned.toFloat() / m.total
                        ProgressRing(fraction, 42.dp, 4.dp, m.color, Color(0xFFE3EEF8)) {
                            Box(Modifier.size(32.dp).background(m.tint, CircleShape), contentAlignment = Alignment.Center) {
                                Text(m.glyph, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk, maxLines = 1)
                            }
                        }
                        Text(
                            m.title, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk, lineHeight = 11.sp,
                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (m.learned >= m.total) {
                            Text("All done!", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF168F84), lineHeight = 11.sp)
                        } else {
                            Text("${m.learned}/${m.total}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft, lineHeight = 11.sp)
                        }
                    }
                }
            }

            // Badges preview
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Badges", modifier = Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                Text(
                    "See all ›",
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onNavigate("rewards") }.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = DashViolet
                )
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                badges.take(5).forEachIndexed { i, (badge, got) ->
                    BadgeCircle(badge.icon, badge.title, got, listOf(DashSun, DashTeal, DashViolet, DashCoral, DashBerry)[i % 5], 44.dp, Modifier.width(60.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        if (showGate) {
            ParentGate(
                onPass = { showGate = false; onNavigate("parent_corner") },
                onCancel = { showGate = false }
            )
        }
    }
}

@Composable
private fun DarkChip(onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

@Composable
internal fun ProgressRing(
    fraction: Float,
    size: Dp,
    stroke: Dp,
    color: Color,
    track: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val inset = w / 2
            val arcSize = Size(this.size.width - w, this.size.height - w)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
            drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

@Composable
internal fun ProgressBar(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE3EEF8))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).clip(RoundedCornerShape(4.dp)).background(color))
    }
}

@Composable
internal fun FlameIcon(size: Dp) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 24f
        val outer = androidx.compose.ui.graphics.Path().apply {
            moveTo(12 * k, 2 * k)
            cubicTo(13 * k, 6 * k, 18 * k, 8 * k, 18 * k, 14 * k)
            cubicTo(18 * k, 18 * k, 15 * k, 21 * k, 12 * k, 21 * k)
            cubicTo(9 * k, 21 * k, 6 * k, 18 * k, 6 * k, 14 * k)
            cubicTo(6 * k, 11 * k, 8 * k, 9 * k, 9 * k, 7 * k)
            cubicTo(9 * k, 9 * k, 10 * k, 10 * k, 11 * k, 10 * k)
            cubicTo(11 * k, 7 * k, 10 * k, 5 * k, 12 * k, 2 * k)
            close()
        }
        drawPath(outer, Color(0xFFFF7A1A))
        drawCircle(Color(0xFFFFC93C), 3 * k, Offset(12 * k, 17 * k))
    }
}

/** A round badge with its emoji (greyed out with a padlock when not earned yet). */
@Composable
internal fun BadgeCircle(icon: String, title: String, earned: Boolean, color: Color, size: Dp, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (earned) color else Color(0xFFD7E3EA))
                .border(3.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (earned) {
                Text(icon, fontSize = (size.value * 0.45f).sp)
            } else {
                Icon(Icons.Default.Lock, contentDescription = "Not earned yet", tint = Color(0xFF6A8297), modifier = Modifier.size(size * 0.4f))
            }
        }
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk, textAlign = TextAlign.Center, lineHeight = 13.sp, maxLines = 2)
        if (subtitle != null) {
            Text(subtitle, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft, textAlign = TextAlign.Center, lineHeight = 12.sp, maxLines = 2)
        }
    }
}
