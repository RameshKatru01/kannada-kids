package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import com.example.ui.components.PauseMenuDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.geometry.Rect

import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.BrightGreen
import com.example.ui.audio.NativeAudioPlayer
import kotlin.random.Random

data class PathPuzzle(
    val fullSequence: List<String>,
    val missingIndices: List<Int>
)

val vowelPuzzles = listOf(
    PathPuzzle(listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ"), listOf(2)),
    PathPuzzle(listOf("ಊ", "ಋ", "ಎ", "ಏ", "ಐ"), listOf(1, 3)),
    PathPuzzle(listOf("ಒ", "ಓ", "ಔ", "ಅಂ", "ಅಃ"), listOf(0, 2, 4)),
    PathPuzzle(listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ", "ಊ"), listOf(1, 4)),
    PathPuzzle(listOf("ಋ", "ಎ", "ಏ", "ಐ", "ಒ", "ಓ"), listOf(0, 3, 5)),
    PathPuzzle(listOf("ಏ", "ಐ", "ಒ", "ಓ", "ಔ", "ಅಂ", "ಅಃ"), listOf(1, 2, 4, 6)),
    PathPuzzle(listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ", "ಊ", "ಋ", "ಎ"), listOf(1, 3, 5, 7)),
    PathPuzzle(listOf("ಋ", "ಎ", "ಏ", "ಐ", "ಒ", "ಓ", "ಔ", "ಅಂ", "ಅಃ"), listOf(0, 2, 4, 6, 8)),
    PathPuzzle(listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ", "ಊ", "ಋ", "ಎ", "ಏ", "ಐ"), listOf(1, 2, 4, 5, 7, 9)),
    PathPuzzle(listOf("ಅ", "ಆ", "ಇ", "ಈ", "ಉ", "ಊ", "ಋ", "ಎ", "ಏ", "ಐ", "ಒ", "ಓ", "ಔ", "ಅಂ", "ಅಃ"), listOf(1, 3, 5, 7, 9, 11, 13, 14))
)

val consonantPuzzles = listOf(
    PathPuzzle(listOf("ಕ", "ಖ", "ಗ", "ಘ", "ಙ"), listOf(1)),
    PathPuzzle(listOf("ಚ", "ಛ", "ಜ", "ಝ", "ಞ"), listOf(2, 4)),
    PathPuzzle(listOf("ಟ", "ಠ", "ಡ", "ಢ", "ಣ"), listOf(0, 3)),
    PathPuzzle(listOf("ತ", "ಥ", "ದ", "ಧ", "ನ"), listOf(1, 2)),
    PathPuzzle(listOf("ಪ", "ಫ", "ಬ", "ಭ", "ಮ"), listOf(0, 2, 4)),
    PathPuzzle(listOf("ಯ", "ರ", "ಲ", "ವ", "ಶ"), listOf(1, 3, 4)),
    PathPuzzle(listOf("ಷ", "ಸ", "ಹ", "ಳ"), listOf(0, 2)),
    PathPuzzle(listOf("ಕ", "ಖ", "ಗ", "ಘ", "ಙ", "ಚ", "ಛ", "ಜ"), listOf(1, 3, 5, 7)),
    PathPuzzle(listOf("ಟ", "ಠ", "ಡ", "ಢ", "ಣ", "ತ", "ಥ", "ದ", "ಧ", "ನ"), listOf(0, 2, 4, 6, 8)),
    PathPuzzle(listOf("ಪ", "ಫ", "ಬ", "ಭ", "ಮ", "ಯ", "ರ", "ಲ", "ವ", "ಶ"), listOf(1, 3, 5, 7, 9))
)

val guninthakshariPuzzles = listOf(
    PathPuzzle(listOf("ಕ", "ಕಾ", "ಕಿ", "ಕೀ", "ಕು"), listOf(2)),
    PathPuzzle(listOf("ಕೂ", "ಕೃ", "ಕೆ", "ಕೇ", "ಕೈ"), listOf(1, 3)),
    PathPuzzle(listOf("ಕೊ", "ಕೋ", "ಕೌ", "ಕಂ", "ಕಃ"), listOf(0, 2, 4)),
    PathPuzzle(listOf("ಕ", "ಕಾ", "ಕಿ", "ಕೀ", "ಕು", "ಕೂ"), listOf(1, 4)),
    PathPuzzle(listOf("ಕೃ", "ಕೆ", "ಕೇ", "ಕೈ", "ಕೊ", "ಕೋ"), listOf(0, 3, 5)),
    PathPuzzle(listOf("ಕೇ", "ಕೈ", "ಕೊ", "ಕೋ", "ಕೌ", "ಕಂ", "ಕಃ"), listOf(1, 2, 4, 6)),
    PathPuzzle(listOf("ಕ", "ಕಾ", "ಕಿ", "ಕೀ", "ಕು", "ಕೂ", "ಕೃ", "ಕೆ"), listOf(1, 3, 5, 7)),
    PathPuzzle(listOf("ಕೃ", "ಕೆ", "ಕೇ", "ಕೈ", "ಕೊ", "ಕೋ", "ಕೌ", "ಕಂ", "ಕಃ"), listOf(0, 2, 4, 6, 8)),
    PathPuzzle(listOf("ಕ", "ಕಾ", "ಕಿ", "ಕೀ", "ಕು", "ಕೂ", "ಕೃ", "ಕೆ", "ಕೇ", "ಕೈ"), listOf(1, 2, 4, 5, 7, 9)),
    PathPuzzle(listOf("ಕ", "ಕಾ", "ಕಿ", "ಕೀ", "ಕು", "ಕೂ", "ಕೃ", "ಕೆ", "ಕೇ", "ಕೈ", "ಕೊ", "ಕೋ", "ಕೌ", "ಕಂ", "ಕಃ"), listOf(1, 3, 5, 7, 9, 11, 13, 14))
)

val numberPuzzles = listOf(
    PathPuzzle(listOf("೧", "೨", "೩", "೪", "೫"), listOf(2)),
    PathPuzzle(listOf("೬", "೭", "೮", "೯", "೧೦"), listOf(1, 3)),
    PathPuzzle(listOf("೧", "೨", "೩", "೪", "೫", "೬"), listOf(0, 3)),
    PathPuzzle(listOf("೫", "೬", "೭", "೮", "೯", "೧೦"), listOf(1, 4)),
    PathPuzzle(listOf("೧", "೨", "೩", "೪", "೫", "೬", "೭", "೮"), listOf(2, 4, 6)),
    PathPuzzle(listOf("೩", "೪", "೫", "೬", "೭", "೮", "೯", "೧೦"), listOf(0, 3, 5, 7)),
    PathPuzzle(listOf("೧", "೨", "೩", "೪", "೫", "೬", "೭", "೮", "೯", "೧೦"), listOf(1, 3, 5, 7, 9)),
    PathPuzzle(listOf("೧", "೨", "೩", "೪", "೫", "೬", "೭", "೮", "೯", "೧೦"), listOf(0, 2, 4, 6, 8)),
    PathPuzzle(listOf("೧", "೨", "೩", "೪", "೫", "೬", "೭", "೮", "೯", "೧೦"), listOf(1, 2, 4, 5, 7, 8)),
    PathPuzzle(listOf("೧", "೨", "೩", "೪", "೫", "೬", "೭", "೮", "೯", "೧೦"), listOf(0, 1, 3, 4, 6, 7, 9))
)

val animals = listOf("🦁", "🐯", "🐶", "🐘", "🐒", "🐼", "🐸", "🐰", "🐻", "🐧")

data class Particle(
    val x: Float,
    val y: Float,
    val speedX: Float,
    val speedY: Float,
    val color: Color,
    val size: Float
)

@Composable
fun ConfettiAnimation(show: Boolean) {
    if (!show) return
    val particles = remember {
        List(100) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.5f,
                speedX = (Random.nextFloat() - 0.5f) * 0.4f,
                speedY = Random.nextFloat() * 0.8f + 0.2f,
                color = listOf(Color(0xFFE91E63), Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFFEB3B), Color(0xFFFF9800)).random(),
                size = Random.nextFloat() * 20f + 10f
            )
        }
    }

    val animationPhase by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
        label = "confetti"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val currentX = p.x * size.width + p.speedX * size.width * animationPhase
            val currentY = p.y * size.height + p.speedY * size.height * animationPhase
            drawCircle(
                color = p.color,
                radius = p.size,
                center = Offset(currentX, currentY)
            )
        }
    }
}



