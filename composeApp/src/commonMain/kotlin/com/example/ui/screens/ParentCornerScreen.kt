package com.example.ui.screens

import com.example.ui.components.readableWidth
import com.example.platform.*

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
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthState
import com.example.ui.auth.AuthViewModel
import com.example.ui.components.ScrapbookInk
import com.example.ui.components.ScrapbookInkSoft
import com.example.ui.components.ScrapbookPaper
import com.example.ui.data.ActivityTracker
import com.example.ui.data.BadgeManager
import com.example.ui.data.LanguageManager
import com.example.ui.data.ScoreManager
import com.example.ui.data.SurpriseStickerManager
import com.example.ui.progress.GameProgress
import com.example.ui.progress.GameRegistry
import com.example.ui.progress.StarGold
import com.example.ui.progress.StarIcon
import com.example.ui.progress.formatPoints
import kotlin.random.Random
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Grown-ups check: a two-digit addition with carrying (too hard for young children)
// ---------------------------------------------------------------------------------------------

/** The public privacy policy page (also given to Google Play and the App Store). */
internal const val PRIVACY_POLICY_URL = "https://rameshkatru01.github.io/kannada-kids/privacy.html"

/** Common questions for parents (GitHub Pages). */
internal const val HELP_URL = "https://rameshkatru01.github.io/kannada-kids/help.html"

/** The app's Play Store page (change the id here if the app id changes before publishing). */
internal const val STORE_URL = "https://play.google.com/store/apps/details?id=com.funlearn.studio"

/** The ready-made message parents can send to friends on WhatsApp. */
internal fun shareMessageUri(): String {
    val text = "We're learning Kannada with Fun Learn – ಕನ್ನಡ! 🎉 Tracing letters, word videos and fun games for kids. Try it: $STORE_URL"
    val enc = buildString {
        for (b in text.encodeToByteArray()) {
            val u = b.toInt() and 0xFF
            val ch = u.toChar()
            if (u < 128 && (ch.isLetterOrDigit() || ch in "-_.~")) append(ch) else append('%').append(u.toString(16).uppercase().padStart(2, '0'))
        }
    }
    return "https://wa.me/?text=$enc"
}

/** Where parents' help emails go. Change it here (and in docs/) when there is a support address. */
internal const val SUPPORT_EMAIL = "ramesh.katru01@gmail.com"

/** A help email with the details we usually need already filled in. */
internal fun supportMailUri(plan: String, language: String): String {
    fun enc(t: String) = buildString {
        for (b in t.encodeToByteArray()) {
            val u = b.toInt() and 0xFF
            val ch = u.toChar()
            if (u < 128 && (ch.isLetterOrDigit() || ch in "-_.~")) append(ch) else append('%').append(u.toString(16).uppercase().padStart(2, '0'))
        }
    }
    val body = "Hi,\n\nPlease tell us what happened:\n\n\n\n---\n" +
        "${com.example.platform.supportDeviceInfo()}\nLanguage: $language · Plan: $plan"
    return "mailto:$SUPPORT_EMAIL?subject=${enc("Fun Learn – Help")}&body=${enc(body)}"
}

private fun newSum(): Pair<Int, Int> {
    while (true) {
        val a = Random.nextInt(26, 90)
        val b = Random.nextInt(17, 80)
        if (a % 10 + b % 10 >= 10 && a + b < 160) return a to b
    }
}

