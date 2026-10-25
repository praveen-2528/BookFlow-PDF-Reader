package com.bookflow.reader.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.ui.theme.DarkSurface
import com.bookflow.reader.ui.theme.WarmCream
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun LaserHighlightOverlay(
    cropRect: List<Float>?,
    pageAspectRatio: Float? = null,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    if (cropRect == null || cropRect.size < 4) return

    val normLeft = cropRect[0]
    val normTop = cropRect[1]
    val normRight = cropRect[2]
    val normBottom = cropRect[3]

    val density = LocalDensity.current

    // Auto dismiss after 5 seconds
    LaunchedEffect(cropRect) {
        kotlinx.coroutines.delay(5000L)
        onDismiss()
    }

    // Infinite breathing laser pulse
    val infiniteTransition = rememberInfiniteTransition(label = "LaserHighlightTransition")
    val laserPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserPulse"
    )

    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LaserDashPhase"
    )

    // Entrance fade animation
    val animAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(350),
        label = "LaserEntrance"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val renderedW: Float
            val renderedH: Float
            val offsetX: Float
            val offsetY: Float

            if (pageAspectRatio != null && pageAspectRatio > 0f) {
                val containerAspect = w / h
                if (pageAspectRatio > containerAspect) {
                    renderedW = w
                    renderedH = w / pageAspectRatio
                    offsetX = 0f
                    offsetY = (h - renderedH) / 2f
                } else {
                    renderedH = h
                    renderedW = h * pageAspectRatio
                    offsetX = (w - renderedW) / 2f
                    offsetY = 0f
                }
            } else {
                renderedW = w
                renderedH = h
                offsetX = 0f
                offsetY = 0f
            }

            val left = (offsetX + normLeft * renderedW).coerceIn(0f, w)
            val top = (offsetY + normTop * renderedH).coerceIn(0f, h)
            val right = (offsetX + normRight * renderedW).coerceIn(left + 10f, w)
            val bottom = (offsetY + normBottom * renderedH).coerceIn(top + 10f, h)

            val rw = right - left
            val rh = bottom - top

            val laserCyan = Color(0xFF00E5FF)
            val laserCore = Color(0xFFE0F7FA)

            // 1. Subtle interior laser tint
            drawRoundRect(
                color = laserCyan.copy(alpha = 0.08f * animAlpha * laserPulse),
                topLeft = Offset(left, top),
                size = Size(rw, rh),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // 2. Outer soft laser glow
            drawRoundRect(
                color = laserCyan.copy(alpha = 0.35f * laserPulse * animAlpha),
                topLeft = Offset(left - 2f, top - 2f),
                size = Size(rw + 4f, rh + 4f),
                cornerRadius = CornerRadius(14f, 14f),
                style = Stroke(width = 7.dp.toPx())
            )

            // 3. Medium laser beam
            drawRoundRect(
                color = laserCyan.copy(alpha = 0.85f * animAlpha),
                topLeft = Offset(left, top),
                size = Size(rw, rh),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f), dashPhase)
                )
            )

            // 4. Ultra-bright high-intensity laser core
            drawRoundRect(
                color = laserCore.copy(alpha = 0.95f * animAlpha),
                topLeft = Offset(left, top),
                size = Size(rw, rh),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // 5. High-tech Corner targeting crosshairs / brackets
            val bracketLen = min(rw, rh) * 0.22f.coerceIn(14f, 44f)
            val bracketStroke = 3.5.dp.toPx()

            // Top-Left bracket
            val tlPath = Path().apply {
                moveTo(left - 1f, top + bracketLen)
                lineTo(left - 1f, top - 1f)
                lineTo(left + bracketLen, top - 1f)
            }
            drawPath(tlPath, laserCyan, style = Stroke(width = bracketStroke))

            // Top-Right bracket
            val trPath = Path().apply {
                moveTo(right - bracketLen, top - 1f)
                lineTo(right + 1f, top - 1f)
                lineTo(right + 1f, top + bracketLen)
            }
            drawPath(trPath, laserCyan, style = Stroke(width = bracketStroke))

            // Bottom-Left bracket
            val blPath = Path().apply {
                moveTo(left - 1f, bottom - bracketLen)
                lineTo(left - 1f, bottom + 1f)
                lineTo(left + bracketLen, bottom + 1f)
            }
            drawPath(blPath, laserCyan, style = Stroke(width = bracketStroke))

            // Bottom-Right bracket
            val brPath = Path().apply {
                moveTo(right - bracketLen, bottom + 1f)
                lineTo(right + 1f, bottom + 1f)
                lineTo(right + 1f, bottom - bracketLen)
            }
            drawPath(brPath, laserCyan, style = Stroke(width = bracketStroke))

            // 6. Living Spark Particles emitting along the laser boundary
            val sparkTime = System.currentTimeMillis()
            for (i in 0 until 16) {
                val phase = (i * 0.0625f + (sparkTime % 2400) / 2400f) % 1f
                val perimeterT = phase * 4f
                val sx: Float
                val sy: Float
                when {
                    perimeterT < 1f -> {
                        sx = left + rw * perimeterT
                        sy = top + kotlin.math.sin((phase * 20f).toDouble()).toFloat() * 4f
                    }
                    perimeterT < 2f -> {
                        sx = right + kotlin.math.cos((phase * 20f).toDouble()).toFloat() * 4f
                        sy = top + rh * (perimeterT - 1f)
                    }
                    perimeterT < 3f -> {
                        sx = right - rw * (perimeterT - 2f)
                        sy = bottom + kotlin.math.sin((phase * 20f).toDouble()).toFloat() * 4f
                    }
                    else -> {
                        sx = left + kotlin.math.cos((phase * 20f).toDouble()).toFloat() * 4f
                        sy = bottom - rh * (perimeterT - 3f)
                    }
                }
                val sparkAlpha = (kotlin.math.sin(phase * Math.PI).toFloat()).coerceIn(0f, 1f) * animAlpha
                val sparkRadius = (2.2f + (i % 3) * 0.8f).dp.toPx()
                drawCircle(
                    color = laserCyan.copy(alpha = sparkAlpha * 0.5f),
                    radius = sparkRadius * 2.2f,
                    center = Offset(sx, sy)
                )
                drawCircle(
                    color = Color.White.copy(alpha = sparkAlpha * 0.9f),
                    radius = sparkRadius * 0.75f,
                    center = Offset(sx, sy)
                )
            }
        }

        // 6. Targeting HUD Pill on Top of the Laser Highlight
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val sw = constraints.maxWidth.toFloat()
            val sh = constraints.maxHeight.toFloat()

            val renderedW: Float
            val renderedH: Float
            val offsetX: Float
            val offsetY: Float

            if (pageAspectRatio != null && pageAspectRatio > 0f) {
                val containerAspect = sw / sh
                if (pageAspectRatio > containerAspect) {
                    renderedW = sw
                    renderedH = sw / pageAspectRatio
                    offsetX = 0f
                    offsetY = (sh - renderedH) / 2f
                } else {
                    renderedH = sh
                    renderedW = sh * pageAspectRatio
                    offsetX = (sw - renderedW) / 2f
                    offsetY = 0f
                }
            } else {
                renderedW = sw
                renderedH = sh
                offsetX = 0f
                offsetY = 0f
            }

            val leftPx = offsetX + normLeft * renderedW
            val rightPx = offsetX + normRight * renderedW
            val topPx = offsetY + normTop * renderedH
            val bottomPx = offsetY + normBottom * renderedH

            val centerX = (leftPx + rightPx) / 2f
            val hudY = if (topPx > 60f) (topPx - 42f) else (bottomPx + 12f)

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = DarkSurface.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.8f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (centerX - 110.dp.toPx()).roundToInt().coerceIn(16, (sw - 220.dp.toPx()).roundToInt().coerceAtLeast(16)),
                            hudY.roundToInt().coerceIn(16, (sh - 50.dp.toPx()).roundToInt().coerceAtLeast(16))
                        )
                    }
                    .clickable { onDismiss() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.GpsFixed,
                        contentDescription = "Target Locked",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Recall Target",
                        color = WarmCream,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = WarmCream.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
