package com.voidx.client.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voidx.client.ui.theme.VoidViolet

/** Original VoidX mark: a ring with an offset core, drawn in code (no bitmap assets). */
@Composable
fun VoidXLogo(size: Dp = 36.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val stroke = w * 0.12f
        drawCircle(
            brush = Brush.sweepGradient(listOf(VoidViolet, Color(0xFF38BDF8), VoidViolet)),
            radius = w / 2f - stroke / 2f,
            style = Stroke(width = stroke),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = w * 0.14f,
            center = Offset(w / 2f, w / 2f),
        )
    }
}
