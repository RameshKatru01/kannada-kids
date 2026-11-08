package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LetterData
import com.example.ui.data.Access
import com.example.ui.data.ActivityTracker
import com.example.ui.data.ScoreManager
import com.example.ui.progress.GameInfo
import com.example.ui.progress.lerpColor

/*
 * Home (after sign-in): greeting + points + streak, then the "Play TV": a cartoon TV showing the
 * chosen section (Vowels, Consonants, Ka-kaa, Numbers, Ottakshara) with its progress and next letter,
 * three big buttons (Watch / Play / Learn) and the sections as cartridges on a shelf.
 * (The older space map, IslandMap, is kept below but no longer shown.)
 */

private val HomeInk = Color(0xFF0B3B66)
private val HomeInkSoft = Color(0xFF3A5F82)
private val HomeSky = Color(0xFFEAF6FF)
private val HomeCoral = Color(0xFFFF6B6B)
private val HomeSun = Color(0xFFFFC93C)

/** One island on the map: a learning module and how far the child has got in it. */
private data class Island(
    val type: String,
    val name: String,
    val glyph: String,
    val color: Color,
    val tint: Color,
    val total: Int,
    val learned: Set<Int>,
    val open: () -> Unit,
    val state: Access.State = Access.State.Open
) {
    val done: Int get() = learned.count { it in 0 until total }
    val finished: Boolean get() = total > 0 && done >= total
    val nextIndex: Int get() = (0 until total).firstOrNull { it !in learned } ?: 0
}

