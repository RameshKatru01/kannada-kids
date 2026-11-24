package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.example.platform.LocalContext
import com.example.platform.ToastHost
import com.example.platform.preloadVideo
import com.example.ui.data.AudioFeedbackManager
import com.example.ui.data.ScoreManager
import com.example.ui.data.ThemeManager
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme

/** Shared entry point used by both the Android MainActivity and the iOS MainViewController. */
@Composable
fun App() {
    setSingletonImageLoaderFactory { platformContext ->
        ImageLoader.Builder(platformContext)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }

    val context = LocalContext.current
    remember(context) {
        ScoreManager.init(context)
        ThemeManager.init(context)
        AudioFeedbackManager.init(context)
        true
    }

    val currentTheme by ThemeManager.currentTheme.collectAsState()
    MyApplicationTheme(appTheme = currentTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            com.example.ui.data.TrackActiveTime()
            LaunchedEffect(Unit) {
                // Subscription first (Gold, free trial, the family's record for the admin list), so it
                // never waits for the video downloads below
                try {
                    com.example.ui.data.SubscriptionManager.loadProducts()
                    com.example.ui.data.SubscriptionManager.refresh()
                    com.example.ui.data.AdminRoles.load()
                } catch (e: Exception) {
                }
            }
            LaunchedEffect(Unit) {
                // Where the videos are served from (can be changed without an app update)
                try {
                    com.example.ui.data.VideoHost.refresh()
                } catch (e: Exception) {
                }
                // Download the opening video of the first Fun Videos folders now, so the first video
                // a child opens is already on the phone (a few seconds after launch, not at once)
                try {
                    kotlinx.coroutines.delay(2000)
                    listOf("Vowels", "Consonants", "Birds").forEach { section ->
                        com.example.ui.screens.videoUrlsFor(section).firstOrNull()?.let { preloadVideo(it) }
                    }
                } catch (e: Exception) {
                }
            }
            AppNavigation()
            ToastHost()
        }
    }
}
