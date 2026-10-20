package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.example.platform.LocalContext
import com.example.platform.ToastHost
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
            AppNavigation()
            ToastHost()
        }
    }
}
