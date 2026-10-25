package com.example.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.QuartzCore.CATransaction
import platform.UIKit.UIBlurEffect
import platform.UIKit.UIBlurEffectStyle
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.UIKit.UIVisualEffectView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import platform.AVFoundation.*
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.darwin.NSObjectProtocol

class IosVideoPlayerController(
    val player: AVPlayer,
    private val loop: Boolean,
    private var fallbackUrl: String? = null,
    /** Saved for replays, but only once the stream has started (so the two downloads don't compete). */
    private var saveWhenReady: String? = null
) : VideoPlayerController {
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
        val pending = saveWhenReady
        if (pending != null && player.currentItem?.status == AVPlayerItemStatusReadyToPlay &&
            player.currentItem?.playbackLikelyToKeepUp == true
        ) {
            saveWhenReady = null
            IosVideoCache.save(pending)
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
        // Download on a background queue, then save the file
        platform.darwin.dispatch_async(platform.darwin.dispatch_get_global_queue(platform.darwin.DISPATCH_QUEUE_PRIORITY_BACKGROUND.toLong(), 0u)) {
            val data = NSData.create(contentsOfURL = remote)
            if (data != null && data.length > 10_000uL) {
                data.writeToFile(f, atomically = true)
            }
            downloading.remove(url)
        }
    }
}

/** Saves the video in the background so it opens instantly later (see [IosVideoCache]). */
actual fun preloadVideo(url: String) {
    val playUrl = com.example.ui.data.VideoHost.resolve(url)
    if (IosVideoCache.cached(playUrl) == null) IosVideoCache.save(playUrl)
}

@Composable
actual fun rememberVideoPlayer(url: String, loop: Boolean, autoPlay: Boolean): VideoPlayerController {
    val controller = remember(url) {
        // The video may be served from another host (see VideoHost); GitHub is the fallback
        val playUrl = com.example.ui.data.VideoHost.resolve(url)
        val local = IosVideoCache.cached(playUrl)
        val nsUrl = local ?: NSURL.URLWithString(playUrl)
        val player = if (nsUrl != null) AVPlayer(AVPlayerItem(nsUrl)) else AVPlayer()
        // These are short clips: start as soon as there is data instead of waiting to buffer a lot
        player.automaticallyWaitsToMinimizeStalling = false
        IosVideoPlayerController(
            player, loop,
            fallbackUrl = url.takeIf { it != playUrl && local == null },
            saveWhenReady = playUrl.takeIf { local == null }
        ).also {
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

/**
 * A view whose AVPlayerLayer is re-fitted to the view on every layout pass.
 *
 * On phone-shaped screens the video fills the whole area (only a thin edge is cropped). On wider
 * screens such as an iPad, filling would cut off the letter at the top and the words at the bottom,
 * so the whole video is shown instead, over a blurred, zoomed copy of itself rather than black bars.
 */
@OptIn(ExperimentalForeignApi::class)
@ExportObjCClass
private class PlayerLayerView(player: AVPlayer) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val playerLayer: AVPlayerLayer = AVPlayerLayer.playerLayerWithPlayer(player).also {
        it.videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    /** Same player, zoomed to fill, shown blurred behind the video when it does not fill the view. */
    val backdropLayer: AVPlayerLayer = AVPlayerLayer.playerLayerWithPlayer(player).also {
        it.videoGravity = AVLayerVideoGravityResizeAspectFill
    }
    private val blurView = UIVisualEffectView(effect = UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleDark))

    init {
        backgroundColor = UIColor.blackColor
        clipsToBounds = true
        layer.addSublayer(backdropLayer)
        addSubview(blurView)
        layer.addSublayer(playerLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        val (w, h) = bounds.useContents { size.width to size.height }
        // Video shape: from the loaded clip if known, otherwise the 9:16 portrait used by the letter videos
        val videoAspect = playerLayer.player?.currentItem?.presentationSize
            ?.useContents { if (width > 0.0 && height > 0.0) width / height else null } ?: (9.0 / 16.0)
        val viewAspect = if (h > 0.0) w / h else videoAspect
        // Share of the picture that aspect-fill would cut away
        val cropped = 1.0 - minOf(viewAspect, videoAspect) / maxOf(viewAspect, videoAspect)
        val fill = cropped <= 0.2

        // Keep the video exactly fitted to the view, with no implicit animation
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        playerLayer.frame = bounds
        playerLayer.videoGravity = if (fill) AVLayerVideoGravityResizeAspectFill else AVLayerVideoGravityResizeAspect
        backdropLayer.frame = bounds
        backdropLayer.hidden = fill
        blurView.setFrame(bounds)
        blurView.hidden = fill
        CATransaction.commit()
    }
}

/**
 * Draws the video with a plain AVPlayerLayer instead of AVPlayerViewController. The view controller
 * got out of step with the swipe pager and left the picture shifted, with black gaps.
 * Tapping toggles play/pause when [showNativeControls] is on.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoSurface(
    controller: VideoPlayerController,
    showNativeControls: Boolean,
    modifier: Modifier
) {
    val ios = controller as IosVideoPlayerController
    val tap = if (showNativeControls) {
        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
            if (ios.playingState.value) ios.pause() else ios.play()
        }
    } else Modifier
    Box(modifier = modifier.then(tap)) {
        UIKitView(
            factory = {
                PlayerLayerView(ios.player)
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (view.playerLayer.player != ios.player) {
                    view.playerLayer.player = ios.player
                    view.backdropLayer.player = ios.player
                    view.setNeedsLayout()
                }
            },
            properties = UIKitInteropProperties(isInteractive = false, isNativeAccessibilityEnabled = false)
        )
    }
}

@Composable
actual fun HideSystemBars() {
    // The iOS status bar stays visible; the video still fills the rest of the screen.
}