@Composable
fun ParentGate(onPass: () -> Unit, onCancel: () -> Unit, purpose: String = "Answer to open the Parent corner") {
    var sum by remember { mutableStateOf(newSum()) }
    var typed by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60B3B66))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            .safeDrawingPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(30.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("For grown-ups", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
            Text(
                if (wrong) "Not quite. Try this one:" else purpose,
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = if (wrong) Color(0xFFD94848) else ScrapbookInkSoft, textAlign = TextAlign.Center
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${sum.first} + ${sum.second} =", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                Box(
                    Modifier
                        .widthIn(min = 76.dp)
                        .height(54.dp)
                        .border(3.dp, if (wrong) Color(0xFFFF6B6B) else ScrapbookInk, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(typed.ifEmpty { "?" }, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = if (typed.isEmpty()) Color(0xFFB8C7D4) else ScrapbookInk)
                }
            }
            // Number pad
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "OK")
            keys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { k ->
                        val isOk = k == "OK"
                        Box(
                            modifier = Modifier
                                .size(width = 84.dp, height = 54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isOk) Color(0xFF2EC4B6) else Color(0xFFEAF6FF))
                                .clickable {
                                    when (k) {
                                        "⌫" -> typed = typed.dropLast(1)
                                        "OK" -> {
                                            if (typed.toIntOrNull() == sum.first + sum.second) {
                                                onPass()
                                            } else {
                                                wrong = true
                                                typed = ""
                                                sum = newSum()
                                            }
                                        }
                                        else -> if (typed.length < 3) typed += k
                                    }
                                }
                                .testTag("gate_key_$k"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (k == "⌫") {
                                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete", tint = ScrapbookInk)
                            } else {
                                Text(k, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                            }
                        }
                    }
                }
            }
            Text(
                "Cancel",
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onCancel() }.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInkSoft
            )
        }
    }
}

@Composable
internal fun BackHeader(title: String, subtitle: String?, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ScrapbookInk)
        }
        Column {
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk, lineHeight = 30.sp)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
        }
    }
}

@Composable
internal fun WhiteCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

// ---------------------------------------------------------------------------------------------
// My rewards
// ---------------------------------------------------------------------------------------------

private val milestones = listOf(100, 250, 500, 1000, 1500, 2500, 5000, 10000)

