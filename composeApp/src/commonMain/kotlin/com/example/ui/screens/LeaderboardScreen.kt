package com.example.ui.screens

import com.example.platform.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.data.ScoreManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val totalCoins by ScoreManager.totalCoins.collectAsState()
    
    val scores = remember { ScoreManager.getAllScores(context) }
    
    // Friendly names mapping
    val friendlyNames = mapOf(
        "listen_and_choose" to "Listen and Choose",
        "banna_galu" to "Color Match",
        "constellation_path" to "Constellation Path",
        "match_letter" to "Match Letter",
        "musical_balloon" to "Balloon Pop",
        "trace_letter" to "Trace Letter",
        "word_search" to "Word Search",
        "buguri_spin" to "Buguri Spin",
        "letter_runner" to "Letter Runner"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaderboard & Coins", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFB300))
            )
        },
        containerColor = Color(0xFFFFF8E1)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Total Coins Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD54F)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Coins", fontSize = 24.sp, color = Color(0xFFE65100))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🪙 $totalCoins", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6F00))
                }
            }

            Text("Game Scores", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))
            Spacer(modifier = Modifier.height(16.dp))

            if (scores.isEmpty()) {
                Text("Play some games to earn coins!", fontSize = 18.sp, color = Color.Gray)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scores.toList().sortedByDescending { it.second }) { (gameId, score) ->
                        val gameName = friendlyNames[gameId] ?: gameId.replace("_", " ").replaceFirstChar { it.uppercase() }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(gameName, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text("🪙 $score", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8F00))
                        }
                    }
                }
            }
        }
    }
}
