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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.ui.progress.GameRegistry
import com.example.ui.components.GameArt
import com.example.resources.Res
import com.example.resources.thumb_animals
import com.example.resources.thumb_birds
import com.example.resources.thumb_body_parts
import com.example.resources.thumb_classroom
import com.example.resources.thumb_colours
import com.example.resources.thumb_consonants
import com.example.resources.thumb_family
import com.example.resources.thumb_fruits
import com.example.resources.thumb_how_many
import com.example.resources.thumb_letter_quiz
import com.example.resources.thumb_number_quiz
import com.example.resources.thumb_numbers
import com.example.resources.thumb_picture_quiz
import com.example.resources.thumb_playtime
import com.example.resources.thumb_vegetables
import com.example.resources.thumb_vowels
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import com.example.ui.progress.lerpColor

/*
 * Home (after sign-in): greeting + points + streak, then a streaming-style page: a banner for the
 * chosen section (Continue learning / Next letter, its videos and games), "My learning path" with
 * every section in order (Vowels → Consonants → Ka-kaa → Numbers → Ottakshara), then a row of video
 * thumbnails and a row of game cards. All parts share the same side margins.
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
    // When the free trial runs out, say so once (with the way to the plans, behind the grown-up check)
    LaunchedEffect(sub.trialEnded) {
        if (sub.trialEnded && !Access.isPremium()) {
            val prefs = com.example.platform.getSharedPreferences("SubscriptionTrial")
            val key = "trial_end_shown_${com.example.platform.PlatformAuth.currentUser()?.uid ?: "guest"}"
            if (!prefs.getBoolean(key, false)) {
                prefs.edit().putBoolean(key, true).apply()
                Access.askGrownUp("Your 15 days of free Gold have finished. We hope you loved it! Choose a plan to keep every letter, game and video open.")
            }
        }
    }
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
                goldLabel = when {
                    sub.inTrial -> "Gold trial · ${sub.trialDaysLeft} day${if (sub.trialDaysLeft == 1) "" else "s"}"
                    !premium -> "Get Gold"
                    else -> null
                },
                goldMessage = if (sub.inTrial) "Your free Gold trial ends on ${com.example.platform.formatDate(sub.trialUntil)}. Choose a plan to keep every letter, game and video open."
                    else "Unlock every letter, game, video and worksheet with Gold.",
                modifier = Modifier.padding(horizontal = side)
            )
            // Streaming-style home: section banner, learning path, video and game rows
            StreamHome(
                islands = islands,
                currentIndex = currentIndex,
                nextGlyph = nextGlyph,
                nextState = nextState,
                onNext = openNext,
                onOpenGame = onOpenGame,
                onSeeAllGames = onSeeAllGames,
                onOpenVideos = onOpenVideos,
                onSeeAllVideos = onSeeAllVideos,
                side = side,
                modifier = Modifier.fillMaxWidth().weight(1f)
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
private fun HomeTopBar(coins: Int, streak: Int, onProfile: () -> Unit, onPoints: () -> Unit, goldLabel: String? = null, goldMessage: String = "", modifier: Modifier = Modifier) {
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
            Text("${com.example.data.Lang.hello} Hello,", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = HomeInkSoft, maxLines = 1)
            val learner by com.example.ui.data.LearnerName.name.collectAsState()
            LaunchedEffect(Unit) { com.example.ui.data.LearnerName.load() }
            // The child's own name in rainbow (VIBGYOR) letters; "Little learner" when none is set
            if (learner.isBlank()) {
                Text("Little learner", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text(rainbowText(learner), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (goldLabel != null) {
            // Free users: always-visible way to Gold. Goes through the grown-ups check first (Families policy).
            Row(
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFFFC93C))
                    .border(2.dp, Color.White, RoundedCornerShape(18.dp))
                    .clickable { Access.askGrownUp(goldMessage) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("home_get_gold"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("👑", fontSize = 14.sp)
                Text(goldLabel, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF5A3A00), maxLines = 1)
            }
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
                .padding(vertical = 6.dp, horizontal = 4.dp),
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


/** Picture for each Fun Videos folder. */
private val folderThumbs: Map<String, DrawableResource> = mapOf(
    "Fruits" to Res.drawable.thumb_fruits,
    "Vegetables" to Res.drawable.thumb_vegetables,
    "Body Parts" to Res.drawable.thumb_body_parts,
    "Family" to Res.drawable.thumb_family,
    "Animals" to Res.drawable.thumb_animals,
    "Birds" to Res.drawable.thumb_birds,
    "Colours" to Res.drawable.thumb_colours,
    "Vowels" to Res.drawable.thumb_vowels,
    "Consonants" to Res.drawable.thumb_consonants,
    "Numbers" to Res.drawable.thumb_numbers,
    "Letter Quiz" to Res.drawable.thumb_letter_quiz,
    "Picture Quiz" to Res.drawable.thumb_picture_quiz,
    "Number Quiz" to Res.drawable.thumb_number_quiz,
    "How Many?" to Res.drawable.thumb_how_many,
    "Class Room" to Res.drawable.thumb_classroom,
    "Play Time" to Res.drawable.thumb_playtime
)

