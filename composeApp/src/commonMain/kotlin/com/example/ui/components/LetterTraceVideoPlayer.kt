package com.example.ui.components

import com.example.platform.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LetterTraceVideoPlayer(
    modifier: Modifier = Modifier,
    letterCharacter: String = "ಅ",
    letterName: String = "a",
    videoUrl: String = "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/main/Final_A.mp4"
) {
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var showControlsOverlay by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }

    // Plays once; when it ends it stops on the last frame until the child taps "Play again"
    val videoPlayer = rememberVideoPlayer(url = videoUrl, loop = false, autoPlay = true)
    val playerIsPlaying by videoPlayer.isPlaying
    val playerIsBuffering by videoPlayer.isBuffering
    val ended by videoPlayer.isEnded

    LaunchedEffect(playerIsPlaying, playerIsBuffering) {
        // Ignore the "not playing yet" state while the stream is still buffering at start-up.
        if (playerIsPlaying || !playerIsBuffering) isPlaying = playerIsPlaying
    }
    LaunchedEffect(playerIsBuffering) {
        isBuffering = playerIsBuffering
    }

    LaunchedEffect(isMuted) {
        videoPlayer.setVolume(if (isMuted) 0f else 1f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFF7043), Color(0xFFFF5722))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Reference",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Watch Stroke Video",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD84315)
                    )
                }

                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFCC80))
                ) {
                    Text(
                        text = "$letterCharacter ($letterName)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Video player container with overlay controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1A1A))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showControlsOverlay = !showControlsOverlay
                    },
                contentAlignment = Alignment.Center
            ) {
                VideoSurface(
                    controller = videoPlayer,
                    showNativeControls = false,
                    modifier = Modifier.fillMaxSize()
                )

                // Transparent tap layer above the video (native video views would otherwise take the tap)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showControlsOverlay = !showControlsOverlay
                        }
                )

                // Streaming loading indicator
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = Color(0xFFFF7043),
                        strokeWidth = 3.dp
                    )
                }

                // Finished: one big "Play again" button
                if (ended && !isPlaying) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0xFFFF5722))
                            .clickable {
                                videoPlayer.play()
                                isPlaying = true
                                showControlsOverlay = false
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Text("Play again", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                } else if (showControlsOverlay || !isPlaying) {
                // Controls overlay
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        // Replay from beginning
                        IconButton(
                            onClick = {
                                videoPlayer.seekTo(0)
                                videoPlayer.play()
                                isPlaying = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Replay",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Play / Pause toggle
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    videoPlayer.pause()
                                } else {
                                    videoPlayer.play()
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Mute / Unmute toggle
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
