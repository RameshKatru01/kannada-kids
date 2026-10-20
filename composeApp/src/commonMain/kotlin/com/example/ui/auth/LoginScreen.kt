package com.example.ui.auth

import com.example.platform.*
import com.example.resources.Res
import com.example.resources.ic_google_logo

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.painterResource

// "Letter Pop" landing screen: minimal, colourful, ready for Kannada, Hindi and English.
private val SkyBackground = Color(0xFFEAF6FF)
private val Ink = Color(0xFF0B3B66)

// VIBGYOR colours, deepened so they stay readable on the light sky background.
private val TitleRainbow = listOf(
    Color(0xFF7B2FF7), Color(0xFF3F3DBC), Color(0xFF1E6FD9), Color(0xFF16994A),
    Color(0xFFB8860B), Color(0xFFE8630A), Color(0xFFE53935)
)
private val TaglineRainbow = listOf(
    Color(0xFF6A1FD8), Color(0xFF3533A8), Color(0xFF1560C2), Color(0xFF12803E),
    Color(0xFF8A6400), Color(0xFFB84D00), Color(0xFFC62828)
)

/** Colours each letter (or each word) of [text] in turn with the rainbow [colors]. */
private fun rainbowText(text: String, colors: List<Color>, perWord: Boolean): AnnotatedString =
    buildAnnotatedString {
        var i = 0
        if (perWord) {
            val words = text.split(" ")
            words.forEachIndexed { index, word ->
                withStyle(SpanStyle(color = colors[i++ % colors.size])) { append(word) }
                if (index < words.lastIndex) append(" ")
            }
        } else {
            text.forEach { ch ->
                if (ch == ' ') append(ch)
                else withStyle(SpanStyle(color = colors[i++ % colors.size])) { append(ch) }
            }
        }
    }

private data class LetterBubble(
    val letter: String,
    val color: Color,
    val textColor: Color,
    /** Position and size in the 390 x 340 design area. */
    val x: Float,
    val y: Float,
    val size: Float,
    val floatMs: Int
)

private val bubbles = listOf(
    LetterBubble("ಅ", Color(0xFFFF6B6B), Color.White, 30f, 86f, 120f, 4000),
    LetterBubble("अ", Color(0xFFFFC93C), Ink, 190f, 54f, 96f, 4600),
    LetterBubble("A", Color(0xFF6C63FF), Color.White, 290f, 166f, 70f, 3800),
    LetterBubble("क", Color(0xFF2EC4B6), Ink, 168f, 188f, 104f, 5000),
    LetterBubble("b", Color(0xFFFF9F1C), Ink, 66f, 252f, 60f, 4300),
    LetterBubble("ಕ", Color(0xFFE84393), Color.White, 300f, 280f, 50f, 4800),
    LetterBubble("", Color(0xFF6C63FF), Color.White, 20f, 40f, 16f, 3500),
    LetterBubble("", Color(0xFF2EC4B6), Color.White, 330f, 80f, 14f, 3900)
)

private data class LearningLanguage(val code: String, val label: String, val available: Boolean)

