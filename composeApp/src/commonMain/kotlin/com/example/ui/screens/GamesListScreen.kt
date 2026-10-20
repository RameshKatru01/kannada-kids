package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
            .background(Color(0xFFEAF6FF))
            .safeDrawingPadding()
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(GameRegistry.games, key = { it.id }) { game ->
                GameProgressCard(
                    game = game,
                    version = version,
                    onClick = { if (game.hasLevels) onOpenLevels(game.id) else onNavigateToGame(game.id) }
                )
            }
        }
    }
}

@Composable
private fun GameProgressCard(game: GameInfo, version: Int, onClick: () -> Unit) {
    val stars = remember(version, game.id) { if (game.hasLevels) GameProgress.totalStars(game) else 0 }
    val done = remember(version, game.id) { if (game.hasLevels) GameProgress.levelsDone(game) else 0 }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("game_card_${game.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(game.color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(game.glyph, color = game.ink, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (game.hasLevels && game.subtitle.isNotEmpty() && game.name == "Feed the Monster") "${game.name}: ${game.subtitle}" else game.name,
                fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = ProgressInk,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (game.hasLevels) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE3EEF8))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(done.toFloat() / game.levels)
                                .clip(RoundedCornerShape(4.dp))
                                .background(game.color)
                        )
                    }
                    Text(
                        if (done >= game.levels) "All done!" else "Level ${done + 1}/${game.levels}",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ProgressInkSoft
                    )
                }
            } else {
                Text(game.subtitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ProgressInkSoft)
            }
        }
        if (game.hasLevels) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                StarIcon(18.dp, StarGold)
                Text("$stars/${game.maxStars}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = ProgressInk)
            }
        } else {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = game.color, modifier = Modifier.size(28.dp))
        }
    }
}
