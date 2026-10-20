package com.example.platform

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Cross-platform replacement for android.widget.Toast.
 * Usage is unchanged: Toast.makeText(context, "text", Toast.LENGTH_SHORT).show()
 * The message is drawn by [ToastHost], which sits on top of the whole app.
 */
class Toast private constructor(private val text: String, private val duration: Int) {

    fun show() {
        ToastState.counter += 1
        ToastState.current.value = ToastMessage(ToastState.counter, text, duration)
    }

    companion object {
        const val LENGTH_SHORT = 0
        const val LENGTH_LONG = 1

        @Suppress("UNUSED_PARAMETER")
        fun makeText(context: Context?, text: CharSequence, duration: Int): Toast =
            Toast(text.toString(), duration)
    }
}

internal data class ToastMessage(val id: Long, val text: String, val duration: Int)

internal object ToastState {
    var counter = 0L
    val current = MutableStateFlow<ToastMessage?>(null)
}

@Composable
fun ToastHost(modifier: Modifier = Modifier) {
    val message by ToastState.current.collectAsState()

    LaunchedEffect(message?.id) {
        val m = message ?: return@LaunchedEffect
        delay(if (m.duration == Toast.LENGTH_LONG) 3500L else 2000L)
        if (ToastState.current.value?.id == m.id) ToastState.current.value = null
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = message != null, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp, start = 24.dp, end = 24.dp)
                    .widthIn(max = 420.dp)
                    .background(Color(0xE6323232), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message?.text ?: "",
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
