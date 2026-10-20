package com.example.platform

import androidx.compose.runtime.ProvidableCompositionLocal

actual typealias Context = android.content.Context

actual val LocalContext: ProvidableCompositionLocal<Context> = androidx.compose.ui.platform.LocalContext

/** Holds the application context so platform helpers can work without a Context parameter. */
object AppContextHolder {
    lateinit var context: android.content.Context

    val isInitialized: Boolean get() = ::context.isInitialized
}
