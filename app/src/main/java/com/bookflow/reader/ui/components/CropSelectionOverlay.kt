package com.bookflow.reader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.ui.theme.DeepBlue
import com.bookflow.reader.ui.theme.OceanBlue
import com.bookflow.reader.ui.theme.WarmCream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private enum class DragHandle {
    NONE, BODY, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    TOP_EDGE, BOTTOM_EDGE, LEFT_EDGE, RIGHT_EDGE
}

@Composable
fun CropSelectionOverlay(
    modifier: Modifier = Modifier,
    onCropSelected: (normalizedLeft: Float, normalizedTop: Float, normalizedRight: Float, normalizedBottom: Float) -> Unit,
    onMarkRecall: ((normalizedLeft: Float, normalizedTop: Float, normalizedRight: Float, normalizedBottom: Float) -> Unit)? = null,
    onCancel: () -> Unit
) {
    // Selection rectangle in pixel coordinates
    var selLeft by remember { mutableStateOf(-1f) }
    var selTop by remember { mutableStateOf(-1f) }
    var selRight by remember { mutableStateOf(-1f) }
    var selBottom by remember { mutableStateOf(-1f) }
    var hasSelection by remember { mutableStateOf(false) }
    var isDrawing by remember { mutableStateOf(false) }
    var activeDragHandle by remember { mutableStateOf(DragHandle.NONE) }
    var dragAnchor by remember { mutableStateOf(Offset.Zero) }
    var dragOrigRect by remember { mutableStateOf(listOf(0f, 0f, 0f, 0f)) }

    val handleRadius = 18f
    val handleHitRadius = 40f

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()

                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val px = down.position.x
                        val py = down.position.y

                        if (hasSelection && !isDrawing) {
                            // Check if touching a handle or the body for transform
                            val handle = detectHandle(
                                px, py, selLeft, selTop, selRight, selBottom, handleHitRadius
                            )
                            if (handle != DragHandle.NONE) {
                                activeDragHandle = handle
                                dragAnchor = Offset(px, py)
                                dragOrigRect = listOf(selLeft, selTop, selRight, selBottom)

                                var event = awaitPointerEvent()
                                while (event.changes.any { it.pressed }) {
                                    val change = event.changes.firstOrNull()
                                    if (change != null && change.pressed) {
                                        val dx = change.position.x - dragAnchor.x
                                        val dy = change.position.y - dragAnchor.y
                                        applyHandleDrag(
                                            handle, dx, dy,
                                            dragOrigRect[0], dragOrigRect[1],
                                            dragOrigRect[2], dragOrigRect[3],
                                            w, h
                                        ) { nl, nt, nr, nb ->
                                            selLeft = nl; selTop = nt
                                            selRight = nr; selBottom = nb
                                        }
                                        change.consume()
                                    }
                                    event = awaitPointerEvent()
                                }
                                activeDragHandle = DragHandle.NONE
                                return@awaitEachGesture
                            }
                        }

                        // Draw new selection rectangle
                        isDrawing = true
                        hasSelection = false
                        selLeft = px; selTop = py
                        selRight = px; selBottom = py

                        var event = awaitPointerEvent()
                        while (event.changes.any { it.pressed }) {
                            val change = event.changes.firstOrNull()
                            if (change != null && change.pressed) {
                                selRight = change.position.x.coerceIn(0f, w)
                                selBottom = change.position.y.coerceIn(0f, h)
                                change.consume()
                            }
                            event = awaitPointerEvent()
                        }

                        // Normalize so left < right, top < bottom
                        val nLeft = min(selLeft, selRight)
                        val nRight = max(selLeft, selRight)
                        val nTop = min(selTop, selBottom)
                        val nBottom = max(selTop, selBottom)
                        selLeft = nLeft; selTop = nTop
                        selRight = nRight; selBottom = nBottom

                        if (abs(selRight - selLeft) > 20f && abs(selBottom - selTop) > 20f) {
                            hasSelection = true
                        }
                        isDrawing = false
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // Semi-transparent dim overlay
            drawRect(color = Color.Black.copy(alpha = 0.45f), size = size)

            val l = min(selLeft, selRight)
            val t = min(selTop, selBottom)
            val r = max(selLeft, selRight)
            val b = max(selTop, selBottom)

            if ((isDrawing || hasSelection) && r - l > 2f && b - t > 2f) {
                val rectSize = Size(r - l, b - t)

                // Cut out the selected area (draw over the dim with a "clear" rect)
                drawRect(
                    color = Color.Black.copy(alpha = 0.45f),
                    topLeft = Offset(l, t),
                    size = rectSize,
                    blendMode = androidx.compose.ui.graphics.BlendMode.DstOut
                )

                // Light tint inside selection
                drawRect(
                    color = Color.White.copy(alpha = 0.06f),
                    topLeft = Offset(l, t),
                    size = rectSize
                )

                // Dashed border
                drawRect(
                    color = Color.White,
                    topLeft = Offset(l, t),
                    size = rectSize,
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    )
                )

                // Rule-of-thirds guides inside selection
                val thirdW = rectSize.width / 3f
                val thirdH = rectSize.height / 3f
                for (i in 1..2) {
                    drawLine(
                        Color.White.copy(alpha = 0.2f),
                        Offset(l + thirdW * i, t),
                        Offset(l + thirdW * i, b),
                        strokeWidth = 0.8f
                    )
                    drawLine(
                        Color.White.copy(alpha = 0.2f),
                        Offset(l, t + thirdH * i),
                        Offset(r, t + thirdH * i),
                        strokeWidth = 0.8f
                    )
                }

                // Corner handles
                val hSize = 24f
                val hStroke = 4f
                val hColor = Color(0xFF4FC3F7)
                // Top-left
                drawLine(hColor, Offset(l - 1, t), Offset(l + hSize, t), strokeWidth = hStroke)
                drawLine(hColor, Offset(l, t - 1), Offset(l, t + hSize), strokeWidth = hStroke)
                // Top-right
                drawLine(hColor, Offset(r + 1, t), Offset(r - hSize, t), strokeWidth = hStroke)
                drawLine(hColor, Offset(r, t - 1), Offset(r, t + hSize), strokeWidth = hStroke)
                // Bottom-left
                drawLine(hColor, Offset(l - 1, b), Offset(l + hSize, b), strokeWidth = hStroke)
                drawLine(hColor, Offset(l, b + 1), Offset(l, b - hSize), strokeWidth = hStroke)
                // Bottom-right
                drawLine(hColor, Offset(r + 1, b), Offset(r - hSize, b), strokeWidth = hStroke)
                drawLine(hColor, Offset(r, b + 1), Offset(r, b - hSize), strokeWidth = hStroke)

                // Edge midpoint handles (small circles)
                val midHandleRadius = 6f
                drawCircle(hColor, midHandleRadius, Offset((l + r) / 2, t))   // Top edge
                drawCircle(hColor, midHandleRadius, Offset((l + r) / 2, b))   // Bottom edge
                drawCircle(hColor, midHandleRadius, Offset(l, (t + b) / 2))   // Left edge
                drawCircle(hColor, midHandleRadius, Offset(r, (t + b) / 2))   // Right edge
            }
        }

        // Confirm / Cancel buttons
        AnimatedVisibility(
            visible = hasSelection && !isDrawing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel
                SmallFloatingActionButton(
                    onClick = onCancel,
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Close, "Cancel crop")
                }

                // Dimension label
                val cropW = abs(selRight - selLeft).toInt()
                val cropH = abs(selBottom - selTop).toInt()
                Text(
                    text = "${cropW} × ${cropH}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )

                // Option 1: To Notebook
                Surface(
                    onClick = {
                        onCropSelected(
                            min(selLeft, selRight),
                            min(selTop, selBottom),
                            max(selLeft, selRight),
                            max(selTop, selBottom)
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF2E7D32),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("To Notebook", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Option 2: Mark Recall (Tag with Emoji & typed description)
                if (onMarkRecall != null) {
                    Surface(
                        onClick = {
                            onMarkRecall(
                                min(selLeft, selRight),
                                min(selTop, selBottom),
                                max(selLeft, selRight),
                                max(selTop, selBottom)
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = DeepBlue,
                        border = BorderStroke(1.dp, OceanBlue.copy(alpha = 0.8f)),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Mark Recall", color = WarmCream, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Instructional hint when no selection
        if (!hasSelection && !isDrawing) {
            Text(
                text = "Draw a rectangle to select the area to crop",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}

private fun detectHandle(
    px: Float, py: Float,
    l: Float, t: Float, r: Float, b: Float,
    hitRadius: Float
): DragHandle {
    // Corners first (highest priority)
    if (dist(px, py, l, t) < hitRadius) return DragHandle.TOP_LEFT
    if (dist(px, py, r, t) < hitRadius) return DragHandle.TOP_RIGHT
    if (dist(px, py, l, b) < hitRadius) return DragHandle.BOTTOM_LEFT
    if (dist(px, py, r, b) < hitRadius) return DragHandle.BOTTOM_RIGHT
    // Edge midpoints
    if (dist(px, py, (l + r) / 2, t) < hitRadius) return DragHandle.TOP_EDGE
    if (dist(px, py, (l + r) / 2, b) < hitRadius) return DragHandle.BOTTOM_EDGE
    if (dist(px, py, l, (t + b) / 2) < hitRadius) return DragHandle.LEFT_EDGE
    if (dist(px, py, r, (t + b) / 2) < hitRadius) return DragHandle.RIGHT_EDGE
    // Body (inside rect)
    if (px in l..r && py in t..b) return DragHandle.BODY
    return DragHandle.NONE
}

private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2; val dy = y1 - y2
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

private fun applyHandleDrag(
    handle: DragHandle,
    dx: Float, dy: Float,
    origL: Float, origT: Float, origR: Float, origB: Float,
    maxW: Float, maxH: Float,
    apply: (Float, Float, Float, Float) -> Unit
) {
    val minDim = 30f
    when (handle) {
        DragHandle.BODY -> {
            val w = origR - origL; val h = origB - origT
            val nl = (origL + dx).coerceIn(0f, maxW - w)
            val nt = (origT + dy).coerceIn(0f, maxH - h)
            apply(nl, nt, nl + w, nt + h)
        }
        DragHandle.TOP_LEFT -> apply(
            (origL + dx).coerceIn(0f, origR - minDim), (origT + dy).coerceIn(0f, origB - minDim), origR, origB
        )
        DragHandle.TOP_RIGHT -> apply(
            origL, (origT + dy).coerceIn(0f, origB - minDim), (origR + dx).coerceIn(origL + minDim, maxW), origB
        )
        DragHandle.BOTTOM_LEFT -> apply(
            (origL + dx).coerceIn(0f, origR - minDim), origT, origR, (origB + dy).coerceIn(origT + minDim, maxH)
        )
        DragHandle.BOTTOM_RIGHT -> apply(
            origL, origT, (origR + dx).coerceIn(origL + minDim, maxW), (origB + dy).coerceIn(origT + minDim, maxH)
        )
        DragHandle.TOP_EDGE -> apply(origL, (origT + dy).coerceIn(0f, origB - minDim), origR, origB)
        DragHandle.BOTTOM_EDGE -> apply(origL, origT, origR, (origB + dy).coerceIn(origT + minDim, maxH))
        DragHandle.LEFT_EDGE -> apply((origL + dx).coerceIn(0f, origR - minDim), origT, origR, origB)
        DragHandle.RIGHT_EDGE -> apply(origL, origT, (origR + dx).coerceIn(origL + minDim, maxW), origB)
        DragHandle.NONE -> {}
    }
}
