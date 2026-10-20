package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthState
import com.example.ui.auth.AuthViewModel
import com.example.ui.data.BadgeManager
import com.example.ui.data.LanguageManager
import com.example.ui.data.ScoreManager
import com.example.ui.theme.BrightCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel = viewModel { AuthViewModel() },
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val userEmail = (authState as? AuthState.Authenticated)?.email ?: "guest"
    val prefs = getSharedPreferences("AppProgress")
    
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val langCode = currentLanguage.code
    
    // Progress for Vowels, Consonants, Guninthakshari, and Numbers
    val vowelsUnlocked = maxOf(
        prefs.getInt("${userEmail}_${langCode}_vowels_unlocked", 0),
        prefs.getInt("${userEmail}_vowels_unlocked", 0)
    )
    val consonantsUnlocked = maxOf(
        prefs.getInt("${userEmail}_${langCode}_consonants_unlocked", 0),
        prefs.getInt("${userEmail}_consonants_unlocked", 0)
    )
    val guninthakshariUnlocked = maxOf(
        prefs.getInt("${userEmail}_${langCode}_guninthakshari_unlocked", 0),
        prefs.getInt("${userEmail}_guninthakshari_unlocked", 0)
    )
    val numbersUnlocked = maxOf(
        prefs.getInt("${userEmail}_${langCode}_numbers_unlocked", 0),
        prefs.getInt("${userEmail}_numbers_unlocked", 0)
    )
    
    val vowelsCount = (vowelsUnlocked + 1).coerceIn(1, 15)
    val consonantsCount = (consonantsUnlocked + 1).coerceIn(1, 34)
    val guninthakshariCount = (guninthakshariUnlocked + 1).coerceIn(1, 34)
    val numbersCount = (numbersUnlocked + 1).coerceIn(1, 10)

    // Progress for Games
    val gamePrefs = getSharedPreferences("GamePrefs")
    val highScore = gamePrefs.getInt("${userEmail}_${langCode}_runner_high_score", 0)
    val totalCoins by ScoreManager.totalCoins.collectAsState()
    val gameScores = remember { ScoreManager.getAllScores(context) }
    
    val totalGames = 16
    val playedGamesCount = remember(gameScores, highScore) {
        val keys = gameScores.keys.toMutableSet()
        if (highScore > 0) keys.add("letter_runner")
        keys.size.coerceIn(0, totalGames)
    }

    // Overall Progress Calculation (Across all 5 modules)
    val overallProgress = (
        (vowelsCount / 15f) +
        (consonantsCount / 34f) +
        (guninthakshariCount / 34f) +
        (numbersCount / 10f) +
        (if (totalGames > 0) playedGamesCount.toFloat() / totalGames.toFloat() else 0f)
    ) / 5f
    val overallPercent = (overallProgress * 100).toInt().coerceIn(0, 100)
    
    val unlockedBadges = BadgeManager.badges.filter { it.isUnlocked(prefs, userEmail, langCode) }
    var subscriptionPlan by remember {
        mutableStateOf(prefs.getString("${langCode}_subscriptionPlan_$userEmail", "Free") ?: "Free")
    }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedPlanPrice by remember { mutableStateOf("") }
    var selectedModule by remember { mutableStateOf<String?>(null) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dashboard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { showSignOutConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrightCyan)
            )
        },
        containerColor = Color(0xFFF4F6F9)
    ) { paddingValues ->
        if (showSignOutConfirm) {
            AlertDialog(
                onDismissRequest = { showSignOutConfirm = false },
                title = {
                    Text(
                        text = "Sign Out",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF263238)
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to sign out and return to the FunBets landing page?",
                        fontSize = 15.sp,
                        color = Color(0xFF546E7A)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSignOutConfirm = false
                            authViewModel.signOut(context)
                            onSignOut()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sign Out", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutConfirm = false }) {
                        Text("Cancel", color = Color(0xFF78909C), fontWeight = FontWeight.SemiBold)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Overall Mastery Hero Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_overall_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF00ACC1), Color(0xFF00838F))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Learning Journey",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Keep learning Kannada step-by-step! 🌟",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "🪙 $totalCoins Coins",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "🏃 High Score: $highScore",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Circular Overall Gauge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(92.dp)
                                .padding(4.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { overallProgress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxSize(),
                                color = Color(0xFFFFD54F),
                                strokeWidth = 8.dp,
                                trackColor = Color.White.copy(alpha = 0.3f),
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$overallPercent%",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Overall",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Overview Circular Stat Cards for ALL 5 Sections
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Stats",
                            tint = Color(0xFF00838F),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Module Progress",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF263238)
                            )
                            Text(
                                text = "Tap a section below to view its detailed progress bar",
                                fontSize = 11.sp,
                                color = Color(0xFF78909C)
                            )
                        }
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            DashboardStatBox(
                                title = "Vowels",
                                charSymbol = "ಅ",
                                value = "$vowelsCount/15",
                                progress = vowelsCount / 15f,
                                color = Color(0xFF4CAF50),
                                testTag = "dashboard_vowels_stat",
                                isSelected = selectedModule == "vowels",
                                onClick = {
                                    selectedModule = if (selectedModule == "vowels") null else "vowels"
                                }
                            )
                        }
                        item {
                            DashboardStatBox(
                                title = "Consonants",
                                charSymbol = "ಕ",
                                value = "$consonantsCount/34",
                                progress = consonantsCount / 34f,
                                color = Color(0xFFFF9800),
                                testTag = "dashboard_consonants_stat",
                                isSelected = selectedModule == "consonants",
                                onClick = {
                                    selectedModule = if (selectedModule == "consonants") null else "consonants"
                                }
                            )
                        }
                        item {
                            DashboardStatBox(
                                title = "Guninthakshari",
                                charSymbol = "ಕಾ",
                                value = "$guninthakshariCount/34",
                                progress = guninthakshariCount / 34f,
                                color = Color(0xFFAB47BC),
                                testTag = "dashboard_guninthakshari_stat",
                                isSelected = selectedModule == "guninthakshari",
                                onClick = {
                                    selectedModule = if (selectedModule == "guninthakshari") null else "guninthakshari"
                                }
                            )
                        }
                        item {
                            DashboardStatBox(
                                title = "Numbers",
                                charSymbol = "೧",
                                value = "$numbersCount/10",
                                progress = numbersCount / 10f,
                                color = Color(0xFF2196F3),
                                testTag = "dashboard_numbers_stat",
                                isSelected = selectedModule == "numbers",
                                onClick = {
                                    selectedModule = if (selectedModule == "numbers") null else "numbers"
                                }
                            )
                        }
                        item {
                            DashboardStatBox(
                                title = "Games",
                                charSymbol = "🎮",
                                value = "$playedGamesCount/$totalGames",
                                progress = if (totalGames > 0) playedGamesCount.toFloat() / totalGames.toFloat() else 0f,
                                color = Color(0xFFFF5722),
                                testTag = "dashboard_games_stat",
                                isSelected = selectedModule == "games",
                                onClick = {
                                    selectedModule = if (selectedModule == "games") null else "games"
                                }
                            )
                        }
                    }
                }
            }

            // Detailed Progress Bar Section (Appears ONLY after clicking respective section in Module Progress)
            AnimatedVisibility(
                visible = selectedModule != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detailed Progress: ${selectedModule?.replaceFirstChar { it.uppercase() }}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF37474F)
                        )
                        TextButton(
                            onClick = { selectedModule = null },
                            modifier = Modifier.testTag("close_detailed_progress_btn")
                        ) {
                            Text("Close ✕", color = Color(0xFF00838F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    when (selectedModule) {
                        "vowels" -> {
                            DetailedProgressCard(
                                categoryTitle = "Vowels (ಸ್ವರಗಳು)",
                                symbol = "ಅ",
                                subtitle = "15 Letters from ಅ to ಅಃ",
                                currentCount = vowelsCount,
                                maxCount = 15,
                                unitLabel = "letters learned",
                                accentColor = Color(0xFF4CAF50),
                                containerBg = Color(0xFFE8F5E9),
                                testTag = "detailed_vowels_card"
                            )
                        }
                        "consonants" -> {
                            DetailedProgressCard(
                                categoryTitle = "Consonants (ವ್ಯಂಜನಗಳು)",
                                symbol = "ಕ",
                                subtitle = "34 Letters from ಕ to ಳ",
                                currentCount = consonantsCount,
                                maxCount = 34,
                                unitLabel = "letters learned",
                                accentColor = Color(0xFFFF9800),
                                containerBg = Color(0xFFFFF3E0),
                                testTag = "detailed_consonants_card"
                            )
                        }
                        "guninthakshari" -> {
                            DetailedProgressCard(
                                categoryTitle = "Guninthakshari (ಗುಣಿತಾಕ್ಷರಗಳು)",
                                symbol = "ಕಾ",
                                subtitle = "34 Consonant sets with 15 vowel signs",
                                currentCount = guninthakshariCount,
                                maxCount = 34,
                                unitLabel = "consonant sets explored",
                                accentColor = Color(0xFFAB47BC),
                                containerBg = Color(0xFFF3E5F5),
                                testTag = "detailed_guninthakshari_card"
                            )
                        }
                        "numbers" -> {
                            DetailedProgressCard(
                                categoryTitle = "Numbers (ಸಂಖ್ಯೆಗಳು)",
                                symbol = "೧",
                                subtitle = "10 Numerals from ೧ to ೧೦",
                                currentCount = numbersCount,
                                maxCount = 10,
                                unitLabel = "numbers mastered",
                                accentColor = Color(0xFF2196F3),
                                containerBg = Color(0xFFE3F2FD),
                                testTag = "detailed_numbers_card"
                            )
                        }
                        "games" -> {
                            DetailedGamesProgressCard(
                                playedCount = playedGamesCount,
                                totalGames = totalGames,
                                coins = totalCoins,
                                highScore = highScore,
                                gameScores = gameScores,
                                testTag = "detailed_games_card"
                            )
                        }
                    }
                }
            }

            if (selectedModule == null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👆", fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Tap on any section in Module Progress above (Vowels, Consonants, Guninthakshari, Numbers, or Games) to view its detailed progress bar!",
                            fontSize = 13.sp,
                            color = Color(0xFF546E7A),
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Badges Section
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_badges_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Icon(
                            Icons.Filled.EmojiEvents, 
                            contentDescription = "Badges", 
                            tint = Color(0xFFFFC107), 
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Collected Badges (${unlockedBadges.size}/${BadgeManager.badges.size})",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF263238)
                        )
                    }
                    
                    if (unlockedBadges.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No badges earned yet.\nPlay games and learn letters to unlock badges! 🏆",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(unlockedBadges) { badge ->
                                DashboardBadgeItem(badge = badge)
                            }
                        }
                    }
                }
            }

            // Surprise Stickers (Google Pay Style) Collection Section
            val unlockedStickers = remember(userEmail) {
                com.example.ui.data.SurpriseStickerManager.getUnlockedStickerIds(context, userEmail)
            }
            var showStickerAlbumDialog by remember { mutableStateOf(false) }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_surprise_stickers_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎁", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Surprise Stickers",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF263238)
                                )
                                Text(
                                    text = "Earned after completing level games",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78909C)
                                )
                            }
                        }
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${unlockedStickers.size}/${com.example.ui.data.SurpriseStickerManager.allStickers.size} Won",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (unlockedStickers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp)
                                .background(Color(0xFFF9FBE7), RoundedCornerShape(12.dp))
                                .clickable { showStickerAlbumDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No surprise stickers unlocked yet!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF33691E)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Complete any game level to earn surprise rewards! 🎁",
                                    fontSize = 12.sp,
                                    color = Color(0xFF558B2F)
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(com.example.ui.data.SurpriseStickerManager.allStickers.filter { unlockedStickers.contains(it.id) }) { s ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(76.dp)
                                        .clickable { showStickerAlbumDialog = true }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(listOf(s.primaryColor, s.secondaryColor))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = s.emoji, fontSize = 30.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = s.nameKannada,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        color = Color(0xFF263238),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = s.rarity.label,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = s.rarity.color
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showStickerAlbumDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("open_sticker_album_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Open Sticker Album (${unlockedStickers.size}/16) 📖",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00838F)
                        )
                    }
                }
            }

            if (showStickerAlbumDialog) {
                com.example.ui.components.StickerAlbumDialog(
                    userEmail = userEmail,
                    onClose = { showStickerAlbumDialog = false }
                )
            }

            // Subscription Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_subscription_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (subscriptionPlan == "Gold") Color(0xFFFFF8E1) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (subscriptionPlan == "Gold") {
                            Icon(
                                Icons.Filled.Star, 
                                contentDescription = "Gold", 
                                tint = Color(0xFFFFC107), 
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "Current Plan: $subscriptionPlan",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (subscriptionPlan == "Gold") Color(0xFFF57F17) else Color(0xFF333333)
                        )
                    }
                    if (subscriptionPlan == "Free") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Upgrade to Gold to unlock all games, consonants, and letters!",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { 
                                    selectedPlanPrice = "₹99 / month"
                                    showPaymentDialog = true 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCA28)),
                                modifier = Modifier.testTag("subscribe_monthly_btn")
                            ) {
                                Text("₹99/mo", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { 
                                    selectedPlanPrice = "₹699 / year"
                                    showPaymentDialog = true 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                                modifier = Modifier.testTag("subscribe_yearly_btn")
                            ) {
                                Text("₹699/yr", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { 
                                subscriptionPlan = "Free"
                                prefs.edit().putString("${langCode}_subscriptionPlan_$userEmail", "Free").apply()
                                com.example.ui.data.UserManager.updateUserPlan(context, userEmail, "Free Explorer", 0)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            modifier = Modifier.testTag("cancel_subscription_btn")
                        ) {
                            Text("Cancel Subscription")
                        }
                    }
                }
            }

            if (showPaymentDialog) {
                AlertDialog(
                    onDismissRequest = { showPaymentDialog = false },
                    title = { Text("Unlock Premium", fontWeight = FontWeight.Bold) },
                    text = {
                        Text("You selected the $selectedPlanPrice plan. Since this is a demo, click confirm to unlock Gold features immediately!")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val previousBadges = BadgeManager.badges.count { it.isUnlocked(prefs, userEmail, langCode) }
                                subscriptionPlan = "Gold"
                                prefs.edit().putString("${langCode}_subscriptionPlan_$userEmail", "Gold").apply()
                                val durationDays = if (selectedPlanPrice.contains("year")) 365 else 30
                                val planName = if (durationDays == 365) "Gold (Annual)" else "Gold (Monthly)"
                                com.example.ui.data.UserManager.updateUserPlan(context, userEmail, planName, durationDays)
                                val newBadges = BadgeManager.badges.count { it.isUnlocked(prefs, userEmail, langCode) }
                                if (newBadges > previousBadges) {
                                    com.example.ui.data.AudioFeedbackManager.playBadgeUnlocked(context)
                                }
                                showPaymentDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.testTag("confirm_mock_upgrade_btn")
                        ) {
                            Text("Confirm Mock Upgrade")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPaymentDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardStatBox(
    title: String, 
    charSymbol: String,
    value: String, 
    progress: Float, 
    color: Color,
    testTag: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.12f) else Color(0xFFFAFAFA)
            )
            .then(
                if (isSelected) Modifier.border(2.5.dp, color, RoundedCornerShape(16.dp))
                else Modifier.border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(72.dp)
        ) {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 6.dp,
                trackColor = Color(0xFFEEEEEE),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = charSymbol,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF555555)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) color else Color(0xFF37474F),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = if (isSelected) color else Color(0xFFECEFF1),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (isSelected) "Active ✓" else "Tap details",
                color = if (isSelected) Color.White else Color(0xFF78909C),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun DetailedProgressCard(
    categoryTitle: String,
    symbol: String,
    subtitle: String,
    currentCount: Int,
    maxCount: Int,
    unitLabel: String,
    accentColor: Color,
    containerBg: Color,
    testTag: String
) {
    val progress = (currentCount.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "progress_$categoryTitle"
    )
    val percent = (progress * 100).toInt()
    val isComplete = currentCount >= maxCount

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Symbol Badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(containerBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = symbol,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF78909C)
                    )
                }

                Surface(
                    color = if (isComplete) Color(0xFFE8F5E9) else containerBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isComplete) "Complete 🎉" else "$percent%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isComplete) Color(0xFF2E7D32) else accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Linear Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accentColor,
                trackColor = Color(0xFFEEEEEE),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$currentCount / $maxCount $unitLabel",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF546E7A)
                )
                Text(
                    text = "${maxCount - currentCount} left",
                    fontSize = 11.sp,
                    color = Color(0xFF90A4AE)
                )
            }
        }
    }
}

