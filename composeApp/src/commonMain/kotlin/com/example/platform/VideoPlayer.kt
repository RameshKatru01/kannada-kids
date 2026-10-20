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
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun release()
}

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
    modifier: Modifier = Modifier
)

/** Hides the system status / navigation bars while in the composition (immersive video). */
@Composable
expect fun HideSystemBars()
