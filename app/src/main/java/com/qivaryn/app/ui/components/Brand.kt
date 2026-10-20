package com.qivaryn.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.qivaryn.app.ui.theme.QivarynColors

enum class Platform(val label: String) {
    INSTAGRAM("Instagram"),
    FACEBOOK("Facebook"),
    TIKTOK("TikTok"),
    YOUTUBE("YouTube")
}

/**
 * The Qivaryn mark: a soft grey Q, two keys, and a teal privacy shield.
 * Drawn directly rather than shipped as a bitmap so it stays crisp at any size.
 */
@Composable
fun QivarynLogo(modifier: Modifier = Modifier) {
    val colors = QivarynColors.current
    Canvas(modifier = modifier) {
        val u = size.minDimension
        val orange = colors.LogoGray
        val shield = colors.Teal

        // Two keys tucked behind the Q's upper-right shoulder.
        rotate(degrees = -38f, pivot = Offset(u * 0.80f, u * 0.20f)) {
            drawKey(
                cx = u * 0.80f,
                cy = u * 0.20f,
                length = u * 0.19f,
                color = orange,
                stroke = u * 0.032f
            )
        }
        rotate(degrees = -38f, pivot = Offset(u * 0.87f, u * 0.36f)) {
            drawKey(
                cx = u * 0.87f,
                cy = u * 0.36f,
                length = u * 0.16f,
                color = orange,
                stroke = u * 0.030f
            )
        }

        // The Q ring.
        drawOval(
            color = orange,
            topLeft = Offset(u * 0.10f, u * 0.10f),
            size = Size(u * 0.55f, u * 0.55f),
            style = Stroke(width = u * 0.112f, cap = StrokeCap.Round)
        )

        // Q tail, flowing down toward the shield.
        val tail = Path().apply {
            moveTo(u * 0.55f, u * 0.55f)
            cubicTo(u * 0.64f, u * 0.66f, u * 0.68f, u * 0.76f, u * 0.68f, u * 0.86f)
        }
        drawPath(
            path = tail,
            color = orange,
            style = Stroke(width = u * 0.105f, cap = StrokeCap.Round)
        )

        // Privacy shield with a check.
        val shieldPath = Path().apply {
            moveTo(u * 0.75f, u * 0.49f)
            lineTo(u * 0.94f, u * 0.56f)
            lineTo(u * 0.94f, u * 0.70f)
            cubicTo(u * 0.94f, u * 0.83f, u * 0.86f, u * 0.91f, u * 0.75f, u * 0.95f)
            cubicTo(u * 0.64f, u * 0.91f, u * 0.56f, u * 0.83f, u * 0.56f, u * 0.70f)
            lineTo(u * 0.56f, u * 0.56f)
            close()
        }
        drawPath(path = shieldPath, color = shield)
        val check = Path().apply {
            moveTo(u * 0.65f, u * 0.72f)
            lineTo(u * 0.72f, u * 0.79f)
            lineTo(u * 0.86f, u * 0.63f)
        }
        drawPath(
            path = check,
            color = colors.Cream,
            style = Stroke(width = u * 0.045f, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawKey(
    cx: Float,
    cy: Float,
    length: Float,
    color: Color,
    stroke: Float
) {
    drawCircle(
        color = color,
        radius = length * 0.22f,
        center = Offset(cx, cy),
        style = Stroke(width = stroke)
    )
    drawLine(
        color = color,
        start = Offset(cx + length * 0.20f, cy),
        end = Offset(cx + length * 0.92f, cy),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(cx + length * 0.70f, cy),
        end = Offset(cx + length * 0.70f, cy + length * 0.22f),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(cx + length * 0.88f, cy),
        end = Offset(cx + length * 0.88f, cy + length * 0.18f),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
}

/**
 * Simplified, monochrome platform marks sized to match Material icons.
 * [plate] is the surface behind the mark, used for cut-outs.
 */
@Composable
fun BrandGlyph(
    platform: Platform,
    tint: Color,
    plate: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val u = size.minDimension
        when (platform) {
            Platform.INSTAGRAM -> {
                val inset = u * 0.10f
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(inset, inset),
                    size = Size(u - inset * 2, u - inset * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.24f),
                    style = Stroke(width = u * 0.085f)
                )
                drawCircle(
                    color = tint,
                    radius = u * 0.19f,
                    center = Offset(u * 0.5f, u * 0.5f),
                    style = Stroke(width = u * 0.085f)
                )
                drawCircle(
                    color = tint,
                    radius = u * 0.05f,
                    center = Offset(u * 0.74f, u * 0.26f)
                )
            }

            Platform.FACEBOOK -> {
                drawCircle(color = tint, radius = u * 0.43f, center = Offset(u * 0.5f, u * 0.5f))
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = plate.toArgb()
                    typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    textSize = u * 0.72f
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "f",
                    u * 0.52f,
                    u * 0.5f - (paint.ascent() + paint.descent()) / 2f,
                    paint
                )
            }

            Platform.TIKTOK -> {
                val note = Path().apply {
                    moveTo(u * 0.52f, u * 0.18f)
                    lineTo(u * 0.52f, u * 0.65f)
                    cubicTo(u * 0.48f, u * 0.61f, u * 0.42f, u * 0.59f, u * 0.36f, u * 0.61f)
                    cubicTo(u * 0.26f, u * 0.63f, u * 0.20f, u * 0.72f, u * 0.23f, u * 0.81f)
                    cubicTo(u * 0.26f, u * 0.91f, u * 0.36f, u * 0.95f, u * 0.45f, u * 0.91f)
                    cubicTo(u * 0.52f, u * 0.88f, u * 0.56f, u * 0.81f, u * 0.56f, u * 0.73f)
                    lineTo(u * 0.56f, u * 0.39f)
                    cubicTo(u * 0.64f, u * 0.49f, u * 0.76f, u * 0.54f, u * 0.88f, u * 0.54f)
                    lineTo(u * 0.88f, u * 0.38f)
                    cubicTo(u * 0.78f, u * 0.38f, u * 0.70f, u * 0.32f, u * 0.68f, u * 0.24f)
                    close()
                }
                drawPath(note, tint)
            }

            Platform.YOUTUBE -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(u * 0.07f, u * 0.23f),
                    size = Size(u * 0.86f, u * 0.54f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.17f)
                )
                val play = Path().apply {
                    moveTo(u * 0.43f, u * 0.37f)
                    lineTo(u * 0.43f, u * 0.63f)
                    lineTo(u * 0.67f, u * 0.50f)
                    close()
                }
                drawPath(play, plate)
            }
        }
    }
}

