package com.bookflow.reader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.ui.theme.DarkSurface
import com.bookflow.reader.ui.theme.LightMint
import com.bookflow.reader.ui.theme.OceanBlue
import com.bookflow.reader.ui.theme.OceanBlueLight
import com.bookflow.reader.ui.theme.WarmCream
import com.bookflow.reader.util.HapticHelper
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class RadialMenuItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val tool: AnnotationTool?,
    val isUndo: Boolean = false,
    val angleDegrees: Float
)

/**
 * Zero-UI Futuristic Hover-Activated Radial Menu.
 * Blooms at the pen tip with glassmorphic depth, allowing rapid wrist-flick tool selection.
 */
@Composable
fun StylusRadialMenu(
    anchorPosition: Offset,
    currentTool: AnnotationTool,
    onToolSelected: (AnnotationTool) -> Unit,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val items = remember {
        listOf(
            RadialMenuItem("pen", "Ballpoint", Icons.Filled.Edit, AnnotationTool.PEN, angleDegrees = -135f),
            RadialMenuItem("fountain", "Fountain Pen", Icons.Filled.Brush, AnnotationTool.FOUNTAIN, angleDegrees = -90f),
            RadialMenuItem("calligraphy", "Calligraphy", Icons.Filled.AutoFixNormal, AnnotationTool.CALLIGRAPHY, angleDegrees = -45f),
            RadialMenuItem("highlighter", "Highlighter", Icons.Filled.Highlight, AnnotationTool.HIGHLIGHTER, angleDegrees = 0f),
            RadialMenuItem("laser", "Laser Pointer", Icons.Filled.Bolt, AnnotationTool.LASER, angleDegrees = 45f),
            RadialMenuItem("eraser", "Eraser", Icons.Filled.CleaningServices, AnnotationTool.ERASER, angleDegrees = 90f),
            RadialMenuItem("lasso", "Lasso Select", Icons.Filled.Gesture, AnnotationTool.LASSO, angleDegrees = 135f),
            RadialMenuItem("undo", "Undo", Icons.AutoMirrored.Filled.Undo, null, isUndo = true, angleDegrees = 180f)
        )
    }

    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    val menuRadius = 96.dp
    val menuRadiusPx = with(density) { menuRadius.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        // Position radial center at anchor, clamped away from display edges
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenW = maxWidth.value * density.density
            val screenH = maxHeight.value * density.density

            val clampedX = anchorPosition.x.coerceIn(menuRadiusPx + 24f, (screenW - menuRadiusPx - 24f).coerceAtLeast(menuRadiusPx + 24f))
            val clampedY = anchorPosition.y.coerceIn(menuRadiusPx + 24f, (screenH - menuRadiusPx - 24f).coerceAtLeast(menuRadiusPx + 24f))

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (clampedX - menuRadiusPx).toInt(),
                            (clampedY - menuRadiusPx).toInt()
                        )
                    }
                    .size(menuRadius * 2)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue
                                val pos = change.position
                                val localX = pos.x - menuRadiusPx
                                val localY = pos.y - menuRadiusPx
                                val dist = hypot(localX, localY)

                                if (dist in 20f..(menuRadiusPx + 24f)) {
                                    var deg = Math.toDegrees(atan2(localY.toDouble(), localX.toDouble())).toFloat()
                                    // Normalize [-180, 180]
                                    var closestIndex = 0
                                    var minDiff = Float.MAX_VALUE
                                    items.forEachIndexed { idx, itm ->
                                        var diff = kotlin.math.abs(deg - itm.angleDegrees)
                                        if (diff > 180f) diff = 360f - diff
                                        if (diff < minDiff) {
                                            minDiff = diff
                                            closestIndex = idx
                                        }
                                    }
                                    if (hoveredIndex != closestIndex) {
                                        hoveredIndex = closestIndex
                                        HapticHelper.tick(context)
                                    }
                                } else {
                                    hoveredIndex = null
                                }

                                // Selection upon release
                                if (event.changes.all { !it.pressed } && change.previousPressed) {
                                    val idx = hoveredIndex
                                    if (idx != null) {
                                        val itm = items[idx]
                                        HapticHelper.click(context)
                                        if (itm.isUndo) {
                                            onUndo()
                                        } else if (itm.tool != null) {
                                            onToolSelected(itm.tool)
                                        }
                                        onDismiss()
                                    }
                                }
                            }
                        }
                    }
            ) {
                // Background Frosted Glass Halo & Ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Outer ambient glass glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(OceanBlue.copy(alpha = 0.35f), Color.Transparent),
                            center = center,
                            radius = size.width / 1.8f
                        )
                    )

                    // Translucent frosted glass core
                    drawCircle(
                        color = Color(0xDD0D1B2A),
                        radius = menuRadiusPx,
                        center = center
                    )

                    // Cybernetic sweep border ring
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(OceanBlue, LightMint, OceanBlueLight, OceanBlue),
                            center = center
                        ),
                        radius = menuRadiusPx - 1f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Inner hub circle
                    drawCircle(
                        color = Color(0xEE1E293B),
                        radius = 24.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = LightMint.copy(alpha = 0.7f),
                        radius = 24.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Render each radial petal item
                items.forEachIndexed { index, item ->
                    val isHovered = hoveredIndex == index
                    val isCurrent = item.tool == currentTool

                    val rad = Math.toRadians(item.angleDegrees.toDouble())
                    val itemDist = with(density) { 62.dp.toPx() }
                    val itemX = menuRadiusPx + itemDist * cos(rad).toFloat()
                    val itemY = menuRadiusPx + itemDist * sin(rad).toFloat()

                    val itemScale by animateFloatAsState(
                        targetValue = if (isHovered) 1.25f else if (isCurrent) 1.1f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "RadialItemScale_$index"
                    )

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (itemX - with(density) { 19.dp.toPx() }).toInt(),
                                    (itemY - with(density) { 19.dp.toPx() }).toInt()
                                )
                            }
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isHovered -> LightMint
                                    isCurrent -> OceanBlue
                                    else -> Color(0x66334155)
                                }
                            )
                            .clickable {
                                HapticHelper.click(context)
                                if (item.isUndo) onUndo() else item.tool?.let(onToolSelected)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = when {
                                isHovered -> Color(0xFF0F172A)
                                isCurrent -> Color.White
                                else -> WarmCream
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Floating label tooltip pill in center
                hoveredIndex?.let { idx ->
                    val hoveredItem = items[idx]
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xEE0F172A),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 0.dp)
                    ) {
                        Text(
                            text = hoveredItem.label,
                            color = LightMint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
