package com.example.ui.components

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.data.AudioFeedbackManager
import com.example.ui.data.SurpriseSticker
import com.example.ui.data.SurpriseStickerManager

@Composable
fun SurpriseStickerDialog(
    userEmail: String,
    levelName: String = "Level Complete",
    seedModifier: Int = 0,
    onClose: () -> Unit,
    onClaimOrNext: () -> Unit
) {
    val context = LocalContext.current
    var isScratched by remember { mutableStateOf(false) }
    var showAlbum by remember { mutableStateOf(false) }

    // Pick sticker once per dialog appearance
    val sticker = remember {
        SurpriseStickerManager.getRandomSticker(context, userEmail, seedModifier)
    }

    // Interactive scratch tracking: collection of touched coordinates
    val scratchPoints = remember { mutableStateListOf<Offset>() }

    // Trigger unlock and celebration sound on scratch or tap
    fun doReveal() {
        if (!isScratched) {
            isScratched = true
            SurpriseStickerManager.unlockSticker(context, userEmail, sticker.id)
            try {
                com.example.ui.audio.SoundEffectsHelper.playSuccess(context)
                com.example.ui.audio.SoundEffectsHelper.playFanfare(context)
                AudioFeedbackManager.playBadgeUnlocked(context)
            } catch (e: Exception) {
                // Ignore audio failure
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD90A0E21))
                .testTag("surprise_sticker_dialog_overlay"),
            contentAlignment = Alignment.Center
        ) {
            // Confetti shower on reveal
            if (isScratched) {
                ConfettiEffect(modifier = Modifier.fillMaxSize())
            }

            // Top Close Button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .testTag("close_sticker_dialog")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Surface(
                    color = Color(0x33FFFFFF),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "🎉 $levelName 🎉",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = if (isScratched) "Surprise Sticker Unlocked!" else "You Earned a Surprise Reward!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // The Card Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(390.dp)
                        .shadow(24.dp, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScratched) {
                        RevealedStickerContent(
                            sticker = sticker,
                            context = context,
                            userEmail = userEmail
                        )
                    } else {
                        // Simplified 1-tap/swipe reward card
                        UnscratchedRewardCard(
                            scratchPoints = scratchPoints,
                            onPointAdded = { offset ->
                                scratchPoints.add(offset)
                                doReveal()
                            },
                            onTapToReveal = { doReveal() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                if (isScratched) {
                    Button(
                        onClick = onClaimOrNext,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(56.dp)
                            .testTag("claim_sticker_button")
                    ) {
                        Text(
                            text = "CLAIM & NEXT LEVEL 🚀",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = { showAlbum = true },
                        modifier = Modifier.testTag("view_album_button")
                    ) {
                        Text(
                            text = "View Sticker Album 📚 (${SurpriseStickerManager.getUnlockedStickerIds(context, userEmail).size}/${SurpriseStickerManager.allStickers.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFCC80)
                        )
                    }
                } else {
                    Button(
                        onClick = { doReveal() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(56.dp)
                            .testTag("scratch_to_reveal_button")
                    ) {
                        Text(
                            text = "🎁 TAP TO OPEN REWARD 🎁",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF3E2723)
                        )
                    }
                }
            }
        }
    }

    if (showAlbum) {
        StickerAlbumDialog(
            userEmail = userEmail,
            onClose = { showAlbum = false }
        )
    }
}

@Composable
private fun UnscratchedRewardCard(
    scratchPoints: List<Offset>,
    onPointAdded: (Offset) -> Unit,
    onTapToReveal: () -> Unit
) {
    // Shimmer effect
    val transition = rememberInfiniteTransition(label = "card_shimmer")
    val shimmerTranslate by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_pos"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> onPointAdded(offset) },
                    onDrag = { change, _ ->
                        change.consume()
                        onPointAdded(change.position)
                    }
                )
            }
            .clickable { onTapToReveal() },
        contentAlignment = Alignment.Center
    ) {
        // Metallic Blue-Indigo Gradient Base
        Canvas(modifier = Modifier.fillMaxSize()) {
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1A237E),
                    Color(0xFF283593),
                    Color(0xFF0D47A1),
                    Color(0xFF1E88E5)
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
            drawRect(brush = brush)

            // Shimmer gleam
            val shimmerBrush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                start = Offset(shimmerTranslate, 0f),
                end = Offset(shimmerTranslate + 200f, size.height)
            )
            drawRect(brush = shimmerBrush)

            // Scratched marks tracking
            scratchPoints.forEach { pt ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = 32f,
                    center = pt
                )
            }
        }

        // Google Pay style center reward gift emblem
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFFD54F), Color(0xFFFF8F00))
                        ),
                        CircleShape
                    )
                    .border(4.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎁",
                    fontSize = 52.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                color = Color(0x33FFFFFF),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "✨ SURPRISE STICKER REWARD ✨",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap to Open Gift!",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "ಉಡುಗೊರೆ ತೆರೆಯಲು ಮುಟ್ಟಿ!",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "✨ Tap anywhere or swipe to open ✨",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun RevealedStickerContent(
    sticker: SurpriseSticker,
    context: Context,
    userEmail: String
) {
    // Spring bounce entry animation
    val scaleAnim = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(sticker.rarity.bgGradient)
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Rarity Tag
            Surface(
                color = sticker.rarity.color,
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sticker.rarity.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Big Sticker Circle Graphic
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(sticker.primaryColor, sticker.secondaryColor)
                        ),
                        CircleShape
                    )
                    .border(4.dp, Color.White, CircleShape)
                    .shadow(12.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sticker.emoji,
                    fontSize = 62.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Kannada Name
            Text(
                text = sticker.nameKannada,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF212121),
                textAlign = TextAlign.Center
            )

            // English Name
            Text(
                text = sticker.nameEnglish,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = sticker.secondaryColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Lore / Congratulatory description
            Text(
                text = sticker.descriptionKannada,
                fontSize = 13.sp,
                color = Color(0xFF424242),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bonus Coins Won Pill
            Surface(
                color = Color(0xFFFFF8E1),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300))
            ) {
                Text(
                    text = "🪙 +${sticker.bonusCoins} Bonus Coins Added!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE65100),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerAlbumDialog(
    userEmail: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val unlockedIds = remember { SurpriseStickerManager.getUnlockedStickerIds(context, userEmail) }
    var selectedSticker by remember { mutableStateOf<SurpriseSticker?>(null) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF5F5F5)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Surprise Stickers Album 🌟",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF263238)
                        )
                        Text(
                            text = "Collected: ${unlockedIds.size} / ${SurpriseStickerManager.allStickers.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00897B)
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFE0E0E0), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                val progress = (unlockedIds.size.toFloat() / SurpriseStickerManager.allStickers.size.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFFF9800),
                    trackColor = Color(0xFFE0E0E0)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Grid of stickers
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(SurpriseStickerManager.allStickers) { sticker ->
                        val isUnlocked = unlockedIds.contains(sticker.id)
                        StickerGridItem(
                            sticker = sticker,
                            isUnlocked = isUnlocked,
                            onClick = {
                                if (isUnlocked) {
                                    selectedSticker = sticker
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail popup if clicked
    selectedSticker?.let { s ->
        Dialog(onDismissRequest = { selectedSticker = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = s.emoji, fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = s.nameKannada,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF212121)
                    )
                    Text(
                        text = s.nameEnglish,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.secondaryColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = s.descriptionKannada,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF424242)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = s.descriptionEnglish,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF757575)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { selectedSticker = null },
                        colors = ButtonDefaults.buttonColors(containerColor = s.primaryColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Awesome!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerGridItem(
    sticker: SurpriseSticker,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isUnlocked) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) Color.White else Color(0xFFEEEEEE)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isUnlocked) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(sticker.primaryColor, sticker.secondaryColor)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = sticker.emoji, fontSize = 32.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = sticker.nameKannada,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF212121)
                )

                Text(
                    text = sticker.rarity.label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = sticker.rarity.color
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFFBDBDBD), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Locked",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF757575)
                )

                Text(
                    text = "Play to unlock",
                    fontSize = 9.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}
