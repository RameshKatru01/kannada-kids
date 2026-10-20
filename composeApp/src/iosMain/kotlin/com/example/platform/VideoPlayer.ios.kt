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

class IosVideoPlayerController(val player: AVPlayer, private val loop: Boolean) : VideoPlayerController {
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
            }
        }
    }

    /** Called periodically to mirror AVPlayer's state into Compose state. */
    fun refreshState() {
        playingState.value = player.rate > 0f
        val item = player.currentItem
        bufferingState.value = item != null && !item.playbackLikelyToKeepUp && player.rate == 0f && wantsToPlay
    }

    internal var wantsToPlay = false

    override fun play() {
        wantsToPlay = true
        player.play()
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

    override fun setVolume(volume: Float) {
        player.volume = volume
    }

    override fun release() {
        player.pause()
        endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
    }
}

@Composable
actual fun rememberVideoPlayer(url: String, loop: Boolean, autoPlay: Boolean): VideoPlayerController {
    val controller = remember(url) {
        val nsUrl = NSURL.URLWithString(url)
        val player = if (nsUrl != null) AVPlayer(AVPlayerItem(nsUrl)) else AVPlayer()
        IosVideoPlayerController(player, loop).also {
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
