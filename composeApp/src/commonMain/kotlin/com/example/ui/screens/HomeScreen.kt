package com.example.ui.screens

import com.example.platform.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RainbowColors

@Composable
fun HomeScreen(
    onNavigateToVowels: () -> Unit,
    onNavigateToConsonants: () -> Unit,
    onNavigateToGunninthakshari: () -> Unit,
    onNavigateToNumbers: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    var showSignOutConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Kid-friendly Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color(0xFF81D4FA), Color(0xFFE1F5FE))
                    ),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FunBets",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE91E63),
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.White,
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Hello, Little Explorer! 😊",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0277BD)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalCoins = com.example.ui.data.ScoreManager.totalCoins.collectAsState().value
                Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFFFFD54F)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.clickable { onNavigateToLeaderboard() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🪙 $totalCoins", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6F00))
                    }
                }
                // Profile Avatar Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color(0xFFFFEB3B), CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                        .clickable { onNavigateToProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧒", fontSize = 26.sp)
                }

                // Sign Out Action Button
                IconButton(
                    onClick = { showSignOutConfirm = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.85f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

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

        MainAdventureMap(
            modifier = Modifier.weight(1f),
            onNavigateToVowels = onNavigateToVowels,
            onNavigateToConsonants = onNavigateToConsonants,
            onNavigateToGunninthakshari = onNavigateToGunninthakshari,
            onNavigateToNumbers = onNavigateToNumbers
        )
    }
}

@Composable
fun MainAdventureMap(
    modifier: Modifier = Modifier,
    onNavigateToVowels: () -> Unit,
    onNavigateToConsonants: () -> Unit,
    onNavigateToGunninthakshari: () -> Unit,
    onNavigateToNumbers: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "path_animation")
    val pathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "pathPhase"
    )

    val currentLanguage by com.example.ui.data.LanguageManager.currentLanguage.collectAsState()
    
    val startText = "START"
    val goalText = "GOAL"

    val modules = listOf(
            Pair("Vowels", "ಅ"),
            Pair("Consonants", "ಕ"),
            Pair("Guninthakshari", "ಕಾ"),
            Pair("Numbers", "೧")
        )
    val clickActions = listOf(onNavigateToVowels, onNavigateToConsonants, onNavigateToGunninthakshari, onNavigateToNumbers)

    val scrollState = androidx.compose.foundation.rememberScrollState(initial = Int.MAX_VALUE)
    BoxWithConstraints(modifier = modifier.fillMaxWidth().background(Color(0xFF81C784))) {
        val mapWidth = maxWidth
        val mapHeight = maxHeight
        val totalHeight = mapHeight.coerceAtLeast(600.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                
                .verticalScroll(scrollState)
                .height(totalHeight)
        ) {
            // Background decorations
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width * 0.4f, 0f)
                        lineTo(size.width * 0.6f, size.height * 0.2f)
                        lineTo(size.width, size.height * 0.05f)
                        lineTo(size.width, 0f)
                        lineTo(0f, 0f)
                        close()
                    },
                    color = Color(0xFF9E9E9E).copy(alpha = 0.5f)
                )
            }

            val stepHeight = totalHeight / (modules.size + 1)
            val positions = modules.mapIndexed { index, _ ->
                val yOffset = totalHeight - (stepHeight * (index + 1)) - 80.dp
                val xSine = kotlin.math.sin(index * 1.5)
                val xOffset = (mapWidth / 2) + (xSine * (mapWidth.value * 0.25)).dp - 60.dp
                Pair(xOffset, yOffset)
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0 until positions.size - 1) {
                    val start = positions[i]
                    val end = positions[i + 1]
                    
                    val startX = start.first.toPx() + 60.dp.toPx()
                    val startY = start.second.toPx() + 60.dp.toPx()
                    val endX = end.first.toPx() + 60.dp.toPx()
                    val endY = end.second.toPx() + 60.dp.toPx()
                    
                    val controlX1 = startX
                    val controlY1 = startY + (endY - startY) / 2
                    val controlX2 = endX
                    val controlY2 = startY + (endY - startY) / 2
                    
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(startX, startY)
                        cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)
                    }
                    
                    val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(30f, 20f), phase = pathPhase * 2f)
                    
                    drawPath(
                        path = path,
                        color = Color(0xFF795548),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 36.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = pathEffect
                        )
                    )
                    drawPath(
                        path = path,
                        color = Color(0xFFFFCC80),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 28.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = pathEffect
                        )
                    )
                }
            }

            // Decorate with emojis
            val decorations = listOf("🌲", "🌳", "🌺", "🌸", "🦋", "🍄", "🌷", "🌻", "🍎", "🦜")
            positions.forEachIndexed { i, pos ->
                val x = pos.first
                val y = pos.second
                val side = if (i % 2 == 0) -80.dp else 120.dp
                Text(
                    text = decorations[i % decorations.size],
                    fontSize = 48.sp,
                    modifier = Modifier.offset(x = x + side, y = y + 20.dp)
                )
            }

            positions.forEachIndexed { index, (x, y) ->
                val module = modules[index]
                val rainbowColor = RainbowColors[index % RainbowColors.size]
                Box(
                    modifier = Modifier
                        .offset(x = x, y = y)
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(rainbowColor)
                        .clickable { 
                            com.example.ui.audio.SoundEffectsHelper.playButtonTap()
                            clickActions[index]() 
                        }
                        .border(6.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = module.second,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.3f),
                                offset = Offset(4f, 4f),
                                blurRadius = 8f
                            )
                        )
                    )
                }
                // Add the label below the bubble
                Text(
                    text = module.first,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .offset(x = x - 20.dp, y = y + 125.dp)
                        .background(Color.Black.copy(alpha=0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                if (index == 0) {
                    Text(
                        text = startText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF388E3C),
                        modifier = Modifier
                            .offset(x = x + 30.dp, y = y + 185.dp)
                            .background(Color.White.copy(alpha=0.9f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }
                if (index == modules.size - 1) { 
                    Text(
                        text = goalText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier
                            .offset(x = x, y = y - 60.dp)
                            .background(Color.White.copy(alpha=0.9f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }
            }
        }
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
                label = "Reference",
                isActive = currentRoute == "reference",
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
