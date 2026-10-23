package com.example.ui.audio

import com.example.platform.*

/**
 * Speaks Kannada letters and words. The voice comes from clips built into the app ([SpeechBank]);
 * the unofficial Google Translate voice is no longer used.
 */
object NativeAudioPlayer {
    private var lastText: String? = null
    private var lastRequestAt = 0L

    fun playPhonics(context: Context, text: String) {
        if (text.isBlank()) return
        // The same letter is often requested twice in a row: play it only once.
        val now = currentTimeMillis()
        if (text == lastText && now - lastRequestAt < 1200L) return
        lastText = text
        lastRequestAt = now
        SpeechBank.play(context, text)
    }

    fun release() {
        // Nothing to release: each clip is played by a short-lived platform player.
    }
}