@Composable
fun RewardsScreen(onBack: () -> Unit, authViewModel: AuthViewModel = viewModel { AuthViewModel() }) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val userEmail = (authState as? AuthState.Authenticated)?.email ?: "guest"
    val langCode = LanguageManager.currentLanguage.collectAsState().value.code
    val prefs = getSharedPreferences("AppProgress")
    val points by ScoreManager.totalCoins.collectAsState()
    val badges = remember(userEmail, langCode) { BadgeManager.badges.map { it to it.isUnlocked(prefs, userEmail, langCode) } }
    val stickerIds = remember(userEmail) { SurpriseStickerManager.getUnlockedStickerIds(context, userEmail) }
    val stickers = SurpriseStickerManager.allStickers.filter { it.id in stickerIds }
    var showAlbum by remember { mutableStateOf(false) }

    val nextGoal = milestones.firstOrNull { it > points } ?: milestones.last()
    val prevGoal = milestones.lastOrNull { it <= points } ?: 0
    val reached = milestones.count { it <= points }
    val colors = listOf(DashSun, DashTeal, DashViolet, DashCoral, DashBerry, DashTangerine)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookPaper)
            .safeDrawingPadding()
            .readableWidth(720.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BackHeader(
            "My Rewards",
            "${badges.count { it.second }} of ${badges.size} badges · ${stickers.size} of ${SurpriseStickerManager.allStickers.size} stickers",
            onBack
        )

        WhiteCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val frac = if (points >= nextGoal) 1f else (points - prevGoal).toFloat() / (nextGoal - prevGoal)
                ProgressRing(frac, 64.dp, 8.dp, DashSun, Color(0xFFFFF0C2)) { StarIcon(24.dp, StarGold) }
                Column {
                    Text("${formatPoints(points)} points", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                    Text(
                        if (points >= milestones.last()) "You reached every trophy!" else "${formatPoints(nextGoal - points)} more for the next trophy",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft
                    )
                }
            }
            // Trophy row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                milestones.forEachIndexed { i, m ->
                    val got = i < reached
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(30.dp).background(if (got) colors[i % colors.size] else Color(0xFFE3EEF8), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text(if (got) "🏆" else "", fontSize = 15.sp) }
                        Text(if (m >= 1000) "${m / 1000}k" else "$m", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
                    }
                }
            }
        }

        Text("Badges", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
        badges.chunked(3).forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { c, (badge, got) ->
                    Box(
                        Modifier.weight(1f).background(Color.White, RoundedCornerShape(20.dp)).padding(vertical = 12.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        BadgeCircle(badge.icon, badge.title, got, colors[(r * 3 + c) % colors.size], 56.dp, Modifier.fillMaxWidth(), badge.description)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sticker album", modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
            Text(
                "Open album",
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { showAlbum = true }.padding(8.dp),
                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk
            )
        }
        if (stickers.isEmpty()) {
            WhiteCard { Text("Finish a game level to win your first sticker!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
                stickers.takeLast(5).forEachIndexed { i, s ->
                    Box(
                        Modifier
                            .weight(1f)
                            .rotate(listOf(-4f, 3f, -2f, 4f, -3f)[i])
                            .shadow(4.dp, RoundedCornerShape(6.dp))
                            .background(Color.White, RoundedCornerShape(6.dp))
                            .clickable { showAlbum = true }
                            .padding(start = 5.dp, end = 5.dp, top = 5.dp, bottom = 10.dp)
                    ) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).background(colors[i % colors.size].copy(alpha = 0.35f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text(s.emoji, fontSize = 26.sp) }
                    }
                }
                repeat(5 - minOf(5, stickers.size)) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showAlbum) {
        com.example.ui.components.StickerAlbumDialog(userEmail = userEmail, onClose = { showAlbum = false })
    }
}

// ---------------------------------------------------------------------------------------------
// Parent corner
// ---------------------------------------------------------------------------------------------

@Composable
fun ParentCornerScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onOpenPlans: () -> Unit = {},
    onOpenLearningPath: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val userEmail = (authState as? AuthState.Authenticated)?.email ?: "guest"
    val langCode = LanguageManager.currentLanguage.collectAsState().value.code
    val prefs = getSharedPreferences("AppProgress")
    val version by ActivityTracker.version.collectAsState()

    val week = remember(version) { ActivityTracker.lastSevenDays() }
    val weekMinutes = week.sumOf { it.minutes }
    val activeDays = week.count { it.minutes > 0 }
    val learned = remember(userEmail, langCode) { learningProgress(userEmail, langCode).sumOf { it.learned } }
    val levelsDone = remember { GameRegistry.games.filter { it.hasLevels }.sumOf { GameProgress.levelsDone(it) } }
    val accuracy = remember(version) { ActivityTracker.accuracyPercent() }
    val weak = remember(version) { ActivityTracker.needsPractice() }
    val strong = remember(version) { ActivityTracker.strongLetters() }

    var showCredits by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookPaper)
            .safeDrawingPadding()
            .readableWidth(720.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BackHeader("Parent corner", "How your child is doing", onBack)

        // Which language the child is learning (each has its own letters, progress and voice)
        val learning by com.example.ui.data.LanguageManager.currentLanguage.collectAsState()
        WhiteCard {
            Text("Learning language", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(com.example.ui.data.AppLanguage.KANNADA to "ಕನ್ನಡ Kannada", com.example.ui.data.AppLanguage.HINDI to "हिन्दी Hindi").forEach { (lang, label) ->
                    val on = learning == lang
                    val ready = lang.available
                    Box(
                        Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp))
                            .background(if (on) ScrapbookInk else Color.White)
                            .border(2.dp, if (ready) ScrapbookInk else Color(0xFFCBD5E1), RoundedCornerShape(24.dp))
                            .clickable {
                                if (ready) com.example.ui.data.LanguageManager.setLanguage(context, lang)
                                else Toast.makeText(context, "${lang.displayName} is coming soon!", Toast.LENGTH_SHORT).show()
                            }
                            .testTag("language_${lang.code}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (on) Color.White else if (ready) ScrapbookInk else Color(0xFF94A3B8), lineHeight = 16.sp)
                            if (!ready) Text("Coming soon", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), lineHeight = 11.sp)
                        }
                    }
                }
            }
            Text(if (com.example.ui.data.AppLanguage.HINDI.available) "Each language keeps its own progress. Gold covers both." else "More languages are coming soon.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
        }

        // Plan
        val sub by com.example.ui.data.SubscriptionManager.state.collectAsState()
        LaunchedEffect(Unit) { com.example.ui.data.SubscriptionManager.refresh() }
        Column(
            Modifier.fillMaxWidth().background(Color(0xFFFFF6E0), RoundedCornerShape(22.dp)).border(2.dp, Color(0xFFF2DFAE), RoundedCornerShape(22.dp))
                .clickable { onOpenPlans() }.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).background(DashSun, CircleShape), contentAlignment = Alignment.Center) { StarIcon(24.dp, Color.White) }
                Column(Modifier.weight(1f)) {
                    val p = sub.plan
                    Text(
                        if (sub.hasPremium && p != null) "Premium · ${p.title}" else if (sub.hasPremium) "Premium (gift)"
                        else if (com.example.ui.data.Access.isTestGold()) "Gold (test account)" else "Free plan",
                        fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk
                    )
                    Text(
                        when {
                            sub.hasPremium && p != null && sub.renewsAt > 0 ->
                                (if (sub.autoRenew) "Renews " else "Ends ") + formatDate(sub.renewsAt)
                            sub.bonusUntil > currentTimeMillis() -> "Free until " + formatDate(sub.bonusUntil)
                            else -> "₹99 a month, ₹499 for 6 months or ₹899 a year"
                        },
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft
                    )
                }
            }
            PlanButton(if (sub.hasPremium) "See my plan" else "See plans", Modifier.fillMaxWidth()) { onOpenPlans() }
        }

        // Learning path (how letters, islands and levels open)
        val pathVersion by com.example.ui.data.Access.version.collectAsState()
        val everythingOpen = remember(pathVersion) { com.example.ui.data.Access.everythingOpen }
        val startFrom = remember(pathVersion) { com.example.ui.data.Access.startFrom }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).clickable { onOpenLearningPath() }
                .padding(14.dp).testTag("open_learning_path"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(44.dp).background(Color(0xFFE6E4FF), CircleShape), contentAlignment = Alignment.Center) {
                Text("🗺️", fontSize = 22.sp)
            }
            Column(Modifier.weight(1f)) {
                Text("Learning path", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                Text(
                    if (everythingOpen) "Everything open"
                    else "Step by step · from " + listOf("the beginning", "Consonants", com.example.data.Lang.islandName("guninthakshari"), com.example.data.Lang.islandName("ottakshara"))[startFrom],
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft
                )
            }
            Text("›", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
        }

        // Needs more practice
        WhiteCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Needs more practice", modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                Text("most mistakes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
            }
            LetterChips(weak, Color(0xFFFFE1E1), Color(0xFF9E2B2B), "No tricky letters yet. Keep playing the games!")
        }

        // Strong letters
        WhiteCard {
            Text("Strong letters", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
            LetterChips(strong, Color(0xFFD9F6F2), Color(0xFF0F6B62), "Letters answered right again and again will show here.")
        }

        // Minutes this week
        WhiteCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Minutes this week", modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                Text("$weekMinutes min · $activeDays of 7 days", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF168F84))
            }
            val maxMin = maxOf(1, week.maxOf { it.minutes })
            Row(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                week.forEach { d ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${d.minutes}", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                        Box(
                            Modifier
                                .width(24.dp)
                                .height((96f * d.minutes / maxMin).dp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(if (d.isToday) DashCoral else if (d.minutes == 0) Color(0xFFE3EEF8) else DashTeal)
                        )
                        Text(d.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
                    }
                }
            }
        }

        // Quick numbers
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("$learned", "letters learned", DashTeal, Modifier.weight(1f))
            StatTile("$levelsDone", "levels finished", DashViolet, Modifier.weight(1f))
            StatTile(accuracy?.let { "$it%" } ?: "–", "right answers", DashSun, Modifier.weight(1f))
        }

        // Rate us: asked once, after the child has made real progress (Google Play only for now)
        val rateUri = androidx.compose.ui.platform.LocalUriHandler.current
        val ratePrefs = remember { getSharedPreferences("AppSettings") }
        var rateAsked by remember { mutableStateOf(ratePrefs.getBoolean("rate_prompt_done", false)) }
        val onPlay = PlatformBilling.storeName() == "Google Play"
        if (onPlay && !rateAsked && (learned >= 10 || levelsDone >= 5)) {
            Column(
                Modifier.fillMaxWidth().background(Color(0xFFFFF0C2), RoundedCornerShape(22.dp)).border(2.dp, DashSun, RoundedCornerShape(22.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Enjoying Fun Learn? ⭐", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                Text("Your child has learned $learned letters! A quick rating helps other families find the app.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).background(ScrapbookInk)
                            .clickable {
                                ratePrefs.edit().putBoolean("rate_prompt_done", true).apply(); rateAsked = true
                                rateUri.openUri(STORE_URL)
                            }.testTag("rate_now"),
                        contentAlignment = Alignment.Center
                    ) { Text("⭐ Rate the app", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
                    Box(
                        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).border(2.dp, ScrapbookInk, RoundedCornerShape(22.dp))
                            .clickable { ratePrefs.edit().putBoolean("rate_prompt_done", true).apply(); rateAsked = true }
                            .testTag("rate_later"),
                        contentAlignment = Alignment.Center
                    ) { Text("Maybe later", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk) }
                }
            }
        }

        // Share with a friend (WhatsApp, with a ready-made message)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White)
                .clickable { rateUri.openUri(shareMessageUri()) }
                .padding(14.dp).testTag("share_app"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(44.dp).background(Color(0xFFDCF8C6), CircleShape), contentAlignment = Alignment.Center) { Text("💚", fontSize = 22.sp) }
            Column(Modifier.weight(1f)) {
                Text("Share with a friend", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                Text("Send Fun Learn to other Kannada families on WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
            }
            Text("›", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
        }

        // Help & support (email with details filled in, and common questions)
        val helpUri = androidx.compose.ui.platform.LocalUriHandler.current
        val helpSub by com.example.ui.data.SubscriptionManager.state.collectAsState()
        val helpLang by com.example.ui.data.LanguageManager.currentLanguage.collectAsState()
        WhiteCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).background(Color(0xFFD9F6F2), CircleShape), contentAlignment = Alignment.Center) {
                    Text("💬", fontSize = 22.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Help & support", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                    Text("Questions or a problem? We're happy to help.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(ScrapbookInk)
                        .clickable {
                            val plan = helpSub.plan?.title ?: if (helpSub.hasPremium) "Premium (gift)" else "Free"
                            try { helpUri.openUri(supportMailUri(plan, helpLang.name)) } catch (e: Exception) {
                                Toast.makeText(context, "Please email us at $SUPPORT_EMAIL", Toast.LENGTH_LONG).show()
                            }
                        }
                        .testTag("support_email"),
                    contentAlignment = Alignment.Center
                ) { Text("📧 Email us", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
                Box(
                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).border(2.dp, ScrapbookInk, RoundedCornerShape(23.dp))
                        .clickable { helpUri.openUri(HELP_URL) }
                        .testTag("support_faq"),
                    contentAlignment = Alignment.Center
                ) { Text("❓ Common questions", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk, maxLines = 1) }
            }
            Text(SUPPORT_EMAIL, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
        }

        // Sign out
        Box(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).border(2.dp, ScrapbookInk, RoundedCornerShape(26.dp)).clickable { confirmSignOut = true },
            contentAlignment = Alignment.Center
        ) { Text("Sign out", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk) }

        // Delete account (required by Google Play and the App Store)
        val hasAccount = remember(authState) { PlatformAuth.currentUser() != null }
        if (hasAccount) {
            Text(
                "Delete account",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { confirmDelete = true }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("delete_account"),
                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD94848)
            )
        }
        val policyUri = androidx.compose.ui.platform.LocalUriHandler.current
        if (PlatformBilling.storeName() == "Google Play") {
            Text(
                "⭐ Rate the app",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { policyUri.openUri(STORE_URL) }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("rate_link"),
                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1565C0)
            )
        }
        Text(
            "Privacy policy",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(12.dp))
                .clickable { policyUri.openUri(PRIVACY_POLICY_URL) }
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("privacy_policy"),
            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1565C0)
        )
        Text(
            "Credits",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(12.dp))
                .clickable { showCredits = true }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft
        )
        Spacer(Modifier.height(8.dp))
    }

    if (showCredits) {
        AlertDialog(
            onDismissRequest = { showCredits = false },
            title = { Text("Credits", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pictures in the Fruits, Vegetables, Body Parts and Family videos: Noto Emoji by Google, licensed under the Apache License 2.0.")
                    Text("Kannada lettering in those videos: Baloo Tamma 2 by Ek Type, licensed under the SIL Open Font License 1.1.")
                    Text("Kannada voice: Google Cloud Text-to-Speech.")
                }
            },
            confirmButton = { TextButton(onClick = { showCredits = false }) { Text("OK") } }
        )
    }

    if (confirmDelete) {
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        val appStore = PlatformBilling.storeName() == "App Store"
        AlertDialog(
            onDismissRequest = { if (!deleting) confirmDelete = false },
            title = { Text("Delete account?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This permanently deletes your Kannada Kids account, your subscription record with us and all progress saved on this device (letters, stars, points, settings). It can't be undone.")
                    Text(
                        "Deleting the account does not cancel a subscription. Cancel it in ${if (appStore) "the App Store" else "Google Play"} so you're not charged again.",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Manage subscriptions",
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                            uriHandler.openUri(
                                if (appStore) "https://apps.apple.com/account/subscriptions"
                                else "https://play.google.com/store/account/subscriptions"
                            )
                        }.padding(vertical = 4.dp),
                        color = Color(0xFF1565C0), fontWeight = FontWeight.ExtraBold
                    )
                    deleteError?.let { Text(it, color = Color(0xFFD94848), fontWeight = FontWeight.Bold) }
                    if (deleting) Text("Deleting… you may be asked to sign in once more.", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        deleting = true
                        deleteError = null
                        scope.launch {
                            when (val r = authViewModel.deleteAccount(context)) {
                                is AuthViewModel.DeleteResult.Done -> {
                                    deleting = false
                                    confirmDelete = false
                                    onSignOut()
                                }
                                is AuthViewModel.DeleteResult.Failed -> {
                                    deleting = false
                                    deleteError = r.message
                                }
                            }
                        }
                    }
                ) { Text("Delete permanently", color = Color(0xFFD94848), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(enabled = !deleting, onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?", fontWeight = FontWeight.Bold) },
            text = { Text("You will go back to the sign-in screen. Progress stays saved on this device.") },
            confirmButton = { TextButton(onClick = { confirmSignOut = false; onSignOut() }) { Text("Sign out") } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatTile(value: String, label: String, color: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .padding(top = 5.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(Color.White)
            .padding(12.dp)
    ) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk, lineHeight = 28.sp)
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft, lineHeight = 14.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LetterChips(letters: List<String>, bg: Color, ink: Color, empty: String) {
    if (letters.isEmpty()) {
        Text(empty, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        letters.forEach { l ->
            Box(
                Modifier.heightIn(min = 44.dp).widthIn(min = 44.dp).background(bg, RoundedCornerShape(14.dp)).padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) { Text(l, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ink) }
        }
    }
}

@Composable
private fun PlanButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(46.dp).clip(RoundedCornerShape(23.dp)).background(DashSun).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(label, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk) }
}
