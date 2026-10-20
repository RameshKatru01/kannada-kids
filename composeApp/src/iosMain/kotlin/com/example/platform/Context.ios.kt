package com.example.platform

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

actual abstract class Context

/** The single iOS "context". iOS has no Context concept, so this is only a placeholder. */
object IosContext : Context()

actual val LocalContext: ProvidableCompositionLocal<Context> = staticCompositionLocalOf { IosContext }
