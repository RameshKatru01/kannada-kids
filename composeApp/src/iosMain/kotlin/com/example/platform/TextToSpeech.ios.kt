package com.example.platform

import com.example.ui.audio.SpeechBank
import platform.AVFAudio.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual fun createTtsEngine(context: Context, onInit: (Boolean) -> Unit): PlatformTtsEngine {
    val engine = IosTtsEngine()
    // Deliver the "ready" callback asynchronously, like Android does.
    dispatch_async(dispatch_get_main_queue()) { onInit(true) }
    return engine
}

private class IosTtsEngine : PlatformTtsEngine {
    private val synthesizer = AVSpeechSynthesizer()
    private var languageTag: String = "en-US"
    private var rate: Float = 1.0f
    private var pitch: Float = 1.0f

    override fun setLanguage(languageTag: String) {
        this.languageTag = languageTag
    }

    private fun findVoice(tag: String): AVSpeechSynthesisVoice? =
        AVSpeechSynthesisVoice.voiceWithLanguage(tag)
            ?: AVSpeechSynthesisVoice.voiceWithLanguage(tag.substringBefore("-"))

    override fun speak(text: String, flush: Boolean) {
        val voice = findVoice(languageTag)
        val lang = languageTag.substringBefore("-").lowercase()
        if (voice == null && (lang == "kn" || lang == "hi")) {
            // Most iPhones have no Kannada voice installed: use the clips built into the app.
            SpeechBank.playClip(text)
            return
        }
        if (flush && synthesizer.speaking) {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        }
        val utterance = AVSpeechUtterance(string = text)
        if (voice != null) utterance.voice = voice
        utterance.rate = (AVSpeechUtteranceDefaultSpeechRate * rate).coerceIn(0.1f, 1.0f)
        utterance.pitchMultiplier = pitch.coerceIn(0.5f, 2.0f)
        synthesizer.speakUtterance(utterance)
    }

    override fun setSpeechRate(rate: Float) { this.rate = rate }
    override fun setPitch(pitch: Float) { this.pitch = pitch }
    override fun isSpeaking(): Boolean = synthesizer.speaking

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    override fun shutdown() {
        stop()
    }
}
