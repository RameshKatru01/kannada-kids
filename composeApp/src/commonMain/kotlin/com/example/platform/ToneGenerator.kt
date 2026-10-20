package com.example.platform

import kotlin.math.PI
import kotlin.math.sin

/** Minimal replacement for android.media.AudioManager constants used by the games. */
object AudioManager {
    const val STREAM_MUSIC = 3
}

/**
 * Cross-platform replacement for android.media.ToneGenerator: synthesises simple beeps and
 * plays them through [PlatformAudio].
 */
class ToneGenerator(@Suppress("UNUSED_PARAMETER") streamType: Int, volume: Int) {
    private val amplitude = 18000.0 * (volume.coerceIn(0, 100) / 100.0)

    fun startTone(toneType: Int, durationMs: Int): Boolean {
        val freqs = when (toneType) {
            TONE_PROP_BEEP -> listOf(880.0)
            TONE_PROP_BEEP2 -> listOf(880.0, 1320.0)
            TONE_PROP_ACK -> listOf(1200.0, 1200.0)
            TONE_PROP_NACK -> listOf(400.0, 400.0)
            TONE_PROP_PROMPT -> listOf(660.0)
            TONE_SUP_ERROR -> listOf(1800.0, 1400.0, 950.0)
            TONE_CDMA_ABBR_ALERT -> listOf(1150.0, 950.0)
            else -> listOf(880.0)
        }
        val sampleRate = 22050
        val total = (sampleRate * durationMs.coerceAtLeast(20) / 1000)
        val perTone = (total / freqs.size).coerceAtLeast(1)
        val samples = ShortArray(perTone * freqs.size)
        var i = 0
        for (f in freqs) {
            for (n in 0 until perTone) {
                val t = n.toDouble() / sampleRate
                val fade = if (n > perTone - 150) (perTone - n) / 150.0 else 1.0
                samples[i++] = (sin(2.0 * PI * f * t) * amplitude * fade).toInt().coerceIn(-32767, 32767).toShort()
            }
        }
        PlatformAudio.playPcm16(samples, sampleRate)
        return true
    }

    fun stopTone() {}

    fun release() {}

    companion object {
        const val TONE_PROP_BEEP = 24
        const val TONE_PROP_ACK = 25
        const val TONE_PROP_NACK = 26
        const val TONE_PROP_PROMPT = 27
        const val TONE_PROP_BEEP2 = 28
        const val TONE_SUP_ERROR = 17
        const val TONE_CDMA_ABBR_ALERT = 97
    }
}