/** "Continue watching" on Home: Vowels, Consonants and Birds first, then the rest (tablets show more). */
private val homeFunVideos = listOf("Vowels", "Consonants", "Birds", "Fruits", "Vegetables", "Body Parts", "Family", "Animals", "Colours")

/** "Games for you" on Home: Letter Runner, Musical Balloon Pop and Colour Sort first. */
private val homeGames = listOf(26, 28, 24, 25, 6, 12, 13)

/** A section's own videos and games (first ones are the closest match). */
private fun sectionVideos(type: String): List<String> = when (type) {
    "vowels" -> listOf("Vowels", "Letter Quiz", "Picture Quiz")
    "consonants" -> listOf("Consonants", "Letter Quiz", "Picture Quiz")
    "numbers" -> listOf("Numbers", "How Many?", "Number Quiz")
    else -> emptyList()
}

private fun sectionGames(type: String): List<Int> = when (type) {
    "vowels" -> listOf(1, 20, 19, 11, 12, 6)
    "consonants" -> listOf(2, 21, 11, 22, 23, 13)
    "guninthakshari" -> listOf(3, 8, 9, 21, 22, 23)
    "numbers" -> listOf(27, 4, 12, 13, 23, 6)
    else -> listOf(8, 9, 22, 21, 23, 13)
}

/** The first two letters shown on a section's banner instead of a picture (Ka-kaa, Ottakshara). */
private fun bannerLetters(type: String): List<String>? = when (type) {
    "guninthakshari" -> listOf(com.example.data.Lang.script("ಕ"), com.example.data.Lang.script("ಕಾ"))
    "ottakshara" -> com.example.data.OttaksharaData.letters.take(2).map { it.glyph }.takeIf { it.size == 2 }
    else -> null
}

/** Picture behind a section's banner. */
private fun sectionThumb(type: String): DrawableResource = when (type) {
    "vowels" -> Res.drawable.thumb_vowels
    "numbers" -> Res.drawable.thumb_numbers
    "guninthakshari" -> Res.drawable.thumb_letter_quiz
    "ottakshara" -> Res.drawable.thumb_picture_quiz
    else -> Res.drawable.thumb_consonants
}

/**
 * Home, streaming style: a banner for the chosen section (Continue / Next letter, its videos and games),
 * "My learning path" with every section in order (progress, stars, locks; tap to choose), then a row of
 * video thumbnails and a row of game cards. All parts share the same side margins.
 */
