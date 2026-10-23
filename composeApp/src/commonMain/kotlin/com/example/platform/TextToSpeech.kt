package com.example.platform

/**
 * Cross-platform replacement for android.speech.tts.TextToSpeech with the same calling style:
 *
 *   val tts = TextToSpeech(context) { status -> if (status == TextToSpeech.SUCCESS) ... }
 *   tts.language = Locale("kn", "IN")
 *   tts.speak("ಅ", TextToSpeech.QUEUE_FLUSH, null, null)
 *
 * Android uses the system TextToSpeech engine; iOS uses AVSpeechSynthesizer and falls back to
 * the Kannada clips built into the app when the device has no Kannada voice installed.
 */
class TextToSpeech(context: Context, listener: ((Int) -> Unit)? = null) {

    private val engine: PlatformTtsEngine = createTtsEngine(context) { ok ->
        listener?.invoke(if (ok) SUCCESS else ERROR)
    }

    var language: Locale? = Locale.US
        set(value) {
            field = value
            engine.setLanguage(value?.toLanguageTag() ?: "en-US")
        }

    fun setLanguage(locale: Locale?): Int {
        language = locale
        return LANG_AVAILABLE
    }

    fun speak(text: CharSequence?, queueMode: Int, params: Any?, utteranceId: String?): Int {
        if (text.isNullOrEmpty()) return ERROR
        engine.speak(text.toString(), queueMode == QUEUE_FLUSH)
        return SUCCESS
    }

    fun setSpeechRate(speechRate: Float): Int {
        engine.setSpeechRate(speechRate)
        return SUCCESS
    }

    fun setPitch(pitch: Float): Int {
        engine.setPitch(pitch)
        return SUCCESS
    }

    val isSpeaking: Boolean get() = engine.isSpeaking()

    fun stop(): Int {
        engine.stop()
        return SUCCESS
    }

    fun shutdown() {
        engine.shutdown()
    }

    companion object {
        const val SUCCESS = 0
        const val ERROR = -1
        const val QUEUE_FLUSH = 0
        const val QUEUE_ADD = 1
        const val LANG_AVAILABLE = 0
    }
}

interface PlatformTtsEngine {
    /** languageTag like "kn-IN" or "en-US" */
    fun setLanguage(languageTag: String)
    fun speak(text: String, flush: Boolean)
    fun setSpeechRate(rate: Float)
    fun setPitch(pitch: Float)
    fun isSpeaking(): Boolean
    fun stop()
    fun shutdown()
}

/**
 * Creates the platform speech engine. [onInit] must be called asynchronously (never from inside
 * this function) so callers can store the TextToSpeech instance before the callback runs.
 */
expect fun createTtsEngine(context: Context, onInit: (Boolean) -> Unit): PlatformTtsEngine