/**
 * A soft editorial fallback used when a download has no readable thumbnail.
 * Real downloads use the decoded frame or image from the public folder.
 */
@Composable
fun DecorativeThumbnail(seed: String, modifier: Modifier = Modifier) {
    val colors = QivarynColors.current
    val index = (seed.hashCode() and Int.MAX_VALUE) % colors.ThumbTints.size
    val (deep, light) = colors.ThumbTints[index]
    Canvas(modifier = modifier) {
        val u = size.minDimension
        drawRect(Brush.verticalGradient(listOf(light, deep)))
        // Low sun.
        drawCircle(
            color = colors.Cream.copy(alpha = 0.55f),
            radius = u * 0.16f,
            center = Offset(u * 0.72f, u * 0.28f)
        )
        // Distant ridges.
        val ridge = Path().apply {
            moveTo(0f, u * 0.70f)
            lineTo(u * 0.22f, u * 0.44f)
            lineTo(u * 0.43f, u * 0.68f)
            lineTo(u * 0.66f, u * 0.38f)
            lineTo(u, u * 0.72f)
            lineTo(u, u)
            lineTo(0f, u)
            close()
        }
        drawPath(ridge, deep.copy(alpha = 0.55f))
        // Foreground bank.
        val foreground = Path().apply {
            moveTo(0f, u * 0.82f)
            cubicTo(u * 0.30f, u * 0.70f, u * 0.62f, u * 0.90f, u, u * 0.78f)
            lineTo(u, u)
            lineTo(0f, u)
            close()
        }
        drawPath(foreground, deep.copy(alpha = 0.9f))
        // Highlight streaks.
        drawLine(
            color = light.copy(alpha = 0.55f),
            start = Offset(u * 0.10f, u * 0.87f),
            end = Offset(u * 0.55f, u * 0.81f),
            strokeWidth = u * 0.025f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = light.copy(alpha = 0.35f),
            start = Offset(u * 0.50f, u * 0.94f),
            end = Offset(u * 0.90f, u * 0.88f),
            strokeWidth = u * 0.02f,
            cap = StrokeCap.Round
        )
    }
}