@Composable
private fun StreamHome(
    islands: List<Island>,
    currentIndex: Int,
    nextGlyph: String,
    nextState: Access.State,
    onNext: () -> Unit,
    onOpenGame: (GameInfo) -> Unit,
    onSeeAllGames: () -> Unit,
    onOpenVideos: (String) -> Unit,
    onSeeAllVideos: () -> Unit,
    side: Dp,
    modifier: Modifier = Modifier
) {
    var selected by androidx.compose.runtime.saveable.rememberSaveable(currentIndex) { androidx.compose.runtime.mutableIntStateOf(currentIndex) }
    val island = islands.getOrNull(selected) ?: islands[currentIndex]
    val isCurrent = selected == currentIndex
    val locked = island.state == Access.State.NeedsProgress
    val hindi = com.example.data.Lang.isHindi
    val sub by com.example.ui.data.SubscriptionManager.state.collectAsState()

    // Videos: on the section in progress the fun word videos; on another section its own videos
    val own = sectionVideos(island.type).filter { !hindi || it in hindiVideoFolders }
    val videoTitle = if (!isCurrent && own.isNotEmpty()) "${island.name} videos" else "Continue watching"
    val videos = (if (!isCurrent && own.isNotEmpty()) own + homeFunVideos else homeFunVideos)
        .distinct()
        .filter { (!hindi || it in hindiVideoFolders) && it in folderThumbs }
    val games = (if (isCurrent) homeGames + sectionGames(island.type) else sectionGames(island.type))
        .distinct()
        .filter { com.example.data.Lang.hasGame(it) }
        .mapNotNull { GameRegistry.byId(it) }
    val gameTitle = if (isCurrent) "Games for you" else "${island.name} games"

    // Top half: the banner and the learning path. Bottom half: the video row and the game row.
    BoxWithConstraints(modifier.padding(bottom = 8.dp)) {
        val inner = maxWidth - side * 2
        val gap = 10.dp
        val perRow = (inner / 110.dp).toInt().coerceIn(3, 6)
        val cardW = (inner - gap * (perRow - 1)) / perRow
        val half = maxHeight / 2
        // Each row in the bottom half gets the same height: its title, then the cards. Inside a card
        // the picture takes whatever is left after the name, so the name always shows.
        val rowH = ((half - gap) / 2).coerceAtMost(34.dp + cardW + 24.dp)
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().height(half), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Swipe left / right to move between sections (the path and the rows below follow)
                val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = selected) { islands.size }
                LaunchedEffect(pager.currentPage) {
                    if (pager.currentPage != selected) {
                        selected = pager.currentPage
                        com.example.ui.audio.SoundEffectsHelper.playButtonTap()
                    }
                }
                LaunchedEffect(selected) {
                    if (pager.currentPage != selected && selected in islands.indices) pager.animateScrollToPage(selected)
                }
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pager,
                    contentPadding = PaddingValues(horizontal = side),
                    pageSpacing = 10.dp,
                    modifier = Modifier.fillMaxWidth().weight(1f).testTag("home_banner_pager")
                ) { page ->
                    val pageIsland = islands[page]
                    val pageLocked = pageIsland.state == Access.State.NeedsProgress
                    val pageVideos = sectionVideos(pageIsland.type).filter { !hindi || it in hindiVideoFolders }
                    val pageGames = (if (page == currentIndex) homeGames + sectionGames(pageIsland.type) else sectionGames(pageIsland.type))
                        .distinct().filter { com.example.data.Lang.hasGame(it) }.mapNotNull { GameRegistry.byId(it) }
                    SectionBanner(
                        island = pageIsland,
                        isCurrent = page == currentIndex,
                        nextGlyph = nextGlyph,
                        nextState = nextState,
                        onLearn = {
                            com.example.ui.audio.SoundEffectsHelper.playButtonTap()
                            // Every section opens its letter list, where the child picks a letter
                            if (pageLocked) Access.showHint(Access.islandHint(pageIsland.type)) else pageIsland.open()
                        },
                        onWatch = { pageVideos.firstOrNull()?.let(onOpenVideos) ?: onSeeAllVideos() },
                        onPlay = { pageGames.firstOrNull()?.let(onOpenGame) ?: onSeeAllGames() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                LearningPath(
                    islands = islands,
                    currentIndex = currentIndex,
                    selected = selected,
                    onSelect = { i ->
                        // Tap a section on the path: open its letter list (and keep it selected on Home)
                        selected = i
                        val isl = islands[i]
                        if (isl.state == Access.State.NeedsProgress) Access.showHint(Access.islandHint(isl.type)) else isl.open()
                    },
                    modifier = Modifier.padding(horizontal = side).fillMaxWidth()
                )
            }
            Column(
                Modifier.fillMaxWidth().height(half).padding(top = gap),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                if (videos.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth().height(rowH)) {
                        RowTitle(videoTitle, onSeeAll = onSeeAllVideos, side = side)
                        Row(Modifier.weight(1f).padding(horizontal = side), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            videos.take(perRow).forEach { folder ->
                                HomeVideoCard(
                                    folder, folderThumbs.getValue(folder), Modifier.width(cardW).fillMaxHeight(),
                                    remember(sub, folder) { Access.videoFolder(folder) }
                                ) { onOpenVideos(folder) }
                            }
                        }
                    }
                }
                if (games.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth().height(rowH)) {
                        RowTitle(gameTitle, onSeeAll = onSeeAllGames, side = side)
                        Row(Modifier.weight(1f).padding(horizontal = side), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            games.take(perRow).forEach { game ->
                                HomeGameCard(game, Modifier.width(cardW).fillMaxHeight(), remember(sub, game.id) { Access.game(game) }) { onOpenGame(game) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The chosen section: name, progress and the main button (Next letter / Learn), plus its videos and games. */
@Composable
private fun SectionBanner(
    island: Island,
    isCurrent: Boolean,
    nextGlyph: String,
    nextState: Access.State,
    onLearn: () -> Unit,
    onWatch: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locked = island.state == Access.State.NeedsProgress
    val base = if (locked) LockedSection else island.color
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(base, lerpColor(base, Color(0xFF1E2250), 0.55f))))
            .clickable { onLearn() }
            .testTag("home_banner")
    ) {
        val letters = bannerLetters(island.type)
        if (letters != null) {
            // Ka-kaa and Ottakshara: their first two letters as big tiles on the right
            Row(
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.42f).padding(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                letters.forEachIndexed { i, g ->
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(0.85f)
                            .graphicsLayer { rotationZ = if (i == 0) -6f else 5f; translationY = if (i == 0) -6f else 6f }
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = if (locked) 0.45f else 1f))
                            .border(3.dp, HomeSun, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(g, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = base, maxLines = 1, softWrap = false)
                    }
                }
            }
        } else {
            // Picture on the right, fading into the colour
            Image(
                painter = painterResource(sectionThumb(island.type)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = if (locked) 0.35f else 1f,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.55f)
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(0.42f to base, 0.62f to base.copy(alpha = 0f))
                )
            )
        }
        Column(Modifier.padding(14.dp).fillMaxWidth(0.62f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                when { locked -> "LOCKED"; isCurrent -> "CONTINUE LEARNING"; else -> "SECTION" },
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.85f)
            )
            Text(island.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (locked) Access.islandHint(island.type).ifEmpty { "Keep going to open it" } else "${island.done}/${island.total} · ${com.example.data.Lang.islandNative(island.type)}",
                fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.9f), maxLines = 2
            )
            if (!locked) {
                Box(Modifier.padding(top = 3.dp).width(130.dp).height(7.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.3f))) {
                    val f = if (island.total == 0) 0f else island.done.toFloat() / island.total
                    Box(Modifier.fillMaxHeight().fillMaxWidth(f.coerceIn(0f, 1f)).clip(RoundedCornerShape(4.dp)).background(HomeSun))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .clickable { onLearn() }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("home_next_letter"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when {
                        locked -> { LockBadge(size = 18.dp); Text("Coming up", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk) }
                        else -> {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = HomeInk, modifier = Modifier.size(18.dp))
                            Text("Learn", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk)
                        }
                    }
                }
                if (!locked) {
                    BannerChip("🎬", "Watch", "home_banner_watch", onWatch)
                    BannerChip("🎮", "Play", "home_banner_play", onPlay)
                }
            }
        }
    }
}

