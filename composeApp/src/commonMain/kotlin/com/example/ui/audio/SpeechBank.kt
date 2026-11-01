package com.example.ui.audio

import com.example.platform.*
import com.example.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Letter and word voice, from audio clips built into the app (licensed voice, generated once by the
 * "Generate voice" GitHub Action, see tools/generate_voice.py). Works offline.
 *
 * Each clip is files/voice/v_<hash>.mp3, where <hash> is the 64-bit FNV-1a hash of the text. Text with
 * no clip falls back to the phone's own text-to-speech voice (and is logged, so it can be added).
 */
object SpeechBank {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cache = HashMap<String, ByteArray>()
    private val missing = HashSet<String>()
    private var fallback: TextToSpeech? = null

    /** 64-bit FNV-1a of the UTF-8 bytes, as 16 hex digits (same as tools/generate_voice.py). */
    fun key(text: String): String {
        var h = 0xcbf29ce484222325uL
        for (b in text.trim().encodeToByteArray()) {
            h = h xor (b.toUByte().toULong())
            h *= 0x100000001b3uL
        }
        return h.toString(16).padStart(16, '0')
    }

    fun path(text: String) = "files/voice/v_${key(text)}.mp3"

    private suspend fun clip(text: String): ByteArray? {
        cache[text]?.let { return it }
        if (text in missing) return null
        return try {
            Res.readBytes(path(text)).also { cache[text] = it }
        } catch (e: Exception) {
            missing += text
            null
        }
    }

    /** Plays [text]: the built-in clip, or the phone's voice if there is none. */
    fun play(context: Context, text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        scope.launch {
            val bytes = clip(t)
            withContext(Dispatchers.Main) {
                if (bytes != null) {
                    PlatformAudio.playBytes(bytes, "mp3", exclusive = true)
                } else {
                    Log.w("SpeechBank", "No voice clip for \"$t\" (${key(t)}); using the phone's voice")
                    speakWithDevice(context, t)
                }
            }
        }
    }

    /** Plays the built-in clip for [text] only (no fallback). Used by the iPhone voice when it has no Kannada voice. */
    fun playClip(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        scope.launch {
            val bytes = clip(t) ?: return@launch
            withContext(Dispatchers.Main) { PlatformAudio.playBytes(bytes, "mp3", exclusive = true) }
        }
    }

    private fun speakWithDevice(context: Context, text: String) {
        val tts = fallback ?: TextToSpeech(context) { }.also { fallback = it }
        // Hindi (Devanagari) text gets the Hindi device voice, everything else Kannada
        val lang = if (text.any { it in '\u0900'..'\u097F' }) "hi" else "kn"
        tts.language = Locale.Builder().setLanguage(lang).setRegion("IN").build()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
}
