package com.example.ui.audio

import com.example.platform.*
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object NativeAudioPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val httpClient by lazy { createHttpClient() }
    private var lastText: String? = null
    private var lastRequestAt = 0L

    @Suppress("UNUSED_PARAMETER")
    fun playPhonics(context: Context, text: String) {
        val lang = com.example.ui.data.LanguageManager.currentLanguage.value.code
        val tl = if (lang == "hi") "hi" else "kn"
        playOnlineSpeech(text, tl)
    }

    /** Downloads spoken audio for [text] (Google Translate voice) and plays it. */
    fun playOnlineSpeech(text: String, languageCode: String) {
        if (text.isBlank()) return
        // The same letter is often requested twice in a row (phonics + text-to-speech):
        // like the original single ExoPlayer, play it only once.
        val now = currentTimeMillis()
        if (text == lastText && now - lastRequestAt < 1200L) return
        lastText = text
        lastRequestAt = now
        val encodedText = Uri.encode(text)
        val urlString = "https://translate.google.com/translate_tts?ie=UTF-8&q=$encodedText&tl=$languageCode&client=tw-ob"

        scope.launch {
            try {
                val response = httpClient.get(urlString) {
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                }
                if (response.status.isSuccess()) {
                    val bytes = response.readRawBytes()
                    withContext(Dispatchers.Main) {
                        PlatformAudio.playBytes(bytes, "mp3", exclusive = true)
                    }
                }
            } catch (e: Exception) {
                Log.w("NativeAudioPlayer", "Online speech failed", e)
            }
        }
    }

    fun release() {
        // Nothing to release: each clip is played by a short-lived platform player.
    }
}