@Composable
private fun BannerChip(emoji: String, label: String, tag: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.28f))
            .clickable {
                com.example.ui.audio.SoundEffectsHelper.playButtonTap()
                onClick()
            }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 15.sp)
    }
}

private val LockedSection = Color(0xFF8A94A8)

/** Every section in order, joined by a line that fills in as the child moves along; tap to choose one. */
@Composable
private fun LearningPath(islands: List<Island>, currentIndex: Int, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("My learning path", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth()) {
            val circle = 46.dp
            // The line behind the circles: green up to the section in progress, dotted after it
            Canvas(Modifier.fillMaxWidth().height(circle)) {
                val n = islands.size
                val slot = size.width / n
                val y = size.height / 2
                val x0 = slot / 2
                val x1 = size.width - slot / 2
                drawLine(
                    Color(0xFFC9D3E6), Offset(x0, y), Offset(x1, y), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
                )
                if (currentIndex > 0) {
                    drawLine(Color(0xFF2EC4B6), Offset(x0, y), Offset(x0 + slot * currentIndex, y), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            Row(Modifier.fillMaxWidth()) {
                islands.forEachIndexed { i, isl ->
                    val locked = isl.state == Access.State.NeedsProgress
                    val on = i == selected
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                com.example.ui.audio.SoundEffectsHelper.playButtonTap()
                                onSelect(i)
                            }
                            .testTag("home_island_${isl.type}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .size(circle)
                                    .then(if (on) Modifier.border(3.dp, HomeSun, CircleShape) else Modifier)
                                    .padding(if (on) 3.dp else 0.dp)
                                    .clip(CircleShape)
                                    .background(if (locked) Color(0xFFB8C2D3) else isl.color)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(isl.glyph, fontSize = if (isl.glyph.length > 1) 15.sp else 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
                            }
                            when {
                                locked -> LockBadge(Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp), size = 18.dp)
                                isl.finished -> com.example.ui.progress.StarIcon(18.dp, HomeSun, Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp))
                                i == currentIndex && !on -> Box(Modifier.align(Alignment.TopEnd).size(10.dp).background(HomeCoral, CircleShape))
                            }
                        }
                        Text(isl.name, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (locked) "Locked" else "${isl.done}/${isl.total}", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = HomeInkSoft, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowTitle(title: String, onSeeAll: () -> Unit, side: Dp) {
    Row(Modifier.fillMaxWidth().padding(horizontal = side), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk, modifier = Modifier.weight(1f))
        Text(
            "See all",
            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6C63FF),
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onSeeAll() }.padding(vertical = 4.dp)
        )
    }
}

/** A game card: its drawn picture (same art as the Games tab) with the name below. */
@Composable
private fun HomeGameCard(game: GameInfo, modifier: Modifier = Modifier, state: Access.State = Access.State.Open, onClick: () -> Unit) {
    HomeCard(
        label = game.name,
        modifier = modifier.testTag("home_game_${game.id}"),
        state = state,
        onClick = onClick
    ) {
        // The art is drawn for a ~150x112dp box; scale it to whatever size the card has
        BoxWithConstraints(
            Modifier.fillMaxSize().background(lerpColor(game.color, Color.White, 0.82f)),
            contentAlignment = Alignment.Center
        ) {
            val scale = minOf(maxWidth / 150.dp, maxHeight / 112.dp)
            Box(Modifier.requiredSize(150.dp, 112.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
                GameArt(
                    kind = artFor(game),
                    color = game.color,
                    background = lerpColor(game.color, Color.White, 0.82f),
                    glyph = game.glyph,
                    ink = game.ink
                )
            }
        }
    }
}

/** A Fun Videos folder card: its picture with a play button, the name below. */
@Composable
private fun HomeVideoCard(folder: String, photo: DrawableResource, modifier: Modifier = Modifier, state: Access.State = Access.State.Open, onClick: () -> Unit) {
    HomeCard(
        label = folder,
        modifier = modifier.testTag("home_video_$folder"),
        state = state,
        onClick = onClick
    ) {
        Image(
            painter = painterResource(photo),
            contentDescription = "$folder videos",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.align(Alignment.BottomStart).padding(6.dp).size(26.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

/** Shared card frame: rounded picture on top, one-line name below. */
@Composable
private fun HomeCard(label: String, modifier: Modifier, state: Access.State = Access.State.Open, onClick: () -> Unit, picture: @Composable BoxScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                com.example.ui.audio.SoundEffectsHelper.playButtonTap()
                onClick()
            },
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(lerpColor(HomeInk, Color.White, 0.8f))
        ) {
            picture()
            AccessBadge(state, Modifier.align(Alignment.TopEnd).padding(5.dp), size = 24.dp)
        }
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
