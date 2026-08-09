package sak.compose.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * SAK brand mark — a rounded indigo tile with a **layered stack** of three slabs.
 *
 * The stack represents the platform layers: config & SDK (base) → runtime (middle) →
 * UI (top). The cyan dot is the accent mark. Pure vector, rendered on both platforms.
 *
 * Pass a size via the [Modifier] (e.g. `Modifier.size(48.dp)`).
 */
@Composable
fun SakLogo(
    modifier: Modifier = Modifier,
    tileStart: Color = SakIndigo,
    tileEnd: Color = SakIndigoDark,
    mark: Color = Color.White,
    accent: Color = SakCyan,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawRoundRect(
            brush = Brush.verticalGradient(listOf(tileStart, tileEnd)),
            cornerRadius = CornerRadius(w * 0.26f, w * 0.26f),
        )

        // Layered stack — SDK base (widest) up to UI (topmost).
        val slabHeight = h * 0.14f
        val slabRadius = CornerRadius(slabHeight / 2f, slabHeight / 2f)
        drawSlab(mark, alpha = 0.55f, x = 0.20f, y = 0.64f, width = 0.60f, height = slabHeight, radius = slabRadius)
        drawSlab(mark, alpha = 0.78f, x = 0.28f, y = 0.44f, width = 0.44f, height = slabHeight, radius = slabRadius)
        drawSlab(mark, alpha = 1.00f, x = 0.36f, y = 0.24f, width = 0.28f, height = slabHeight, radius = slabRadius)

        drawCircle(
            color = accent,
            radius = w * 0.055f,
            center = Offset(w * 0.82f, h * 0.20f),
        )
    }
}

private fun DrawScope.drawSlab(
    color: Color,
    alpha: Float,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    radius: CornerRadius,
) {
    drawRoundRect(
        color = color.copy(alpha = alpha),
        topLeft = Offset(size.width * x, size.height * y),
        size = Size(size.width * width, height),
        cornerRadius = radius,
    )
}

/** SAK brand palette. */
val SakIndigo = Color(0xFF6366F1)
val SakIndigoDark = Color(0xFF4338CA)
val SakCyan = Color(0xFF22D3EE)
