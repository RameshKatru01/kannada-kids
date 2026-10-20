package com.example.ui.screens

import com.example.platform.*

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TraceRepository
import com.example.ui.theme.BentoBackground
import com.example.ui.theme.BentoTitle
import com.example.ui.theme.BrightCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalControlScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parental Controls", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BentoTitle)
            )
        },
        containerColor = BentoBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Student Progress - Tracing",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BentoTitle,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val traces = TraceRepository.userTraces
            if (traces.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No traces found yet. Have the student complete some tracing games!", color = Color.DarkGray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(traces.entries.toList()) { (letter, attempts) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = BrightCyan.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Letter: $letter",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTitle
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Latest attempt:",
                                    fontSize = 14.sp,
                                    color = Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val lastAttempt = attempts.last()
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = letter,
                                        fontSize = 120.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.LightGray.copy(alpha = 0.3f),
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                    
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val strokeColor = BentoTitle
                                        val strokeWidth = 12f
                        
                                        lastAttempt.forEach { stroke ->
                                            if (stroke.size > 1) {
                                                val path = Path().apply {
                                                    // Normalize the points for preview size assuming original was somewhat screen width
                                                    // This is a simple preview, so we'll just draw it scaled down slightly.
                                                    val scale = 0.5f 
                                                    moveTo(stroke.first().x * scale, stroke.first().y * scale)
                                                    for (i in 1 until stroke.size) {
                                                        lineTo(stroke[i].x * scale, stroke[i].y * scale)
                                                    }
                                                }
                                                drawPath(
                                                    path = path,
                                                    color = strokeColor,
                                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                                )
                                            } else if (stroke.size == 1) {
                                                drawCircle(color = strokeColor, radius = strokeWidth / 2, center = Offset(stroke.first().x * 0.5f, stroke.first().y * 0.5f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
