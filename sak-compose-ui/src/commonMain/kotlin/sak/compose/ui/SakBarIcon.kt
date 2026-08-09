package sak.compose.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

private val KnownBarIcons = setOf("home", "bell", "alert", "person", "profile", "search", "settings", "grid")

/** True when [name] has a built-in [SakBarIcon] drawing. */
fun sakBarIconSupported(name: String): Boolean = name.lowercase() in KnownBarIcons

/**
 * Lightweight stroke icons for bottom-bar items — drawn with Canvas so no icon
 * library dependency is required and they render identically on Android + iOS.
 */
@Composable
fun SakBarIcon(
    name: String,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier) {
        val scale = size.width / 24f
        val stroke = 2f * scale
        val style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)

        fun s(v: Float): Float = v * scale
        fun pt(x: Float, y: Float) = Offset(s(x), s(y))
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, pt(x1, y1), pt(x2, y2), strokeWidth = stroke, cap = StrokeCap.Round)

        when (name.lowercase()) {
            "home" -> {
                drawPath(
                    path = Path().apply {
                        moveTo(s(4f), s(11f))
                        lineTo(s(12f), s(4f))
                        lineTo(s(20f), s(11f))
                    },
                    color = tint,
                    style = style,
                )
                drawPath(
                    path = Path().apply {
                        moveTo(s(6f), s(11f))
                        lineTo(s(6f), s(19f))
                        lineTo(s(18f), s(19f))
                        lineTo(s(18f), s(11f))
                    },
                    color = tint,
                    style = style,
                )
                line(12f, 13f, 12f, 19f)
            }

            "bell", "alert" -> {
                drawPath(
                    path = Path().apply {
                        moveTo(s(5f), s(13f))
                        quadraticTo(s(5f), s(6f), s(12f), s(6f))
                        quadraticTo(s(19f), s(6f), s(19f), s(13f))
                    },
                    color = tint,
                    style = style,
                )
                line(4f, 13f, 20f, 13f)
                line(12f, 14f, 12f, 18f)
            }

            "person", "profile" -> {
                drawCircle(
                    color = tint,
                    radius = s(3.5f),
                    center = pt(12f, 8f),
                    style = Stroke(width = stroke),
                )
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(s(6f), s(12f)),
                    size = Size(s(12f), s(7f)),
                    style = Stroke(width = stroke),
                )
            }

            "search" -> {
                drawCircle(
                    color = tint,
                    radius = s(6f),
                    center = pt(10f, 10f),
                    style = Stroke(width = stroke),
                )
                line(14.5f, 14.5f, 20f, 20f)
            }

            "settings" -> {
                line(4f, 6f, 20f, 6f)
                drawCircle(color = tint, radius = s(1.8f), center = pt(12f, 6f))
                line(4f, 12f, 20f, 12f)
                drawCircle(color = tint, radius = s(1.8f), center = pt(8f, 12f))
                line(4f, 18f, 20f, 18f)
                drawCircle(color = tint, radius = s(1.8f), center = pt(16f, 18f))
            }

            "grid" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(s(5f), s(5f)),
                    size = Size(s(6f), s(6f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(1.5f), s(1.5f)),
                    style = Stroke(width = stroke),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(s(13f), s(5f)),
                    size = Size(s(6f), s(6f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(1.5f), s(1.5f)),
                    style = Stroke(width = stroke),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(s(5f), s(13f)),
                    size = Size(s(6f), s(6f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(1.5f), s(1.5f)),
                    style = Stroke(width = stroke),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(s(13f), s(13f)),
                    size = Size(s(6f), s(6f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s(1.5f), s(1.5f)),
                    style = Stroke(width = stroke),
                )
            }
        }
    }
}