@Composable
fun DetailedGamesProgressCard(
    playedCount: Int,
    totalGames: Int,
    coins: Int,
    highScore: Int,
    gameScores: Map<String, Int>,
    testTag: String
) {
    val progress = (playedCount.toFloat() / totalGames.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "progress_games"
    )
    val percent = (progress * 100).toInt()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFBE9E7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Games,
                        contentDescription = "Games",
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Games & Challenges (ಆಟಗಳು)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )
                    Text(
                        text = "17 interactive games & puzzles",
                        fontSize = 12.sp,
                        color = Color(0xFF78909C)
                    )
                }

                Surface(
                    color = Color(0xFFFBE9E7),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$percent%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5722),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFFF5722),
                trackColor = Color(0xFFEEEEEE),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$playedCount of $totalGames games explored",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF546E7A)
                )
                Text(
                    text = "🪙 $coins coins earned",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF8F00)
                )
            }

            if (gameScores.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Recently Played Games:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF455A64)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(gameScores.toList().take(5)) { (gameId, score) ->
                        val friendlyName = gameId
                            .replace("_", " ")
                            .split(" ")
                            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                        Surface(
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = friendlyName,
                                    fontSize = 11.sp,
                                    color = Color(0xFF37474F)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "🪙$score",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF8F00)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardBadgeItem(badge: com.example.ui.data.Badge) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(86.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFF9C4)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = badge.icon, fontSize = 32.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = badge.title,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Color(0xFF333333),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
