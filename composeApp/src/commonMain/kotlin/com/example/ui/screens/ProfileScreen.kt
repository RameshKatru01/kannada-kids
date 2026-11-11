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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.BrightCyan
import com.example.ui.data.LanguageManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.AuthState
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.foundation.text.KeyboardOptions

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
    val cloudAdmins by com.example.ui.data.AdminRoles.cloudAdmins.collectAsState()
    LaunchedEffect(userEmail) { try { com.example.ui.data.AdminRoles.load() } catch (e: Exception) {} }
    val isAdmin = com.example.ui.data.UserManager.isAdmin(userEmail) || userEmail.trim().lowercase() in cloudAdmins
    
    val prefs = getSharedPreferences("AppProgress")
    val gamePrefs = getSharedPreferences("GamePrefs")
    
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val langCode = currentLanguage.code
    val vowelsUnlocked = prefs.getInt("${userEmail}_${langCode}_vowels_unlocked", 0)
    val consonantsUnlocked = prefs.getInt("${userEmail}_${langCode}_consonants_unlocked", 0)
    val numbersUnlocked = prefs.getInt("${userEmail}_${langCode}_numbers_unlocked", 0)
    val highScore = gamePrefs.getInt("${userEmail}_runner_high_score", 0)
    
    val vowelsCount = (vowelsUnlocked + 1).coerceAtMost(15)
    val consonantsCount = (consonantsUnlocked + 1).coerceAtMost(34)
    val numbersCount = (numbersUnlocked + 1).coerceAtMost(10)
    
    
    var subscriptionPlan by remember(userEmail, langCode) { mutableStateOf(prefs.getString("${langCode}_subscriptionPlan_$userEmail", "Free") ?: "Free") }
    
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Learner name: the child types their own name; Home greets them with it
            var learnerName by remember(userEmail) { mutableStateOf(com.example.ui.data.LearnerName.load()) }
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Learner Name", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = learnerName,
                        onValueChange = {
                            val v = it.replace("\n", " ").take(com.example.ui.data.LearnerName.MAX_LENGTH)
                            learnerName = v
                            com.example.ui.data.LearnerName.set(v)
                        },
                        placeholder = { Text("Type your name", color = Color(0xFF94A3B8)) },
                        leadingIcon = { Text("🧒", fontSize = 20.sp) },
                        singleLine = true,
                        // Each letter in a rainbow colour (VIBGYOR); dark cursor so it shows on white
                        visualTransformation = RainbowText,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1E293B),
                            unfocusedTextColor = Color(0xFF1E293B),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFF6C63FF),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("learner_name_field")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("This name shows on the home screen.", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

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
                        text = "Are you sure you want to sign out? You'll go back to the start screen.",
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




/** VIBGYOR: violet, indigo, blue, green, yellow (a deeper gold so it reads on white), orange, red. */
internal val Vibgyor = listOf(
    Color(0xFF8B00FF), Color(0xFF4B0082), Color(0xFF1E6FFF), Color(0xFF1FA23A),
    Color(0xFFE6A800), Color(0xFFFF7A00), Color(0xFFE53935)
)

/** [text] with each letter in turn in a VIBGYOR colour (spaces skipped). Used for the learner's name. */
internal fun rainbowText(text: String): androidx.compose.ui.text.AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    var n = 0
    text.forEach { ch ->
        if (ch.isWhitespace()) append(ch)
        else {
            pushStyle(androidx.compose.ui.text.SpanStyle(color = Vibgyor[n % Vibgyor.size]))
            append(ch)
            pop()
            n++
        }
    }
}

/** Colours each letter of a text field in turn with the VIBGYOR colours. */
private object RainbowText : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText =
        androidx.compose.ui.text.input.TransformedText(rainbowText(text.text), androidx.compose.ui.text.input.OffsetMapping.Identity)
}
