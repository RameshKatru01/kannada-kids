package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import com.example.ui.components.GameArt
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LetterData
import com.example.resources.Res
import com.example.resources.thumb_animals
import com.example.resources.thumb_birds
import com.example.resources.thumb_colours
import com.example.resources.thumb_consonants
import com.example.resources.thumb_numbers
import com.example.resources.thumb_vowels
import com.example.ui.data.Access
import com.example.ui.data.ActivityTracker
import com.example.ui.data.ScoreManager
import com.example.ui.progress.GameInfo
import com.example.ui.progress.GameRegistry
import com.example.ui.progress.lerpColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/*
 * Home (after sign-in): greeting + points + streak, the letter-islands map (Vowels → Consonants →
 * Ka-kaa → Numbers, each with a progress ring and a "Next letter" button), then a row of games and
 * a row of Fun Videos.
 */

private val HomeInk = Color(0xFF0B3B66)
private val HomeInkSoft = Color(0xFF3A5F82)
private val HomeSky = Color(0xFFEAF6FF)
private val MapWater = Color(0xFFBDE6FF)
private val MapHill = Color(0xFF8FD694)
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

    // Everything fits on one screen (no scrolling): the two shelves take a fixed height and the
    // map gets whatever is left. All three share the same 16dp side margins so their edges line up.
    BoxWithConstraints(Modifier.fillMaxSize().background(HomeSky)) {
        val side = 16.dp
        val gap = 10.dp
        val inner = maxWidth - side * 2
        // 3 cards per row on phones, more on tablets
        val perRow = (inner / 120.dp).toInt().coerceIn(3, 6)
        val cardWidth = (inner - gap * (perRow - 1)) / perRow
        val pictureHeight = (cardWidth * 0.66f).coerceAtMost(130.dp)
        val picks = remember { listOf(1, 21, 23, 22, 12, 13).mapNotNull { GameRegistry.byId(it) } }
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
            IslandMap(
                islands = islands,
                currentIndex = currentIndex,
                nextGlyph = nextGlyph,
                nextState = nextState,
                onNext = openNext,
                modifier = Modifier
                    .padding(horizontal = side)
                    .fillMaxWidth()
                    .weight(1f)
            )
            ShelfTitle("Games", onSeeAll = onSeeAllGames, side = side)
            Row(Modifier.fillMaxWidth().padding(horizontal = side), horizontalArrangement = Arrangement.spacedBy(gap)) {
                picks.take(perRow).forEach { game ->
                    HomeGameCard(game, pictureHeight, Modifier.width(cardWidth), remember(sub) { Access.game(game) }) { onOpenGame(game) }
                }
            }
            // Fun Videos are in Kannada; Hindi videos come later
            if (!com.example.data.Lang.isHindi) {
            ShelfTitle("Fun Videos", onSeeAll = onSeeAllVideos, side = side)
            Row(Modifier.fillMaxWidth().padding(horizontal = side), horizontalArrangement = Arrangement.spacedBy(gap)) {
                homeVideos.take(perRow).forEach { (folder, photo) ->
                    HomeVideoCard(folder, photo, pictureHeight, Modifier.width(cardWidth), remember(sub) { Access.videoFolder(folder) }) { onOpenVideos(folder) }
                }
            }
            }
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

private val homeVideos: List<Pair<String, DrawableResource>> = listOf(
    "Vowels" to Res.drawable.thumb_vowels,
    "Consonants" to Res.drawable.thumb_consonants,
    "Animals" to Res.drawable.thumb_animals,
    "Birds" to Res.drawable.thumb_birds,
    "Colours" to Res.drawable.thumb_colours,
    "Numbers" to Res.drawable.thumb_numbers
)

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

/** Where each planet sits in space, as fractions of the box (centre of the planet). */
private val islandSpots = listOf(Offset(0.20f, 0.80f), Offset(0.76f, 0.71f), Offset(0.40f, 0.48f), Offset(0.81f, 0.36f), Offset(0.51f, 0.14f))

private val SpaceTop = Color(0xFF3B3F8F)
private val SpaceDeep = Color(0xFF141A3A)
private val LockedPlanet = Color(0xFF3D4A66)
private val Glow = Color(0xFFFFC93C)

/** Fixed "random" star positions (fractions of the box) so the sky doesn't jump around. */
private val spaceStars: List<Triple<Float, Float, Float>> = (0 until 60).map { i ->
    Triple(((i * 73) % 354) / 354f, ((i * 41) % 340) / 340f, 1f + (i % 3) * 0.6f)
}

/**
 * Space trip: each learning section is a planet, joined by a dotted flight path. Finished planets
 * get a gold star, the current one glows ("Land!") with a rocket flying to it, locked ones are dark
 * with a padlock. A "Next" button sits in the corner.
 */
@Composable
private fun IslandMap(islands: List<Island>, currentIndex: Int, nextGlyph: String, nextState: Access.State, onNext: () -> Unit, modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "space")
    val glow by anim.animateFloat(0.25f, 0.6f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val bob by anim.animateFloat(-4f, 4f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val dash by anim.animateFloat(0f, 40f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "dash")
    val twinkle by anim.animateFloat(0.3f, 1f, infiniteRepeatable(tween(1700), RepeatMode.Reverse), label = "twinkle")

    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(SpaceDeep)
    ) {
        val w = maxWidth
        val h = maxHeight
        val base = (minOf(w, h) * 0.10f).coerceIn(24.dp, 44.dp)   // planet radius

        // Sky, stars and the flight path
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    listOf(SpaceTop, SpaceDeep),
                    center = Offset(size.width * 0.8f, size.height * 0.1f),
                    radius = size.width * 1.1f
                )
            )
            spaceStars.forEachIndexed { i, (fx, fy, r) ->
                val a = if (i % 4 == 0) twinkle else 0.55f + (i % 3) * 0.15f
                drawCircle(Color.White.copy(alpha = a.coerceIn(0f, 1f)), radius = r * density, center = Offset(fx * size.width, fy * size.height))
            }
            val pts = islandSpots.map { Offset(it.x * size.width, it.y * size.height) }
            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 1 until pts.size) {
                    val p0 = pts[i - 1]
                    val p1 = pts[i]
                    val midY = (p0.y + p1.y) / 2
                    cubicTo(p0.x, midY, p1.x, midY, p1.x, p1.y)
                }
            }
            drawPath(
                path, Color.White.copy(alpha = 0.45f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 10.dp.toPx()), phase = -dash))
            )
        }

        islands.forEachIndexed { i, island ->
            val isCurrent = i == currentIndex
            val locked = island.state == Access.State.NeedsProgress
            val r = if (isCurrent) base * 1.3f else base
            val spot = islandSpots[i]
            val boxW = 140.dp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .width(boxW)
                    .offset(x = w * spot.x - boxW / 2, y = h * spot.y - r)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        com.example.ui.audio.SoundEffectsHelper.playButtonTap()
                        if (locked) Access.showHint(Access.islandHint(island.type)) else island.open()
                    }
                    .testTag("home_island_${island.type}")
            ) {
                Box(Modifier.size(r * 2), contentAlignment = Alignment.Center) {
                    val color = if (locked) LockedPlanet else island.color
                    val fraction = if (island.total == 0) 0f else island.done.toFloat() / island.total
                    Canvas(Modifier.fillMaxSize()) {
                        val rad = size.minDimension / 2
                        // Glow around the current planet
                        if (isCurrent) drawCircle(Glow.copy(alpha = glow), radius = rad * 1.25f)
                        // Saturn-style ring on the 2nd planet
                        if (i == 1 && !locked) {
                            rotate(-14f) {
                                drawOval(Glow, topLeft = Offset(-rad * 0.35f, rad * 0.82f), size = Size(rad * 2.7f, rad * 0.36f), style = Stroke(2.5f.dp.toPx()))
                            }
                        }
                        drawCircle(color, radius = rad)
                        // Shine
                        drawCircle(
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                                center = Offset(rad * 0.7f, rad * 0.6f), radius = rad
                            ),
                            radius = rad
                        )
                        // Progress around the planet
                        if (!locked && fraction > 0f) {
                            val sw = 3.dp.toPx()
                            drawArc(
                                Color.White.copy(alpha = 0.9f), startAngle = -90f, sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                                useCenter = false, topLeft = Offset(sw, sw), size = Size(size.width - 2 * sw, size.height - 2 * sw),
                                style = Stroke(width = sw, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Text(island.glyph, fontSize = (r.value * 0.72f).sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    when {
                        locked -> LockBadge(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp), size = 24.dp)
                        isCurrent -> Text(
                            "Land!",
                            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 12.dp, y = (-6).dp)
                                .background(HomeCoral, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 1.dp)
                        )
                        island.finished -> com.example.ui.progress.StarIcon(
                            24.dp, Color(0xFFFFC93C),
                            Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)
                        )
                    }
                }
                Text(
                    if (locked) island.name else "${island.name} · ${island.done}/${island.total}",
                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 1.dp)
                )
            }
        }

        // Rocket flying towards the current planet
        islandSpots.getOrNull(currentIndex)?.let { spot ->
            Text(
                "🚀",
                fontSize = 30.sp,
                modifier = Modifier
                    .offset(x = w * spot.x + base * 1.3f + 4.dp, y = h * spot.y - base * 0.4f + bob.dp)
                    .graphicsLayer { rotationZ = -35f }
            )
        }

        // "Next letter" button
        Row(
            Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .shadow(4.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.95f))
                .clickable { onNext() }
                .padding(start = 12.dp, end = 5.dp, top = 5.dp, bottom = 5.dp)
                .testTag("home_next_letter"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Next:", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk)
            Text(nextGlyph, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk)
            if (nextState == Access.State.NeedsPremium) {
                CrownBadge(size = 30.dp)
            } else {
                Box(Modifier.size(30.dp).background(HomeCoral, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start the next letter", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ProgressRing(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 8.dp.toPx()
        drawCircle(Color.White)
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * fraction.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun ShelfTitle(title: String, onSeeAll: () -> Unit, side: Dp = 16.dp) {
    Row(
        Modifier.fillMaxWidth().padding(start = side, end = side - 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk, modifier = Modifier.weight(1f))
        Text(
            "See all",
            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onSeeAll() }.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

/** A game card: its drawn picture (same art as the Games tab) with the name below, like a video card. */
@Composable
private fun HomeGameCard(game: GameInfo, pictureHeight: Dp, modifier: Modifier = Modifier, state: Access.State = Access.State.Open, onClick: () -> Unit) {
    HomeCard(
        label = game.name,
        pictureHeight = pictureHeight,
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
            Box(
                Modifier
                    .requiredSize(150.dp, 112.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
            ) {
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

@Composable
private fun HomeVideoCard(folder: String, photo: DrawableResource, pictureHeight: Dp, modifier: Modifier = Modifier, state: Access.State = Access.State.Open, onClick: () -> Unit) {
    HomeCard(
        label = folder,
        pictureHeight = pictureHeight,
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
            Modifier.align(Alignment.BottomEnd).padding(6.dp).size(24.dp).background(HomeInk.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

/** Shared card frame for the Home shelves: rounded picture on top, one-line name below. */
@Composable
private fun HomeCard(label: String, pictureHeight: Dp, modifier: Modifier, state: Access.State = Access.State.Open, onClick: () -> Unit, picture: @Composable BoxScope.() -> Unit) {
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
                .height(pictureHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(lerpColor(HomeInk, Color.White, 0.8f))
        ) {
            picture()
            AccessBadge(state, Modifier.align(Alignment.TopEnd).padding(5.dp), size = 26.dp)
        }
        Text(
            label,
            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = HomeInk,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
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
