package com.example.ui.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Cheerful looping background tune for Balloon Pop, made in code (no audio files, works offline).
 * 8 bars in C major (C–G–Am–F–C–G–F–G): marimba melody, music-box arpeggios, soft bass,
 * a light shaker and a gentle kick. [track] makes it at any tempo, so it can speed up with the game.
 */
object BalloonMusic {
    const val SAMPLE_RATE = 22050

    private val cache = mutableMapOf<Int, ShortArray>()

    /** MIDI note number → frequency. */
    private fun hz(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)

    // Chords per bar (root MIDI note in octave 3, then the chord tones above middle C)
    private val chordRoots = listOf(48, 43, 45, 41, 48, 43, 41, 43)            // C G A F C G F G
    private val chordTones = listOf(
        listOf(60, 64, 67), listOf(59, 62, 67), listOf(60, 64, 69), listOf(60, 65, 69),
        listOf(60, 64, 67), listOf(59, 62, 67), listOf(60, 65, 69), listOf(59, 62, 65)
    )

    // Melody in eighth notes per bar (MIDI; 0 = rest, -1 = hold previous)
    private val melody = listOf(
        listOf(76, 79, 81, 79, 76, -1, 72, -1),
        listOf(74, 79, 83, 81, 79, -1, -1, -1),
        listOf(72, 76, 81, 79, 76, -1, 74, -1),
        listOf(72, 74, 77, 76, 74, -1, -1, -1),
        listOf(76, 79, 84, 83, 79, -1, 76, -1),
        listOf(74, 79, 83, 86, 83, -1, 79, -1),
        listOf(81, 84, 81, 77, 76, -1, 74, -1),
        listOf(74, 76, 77, 74, 71, -1, -1, 0)
    )

    /** The whole loop at [bpm] beats per minute (cached). */
    fun track(bpm: Int): ShortArray = cache.getOrPut(bpm) { render(bpm) }

    private fun render(bpm: Int): ShortArray {
        val beat = 60.0 / bpm
        val eighth = beat / 2
        val bar = beat * 4
        val total = (bar * 8 * SAMPLE_RATE).toInt()
        val mix = DoubleArray(total)

        fun add(start: Double, length: Double, gain: Double, voice: (t: Double) -> Double) {
            val s = (start * SAMPLE_RATE).toInt()
            val n = (length * SAMPLE_RATE).toInt()
            for (i in 0 until n) {
                val idx = s + i
                if (idx >= total) break
                val t = i.toDouble() / SAMPLE_RATE
                val release = if (i > n - 300) (n - i) / 300.0 else 1.0
                mix[idx] += voice(t) * gain * release
            }
        }

        fun marimba(f: Double) = { t: Double ->
            val a = (t / 0.004).coerceAtMost(1.0)
            a * (sin(2 * PI * f * t) * exp(-t * 4.5) + 0.3 * sin(2 * PI * f * 4.0 * t) * exp(-t * 18.0))
        }

        fun musicBox(f: Double) = { t: Double ->
            val a = (t / 0.002).coerceAtMost(1.0)
            a * (sin(2 * PI * f * t) + 0.25 * sin(2 * PI * f * 3.0 * t)) * exp(-t * 6.0)
        }

        fun bass(f: Double) = { t: Double ->
            val a = (t / 0.01).coerceAtMost(1.0)
            a * (sin(2 * PI * f * t) + 0.35 * sin(2 * PI * f * 2 * t)) * exp(-t * 3.0)
        }

        val kick = { t: Double ->
            val f = 50.0 + 70.0 * exp(-t * 30.0)
            sin(2 * PI * f * t) * exp(-t * 12.0)
        }

        val rnd = Random(7)
        val shakerNoise = DoubleArray((0.06 * SAMPLE_RATE).toInt()) { rnd.nextDouble(-1.0, 1.0) }
        val shaker = { t: Double ->
            val i = (t * SAMPLE_RATE).toInt().coerceIn(0, shakerNoise.size - 1)
            // crude high-pass: difference of neighbours
            val hp = shakerNoise[i] - shakerNoise[(i - 1).coerceAtLeast(0)]
            hp * exp(-t * 60.0)
        }

        for (b in 0 until 8) {
            val t0 = b * bar
            val root = hz(chordRoots[b])
            val fifth = hz(chordRoots[b] + 7)
            // Bass: root on 1 and 3, fifth on 2 and 4
            for (k in 0 until 4) add(t0 + k * beat, beat * 0.95, 0.55, bass(if (k % 2 == 0) root else fifth))
            // Music-box arpeggio in eighth notes, one octave up
            val tones = chordTones[b]
            val pattern = listOf(0, 1, 2, 1, 0, 1, 2, 1)
            for (k in 0 until 8) add(t0 + k * eighth, eighth * 1.6, 0.16, musicBox(hz(tones[pattern[k]] + 12)))
            // Melody
            val line = melody[b]
            var k = 0
            while (k < 8) {
                val note = line[k]
                if (note > 0) {
                    var len = 1
                    while (k + len < 8 && line[k + len] == -1) len++
                    add(t0 + k * eighth, eighth * len + 0.15, 0.42, marimba(hz(note)))
                    k += len
                } else k++
            }
            // Drums
            add(t0, 0.25, 0.5, kick)
            add(t0 + 2 * beat, 0.25, 0.4, kick)
            for (k2 in 0 until 8) if (k2 % 2 == 1) add(t0 + k2 * eighth, 0.06, 0.10, shaker)
        }

        var peak = 0.0
        for (v in mix) peak = maxOf(peak, kotlin.math.abs(v))
        val scale = if (peak > 0) 32767.0 * 0.42 / peak else 0.0     // quiet enough to sit under sound effects
        return ShortArray(total) { (mix[it] * scale).toInt().toShort() }
    }
}
