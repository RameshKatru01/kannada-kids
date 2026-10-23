package com.example.platform

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R

class AndroidVideoPlayerController(val player: ExoPlayer) : VideoPlayerController {
    internal val playingState: MutableState<Boolean> = mutableStateOf(false)
    internal val bufferingState: MutableState<Boolean> = mutableStateOf(true)

    override val isPlaying: State<Boolean> get() = playingState
    override val isBuffering: State<Boolean> get() = bufferingState

    override fun play() = player.play()
    override fun pause() = player.pause()
    override fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    override fun setVolume(volume: Float) { player.volume = volume }
    override fun setSpeed(speed: Float) { player.setPlaybackSpeed(speed) }
    override fun release() {
        try {
            player.stop()
            player.release()
        } catch (_: Exception) {
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
actual fun rememberVideoPlayer(url: String, loop: Boolean, autoPlay: Boolean): VideoPlayerController {
    val context = LocalContext.current
    val controller = remember(context, url) {
        val renderersFactory = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        val exo = ExoPlayer.Builder(context, renderersFactory).build()
        exo.setAudioAttributes(audioAttributes, true)
        exo.setMediaItem(MediaItem.fromUri(url))
        if (loop) exo.repeatMode = Player.REPEAT_MODE_ONE
        exo.prepare()
        exo.playWhenReady = autoPlay
        val c = AndroidVideoPlayerController(exo)
        exo.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                c.playingState.value = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                c.bufferingState.value = playbackState == Player.STATE_BUFFERING
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.w("VideoPlayer", "Video playback error: ${error.message}")
                c.bufferingState.value = false
            }
        })
        c
    }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    return controller
}

@OptIn(UnstableApi::class)
@Composable
actual fun VideoSurface(
    controller: VideoPlayerController,
    showNativeControls: Boolean,
    modifier: Modifier
) {
    val player = (controller as AndroidVideoPlayerController).player
    AndroidView(
        factory = { ctx ->
            val layout = if (showNativeControls) R.layout.view_video_player else R.layout.view_letter_trace_video
            val view = android.view.LayoutInflater.from(ctx).inflate(layout, null, false) as PlayerView
            view.player = player
            view.useController = showNativeControls
            view.controllerAutoShow = false
            if (showNativeControls) {
                // Controls stay hidden while the video plays; a tap shows them for 3 seconds.
                view.controllerHideOnTouch = true
                view.controllerShowTimeoutMs = 3000
                view.hideController()
                player.addListener(object : androidx.media3.common.Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) view.hideController()
                    }
                })
            }
            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            if (!showNativeControls) view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            view
        },
        update = { view ->
            if (view.player != player) view.player = player
        },
        modifier = modifier
    )
}

@Composable
actual fun HideSystemBars() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        var ctx: android.content.Context? = context
        while (ctx is android.content.ContextWrapper && ctx !is android.app.Activity) ctx = ctx.baseContext
        val window = (ctx as? android.app.Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
