package com.example.ui.screens

import com.example.platform.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.KannadaLetter
import com.example.data.LetterData
import com.example.ui.components.LetterTraceVideoPlayer
import com.example.ui.data.LanguageManager
import com.example.ui.theme.*

fun getVideoUrlForLetter(letter: KannadaLetter): String? {
    val base = "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/main"
    return letter.videoUrl ?: when (letter.character) {
        "ಅ" -> "$base/Final_A.mp4"
        "ಆ" -> "$base/Final%20AA.mp4"
        "ಇ" -> "$base/Final_E.mp4"
        "ಈ" -> "$base/Final_EE.mp4"
        "ಉ" -> "$base/Final_u.mp4"
        "ಊ" -> "$base/Final_uu.mp4"
        "ಋ" -> "$base/Final_RU.mp4"
        "ಎ" -> "$base/Final_ye.mp4"
        "ಏ" -> "$base/Final_yeee.mp4"
        "ಐ" -> "$base/Final_I.mp4"
        "ಒ" -> "$base/Final_O.mp4"
        "ಓ" -> "$base/Final_OO.mp4"
        "ಔ" -> "$base/Final_ouh.mp4"
        "ಅಂ" -> "$base/Final_AUM.mp4"
        "ಅಃ" -> "$base/Final_AHA.mp4"
        "ಕ" -> "$base/Vowel_KA.mp4"
        "ಖ" -> "$base/Vowel_KHA.mp4"
        "ಗ" -> "$base/Vowel_GA.mp4"
        "ಘ" -> "$base/Vowel_GHHHA.mp4"
        "ಙ" -> "$base/Vowle_Inya.mp4"
        "ಚ" -> "$base/Vowel_cha.mp4"
        "ಛ" -> "$base/Vowel_CHHHA.mp4"
        "ಜ" -> "$base/Vowel_JA.mp4"
        "ಝ" -> "$base/Vowel_JJJAA.mp4"
        "ಞ" -> "$base/Vowel_INI.mp4"
        "ಟ" -> "$base/Vowel_Ta.mp4"
        "ಠ" -> "$base/Vowel_tttta.mp4"
        "ಡ" -> "$base/Vowle_Da.mp4"
        "ಢ" -> "$base/Vowel_DDDAA.mp4"
        "ಣ" -> "$base/Vowel_ANA.mp4"
        "ತ" -> "$base/Vowel_Tha.mp4"
        "ಥ" -> "$base/Vowel_THA2.mp4"
        "ದ" -> "$base/Vowel_Dha.mp4"
        "ಧ" -> "$base/Vowel_DHA2.mp4"
        "ನ" -> "$base/Vowels_Na.mp4"
        "ಪ" -> "$base/Vowel_Pa.mp4"
        "ಫ" -> "$base/Vowel_Pha2.mp4"
        "ಬ" -> "$base/Vowel_Ba.mp4"
        "ಭ" -> "$base/Vowel_Bha2.mp4"
        "ಮ" -> "$base/Vowel_ma.mp4"
        "ಯ" -> "$base/Vowel_Ya.mp4"
        "ರ" -> "$base/Vowel_Ra.mp4"
        "ಲ" -> "$base/Vowel_La.mp4"
        "ವ" -> "$base/Vowel_va.mp4"
        "ಶ" -> "$base/Vowel%20SSA.mp4"
        "ಷ" -> "$base/Vowel_Sha.mp4"
        "ಸ" -> "$base/Vowel_Sa.mp4"
        "ಹ" -> "$base/Vowel_ha.mp4"
        "ಳ" -> "$base/Vowel_lha.mp4"
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    var selectedLetterForVideo by remember { mutableStateOf<KannadaLetter?>(null) }

    val authPrefs = remember { getSharedPreferences("AppProgress") }
    val userEmail = authPrefs.getString("last_active_user", "guest") ?: "guest"
    val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reference Library", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        },
        containerColor = Color(0xFFE1F5FE)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Surface(
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = "Videos Available",
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (isGuest) "🔒 Guest mode: First letter unlocked in each section!" else "Tap any Vowel or Consonant to watch its stroke writing video!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF5D4037)
                        )
                    }
                }
            }

            item {
                ReferenceSection(
                    title = "Vowels",
                    items = LetterData.vowels,
                    color = BrightCyan,
                    hasVideos = true,
                    isGuest = isGuest,
                    onClick = { letter ->
                        com.example.ui.audio.NativeAudioPlayer.playPhonics(context, letter.character)
                        val videoUrl = getVideoUrlForLetter(letter)
                        if (videoUrl != null) {
                            selectedLetterForVideo = letter.copy(videoUrl = videoUrl)
                        }
                    }
                )
            }
            item {
                ReferenceSection(
                    title = "Consonants",
                    items = LetterData.consonants,
                    color = BrightGreen,
                    hasVideos = true,
                    isGuest = isGuest,
                    onClick = { letter ->
                        com.example.ui.audio.NativeAudioPlayer.playPhonics(context, letter.character)
                        val videoUrl = getVideoUrlForLetter(letter)
                        if (videoUrl != null) {
                            selectedLetterForVideo = letter.copy(videoUrl = videoUrl)
                        }
                    }
                )
            }
            item {
                ReferenceSection(
                    title = "Guninthakshari (Ka)",
                    items = LetterData.kaGuninthakshari,
                    color = Color(0xFFBA68C8),
                    hasVideos = false,
                    isGuest = isGuest,
                    onClick = { letter ->
                        com.example.ui.audio.NativeAudioPlayer.playPhonics(context, letter.character)
                    }
                )
            }
            item {
                ReferenceSection(
                    title = "Numbers",
                    items = LetterData.numbers,
                    color = Color(0xFFFF9800),
                    hasVideos = false,
                    isGuest = isGuest,
                    onClick = { letter ->
                        com.example.ui.audio.NativeAudioPlayer.playPhonics(context, letter.character)
                    }
                )
            }
        }
    }

    // Video Demonstration Popup Dialog
    selectedLetterForVideo?.let { letter ->
        val videoUrl = getVideoUrlForLetter(letter)
        if (videoUrl != null) {
            Dialog(
                onDismissRequest = { selectedLetterForVideo = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Dialog Header: letter badge, info & close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(
                                            if (letter.isVowel) BrightCyan else BrightGreen,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = letter.character,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Column {
                                    Text(
                                        text = "${letter.character} (${letter.transliteration})",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoTitle
                                    )
                                    Text(
                                        text = if (letter.isVowel) "Vowel Stroke Video" else "Consonant Stroke Video",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            IconButton(
                                onClick = { selectedLetterForVideo = null },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFEEEEEE), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Video Player (Streams directly from GitHub)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        ) {
                            LetterTraceVideoPlayer(
                                modifier = Modifier.fillMaxSize(),
                                letterCharacter = letter.character,
                                letterName = letter.transliteration,
                                videoUrl = videoUrl
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dialog Bottom Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    com.example.ui.audio.NativeAudioPlayer.playPhonics(context, letter.character)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Pronounce",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pronounce", fontSize = 13.sp)
                            }

                            Button(
                                onClick = { selectedLetterForVideo = null },
                                colors = ButtonDefaults.buttonColors(containerColor = BentoTitle),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Close", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReferenceSection(
    title: String,
    items: List<KannadaLetter>,
    color: Color,
    hasVideos: Boolean = false,
    isGuest: Boolean = false,
    onClick: (KannadaLetter) -> Unit
) {
    val context = LocalContext.current
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = BentoTitle
            )
            if (hasVideos) {
                Surface(
                    color = color.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = "Video",
                            tint = BentoTitle,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Videos",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTitle
                        )
                    }
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEachIndexed { index, letter ->
                val isLocked = isGuest && index > 0
                val itemColor = if (isLocked) Color(0xFFEEEEEE) else color
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = itemColor,
                    shadowElevation = if (isLocked) 1.dp else 3.dp,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { 
                            if (isLocked) {
                                Toast.makeText(context, "🔒 Locked: Sign in with Gmail to explore all letters!", Toast.LENGTH_SHORT).show()
                            } else {
                                onClick(letter) 
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = letter.character,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (letter.transliteration.isNotEmpty()) {
                                    Text(
                                        text = letter.transliteration,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }

                            if (hasVideos || letter.videoUrl != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(16.dp)
                                        .background(Color.Black.copy(alpha = 0.25f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Video available",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
