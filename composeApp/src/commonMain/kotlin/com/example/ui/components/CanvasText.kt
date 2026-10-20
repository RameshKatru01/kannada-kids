package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight

/**
 * Draws [text] horizontally centred on [centerX] with its baseline at [baselineY].
 * Cross-platform replacement for android.graphics.Canvas.drawText(text, x, y, paint) with
 * Paint.Align.CENTER. [textSizePx] is in pixels, like Paint.textSize.
 */
fun DrawScope.drawTextCentered(
    textMeasurer: TextMeasurer,
    text: String,
    centerX: Float,
    baselineY: Float,
    textSizePx: Float,
    color: Color = Color.Unspecified,
    bold: Boolean = false
) {
    val layout = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = textSizePx.toSp(),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(centerX - layout.size.width / 2f, baselineY - layout.firstBaseline)
    )
}
