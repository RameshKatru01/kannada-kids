package com.example.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.*
import platform.Foundation.*
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

/** Wraps mono 16-bit PCM samples in a WAV container so AVAudioPlayer can play them. */
internal fun pcm16ToWav(samples: ShortArray, sampleRate: Int): ByteArray {
    val dataSize = samples.size * 2
    val out = ByteArray(44 + dataSize)
    fun putInt(offset: Int, value: Int) {
        out[offset] = (value and 0xFF).toByte()
        out[offset + 1] = ((value shr 8) and 0xFF).toByte()
        out[offset + 2] = ((value shr 16) and 0xFF).toByte()
        out[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }
    fun putShort(offset: Int, value: Int) {
        out[offset] = (value and 0xFF).toByte()
        out[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }
    fun putAscii(offset: Int, text: String) {
        text.forEachIndexed { i, c -> out[offset + i] = c.code.toByte() }
    }
    putAscii(0, "RIFF")
    putInt(4, 36 + dataSize)
    putAscii(8, "WAVE")
    putAscii(12, "fmt ")
    putInt(16, 16)          // PCM chunk size
    putShort(20, 1)         // PCM format
    putShort(22, 1)         // mono
    putInt(24, sampleRate)
    putInt(28, sampleRate * 2) // byte rate
    putShort(32, 2)         // block align
    putShort(34, 16)        // bits per sample
    putAscii(36, "data")
    putInt(40, dataSize)
    var o = 44
    for (s in samples) {
        val v = s.toInt()
        out[o] = (v and 0xFF).toByte()
        out[o + 1] = ((v shr 8) and 0xFF).toByte()
        o += 2
    }
    return out
}

actual object PlatformAudio {
    // AVAudioPlayer stops when it is deallocated, so keep players alive until they finish.
    private val activePlayers = mutableListOf<AVAudioPlayer>()

    actual fun playPcm16(samples: ShortArray, sampleRate: Int) {
        playBytes(pcm16ToWav(samples, sampleRate), "wav")
    }

    @OptIn(ExperimentalForeignApi::class)
    private var exclusivePlayer: AVAudioPlayer? = null

    actual fun playBytes(bytes: ByteArray, fileExtension: String, exclusive: Boolean) {
        try {
            if (exclusive) {
                exclusivePlayer?.stop()
                exclusivePlayer = null
            }
            activePlayers.removeAll { !it.playing }
            val player = AVAudioPlayer(data = bytes.toNSData(), error = null)
            player.volume = SystemVolume.appVolumeFraction()
            player.prepareToPlay()
            player.play()
            activePlayers.add(player)
            if (exclusive) exclusivePlayer = player
        } catch (e: Throwable) {
            println("PlatformAudio: could not play clip - ${e.message}")
        }
    }

    actual fun playBeep(triumphant: Boolean) {
        val sampleRate = 22050
        val duration = 0.15
        val n = (sampleRate * duration).toInt()
        val samples = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val f = if (triumphant) (if (t < duration / 2) 880.0 else 1320.0) else 660.0
            val env = exp(-t * 10.0)
            samples[i] = (sin(2.0 * PI * f * t) * env * 20000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm16(samples, sampleRate)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun startLoop(samples: ShortArray, sampleRate: Int): Any? {
        return try {
            val player = AVAudioPlayer(data = pcm16ToWav(samples, sampleRate).toNSData(), error = null)
            player.numberOfLoops = -1
            player.volume = SystemVolume.appVolumeFraction()
            player.prepareToPlay()
            player.play()
            player
        } catch (e: Throwable) {
            null
        }
    }

    actual fun stopLoop(handle: Any?) {
        (handle as? AVAudioPlayer)?.stop()
    }
}

/**
 * iOS apps cannot change the system volume, so the pause-menu slider controls the app's own
 * sound-effect volume instead (0..15, like Android's media stream).
 */
actual object SystemVolume {
    private const val KEY = "kk_app_volume"
    private const val MAX = 15

    actual fun getMaxVolume(): Int = MAX

    actual fun getVolume(): Int {
        val defaults = NSUserDefaults.standardUserDefaults
        return if (defaults.objectForKey(KEY) == null) MAX else defaults.integerForKey(KEY).toInt().coerceIn(0, MAX)
    }

    actual fun setVolume(volume: Int) {
        NSUserDefaults.standardUserDefaults.setInteger(volume.coerceIn(0, MAX).toLong(), forKey = KEY)
    }

    internal fun appVolumeFraction(): Float = getVolume().toFloat() / MAX
}

actual fun createHttpClient(): HttpClient = HttpClient(Darwin)

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun formatDate(millis: Long, pattern: String): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = pattern
    formatter.locale = NSLocale(localeIdentifier = "en_US_POSIX")
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis / 1000.0))
}

actual fun addToDate(millis: Long, years: Int, months: Int, days: Int): Long {
    val components = NSDateComponents()
    components.year = years.toLong()
    components.month = months.toLong()
    components.day = days.toLong()
    val start = NSDate.dateWithTimeIntervalSince1970(millis / 1000.0)
    val result = NSCalendar.currentCalendar.dateByAddingComponents(components, toDate = start, options = 0uL)
        ?: return millis + days * 86_400_000L
    return (result.timeIntervalSince1970 * 1000.0).toLong()
}

actual val platformName: String = "iOS"

actual fun supportDeviceInfo(): String {
    val bundle = platform.Foundation.NSBundle.mainBundle
    val version = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "?"
    val build = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "?"
    val device = platform.UIKit.UIDevice.currentDevice
    return "App $version ($build) · ${device.model} · ${device.systemName} ${device.systemVersion}"
}
