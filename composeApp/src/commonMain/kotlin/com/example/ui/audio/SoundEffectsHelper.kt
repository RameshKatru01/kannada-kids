package com.example.ui.audio

import com.example.platform.*
import com.example.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * All game sound effects, synthesised in code (no audio files needed, works offline on Android and iOS).
 *
 * Kid-friendly design:
 *  - Right answer: bright xylophone "ding-ding!" that rises up the scale (3 variations, picked at random).
 *  - Wrong answer: never a buzzer or a sad falling tone; a soft playful "boing" or a gentle
 *    rising "hmm-mm?" that sounds like "try again!".
 *  - 3 right answers in a row: an extra sparkly glockenspiel jingle.
 *  - Level complete: a short happy celebration tune (plus applause).
 */
object SoundEffectsHelper {
    private const val SAMPLE_RATE = 22050

    // ---- Note frequencies (Hz) ----
    private const val E5 = 659.25
    private const val G5 = 783.99
    private const val A5 = 880.00
    private const val C6 = 1046.50
    private const val D6 = 1174.66
    private const val E6 = 1318.51
    private const val G6 = 1567.98
    private const val A6 = 1760.00
    private const val C7 = 2093.00
    private const val E7 = 2637.02
    private const val G7 = 3135.96
    private const val C8 = 4186.01

    private var streak = 0

    // ---- Pre-built sounds (built once, on first use) ----
    private val popSoundBuffer: ShortArray by lazy { generatePopSound() }
    private val clickSoundBuffer: ShortArray by lazy { generateClickSound() }

    private val successVariants: List<ShortArray> by lazy {
        listOf(
            // ding-ding-DING (C E G)
            xylophone(listOf(Note(0.00, C6, 0.25), Note(0.08, E6, 0.25), Note(0.16, G6, 0.45))),
            // quick climb G C E G
            xylophone(listOf(Note(0.00, G5, 0.2), Note(0.06, C6, 0.2), Note(0.12, E6, 0.2), Note(0.18, G6, 0.45))),
            // sparkly E G C(high)
            xylophone(listOf(Note(0.00, E6, 0.25), Note(0.09, G6, 0.25), Note(0.18, C7, 0.5)))
        )
    }

    private val sparkleBuffer: ShortArray by lazy { glockenspiel(listOf(
        Note(0.00, C7, 0.3), Note(0.05, E7, 0.3), Note(0.10, G7, 0.3), Note(0.15, C8, 0.6),
        Note(0.30, G7, 0.4), Note(0.36, C8, 0.7)
    )) }

    /** Success + sparkle, used on every 3rd right answer in a row. */
    private val streakVariants: List<ShortArray> by lazy {
        successVariants.map { overlay(it, sparkleBuffer, offsetSeconds = 0.32) }
    }

    private val tryAgainVariants: List<ShortArray> by lazy {
        listOf(generateBoing(), generateHmmQuestion())
    }

    private val celebrationBuffer: ShortArray by lazy { generateCelebration() }

    // ---- Public API (used by all games) ----

    fun playButtonTap(context: Context? = null) {
        playPcm(popSoundBuffer)
    }

    fun playPop(context: Context? = null) {
        playPcm(popSoundBuffer)
    }

    fun playClick(context: Context? = null) {
        playPcm(clickSoundBuffer)
    }

    /** Right answer. Every 3rd right answer in a row also gets a sparkly jingle. */
    fun playSuccess(context: Context? = null) {
        streak += 1
        val variant = Random.nextInt(successVariants.size)
        playPcm(if (streak % 3 == 0) streakVariants[variant] else successVariants[variant])
    }

    /** Wrong answer: a gentle, playful "try again" sound (never a buzzer). */
    fun playOops(context: Context? = null) {
        streak = 0
        playPcm(tryAgainVariants[Random.nextInt(tryAgainVariants.size)])
    }

    /** Short sparkle (badges, stickers). */
    fun playSparkle(context: Context? = null) {
        playPcm(sparkleBuffer)
    }

    /** Happy celebration tune. */
    fun playFanfare(context: Context? = null) {
        playPcm(celebrationBuffer)
    }

