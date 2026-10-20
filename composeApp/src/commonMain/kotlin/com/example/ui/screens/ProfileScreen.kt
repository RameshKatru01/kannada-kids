package com.example.ui.screens

import com.example.platform.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ui.data.ThemeManager
import com.example.ui.data.AppTheme
import com.example.ui.data.LanguageManager
import com.example.ui.data.AppLanguage
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.AuthState
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onNavigateToBadges: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    onSignOut: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val userEmail = (authState as? AuthState.Authenticated)?.email ?: "guest"
    val isAdmin = com.example.ui.data.UserManager.isAdmin(userEmail)
    
    val prefs = getSharedPreferences("AppProgress")
    val gamePrefs = getSharedPreferences("GamePrefs")
    
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val langCode = currentLanguage.code
    val vowelsUnlocked = prefs.getInt("${userEmail}_${langCode}_vowels_unlocked", 0)
    val consonantsUnlocked = prefs.getInt("${userEmail}_${langCode}_consonants_unlocked", 0)
    val numbersUnlocked = prefs.getInt("${userEmail}_${langCode}_numbers_unlocked", 0)
    val highScore = gamePrefs.getInt("${userEmail}_${langCode}_runner_high_score", 0)
    
    val vowelsCount = (vowelsUnlocked + 1).coerceAtMost(15)
    val consonantsCount = (consonantsUnlocked + 1).coerceAtMost(34)
    val numbersCount = (numbersUnlocked + 1).coerceAtMost(10)
    
    val currentTheme by ThemeManager.currentTheme.collectAsState()
    
    var subscriptionPlan by remember(userEmail, langCode) { mutableStateOf(prefs.getString("${langCode}_subscriptionPlan_$userEmail", "Free") ?: "Free") }
    
    // Parental Gate State
    var isParentVerified by remember { mutableStateOf(true) }
    var mathNum1 by remember { mutableStateOf(Random.nextInt(5, 12)) }
    var mathNum2 by remember { mutableStateOf(Random.nextInt(5, 12)) }
    var mathAnswer by remember { mutableStateOf("") }
    var mathError by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        if (false) {
            // Parental Gate UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = "Parent", modifier = Modifier.size(64.dp), tint = BrightCyan)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "For Parents Only",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please solve this math problem to access settings and progress.",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "$mathNum1 x $mathNum2 = ?",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrightCyan
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = mathAnswer,
                    onValueChange = { 
                        mathAnswer = it 
                        mathError = false
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Answer") },
                    isError = mathError,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
                if (mathError) {
                    Text("Incorrect answer, try again.", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val answer = mathAnswer.toIntOrNull()
                        if (answer == mathNum1 * mathNum2) {
                            isParentVerified = true
                        } else {
                            mathError = true
                            mathAnswer = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.6f),
                    colors = ButtonDefaults.buttonColors(containerColor = BrightCyan)
                ) {
                    Text("Enter", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Verified Profile UI (Simplified)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (authState is AuthState.Authenticated) {
                    val authUser = authState as AuthState.Authenticated
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (authUser.displayName?.take(1) ?: userEmail.take(1)).uppercase(),
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = authUser.displayName ?: "Little Explorer",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = userEmail,
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = Color(0xFFE0F2FE),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (userEmail.endsWith("@gmail.com", ignoreCase = true)) "✉️ Verified Gmail Account" else "🌟 Active Learner",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isAdmin) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAdmin() },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0xFFF59E0B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Administration Portal",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Manage users, plans, and expiry dates",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Button(
                                onClick = onNavigateToAdmin,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Open", fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "App Theme",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    modifier = Modifier.align(Alignment.Start).padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AppTheme.entries.forEach { theme ->
                        ThemeOption(
                            theme = theme,
                            isSelected = currentTheme == theme,
                            onClick = { ThemeManager.setTheme(context, theme) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Sign Out Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSignOutConfirm = true },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFCDD2)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sign Out",
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
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
        }
    }
}

@Composable
fun ThemeOption(theme: AppTheme, isSelected: Boolean, onClick: () -> Unit) {
    val color = when(theme) {
        AppTheme.DEFAULT -> Color(0xFFE1F5FE)
        AppTheme.OCEAN -> Color(0xFF039BE5)
        AppTheme.FOREST -> Color(0xFF43A047)
        AppTheme.SUNSET -> Color(0xFFF4511E)
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color)
                .border(
                    width = if (isSelected) 4.dp else 1.dp,
                    color = if (isSelected) Color.Black else Color.LightGray,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onClick() }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = theme.displayName.replace(" ", "\n"),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = Color.DarkGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color) {
    Card(
        modifier = Modifier
            .size(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}
