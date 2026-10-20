package com.example.ui.components

import com.example.platform.*

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlinx.coroutines.isActive
import kotlin.random.Random

data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val rotationSpeed: Float
)

@Composable
fun ConfettiEffect(modifier: Modifier = Modifier) {
    var particles by remember { mutableStateOf(emptyList<ConfettiParticle>()) }

    LaunchedEffect(Unit) {
        val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Magenta, Color.Cyan, Color(0xFFFF9800))
        var currentParticles = List(150) {
            ConfettiParticle(
                x = Random.nextFloat(), // 0 to 1 (relative to screen width)
                y = -Random.nextFloat(), // Start slightly above screen
                vx = Random.nextFloat() * 1f - 0.5f,
                vy = Random.nextFloat() * 1.5f + 1f,
                color = colors[Random.nextInt(colors.size)],
                size = Random.nextFloat() * 20f + 15f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 10f - 5f
            )
        }
        
        while (isActive) {
            withFrameNanos { time ->
                currentParticles = currentParticles.map { p ->
                    var newY = p.y + p.vy * 0.005f
                    var newX = p.x + p.vx * 0.005f
                    var newVy = p.vy + 0.02f // Gravity
                    
                    if (newY > 1.2f) { // Reset if fallen out of bounds
                        newY = -Random.nextFloat() * 0.2f
                        newX = Random.nextFloat()
                        newVy = Random.nextFloat() * 1.5f + 1f
                    }
                    
                    p.copy(
                        x = newX,
                        y = newY,
                        vy = newVy,
                        rotation = p.rotation + p.rotationSpeed
                    )
                }
                particles = currentParticles
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        
        particles.forEach { p ->
            withTransform({
                translate(p.x * w, p.y * h)
                rotate(p.rotation)
            }) {
                drawRect(
                    color = p.color,
                    size = Size(p.size, p.size / 2f)
                )
            }
        }
    }
}