private val learningLanguages = listOf(
    LearningLanguage("kn", "ಕನ್ನಡ", available = true),
    LearningLanguage("hi", "हिन्दी", available = false),
    LearningLanguage("en", "English", available = false)
)

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val currentLanguage by com.example.ui.data.LanguageManager.currentLanguage.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onNavigateToHome()
        }
    }

    val isLoading = authState is AuthState.Loading
    val haptic = LocalHapticFeedback.current
    var showAdminLogin by remember { mutableStateOf(false) }

    if (showAdminLogin) {
        AdminLoginDialog(
            onDismiss = { showAdminLogin = false },
            onSignIn = { email ->
                showAdminLogin = false
                viewModel.signInWithGmailAddress(context, email)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBackground)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Floating letter bubbles
        LetterBubbleField(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .aspectRatio(390f / 352f)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Long-press the title for the hidden admin sign-in.
            Text(
                text = rainbowText("Letter Pop", TitleRainbow, perWord = false),
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .testTag("admin_long_press_title")
                    .pointerInput(isLoading) {
                        detectTapGestures(
                            onLongPress = {
                                if (!isLoading) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showAdminLogin = true
                                }
                            }
                        )
                    }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rainbowText("Pop a bubble, learn a letter.", TaglineRainbow, perWord = true),
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            LanguageSwitch(
                selectedCode = currentLanguage.code,
                onSelect = { language ->
                    if (!language.available) {
                        Toast.makeText(context, "${language.label} is coming soon!", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Error banner (e.g. Google sign-in not available on this device)
            val error = authState as? AuthState.Error
            if (error != null) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error.message,
                            fontSize = 13.sp,
                            color = Color(0xFFB71C1C),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFFB71C1C),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Continue with Google
            Surface(
                onClick = {
                    com.example.ui.audio.SoundEffectsHelper.playButtonTap(context)
                    viewModel.signInWithGoogle(context)
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("google_login_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Ink,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Signing in…",
                            color = Ink,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                    } else {
                        Icon(
                            painter = painterResource(Res.drawable.ic_google_logo),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            color = Ink,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Explore as a guest
            TextButton(
                onClick = {
                    com.example.ui.audio.SoundEffectsHelper.playButtonTap(context)
                    viewModel.signInAsGuest(context)
                },
                enabled = !isLoading,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("guest_explorer_button")
            ) {
                Text(
                    text = "Explore as a guest",
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * Hidden admin sign-in, opened by long-pressing the "Letter Pop" title.
 * Only e-mail addresses that are admins in the user directory are accepted.
 */
@Composable
private fun AdminLoginDialog(
    onDismiss: () -> Unit,
    onSignIn: (String) -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // Make sure the admin list is loaded before checking.
        com.example.ui.data.UserManager.init(context)
    }

    fun submit() {
        val trimmed = email.trim()
        val fullEmail = if (trimmed.isNotEmpty() && !trimmed.contains("@")) "$trimmed@gmail.com" else trimmed
        when {
            fullEmail.isEmpty() -> error = "Enter the admin e-mail address."
            !com.example.ui.data.UserManager.isAdmin(fullEmail) -> error = "This is not an admin account."
            else -> onSignIn(fullEmail)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Admin sign-in", fontWeight = FontWeight.ExtraBold, color = Ink)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        error = null
                    },
                    label = { Text("Admin e-mail") },
                    singleLine = true,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_email_field")
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = error ?: "", color = Color(0xFFB71C1C), fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }) {
                Text("Sign in", fontWeight = FontWeight.ExtraBold, color = Ink)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Ink)
            }
        },
        containerColor = Color.White
    )
}

@Composable
private fun LanguageSwitch(
    selectedCode: String,
    onSelect: (LearningLanguage) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        learningLanguages.forEach { language ->
            val selected = language.code == selectedCode
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (selected) Ink else Color.White)
                    .clickable(role = Role.Tab) { onSelect(language) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = language.label,
                    color = if (selected) Color.White else Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LetterBubbleField(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        // Scale the 390 x 340 design to the space available, sitting just above the title.
        val scale = minOf(maxWidth.value / 390f, (maxHeight.value - 12f).coerceAtLeast(1f) / 340f)
        val offsetX = (maxWidth.value - 390f * scale) / 2f
        val offsetY = maxHeight.value - 12f - 340f * scale
        val transition = rememberInfiniteTransition(label = "bubbles")

        bubbles.forEach { bubble ->
            val drift by transition.animateFloat(
                initialValue = 0f,
                targetValue = -10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(bubble.floatMs, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "drift_${bubble.x}"
            )
            val sizeDp: Dp = (bubble.size * scale).dp
            Box(
                modifier = Modifier
                    .offset(
                        x = (offsetX + bubble.x * scale).dp,
                        y = (offsetY + bubble.y * scale + drift).dp
                    )
                    .size(sizeDp)
                    .background(bubble.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (bubble.letter.isNotEmpty()) {
                    Text(
                        text = bubble.letter,
                        color = bubble.textColor,
                        fontSize = (bubble.size * scale * 0.5f).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
