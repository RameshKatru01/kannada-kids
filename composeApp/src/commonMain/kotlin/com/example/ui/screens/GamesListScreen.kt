package com.example.ui.screens

import com.example.platform.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Games
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.BrightGreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.AuthState
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.data.LanguageManager
import com.example.ui.data.AppLanguage

data class GameItem(val id: Int, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesListScreen(
    onNavigateToGame: (Int) -> Unit,
    onBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current
    val userEmail = (authState as? AuthState.Authenticated)?.email ?: "guest"
    val prefs = getSharedPreferences("AppProgress")
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val langCode = currentLanguage.code
    val subscriptionPlan = prefs.getString("${langCode}_subscriptionPlan_$userEmail", "Free") ?: "Free"

    val games = listOf(
        GameItem(1, "Feed the Monster: Vowels"),
        GameItem(2, "Feed the Monster: Consonants"),
        GameItem(3, "Feed the Monster: Guninthakshari"),
        GameItem(4, "Feed the Monster: Numbers"),
        GameItem(21, "Letter Train"),
        GameItem(22, "Paint the Monster: Colours"),
        GameItem(6, "Letter Runner"),
        GameItem(7, "Vowel Map Navigator"),
        GameItem(8, "Scrambled Words"),
        GameItem(9, "Word Search Puzzles"),
        GameItem(10, "Trace the Letter"),
        GameItem(11, "Match the Letter"),
        GameItem(12, "Musical Balloon Pop"),
        GameItem(13, "Colors Sort"),
        GameItem(15, "Buguri Spin"),
        GameItem(16, "Constellation Path"),
        GameItem(19, "Listen & Choose"),
        GameItem(20, "Swara Adventure")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Games", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrightCyan)
            )
        },
        containerColor = Color(0xFFE1F5FE)
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(games) { game ->
                val isUnlocked = true
                GameMockCard(
                    name = game.name, 
                    isUnlocked = isUnlocked,
                    onClick = { 
                        if (game.id <= 22) {
                            onNavigateToGame(game.id)
                        } 
                    }
                )
            }
        }
    }
}

@Composable
fun GameMockCard(name: String, isUnlocked: Boolean = true, onClick: () -> Unit) {
    val colors = listOf(Color(0xFFFFCC80), BrightCyan, BrightGreen, Color(0xFFCE93D8), Color(0xFF81D4FA), Color(0xFFFFAB91))
    val bgColor = colors[kotlin.math.abs(name.hashCode()) % colors.size]

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isUnlocked) { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Games,
                contentDescription = "Game",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
            if (!isUnlocked) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}