@Composable
fun DancingAnimal(animal: String, show: Boolean) {
    if (!show) return
    val infiniteTransition = rememberInfiniteTransition(label = "dancing")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Text(
        text = animal,
        fontSize = 80.sp,
        modifier = Modifier
            .offset(y = offsetY.dp)
            .graphicsLayer {
                rotationZ = rotation
            }
    )
}

/** Game sounds for the tile games (Swara Adventure); uses the shared kid-friendly sounds. */
class SoundEffects {
    fun playCorrectTile() {
        com.example.ui.audio.SoundEffectsHelper.playSuccess()
    }

    fun playIncorrectTile() {
        com.example.ui.audio.SoundEffectsHelper.playOops()
    }

    fun playLevelComplete() {
        com.example.ui.audio.SoundEffectsHelper.playFanfare()
    }

    fun release() {
        // Nothing to release: sounds are played by short-lived platform players.
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PathBuilderScreen(gameType: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val sfx = remember { SoundEffects() }

    DisposableEffect(Unit) {
        onDispose {
            sfx.release()
        }
    }

    val puzzles = remember(gameType) {
        when (gameType) {
            1 -> vowelPuzzles
            2 -> consonantPuzzles
            3 -> guninthakshariPuzzles
            4 -> numberPuzzles
            else -> vowelPuzzles
        }
    }

    var puzzleIndex by remember { mutableIntStateOf(0) }
    
    val currentPuzzle = puzzles[puzzleIndex % puzzles.size]
    
    val initialSlots = remember(puzzleIndex, currentPuzzle) {
        currentPuzzle.fullSequence.mapIndexed { index, char ->
            if (currentPuzzle.missingIndices.contains(index)) "" else char
        }
    }
    
    val initialTiles = remember(puzzleIndex, currentPuzzle) {
        currentPuzzle.missingIndices.map { currentPuzzle.fullSequence[it] }.shuffled()
    }
    
    var currentSlots by remember(puzzleIndex, currentPuzzle) { mutableStateOf(initialSlots) }
    var availableTiles by remember(puzzleIndex, currentPuzzle) { mutableStateOf(initialTiles) }
    var showSuccess by remember(puzzleIndex, currentPuzzle) { mutableStateOf(false) }
    var showSurpriseSticker by remember(puzzleIndex, currentPuzzle) { mutableStateOf(false) }
    var selectedTile by remember(puzzleIndex) { mutableStateOf<String?>(null) }
    var rewardCharacter by remember(puzzleIndex) { mutableStateOf<String?>(null) }

    val slotBounds = remember { mutableStateMapOf<Int, Rect>() }
    var draggedTile by remember(puzzleIndex) { mutableStateOf<String?>(null) }
    var dragPosition by remember(puzzleIndex) { mutableStateOf(Offset.Zero) }

    fun placeTileIntoSlot(slotIndex: Int, tile: String) {
        val newSlots = currentSlots.toMutableList()
        newSlots[slotIndex] = tile
        currentSlots = newSlots

        val newTiles = availableTiles.toMutableList()
        newTiles.remove(tile)
        availableTiles = newTiles
        selectedTile = null
        rewardCharacter = tile

        com.example.ui.audio.SoundEffectsHelper.playSuccess(context)
        NativeAudioPlayer.playPhonics(context, tile)
        if (!newSlots.contains("")) {
            showSuccess = true
            showSurpriseSticker = true
            com.example.ui.audio.SoundEffectsHelper.playGameCompletion(context)
            try { com.example.ui.data.ScoreManager.addCoins(context, "path_builder", 10) } catch (e: Exception) {}
        }
    }

    fun onTileDrop(tile: String, dropPosition: Offset) {
        // 1. Direct matching slot lookup
        val matchingSlotIndex = currentSlots.indices.firstOrNull { index ->
            currentSlots[index] == "" && currentPuzzle.fullSequence.getOrNull(index) == tile
        }

        if (matchingSlotIndex != null) {
            val matchingRect = slotBounds[matchingSlotIndex]
            if (matchingRect != null) {
                val dist = (dropPosition - matchingRect.center).getDistance()
                val verticalDist = kotlin.math.abs(dropPosition.y - matchingRect.center.y)
                val horizontalDist = kotlin.math.abs(dropPosition.x - matchingRect.center.x)
                if (dist < 450f || (verticalDist < 350f && horizontalDist < 350f)) {
                    placeTileIntoSlot(matchingSlotIndex, tile)
                    return
                }
            }
            // Check if dropped near ANY slot or dragged upward into the slots row area
            val anySlotRect = slotBounds.values.firstOrNull()
            if (anySlotRect != null && dropPosition.y <= (anySlotRect.bottom + 180f)) {
                placeTileIntoSlot(matchingSlotIndex, tile)
                return
            }
            // If dropped generally in upper region or dragged up significantly
            placeTileIntoSlot(matchingSlotIndex, tile)
            return
        }

        // 2. Check if dropped onto ANY empty slot that matches this tile
        val emptyMatchingSlots = slotBounds.filter { (idx, _) ->
            currentSlots.getOrNull(idx) == "" && currentPuzzle.fullSequence.getOrNull(idx) == tile
        }
        val closest = emptyMatchingSlots.minByOrNull { (_, rect) -> (dropPosition - rect.center).getDistance() }
        if (closest != null) {
            placeTileIntoSlot(closest.key, tile)
            return
        }

        com.example.ui.audio.SoundEffectsHelper.playOops(context)
    }

    fun onTileTap(tile: String) {
        com.example.ui.audio.SoundEffectsHelper.playButtonTap(context)
        // Direct tap on tile instantly places it into the empty slot matching it
        val matchingSlotIndex = currentSlots.indices.firstOrNull { index ->
            currentSlots[index] == "" && currentPuzzle.fullSequence.getOrNull(index) == tile
        }
        if (matchingSlotIndex != null) {
            placeTileIntoSlot(matchingSlotIndex, tile)
        } else {
            selectedTile = if (selectedTile == tile) null else tile
        }
    }

    val authPrefs = remember { getSharedPreferences("AppProgress") }
    val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
    val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)

    var showPauseMenu by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Level ${(puzzleIndex % puzzles.size) + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("/${puzzles.size}", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (puzzleIndex > 0) {
                        IconButton(onClick = { puzzleIndex-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Level", tint = Color.White)
                        }
                    }
                    if (puzzleIndex < puzzles.size - 1) {
                        IconButton(onClick = { 
                            if (isGuest && puzzleIndex >= 0) {
                                Toast.makeText(context, "🔒 Locked: Sign in with Gmail to unlock all levels!", Toast.LENGTH_SHORT).show()
                            } else {
                                puzzleIndex++ 
                            }
                        }) {
                            Icon(
                                imageVector = if (isGuest) Icons.Default.Lock else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Level",
                                tint = Color.White
                            )
                        }
                    }
                    IconButton(onClick = { showPauseMenu = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrightCyan)
            )
        },
        containerColor = Color(0xFFE1F5FE)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!showSuccess) {
                    Text(
                        text = "Complete the path!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0277BD),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }

                // Slots
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                ) {
                    currentSlots.forEachIndexed { index, slot ->
                        val isFilledByPlayer = currentPuzzle.missingIndices.contains(index) && slot.isNotEmpty()
                        val scale by animateFloatAsState(
                            targetValue = if (isFilledByPlayer) 1.15f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "tileScale"
                        )
                        
                        Box(
                            modifier = Modifier
                                .onGloballyPositioned { coords ->
                                    val r = coords.boundsInRoot()
                                    if (r.width > 0f && r.height > 0f) {
                                        slotBounds[index] = r
                                    }
                                }
                                .size(56.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (slot.isEmpty()) Color.LightGray else BrightGreen)
                                .clickable(!showSuccess) {
                                    if (isFilledByPlayer) {
                                        val returnedTile = slot
                                        val newSlots = currentSlots.toMutableList()
                                        newSlots[index] = ""
                                        currentSlots = newSlots
                                        availableTiles = (availableTiles + returnedTile)
                                        selectedTile = null
                                    } else if (slot.isEmpty()) {
                                        // Tap on empty slot: auto-fill from selected tile or matching available tile!
                                        val expected = currentPuzzle.fullSequence.getOrNull(index) ?: ""
                                        if (selectedTile != null) {
                                            if (selectedTile == expected) {
                                                placeTileIntoSlot(index, selectedTile!!)
                                            } else {
                                                sfx.playIncorrectTile()
                                            }
                                        } else {
                                            val matching = availableTiles.firstOrNull { it == expected }
                                            if (matching != null) {
                                                placeTileIntoSlot(index, matching)
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = slot,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Tiles
                if (availableTiles.isNotEmpty()) {
                    Text(
                        text = "Drag or tap a tile to place:",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0277BD),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
                    ) {
                        availableTiles.forEachIndexed { tileIdx, tile ->
                            key("$tile-$tileIdx-$puzzleIndex") {
                                var tileCenter by remember(tile, tileIdx, puzzleIndex) { mutableStateOf(Offset.Zero) }
                                val isDragged = draggedTile == tile
                                val offset = if (isDragged) dragPosition else Offset.Zero
                                val isSelected = selectedTile == tile

                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { coords ->
                                            if (!isDragged) {
                                                tileCenter = coords.boundsInRoot().center
                                            }
                                        }
                                        .zIndex(if (isDragged) 10f else 1f)
                                        .offset { androidx.compose.ui.unit.IntOffset(offset.x.toInt(), offset.y.toInt()) }
                                        .size(58.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDragged) Color(0xFFFFB74D) else Color(0xFFFF9800))
                                        .then(
                                            if (isSelected) Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp))
                                            else Modifier
                                        )
                                        .pointerInput(tile, tileIdx, puzzleIndex) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                var totalDrag = Offset.Zero
                                                var isDrag = false
                                                val slop = viewConfiguration.touchSlop

                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                                    if (change.changedToUp()) {
                                                        change.consume()
                                                        if (isDrag) {
                                                            val dropCenter = tileCenter + dragPosition
                                                            onTileDrop(tile, dropCenter)
                                                            draggedTile = null
                                                            dragPosition = Offset.Zero
                                                        } else {
                                                            onTileTap(tile)
                                                        }
                                                        break
                                                    }

                                                    val dragAmount = change.positionChange()
                                                    totalDrag += dragAmount

                                                    if (!isDrag && totalDrag.getDistance() > slop) {
                                                        isDrag = true
                                                        draggedTile = tile
                                                        dragPosition = Offset.Zero
                                                    }

                                                    if (isDrag) {
                                                        change.consume()
                                                        dragPosition += dragAmount
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tile,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                if (showSuccess) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val successScale by animateFloatAsState(
                        targetValue = if (showSuccess) 1.2f else 0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "successScale"
                    )

                    DancingAnimal(
                        animal = animals[(puzzleIndex % puzzles.size) % animals.size],
                        show = showSuccess
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "🎉 Awesome! 🎉",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE91E63),
                        modifier = Modifier.graphicsLayer {
                            scaleX = successScale
                            scaleY = successScale
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    val isLastLevel = (puzzleIndex % puzzles.size) == puzzles.size - 1
                    
                    val infiniteTransition = rememberInfiniteTransition(label = "btn_bounce")
                    val bounceScale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bounceScale"
                    )

                    Button(
                        onClick = {
                            com.example.ui.audio.SoundEffectsHelper.playButtonTap(context)
                            if (isGuest && (puzzleIndex % puzzles.size) >= 0) {
                                Toast.makeText(context, "🔒 Level 1 Complete! Sign in with Gmail to play all levels!", Toast.LENGTH_LONG).show()
                                onBack()
                            } else {
                                puzzleIndex++
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722)
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(80.dp)
                            .graphicsLayer {
                                scaleX = successScale * bounceScale
                                scaleY = successScale * bounceScale
                            },
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 12.dp,
                            pressedElevation = 4.dp
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = if (isGuest) "FINISH LEVEL 1" else if (isLastLevel) "PLAY AGAIN!" else "NEXT LEVEL!",
                            fontSize = if (isGuest) 24.sp else 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            if (showPauseMenu) {
                PauseMenuDialog(
                    onDismiss = { showPauseMenu = false },
                    onRestart = {
                        showPauseMenu = false
                        currentSlots = initialSlots
                        availableTiles = initialTiles
                        showSuccess = false
                    },
                    onMainMenu = {
                        showPauseMenu = false
                        onBack()
                    }
                )
            }

            ConfettiAnimation(show = showSuccess)

            if (showSurpriseSticker) {
                val authPrefs = getSharedPreferences("AppProgress")
                val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
                com.example.ui.components.SurpriseStickerDialog(
                    userEmail = userEmail,
                    levelName = "Level ${(puzzleIndex % puzzles.size) + 1}",
                    seedModifier = puzzleIndex,
                    onClose = {
                        showSurpriseSticker = false
                    },
                    onClaimOrNext = {
                        showSurpriseSticker = false
                        puzzleIndex++
                    }
                )
            }

            // Lottie Animation Reward whenever child correctly identifies a Kannada character
            com.example.ui.components.LottieCharacterRewardOverlay(
                visible = rewardCharacter != null && !showSuccess,
                character = rewardCharacter ?: "",
                onDismiss = { rewardCharacter = null }
            )
        }
    }
}
