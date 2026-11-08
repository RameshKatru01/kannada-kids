package com.example.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier

/**
 * Cross-platform video player (ExoPlayer on Android, AVPlayer on iOS).
 */
interface VideoPlayerController {
    val isPlaying: State<Boolean>
    val isBuffering: State<Boolean>
    /** True when a video that doesn't loop has played to the end (play() then starts it again). */
    val isEnded: State<Boolean> get() = neverEnded
    /** Plays; a video that has ended starts again from the beginning. */
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    /** Playback speed: 1 = normal, 0.5 = half speed. */
    fun setSpeed(speed: Float) {}
    fun release()
}

private val neverEnded: State<Boolean> = androidx.compose.runtime.mutableStateOf(false)

/**
 * Creates and remembers a player for [url]. The player is released automatically when it leaves
 * the composition (or when [url] changes).
 */
@Composable
expect fun rememberVideoPlayer(url: String, loop: Boolean, autoPlay: Boolean): VideoPlayerController

/**
 * Draws the video for [controller]. When [showNativeControls] is true the platform's standard
 * playback controls are shown on top of the video.
 */
@Composable
expect fun VideoSurface(
    controller: VideoPlayerController,
    showNativeControls: Boolean,
    modifier: Modifier = Modifier,
    /** Fill any space the video doesn't cover with white (for white-background videos, e.g. letter strokes). */
    whiteBackground: Boolean = false
)

/** Hides the system status / navigation bars while in the composition (immersive video). */
@Composable
expect fun HideSystemBars()

/**
 * Starts fetching [url] in the background so it plays at once when it is opened (e.g. the next
 * videos in a list). Safe to call repeatedly; does nothing where not supported.
 */
expect fun preloadVideo(url: String)
