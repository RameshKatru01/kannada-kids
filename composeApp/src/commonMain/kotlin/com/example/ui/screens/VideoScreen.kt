package com.example.ui.screens

import com.example.platform.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightCyan
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.AuthState
import androidx.compose.material.icons.filled.Lock

@Composable
fun VideoPlayerItem(uri: String, isCurrentPage: Boolean) {
    val videoPlayer = rememberVideoPlayer(url = uri, loop = true, autoPlay = false)

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            videoPlayer.play()
        } else {
            videoPlayer.pause()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        VideoSurface(
            controller = videoPlayer,
            showNativeControls = true,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** Fun Videos folders that have Hindi videos (the rest are Kannada only). */
val hindiVideoFolders: List<String> = listOf("Vowels", "Fruits", "Birds")

/** The video links for a Fun Videos section ([title] is the folder name). */
fun videoUrlsFor(title: String): List<String> {
    // Letter videos with on-screen text: alphabet_with_text/video01-15 = vowels ಅ…ಅಃ, video16-49 = consonants ಕ…ಳ
    return if (title == "Vowels") {
            // Hindi: स्वर videos (अ अनार … अं अंगूर)
            if (com.example.data.Lang.isHindi) listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/17e05a004c689f8fe3aca0b3f28cc44f7d3474e9/assets/videos/vowels_hi/vowel_hi_01.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/17e05a004c689f8fe3aca0b3f28cc44f7d3474e9/assets/videos/vowels_hi/vowel_hi_02.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/17e05a004c689f8fe3aca0b3f28cc44f7d3474e9/assets/videos/vowels_hi/vowel_hi_03.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/17e05a004c689f8fe3aca0b3f28cc44f7d3474e9/assets/videos/vowels_hi/vowel_hi_04.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/17e05a004c689f8fe3aca0b3f28cc44f7d3474e9/assets/videos/vowels_hi/vowel_hi_05.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/17e05a004c689f8fe3aca0b3f28cc44f7d3474e9/assets/videos/vowels_hi/vowel_hi_06.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/17e05a004c689f8fe3aca0b3f28cc44f7d3474e9/assets/videos/vowels_hi/vowel_hi_07.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/17e05a004c689f8fe3aca0b3f28cc44f7d3474e9/assets/videos/vowels_hi/vowel_hi_08.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/17e05a004c689f8fe3aca0b3f28cc44f7d3474e9/assets/videos/vowels_hi/vowel_hi_09.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/17e05a004c689f8fe3aca0b3f28cc44f7d3474e9/assets/videos/vowels_hi/vowel_hi_10.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/17e05a004c689f8fe3aca0b3f28cc44f7d3474e9/assets/videos/vowels_hi/vowel_hi_11.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/17e05a004c689f8fe3aca0b3f28cc44f7d3474e9/assets/videos/vowels_hi/vowel_hi_12.mp4"
            ) else listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video01.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video02.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video03.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video04.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video05.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video06.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video07.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video08.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video09.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video10.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video11.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video12.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video13.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video14.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video15.mp4"
            )
        } else if (title == "Consonants") {
            listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video16.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video17.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video18.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video19.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video20.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video21.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video22.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video23.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video24.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video25.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video26.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video27.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video28.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video29.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video30.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video31.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video32.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video33.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video34.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video35.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video36.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video37.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video38.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video39.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video40.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video41.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video42.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video43.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video44.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video45.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video46.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video47.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video48.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/alphabet_with_text/video49.mp4"
            )
        } else if (title == "Numbers") {
            // numbers_with_text/number00-10 = ೦ … ೧೦
            listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/numbers_with_text/number00.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/numbers_with_text/number01.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/numbers_with_text/number02.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/numbers_with_text/number03.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/numbers_with_text/number04.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/numbers_with_text/number05.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/numbers_with_text/number06.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/numbers_with_text/number07.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/numbers_with_text/number08.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/numbers_with_text/number09.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/numbers_with_text/number10.mp4"
            )
        } else if (title == "Letter Quiz") {
            listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_01.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_02.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_03.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_04.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_05.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_06.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_07.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_08.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_09.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_10.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_11.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_12.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_13.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_14.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_15.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_16.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_17.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_18.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_19.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_20.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_21.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_22.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_23.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_24.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_25.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_26.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_27.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_28.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_29.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_30.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_31.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_32.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_33.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_34.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_35.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_36.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_37.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_38.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_39.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_40.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_41.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_42.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_43.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_44.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_45.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_46.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_47.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_48.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_letter_49.mp4"
            )
        } else if (title == "Picture Quiz") {
            listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_01.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_02.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_03.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_04.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_05.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_06.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_07.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_08.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_09.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_10.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_11.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_12.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_13.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_14.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_16.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_17.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_18.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_19.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_21.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_22.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_23.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_24.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_26.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_27.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_28.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_29.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_31.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_32.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_33.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_34.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_35.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_36.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_37.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_38.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_39.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_40.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_41.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_42.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_43.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_44.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_45.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_46.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_47.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_picture_48.mp4"
            )
        } else if (title == "Number Quiz") {
            listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_number_00.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_number_01.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_number_02.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_number_03.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_number_04.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_number_05.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_number_06.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_number_07.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_number_08.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_number_09.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_number_10.mp4"
            )
        } else if (title == "How Many?") {
            listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_howmany_00.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_howmany_01.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_howmany_02.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_howmany_03.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_howmany_04.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_howmany_05.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_howmany_06.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_howmany_07.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_howmany_08.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_howmany_09.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/ed79c3d59bc6f89784da6c8531c2462d0c6bbd4e/assets/videos/quiz/quiz_howmany_10.mp4"
            )
        } else if (title == "Class Room") {
            listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/d172d9c98346cca20182a48632504f19967005a4/assets/videos/class_room/class_room_01.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/d172d9c98346cca20182a48632504f19967005a4/assets/videos/class_room/class_room_02.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/d172d9c98346cca20182a48632504f19967005a4/assets/videos/class_room/class_room_03.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/d172d9c98346cca20182a48632504f19967005a4/assets/videos/class_room/class_room_04.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/d172d9c98346cca20182a48632504f19967005a4/assets/videos/class_room/class_room_05.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/d172d9c98346cca20182a48632504f19967005a4/assets/videos/class_room/class_room_06.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/d172d9c98346cca20182a48632504f19967005a4/assets/videos/class_room/class_room_07.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/d172d9c98346cca20182a48632504f19967005a4/assets/videos/class_room/class_room_08.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/d172d9c98346cca20182a48632504f19967005a4/assets/videos/class_room/class_room_09.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/d172d9c98346cca20182a48632504f19967005a4/assets/videos/class_room/class_room_10.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/d172d9c98346cca20182a48632504f19967005a4/assets/videos/class_room/class_room_11.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/d172d9c98346cca20182a48632504f19967005a4/assets/videos/class_room/class_room_12.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/d172d9c98346cca20182a48632504f19967005a4/assets/videos/class_room/class_room_13.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/d172d9c98346cca20182a48632504f19967005a4/assets/videos/class_room/class_room_14.mp4"
            )
        } else if (title == "Play Time") {
            listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/1f165651adfb710949b40924ecb394dc6fb7b8ed/playtime_1.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/1f165651adfb710949b40924ecb394dc6fb7b8ed/playtime_2.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/1f165651adfb710949b40924ecb394dc6fb7b8ed/playtime_3.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/1f165651adfb710949b40924ecb394dc6fb7b8ed/playtime_4.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/1f165651adfb710949b40924ecb394dc6fb7b8ed/playtime_5.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/1f165651adfb710949b40924ecb394dc6fb7b8ed/playtime_6.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/1f165651adfb710949b40924ecb394dc6fb7b8ed/playtime_7.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/1f165651adfb710949b40924ecb394dc6fb7b8ed/playtime_8.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/1f165651adfb710949b40924ecb394dc6fb7b8ed/playtime_9.mp4"
            )
        } else if (title == "Animals") {
            listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/8a19baa98cadfe0f650e61b4d22eaf72f408f201/ANIMAL_Baby_elephant_playing_in_water_20261017000510.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/8a19baa98cadfe0f650e61b4d22eaf72f408f201/ANIMAL_Baby_giraffe_eating_leaf_20261017002157.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/8a19baa98cadfe0f650e61b4d22eaf72f408f201/ANIMAL_Baby_tiger_pounces_on_dandelion_20261017002538.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/8a19baa98cadfe0f650e61b4d22eaf72f408f201/ANIMAL_Bear_cub_eating_honey_20261017002121.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/8a19baa98cadfe0f650e61b4d22eaf72f408f201/ANIMAL_Camel_walking_on_sand_dune_20261017002150.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/8a19baa98cadfe0f650e61b4d22eaf72f408f201/ANIMAL_Lion_cub_roaring_with_typography_20261017002101.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/8a19baa98cadfe0f650e61b4d22eaf72f408f201/ANIMAL_Monkey_swings_and_eats_banana_20261017002125.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/8a19baa98cadfe0f650e61b4d22eaf72f408f201/ANIMAL_Puppy_playing_on_green_lawn_20261017002120.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/8a19baa98cadfe0f650e61b4d22eaf72f408f201/ANIMAL_Spotted_cow_eating_green_grass_20261017002137.mp4"
            )
        } else if (title == "Birds") {
            // Hindi: the same bird clips with the Hindi word and voice
            if (com.example.data.Lang.isHindi) listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/birds_ai/hi/birds_hi_01.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/birds_ai/hi/birds_hi_02.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/birds_ai/hi/birds_hi_03.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/birds_ai/hi/birds_hi_04.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/birds_ai/hi/birds_hi_05.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/birds_ai/hi/birds_hi_06.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/birds_ai/hi/birds_hi_07.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/birds_ai/hi/birds_hi_08.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/birds_ai/hi/birds_hi_09.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/birds_ai/hi/birds_hi_10.mp4"
            ) else listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/76eac0906e8eb2ee571914fd7f9ebac1e2dcbecf/BIRD_cBaby_eagle_leaping_on_cliff_20261017004242.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/76eac0906e8eb2ee571914fd7f9ebac1e2dcbecf/BIRD_cBaby_owl_waving_on_branch_20261017004259.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/76eac0906e8eb2ee571914fd7f9ebac1e2dcbecf/BIRD_cBaby_swan_gliding_on_lake_20261017004303.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/76eac0906e8eb2ee571914fd7f9ebac1e2dcbecf/BIRD_cCrane_catching_fish_in_marsh_20261017004310.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/76eac0906e8eb2ee571914fd7f9ebac1e2dcbecf/BIRD_cGreen_parrot_eating_chili_on_20261017004203.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/76eac0906e8eb2ee571914fd7f9ebac1e2dcbecf/BIRD_cHen_pecking_in_barnyard_20261017004234.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/76eac0906e8eb2ee571914fd7f9ebac1e2dcbecf/BIRD_cPeacock_dancing_in_rain_20261017004209.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/76eac0906e8eb2ee571914fd7f9ebac1e2dcbecf/BIRD_crow_drinking_water_20261017004218.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/76eac0906e8eb2ee571914fd7f9ebac1e2dcbecf/BIRD_cSparrow_hopping_on_wooden_fence_20261017004227.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/76eac0906e8eb2ee571914fd7f9ebac1e2dcbecf/BIRDAnimated_pigeon_pecking_breadcrumbs_20261017004248.mp4"
            )
        } else if (title == "Fruits") {
            // AI (Google Flow) clips with the word and voice added, 9:16 for phones
            if (com.example.data.Lang.isHindi) listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/hi/fruits_hi_01.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/hi/fruits_hi_02.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/hi/fruits_hi_03.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/hi/fruits_hi_04.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/hi/fruits_hi_05.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/hi/fruits_hi_06.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/hi/fruits_hi_07.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/hi/fruits_hi_08.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/hi/fruits_hi_09.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/hi/fruits_hi_10.mp4"
            ) else listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/kn/fruits_kn_01.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/kn/fruits_kn_02.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/kn/fruits_kn_03.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/kn/fruits_kn_04.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/kn/fruits_kn_05.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/kn/fruits_kn_06.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/kn/fruits_kn_07.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/kn/fruits_kn_08.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/kn/fruits_kn_09.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/0aab9912cfa6f8ad8eddc0fd3e46873b956c32b8/assets/videos/fruits_ai/kn/fruits_kn_10.mp4"
            )
        } else if (title == "Vegetables") {
            listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/vegetables/vegetables_01.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/vegetables/vegetables_02.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/vegetables/vegetables_03.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/vegetables/vegetables_04.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/vegetables/vegetables_05.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/vegetables/vegetables_06.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/vegetables/vegetables_07.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/vegetables/vegetables_08.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/vegetables/vegetables_09.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/vegetables/vegetables_10.mp4"
            )
        } else if (title == "Body Parts") {
            listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/body_parts/body_parts_01.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/body_parts/body_parts_02.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/body_parts/body_parts_03.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/body_parts/body_parts_04.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/body_parts/body_parts_05.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/body_parts/body_parts_06.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/body_parts/body_parts_07.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/body_parts/body_parts_08.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/body_parts/body_parts_09.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/body_parts/body_parts_10.mp4"
            )
        } else if (title == "Family") {
            listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/family/family_01.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/family/family_02.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/family/family_03.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/family/family_04.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/family/family_05.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/family/family_06.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/family/family_07.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/family/family_08.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/family/family_09.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/9bb58166ebfdeed205319cc42b9d1c6099dc686f/assets/videos/themes/family/family_10.mp4"
            )
        } else if (title == "Colors" || title == "Colours") {
            listOf(
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/4b45114996510ce37844caae00109c86706b72a2/COLOR_Black_blob_transforms_into_umbrella_20261017010034.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/4b45114996510ce37844caae00109c86706b72a2/COLOR_Blue_paint_blob_morphing_20261017005946.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/4b45114996510ce37844caae00109c86706b72a2/COLOR_Fuzzy_brown_blob_transforming_20261017010028.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/4b45114996510ce37844caae00109c86706b72a2/COLOR_Green_blob_transforms_into_frog_20261017010004.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/4b45114996510ce37844caae00109c86706b72a2/COLOR_Orange_blob_juggles_fruit_20261017010013.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/4b45114996510ce37844caae00109c86706b72a2/COLOR_Pink_blob_transforms_and_bounces_20261017010025.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/4b45114996510ce37844caae00109c86706b72a2/COLOR_Purple_blob_morphing_into_fruits_20261017010018.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/4b45114996510ce37844caae00109c86706b72a2/COLOR_Red_blob_transforming_into_objects_20261017005942.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/4b45114996510ce37844caae00109c86706b72a2/COLOR_White_blob_transforms_into_dove_20261017010031.mp4",
                "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/4b45114996510ce37844caae00109c86706b72a2/COLOR_Yellow_blob_transforms_into_sun_20261017005956.mp4"
            )
        } else {
            // No videos for this folder yet (the screen says so instead of playing a stand-in clip)
            emptyList()
        }
    
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(title: String, onBack: () -> Unit, authViewModel: AuthViewModel = viewModel { AuthViewModel() }) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val userEmail = (authState as? AuthState.Authenticated)?.email ?: "guest"
    val prefs = getSharedPreferences("AppProgress")
    val currentLanguage by com.example.ui.data.LanguageManager.currentLanguage.collectAsState()
    val langCode = currentLanguage.code
    val subscriptionPlan = prefs.getString("${langCode}_subscriptionPlan_$userEmail", "Free") ?: "Free"

    

    
    val videoUris = remember(title) { videoUrlsFor(title) }

    val pagerState = rememberPagerState(pageCount = { videoUris.size })

    // Fetch the next two videos in the background so swiping to them is instant. Started a moment
    // after the current video begins so it doesn't compete with it for bandwidth.
    LaunchedEffect(pagerState.currentPage, videoUris) {
        kotlinx.coroutines.delay(1500)
        for (next in pagerState.currentPage + 1..pagerState.currentPage + 2) {
            videoUris.getOrNull(next)?.let { preloadVideo(it) }
        }
    }

    // Full-screen video: hide the status / navigation bars while this screen is open.
    HideSystemBars()

    if (videoUris.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF10121A)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Text("🎬", fontSize = 56.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No recordings in $title",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recordings in this section have been cleared.",
                    fontSize = 14.sp,
                    color = Color(0xFFB0BEC5),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = BrightCyan),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Back to Folders ↩", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(percent = 50))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val isGuest = com.example.ui.data.UserManager.isGuest(userEmail)
            val isUnlocked = com.example.ui.data.FeatureFlags.UNLOCK_ALL || (!isGuest && subscriptionPlan == "Gold") || page == 0
            
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    VideoPlayerItem(
                        uri = videoUris[page],
                        isCurrentPage = (page == pagerState.currentPage)
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isGuest) "🔒 Guest Preview: Only the 1st video is unlocked!" else "Upgrade to Gold to unlock all videos!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isGuest) "Sign in with your Gmail account to watch all videos." else "Subscribe to access the complete video library.",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        // Floating Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(percent = 50))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}
