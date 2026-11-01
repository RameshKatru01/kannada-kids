package com.example.ui.screens

import com.example.platform.*
import com.example.ui.components.readableWidth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.audio.SoundEffectsHelper
import com.example.ui.components.ScrapbookInk
import com.example.ui.components.ScrapbookInkSoft
import com.example.ui.components.ScrapbookPaper
import com.example.ui.data.Access
import kotlinx.coroutines.delay

/*
 * Screens and badges for locks: the 🔒 / 👑 badges, the "Ask a grown-up" card (→ grown-ups
 * check → plans), the "Not yet!" hint, the "New island opened!" card and Parent corner →
 * Learning path.
 */

private val LockGrey = Color(0xFF5B7189)
private val CrownGold = Color(0xFFFFC93C)
private val CrownSoft = Color(0xFFFFF1C4)

/** Small round badge with a padlock (not reached yet). */
@Composable
fun LockBadge(modifier: Modifier = Modifier, size: Dp = 26.dp) {
    Box(
        modifier.size(size).shadow(2.dp, CircleShape).background(Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = LockGrey, modifier = Modifier.size(size * 0.6f))
    }
}

/** Small round badge with a crown (Gold pack). */
@Composable
fun CrownBadge(modifier: Modifier = Modifier, size: Dp = 26.dp) {
    Box(
        modifier.size(size).shadow(2.dp, CircleShape).background(CrownSoft, CircleShape).border(2.dp, CrownGold, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("👑", fontSize = (size.value * 0.5f).sp)
    }
}

/** Badge for a lock state, or nothing when open. */
@Composable
fun AccessBadge(state: Access.State, modifier: Modifier = Modifier, size: Dp = 26.dp) {
    when (state) {
        Access.State.NeedsProgress -> LockBadge(modifier, size)
        Access.State.NeedsPremium -> CrownBadge(modifier, size)
        Access.State.Open -> Unit
    }
}

/**
 * Shows [Access.prompt] on top of everything: "Not yet!" hints go away by themselves; the Gold card
 * has "Ask a grown-up" → grown-ups check → [onOpenPlans].
 */
@Composable
fun AccessPromptHost(onOpenPlans: () -> Unit) {
    val prompt by Access.prompt.collectAsState()
    var gate by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val p = prompt

    if (p != null && !gate) {
        LaunchedEffect(p) {
            if (p.premium) SoundEffectsHelper.playSparkle(context) else SoundEffectsHelper.playPop(context)
            if (!p.premium) {
                delay(3200)
                Access.dismissPrompt()
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x990B3B66))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { Access.dismissPrompt() }
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            val pop by animateFloatAsState(1f, spring(dampingRatio = 0.5f), label = "pop")
            Column(
                Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .scale(pop)
                    .background(Color.White, RoundedCornerShape(30.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    .padding(22.dp)
                    .testTag(if (p.premium) "gold_prompt" else "lock_hint"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(84.dp).background(if (p.premium) CrownSoft else Color(0xFFE6E4FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (p.premium) Text("👑", fontSize = 42.sp)
                    else Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF6C63FF), modifier = Modifier.size(42.dp))
                }
                Text(p.title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk, textAlign = TextAlign.Center)
                Text(p.message, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft, textAlign = TextAlign.Center)
                if (p.premium) {
                    Text("Ask a grown-up!", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE08A00))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(CrownGold)
                            .clickable { gate = true }
                            .testTag("gold_prompt_grownup"),
                        contentAlignment = Alignment.Center
                    ) { Text("I'm a grown-up", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk) }
                    Text(
                        "Not now",
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { Access.dismissPrompt() }.padding(horizontal = 16.dp, vertical = 6.dp),
                        fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInkSoft
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF6C63FF))
                            .clickable { Access.dismissPrompt() },
                        contentAlignment = Alignment.Center
                    ) { Text("OK!", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
                }
            }
        }
    }
    if (gate) {
        ParentGate(
            onPass = {
                gate = false
                Access.dismissPrompt()
                onOpenPlans()
            },
            onCancel = {
                gate = false
                Access.dismissPrompt()
            },
            purpose = "Answer to see the Gold plans"
        )
    }
}

