package com.example.platform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.ToneGenerator
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.Executors

actual object PlatformAudio {
    private val executor = Executors.newFixedThreadPool(2)
    private var clipCounter = 0

    actual fun playPcm16(samples: ShortArray, sampleRate: Int) {
        executor.execute {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.play()
                val durationMs = (samples.size * 1000L / sampleRate) + 80L
                Thread.sleep(durationMs)
                try {
                    track.stop()
                    track.release()
                } catch (e: Exception) {
                    // ignore
                }
            } catch (e: Exception) {
                android.util.Log.w("PlatformAudio", "Error in playPcm16", e)
            }
        }
    }

    private var exclusivePlayer: MediaPlayer? = null

    actual fun playBytes(bytes: ByteArray, fileExtension: String, exclusive: Boolean) {
        executor.execute {
            try {
                if (exclusive) {
                    synchronized(this) {
                        exclusivePlayer?.let { previous ->
                            try { previous.stop() } catch (_: Exception) {}
                            try { previous.release() } catch (_: Exception) {}
                        }
                        exclusivePlayer = null
                    }
                }
                val ctx = AppContextHolder.context
                clipCounter = (clipCounter + 1) % 4
                val file = File(ctx.cacheDir, "clip_$clipCounter.$fileExtension")
                file.writeBytes(bytes)
                val player = MediaPlayer()
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                player.setDataSource(file.absolutePath)
                player.setOnCompletionListener { mp ->
                    synchronized(this) { if (exclusivePlayer === mp) exclusivePlayer = null }
                    try { mp.release() } catch (_: Exception) {}
                }
                player.setOnErrorListener { mp, _, _ ->
                    synchronized(this) { if (exclusivePlayer === mp) exclusivePlayer = null }
                    try { mp.release() } catch (_: Exception) {}
                    true
                }
                player.setOnPreparedListener { mp -> mp.start() }
                if (exclusive) synchronized(this) { exclusivePlayer = player }
                player.prepareAsync()
            } catch (e: Exception) {
                android.util.Log.w("PlatformAudio", "Error playing clip", e)
            }
        }
    }

    actual fun playBeep(triumphant: Boolean) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGen.startTone(
                if (triumphant) ToneGenerator.TONE_CDMA_ABBR_ALERT else ToneGenerator.TONE_PROP_PROMPT,
                150
            )
            Thread {
                Thread.sleep(200)
                toneGen.release()
            }.start()
        } catch (e: Exception) {
            // Ignore if tone generator fails
        }
    }

    actual fun startLoop(samples: ShortArray, sampleRate: Int): Any? {
        return try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.setLoopPoints(0, samples.size, -1)
            track.play()
            track
        } catch (e: Exception) {
            android.util.Log.w("PlatformAudio", "Error starting loop", e)
            null
        }
    }

    actual fun stopLoop(handle: Any?) {
        val track = handle as? AudioTrack ?: return
        try {
            track.stop()
            track.release()
        } catch (e: Exception) {
            // ignore
        }
    }
}

actual object SystemVolume {
    private val audioManager: AudioManager
        get() = AppContextHolder.context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager

    actual fun getMaxVolume(): Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    actual fun getVolume(): Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    actual fun setVolume(volume: Int) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
    }
}

actual fun createHttpClient(): HttpClient = HttpClient(OkHttp)

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun formatDate(millis: Long, pattern: String): String =
    SimpleDateFormat(pattern, java.util.Locale.US).format(Date(millis))

actual fun addToDate(millis: Long, years: Int, months: Int, days: Int): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    if (years != 0) cal.add(Calendar.YEAR, years)
    if (months != 0) cal.add(Calendar.MONTH, months)
    if (days != 0) cal.add(Calendar.DAY_OF_YEAR, days)
    return cal.timeInMillis
}

actual val platformName: String = "Android"

actual fun supportDeviceInfo(): String {
    val ctx = AppContextHolder.context
    val app = try {
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        val code = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        "${info.versionName} ($code)"
    } catch (e: Exception) {
        "?"
    }
    return "App $app · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · Android ${android.os.Build.VERSION.RELEASE}"
}

actual fun formatMoney(micros: Long, currencyCode: String): String {
    val amount = micros / 1_000_000.0
    return try {
        val f = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.getDefault())
        f.currency = java.util.Currency.getInstance(currencyCode)
        val digits = if (amount >= 20) 0 else 2
        f.minimumFractionDigits = digits
        f.maximumFractionDigits = digits
        f.format(amount)
    } catch (e: Exception) {
        "$currencyCode " + (if (amount >= 20) amount.toLong().toString() else ((amount * 100).toLong() / 100.0).toString())
    }
}

@androidx.compose.runtime.Composable
actual fun KeepScreenOn() {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
