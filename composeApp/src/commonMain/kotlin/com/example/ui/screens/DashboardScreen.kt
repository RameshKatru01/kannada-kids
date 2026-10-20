package com.example.ui.screens

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
import com.example.ui.progress.PointsPill
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
        ModuleProgress("vowels", "Vowels", "ಅ", unlocked("vowels").coerceIn(0, 15), 15, DashTeal, Color(0xFFD9F6F2), "list/vowels"),
        ModuleProgress("consonants", "Consonants", "ಕ", unlocked("consonants").coerceIn(0, 34), 34, DashViolet, Color(0xFFE6E4FF), "list/consonants"),
        ModuleProgress("guninthakshari", "Ka-kaa", "ಕಾ", unlocked("guninthakshari").coerceIn(0, 34), 34, DashCoral, Color(0xFFFFE1E1), "guninthakshari"),
        ModuleProgress("numbers", "Numbers", "೧", unlocked("numbers").coerceIn(0, 10), 10, DashTangerine, Color(0xFFFFE6CC), "list/numbers")
    )
}

/** The child's dashboard: overall progress, what to do next, sections, game stars and badges. */
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

    val modules = remember(userEmail, langCode) { learningProgress(userEmail, langCode) }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("My Dashboard", modifier = Modifier.weight(1f), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
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

            // Big summary card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(ScrapbookInk)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProgressRing(percent / 100f, 112.dp, 14.dp, DashSun, Color.White.copy(alpha = 0.18f)) {
                    Text("$percent%", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("of all letters", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hi, little learner!", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 24.sp)
                    Text("$learned of $total letters learned", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCFE3F5))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DarkChip { StarIcon(15.dp, StarGold); Text(formatPoints(totalPoints), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
                        DarkChip {
                            FlameIcon(15.dp)
                            Text(if (streak == 1) "1 day" else "$streak days", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
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
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .clickable { onNavigate(route) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("dashboard_keep_going"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(m.color)
                            .padding(bottom = 4.dp).clip(RoundedCornerShape(18.dp)).background(m.tint),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(glyph, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("KEEP GOING", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInkSoft, letterSpacing = 0.4.sp)
                        Text("Next letter: $glyph", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                        Text("${m.title} · ${m.learned + 1} of ${m.total}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
                    }
                    Box(Modifier.size(48.dp).background(DashCoral, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = Color.White)
                    }
                }
            }

            // Sections
            Text("My letters", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
            modules.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { m ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White)
                                .clickable { onNavigate(m.route) }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(44.dp).background(m.tint, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                    Text(m.glyph, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                                }
                                Column {
                                    Text(m.title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                                    if (m.learned >= m.total) {
                                        Text("All done!", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF168F84))
                                    } else {
                                        Text("${m.learned} of ${m.total}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
                                    }
                                }
                            }
                            ProgressBar(m.learned.toFloat() / m.total, m.color)
                        }
                    }
                }
            }

            // Game stars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .clickable { onNavigate("games") }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.size(44.dp).background(Color(0xFFFDEEF6), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    StarIcon(26.dp, StarGold)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Text("Game stars", modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                        Text("$gameStars / $maxStars", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                    }
                    ProgressBar(if (maxStars == 0) 0f else gameStars.toFloat() / maxStars, DashBerry)
                }
            }

            // Badges preview
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Badges", modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                Text(
                    "See all",
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onNavigate("rewards") }.padding(8.dp),
                    fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk
                )
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                badges.take(5).forEachIndexed { i, (badge, got) ->
                    BadgeCircle(badge.icon, badge.title, got, listOf(DashSun, DashTeal, DashViolet, DashCoral, DashBerry)[i % 5], 54.dp, Modifier.width(62.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
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
private fun DarkChip(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
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
