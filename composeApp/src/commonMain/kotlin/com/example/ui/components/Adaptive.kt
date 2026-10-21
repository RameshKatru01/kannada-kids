package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Keeps content in a comfortable centred column on tablets (iPad / Android tablets),
 * while phones still use the full width. Put it after the full-width background.
 */
fun Modifier.readableWidth(max: Dp = 640.dp): Modifier =
    this.fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = max)
        .fillMaxWidth()
