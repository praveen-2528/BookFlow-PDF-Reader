package com.bookflow.reader.util

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Reusable Stylus Hover Modifier that scales up and illuminates interactive UI elements
 * whenever a stylus or cursor hovers over them.
 */
fun Modifier.stylusHoverEffect(
    hoverScale: Float = 1.14f
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = if (isHovered) hoverScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "stylus_hover_scale"
    )

    this
        .hoverable(interactionSource = interactionSource)
        .scale(scale)
}

/**
 * Tracks floating stylus hover position over canvas surfaces for drawing precision reticles.
 */
fun Modifier.stylusHoverTracking(
    onHover: (Offset?) -> Unit
): Modifier = stylusHoverTracking(onHover = onHover, onRadialMenuRequested = null)

/**
 * Tracks floating stylus hover position over canvas surfaces for drawing precision reticles,
 * and detects barrel button clicks or hover stillness to summon the Radial Menu right at the pen tip.
 */
fun Modifier.stylusHoverTracking(
    onHover: (Offset?) -> Unit,
    onRadialMenuRequested: ((Offset) -> Unit)?
): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        var hoverAnchor: Offset? = null
        var hoverStartTime = 0L

        while (true) {
            val event = awaitPointerEvent()
            val stylusOrMouse = event.changes.firstOrNull { 
                it.type == PointerType.Stylus || it.type == PointerType.Mouse 
            }

            // 1. Detect Stylus Barrel Button Press (Secondary Button)
            if (stylusOrMouse != null && event.buttons.isSecondaryPressed && onRadialMenuRequested != null) {
                onRadialMenuRequested(stylusOrMouse.position)
            }

            when (event.type) {
                PointerEventType.Move, PointerEventType.Enter -> {
                    if (stylusOrMouse != null && !stylusOrMouse.pressed) {
                        val pos = stylusOrMouse.position
                        onHover(pos)

                        // 2. Detect Still Hover (hovering in place for 850ms summons Radial Menu)
                        val anchor = hoverAnchor
                        if (anchor != null && (pos - anchor).getDistance() < 20f) {
                            if (hoverStartTime != 0L && hoverStartTime != Long.MAX_VALUE &&
                                System.currentTimeMillis() - hoverStartTime >= 850L
                            ) {
                                onRadialMenuRequested?.invoke(pos)
                                hoverStartTime = Long.MAX_VALUE // Trigger once per hover rest
                            }
                        } else {
                            hoverAnchor = pos
                            hoverStartTime = System.currentTimeMillis()
                        }
                    } else if (stylusOrMouse != null && stylusOrMouse.pressed) {
                        // When stylus touches the screen to ink, hide the hover reticle once
                        if (hoverAnchor != null || hoverStartTime != 0L) {
                            onHover(null)
                            hoverAnchor = null
                            hoverStartTime = 0L
                        }
                    }
                }
                PointerEventType.Exit -> {
                    onHover(null)
                    hoverAnchor = null
                    hoverStartTime = 0L
                }
                PointerEventType.Release -> {
                    if (stylusOrMouse != null) {
                        onHover(stylusOrMouse.position)
                        hoverAnchor = stylusOrMouse.position
                        hoverStartTime = System.currentTimeMillis()
                    }
                }
            }
        }
    }
}
