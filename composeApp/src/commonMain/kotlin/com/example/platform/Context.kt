package com.example.platform

import androidx.compose.runtime.ProvidableCompositionLocal

/**
 * Cross-platform stand-in for android.content.Context.
 *
 * On Android this is exactly android.content.Context (a typealias), so existing code that
 * passes `context` around keeps working. On iOS it is a lightweight placeholder object.
 * Screens get it with `LocalContext.current`, just like before.
 */
expect abstract class Context

/** Same role as LocalContext, available on every platform. */
expect val LocalContext: ProvidableCompositionLocal<Context>