    /** Level / game complete: celebration tune + applause (+ spoken encouragement). */
    fun playGameCompletion(context: Context? = null) {
        streak = 0
        playPcm(celebrationBuffer)
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

    // ---- Synthesis helpers ----

    private data class Note(val start: Double, val freq: Double, val length: Double, val gain: Double = 1.0)

    /** Wooden xylophone / marimba-like tone: strong fundamental, quick bright overtone. */
    private fun xylophone(notes: List<Note>, volume: Double = 0.75): ShortArray =
        render(notes, volume) { t, f ->
            val attack = (t / 0.004).coerceAtMost(1.0)
            attack * (sin(2 * PI * f * t) * exp(-t * 7.0) +
                0.35 * sin(2 * PI * f * 3.9 * t) * exp(-t * 28.0) +
                0.12 * sin(2 * PI * f * 9.2 * t) * exp(-t * 60.0))
        }

    /** Glockenspiel / bell tone for sparkles. */
    private fun glockenspiel(notes: List<Note>, volume: Double = 0.45): ShortArray =
        render(notes, volume) { t, f ->
            val attack = (t / 0.002).coerceAtMost(1.0)
            attack * (sin(2 * PI * f * t) * exp(-t * 5.0) +
                0.4 * sin(2 * PI * f * 2.76 * t) * exp(-t * 12.0) +
                0.2 * sin(2 * PI * f * 5.4 * t) * exp(-t * 25.0))
        }

    /** Mixes notes into one buffer and normalises it so it never clips. */
    private fun render(notes: List<Note>, volume: Double, voice: (Double, Double) -> Double): ShortArray {
        val total = notes.maxOf { it.start + it.length } + 0.02
        val n = (total * SAMPLE_RATE).toInt()
        val mix = DoubleArray(n)
        for (note in notes) {
            val startIndex = (note.start * SAMPLE_RATE).toInt()
            val len = (note.length * SAMPLE_RATE).toInt()
            for (i in 0 until len) {
                val idx = startIndex + i
                if (idx >= n) break
                val t = i.toDouble() / SAMPLE_RATE
                val release = if (i > len - 220) (len - i) / 220.0 else 1.0
                mix[idx] += voice(t, note.freq) * note.gain * release
            }
        }
        return normalise(mix, volume)
    }

    private fun normalise(mix: DoubleArray, volume: Double): ShortArray {
        var peak = 0.0
        for (v in mix) peak = maxOf(peak, abs(v))
        val scale = if (peak > 0) 32000.0 * volume / peak else 0.0
        return ShortArray(mix.size) { (mix[it] * scale).toInt().coerceIn(-32767, 32767).toShort() }
    }

    /** Plays [top] over [base] starting [offsetSeconds] in. */
    private fun overlay(base: ShortArray, top: ShortArray, offsetSeconds: Double): ShortArray {
        val offset = (offsetSeconds * SAMPLE_RATE).toInt()
        val n = maxOf(base.size, offset + top.size)
        val mix = DoubleArray(n)
        for (i in base.indices) mix[i] += base[i].toDouble() / 32000.0
        for (i in top.indices) mix[offset + i] += top[i].toDouble() / 32000.0
        return normalise(mix, 0.8)
    }

    // ---- The individual sounds ----

    /** Soft cartoon "boing": pitch springs up, wobbles and settles. */
    private fun generateBoing(): ShortArray {
        val duration = 0.42
        val n = (SAMPLE_RATE * duration).toInt()
        val mix = DoubleArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val spring = 1.0 - exp(-t * 18.0)                       // jump up quickly
            val wobble = 1.0 + 0.06 * sin(2 * PI * 14.0 * t) * exp(-t * 4.0)
            val f = (260.0 + 260.0 * spring) * wobble                // ~260 Hz -> ~520 Hz, wobbling
            phase += 2 * PI * f / SAMPLE_RATE
            val env = (t / 0.01).coerceAtMost(1.0) * exp(-t * 6.0)
            mix[i] = (sin(phase) + 0.25 * sin(2 * phase)) * env
        }
        return normalise(mix, 0.45)
    }

    /** Gentle rising two-note "hmm-mm?" — sounds like "shall we try again?". */
    private fun generateHmmQuestion(): ShortArray =
        render(
            listOf(Note(0.00, E5, 0.22, gain = 0.9), Note(0.16, A5, 0.35)),
            volume = 0.5
        ) { t, f ->
            // Soft, round tone (sine with a little warmth), no harsh overtones
            val attack = (t / 0.015).coerceAtMost(1.0)
            attack * (sin(2 * PI * f * t) + 0.2 * sin(2 * PI * f * 2 * t)) * exp(-t * 5.0)
        }

    /** Short happy celebration tune: climb up, little bounce, big happy chord. */
    private fun generateCelebration(): ShortArray {
        val melody = listOf(
            Note(0.00, C6, 0.20), Note(0.12, E6, 0.20), Note(0.24, G6, 0.20), Note(0.36, C7, 0.30),
            Note(0.54, A6, 0.18), Note(0.66, C7, 0.30),
            // final chord
            Note(0.86, C6, 0.9, gain = 0.6), Note(0.86, E6, 0.9, gain = 0.6),
            Note(0.86, G6, 0.9, gain = 0.6), Note(0.86, C7, 1.0, gain = 0.8)
        )
        val tune = xylophone(melody, volume = 0.8)
        val sparkle = glockenspiel(listOf(Note(0.0, C8, 0.6), Note(0.08, G7, 0.6), Note(0.16, C8, 0.8)), volume = 0.35)
        return overlay(tune, sparkle, offsetSeconds = 0.88)
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
}
