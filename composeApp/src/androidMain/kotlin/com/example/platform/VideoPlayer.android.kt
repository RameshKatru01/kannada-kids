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

/** Videos already played are kept on the phone (up to 600 MB), so replays need no data and work offline. */
@OptIn(UnstableApi::class)
internal object VideoCache {
    @Volatile private var cache: androidx.media3.datasource.cache.SimpleCache? = null

    fun get(context: android.content.Context): androidx.media3.datasource.cache.SimpleCache =
        cache ?: synchronized(this) {
            cache ?: androidx.media3.datasource.cache.SimpleCache(
                java.io.File(context.applicationContext.cacheDir, "videos"),
                androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(600L * 1024 * 1024),
                androidx.media3.database.StandaloneDatabaseProvider(context.applicationContext)
            ).also { cache = it }
        }

    fun mediaSourceFactory(context: android.content.Context): androidx.media3.exoplayer.source.MediaSource.Factory {
        val http = androidx.media3.datasource.DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
        val upstream = androidx.media3.datasource.DefaultDataSource.Factory(context, http)
        val cached = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(get(context))
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return androidx.media3.exoplayer.source.DefaultMediaSourceFactory(cached)
    }
}

class AndroidVideoPlayerController(val player: ExoPlayer) : VideoPlayerController {
    internal val playingState: MutableState<Boolean> = mutableStateOf(false)
    internal val bufferingState: MutableState<Boolean> = mutableStateOf(true)
    internal val endedState: MutableState<Boolean> = mutableStateOf(false)

    override val isPlaying: State<Boolean> get() = playingState
    override val isBuffering: State<Boolean> get() = bufferingState
    override val isEnded: State<Boolean> get() = endedState

    override fun play() {
        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
        endedState.value = false
        player.play()
    }
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
        val exo = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(VideoCache.mediaSourceFactory(context))
            .build()
        exo.setAudioAttributes(audioAttributes, true)
        // The video may be served from another host (see VideoHost); GitHub is the fallback
        val playUrl = com.example.ui.data.VideoHost.resolve(url)
        var fellBack = playUrl == url
        exo.setMediaItem(MediaItem.fromUri(playUrl))
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
                c.endedState.value = playbackState == Player.STATE_ENDED
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.w("VideoPlayer", "Video playback error: ${error.message}")
                if (!fellBack) {
                    // The moved copy failed: play the original GitHub link instead
                    fellBack = true
                    val wasPlaying = exo.playWhenReady
                    exo.setMediaItem(MediaItem.fromUri(url))
                    exo.prepare()
                    exo.playWhenReady = wasPlaying
                    return
                }
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
    modifier: Modifier,
    whiteBackground: Boolean
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
            if (whiteBackground) {
                view.setBackgroundColor(android.graphics.Color.WHITE)
                view.setShutterBackgroundColor(android.graphics.Color.WHITE)
            }
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

actual fun preloadVideo(url: String) {
    // Not needed on Android yet (ExoPlayer buffers when a video opens)
}
