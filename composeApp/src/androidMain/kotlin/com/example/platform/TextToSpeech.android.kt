package com.example.platform

import android.os.Handler
import android.os.Looper

actual fun createTtsEngine(context: Context, onInit: (Boolean) -> Unit): PlatformTtsEngine =
    AndroidTtsEngine(context, onInit)

private class AndroidTtsEngine(context: Context, onInit: (Boolean) -> Unit) : PlatformTtsEngine {
    private var ready = false
    private var pendingLanguage: String? = null

    private val tts: android.speech.tts.TextToSpeech =
        android.speech.tts.TextToSpeech(context.applicationContext) { status ->
            ready = status == android.speech.tts.TextToSpeech.SUCCESS
            pendingLanguage?.let { applyLanguage(it) }
            // Always deliver on the main thread, after the constructor has returned.
            Handler(Looper.getMainLooper()).post { onInit(ready) }
        }

    private fun applyLanguage(tag: String) {
        try {
            tts.language = java.util.Locale.forLanguageTag(tag)
        } catch (e: Exception) {
            android.util.Log.w("TextToSpeech", "Could not set language $tag", e)
        }
    }

    override fun setLanguage(languageTag: String) {
        pendingLanguage = languageTag
        if (ready) applyLanguage(languageTag)
    }

    override fun speak(text: String, flush: Boolean) {
        val mode = if (flush) android.speech.tts.TextToSpeech.QUEUE_FLUSH else android.speech.tts.TextToSpeech.QUEUE_ADD
        tts.speak(text, mode, null, null)
    }

    override fun setSpeechRate(rate: Float) { tts.setSpeechRate(rate) }
    override fun setPitch(pitch: Float) { tts.setPitch(pitch) }
    override fun isSpeaking(): Boolean = tts.isSpeaking
    override fun stop() { tts.stop() }
    override fun shutdown() { tts.shutdown() }
}