/** "New island opened!" celebration. */
@Composable
fun IslandOpenedCard(type: String, onGo: () -> Unit, onLater: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(type) { SoundEffectsHelper.playGameCompletion(context) }
    val (glyph, name, color, tint, line) = when (type) {
        "consonants" -> IslandInfo(com.example.data.Lang.script("ಕ"), "Consonants", Color(0xFF6C63FF), Color(0xFFE6E4FF), com.example.data.Lang.script("You learned all the vowels! Now let's meet ಕ ಖ ಗ and friends."))
        "ottakshara" -> if (com.example.data.Lang.isHindi) IslandInfo("क्ष", "Sanyuktakshar", Color(0xFF14A098), Color(0xFFD3F4EF), "You finished Barakhadi! Now let's join two letters: क + क = क्क")
            else IslandInfo("ಕ್ಕ", "Ottakshara", Color(0xFF14A098), Color(0xFFD3F4EF), "You finished Ka-kaa! Now let's join two letters: ಕ + ಕ = ಕ್ಕ")
        else -> IslandInfo(com.example.data.Lang.script("ಕಾ"), com.example.data.Lang.islandName("guninthakshari"), Color(0xFFFF6B6B), Color(0xFFFFE1E1), com.example.data.Lang.script("You learned all the consonants! Now let's join letters: ಕ ಕಾ ಕಿ ಕೀ…"))
    }
    val pulse = rememberInfiniteTransition(label = "island")
    val grow by pulse.animateFloat(1f, 1.08f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "grow")
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x990B3B66))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            .safeDrawingPadding()
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        ConfettiAnimation()
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(32.dp))
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .testTag("island_opened"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("NEW ISLAND OPENED!", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = color, letterSpacing = 0.5.sp)
            Box(
                Modifier.size(140.dp).scale(grow).background(Color.White, CircleShape).border(12.dp, color, CircleShape).padding(14.dp).background(tint, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(glyph, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk) }
            Text(name, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
            Text(line, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft, textAlign = TextAlign.Center)
            Box(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).background(color).clickable { onGo() },
                contentAlignment = Alignment.Center
            ) { Text("▶  Let's go!", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
            Text(
                "Later",
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onLater() }.padding(horizontal = 16.dp, vertical = 6.dp),
                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInkSoft
            )
        }
    }
}

private data class IslandInfo(val glyph: String, val name: String, val color: Color, val tint: Color, val line: String)

// ---------------------------------------------------------------------------------------------
// Parent corner → Learning path
// ---------------------------------------------------------------------------------------------

@Composable
fun LearningPathScreen(onBack: () -> Unit) {
    val version by Access.version.collectAsState()
    val open = remember(version) { Access.everythingOpen }
    val start = remember(version) { Access.startFrom }
    val levelStar = remember(version) { Access.levelsNeedStar }

    Column(
        Modifier
            .fillMaxSize()
            .background(ScrapbookPaper)
            .safeDrawingPadding()
            .readableWidth(720.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BackHeader("Learning path", "Parent corner", onBack)

        WhiteCard {
            Text("How things open", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
            PathOption(
                "Step by step",
                "Letters open one by one. New islands open as your child learns. (Recommended)",
                selected = !open
            ) { Access.everythingOpen = false }
            PathOption(
                "Everything open",
                "All letters, islands and levels can be played any time.",
                selected = open
            ) { Access.everythingOpen = true }
        }

        WhiteCard {
            Text("Start from", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
            Text(
                "Already knows some letters? Skip ahead. Earlier letters stay open.",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Beginning", "Consonants", com.example.data.Lang.islandName("guninthakshari"), com.example.data.Lang.islandName("ottakshara")).forEachIndexed { i, label ->
                    val on = start == i
                    Text(
                        label,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (on) ScrapbookInk else Color(0xFFF0F4F8))
                            .clickable { Access.startFrom = i }
                            .padding(horizontal = 13.dp, vertical = 8.dp)
                            .testTag("path_start_$i"),
                        fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (on) Color.White else ScrapbookInk
                    )
                }
            }
        }

        WhiteCard {
            Text("Games", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Levels need a star", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                    Text("Finish a level to open the next one", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
                }
                Switchy(on = levelStar) { Access.levelsNeedStar = !levelStar }
            }
        }

        Text(
            "Letters your child has already learned always stay open.",
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PathOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color(0xFFE6F8F6) else Color(0xFFF5F8FB))
            .border(2.dp, if (selected) Color(0xFF2EC4B6) else Color.Transparent, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(22.dp).border(3.dp, if (selected) Color(0xFF2EC4B6) else Color(0xFFB8C4CF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Box(Modifier.size(10.dp).background(Color(0xFF2EC4B6), CircleShape))
        }
        Column {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
            Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
        }
    }
}

@Composable
private fun Switchy(on: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier
            .size(width = 50.dp, height = 30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (on) Color(0xFF2EC4B6) else Color(0xFFB8C4CF))
            .clickable { onToggle() }
            .padding(4.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(Modifier.size(22.dp).background(Color.White, CircleShape))
    }
}
