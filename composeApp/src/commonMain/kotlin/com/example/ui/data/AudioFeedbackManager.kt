package com.example.ui.data

import com.example.platform.*
import kotlin.random.Random

object AudioFeedbackManager {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val encouragements = listOf(
        "Great job!",
        "Awesome!",
        "You did it!",
        "Fantastic!",
        "Super star!"
    )

    private val badgeUnlockMessages = listOf(
        "Wow, you earned a new badge!",
        "Badge unlocked! Great work!",
        "A new badge! Keep it up!"
    )

    fun init(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                    isInitialized = true
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun playLevelComplete(context: Context) {
        PlatformAudio.playBeep(triumphant = false)
        if (isInitialized) {
            val message = encouragements[Random.nextInt(encouragements.size)]
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun playBadgeUnlocked(context: Context) {
        // A slightly different, more triumphant tone combination
        PlatformAudio.playBeep(triumphant = true)
        if (isInitialized) {
            val message = badgeUnlockMessages[Random.nextInt(badgeUnlockMessages.size)]
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