@Composable
fun HomeScreen(
    onNavigateToVowels: () -> Unit,
    onNavigateToConsonants: () -> Unit,
    onNavigateToGunninthakshari: () -> Unit,
    onNavigateToNumbers: () -> Unit,
    onNavigateToOttakshara: () -> Unit = {},
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onOpenLetter: (type: String, index: Int) -> Unit = { _, _ -> },
    onOpenGame: (GameInfo) -> Unit = {},
    onSeeAllGames: () -> Unit = {},
    onOpenVideos: (folder: String) -> Unit = {},
    onSeeAllVideos: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val version by ActivityTracker.version.collectAsState()
    val coins by ScoreManager.totalCoins.collectAsState()
    val currentLanguage by com.example.ui.data.LanguageManager.currentLanguage.collectAsState()

    val pathVersion by Access.version.collectAsState()
    val sub by com.example.ui.data.SubscriptionManager.state.collectAsState()
    val premium = remember(sub, pathVersion) { Access.isPremium() }
    val islands = remember(version, currentLanguage, pathVersion, sub) {
        listOf(
            Island("vowels", com.example.data.Lang.islandName("vowels"), com.example.data.Lang.script("ಅ"), Color(0xFF2EC4B6), Color(0xFFD9F6F2), LetterData.vowels.size, ActivityTracker.learned("vowels"), onNavigateToVowels),
            Island("consonants", com.example.data.Lang.islandName("consonants"), com.example.data.Lang.script("ಕ"), Color(0xFF6C63FF), Color(0xFFE6E4FF), LetterData.consonants.size, ActivityTracker.learned("consonants"), onNavigateToConsonants),
            Island("guninthakshari", com.example.data.Lang.islandName("guninthakshari"), com.example.data.Lang.script("ಕಾ"), HomeCoral, Color(0xFFFFE1E1), LetterData.consonants.size, ActivityTracker.learned("guninthakshari"), onNavigateToGunninthakshari),
            Island("numbers", com.example.data.Lang.islandName("numbers"), com.example.data.Lang.script("೧"), Color(0xFFFF9F1C), Color(0xFFFFE6CC), LetterData.numbers.size, ActivityTracker.learned("numbers"), onNavigateToNumbers),
            Island("ottakshara", com.example.data.Lang.islandName("ottakshara"), if (com.example.data.Lang.isHindi) "क्ष" else "ಕ್ಕ", Color(0xFF14A098), Color(0xFFD3F4EF), com.example.data.OttaksharaData.letters.size, ActivityTracker.learned("ottakshara"), onNavigateToOttakshara)
        ).map { it.copy(state = Access.island(it.type)) }
    }
    val streak = remember(version) { ActivityTracker.streakDays() }
    // The island to work on now: the first one that isn't finished (or the last one when all are done)
    // Free users: the first island whose free letter isn't done yet; Gold: the first open, unfinished island
    val currentIndex = islands.indexOfFirst {
        it.state == Access.State.Open && (if (premium) !it.finished else 0 !in it.learned)
    }.let { if (it < 0) islands.indexOfFirst { i -> i.state == Access.State.Open && !i.finished }.coerceAtLeast(0) else it }
    val current = islands[currentIndex]
    val nextIdx = current.nextIndex
    val nextState = remember(current.type, nextIdx, version, pathVersion, sub) { Access.letter(current.type, nextIdx) }
    val nextGlyph = when (current.type) {
        "vowels" -> LetterData.vowels.getOrNull(nextIdx)?.character
        "consonants", "guninthakshari" -> LetterData.consonants.getOrNull(nextIdx)?.character
        "ottakshara" -> com.example.data.OttaksharaData.letters.getOrNull(nextIdx)?.glyph
        else -> LetterData.numbers.getOrNull(nextIdx)?.character
    } ?: current.glyph
    val openNext: () -> Unit = {
        com.example.ui.audio.SoundEffectsHelper.playButtonTap()
        when (nextState) {
            // Ka-kaa lessons are picked from their own chart (consonant + vowel sign)
            Access.State.Open -> if (current.type == "guninthakshari" || current.type == "ottakshara") current.open() else onOpenLetter(current.type, nextIdx)
            Access.State.NeedsPremium -> Access.askGrownUp("$nextGlyph and all the other letters are in the Gold pack.")
            Access.State.NeedsProgress -> Access.showHint(Access.islandHint(current.type).ifEmpty { "Keep going!" })
        }
    }
    // "New island opened!" once, when the child unlocks Consonants / Ka-kaa by learning
    val celebrate = remember(version, pathVersion, sub) { Access.newlyOpenedIsland() }

    // Everything fits on one screen (no scrolling): the top bar, then the Play TV fills the rest.
    Box(Modifier.fillMaxSize().background(HomeSky)) {
        val side = 16.dp
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 8.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeTopBar(
                coins = coins,
                streak = streak,
                onProfile = onNavigateToProfile,
                onPoints = onNavigateToLeaderboard,
                modifier = Modifier.padding(horizontal = side)
            )
            // Play TV: the TV shows the chosen section; Watch / Play / Learn below; sections are cartridges
            PlayTvHome(
                islands = islands,
                currentIndex = currentIndex,
                nextGlyph = nextGlyph,
                nextState = nextState,
                onNext = openNext,
                onWatch = { island ->
                    val folder = watchFolderFor(island.type)
                    if (folder != null) onOpenVideos(folder) else onSeeAllVideos()
                },
                onPlay = onSeeAllGames,
                modifier = Modifier
                    .padding(horizontal = side)
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
        if (celebrate != null) {
            IslandOpenedCard(
                type = celebrate,
                onGo = {
                    Access.markCelebrated(celebrate)
                    islands.firstOrNull { it.type == celebrate }?.open?.invoke()
                },
                onLater = { Access.markCelebrated(celebrate) }
            )
        }
    }
}

@Composable
private fun HomeTopBar(coins: Int, streak: Int, onProfile: () -> Unit, onPoints: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(HomeSun)
                .border(3.dp, Color.White, CircleShape)
                .clickable { onProfile() }
                .testTag("home_profile"),
            contentAlignment = Alignment.Center
        ) {
            Text(com.example.data.Lang.script("ಅ"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk)
        }
        Column(Modifier.weight(1f)) {
            Text("${com.example.data.Lang.hello} Hello,", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HomeInkSoft, maxLines = 1)
            Text("Little learner", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        HomePill(onClick = onPoints) {
            com.example.ui.progress.StarIcon(16.dp, Color(0xFFFFB300))
            Text(formatPoints(coins), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk)
        }
        HomePill {
            Text("🔥", fontSize = 14.sp)
            Text("$streak", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk)
        }
    }
}

private fun formatPoints(n: Int): String =
    n.toString().reversed().chunked(3).joinToString(",").reversed()

@Composable
private fun HomePill(onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

private val LockedPlanet = Color(0xFF3D4A66)
@Composable
fun BentoBottomNav(
    currentRoute: String,
    isAdmin: Boolean = false,
    onNavigate: (String) -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 6.dp, horizontal = if (isAdmin) 2.dp else 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isActive = currentRoute == "home" || currentRoute == "home/",
                onClick = { onNavigate("home") }
            )
            NavItem(
                icon = Icons.Default.Games,
                label = "Games",
                isActive = currentRoute.startsWith("game"),
                onClick = { onNavigate("games") }
            )
            NavItem(
                icon = Icons.Default.SmartDisplay, 
                label = "Fun Videos", 
                isActive = currentRoute.startsWith("fun_videos"), 
                onClick = { onNavigate("fun_videos") }
            )
            NavItem(
                icon = Icons.Default.MenuBook,
                label = "Letter Box",
                isActive = currentRoute.startsWith("reference"),
                onClick = { onNavigate("reference") }
            )
            NavItem(
                icon = Icons.Default.Dashboard,
                label = "Dashboard",
                isActive = currentRoute == "dashboard",
                onClick = { onNavigate("dashboard") }
            )
            if (isAdmin) {
                NavItem(
                    icon = Icons.Default.AdminPanelSettings,
                    label = "Admin",
                    isActive = currentRoute == "admin",
                    onClick = { onNavigate("admin") }
                )
            }
        }
    }
}

