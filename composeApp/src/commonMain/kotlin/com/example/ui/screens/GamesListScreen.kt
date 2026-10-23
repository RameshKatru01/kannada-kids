package com.example.ui.screens

import com.example.ui.components.readableWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.progress.GameInfo
import com.example.ui.progress.GameProgress
import com.example.ui.progress.GameRegistry
import com.example.ui.progress.PointsPill
import com.example.ui.progress.ProgressInk
import com.example.ui.progress.ProgressInkSoft
import com.example.ui.progress.StarGold
import com.example.ui.progress.StarIcon
import com.example.ui.progress.lerpColor
import com.example.ui.components.GameArt
import com.example.ui.components.GameArtKind
import com.example.ui.components.PolaroidCard
import com.example.ui.components.ScrapbookPaper

/**
 * Games list: every game with its level progress and stars.
 * Games with levels open their level map; the older "just for fun" games open directly.
 */
@Composable
fun GamesListScreen(
    onOpenLevels: (Int) -> Unit,
    onNavigateToGame: (Int) -> Unit,
    onBack: () -> Unit
) {
    val version by GameProgress.version.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookPaper)
            .safeDrawingPadding()
            .readableWidth(1000.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 20.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ProgressInk)
            }
            Spacer(Modifier.width(12.dp))
            Text("Games", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = ProgressInk, modifier = Modifier.weight(1f))
            PointsPill()
        }
        Text(
            "Finish levels to earn stars and points!",
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp),
            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ProgressInkSoft
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(GameRegistry.games, key = { _, g -> g.id }) { index, game ->
                GamePolaroid(
                    index = index,
                    game = game,
                    version = version,
                    onClick = { if (game.hasLevels) onOpenLevels(game.id) else onNavigateToGame(game.id) }
                )
            }
        }
    }
}

internal fun artFor(game: GameInfo): GameArtKind = when (game.id) {
    in 1..4 -> GameArtKind.Monster
    21 -> GameArtKind.Train
    22 -> GameArtKind.Paint
    12 -> GameArtKind.Balloons
    8 -> GameArtKind.Tiles
    9 -> GameArtKind.Grid
    13 -> GameArtKind.Jars
    else -> GameArtKind.Letter
}

@Composable
private fun GamePolaroid(index: Int, game: GameInfo, version: Int, onClick: () -> Unit) {
    val stars = remember(version, game.id) { if (game.hasLevels) GameProgress.totalStars(game) else 0 }
    val done = remember(version, game.id) { if (game.hasLevels) GameProgress.levelsDone(game) else 0 }
    val isMonster = game.id in 1..4
    PolaroidCard(
        index = index,
        onClick = onClick,
        modifier = Modifier.testTag("game_card_${game.id}"),
        pictureHeight = 112,
        picture = {
            GameArt(
                kind = artFor(game),
                color = game.color,
                background = lerpColor(game.color, Color.White, 0.82f),
                glyph = game.glyph,
                ink = game.ink
            )
        },
        caption = {
            Text(
                game.name,
                fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = ProgressInk,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (game.hasLevels) {
                if (isMonster) {
                    Text(game.subtitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ProgressInkSoft, maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StarIcon(15.dp, StarGold)
                    Text(
                        "$stars/${game.maxStars}",
                        fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = ProgressInk
                    )
                    Text(
                        if (done >= game.levels) "· All done!" else "· Level ${done + 1}/${game.levels}",
                        fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ProgressInkSoft, maxLines = 1
                    )
                }
            } else {
                Text(game.subtitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ProgressInkSoft)
            }
        }
    )
}

