package com.example.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import platform.AVFoundation.*
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.NSObjectProtocol

class IosVideoPlayerController(val player: AVPlayer, private val loop: Boolean, private var fallbackUrl: String? = null) : VideoPlayerController {
    internal val playingState: MutableState<Boolean> = mutableStateOf(false)
    internal val bufferingState: MutableState<Boolean> = mutableStateOf(true)
    private var endObserver: NSObjectProtocol? = null

    override val isPlaying: State<Boolean> get() = playingState
    override val isBuffering: State<Boolean> get() = bufferingState

    @OptIn(ExperimentalForeignApi::class)
    fun attachLoopObserver() {
        endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = player.currentItem,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            if (loop) {
                player.seekToTime(CMTimeMake(value = 0, timescale = 1))
                player.play()
                if (speed != 1f) player.rate = speed
            }
        }
    }

    /** Called periodically to mirror AVPlayer's state into Compose state. */
    fun refreshState() {
        // The moved copy failed to load: switch to the original GitHub link once
        val failed = player.currentItem?.status == AVPlayerItemStatusFailed
        val fb = fallbackUrl
        if (failed && fb != null) {
            fallbackUrl = null
            NSURL.URLWithString(fb)?.let { u ->
                endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
                player.replaceCurrentItemWithPlayerItem(AVPlayerItem(u))
                attachLoopObserver()
                if (wantsToPlay) player.play()
            }
        }
        playingState.value = player.rate > 0f
        val item = player.currentItem
        bufferingState.value = item != null && !item.playbackLikelyToKeepUp && player.rate == 0f && wantsToPlay
    }

    internal var wantsToPlay = false

    override fun play() {
        wantsToPlay = true
        player.play()
        if (speed != 1f) player.rate = speed
        refreshState()
    }

    override fun pause() {
        wantsToPlay = false
        player.pause()
        refreshState()
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun seekTo(positionMs: Long) {
        player.seekToTime(CMTimeMakeWithSeconds(positionMs / 1000.0, preferredTimescale = 600))
    }

    private var speed = 1f

    override fun setSpeed(speed: Float) {
        this.speed = speed
        if (wantsToPlay) player.rate = speed
    }

    override fun setVolume(volume: Float) {
        player.volume = volume
    }

    override fun release() {
        player.pause()
        endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
    }
}

/**
 * Videos already played are kept in the app's Caches folder, so replays need no data and work
 * offline. The first play streams; a copy is downloaded alongside it.
 */
@OptIn(ExperimentalForeignApi::class)
private object IosVideoCache {
    private val dir: String? by lazy {
        val caches = platform.Foundation.NSSearchPathForDirectoriesInDomains(
            platform.Foundation.NSCachesDirectory, platform.Foundation.NSUserDomainMask, true
        ).firstOrNull() as? String ?: return@lazy null
        val d = "$caches/videos"
        platform.Foundation.NSFileManager.defaultManager.createDirectoryAtPath(d, true, null, null)
        d
    }
    private val downloading = mutableSetOf<String>()

    private fun fileFor(url: String): String? {
        var h = 0xcbf29ce484222325uL
        for (b in url.encodeToByteArray()) { h = h xor b.toUByte().toULong(); h *= 0x100000001b3uL }
        return dir?.let { "$it/${h.toString(16)}.mp4" }
    }

    /** A local file URL when the video is already saved, else null. */
    fun cached(url: String): NSURL? {
        val f = fileFor(url) ?: return null
        return if (platform.Foundation.NSFileManager.defaultManager.fileExistsAtPath(f)) NSURL.fileURLWithPath(f) else null
    }

    /** Saves a copy of [url] in the background (once). */
    fun save(url: String) {
        val f = fileFor(url) ?: return
        val remote = NSURL.URLWithString(url) ?: return
        if (!downloading.add(url)) return
        platform.Foundation.NSURLSession.sharedSession.downloadTaskWithURL(remote) { tmp, response, error ->
            val ok = error == null && ((response as? platform.Foundation.NSHTTPURLResponse)?.statusCode ?: 0L) in 200L..299L
            if (ok && tmp != null) {
                val fm = platform.Foundation.NSFileManager.defaultManager
                fm.removeItemAtPath(f, null)
                fm.moveItemAtURL(tmp, NSURL.fileURLWithPath(f), null)
            }
            downloading.remove(url)
        }.resume()
    }
}

@Composable
actual fun rememberVideoPlayer(url: String, loop: Boolean, autoPlay: Boolean): VideoPlayerController {
    val controller = remember(url) {
        // The video may be served from another host (see VideoHost); GitHub is the fallback
        val playUrl = com.example.ui.data.VideoHost.resolve(url)
        val local = IosVideoCache.cached(playUrl)
        val nsUrl = local ?: NSURL.URLWithString(playUrl)
        if (local == null) IosVideoCache.save(playUrl)
        val player = if (nsUrl != null) AVPlayer(AVPlayerItem(nsUrl)) else AVPlayer()
        IosVideoPlayerController(player, loop, fallbackUrl = url.takeIf { it != playUrl && local == null }).also {
            it.attachLoopObserver()
            if (autoPlay) it.play()
        }
    }
    LaunchedEffect(controller) {
        while (true) {
            controller.refreshState()
            delay(250)
        }
    }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    return controller
}

@Composable
actual fun VideoSurface(
    controller: VideoPlayerController,
    showNativeControls: Boolean,
    modifier: Modifier
) {
    val player = (controller as IosVideoPlayerController).player
    UIKitViewController(
        factory = {
            AVPlayerViewController().apply {
                this.player = player
                this.showsPlaybackControls = showNativeControls
                // Without native controls the Compose overlay on top of the video handles taps.
                this.view.userInteractionEnabled = showNativeControls
            }
        },
        modifier = modifier,
        update = { vc ->
            if (vc.player != player) vc.player = player
            vc.showsPlaybackControls = showNativeControls
        }
    )
}

@Composable
actual fun HideSystemBars() {
    // The iOS status bar stays visible; the video still fills the rest of the screen.
}