@Composable
fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = if (isActive) Color(0xFFFF4081) else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.White else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isActive) Color(0xFFFF4081) else Color.Gray
        )
    }
}


/** The Fun Videos folder that goes with a section, or null (then all Fun Videos open). */
private fun watchFolderFor(type: String): String? {
    val folder = when (type) {
        "vowels" -> "Vowels"
        "consonants" -> "Consonants"
        "numbers" -> "Numbers"
        else -> null
    } ?: return null
    return if (com.example.data.Lang.isHindi && folder !in hindiVideoFolders) null else folder
}

private val TvBody = Color(0xFF5C6BC0)
private val TvBodyDark = Color(0xFF3F4A9A)
private val TvRoom = Color(0xFFFFE5EC)

/**
 * Landing page "Play TV": a big cartoon TV shows one section (its letter, name, progress and the next
 * letter). Three big buttons under it: Watch (the section's videos), Play (games), Learn (the next
 * letter). The sections sit on a shelf as cartridges; tapping one shows it on the TV.
 */
@Composable
private fun PlayTvHome(
    islands: List<Island>,
    currentIndex: Int,
    nextGlyph: String,
    nextState: Access.State,
    onNext: () -> Unit,
    onWatch: (Island) -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by androidx.compose.runtime.saveable.rememberSaveable(currentIndex) { androidx.compose.runtime.mutableIntStateOf(currentIndex) }
    val island = islands.getOrNull(selected) ?: islands[currentIndex]
    val isCurrent = selected == currentIndex
    val anim = rememberInfiniteTransition(label = "tv")
    val wiggle by anim.animateFloat(-6f, 6f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "wiggle")
    val bounce by anim.animateFloat(1f, 1.06f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bounce")
    val glow by anim.animateFloat(0.35f, 0.9f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "glow")
    val learn: () -> Unit = {
        com.example.ui.audio.SoundEffectsHelper.playButtonTap()
        when {
            island.state == Access.State.NeedsProgress -> Access.showHint(Access.islandHint(island.type))
            isCurrent -> onNext()
            else -> island.open()
        }
    }

    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(TvRoom, Color(0xFFFFD1DC))))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        val w = maxWidth
        val h = maxHeight
        val buttonSize = ((w - 24.dp) / 3).coerceAtMost(104.dp).coerceAtMost(h * 0.2f)
        val cartH = (h * 0.11f).coerceIn(44.dp, 62.dp)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            // ---- The TV ----
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                // Antennae
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width / 2
                    val top = 2.dp.toPx()
                    rotate(wiggle, pivot = Offset(cx, 26.dp.toPx())) {
                        drawLine(TvBodyDark, Offset(cx - 6.dp.toPx(), 26.dp.toPx()), Offset(cx - 46.dp.toPx(), top + 6.dp.toPx()), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(TvBodyDark, Offset(cx + 6.dp.toPx(), 26.dp.toPx()), Offset(cx + 46.dp.toPx(), top + 6.dp.toPx()), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
                        drawCircle(HomeCoral, 6.dp.toPx(), Offset(cx - 46.dp.toPx(), top + 6.dp.toPx()))
                        drawCircle(HomeSun, 6.dp.toPx(), Offset(cx + 46.dp.toPx(), top + 6.dp.toPx()))
                    }
                }
                Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .shadow(0.dp, RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .background(TvBodyDark)
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(TvBody)
                            .padding(12.dp)
                    ) {
                        // The screen: tap it to learn
                        val screen = if (island.state == Access.State.NeedsProgress) LockedPlanet else island.color
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    androidx.compose.ui.graphics.Brush.radialGradient(
                                        listOf(lerpColor(screen, Color.White, 0.35f), screen),
                                        center = Offset(120f, 80f), radius = 900f
                                    )
                                )
                                .clickable { learn() }
                                .testTag("home_tv_screen"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    island.glyph,
                                    fontSize = 72.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                                    modifier = Modifier.scale(if (isCurrent) bounce else 1f)
                                )
                                Text(island.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
                                Text(com.example.data.Lang.islandNative(island.type), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f), maxLines = 1)
                                if (island.state != Access.State.NeedsProgress) {
                                    Box(Modifier.width(150.dp).height(9.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.3f))) {
                                        val f = if (island.total == 0) 0f else island.done.toFloat() / island.total
                                        Box(Modifier.fillMaxHeight().fillMaxWidth(f.coerceIn(0f, 1f)).clip(RoundedCornerShape(5.dp)).background(HomeSun))
                                    }
                                    Text("${island.done} / ${island.total}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                            }
                            // Corner badge: Next letter / finished / locked
                            Row(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.95f))
                                    .clickable { learn() }
                                    .padding(start = 10.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
                                    .testTag("home_next_letter"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                when {
                                    island.state == Access.State.NeedsProgress -> { Text("Locked", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk); LockBadge(size = 22.dp) }
                                    isCurrent -> {
                                        Text("Next:", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk)
                                        Text(nextGlyph, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk)
                                        if (nextState == Access.State.NeedsPremium) CrownBadge(size = 24.dp)
                                        else Box(Modifier.size(24.dp).background(HomeCoral, CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Start the next letter", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    island.finished -> { Text("Done", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk); com.example.ui.progress.StarIcon(20.dp, HomeSun) }
                                    else -> { Text("Open", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk)
                                        Box(Modifier.size(24.dp).background(HomeCoral, CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Open ${island.name}", tint = Color.White, modifier = Modifier.size(16.dp))
                                        } }
                                }
                            }
                        }
                    }
                    // Stand
                    Box(Modifier.width(110.dp).height(12.dp).clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)).background(TvBodyDark))
                }
            }
            Spacer(Modifier.height(10.dp))
            // ---- Watch / Play / Learn ----
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TvButton("🎬", "Watch", Color(0xFFFF7043), buttonSize, "home_tv_watch") {
                    com.example.ui.audio.SoundEffectsHelper.playButtonTap(); onWatch(island)
                }
                TvButton("🎮", "Play", Color(0xFFAB47BC), buttonSize, "home_tv_play") {
                    com.example.ui.audio.SoundEffectsHelper.playButtonTap(); onPlay()
                }
                TvButton("📖", "Learn", Color(0xFF26A69A), buttonSize, "home_tv_learn") { learn() }
            }
            Spacer(Modifier.height(12.dp))
            // ---- Cartridge shelf ----
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                islands.forEachIndexed { i, isl ->
                    val locked = isl.state == Access.State.NeedsProgress
                    val on = i == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .height(cartH)
                            .graphicsLayer { translationY = if (on) -6.dp.toPx() else 0f }
                            .then(if (on) Modifier.border(3.dp, HomeSun.copy(alpha = if (i == currentIndex) glow else 1f), RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp)) else Modifier)
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                            .background(if (locked) LockedPlanet.copy(alpha = 0.55f) else isl.color)
                            .clickable {
                                com.example.ui.audio.SoundEffectsHelper.playButtonTap()
                                selected = i
                            }
                            .testTag("home_island_${isl.type}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(isl.glyph, fontSize = (cartH.value * 0.4f).sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
                        when {
                            locked -> LockBadge(Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp), size = 18.dp)
                            isl.finished -> com.example.ui.progress.StarIcon(18.dp, HomeSun, Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp))
                            i == currentIndex && !on -> Box(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp).size(10.dp).background(HomeCoral, CircleShape))
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFA1887F)))
            Spacer(Modifier.height(4.dp))
            Text("Pick a cartridge to change the lesson", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF8E3B5A))
        }
    }
}

@Composable
private fun TvButton(emoji: String, label: String, color: Color, size: Dp, tag: String, onClick: () -> Unit) {
    Column(
        Modifier
            .size(size)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() }
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = (size.value * 0.3f).sp)
        Text(label, fontSize = (size.value * 0.17f).sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    }
}
