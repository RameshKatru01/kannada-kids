package com.example.ui.audio

import com.example.platform.*
import com.example.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SoundEffectsHelper {
    private const val SAMPLE_RATE = 22050

    private val popSoundBuffer: ShortArray by lazy { generatePopSound() }
    private val clickSoundBuffer: ShortArray by lazy { generateClickSound() }
    private val successChimeBuffer: ShortArray by lazy { generateSuccessChime() }
    private val fanfareBuffer: ShortArray by lazy { generateFanfare() }
    private val oopsBuffer: ShortArray by lazy { generateOopsSound() }

    fun playButtonTap(context: Context? = null) {
        playPcm(popSoundBuffer)
    }

    fun playPop(context: Context? = null) {
        playPcm(popSoundBuffer)
    }

    fun playClick(context: Context? = null) {
        playPcm(clickSoundBuffer)
    }

    fun playSuccess(context: Context? = null) {
        playPcm(successChimeBuffer)
    }

    fun playOops(context: Context? = null) {
        playPcm(oopsBuffer)
    }

    fun playFanfare(context: Context? = null) {
        playPcm(fanfareBuffer)
    }

    fun playGameCompletion(context: Context? = null) {
        playPcm(fanfareBuffer)
        if (context != null) {
            playApplause()
            try {
                com.example.ui.data.AudioFeedbackManager.playLevelComplete(context)
            } catch (e: Exception) {
                // Ignore audio feedback manager errors
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var applauseBytes: ByteArray? = null

    /** Plays the bundled applause clip (composeResources/files/applause.wav). */
    @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
    fun playApplause() {
        scope.launch {
            try {
                val bytes = applauseBytes ?: Res.readBytes("files/applause.wav").also { applauseBytes = it }
                withContext(Dispatchers.Main) {
                    PlatformAudio.playBytes(bytes, "wav")
                }
            } catch (e: Exception) {
                Log.w("SoundEffectsHelper", "Error playing sound effect", e)
            }
        }
    }

    private fun playPcm(buffer: ShortArray) {
        try {
            PlatformAudio.playPcm16(buffer, SAMPLE_RATE)
        } catch (e: Exception) {
            Log.w("SoundEffectsHelper", "Error in playPcm", e)
        }
    }

    private fun generatePopSound(): ShortArray {
        val duration = 0.085
        val nSamples = (SAMPLE_RATE * duration).toInt()
        val buffer = ShortArray(nSamples)
        var phase = 0.0
        for (i in 0 until nSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val f = 400.0 + 550.0 * (t / duration)
            phase += 2.0 * PI * f / SAMPLE_RATE
            val env = exp(-t * 35.0)
            buffer[i] = (sin(phase) * env * 28000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateClickSound(): ShortArray {
        val duration = 0.05
        val nSamples = (SAMPLE_RATE * duration).toInt()
        val buffer = ShortArray(nSamples)
        for (i in 0 until nSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 70.0)
            val sample = sin(2.0 * PI * 900.0 * t) * env * 22000.0
            buffer[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateSuccessChime(): ShortArray {
        val duration = 0.42
        val nSamples = (SAMPLE_RATE * duration).toInt()
        val buffer = ShortArray(nSamples)
        val note1Duration = 0.14
        for (i in 0 until nSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            if (t < note1Duration) {
                val env = exp(-t * 12.0)
                val sample = (sin(2.0 * PI * 523.25 * t) + 0.3 * sin(2.0 * PI * 1046.50 * t)) * env * 24000.0
                buffer[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
            } else {
                val t2 = t - note1Duration
                val env = exp(-t2 * 8.0)
                val sample = (sin(2.0 * PI * 783.99 * t2) + 0.35 * sin(2.0 * PI * 1567.98 * t2)) * env * 26000.0
                buffer[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
            }
        }
        return buffer
    }

    private fun generateFanfare(): ShortArray {
        val duration = 1.1
        val nSamples = (SAMPLE_RATE * duration).toInt()
        val buffer = ShortArray(nSamples)
        val noteDuration = 0.18
        val notes = listOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6

        for (i in 0 until nSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIndex = (t / noteDuration).toInt().coerceAtMost(notes.size - 1)
            val noteFreq = notes[noteIndex]
            val tInNote = if (noteIndex < 3) (t - noteIndex * noteDuration) else (t - 3 * noteDuration)
            val decayRate = if (noteIndex == 3) 5.0 else 10.0
            val env = exp(-tInNote * decayRate)
            val sample = (sin(2.0 * PI * noteFreq * tInNote) + 0.3 * sin(2.0 * PI * noteFreq * 2 * tInNote)) * env * 26000.0
            buffer[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateOopsSound(): ShortArray {
        val duration = 0.18
        val nSamples = (SAMPLE_RATE * duration).toInt()
        val buffer = ShortArray(nSamples)
        var phase = 0.0
        for (i in 0 until nSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val f = 320.0 - 140.0 * (t / duration)
            phase += 2.0 * PI * f / SAMPLE_RATE
            val env = exp(-t * 12.0)
            buffer[i] = (sin(phase) * env * 20000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }
}
