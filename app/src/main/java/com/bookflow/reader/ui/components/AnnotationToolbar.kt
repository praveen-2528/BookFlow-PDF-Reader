package com.bookflow.reader.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.HighlightAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.ui.theme.DarkSurface
import com.bookflow.reader.ui.theme.DarkSurfaceVariant
import com.bookflow.reader.ui.theme.DeepBlue
import com.bookflow.reader.ui.theme.LightMint
import com.bookflow.reader.ui.theme.OceanBlue
import com.bookflow.reader.ui.theme.WarmCream
import com.bookflow.reader.util.stylusHoverEffect
import java.util.Locale

enum class AnnotationTool { PEN, FOUNTAIN, CALLIGRAPHY, HIGHLIGHTER, RECT_HIGHLIGHT, ERASER, LASER, LASSO }
enum class EraserMode { STROKE, PRECISE }

@Composable
fun AnnotationToolbar(
    isVisible: Boolean,
    currentTool: AnnotationTool,
    eraserMode: EraserMode = EraserMode.STROKE,
    currentColor: Color,
    strokeWidth: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    onToolSelected: (AnnotationTool) -> Unit,
    onEraserModeSelected: (EraserMode) -> Unit = {},
    onColorSelected: (Color) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit,
    onClearPageClick: () -> Unit = {},
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onMoreColorsClick: () -> Unit = {},
    onPressureCurveClick: () -> Unit = {},
    isStylusOnly: Boolean = true,
    onToggleStylusOnly: () -> Unit = {},
    isCollapsed: Boolean = false,
    onToggleCollapse: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activePopupTool by remember { mutableStateOf<AnnotationTool?>(null) }

    LaunchedEffect(currentTool) {
        if (activePopupTool != null && activePopupTool != currentTool) {
            activePopupTool = null
        }
    }

    val handleToolClick: (AnnotationTool) -> Unit = { tool ->
        if (currentTool != tool) {
            onToolSelected(tool)
            activePopupTool = null
        } else {
            // Tapped already selected tool -> toggle options popup!
            activePopupTool = if (activePopupTool == tool) null else tool
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp, start = 12.dp, end = 12.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedContent(
                targetState = isCollapsed,
                transitionSpec = {
                    (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                     scaleIn(initialScale = 0.88f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)))
                    .togetherWith(
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        scaleOut(targetScale = 0.88f)
                    )
                },
                label = "ToolbarCollapseAnimation"
            ) { collapsed ->
                if (collapsed) {
                    // Floating Collapsed Bubble
                    Surface(
                        modifier = Modifier
                            .shadow(16.dp, shape = RoundedCornerShape(26.dp))
                            .border(
                                BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(26.dp)
                            ),
                        shape = RoundedCornerShape(26.dp),
                        color = DarkSurface.copy(alpha = 0.95f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Tap active tool pill to expand toolbar
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DeepBlue.copy(alpha = 0.45f))
                                    .clickable { onToggleCollapse() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .stylusHoverEffect(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val toolIcon = when (currentTool) {
                                    AnnotationTool.PEN -> Icons.Filled.Edit
                                    AnnotationTool.FOUNTAIN -> Icons.Filled.Brush
                                    AnnotationTool.CALLIGRAPHY -> Icons.Filled.AutoFixNormal
                                    AnnotationTool.HIGHLIGHTER -> Icons.Filled.BorderColor
                                    AnnotationTool.RECT_HIGHLIGHT -> Icons.Filled.FormatColorFill
                                    AnnotationTool.ERASER -> Icons.Filled.Delete
                                    AnnotationTool.LASSO -> Icons.Filled.HighlightAlt
                                    AnnotationTool.LASER -> Icons.Filled.Bolt
                                }
                                Icon(
                                    imageVector = toolIcon,
                                    contentDescription = "Active Tool",
                                    tint = if (currentTool == AnnotationTool.ERASER) WarmCream else currentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                if (currentTool != AnnotationTool.ERASER) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(currentColor)
                                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (eraserMode == EraserMode.STROKE) Icons.Filled.Bolt else Icons.Filled.TrackChanges,
                                        contentDescription = null,
                                        tint = OceanBlue,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Expand Toolbar",
                                    tint = OceanBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            VerticalDivider(
                                modifier = Modifier.height(20.dp).padding(horizontal = 2.dp),
                                color = DarkSurfaceVariant
                            )

                            // Quick Undo / Redo
                            IconButton(
                                onClick = onUndo,
                                enabled = canUndo,
                                modifier = Modifier.size(32.dp).stylusHoverEffect()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo",
                                    tint = if (canUndo) WarmCream else Color.Gray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            IconButton(
                                onClick = onRedo,
                                enabled = canRedo,
                                modifier = Modifier.size(32.dp).stylusHoverEffect()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Redo,
                                    contentDescription = "Redo",
                                    tint = if (canRedo) WarmCream else Color.Gray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            VerticalDivider(
                                modifier = Modifier.height(20.dp).padding(horizontal = 2.dp),
                                color = DarkSurfaceVariant
                            )

                            // Close / Done button
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(DeepBlue.copy(alpha = 0.4f))
                                    .stylusHoverEffect()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Done / Close",
                                    tint = OceanBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ==========================================
                        // FLOATING EXTENDED OPTIONS POPUP CARD (ON 2ND TAP)
                        // ==========================================
                        AnimatedVisibility(
                            visible = activePopupTool != null,
                            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                                    slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) { it / 2 } +
                                    scaleIn(initialScale = 0.92f),
                            exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
                                   slideOutVertically { it / 2 } +
                                   scaleOut(targetScale = 0.92f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = DarkSurface.copy(alpha = 0.98f),
                                border = BorderStroke(1.dp, OceanBlue.copy(alpha = 0.45f)),
                                shadowElevation = 18.dp,
                                modifier = Modifier.widthIn(max = 520.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Header: Title & Close Button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val popupTitle = when (activePopupTool) {
                                            AnnotationTool.PEN -> "Pen Settings"
                                            AnnotationTool.FOUNTAIN -> "Fountain Pen Settings"
                                            AnnotationTool.CALLIGRAPHY -> "Calligraphy Pen Settings"
                                            AnnotationTool.HIGHLIGHTER -> "Highlighter Settings"
                                            AnnotationTool.RECT_HIGHLIGHT -> "Box Highlight Settings"
                                            AnnotationTool.ERASER -> "Eraser Settings"
                                            AnnotationTool.LASER -> "Laser Pointer"
                                            AnnotationTool.LASSO -> "Lasso Selection"
                                            else -> "Tool Settings"
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(if (activePopupTool == AnnotationTool.ERASER) OceanBlue else currentColor)
                                            )
                                            Text(
                                                text = popupTitle,
                                                color = WarmCream,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        IconButton(
                                            onClick = { activePopupTool = null },
                                            modifier = Modifier.size(26.dp).stylusHoverEffect()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Close Popup",
                                                tint = WarmCream.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.6f))

                                    when (activePopupTool) {
                                        AnnotationTool.PEN, AnnotationTool.FOUNTAIN, AnnotationTool.CALLIGRAPHY,
                                        AnnotationTool.HIGHLIGHTER, AnnotationTool.RECT_HIGHLIGHT -> {
                                            // 1. Thickness Presets & Fine Slider
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Thickness: ${String.format(Locale.US, "%.1f", strokeWidth)} pt",
                                                    color = WarmCream.copy(alpha = 0.85f),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )

                                                // Quick Presets
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    val presets = listOf(2.5f to "Fine", 4.5f to "Med", 8.0f to "Bold", 14.0f to "Thick")
                                                    presets.forEach { (presetW, _) ->
                                                        val isSel = kotlin.math.abs(strokeWidth - presetW) < 1.0f
                                                        Box(
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSel) OceanBlue.copy(alpha = 0.3f) else DarkSurfaceVariant.copy(alpha = 0.6f))
                                                                .border(
                                                                    width = if (isSel) 1.5.dp else 0.5.dp,
                                                                    color = if (isSel) OceanBlue else Color.White.copy(alpha = 0.15f),
                                                                    shape = CircleShape
                                                                )
                                                                .clickable { onStrokeWidthChanged(presetW) }
                                                                .stylusHoverEffect(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size((presetW * 1.1f).coerceIn(4f, 16f).dp)
                                                                    .clip(CircleShape)
                                                                    .background(currentColor)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Slider with Steppers
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { onStrokeWidthChanged((strokeWidth - 1f).coerceAtLeast(1.5f)) },
                                                    modifier = Modifier.size(28.dp).stylusHoverEffect()
                                                ) {
                                                    Text("-", color = WarmCream, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Slider(
                                                    value = strokeWidth,
                                                    onValueChange = onStrokeWidthChanged,
                                                    valueRange = 1.5f..24f,
                                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = OceanBlue,
                                                        activeTrackColor = OceanBlue,
                                                        inactiveTrackColor = DarkSurfaceVariant
                                                    )
                                                )
                                                IconButton(
                                                    onClick = { onStrokeWidthChanged((strokeWidth + 1f).coerceAtMost(24f)) },
                                                    modifier = Modifier.size(28.dp).stylusHoverEffect()
                                                ) {
                                                    Text("+", color = WarmCream, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            // 2. Color Palette Swatches
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = "Color Palette",
                                                    color = WarmCream.copy(alpha = 0.85f),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                val palette = listOf(
                                                    Color(0xFF790D16), // Palette Burgundy
                                                    Color(0xFF2D2D2D), // Charcoal Black
                                                    Color(0xFFE53935), // Crimson Red
                                                    Color(0xFF2D6A9F), // Deep Blue
                                                    Color(0xFF3498DB), // Ocean Blue
                                                    Color(0xFF2ECC71), // Emerald Green
                                                    Color(0xFFF1C40F), // Golden Yellow
                                                    Color(0xFFE67E22), // Orange
                                                    Color(0xFF9B59B6), // Purple
                                                    Color(0xFFE5D3AF), // Parchment
                                                    Color(0xFFFFFFFF)  // White
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    palette.forEach { color ->
                                                        ColorSwatch(
                                                            color = color,
                                                            isSelected = currentColor == color,
                                                            onClick = { onColorSelected(color) }
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = onMoreColorsClick,
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(DarkSurfaceVariant.copy(alpha = 0.6f))
                                                            .stylusHoverEffect()
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Add,
                                                            contentDescription = "More Colors",
                                                            tint = WarmCream,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = onPressureCurveClick,
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(DarkSurfaceVariant.copy(alpha = 0.6f))
                                                            .stylusHoverEffect()
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Settings,
                                                            contentDescription = "Stylus Pressure",
                                                            tint = WarmCream,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        AnnotationTool.ERASER -> {
                                            // Eraser Mode selection
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Eraser Mode",
                                                    color = WarmCream.copy(alpha = 0.85f),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    EraserModeChip(
                                                        label = "Stroke Eraser",
                                                        icon = Icons.Filled.Bolt,
                                                        isSelected = eraserMode == EraserMode.STROKE,
                                                        onClick = { onEraserModeSelected(EraserMode.STROKE) }
                                                    )
                                                    EraserModeChip(
                                                        label = "Precise Eraser",
                                                        icon = Icons.Filled.TrackChanges,
                                                        isSelected = eraserMode == EraserMode.PRECISE,
                                                        onClick = { onEraserModeSelected(EraserMode.PRECISE) }
                                                    )
                                                }
                                            }

                                            if (eraserMode == EraserMode.PRECISE) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Radius: ${String.format(Locale.US, "%.0f pt", strokeWidth)}",
                                                        color = WarmCream.copy(alpha = 0.8f),
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.width(80.dp)
                                                    )
                                                    Slider(
                                                        value = strokeWidth,
                                                        onValueChange = onStrokeWidthChanged,
                                                        valueRange = 4f..40f,
                                                        modifier = Modifier.weight(1f),
                                                        colors = SliderDefaults.colors(
                                                            thumbColor = OceanBlue,
                                                            activeTrackColor = OceanBlue,
                                                            inactiveTrackColor = DarkSurfaceVariant
                                                        )
                                                    )
                                                }
                                            }

                                            HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.6f))

                                            // Clear Page
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Clear All Annotations",
                                                    color = WarmCream.copy(alpha = 0.7f),
                                                    fontSize = 12.sp
                                                )
                                                Surface(
                                                    onClick = onClearPageClick,
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0xFFD32F2F).copy(alpha = 0.2f),
                                                    border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.5f)),
                                                    modifier = Modifier.stylusHoverEffect()
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(14.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Clear Page", color = WarmCream, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        AnnotationTool.LASER -> {
                                            Text(
                                                text = "Temporary laser pointer trail for presentations and highlighting text without drawing permanent marks.",
                                                color = WarmCream.copy(alpha = 0.75f),
                                                fontSize = 12.sp
                                            )
                                        }

                                        AnnotationTool.LASSO -> {
                                            Text(
                                                text = "Circle any handwritten strokes to select, move, scale, or delete them.",
                                                color = WarmCream.copy(alpha = 0.75f),
                                                fontSize = 12.sp
                                            )
                                        }

                                        else -> {}
                                    }
                                }
                            }
                        }

                        // ==========================================
                        // SINGLE-ROW SLEEK MAIN TOOLBAR CAPSULE
                        // ==========================================
                        Surface(
                            modifier = Modifier
                                .widthIn(max = 780.dp)
                                .shadow(16.dp, shape = RoundedCornerShape(24.dp))
                                .border(
                                    BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            shape = RoundedCornerShape(24.dp),
                            color = DarkSurface.copy(alpha = 0.96f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                RefinedToolbarButton(
                                    icon = Icons.Filled.Edit,
                                    label = "Pen",
                                    isSelected = currentTool == AnnotationTool.PEN,
                                    isPopupOpen = activePopupTool == AnnotationTool.PEN,
                                    activeColor = currentColor,
                                    onClick = { handleToolClick(AnnotationTool.PEN) }
                                )
                                RefinedToolbarButton(
                                    icon = Icons.Filled.Brush,
                                    label = "Fountain",
                                    isSelected = currentTool == AnnotationTool.FOUNTAIN,
                                    isPopupOpen = activePopupTool == AnnotationTool.FOUNTAIN,
                                    activeColor = currentColor,
                                    onClick = { handleToolClick(AnnotationTool.FOUNTAIN) }
                                )
                                RefinedToolbarButton(
                                    icon = Icons.Filled.AutoFixNormal,
                                    label = "Calligraphy",
                                    isSelected = currentTool == AnnotationTool.CALLIGRAPHY,
                                    isPopupOpen = activePopupTool == AnnotationTool.CALLIGRAPHY,
                                    activeColor = currentColor,
                                    onClick = { handleToolClick(AnnotationTool.CALLIGRAPHY) }
                                )
                                RefinedToolbarButton(
                                    icon = Icons.Filled.BorderColor,
                                    label = "Highlighter",
                                    isSelected = currentTool == AnnotationTool.HIGHLIGHTER,
                                    isPopupOpen = activePopupTool == AnnotationTool.HIGHLIGHTER,
                                    activeColor = currentColor,
                                    onClick = { handleToolClick(AnnotationTool.HIGHLIGHTER) }
                                )
                                RefinedToolbarButton(
                                    icon = Icons.Filled.FormatColorFill,
                                    label = "Box",
                                    isSelected = currentTool == AnnotationTool.RECT_HIGHLIGHT,
                                    isPopupOpen = activePopupTool == AnnotationTool.RECT_HIGHLIGHT,
                                    activeColor = currentColor,
                                    onClick = { handleToolClick(AnnotationTool.RECT_HIGHLIGHT) }
                                )
                                RefinedToolbarButton(
                                    icon = Icons.Filled.Bolt,
                                    label = "Laser",
                                    isSelected = currentTool == AnnotationTool.LASER,
                                    isPopupOpen = activePopupTool == AnnotationTool.LASER,
                                    activeColor = null,
                                    onClick = { handleToolClick(AnnotationTool.LASER) }
                                )
                                RefinedToolbarButton(
                                    icon = Icons.Filled.Delete,
                                    label = "Eraser",
                                    isSelected = currentTool == AnnotationTool.ERASER,
                                    isPopupOpen = activePopupTool == AnnotationTool.ERASER,
                                    activeColor = null,
                                    onClick = { handleToolClick(AnnotationTool.ERASER) }
                                )
                                RefinedToolbarButton(
                                    icon = Icons.Filled.HighlightAlt,
                                    label = "Lasso",
                                    isSelected = currentTool == AnnotationTool.LASSO,
                                    isPopupOpen = activePopupTool == AnnotationTool.LASSO,
                                    activeColor = null,
                                    onClick = { handleToolClick(AnnotationTool.LASSO) }
                                )

                                VerticalDivider(
                                    modifier = Modifier.height(24.dp).padding(horizontal = 2.dp),
                                    color = DarkSurfaceVariant
                                )

                                // Undo / Redo
                                IconButton(
                                    onClick = onUndo,
                                    enabled = canUndo,
                                    modifier = Modifier.size(34.dp).stylusHoverEffect()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Undo",
                                        tint = if (canUndo) WarmCream else Color.Gray.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onRedo,
                                    enabled = canRedo,
                                    modifier = Modifier.size(34.dp).stylusHoverEffect()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Redo,
                                        contentDescription = "Redo",
                                        tint = if (canRedo) WarmCream else Color.Gray.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                VerticalDivider(
                                    modifier = Modifier.height(24.dp).padding(horizontal = 2.dp),
                                    color = DarkSurfaceVariant
                                )

                                // Stylus-Only Palm Rejection toggle pill
                                Surface(
                                    onClick = onToggleStylusOnly,
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isStylusOnly) OceanBlue.copy(alpha = 0.22f) else DarkSurfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, if (isStylusOnly) OceanBlue else Color.White.copy(alpha = 0.12f)),
                                    modifier = Modifier.stylusHoverEffect()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Stylus Only Mode",
                                            tint = if (isStylusOnly) LightMint else WarmCream.copy(alpha = 0.5f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = if (isStylusOnly) "Stylus" else "Touch",
                                            color = if (isStylusOnly) LightMint else WarmCream.copy(alpha = 0.7f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Collapse to Bubble button
                                IconButton(
                                    onClick = onToggleCollapse,
                                    modifier = Modifier.size(34.dp).stylusHoverEffect()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Collapse to Bubble",
                                        tint = WarmCream,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Close / Done button
                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(DeepBlue.copy(alpha = 0.45f))
                                        .stylusHoverEffect()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Done / Close",
                                        tint = OceanBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RefinedToolbarButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isPopupOpen: Boolean,
    activeColor: Color? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .stylusHoverEffect(hoverScale = 1.15f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPopupOpen) DeepBlue.copy(alpha = 0.55f)
                else if (isSelected) DeepBlue.copy(alpha = 0.40f)
                else Color.Transparent
            )
            .border(
                width = if (isPopupOpen) 1.5.dp else if (isSelected) 1.dp else 0.dp,
                color = if (isPopupOpen) LightMint else if (isSelected) OceanBlue else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected || isPopupOpen) (if (isPopupOpen) LightMint else OceanBlue) else WarmCream,
                modifier = Modifier.size(19.dp)
            )
            if (isSelected && activeColor != null) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                )
            } else if (isPopupOpen) {
                Spacer(Modifier.height(2.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    tint = LightMint,
                    modifier = Modifier.size(7.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolbarToolButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .stylusHoverEffect(hoverScale = 1.15f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) OceanBlue.copy(alpha = 0.28f) else Color.Transparent
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) OceanBlue else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) OceanBlue else WarmCream,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EraserModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) OceanBlue.copy(alpha = 0.28f) else DarkSurfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 0.5.dp,
            color = if (isSelected) OceanBlue else Color.White.copy(alpha = 0.15f)
        ),
        modifier = Modifier.stylusHoverEffect()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) OceanBlue else WarmCream,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                color = if (isSelected) OceanBlue else WarmCream,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .stylusHoverEffect(hoverScale = 1.25f)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}
