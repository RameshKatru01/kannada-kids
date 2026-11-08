package com.example.platform

import io.ktor.client.HttpClient

/** Plays short sound effects and audio clips. */
expect object PlatformAudio {
    /** Plays mono 16-bit PCM samples (used for the synthesised pop / chime / fanfare sounds). */
    fun playPcm16(samples: ShortArray, sampleRate: Int)

    /**
     * Plays an encoded audio clip (wav / mp3) held in memory. When [exclusive] is true, the
     * previous exclusive clip (e.g. the last spoken letter) is stopped first.
     */
    fun playBytes(bytes: ByteArray, fileExtension: String, exclusive: Boolean = false)

    /** Plays a short notification-style beep (replacement for ToneGenerator). */
    fun playBeep(triumphant: Boolean)

    /** Starts looping mono 16-bit PCM (background music). Returns a handle for [stopLoop]. */
    fun startLoop(samples: ShortArray, sampleRate: Int): Any?

    /** Stops a loop started with [startLoop]. */
    fun stopLoop(handle: Any?)
}

/** App / system media volume used by the pause menu slider. */
expect object SystemVolume {
    fun getMaxVolume(): Int
    fun getVolume(): Int
    fun setVolume(volume: Int)
}

/** HTTP client with the right engine for the platform (OkHttp on Android, NSURLSession on iOS). */
expect fun createHttpClient(): HttpClient

/** Milliseconds since the Unix epoch. */
expect fun currentTimeMillis(): Long

/** Formats a timestamp in the device time zone using a pattern such as "dd MMM yyyy" (US English month names). */
expect fun formatDate(millis: Long, pattern: String = "dd MMM yyyy"): String

/** Adds calendar years / months / days to a timestamp in the device time zone (like java.util.Calendar.add). */
expect fun addToDate(millis: Long, years: Int = 0, months: Int = 0, days: Int = 0): Long

/** "Android" or "iOS" */
expect val platformName: String

/** App version and phone model, e.g. "App 1.1 (2) · Pixel 7 · Android 15" (for support emails). */
expect fun supportDeviceInfo(): String
