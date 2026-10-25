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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.hypot
import kotlin.math.pow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.data.model.AnnotationPoint
import com.bookflow.reader.data.model.AnnotationStroke
import com.bookflow.reader.data.model.CanvasTemplate
import com.bookflow.reader.data.model.NoteTextBox
import com.bookflow.reader.ui.theme.*
import com.bookflow.reader.util.ShapeRecognizer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import com.bookflow.reader.util.stylusHoverEffect
import com.bookflow.reader.util.stylusHoverTracking
import kotlin.math.roundToInt

enum class ToolbarDock { BOTTOM, TOP, LEFT, RIGHT, FLOAT }

const val A3_WIDTH = 1200f
const val A3_HEIGHT = 1697f // Exact 1 : sqrt(2) A3 ratio
const val A3_PAGE_GAP = 50f // Gap in world units between sequential A3 pages in continuous flow
fun getPageOffsetY(pageIndex: Int): Float = pageIndex * (A3_HEIGHT + A3_PAGE_GAP)

private class NoteFrontSeg(val path: Path = Path(), var width: Float = 0f)

@Composable
fun InfiniteNotePad(
    strokes: List<AnnotationStroke>,
    canUndo: Boolean,
    canRedo: Boolean,
    canvasTemplate: CanvasTemplate = CanvasTemplate.DOT_GRID,
    isShapeModeEnabled: Boolean = false,
    noteTextBoxes: List<NoteTextBox> = emptyList(),
    showTemplateInsert: Boolean = false,
    onAddStroke: (AnnotationStroke) -> Unit,
    onEraseAt: (Float, Float) -> Unit,
    onEraseStrokeAt: (Float, Float, Float, EraserMode, Int) -> Unit = { x, y, _, _, _ -> onEraseAt(x, y) },
    onEraseGestureComplete: () -> Unit = {},
    onClear: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onExport: () -> Unit,
    onClose: () -> Unit,
    onCycleTemplate: () -> Unit = {},
    onToggleShapeMode: () -> Unit = {},
    onAddTextBox: (Float, Float) -> Unit = { _, _ -> },
    onAddTextBoxObj: ((NoteTextBox) -> Unit)? = null,
    onUpdateTextBox: (NoteTextBox) -> Unit = {},
    onDeleteTextBox: (String) -> Unit = {},
    onToggleTemplateInsert: () -> Unit = {},
    onInsertTemplate: (List<AnnotationStroke>) -> Unit = {},
    onMoveStrokes: (Set<Int>, Float, Float) -> Unit = { _, _, _ -> },
    onScaleStrokes: (Set<Int>, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    onDeleteStrokes: (Set<Int>) -> Unit = {},
    onStampsClick: () -> Unit = {},
    getImagePath: (Long) -> String? = { null },
    isGlobalNotebook: Boolean = false,
    noteTitle: String = if (isGlobalNotebook) "Universal Notebook" else "Infinite Notebook",
    initialFullScreen: Boolean = true,
    onToggleGlobalNotebook: (() -> Unit)? = null,
    onMarkRecallSelection: ((Bitmap) -> Unit)? = null,
    stylusOnly: Boolean = true
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val hapticContext = androidx.compose.ui.platform.LocalContext.current
    var isFullScreen by remember { mutableStateOf(initialFullScreen) }

    // Proportional, generous floating window dimensions (72% width, 82% height of screen)
    val defaultWidthPx = with(density) { (configuration.screenWidthDp.dp * 0.72f).toPx() }
    val defaultHeightPx = with(density) { (configuration.screenHeightDp.dp * 0.82f).toPx() }
    val defaultOffsetXPx = with(density) { (configuration.screenWidthDp.dp * 0.14f).toPx() }
    val defaultOffsetYPx = with(density) { 70.dp.toPx() } // Safely below notification panel

    var windowOffset by remember { mutableStateOf(Offset(defaultOffsetXPx, defaultOffsetYPx)) }
    var windowSize by remember { mutableStateOf(Offset(defaultWidthPx, defaultHeightPx)) }
    
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableStateOf(1f) }
    
    var currentTool by remember { mutableStateOf(AnnotationTool.PEN) }
    var currentColor by remember { mutableStateOf(Color(0xFF2D2D2D)) }
    var currentStrokeWidth by remember { mutableStateOf(4f) }
    var isTextMode by remember { mutableStateOf(false) }
    var isLassoMode by remember { mutableStateOf(false) }
    var lassoTarget by remember { mutableStateOf(0) } // 0 = ALL, 1 = STROKES, 2 = IMAGES
    var lassoDragOffset by remember { mutableStateOf(Offset.Zero) }
    var lassoScaleFactor by remember { mutableStateOf(1f) }
    var eraserMode by remember { mutableStateOf(EraserMode.PRECISE) }
    var eraserRadius by remember { mutableStateOf(16f) }
    var activeEraserScreenPos by remember { mutableStateOf<Offset?>(null) }
    var isToolbarCollapsed by remember { mutableStateOf(false) }
    var activeInfinitePopup by remember { mutableStateOf<String?>(null) }
    var noteBackgroundColor by remember { mutableStateOf(Color(0xFFFAF8F5)) }
    var localCanvasTemplate by remember(canvasTemplate) { mutableStateOf(canvasTemplate) }

    // Multi-Page A3 Document State
    var currentPage by remember { mutableStateOf(0) }
    var totalPages by remember { mutableStateOf(1) }
    LaunchedEffect(strokes, noteTextBoxes) {
        val maxStrokePage = strokes.maxOfOrNull { it.pageIndex } ?: 0
        val maxTextPage = noteTextBoxes.maxOfOrNull { it.pageIndex } ?: 0
        val computed = maxOf(1, maxOf(maxStrokePage, maxTextPage) + 1)
        if (computed > totalPages) {
            totalPages = computed
        }
    }

    // Performance Isolation: Group completed strokes & text boxes by page so writing on one page never invalidates other pages
    val strokesByPage = remember(strokes) {
        strokes.groupBy { it.pageIndex }
    }
    val noteTextBoxesByPage = remember(noteTextBoxes) {
        noteTextBoxes.groupBy { it.pageIndex }
    }

    fun isPointInTextBox(worldX: Float, worldY: Float, tb: NoteTextBox, tolerance: Float = 0f): Boolean {
        val pageTop = getPageOffsetY(tb.pageIndex)
        val localWorldY = worldY - pageTop
        val dx = worldX - tb.x
        val dy = localWorldY - tb.y
        val rad = Math.toRadians(tb.rotation.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()
        val localX = dx * cos + dy * sin
        val localY = -dx * sin + dy * cos
        return localX in -tolerance..(tb.width + tolerance) && localY in -tolerance..(tb.height + tolerance)
    }

    // Movable & Dockable Toolbar State
    var toolbarDock by remember { mutableStateOf(ToolbarDock.BOTTOM) }
    var toolbarOffset by remember { mutableStateOf(Offset.Zero) }
    var isDraggingToolbar by remember { mutableStateOf(false) }

    // Rich Text & Typography State
    var activeTextFontFamily by remember { mutableStateOf("DEFAULT") }
    var activeTextFontSize by remember { mutableStateOf(18f) }
    var activeTextBold by remember { mutableStateOf(false) }
    var activeTextItalic by remember { mutableStateOf(false) }
    var activeTextUnderline by remember { mutableStateOf(false) }
    var activeTextAlign by remember { mutableStateOf("LEFT") }
    var selectedTextBoxId by remember { mutableStateOf<String?>(null) }
    
    // High-performance live inking state (O(1) incremental path rendering, 0ms latency)
    val activeDrawPoints = remember { mutableListOf<AnnotationPoint>() }
    val liveDrawMainPath = remember { Path() }
    val liveTipPath = remember { Path() }
    val liveFrontSegs = remember { List(10) { NoteFrontSeg() } }
    var liveFrontCount by remember { mutableStateOf(0) }
    var liveBodyStarted by remember { mutableStateOf(false) }
    var liveCurrentPressure by remember { mutableStateOf(1.0f) }
    var liveRevision by remember { mutableStateOf(0) }
    
    data class LaserPoint(val x: Float, val y: Float, val timestamp: Long = System.currentTimeMillis())
    val laserPoints = remember { mutableStateListOf<LaserPoint>() }
    
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            if (laserPoints.isNotEmpty()) {
                laserPoints.removeAll { now - it.timestamp > 1500 }
            }
            kotlinx.coroutines.delay(32) // ~30fps cleanup
        }
    }
    var isActiveDrawing by remember { mutableStateOf(false) }
    var hoverScreenOffset by remember { mutableStateOf<Offset?>(null) }
    var activeDrawColor by remember { mutableStateOf(0L) }
    var activeDrawWidth by remember { mutableStateOf(4f) }
    var activeDrawHighlighter by remember { mutableStateOf(false) }
    var activeDrawPenType by remember { mutableStateOf(0) }
    
    // Lasso selection state
    var lassoPath by remember { mutableStateOf<List<AnnotationPoint>>(emptyList()) }
    var selectedStrokeIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectionBounds by remember { mutableStateOf<Offset?>(null) } // min corner in world coords
    var selectionSize by remember { mutableStateOf<Offset?>(null) } // width/height in world coords
    var isDraggingSelection by remember { mutableStateOf(false) }
    var dragStartWorld by remember { mutableStateOf(Offset.Zero) }

    val imageCache = remember { mutableStateMapOf<Long, ImageBitmap>() }

    LaunchedEffect(strokes) {
        withContext(Dispatchers.IO) {
            strokes.filter { it.penType == 2 }.forEach { stroke ->
                if (!imageCache.containsKey(stroke.color)) {
                    val path = getImagePath(stroke.color)
                    if (path != null) {
                        try {
                            val bitmap = BitmapFactory.decodeFile(path)
                            if (bitmap != null) {
                                val imageBitmap = bitmap.asImageBitmap()
                                imageCache[stroke.color] = imageBitmap
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }
        }
    }

    // Pre-built path cache for completed strokes (pre-compiled once in world coords)
    val strokeCache = remember { mutableMapOf<AnnotationStroke, CachedNoteStroke>() }
    LaunchedEffect(strokes) {
        val currentSet = strokes.toSet()
        strokeCache.keys.retainAll(currentSet)
    }

    Box(modifier = Modifier.fillMaxSize()) { 
        val cardModifier = if (isFullScreen) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .offset { IntOffset(windowOffset.x.roundToInt(), windowOffset.y.roundToInt()) }
                .size(
                    width = with(density) { windowSize.x.toDp() },
                    height = with(density) { windowSize.y.toDp() }
                )
        }
        val cardShape = if (isFullScreen) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp)
        val cardElevation = if (isFullScreen) 0.dp else 12.dp

        Card(
            modifier = cardModifier,
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = noteBackgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
            border = if (!isFullScreen) BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)) else null
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isFullScreen) {
                    // Sleek, Clean Top Bar for Minimized (Windowed) Mode
                    // Contains ONLY: Name (with draggable handle), Full Screen button, and Close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(DarkSurface.copy(alpha = 0.96f))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Draggable Title Area (drag gestures isolated here so buttons always work)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val minOffsetY = with(density) { 56.dp.toPx() } // Can never slide under notification panel
                                        val minOffsetX = with(density) { 0.dp.toPx() }
                                        val maxOffsetX = with(density) { (configuration.screenWidthDp.dp - 120.dp).toPx() }
                                        val maxOffsetY = with(density) { (configuration.screenHeightDp.dp - 120.dp).toPx() }
                                        windowOffset = Offset(
                                            x = (windowOffset.x + dragAmount.x).coerceIn(minOffsetX, maxOffsetX),
                                            y = (windowOffset.y + dragAmount.y).coerceIn(minOffsetY, maxOffsetY)
                                        )
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 24.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(WarmCream.copy(alpha = 0.35f))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = noteTitle,
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                color = WarmCream,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Action Buttons: ONLY Full Screen & Close (Solid, instantaneous clicks, no hover scaling)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .clickable {
                                        isFullScreen = true
                                        try { com.bookflow.reader.util.HapticHelper.click(hapticContext) } catch (_: Exception) {}
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Fullscreen,
                                    contentDescription = "Full Screen",
                                    tint = WarmCream,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .clickable {
                                        onClose()
                                        try { com.bookflow.reader.util.HapticHelper.click(hapticContext) } catch (_: Exception) {}
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close",
                                    tint = WarmCream,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Fullscreen Header Bar with statusBarsPadding so it sits safely BELOW the notification panel
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface.copy(alpha = 0.96f))
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = noteTitle,
                                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                color = WarmCream,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Action Buttons: ONLY Windowed toggle & Close (Solid, instantaneous clicks, no hover scaling)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .clickable {
                                            isFullScreen = false
                                            try { com.bookflow.reader.util.HapticHelper.click(hapticContext) } catch (_: Exception) {}
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FullscreenExit,
                                        contentDescription = "Windowed Mode",
                                        tint = WarmCream,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .clickable {
                                            onClose()
                                            try { com.bookflow.reader.util.HapticHelper.click(hapticContext) } catch (_: Exception) {}
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close",
                                        tint = WarmCream,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Canvas Area with Auto-Fit A3 Constraints
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val canvasWidth = constraints.maxWidth.toFloat()
                    val canvasHeight = constraints.maxHeight.toFloat()

                    val isLandscape = canvasWidth > canvasHeight

                    val applyOrientationFit = {
                        if (canvasWidth > 50f && canvasHeight > 50f) {
                            val pageTopWorldY = getPageOffsetY(currentPage)
                            if (canvasWidth > canvasHeight) {
                                // Horizontal orientation (Landscape): auto fill whole screen width by zooming enough
                                val fitScale = (canvasWidth / A3_WIDTH).coerceIn(0.2f, 5f)
                                scale = fitScale
                                canvasOffset = Offset(0f, -pageTopWorldY * fitScale)
                            } else {
                                // Vertical orientation (Portrait): come back to fill cleanly vertically & centered
                                val fitScale = minOf(canvasWidth / A3_WIDTH, canvasHeight / A3_HEIGHT).coerceIn(0.2f, 5f)
                                scale = fitScale
                                canvasOffset = Offset(
                                    (canvasWidth - A3_WIDTH * fitScale) / 2f,
                                    -pageTopWorldY * fitScale + (canvasHeight - A3_HEIGHT * fitScale) / 2f
                                )
                            }
                        }
                    }

                    var lastIsLandscape by remember { mutableStateOf<Boolean?>(null) }
                    LaunchedEffect(isLandscape, canvasWidth, canvasHeight) {
                        if (canvasWidth > 50f && canvasHeight > 50f) {
                            if (lastIsLandscape != isLandscape) {
                                lastIsLandscape = isLandscape
                                applyOrientationFit()
                            }
                        }
                    }

                    // Continuous Flow: Auto-track currentPage to whichever page is in the viewport center as user scrolls
                    LaunchedEffect(canvasOffset.y, scale, totalPages, canvasHeight) {
                        if (canvasHeight > 50f && scale > 0.05f) {
                            val viewportCenterWorldY = (-canvasOffset.y + canvasHeight / 2f) / scale
                            val pageSpan = A3_HEIGHT + A3_PAGE_GAP
                            val visiblePage = (viewportCenterWorldY / pageSpan).toInt().coerceIn(0, totalPages - 1)
                            if (visiblePage != currentPage) {
                                currentPage = visiblePage
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(stylusOnly, totalPages) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    do {
                                        val event = awaitPointerEvent()
                                        val activePointers = event.changes.filter { it.pressed }
                                        val hasStylus = activePointers.any {
                                            it.type == PointerType.Stylus || it.type == PointerType.Eraser
                                        }
                                        if (hasStylus) {
                                            // Stylus writing: reject palm touches from transforming canvas
                                            event.changes.forEach { change ->
                                                if (change.type == PointerType.Touch && change.positionChanged()) {
                                                    change.consume()
                                                }
                                            }
                                        } else {
                                            // Finger touch gestures: 2-finger zoom/pan or 1-finger vertical flow scroll when stylusOnly is on
                                            val touchPointers = activePointers.filter {
                                                it.type == PointerType.Touch || it.type == PointerType.Mouse
                                            }
                                            if (touchPointers.size >= 2) {
                                                val zoom = event.calculateZoom()
                                                val pan = event.calculatePan()
                                                scale = (scale * zoom).coerceIn(0.2f, 5f)
                                                canvasOffset += pan
                                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                                            } else if (touchPointers.size == 1 && stylusOnly && !hasStylus) {
                                                val pan = event.calculatePan()
                                                canvasOffset += pan
                                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                    ) {
                        // LAYER 1: Base Canvas (Desk Tabletop, Continuous A3 Paper Sheets, Templates, Completed Strokes)
                        // Performance Isolation: Viewport culling skips non-visible pages, and strokes are grouped by page
                        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer()) {
                            // 1. Desk Background
                            drawRect(Color(0xFF181A1E), size = size)

                            val isDarkBg = noteBackgroundColor.luminance() < 0.5f
                            val dotColor = if (isDarkBg) Color.White.copy(alpha = 0.22f) else Color.LightGray.copy(alpha = 0.5f)
                            val lineGridColor = if (isDarkBg) Color.White.copy(alpha = 0.16f) else Color.LightGray.copy(alpha = 0.25f)
                            val ruledLineColor = if (isDarkBg) Color.White.copy(alpha = 0.18f) else Color.LightGray.copy(alpha = 0.3f)
                            val redMarginColor = if (isDarkBg) Color(0xFFFF5252).copy(alpha = 0.35f) else Color.Red.copy(alpha = 0.2f)
                            val redCueColor = if (isDarkBg) Color(0xFFFF5252).copy(alpha = 0.4f) else Color.Red.copy(alpha = 0.3f)

                            // 2. Render all A3 Pages in continuous vertical flow
                            for (p in 0 until totalPages) {
                                val worldPageTop = getPageOffsetY(p)
                                val pX = canvasOffset.x
                                val pY = canvasOffset.y + worldPageTop * scale
                                val pW = A3_WIDTH * scale
                                val pH = A3_HEIGHT * scale

                                // Viewport Culling: Skip pages completely outside visible viewport
                                if (pY + pH < -100f || pY > size.height + 100f) continue

                                // 3. Realistic Paper Elevation Shadow
                                drawRoundRect(
                                    color = Color.Black.copy(alpha = 0.38f),
                                    topLeft = Offset(pX + 8f, pY + 12f),
                                    size = Size(pW, pH),
                                    cornerRadius = CornerRadius(8f, 8f)
                                )
                                drawRoundRect(
                                    color = Color.Black.copy(alpha = 0.16f),
                                    topLeft = Offset(pX + 2f, pY + 4f),
                                    size = Size(pW, pH),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )

                                // 4. A3 Paper Sheet Body
                                drawRoundRect(
                                    color = noteBackgroundColor,
                                    topLeft = Offset(pX, pY),
                                    size = Size(pW, pH),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )

                                // 5. Crisp Paper Border
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.22f),
                                    topLeft = Offset(pX, pY),
                                    size = Size(pW, pH),
                                    cornerRadius = CornerRadius(4f, 4f),
                                    style = Stroke(width = 1.2f)
                                )

                                // 6. Page Watermark Indicator
                                drawContext.canvas.nativeCanvas.drawText(
                                    "Page ${p + 1}",
                                    pX + pW - 24f * scale,
                                    pY + 36f * scale,
                                    android.graphics.Paint().apply {
                                        color = if (isDarkBg) 0x66FFFFFF else 0x442D2D2D
                                        textSize = (13f * scale).coerceAtLeast(10f)
                                        isFakeBoldText = true
                                        textAlign = android.graphics.Paint.Align.RIGHT
                                    }
                                )

                                // 7. Templates (Strictly constrained within page p boundaries)
                                when (localCanvasTemplate) {
                                    CanvasTemplate.DOT_GRID -> {
                                        val dotSpacing = 40f * scale
                                        if (dotSpacing > 10f) {
                                            var x = pX + dotSpacing
                                            while (x < pX + pW) {
                                                var y = pY + dotSpacing
                                                while (y < pY + pH) {
                                                    drawCircle(dotColor, radius = 2f, center = Offset(x, y))
                                                    y += dotSpacing
                                                }
                                                x += dotSpacing
                                            }
                                        }
                                    }
                                    CanvasTemplate.RULED_LINES -> {
                                        val lineSpacing = 38f * scale
                                        if (lineSpacing > 8f) {
                                            var y = pY + lineSpacing * 1.5f
                                            while (y < pY + pH - 15f) {
                                                drawLine(ruledLineColor, Offset(pX, y), Offset(pX + pW, y), strokeWidth = 1f)
                                                y += lineSpacing
                                            }
                                            val marginX = pX + 80f * scale
                                            if (marginX > pX && marginX < pX + pW) {
                                                drawLine(redMarginColor, Offset(marginX, pY), Offset(marginX, pY + pH), strokeWidth = 1.5f)
                                            }
                                        }
                                    }
                                    CanvasTemplate.GRAPH_PAPER -> {
                                        val gridSpacing = 28f * scale
                                        if (gridSpacing > 8f) {
                                            var x = pX + gridSpacing
                                            while (x < pX + pW) {
                                                drawLine(lineGridColor, Offset(x, pY), Offset(x, pY + pH), strokeWidth = 0.5f)
                                                x += gridSpacing
                                            }
                                            var y = pY + gridSpacing
                                            while (y < pY + pH) {
                                                drawLine(lineGridColor, Offset(pX, y), Offset(pX + pW, y), strokeWidth = 0.5f)
                                                y += gridSpacing
                                            }
                                        }
                                    }
                                    CanvasTemplate.BLANK -> { /* Clean A3 paper */ }
                                    CanvasTemplate.CORNELL -> {
                                        val lineSpacing = 38f * scale
                                        if (lineSpacing > 8f) {
                                            val summaryTop = pY + pH * 0.78f
                                            val cueRight = pX + pW * 0.30f
                                            var y = pY + lineSpacing * 1.5f
                                            while (y < summaryTop) {
                                                drawLine(lineGridColor, Offset(cueRight, y), Offset(pX + pW, y), strokeWidth = 0.5f)
                                                y += lineSpacing
                                            }
                                            drawLine(redCueColor, Offset(cueRight, pY), Offset(cueRight, summaryTop), strokeWidth = 2f)
                                            drawLine(redCueColor, Offset(pX, summaryTop), Offset(pX + pW, summaryTop), strokeWidth = 2f)
                                        }
                                    }
                                }

                                // 8. Page-Isolated Strokes Rendering (O(1) memory, Skia path cached)
                                val pageStrokes = strokesByPage[p] ?: emptyList()
                                pageStrokes.forEachIndexed { pIdx, stroke ->
                                    if (stroke.points.isNotEmpty()) {
                                        val strokeGlobalIdx = strokes.indexOf(stroke)
                                        val isSelected = strokeGlobalIdx != -1 && strokeGlobalIdx in selectedStrokeIndices
                                        val dragDx = if (isSelected) lassoDragOffset.x * scale else 0f
                                        val dragDy = if (isSelected) lassoDragOffset.y * scale else 0f
                                        val lScale = if (isSelected) lassoScaleFactor else 1f

                                        val scaleOriginX = if (selectionBounds != null && selectionSize != null) selectionBounds!!.x + selectionSize!!.x / 2f else 0f
                                        val scaleOriginY = if (selectionBounds != null && selectionSize != null) selectionBounds!!.y + selectionSize!!.y / 2f else 0f

                                        if (stroke.penType == 2 && stroke.points.size >= 2) {
                                            val origWorldX = stroke.points[0].x
                                            val origWorldY = stroke.points[0].y
                                            val newWorldX = scaleOriginX + (origWorldX - scaleOriginX) * lScale
                                            val newWorldY = scaleOriginY + (origWorldY - scaleOriginY) * lScale
                                            val posX = newWorldX * scale + canvasOffset.x + dragDx
                                            val posY = (newWorldY + worldPageTop) * scale + canvasOffset.y + dragDy
                                            val imgW = stroke.points[1].x * scale * lScale
                                            val imgH = stroke.points[1].y * scale * lScale
                                            val imageBitmap = imageCache[stroke.color]
                                            if (imageBitmap != null) {
                                                drawImage(
                                                    image = imageBitmap,
                                                    dstOffset = IntOffset(posX.toInt(), posY.toInt()),
                                                    dstSize = IntSize(imgW.toInt(), imgH.toInt())
                                                )
                                            } else {
                                                drawRect(color = Color(0xFF1C2D4A).copy(alpha = 0.2f), topLeft = Offset(posX, posY), size = Size(imgW, imgH))
                                                drawRect(color = Color(0xFF3498DB).copy(alpha = 0.6f), topLeft = Offset(posX, posY), size = Size(imgW, imgH), style = Stroke(width = 2f))
                                            }
                                        } else {
                                            val render = strokeCache.getOrPut(stroke) { buildCachedNoteStroke(stroke) }
                                            withTransform({
                                                translate(canvasOffset.x + dragDx, pY + dragDy)
                                                scale(scale, scale, Offset.Zero)
                                                if (isSelected) {
                                                    scale(lScale, lScale, Offset(scaleOriginX, scaleOriginY))
                                                }
                                            }) {
                                                render.draw(this, isSelected)
                                            }
                                        }
                                    }
                                }
                            }

                            // Draw selection bounding box
                            val sb = selectionBounds
                            val ss = selectionSize
                            if (sb != null && ss != null && selectedStrokeIndices.isNotEmpty()) {
                                val scaleOriginX = sb.x + ss.x / 2f
                                val scaleOriginY = sb.y + ss.y / 2f
                                
                                val newX = scaleOriginX + (sb.x - scaleOriginX) * lassoScaleFactor
                                val newY = scaleOriginY + (sb.y - scaleOriginY) * lassoScaleFactor
                                val newW = ss.x * lassoScaleFactor
                                val newH = ss.y * lassoScaleFactor
                                
                                val rx = (newX + lassoDragOffset.x) * scale + canvasOffset.x
                                val ry = (newY + lassoDragOffset.y) * scale + canvasOffset.y
                                val rw = newW * scale
                                val rh = newH * scale
                                drawRect(
                                    color = Color(0x143498DB), // ~8% alpha
                                    topLeft = Offset(rx, ry),
                                    size = Size(rw, rh)
                                )
                                drawRect(
                                    color = Color(0xFF3498DB),
                                    topLeft = Offset(rx, ry),
                                    size = Size(rw, rh),
                                    style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f)))
                                )
                                // Corner handles
                                val handleR = 6f
                                listOf(Offset(rx, ry), Offset(rx + rw, ry), Offset(rx, ry + rh), Offset(rx + rw, ry + rh)).forEach { corner ->
                                    drawCircle(Color.White, radius = handleR + 1f, center = corner)
                                    drawCircle(Color(0xFF3498DB), radius = handleR, center = corner)
                                }
                            }
                            
                            // Highlight selected strokes (multi-page continuous flow)
                            selectedStrokeIndices.forEach { idx ->
                                if (idx < strokes.size) {
                                    val s = strokes[idx]
                                    val pageTop = getPageOffsetY(s.pageIndex)
                                    s.points.forEach { p ->
                                        drawCircle(
                                            Color(0xFF3498DB).copy(alpha = 0.15f),
                                            radius = 6f * scale,
                                            center = Offset(p.x * scale + canvasOffset.x, (p.y + pageTop) * scale + canvasOffset.y)
                                        )
                                    }
                                }
                            }
                        }

                        // LAYER 2: Live Inking & Interactive Overlays Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer()
                                .stylusHoverTracking { hoverScreenOffset = it }
                                .pointerInput(canvasOffset, scale, currentTool, currentColor, isTextMode, isShapeModeEnabled, isLassoMode, stylusOnly) {
                                    awaitEachGesture {
                                        val downEvent = awaitFirstDown(requireUnconsumed = false)
                                        val initialChanges = currentEvent.changes
                                        if (initialChanges.size == 2 && initialChanges.all { it.type == androidx.compose.ui.input.pointer.PointerType.Touch }) {
                                            val startTime = System.currentTimeMillis()
                                            val p1 = initialChanges[0].position
                                            val p2 = initialChanges[1].position
                                            var maxMove = 0f
                                            var released = false
                                            while (true) {
                                                val evt = awaitPointerEvent()
                                                val c1 = evt.changes.firstOrNull { it.id == initialChanges[0].id }
                                                val c2 = evt.changes.firstOrNull { it.id == initialChanges[1].id }
                                                if (c1 != null) maxMove = maxOf(maxMove, hypot(c1.position.x - p1.x, c1.position.y - p1.y))
                                                if (c2 != null) maxMove = maxOf(maxMove, hypot(c2.position.x - p2.x, c2.position.y - p2.y))
                                                if (evt.changes.all { !it.pressed }) {
                                                    released = true
                                                    evt.changes.forEach { it.consume() }
                                                    break
                                                }
                                                if (System.currentTimeMillis() - startTime > 300L || maxMove > 35f) break
                                            }
                                            if (released && maxMove <= 35f && System.currentTimeMillis() - startTime <= 300L) {
                                                try { com.bookflow.reader.util.HapticHelper.click(hapticContext) } catch (_: Exception) {}
                                                onUndo()
                                                return@awaitEachGesture
                                            }
                                        }

                                        // Strict Stylus Rejection: Writing & erasing work ONLY with stylus (not with fingers or others)
                                        val isStylus = downEvent.type == androidx.compose.ui.input.pointer.PointerType.Stylus || 
                                                       downEvent.type == androidx.compose.ui.input.pointer.PointerType.Eraser
                                        if (!isTextMode && stylusOnly && !isStylus) {
                                            return@awaitEachGesture
                                        }

                                        downEvent.consume()
                                        if (!isToolbarCollapsed) {
                                            isToolbarCollapsed = true
                                        }
                                        
                                        val wX = (downEvent.position.x - canvasOffset.x) / scale
                                        val wY = (downEvent.position.y - canvasOffset.y) / scale
                                        
                                        val pageSpan = A3_HEIGHT + A3_PAGE_GAP
                                        val tapPageIndex = (wY / pageSpan).toInt().coerceIn(0, totalPages - 1)
                                        val localWY = wY - getPageOffsetY(tapPageIndex)
                                        
                                        if (isTextMode) {
                                            // Check if tap hit an existing text box first
                                            val hitBox = noteTextBoxes.lastOrNull { tb -> isPointInTextBox(wX, wY, tb, 10f) }
                                            if (hitBox != null) {
                                                selectedTextBoxId = hitBox.id
                                                activeInfinitePopup = "TEXT"
                                                activeTextFontFamily = hitBox.fontFamily
                                                activeTextFontSize = hitBox.fontSize
                                                activeTextBold = hitBox.isBold
                                                activeTextItalic = hitBox.isItalic
                                                activeTextUnderline = hitBox.isUnderline
                                                activeTextAlign = hitBox.textAlign
                                                currentColor = Color(hitBox.color.toInt())
                                                isTextMode = false
                                                return@awaitEachGesture
                                            }

                                            // Text mode: add a styled text box at tap position in continuous flow
                                            val newBox = NoteTextBox(
                                                pageIndex = tapPageIndex,
                                                x = wX,
                                                y = localWY,
                                                fontSize = activeTextFontSize,
                                                fontFamily = activeTextFontFamily,
                                                isBold = activeTextBold,
                                                isItalic = activeTextItalic,
                                                isUnderline = activeTextUnderline,
                                                textAlign = activeTextAlign,
                                                color = currentColor.toArgb().toLong()
                                            )
                                            if (onAddTextBoxObj != null) {
                                                onAddTextBoxObj(newBox)
                                            } else {
                                                onAddTextBox(wX, localWY)
                                            }
                                            selectedTextBoxId = newBox.id
                                            activeInfinitePopup = "TEXT"
                                            isTextMode = false
                                            return@awaitEachGesture
                                        }

                                        // When not in text mode or lasso mode, check if tapped an existing text box or blank canvas
                                        if (!isLassoMode && currentTool != AnnotationTool.ERASER) {
                                            val hitBox = noteTextBoxes.lastOrNull { tb -> isPointInTextBox(wX, wY, tb, 10f) }
                                            if (hitBox != null) {
                                                selectedTextBoxId = hitBox.id
                                                activeInfinitePopup = "TEXT"
                                                activeTextFontFamily = hitBox.fontFamily
                                                activeTextFontSize = hitBox.fontSize
                                                activeTextBold = hitBox.isBold
                                                activeTextItalic = hitBox.isItalic
                                                activeTextUnderline = hitBox.isUnderline
                                                activeTextAlign = hitBox.textAlign
                                                currentColor = Color(hitBox.color.toInt())
                                                return@awaitEachGesture
                                            } else if (selectedTextBoxId != null) {
                                                // Tapped blank canvas outside any text box - clean up blank boxes and deselect
                                                val sel = noteTextBoxes.find { it.id == selectedTextBoxId }
                                                if (sel != null && sel.text.trim().isEmpty()) {
                                                    onDeleteTextBox(sel.id)
                                                }
                                                selectedTextBoxId = null
                                            }
                                        }
                                        
                                        if (isLassoMode) {
                                            // Check if tapping inside existing selection to drag
                                            val sb = selectionBounds
                                            val ss = selectionSize
                                            if (sb != null && ss != null && selectedStrokeIndices.isNotEmpty()) {
                                                val inSel = wX >= sb.x && wX <= sb.x + ss.x && wY >= sb.y && wY <= sb.y + ss.y
                                                
                                                // Check for corner touches (for resize)
                                                val cornerThreshold = 20f / scale
                                                val isBr = kotlin.math.hypot(wX - (sb.x + ss.x), wY - (sb.y + ss.y)) <= cornerThreshold
                                                val isBl = kotlin.math.hypot(wX - sb.x, wY - (sb.y + ss.y)) <= cornerThreshold
                                                val isTr = kotlin.math.hypot(wX - (sb.x + ss.x), wY - sb.y) <= cornerThreshold
                                                val isTl = kotlin.math.hypot(wX - sb.x, wY - sb.y) <= cornerThreshold
                                                val isResize = isBr || isBl || isTr || isTl
                                                
                                                if (isResize) {
                                                    val startDrag = Offset(wX, wY)
                                                    val originX = sb.x + ss.x / 2f
                                                    val originY = sb.y + ss.y / 2f
                                                    val initialDist = kotlin.math.hypot(startDrag.x - originX, startDrag.y - originY)
                                                    
                                                    var dragEvt = awaitPointerEvent()
                                                    while (dragEvt.changes.any { it.pressed }) {
                                                        val ch = dragEvt.changes.firstOrNull { it.id == downEvent.id }
                                                        if (ch != null && ch.pressed) {
                                                            val curX = (ch.position.x - canvasOffset.x) / scale
                                                            val curY = (ch.position.y - canvasOffset.y) / scale
                                                            val curDist = kotlin.math.hypot(curX - originX, curY - originY)
                                                            lassoScaleFactor = if (initialDist > 0f) curDist / initialDist else 1f
                                                            ch.consume()
                                                        }
                                                        dragEvt = awaitPointerEvent()
                                                    }
                                                    
                                                    val finalScale = lassoScaleFactor
                                                    lassoScaleFactor = 1f
                                                    if (kotlin.math.abs(finalScale - 1f) > 0.05f) {
                                                        onScaleStrokes(selectedStrokeIndices, finalScale, originX, originY)
                                                        selectionBounds = Offset(originX + (sb.x - originX) * finalScale, originY + (sb.y - originY) * finalScale)
                                                        selectionSize = Offset(ss.x * finalScale, ss.y * finalScale)
                                                    }
                                                    return@awaitEachGesture
                                                } else if (inSel) {
                                                    val startDrag = Offset(wX, wY)
                                                    var lastWX = wX; var lastWY = wY
                                                    var dragEvt = awaitPointerEvent()
                                                    while (dragEvt.changes.any { it.pressed }) {
                                                        val ch = dragEvt.changes.firstOrNull { it.id == downEvent.id }
                                                        if (ch != null && ch.pressed) {
                                                            lastWX = (ch.position.x - canvasOffset.x) / scale
                                                            lastWY = (ch.position.y - canvasOffset.y) / scale
                                                            lassoDragOffset = Offset(lastWX - startDrag.x, lastWY - startDrag.y)
                                                            ch.consume()
                                                        }
                                                        dragEvt = awaitPointerEvent()
                                                    }
                                                    val finalDx = lastWX - startDrag.x
                                                    val finalDy = lastWY - startDrag.y
                                                    lassoDragOffset = Offset.Zero
                                                    if (kotlin.math.abs(finalDx) > 1f || kotlin.math.abs(finalDy) > 1f) {
                                                        onMoveStrokes(selectedStrokeIndices, finalDx, finalDy)
                                                        selectionBounds = Offset(sb.x + finalDx, sb.y + finalDy)
                                                    }
                                                    return@awaitEachGesture
                                                }
                                            }
                                            // Draw new lasso selection path
                                            val lPoints = mutableListOf<AnnotationPoint>()
                                            lPoints.add(AnnotationPoint(wX, wY))
                                            lassoPath = lPoints.toList()
                                            var dragEvt = awaitPointerEvent()
                                            while (dragEvt.changes.any { it.pressed }) {
                                                val ch = dragEvt.changes.firstOrNull { it.id == downEvent.id }
                                                if (ch != null && ch.pressed) {
                                                    ch.historical.forEach { hist ->
                                                        lPoints.add(AnnotationPoint((hist.position.x - canvasOffset.x) / scale, (hist.position.y - canvasOffset.y) / scale))
                                                    }
                                                    lPoints.add(AnnotationPoint((ch.position.x - canvasOffset.x) / scale, (ch.position.y - canvasOffset.y) / scale))
                                                    lassoPath = lPoints.toList()
                                                    ch.consume()
                                                }
                                                dragEvt = awaitPointerEvent()
                                            }
                                            // Find strokes inside lasso
                                            if (lPoints.size > 5) {
                                                val sel = mutableSetOf<Int>()
                                                strokes.forEachIndexed { idx, stroke ->
                                                    val isImage = stroke.penType == 2
                                                    val canSelect = when(lassoTarget) {
                                                        1 -> !isImage
                                                        2 -> isImage
                                                        else -> true
                                                    }
                                                    if (canSelect && stroke.points.isNotEmpty()) {
                                                        if (stroke.penType == 2 && stroke.points.size >= 2) {
                                                            // Image stroke: check if center or corners are inside lasso
                                                            val pageTop = getPageOffsetY(stroke.pageIndex)
                                                            val x = stroke.points[0].x
                                                            val y = stroke.points[0].y + pageTop
                                                            val w = stroke.points[1].x
                                                            val h = stroke.points[1].y
                                                            val cx = x + w / 2
                                                            val cy = y + h / 2
                                                            val isInside = pointInPolygon(cx, cy, lPoints) || 
                                                                           pointInPolygon(x, y, lPoints) ||
                                                                           pointInPolygon(x + w, y + h, lPoints)
                                                            if (isInside) sel.add(idx)
                                                        } else {
                                                            // Normal stroke: Select if ANY point of the stroke is inside the lasso
                                                            val pageTop = getPageOffsetY(stroke.pageIndex)
                                                            val isInside = stroke.points.any { p -> pointInPolygon(p.x, p.y + pageTop, lPoints) }
                                                            if (isInside) sel.add(idx)
                                                        }
                                                    }
                                                }
                                                selectedStrokeIndices = sel
                                                if (sel.isNotEmpty()) {
                                                    var sMinX = Float.MAX_VALUE; var sMinY = Float.MAX_VALUE
                                                    var sMaxX = -Float.MAX_VALUE; var sMaxY = -Float.MAX_VALUE
                                                    sel.forEach { i -> 
                                                        val stroke = strokes[i]
                                                        val pageTop = getPageOffsetY(stroke.pageIndex)
                                                        if (stroke.penType == 2 && stroke.points.size >= 2) {
                                                            val x = stroke.points[0].x
                                                            val y = stroke.points[0].y + pageTop
                                                            val w = stroke.points[1].x
                                                            val h = stroke.points[1].y
                                                            if (x < sMinX) sMinX = x; if (y < sMinY) sMinY = y
                                                            if (x + w > sMaxX) sMaxX = x + w; if (y + h > sMaxY) sMaxY = y + h
                                                        } else {
                                                            stroke.points.forEach { p ->
                                                                val py = p.y + pageTop
                                                                if (p.x < sMinX) sMinX = p.x; if (py < sMinY) sMinY = py
                                                                if (p.x > sMaxX) sMaxX = p.x; if (py > sMaxY) sMaxY = py
                                                            }
                                                        }
                                                    }
                                                    selectionBounds = Offset(sMinX - 10f, sMinY - 10f)
                                                    selectionSize = Offset(sMaxX - sMinX + 20f, sMaxY - sMinY + 20f)
                                                }
                                            }
                                            lassoPath = emptyList()
                                            return@awaitEachGesture
                                        }
                                        
                                        val isEraserAction = currentTool == AnnotationTool.ERASER || 
                                                             downEvent.type == androidx.compose.ui.input.pointer.PointerType.Eraser
                                        if (isEraserAction) {
                                            var lastWX = wX
                                            var lastWY = wY
                                            val eraserPage = (lastWY / pageSpan).toInt().coerceIn(0, totalPages - 1)
                                            val localY = lastWY - getPageOffsetY(eraserPage)
                                            activeEraserScreenPos = downEvent.position
                                            onEraseStrokeAt(lastWX, localY, eraserRadius, eraserMode, eraserPage)
                                            noteTextBoxes.filter { isPointInTextBox(lastWX, lastWY, it, eraserRadius) }.forEach { tb ->
                                                onDeleteTextBox(tb.id)
                                                if (selectedTextBoxId == tb.id) selectedTextBoxId = null
                                            }
                                            var dragEvent = awaitPointerEvent()
                                            while (dragEvent.changes.any { it.pressed }) {
                                                val change = dragEvent.changes.firstOrNull { 
                                                    it.id == downEvent.id || ((it.type == androidx.compose.ui.input.pointer.PointerType.Stylus || it.type == androidx.compose.ui.input.pointer.PointerType.Eraser) && it.pressed)
                                                }
                                                if (change != null && change.pressed) {
                                                    activeEraserScreenPos = change.position
                                                    // Process high-frequency historical stylus points
                                                    change.historical.forEach { hist ->
                                                        val hwX = (hist.position.x - canvasOffset.x) / scale
                                                        val hwY = (hist.position.y - canvasOffset.y) / scale
                                                        val hDist = kotlin.math.hypot((hwX - lastWX).toDouble(), (hwY - lastWY).toDouble()).toFloat()
                                                        if (hDist >= 3f) {
                                                            val hSteps = (hDist / 5f).toInt().coerceIn(1, 10)
                                                            for (s in 1..hSteps) {
                                                                val ix = lastWX + (hwX - lastWX) * (s.toFloat() / hSteps)
                                                                val iy = lastWY + (hwY - lastWY) * (s.toFloat() / hSteps)
                                                                val stepPage = (iy / pageSpan).toInt().coerceIn(0, totalPages - 1)
                                                                val stepLocalY = iy - getPageOffsetY(stepPage)
                                                                onEraseStrokeAt(ix, stepLocalY, eraserRadius, eraserMode, stepPage)
                                                                noteTextBoxes.filter { isPointInTextBox(ix, iy, it, eraserRadius) }.forEach { tb ->
                                                                    onDeleteTextBox(tb.id)
                                                                    if (selectedTextBoxId == tb.id) selectedTextBoxId = null
                                                                }
                                                            }
                                                            lastWX = hwX
                                                            lastWY = hwY
                                                        }
                                                    }

                                                    val currWX = (change.position.x - canvasOffset.x) / scale
                                                    val currWY = (change.position.y - canvasOffset.y) / scale
                                                    val dist = kotlin.math.hypot((currWX - lastWX).toDouble(), (currWY - lastWY).toDouble()).toFloat()
                                                    if (dist >= 2f) {
                                                        val steps = (dist / 5f).toInt().coerceIn(1, 15)
                                                        for (s in 1..steps) {
                                                            val ix = lastWX + (currWX - lastWX) * (s.toFloat() / steps)
                                                            val iy = lastWY + (currWY - lastWY) * (s.toFloat() / steps)
                                                            val stepPage = (iy / pageSpan).toInt().coerceIn(0, totalPages - 1)
                                                            val stepLocalY = iy - getPageOffsetY(stepPage)
                                                            onEraseStrokeAt(ix, stepLocalY, eraserRadius, eraserMode, stepPage)
                                                            noteTextBoxes.filter { isPointInTextBox(ix, iy, it, eraserRadius) }.forEach { tb ->
                                                                onDeleteTextBox(tb.id)
                                                                if (selectedTextBoxId == tb.id) selectedTextBoxId = null
                                                            }
                                                        }
                                                        lastWX = currWX
                                                        lastWY = currWY
                                                    }
                                                    change.consume()
                                                }
                                                dragEvent = awaitPointerEvent()
                                            }
                                            activeEraserScreenPos = null
                                            onEraseGestureComplete()
                                        } else if (currentTool == AnnotationTool.LASER) {
                                            val t = System.currentTimeMillis()
                                            laserPoints.add(LaserPoint(wX, wY, t))
                                            var dragEvent = awaitPointerEvent()
                                            while (dragEvent.changes.any { it.pressed }) {
                                                val change = dragEvent.changes.firstOrNull { 
                                                    it.id == downEvent.id || ((it.type == androidx.compose.ui.input.pointer.PointerType.Stylus || it.type == androidx.compose.ui.input.pointer.PointerType.Eraser) && it.pressed)
                                                }
                                                if (change != null && change.pressed) {
                                                    val currWX = (change.position.x - canvasOffset.x) / scale
                                                    val currWY = (change.position.y - canvasOffset.y) / scale
                                                    laserPoints.add(LaserPoint(currWX, currWY, System.currentTimeMillis()))
                                                    change.consume()
                                                }
                                                dragEvent = awaitPointerEvent()
                                            }
                                        } else {
                                            // Clear any lasso selection when drawing
                                            selectedStrokeIndices = emptySet()
                                            selectionBounds = null
                                            selectionSize = null
                                            selectedTextBoxId = null
                                            
                                            activeDrawPoints.clear()
                                            val isHighlighter = currentTool == AnnotationTool.HIGHLIGHTER
                                            val isFountain = currentTool == AnnotationTool.FOUNTAIN
                                            val isCalligraphy = currentTool == AnnotationTool.CALLIGRAPHY
                                            activeDrawWidth = if (isHighlighter) 20f / scale else currentStrokeWidth / scale
                                            activeDrawColor = currentColor.toArgb().toLong()
                                            activeDrawHighlighter = isHighlighter
                                            activeDrawPenType = if (isCalligraphy) 3 else if (isFountain) 1 else 0
                                            isActiveDrawing = true
                                            
                                            val isStylusDown = downEvent.type == androidx.compose.ui.input.pointer.PointerType.Stylus || 
                                                               downEvent.type == androidx.compose.ui.input.pointer.PointerType.Eraser
                                            val pressure = if (isStylusDown) downEvent.pressure else 1.0f
                                            liveCurrentPressure = pressure
                                            
                                            val activeDrawingPageIndex = (wY / pageSpan).toInt().coerceIn(0, totalPages - 1)
                                            val pageTopWorldY = getPageOffsetY(activeDrawingPageIndex)
                                            val localWY = wY - pageTopWorldY
                                            activeDrawPoints.add(AnnotationPoint(wX, localWY, pressure))
                                            
                                            liveDrawMainPath.reset()
                                            liveTipPath.reset()
                                            liveFrontCount = 0
                                            liveBodyStarted = false
                                            
                                            var prevAnchorX = downEvent.position.x
                                            var prevAnchorY = downEvent.position.y
                                            var prevMidX = prevAnchorX
                                            var prevMidY = prevAnchorY
                                            var cumDist = 0f
                                            val effWidth = activeDrawWidth * scale
                                            val frontTaperDist = (effWidth * 3.0f).coerceIn(12f, 48f)
                                            
                                            // Immediate live dot on stylus contact
                                            liveTipPath.moveTo(prevAnchorX, prevAnchorY)
                                            liveTipPath.lineTo(prevAnchorX + 0.5f, prevAnchorY + 0.5f)
                                            liveRevision++
                                            
                                            var isSnapped = false
                                            var stillAnchorPos = downEvent.position
                                            var stillStartTime = 0L

                                            var dragEvent = awaitPointerEvent()
                                            while (dragEvent.changes.any { it.pressed }) {
                                                val change = dragEvent.changes.firstOrNull { 
                                                    it.id == downEvent.id || ((it.type == androidx.compose.ui.input.pointer.PointerType.Stylus || it.type == androidx.compose.ui.input.pointer.PointerType.Eraser) && it.pressed)
                                                }
                                                if (change != null && change.pressed) {
                                                    val isStylus2 = change.type == androidx.compose.ui.input.pointer.PointerType.Stylus || 
                                                                    change.type == androidx.compose.ui.input.pointer.PointerType.Eraser
                                                    val curPos = change.position
                                                    val cp = if (isStylus2) change.pressure else 1.0f
                                                    liveCurrentPressure = cp
                                                    val curX = curPos.x
                                                    val curY = curPos.y

                                                    if (!isSnapped) {
                                                        val pointsToProcess = ArrayList<Pair<Offset, Float>>(change.historical.size + 1)
                                                        change.historical.forEach { hist ->
                                                            val hp = cp
                                                            pointsToProcess.add(Pair(hist.position, hp))
                                                        }
                                                        pointsToProcess.add(Pair(curPos, cp))

                                                        for (pt in pointsToProcess) {
                                                            val px = pt.first.x
                                                            val py = pt.first.y
                                                            val pp = pt.second
                                                            val dx = px - prevAnchorX
                                                            val dy = py - prevAnchorY
                                                            val stepDist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

                                                            // 2.0px spatial threshold: eliminates digitizer sub-pixel jitter without lag
                                                            if (stepDist >= 2.0f) {
                                                                val midX = (prevAnchorX + px) / 2f
                                                                val midY = (prevAnchorY + py) / 2f
                                                                cumDist += stepDist

                                                                val hwX = (px - canvasOffset.x) / scale
                                                                val hwY = (py - canvasOffset.y) / scale
                                                                val localHWY = hwY - pageTopWorldY
                                                                activeDrawPoints.add(AnnotationPoint(hwX, localHWY, pp))

                                                                if (activeDrawHighlighter || activeDrawPenType == 3) {
                                                                    if (!liveBodyStarted) {
                                                                        liveDrawMainPath.moveTo(prevAnchorX, prevAnchorY)
                                                                        liveBodyStarted = true
                                                                    }
                                                                    liveDrawMainPath.quadraticTo(prevAnchorX, prevAnchorY, midX, midY)
                                                                } else if (cumDist <= frontTaperDist && liveFrontCount < liveFrontSegs.size) {
                                                                    val r = (cumDist / frontTaperDist).coerceIn(0f, 1f)
                                                                    val taper = r.toDouble().pow(0.85).toFloat().coerceIn(0.04f, 1f)
                                                                    val wSeg = (effWidth * taper).coerceIn(0.75f, effWidth * 2.2f)

                                                                    val seg = liveFrontSegs[liveFrontCount]
                                                                    seg.path.reset()
                                                                    seg.path.moveTo(prevAnchorX, prevAnchorY)
                                                                    seg.path.quadraticTo(prevAnchorX, prevAnchorY, midX, midY)
                                                                    seg.width = wSeg
                                                                    liveFrontCount++
                                                                } else {
                                                                    if (!liveBodyStarted) {
                                                                        liveDrawMainPath.moveTo(prevMidX, prevMidY)
                                                                        liveBodyStarted = true
                                                                    }
                                                                    liveDrawMainPath.quadraticTo(prevAnchorX, prevAnchorY, midX, midY)
                                                                }

                                                                prevAnchorX = px
                                                                prevAnchorY = py
                                                                prevMidX = midX
                                                                prevMidY = midY

                                                                if (!activeDrawHighlighter && (activeDrawPenType == 0 || activeDrawPenType == 1)) {
                                                                    try { com.bookflow.reader.util.HapticHelper.pencilGrittyTexture(hapticContext) } catch (_: Exception) {}
                                                                }
                                                            }
                                                        }

                                                        // Real-time live tip: connects smoothed spine directly to stylus nib (ZERO latency)
                                                        liveTipPath.reset()
                                                        liveTipPath.moveTo(prevMidX, prevMidY)
                                                        liveTipPath.lineTo(curX, curY)
                                                        liveRevision++

                                                        // Natural Pen Gesture: Hold-to-Snap stillness detection (550ms hold)
                                                        val distFromAnchor = kotlin.math.hypot((curPos.x - stillAnchorPos.x).toDouble(), (curPos.y - stillAnchorPos.y).toDouble()).toFloat()
                                                        if (distFromAnchor < 14f) {
                                                            if (stillStartTime == 0L) {
                                                                stillStartTime = System.currentTimeMillis()
                                                            } else if (System.currentTimeMillis() - stillStartTime >= 550L && activeDrawPoints.size >= 6) {
                                                                val stroke = AnnotationStroke(
                                                                    points = activeDrawPoints.toList(),
                                                                    color = activeDrawColor,
                                                                    strokeWidth = activeDrawWidth,
                                                                    isHighlighter = activeDrawHighlighter,
                                                                    penType = activeDrawPenType,
                                                                    pageIndex = activeDrawingPageIndex
                                                                )
                                                                val snapped = ShapeRecognizer.recognizeAndSnap(stroke)
                                                                if (snapped.points != activeDrawPoints.toList()) {
                                                                    isSnapped = true
                                                                    activeDrawPoints.clear()
                                                                    activeDrawPoints.addAll(snapped.points)
                                                                    activeDrawPenType = snapped.penType
                                                                    try { com.bookflow.reader.util.HapticHelper.click(hapticContext) } catch (_: Exception) {}

                                                                    liveDrawMainPath.reset()
                                                                    liveTipPath.reset()
                                                                    liveFrontCount = 0
                                                                    if (snapped.points.size >= 2) {
                                                                        val p0 = snapped.points[0]
                                                                        val p0X = p0.x * scale + canvasOffset.x
                                                                        val p0Y = (p0.y + pageTopWorldY) * scale + canvasOffset.y
                                                                        liveDrawMainPath.moveTo(p0X, p0Y)
                                                                        for (si in 1 until snapped.points.size) {
                                                                            val spt = snapped.points[si]
                                                                            liveDrawMainPath.lineTo(spt.x * scale + canvasOffset.x, (spt.y + pageTopWorldY) * scale + canvasOffset.y)
                                                                        }
                                                                        liveBodyStarted = true
                                                                    }
                                                                    liveRevision++
                                                                }
                                                            }
                                                        } else {
                                                            stillAnchorPos = curPos
                                                            stillStartTime = System.currentTimeMillis()
                                                        }
                                                    } else {
                                                        // Live shape resizing while stylus is held
                                                        if (activeDrawPoints.size == 2) {
                                                            val firstPoint = activeDrawPoints[0]
                                                            val currWX = (curPos.x - canvasOffset.x) / scale
                                                            val currWY = (curPos.y - canvasOffset.y) / scale
                                                            val currLocalWY = currWY - pageTopWorldY
                                                            activeDrawPoints.clear()
                                                            activeDrawPoints.add(firstPoint)
                                                            activeDrawPoints.add(AnnotationPoint(currWX, currLocalWY, cp))

                                                            liveDrawMainPath.reset()
                                                            val fX = firstPoint.x * scale + canvasOffset.x
                                                            val fY = (firstPoint.y + pageTopWorldY) * scale + canvasOffset.y
                                                            liveDrawMainPath.moveTo(fX, fY)
                                                            liveDrawMainPath.lineTo(curPos.x, curPos.y)
                                                            liveRevision++
                                                        }
                                                    }
                                                    change.consume()
                                                }
                                                dragEvent = awaitPointerEvent()
                                            }

                                            // Stroke complete - handle Scratch-to-Erase or commit stroke
                                            if (!isSnapped && ShapeRecognizer.isScratchOut(activeDrawPoints, 1f, 1f)) {
                                                try { com.bookflow.reader.util.HapticHelper.heavyClick(hapticContext) } catch (_: Exception) {}
                                                val minX = activeDrawPoints.minOf { it.x }
                                                val maxX = activeDrawPoints.maxOf { it.x }
                                                val minY = activeDrawPoints.minOf { it.y }
                                                val maxY = activeDrawPoints.maxOf { it.y }

                                                val steps = 6
                                                for (sx in 0..steps) {
                                                    for (sy in 0..steps) {
                                                        val ex = minX + (maxX - minX) * (sx.toFloat() / steps)
                                                        val ey = minY + (maxY - minY) * (sy.toFloat() / steps)
                                                        onEraseStrokeAt(ex, ey, eraserRadius, eraserMode, activeDrawingPageIndex)
                                                    }
                                                }
                                            } else if (activeDrawPoints.size == 1) {
                                                val pt = activeDrawPoints[0]
                                                val dotStroke = AnnotationStroke(
                                                    points = listOf(pt, AnnotationPoint(pt.x + 0.1f, pt.y + 0.1f, pt.pressure)),
                                                    color = activeDrawColor,
                                                    strokeWidth = activeDrawWidth,
                                                    isHighlighter = activeDrawHighlighter,
                                                    penType = activeDrawPenType,
                                                    pageIndex = activeDrawingPageIndex
                                                )
                                                onAddStroke(dotStroke)
                                            } else if (activeDrawPoints.size > 1) {
                                                val stroke = AnnotationStroke(
                                                    points = activeDrawPoints.toList(),
                                                    color = activeDrawColor,
                                                    strokeWidth = activeDrawWidth,
                                                    isHighlighter = activeDrawHighlighter,
                                                    penType = activeDrawPenType,
                                                    pageIndex = activeDrawingPageIndex
                                                )
                                                val finalStroke = if (isShapeModeEnabled && !isSnapped) {
                                                    ShapeRecognizer.recognizeAndSnap(stroke)
                                                } else {
                                                    stroke
                                                }
                                                onAddStroke(finalStroke)
                                            }
                                            activeDrawPoints.clear()
                                            liveDrawMainPath.reset()
                                            liveTipPath.reset()
                                            liveFrontCount = 0
                                            liveBodyStarted = false
                                            isActiveDrawing = false
                                            liveRevision++
                                        }
                                    }
                                }
                        ) {
                            // Draw in-progress stroke with live front-end sharp tip taper (ZERO ALLOCATIONS, O(1) CONSTANT TIME)
                            if (isActiveDrawing) {
                                val _rev = liveRevision
                                val adColor = Color(activeDrawColor.toInt())
                                val finalColor = if (activeDrawHighlighter) adColor.copy(alpha = 0.4f) else adColor
                                val effWidth = activeDrawWidth * scale
                                val wBody = when (activeDrawPenType) {
                                    1 -> effWidth * (0.35f + liveCurrentPressure * 1.35f)
                                    3 -> effWidth * (0.5f + liveCurrentPressure * 0.8f)
                                    else -> effWidth * (0.6f + liveCurrentPressure * 0.6f)
                                }.coerceIn(0.75f, effWidth * 2.2f)

                                if (activeDrawHighlighter || activeDrawPenType == 3) {
                                    if (liveBodyStarted) {
                                        drawPath(liveDrawMainPath, color = finalColor, style = Stroke(width = effWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                    }
                                    drawPath(liveTipPath, color = finalColor, style = Stroke(width = effWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                } else {
                                    // 1. Draw front taper segments
                                    for (i in 0 until liveFrontCount) {
                                        val seg = liveFrontSegs[i]
                                        drawPath(seg.path, color = finalColor, style = Stroke(width = seg.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                    }
                                    // 2. Draw continuous main body (ONE Skia draw call, 0.01ms)
                                    if (liveBodyStarted) {
                                        drawPath(liveDrawMainPath, color = finalColor, style = Stroke(width = wBody, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                    }
                                    // 3. Draw live tip connecting directly to physical stylus nib (ONE Skia draw call, 0.01ms)
                                    drawPath(liveTipPath, color = finalColor, style = Stroke(width = wBody, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                }
                            }
                            
                            // Draw laser points
                            if (laserPoints.size > 1) {
                                val now = System.currentTimeMillis()
                                var pX = laserPoints.first().x * scale + canvasOffset.x
                                var pY = laserPoints.first().y * scale + canvasOffset.y
                                for (i in 1 until laserPoints.size) {
                                    val pt = laserPoints[i]
                                    val x = pt.x * scale + canvasOffset.x
                                    val y = pt.y * scale + canvasOffset.y
                                    val age = now - pt.timestamp
                                    if (age < 1500) {
                                        val alpha = (1f - (age / 1500f)).coerceIn(0f, 1f)
                                        val midX = (pX + x) / 2
                                        val midY = (pY + y) / 2
                                        val seg = Path()
                                        seg.moveTo(pX, pY)
                                        seg.quadraticTo(pX, pY, midX, midY)
                                        
                                        // Glow effect
                                        drawPath(seg, color = Color.Red.copy(alpha = alpha * 0.4f), style = Stroke(width = 12f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                        drawPath(seg, color = Color.White.copy(alpha = alpha), style = Stroke(width = 4f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                    }
                                    pX = x
                                    pY = y
                                }
                            }
                            
                            // Draw lasso selection path while drawing
                            if (lassoPath.size > 1) {
                                val lp = Path()
                                lp.moveTo(lassoPath.first().x * scale + canvasOffset.x, lassoPath.first().y * scale + canvasOffset.y)
                                for (i in 1 until lassoPath.size) {
                                    lp.lineTo(lassoPath[i].x * scale + canvasOffset.x, lassoPath[i].y * scale + canvasOffset.y)
                                }
                                drawPath(lp, color = Color(0xFF3498DB), style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))))
                            }
                            
                            // Live Active Dragging Reticle for Eraser
                            activeEraserScreenPos?.let { pos ->
                                val screenR = (eraserRadius * scale).coerceIn(6f, 120f)
                                if (eraserMode == EraserMode.PRECISE) {
                                    drawCircle(
                                        color = Color(0xFF2D2D2D).copy(alpha = 0.14f),
                                        radius = screenR,
                                        center = pos
                                    )
                                    drawCircle(
                                        color = Color(0xFF2D2D2D).copy(alpha = 0.85f),
                                        radius = screenR,
                                        center = pos,
                                        style = Stroke(
                                            width = 1.5f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                        )
                                    )
                                    drawCircle(
                                        color = Color(0xFF2D2D2D),
                                        radius = 2.5f,
                                        center = pos
                                    )
                                } else {
                                    drawCircle(
                                        color = Color(0xFFE53935).copy(alpha = 0.15f),
                                        radius = 22f * scale,
                                        center = pos
                                    )
                                    drawCircle(
                                        color = Color(0xFFE53935).copy(alpha = 0.85f),
                                        radius = 22f * scale,
                                        center = pos,
                                        style = Stroke(
                                            width = 1.5f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                        )
                                    )
                                }
                            }

                            // Live Stylus Hover Reticle (rendered in screen coordinates)
                            hoverScreenOffset?.let { hoverPos ->
                                if (currentTool == AnnotationTool.ERASER) {
                                    val screenR = (eraserRadius * scale).coerceIn(6f, 120f)
                                    if (eraserMode == EraserMode.PRECISE) {
                                        drawCircle(
                                            color = Color.DarkGray.copy(alpha = 0.75f),
                                            radius = screenR,
                                            center = hoverPos,
                                            style = Stroke(
                                                width = 1.5f,
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                            )
                                        )
                                        drawCircle(
                                            color = Color(0xFF2D2D2D).copy(alpha = 0.85f),
                                            radius = 2.5f,
                                            center = hoverPos
                                        )
                                    } else {
                                        drawCircle(
                                            color = Color(0xFFE53935).copy(alpha = 0.85f),
                                            radius = 22f * scale,
                                            center = hoverPos,
                                            style = Stroke(
                                                width = 1.5f,
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                            )
                                        )
                                        drawCircle(
                                            color = Color(0xFFE53935),
                                            radius = 3.5f,
                                            center = hoverPos
                                        )
                                    }
                                } else if (currentTool == AnnotationTool.LASER) {
                                    drawCircle(color = Color.Red.copy(alpha = 0.35f), radius = 12f * scale, center = hoverPos)
                                    drawCircle(color = Color.Red, radius = 4f * scale, center = hoverPos)
                                    drawCircle(color = Color.White, radius = 2f * scale, center = hoverPos)
                                } else if (!isLassoMode && !isTextMode) {
                                    val reticleRadius = (currentStrokeWidth * scale / 2f).coerceAtLeast(3f)
                                    // Outer ring indicator
                                    drawCircle(
                                        color = Color(0xFF2D2D2D).copy(alpha = 0.5f),
                                        radius = reticleRadius + 3f,
                                        center = hoverPos,
                                        style = Stroke(width = 1f)
                                    )
                                    // Brush preview dot
                                    drawCircle(
                                        color = currentColor.copy(alpha = if (currentTool == AnnotationTool.HIGHLIGHTER) 0.5f else 0.85f),
                                        radius = reticleRadius,
                                        center = hoverPos
                                    )
                                    // Precise center targeting pip
                                    drawCircle(
                                        color = Color.White,
                                        radius = 1.5f,
                                        center = hoverPos
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Multi-Page Text Box Overlays with Typography & Transformations
                    noteTextBoxes.forEach { tb ->
                        val tbX = tb.x * scale + canvasOffset.x
                        val tbY = (tb.y + getPageOffsetY(tb.pageIndex)) * scale + canvasOffset.y
                        val tbW = (tb.width * scale).coerceAtLeast(120f)
                        val tbH = (tb.height * scale).coerceAtLeast(48f)
                        val isSelected = selectedTextBoxId == tb.id
                        
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(tbX.roundToInt(), tbY.roundToInt()) }
                                .width(with(density) { tbW.toDp() })
                                .heightIn(min = with(density) { tbH.toDp() })
                                .graphicsLayer {
                                    rotationZ = tb.rotation
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                                }
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                                            .border(2.dp, OceanBlue, RoundedCornerShape(10.dp))
                                    } else {
                                        Modifier
                                            .background(Color.Transparent)
                                            .border(1.dp, Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    }
                                )
                                .clickable {
                                    if (currentTool == AnnotationTool.ERASER) {
                                        onDeleteTextBox(tb.id)
                                        if (selectedTextBoxId == tb.id) selectedTextBoxId = null
                                    } else {
                                        selectedTextBoxId = tb.id
                                        activeInfinitePopup = "TEXT"
                                        activeTextFontFamily = tb.fontFamily
                                        activeTextFontSize = tb.fontSize
                                        activeTextBold = tb.isBold
                                        activeTextItalic = tb.isItalic
                                        activeTextUnderline = tb.isUnderline
                                        activeTextAlign = tb.textAlign
                                        currentColor = Color(tb.color.toInt())
                                    }
                                }
                                .padding(if (isSelected) 8.dp else 4.dp)
                        ) {
                            Column {
                                if (isSelected) {
                                    // Transformation Controls Row (Move Grip Bar, Rotation Button, Duplicate Button, Delete Button)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Wide, comfortable Move Grip Bar with Rotation-compensated world coordinates
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(30.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(DeepBlue.copy(alpha = 0.90f))
                                                .pointerInput(tb.id, tb.rotation, scale) {
                                                    detectDragGestures { change, dragAmount ->
                                                        change.consume()
                                                        val rad = Math.toRadians(tb.rotation.toDouble())
                                                        val cos = Math.cos(rad).toFloat()
                                                        val sin = Math.sin(rad).toFloat()
                                                        val worldDx = (dragAmount.x * cos - dragAmount.y * sin) / scale
                                                        val worldDy = (dragAmount.x * sin + dragAmount.y * cos) / scale
                                                        val newWorldX = tb.x + worldDx
                                                        val totalWorldY = tb.y + getPageOffsetY(tb.pageIndex) + worldDy
                                                        val pageSpan = A3_HEIGHT + A3_PAGE_GAP
                                                        val newPage = (totalWorldY / pageSpan).toInt().coerceIn(0, totalPages - 1)
                                                        val newLocalY = totalWorldY - getPageOffsetY(newPage)
                                                        onUpdateTextBox(tb.copy(x = newWorldX, y = newLocalY, pageIndex = newPage))
                                                    }
                                                }
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.OpenWith,
                                                contentDescription = "Move Text",
                                                tint = WarmCream,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Move",
                                                color = WarmCream,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        // 90° Rotate Clockwise Button
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(DarkSurfaceVariant)
                                                .clickable {
                                                    val nextRot = (tb.rotation + 90f) % 360f
                                                    onUpdateTextBox(tb.copy(rotation = nextRot))
                                                }
                                                .stylusHoverEffect(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.RotateRight,
                                                contentDescription = "Rotate 90°",
                                                tint = WarmCream,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Duplicate Button
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(DarkSurfaceVariant)
                                                .clickable {
                                                    val dup = tb.copy(
                                                        id = java.util.UUID.randomUUID().toString(),
                                                        x = tb.x + 30f,
                                                        y = tb.y + 30f
                                                    )
                                                    if (onAddTextBoxObj != null) {
                                                        onAddTextBoxObj(dup)
                                                    } else {
                                                        onAddTextBox(dup.x, dup.y)
                                                    }
                                                    selectedTextBoxId = dup.id
                                                }
                                                .stylusHoverEffect(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ContentCopy,
                                                contentDescription = "Duplicate",
                                                tint = WarmCream,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        // Delete Button (Prominent Red Trash Can)
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE53935))
                                                .clickable {
                                                    onDeleteTextBox(tb.id)
                                                    if (selectedTextBoxId == tb.id) selectedTextBoxId = null
                                                    if (activeInfinitePopup == "TEXT") activeInfinitePopup = null
                                                }
                                                .stylusHoverEffect(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Delete Text Box",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                val fontFam = when (tb.fontFamily) {
                                    "SERIF" -> FontFamily.Serif
                                    "MONOSPACE" -> FontFamily.Monospace
                                    "CURSIVE" -> FontFamily.Cursive
                                    else -> FontFamily.Default
                                }
                                val tAlign = when (tb.textAlign) {
                                    "CENTER" -> TextAlign.Center
                                    "RIGHT" -> TextAlign.Right
                                    else -> TextAlign.Left
                                }
                                val tDecoration = if (tb.isUnderline) TextDecoration.Underline else TextDecoration.None

                                var textValue by remember(tb.id, tb.text) { mutableStateOf(tb.text) }

                                BasicTextField(
                                    value = textValue,
                                    onValueChange = { newText ->
                                        textValue = newText
                                        onUpdateTextBox(tb.copy(text = newText))
                                    },
                                    textStyle = TextStyle(
                                        color = Color(tb.color.toInt()),
                                        fontSize = (tb.fontSize * scale).sp,
                                        fontWeight = if (tb.isBold) FontWeight.Bold else FontWeight.Normal,
                                        fontStyle = if (tb.isItalic) FontStyle.Italic else FontStyle.Normal,
                                        fontFamily = fontFam,
                                        textAlign = tAlign,
                                        textDecoration = tDecoration
                                    ),
                                    cursorBrush = SolidColor(OceanBlue),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp, vertical = 2.dp)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                selectedTextBoxId = tb.id
                                                activeInfinitePopup = "TEXT"
                                                activeTextFontFamily = tb.fontFamily
                                                activeTextFontSize = tb.fontSize
                                                activeTextBold = tb.isBold
                                                activeTextItalic = tb.isItalic
                                                activeTextUnderline = tb.isUnderline
                                                activeTextAlign = tb.textAlign
                                                currentColor = Color(tb.color.toInt())
                                            }
                                        },
                                    decorationBox = { innerTextField ->
                                        if (textValue.isEmpty()) {
                                            Text(
                                                text = "Tap to type...",
                                                color = Color.Gray.copy(alpha = 0.5f),
                                                fontSize = (tb.fontSize * scale).sp,
                                                fontFamily = fontFam,
                                                textAlign = tAlign
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                if (isSelected) {
                                    // Bottom Controls: Size Info Badge + Corner Resize Grip (Bottom-Right)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Size & Rotation Badge
                                        Text(
                                            text = "${tb.width.toInt()} × ${tb.height.toInt()} pt • ${tb.rotation.toInt()}°",
                                            color = LightMint.copy(alpha = 0.75f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )

                                        // Corner Resize Handle (Bottom-Right)
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(OceanBlue.copy(alpha = 0.5f))
                                                .border(1.dp, LightMint.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                                .pointerInput(tb.id, scale) {
                                                    detectDragGestures { change, dragAmount ->
                                                        change.consume()
                                                        val newW = (tb.width + dragAmount.x / scale).coerceIn(100f, 2500f)
                                                        val newH = (tb.height + dragAmount.y / scale).coerceIn(40f, 2500f)
                                                        onUpdateTextBox(tb.copy(width = newW, height = newH))
                                                    }
                                                }
                                                .stylusHoverEffect(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.OpenInFull,
                                                contentDescription = "Resize",
                                                tint = LightMint,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // End-of-Document "+ Add Page" card in continuous vertical flow
                    val addPageWorldY = getPageOffsetY(totalPages - 1) + A3_HEIGHT + 36f
                    val addPageScreenY = addPageWorldY * scale + canvasOffset.y
                    val addPageCardWidth = with(density) { 240.dp.toPx() }
                    val addPageScreenX = canvasOffset.x + (A3_WIDTH * scale - addPageCardWidth) / 2f

                    if (addPageScreenY > -100f && addPageScreenY < canvasHeight + 100f) {
                        Card(
                            modifier = Modifier
                                .offset { IntOffset(addPageScreenX.roundToInt(), addPageScreenY.roundToInt()) }
                                .width(240.dp)
                                .clickable {
                                    totalPages++
                                    currentPage = totalPages - 1
                                    selectedTextBoxId = null
                                    val targetY = -getPageOffsetY(currentPage) * scale + (canvasHeight - A3_HEIGHT * scale).coerceAtLeast(0f) / 2f
                                    canvasOffset = Offset(canvasOffset.x, targetY)
                                }
                                .stylusHoverEffect(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.94f)),
                            border = BorderStroke(1.5.dp, OceanBlue.copy(alpha = 0.65f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.AddCircle, contentDescription = "Add Page", tint = LightMint, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Add Page ${totalPages + 1}",
                                    color = WarmCream,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // Action buttons for lasso selection
                    if (selectedStrokeIndices.isNotEmpty() && selectionBounds != null && selectionSize != null && lassoDragOffset == Offset.Zero && lassoScaleFactor == 1f) {
                        val sb = selectionBounds!!
                        val ss = selectionSize!!
                        val btnX = (sb.x + ss.x) * scale + canvasOffset.x
                        val btnY = sb.y * scale + canvasOffset.y - 40f
                        Row(
                            modifier = Modifier
                                .offset { IntOffset(btnX.roundToInt(), btnY.roundToInt()) },
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (onMarkRecallSelection != null) {
                                Box(
                                    modifier = Modifier
                                        .background(DeepBlue, RoundedCornerShape(8.dp))
                                        .border(1.dp, OceanBlue.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                        .stylusHoverEffect(hoverScale = 1.15f)
                                        .clickable {
                                            val bmp = renderSelectionToBitmap(strokes, selectedStrokeIndices, sb, ss, getImagePath)
                                            onMarkRecallSelection(bmp)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Bolt,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD54F),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text("Recall", color = WarmCream, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE74C3C), RoundedCornerShape(8.dp))
                                    .stylusHoverEffect(hoverScale = 1.15f)
                                    .clickable {
                                        onDeleteStrokes(selectedStrokeIndices)
                                        selectedStrokeIndices = emptySet()
                                        selectionBounds = null
                                        selectionSize = null
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text("Delete", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    
                    // Floating Zoom & Navigation HUD (Shown only in Fullscreen to keep minimized canvas completely clear for writing)
                    if (isFullScreen) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 14.dp, top = 14.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = DarkSurface.copy(alpha = 0.88f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Zoom Out [-]
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .stylusHoverEffect(hoverScale = 1.15f)
                                        .clickable {
                                            scale = (scale / 1.15f).coerceIn(0.2f, 5.0f)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("−", color = WarmCream, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                // Zoom % display with click to reset
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            scale = 1f
                                            canvasOffset = Offset.Zero
                                        }
                                        .stylusHoverEffect(hoverScale = 1.08f)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${(scale * 100).toInt()}%",
                                        color = OceanBlueLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Zoom In [+]
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .stylusHoverEffect(hoverScale = 1.15f)
                                        .clickable {
                                            scale = (scale * 1.15f).coerceIn(0.2f, 5.0f)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", color = WarmCream, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                // 1:1 Reset Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DeepBlue.copy(alpha = 0.45f))
                                        .stylusHoverEffect(hoverScale = 1.12f)
                                        .clickable {
                                            scale = 1f
                                            canvasOffset = Offset.Zero
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("1:1", color = WarmCream, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Floating Multi-Page Document Navigation Pill (A3 Multi-Page Document)
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkSurface.copy(alpha = 0.94f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Previous Page Button [<]
                            IconButton(
                                onClick = {
                                    if (currentPage > 0) {
                                        currentPage--
                                        selectedTextBoxId = null
                                        val targetY = -getPageOffsetY(currentPage) * scale + (canvasHeight - A3_HEIGHT * scale).coerceAtLeast(0f) / 2f
                                        canvasOffset = Offset(canvasOffset.x, targetY)
                                    }
                                },
                                enabled = currentPage > 0,
                                modifier = Modifier.size(28.dp).stylusHoverEffect()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NavigateBefore,
                                    contentDescription = "Previous Page",
                                    tint = if (currentPage > 0) WarmCream else Color.Gray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Page Counter Text
                            Text(
                                text = "Page ${currentPage + 1} / $totalPages",
                                color = WarmCream,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // Next Page Button [>]
                            IconButton(
                                onClick = {
                                    if (currentPage < totalPages - 1) {
                                        currentPage++
                                        selectedTextBoxId = null
                                        val targetY = -getPageOffsetY(currentPage) * scale + (canvasHeight - A3_HEIGHT * scale).coerceAtLeast(0f) / 2f
                                        canvasOffset = Offset(canvasOffset.x, targetY)
                                    }
                                },
                                enabled = currentPage < totalPages - 1,
                                modifier = Modifier.size(28.dp).stylusHoverEffect()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NavigateNext,
                                    contentDescription = "Next Page",
                                    tint = if (currentPage < totalPages - 1) WarmCream else Color.Gray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            VerticalDivider(modifier = Modifier.height(18.dp).padding(horizontal = 2.dp), color = DarkSurfaceVariant)

                            // Add Page Button [+]
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OceanBlue.copy(alpha = 0.25f))
                                    .border(1.dp, OceanBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        totalPages++
                                        currentPage = totalPages - 1
                                        selectedTextBoxId = null
                                        val targetY = -getPageOffsetY(currentPage) * scale + (canvasHeight - A3_HEIGHT * scale).coerceAtLeast(0f) / 2f
                                        canvasOffset = Offset(canvasOffset.x, targetY)
                                    }
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                                    .stylusHoverEffect(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add Page", tint = LightMint, modifier = Modifier.size(13.dp))
                                    Text("Page", color = LightMint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Auto-fit A3 Page to screen Button
                            IconButton(
                                onClick = {
                                    applyOrientationFit()
                                },
                                modifier = Modifier.size(28.dp).stylusHoverEffect()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FitScreen,
                                    contentDescription = "Fit A3 Page",
                                    tint = OceanBlueLight,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Movable & Dockable Inking Toolbar Container
                    val isVertical = toolbarDock == ToolbarDock.LEFT || toolbarDock == ToolbarDock.RIGHT

                    val toolbarPlacementModifier = if (isDraggingToolbar || toolbarDock == ToolbarDock.FLOAT) {
                        Modifier.offset {
                            IntOffset(
                                toolbarOffset.x.roundToInt().coerceIn(0, maxOf(0, (canvasWidth - 70).toInt())),
                                toolbarOffset.y.roundToInt().coerceIn(0, maxOf(0, (canvasHeight - 70).toInt()))
                            )
                        }
                    } else {
                        when (toolbarDock) {
                            ToolbarDock.LEFT -> Modifier.align(Alignment.CenterStart).padding(start = 10.dp)
                            ToolbarDock.RIGHT -> Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)
                            ToolbarDock.TOP -> Modifier.align(Alignment.TopCenter).padding(top = 56.dp)
                            ToolbarDock.BOTTOM -> Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                            ToolbarDock.FLOAT -> Modifier.offset {
                                IntOffset(toolbarOffset.x.roundToInt(), toolbarOffset.y.roundToInt())
                            }
                        }
                    }

                    val toolbarDragModifier = Modifier.pointerInput(canvasWidth, canvasHeight, toolbarDock) {
                        detectDragGestures(
                            onDragStart = {
                                isDraggingToolbar = true
                                val currentX = when (toolbarDock) {
                                    ToolbarDock.LEFT -> 10f
                                    ToolbarDock.RIGHT -> (canvasWidth - 70f).coerceAtLeast(0f)
                                    ToolbarDock.TOP, ToolbarDock.BOTTOM -> ((canvasWidth - 620f) / 2f).coerceAtLeast(10f)
                                    ToolbarDock.FLOAT -> toolbarOffset.x
                                }
                                val currentY = when (toolbarDock) {
                                    ToolbarDock.TOP -> 56f
                                    ToolbarDock.BOTTOM -> (canvasHeight - 76f).coerceAtLeast(0f)
                                    ToolbarDock.LEFT, ToolbarDock.RIGHT -> ((canvasHeight - 480f) / 2f).coerceAtLeast(10f)
                                    ToolbarDock.FLOAT -> toolbarOffset.y
                                }
                                toolbarOffset = Offset(currentX, currentY)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                toolbarOffset = Offset(
                                    x = (toolbarOffset.x + dragAmount.x).coerceIn(0f, (canvasWidth - 60f).coerceAtLeast(0f)),
                                    y = (toolbarOffset.y + dragAmount.y).coerceIn(0f, (canvasHeight - 60f).coerceAtLeast(0f))
                                )
                            },
                            onDragEnd = {
                                isDraggingToolbar = false
                                val thresholdX = 140f
                                val thresholdY = 120f
                                toolbarDock = when {
                                    toolbarOffset.x < thresholdX -> ToolbarDock.LEFT
                                    toolbarOffset.x > canvasWidth - thresholdX - 60f -> ToolbarDock.RIGHT
                                    toolbarOffset.y < thresholdY -> ToolbarDock.TOP
                                    toolbarOffset.y > canvasHeight - thresholdY - 60f -> ToolbarDock.BOTTOM
                                    else -> ToolbarDock.FLOAT
                                }
                            },
                            onDragCancel = {
                                isDraggingToolbar = false
                            }
                        )
                    }

                    Box(
                        modifier = toolbarPlacementModifier
                    ) {
                        // Expandable Template Insertion Drawer (fallback if showTemplateInsert is triggered externally)
                        if (showTemplateInsert && activeInfinitePopup == null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DarkSurfaceVariant.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                shadowElevation = 8.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TemplateButton("Rect") { onInsertTemplate(listOf(SketchTemplates.getFlowchartRect(0f, 0f))) }
                                    TemplateButton("Diamond") { onInsertTemplate(listOf(SketchTemplates.getFlowchartDiamond(0f, 0f))) }
                                    TemplateButton("Oval") { onInsertTemplate(listOf(SketchTemplates.getFlowchartOval(0f, 0f))) }
                                    TemplateButton("Table") { onInsertTemplate(SketchTemplates.getTable(0f, 0f)) }
                                    TemplateButton("Axes") { onInsertTemplate(SketchTemplates.getCoordinateAxes(0f, 0f)) }
                                    TemplateButton("Venn") { onInsertTemplate(SketchTemplates.getVennDiagram(0f, 0f)) }
                                    TemplateButton("NumLine") { onInsertTemplate(SketchTemplates.getNumberLine(0f, 0f)) }
                                    TemplateButton("Mind Map") { onInsertTemplate(SketchTemplates.getMindMapCenter(0f, 0f)) }
                                }
                            }
                        }

                        // Main Floating Inking Toolbar Card (Collapsible into Bubble)
                        AnimatedContent(
                            targetState = isToolbarCollapsed,
                            transitionSpec = {
                                (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                 scaleIn(initialScale = 0.88f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)))
                                .togetherWith(
                                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                    scaleOut(targetScale = 0.88f)
                                )
                            },
                            label = "InfiniteToolbarCollapseAnimation"
                        ) { collapsed ->
                            if (collapsed) {
                                Surface(
                                    shape = RoundedCornerShape(26.dp),
                                    color = DarkSurface.copy(alpha = 0.95f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                    shadowElevation = 14.dp,
                                    modifier = Modifier.then(toolbarDragModifier)
                                ) {
                                    if (isVertical) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Filled.DragIndicator, contentDescription = "Drag to Move", tint = WarmCream.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(DeepBlue.copy(alpha = 0.45f))
                                                    .clickable { isToolbarCollapsed = false }
                                                    .padding(8.dp)
                                                    .stylusHoverEffect(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val toolIcon = when {
                                                    isTextMode -> Icons.Filled.TextFields
                                                    isLassoMode -> Icons.Filled.HighlightAlt
                                                    isShapeModeEnabled -> Icons.Filled.Category
                                                    currentTool == AnnotationTool.FOUNTAIN -> Icons.Filled.Brush
                                                    currentTool == AnnotationTool.CALLIGRAPHY -> Icons.Filled.Create
                                                    currentTool == AnnotationTool.HIGHLIGHTER -> Icons.Filled.BorderColor
                                                    currentTool == AnnotationTool.LASER -> Icons.Filled.Star
                                                    currentTool == AnnotationTool.ERASER -> if (eraserMode == EraserMode.PRECISE) Icons.Filled.TrackChanges else Icons.Filled.Bolt
                                                    else -> Icons.Filled.Edit
                                                }
                                                Icon(toolIcon, contentDescription = "Expand Toolbar", tint = if (currentTool == AnnotationTool.ERASER) WarmCream else currentColor, modifier = Modifier.size(18.dp))
                                            }
                                            HorizontalDivider(modifier = Modifier.width(20.dp), color = DarkSurfaceVariant)
                                            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(30.dp).stylusHoverEffect()) {
                                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (canUndo) WarmCream else Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(30.dp).stylusHoverEffect()) {
                                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (canRedo) WarmCream else Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Filled.DragIndicator, contentDescription = "Drag to Move", tint = WarmCream.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(DeepBlue.copy(alpha = 0.45f))
                                                    .clickable { isToolbarCollapsed = false }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    .stylusHoverEffect(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val toolIcon = when {
                                                    isTextMode -> Icons.Filled.TextFields
                                                    isLassoMode -> Icons.Filled.HighlightAlt
                                                    isShapeModeEnabled -> Icons.Filled.Category
                                                    currentTool == AnnotationTool.FOUNTAIN -> Icons.Filled.Brush
                                                    currentTool == AnnotationTool.CALLIGRAPHY -> Icons.Filled.Create
                                                    currentTool == AnnotationTool.HIGHLIGHTER -> Icons.Filled.BorderColor
                                                    currentTool == AnnotationTool.LASER -> Icons.Filled.Star
                                                    currentTool == AnnotationTool.ERASER -> if (eraserMode == EraserMode.PRECISE) Icons.Filled.TrackChanges else Icons.Filled.Bolt
                                                    else -> Icons.Filled.Edit
                                                }
                                                Icon(toolIcon, contentDescription = "Active Tool", tint = if (currentTool == AnnotationTool.ERASER) WarmCream else currentColor, modifier = Modifier.size(18.dp))
                                                if (currentTool != AnnotationTool.ERASER && !isLassoMode && !isTextMode) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(currentColor)
                                                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                                    )
                                                }
                                                Icon(
                                                    imageVector = if (toolbarDock == ToolbarDock.TOP) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                                    contentDescription = "Expand Toolbar",
                                                    tint = OceanBlue,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Box(modifier = Modifier.width(1.dp).height(20.dp).background(DarkSurfaceVariant))
                                            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(32.dp).stylusHoverEffect()) {
                                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (canUndo) WarmCream else Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(17.dp))
                                            }
                                            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(32.dp).stylusHoverEffect()) {
                                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (canRedo) WarmCream else Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(17.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                val popupCardContent = @Composable {
                                    Column {
                                        // ==========================================
                                        // FLOATING EXTENDED OPTIONS POPUP CARD (ON 2ND TAP)
                                        // ==========================================
                                        AnimatedVisibility(
                                        visible = activeInfinitePopup != null,
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
                                                    val popupTitle = when (activeInfinitePopup) {
                                                        "PEN" -> "Pen Settings"
                                                        "FOUNTAIN" -> "Fountain Pen Settings"
                                                        "CALLIGRAPHY" -> "Calligraphy Settings"
                                                        "HIGHLIGHTER" -> "Highlighter Settings"
                                                        "LASER" -> "Laser Pointer"
                                                        "ERASER" -> "Eraser Settings"
                                                        "LASSO" -> "Lasso Selection"
                                                        "TEXT" -> "Text & Typography"
                                                        "TEMPLATES" -> "Diagram Templates"
                                                        "BACKGROUND" -> "Note Background & Paper"
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
                                                                .background(
                                                                    when (activeInfinitePopup) {
                                                                        "BACKGROUND" -> noteBackgroundColor
                                                                        "ERASER" -> OceanBlue
                                                                        "TEXT" -> currentColor
                                                                        else -> currentColor
                                                                    }
                                                                )
                                                        )
                                                        Text(
                                                            text = popupTitle,
                                                            color = WarmCream,
                                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { activeInfinitePopup = null },
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

                                                when (activeInfinitePopup) {
                                                    "PEN", "FOUNTAIN", "CALLIGRAPHY", "HIGHLIGHTER" -> {
                                                        // Thickness Presets & Stepper/Slider
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                text = "Thickness: ${String.format(Locale.US, "%.1f", currentStrokeWidth)} pt",
                                                                color = WarmCream.copy(alpha = 0.85f),
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )

                                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                val presets = listOf(2.5f to "Fine", 4.5f to "Med", 8.0f to "Bold", 14.0f to "Thick")
                                                                presets.forEach { (presetW, _) ->
                                                                    val isSel = kotlin.math.abs(currentStrokeWidth - presetW) < 1.0f
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
                                                                            .clickable { currentStrokeWidth = presetW }
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

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            IconButton(
                                                                onClick = { currentStrokeWidth = (currentStrokeWidth - 1f).coerceAtLeast(1.5f) },
                                                                modifier = Modifier.size(28.dp).stylusHoverEffect()
                                                            ) {
                                                                Text("-", color = WarmCream, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                            Slider(
                                                                value = currentStrokeWidth,
                                                                onValueChange = { currentStrokeWidth = it },
                                                                valueRange = 1.5f..24f,
                                                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                                                colors = SliderDefaults.colors(
                                                                    thumbColor = OceanBlue,
                                                                    activeTrackColor = OceanBlue,
                                                                    inactiveTrackColor = DarkSurfaceVariant
                                                                )
                                                            )
                                                            IconButton(
                                                                onClick = { currentStrokeWidth = (currentStrokeWidth + 1f).coerceAtMost(24f) },
                                                                modifier = Modifier.size(28.dp).stylusHoverEffect()
                                                            ) {
                                                                Text("+", color = WarmCream, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }

                                                        // Color Palette Swatches
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
                                                                    val isSelected = currentColor == color
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(28.dp)
                                                                            .stylusHoverEffect(hoverScale = 1.25f)
                                                                            .clip(CircleShape)
                                                                            .background(color)
                                                                            .border(
                                                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                                                color = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.35f),
                                                                                shape = CircleShape
                                                                            )
                                                                            .clickable { currentColor = color }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    "ERASER" -> {
                                                        Column(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            Text(
                                                                text = "Eraser Mode",
                                                                color = WarmCream.copy(alpha = 0.85f),
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )

                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                // Precise Eraser Chip
                                                                Surface(
                                                                    onClick = { eraserMode = EraserMode.PRECISE },
                                                                    shape = RoundedCornerShape(10.dp),
                                                                    color = if (eraserMode == EraserMode.PRECISE) OceanBlue.copy(alpha = 0.28f) else DarkSurfaceVariant.copy(alpha = 0.5f),
                                                                    border = BorderStroke(
                                                                        width = if (eraserMode == EraserMode.PRECISE) 1.5.dp else 0.5.dp,
                                                                        color = if (eraserMode == EraserMode.PRECISE) OceanBlue else Color.White.copy(alpha = 0.15f)
                                                                    ),
                                                                    modifier = Modifier.weight(1f).stylusHoverEffect()
                                                                ) {
                                                                    Row(
                                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.Center
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Filled.TrackChanges,
                                                                            contentDescription = null,
                                                                            tint = if (eraserMode == EraserMode.PRECISE) OceanBlue else WarmCream,
                                                                            modifier = Modifier.size(14.dp)
                                                                        )
                                                                        Spacer(Modifier.width(5.dp))
                                                                        Text(
                                                                            text = "Precise Eraser",
                                                                            color = if (eraserMode == EraserMode.PRECISE) OceanBlue else WarmCream,
                                                                            fontSize = 11.sp,
                                                                            fontWeight = if (eraserMode == EraserMode.PRECISE) FontWeight.Bold else FontWeight.Normal
                                                                        )
                                                                    }
                                                                }

                                                                // Stroke Eraser Chip
                                                                Surface(
                                                                    onClick = { eraserMode = EraserMode.STROKE },
                                                                    shape = RoundedCornerShape(10.dp),
                                                                    color = if (eraserMode == EraserMode.STROKE) OceanBlue.copy(alpha = 0.28f) else DarkSurfaceVariant.copy(alpha = 0.5f),
                                                                    border = BorderStroke(
                                                                        width = if (eraserMode == EraserMode.STROKE) 1.5.dp else 0.5.dp,
                                                                        color = if (eraserMode == EraserMode.STROKE) OceanBlue else Color.White.copy(alpha = 0.15f)
                                                                    ),
                                                                    modifier = Modifier.weight(1f).stylusHoverEffect()
                                                                ) {
                                                                    Row(
                                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.Center
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Filled.Bolt,
                                                                            contentDescription = null,
                                                                            tint = if (eraserMode == EraserMode.STROKE) OceanBlue else WarmCream,
                                                                            modifier = Modifier.size(14.dp)
                                                                        )
                                                                        Spacer(Modifier.width(5.dp))
                                                                        Text(
                                                                            text = "Stroke Eraser",
                                                                            color = if (eraserMode == EraserMode.STROKE) OceanBlue else WarmCream,
                                                                            fontSize = 11.sp,
                                                                            fontWeight = if (eraserMode == EraserMode.STROKE) FontWeight.Bold else FontWeight.Normal
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            Text(
                                                                text = if (eraserMode == EraserMode.PRECISE) 
                                                                    "Cuts & erases only exact parts of writing under the circle."
                                                                else 
                                                                    "Deletes the entire line or shape when touched.",
                                                                color = WarmCream.copy(alpha = 0.65f),
                                                                fontSize = 11.sp
                                                            )

                                                            if (eraserMode == EraserMode.PRECISE) {
                                                                // Quick size presets
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    listOf(
                                                                        "Fine (10pt)" to 10f,
                                                                        "Medium (18pt)" to 18f,
                                                                        "Broad (32pt)" to 32f
                                                                    ).forEach { (label, presetRadius) ->
                                                                        val isSel = kotlin.math.abs(eraserRadius - presetRadius) < 1f
                                                                        Surface(
                                                                            onClick = { eraserRadius = presetRadius },
                                                                            shape = RoundedCornerShape(8.dp),
                                                                            color = if (isSel) OceanBlue.copy(alpha = 0.22f) else DarkSurfaceVariant.copy(alpha = 0.35f),
                                                                            border = BorderStroke(
                                                                                width = if (isSel) 1.dp else 0.5.dp,
                                                                                color = if (isSel) OceanBlue else Color.White.copy(alpha = 0.12f)
                                                                            ),
                                                                            modifier = Modifier.weight(1f).stylusHoverEffect()
                                                                        ) {
                                                                            Box(
                                                                                contentAlignment = Alignment.Center,
                                                                                modifier = Modifier.padding(vertical = 4.dp)
                                                                            ) {
                                                                                Text(
                                                                                    text = label,
                                                                                    color = if (isSel) OceanBlue else WarmCream.copy(alpha = 0.85f),
                                                                                    fontSize = 10.sp,
                                                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }

                                                                // Fine-tuning Radius Slider
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(
                                                                        text = "Size: ${eraserRadius.toInt()} pt",
                                                                        color = WarmCream.copy(alpha = 0.8f),
                                                                        fontSize = 11.sp,
                                                                        modifier = Modifier.width(70.dp)
                                                                    )
                                                                    Slider(
                                                                        value = eraserRadius,
                                                                        onValueChange = { eraserRadius = it },
                                                                        valueRange = 6f..48f,
                                                                        modifier = Modifier.weight(1f),
                                                                        colors = SliderDefaults.colors(
                                                                            thumbColor = OceanBlue,
                                                                            activeTrackColor = OceanBlue,
                                                                            inactiveTrackColor = DarkSurfaceVariant
                                                                        )
                                                                    )
                                                                    // Live circle indicator
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .padding(start = 8.dp)
                                                                            .size(28.dp),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .size((eraserRadius * 0.6f).coerceIn(4f, 26f).dp)
                                                                                .clip(CircleShape)
                                                                                .background(Color.White.copy(alpha = 0.85f))
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.6f))

                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(
                                                                    text = "Clear Entire Note Canvas",
                                                                    color = WarmCream.copy(alpha = 0.7f),
                                                                    fontSize = 12.sp
                                                                )
                                                                Surface(
                                                                    onClick = {
                                                                        onClear()
                                                                        activeInfinitePopup = null
                                                                    },
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
                                                                        Text("Clear All", color = Color(0xFFFFCDD2), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    "LASSO" -> {
                                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Text(
                                                                text = "Filter Selection Target",
                                                                color = WarmCream.copy(alpha = 0.85f),
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                listOf(0 to "All", 1 to "Strokes", 2 to "Images").forEach { (targetIdx, label) ->
                                                                    val isSel = lassoTarget == targetIdx
                                                                    Surface(
                                                                        onClick = { lassoTarget = targetIdx },
                                                                        shape = RoundedCornerShape(10.dp),
                                                                        color = if (isSel) DeepBlue.copy(alpha = 0.6f) else DarkSurfaceVariant,
                                                                        border = BorderStroke(1.dp, if (isSel) OceanBlue else Color.White.copy(alpha = 0.12f)),
                                                                        modifier = Modifier.stylusHoverEffect()
                                                                    ) {
                                                                        Text(
                                                                            text = label,
                                                                            color = if (isSel) LightMint else WarmCream,
                                                                            fontSize = 11.sp,
                                                                            fontWeight = FontWeight.SemiBold,
                                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            Text(
                                                                text = "Draw a loop around elements with stylus to move, scale, or delete.",
                                                                color = WarmCream.copy(alpha = 0.65f),
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                    }

                                                    "TEMPLATES" -> {
                                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Text(
                                                                text = "Insert Flowchart & Diagram Components",
                                                                color = WarmCream.copy(alpha = 0.85f),
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                TemplateButton("Rect") {
                                                                    onInsertTemplate(listOf(SketchTemplates.getFlowchartRect(0f, 0f)))
                                                                    activeInfinitePopup = null
                                                                }
                                                                TemplateButton("Diamond") {
                                                                    onInsertTemplate(listOf(SketchTemplates.getFlowchartDiamond(0f, 0f)))
                                                                    activeInfinitePopup = null
                                                                }
                                                                TemplateButton("Oval") {
                                                                    onInsertTemplate(listOf(SketchTemplates.getFlowchartOval(0f, 0f)))
                                                                    activeInfinitePopup = null
                                                                }
                                                                TemplateButton("Table") {
                                                                    onInsertTemplate(SketchTemplates.getTable(0f, 0f))
                                                                    activeInfinitePopup = null
                                                                }
                                                            }
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                TemplateButton("Axes") {
                                                                    onInsertTemplate(SketchTemplates.getCoordinateAxes(0f, 0f))
                                                                    activeInfinitePopup = null
                                                                }
                                                                TemplateButton("Venn") {
                                                                    onInsertTemplate(SketchTemplates.getVennDiagram(0f, 0f))
                                                                    activeInfinitePopup = null
                                                                }
                                                                TemplateButton("NumLine") {
                                                                    onInsertTemplate(SketchTemplates.getNumberLine(0f, 0f))
                                                                    activeInfinitePopup = null
                                                                }
                                                                TemplateButton("Mind Map") {
                                                                    onInsertTemplate(SketchTemplates.getMindMapCenter(0f, 0f))
                                                                    activeInfinitePopup = null
                                                                }
                                                            }
                                                        }
                                                    }

                                                    "BACKGROUND" -> {
                                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                            // Section 1: Background Color Swatches
                                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                Text(
                                                                    text = "Note Background Color",
                                                                    color = WarmCream.copy(alpha = 0.85f),
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                                val bgColors = listOf(
                                                                    Color(0xFFFAF8F5) to "Warm Paper",
                                                                    Color(0xFFF5EFE1) to "Linen",
                                                                    Color(0xFFE5D3AF) to "Parchment",
                                                                    Color(0xFFFBF0D9) to "Sepia",
                                                                    Color(0xFFE8EEF3) to "Soft Slate",
                                                                    Color(0xFFAEC4D4) to "Slate Gray",
                                                                    Color(0xFF1E1417) to "Burgundy Dark",
                                                                    Color(0xFF140D0F) to "Obsidian",
                                                                    Color(0xFF000000) to "Black",
                                                                    Color(0xFFFFFFFF) to "Pure White"
                                                                )
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    bgColors.forEach { (bgColor, _) ->
                                                                        val isSelected = noteBackgroundColor == bgColor
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .size(28.dp)
                                                                                .stylusHoverEffect(hoverScale = 1.25f)
                                                                                .clip(CircleShape)
                                                                                .background(bgColor)
                                                                                .border(
                                                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                                                    color = if (isSelected) OceanBlue else Color.Gray.copy(alpha = 0.4f),
                                                                                    shape = CircleShape
                                                                                )
                                                                                .clickable { noteBackgroundColor = bgColor }
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.6f))

                                                            // Section 2: Canvas Paper Template / Grid Style
                                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                Text(
                                                                    text = "Paper Grid & Layout",
                                                                    color = WarmCream.copy(alpha = 0.85f),
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    val templates = listOf(
                                                                        CanvasTemplate.DOT_GRID to "Dots",
                                                                        CanvasTemplate.RULED_LINES to "Ruled",
                                                                        CanvasTemplate.GRAPH_PAPER to "Graph",
                                                                        CanvasTemplate.BLANK to "Blank",
                                                                        CanvasTemplate.CORNELL to "Cornell"
                                                                    )
                                                                    templates.forEach { (tmpl, label) ->
                                                                        val isSel = localCanvasTemplate == tmpl
                                                                        Surface(
                                                                            onClick = { localCanvasTemplate = tmpl },
                                                                            shape = RoundedCornerShape(10.dp),
                                                                            color = if (isSel) DeepBlue.copy(alpha = 0.65f) else DarkSurfaceVariant,
                                                                            border = BorderStroke(1.dp, if (isSel) LightMint else Color.White.copy(alpha = 0.12f)),
                                                                            modifier = Modifier.stylusHoverEffect()
                                                                        ) {
                                                                            Text(
                                                                                text = label,
                                                                                color = if (isSel) LightMint else WarmCream,
                                                                                fontSize = 11.sp,
                                                                                fontWeight = FontWeight.SemiBold,
                                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    "TEXT" -> {
                                                        Column(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            // 1. Font Family Selector
                                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                Text(
                                                                    text = "Font Family",
                                                                    color = WarmCream.copy(alpha = 0.85f),
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    val fonts = listOf(
                                                                        "DEFAULT" to "Sans",
                                                                        "SERIF" to "Serif",
                                                                        "MONOSPACE" to "Mono",
                                                                        "CURSIVE" to "Script"
                                                                    )
                                                                    fonts.forEach { (famKey, label) ->
                                                                        val isSel = activeTextFontFamily == famKey
                                                                        Surface(
                                                                            onClick = {
                                                                                activeTextFontFamily = famKey
                                                                                val sel = noteTextBoxes.find { it.id == selectedTextBoxId }
                                                                                if (sel != null) {
                                                                                    onUpdateTextBox(sel.copy(fontFamily = famKey))
                                                                                }
                                                                            },
                                                                            shape = RoundedCornerShape(10.dp),
                                                                            color = if (isSel) DeepBlue.copy(alpha = 0.65f) else DarkSurfaceVariant,
                                                                            border = BorderStroke(1.dp, if (isSel) LightMint else Color.White.copy(alpha = 0.12f)),
                                                                            modifier = Modifier.stylusHoverEffect()
                                                                        ) {
                                                                            Text(
                                                                                text = label,
                                                                                color = if (isSel) LightMint else WarmCream,
                                                                                fontSize = 11.sp,
                                                                                fontWeight = FontWeight.SemiBold,
                                                                                fontFamily = when (famKey) {
                                                                                    "SERIF" -> FontFamily.Serif
                                                                                    "MONOSPACE" -> FontFamily.Monospace
                                                                                    "CURSIVE" -> FontFamily.Cursive
                                                                                    else -> FontFamily.Default
                                                                                },
                                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.6f))

                                                            // 2. Font Size Stepper & Presets
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(
                                                                    text = "Size: ${activeTextFontSize.roundToInt()} sp",
                                                                    color = WarmCream.copy(alpha = 0.85f),
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )

                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    IconButton(
                                                                        onClick = {
                                                                            val newSize = (activeTextFontSize - 2f).coerceAtLeast(10f)
                                                                            activeTextFontSize = newSize
                                                                            val sel = noteTextBoxes.find { it.id == selectedTextBoxId }
                                                                            if (sel != null) onUpdateTextBox(sel.copy(fontSize = newSize))
                                                                        },
                                                                        modifier = Modifier.size(28.dp).stylusHoverEffect()
                                                                    ) {
                                                                        Icon(Icons.Filled.Remove, contentDescription = "Decrease Font Size", tint = WarmCream, modifier = Modifier.size(16.dp))
                                                                    }

                                                                    val sizePresets = listOf(14f, 18f, 24f, 32f, 40f)
                                                                    sizePresets.forEach { s ->
                                                                        val isSel = activeTextFontSize == s
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .size(28.dp)
                                                                                .clip(CircleShape)
                                                                                .background(if (isSel) OceanBlue.copy(alpha = 0.35f) else DarkSurfaceVariant)
                                                                                .border(1.dp, if (isSel) OceanBlue else Color.White.copy(alpha = 0.12f), CircleShape)
                                                                                .clickable {
                                                                                    activeTextFontSize = s
                                                                                    val sel = noteTextBoxes.find { it.id == selectedTextBoxId }
                                                                                    if (sel != null) onUpdateTextBox(sel.copy(fontSize = s))
                                                                                }
                                                                                .stylusHoverEffect(),
                                                                            contentAlignment = Alignment.Center
                                                                        ) {
                                                                            Text(
                                                                                text = s.toInt().toString(),
                                                                                color = if (isSel) LightMint else WarmCream,
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.Bold
                                                                            )
                                                                        }
                                                                    }

                                                                    IconButton(
                                                                        onClick = {
                                                                            val newSize = (activeTextFontSize + 2f).coerceAtMost(72f)
                                                                            activeTextFontSize = newSize
                                                                            val sel = noteTextBoxes.find { it.id == selectedTextBoxId }
                                                                            if (sel != null) onUpdateTextBox(sel.copy(fontSize = newSize))
                                                                        },
                                                                        modifier = Modifier.size(28.dp).stylusHoverEffect()
                                                                    ) {
                                                                        Icon(Icons.Filled.Add, contentDescription = "Increase Font Size", tint = WarmCream, modifier = Modifier.size(16.dp))
                                                                    }
                                                                }
                                                            }

                                                            HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.6f))

                                                            // 3. Styles & Alignment
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                // Formatting: Bold, Italic, Underline
                                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                    Surface(
                                                                        onClick = {
                                                                            activeTextBold = !activeTextBold
                                                                            val sel = noteTextBoxes.find { it.id == selectedTextBoxId }
                                                                            if (sel != null) onUpdateTextBox(sel.copy(isBold = activeTextBold))
                                                                        },
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        color = if (activeTextBold) OceanBlue.copy(alpha = 0.4f) else DarkSurfaceVariant,
                                                                        border = BorderStroke(1.dp, if (activeTextBold) OceanBlue else Color.White.copy(alpha = 0.12f)),
                                                                        modifier = Modifier.size(32.dp).stylusHoverEffect()
                                                                    ) {
                                                                        Box(contentAlignment = Alignment.Center) {
                                                                            Icon(Icons.Filled.FormatBold, contentDescription = "Bold", tint = if (activeTextBold) LightMint else WarmCream, modifier = Modifier.size(18.dp))
                                                                        }
                                                                    }

                                                                    Surface(
                                                                        onClick = {
                                                                            activeTextItalic = !activeTextItalic
                                                                            val sel = noteTextBoxes.find { it.id == selectedTextBoxId }
                                                                            if (sel != null) onUpdateTextBox(sel.copy(isItalic = activeTextItalic))
                                                                        },
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        color = if (activeTextItalic) OceanBlue.copy(alpha = 0.4f) else DarkSurfaceVariant,
                                                                        border = BorderStroke(1.dp, if (activeTextItalic) OceanBlue else Color.White.copy(alpha = 0.12f)),
                                                                        modifier = Modifier.size(32.dp).stylusHoverEffect()
                                                                    ) {
                                                                        Box(contentAlignment = Alignment.Center) {
                                                                            Icon(Icons.Filled.FormatItalic, contentDescription = "Italic", tint = if (activeTextItalic) LightMint else WarmCream, modifier = Modifier.size(18.dp))
                                                                        }
                                                                    }

                                                                    Surface(
                                                                        onClick = {
                                                                            activeTextUnderline = !activeTextUnderline
                                                                            val sel = noteTextBoxes.find { it.id == selectedTextBoxId }
                                                                            if (sel != null) onUpdateTextBox(sel.copy(isUnderline = activeTextUnderline))
                                                                        },
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        color = if (activeTextUnderline) OceanBlue.copy(alpha = 0.4f) else DarkSurfaceVariant,
                                                                        border = BorderStroke(1.dp, if (activeTextUnderline) OceanBlue else Color.White.copy(alpha = 0.12f)),
                                                                        modifier = Modifier.size(32.dp).stylusHoverEffect()
                                                                    ) {
                                                                        Box(contentAlignment = Alignment.Center) {
                                                                            Icon(Icons.Filled.FormatUnderlined, contentDescription = "Underline", tint = if (activeTextUnderline) LightMint else WarmCream, modifier = Modifier.size(18.dp))
                                                                        }
                                                                    }
                                                                }

                                                                // Alignment: Left, Center, Right
                                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                    val aligns = listOf("LEFT" to Icons.Filled.FormatAlignLeft, "CENTER" to Icons.Filled.FormatAlignCenter, "RIGHT" to Icons.Filled.FormatAlignRight)
                                                                    aligns.forEach { (al, icon) ->
                                                                        val isSel = activeTextAlign == al
                                                                        Surface(
                                                                            onClick = {
                                                                                activeTextAlign = al
                                                                                val sel = noteTextBoxes.find { it.id == selectedTextBoxId }
                                                                                if (sel != null) onUpdateTextBox(sel.copy(textAlign = al))
                                                                            },
                                                                            shape = RoundedCornerShape(8.dp),
                                                                            color = if (isSel) OceanBlue.copy(alpha = 0.4f) else DarkSurfaceVariant,
                                                                            border = BorderStroke(1.dp, if (isSel) OceanBlue else Color.White.copy(alpha = 0.12f)),
                                                                            modifier = Modifier.size(32.dp).stylusHoverEffect()
                                                                        ) {
                                                                            Box(contentAlignment = Alignment.Center) {
                                                                                Icon(icon, contentDescription = al, tint = if (isSel) LightMint else WarmCream, modifier = Modifier.size(18.dp))
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.6f))

                                                            // 4. Color Palette
                                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                Text(
                                                                    text = "Text Color",
                                                                    color = WarmCream.copy(alpha = 0.85f),
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                                val textPalette = listOf(
                                                                    Color(0xFF2D2D2D), Color(0xFFFFFFFF), Color(0xFF1E88E5), Color(0xFF00ACC1),
                                                                    Color(0xFF43A047), Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFF8E24AA)
                                                                )
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    textPalette.forEach { c ->
                                                                        val isSel = currentColor == c
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .size(28.dp)
                                                                                .clip(CircleShape)
                                                                                .background(c)
                                                                                .border(
                                                                                    width = if (isSel) 2.5.dp else 1.dp,
                                                                                    color = if (isSel) OceanBlue else Color.Gray.copy(alpha = 0.4f),
                                                                                    shape = CircleShape
                                                                                )
                                                                                .clickable {
                                                                                    currentColor = c
                                                                                    val sel = noteTextBoxes.find { it.id == selectedTextBoxId }
                                                                                    if (sel != null) onUpdateTextBox(sel.copy(color = c.toArgb().toLong()))
                                                                                }
                                                                                .stylusHoverEffect(hoverScale = 1.25f)
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.6f))

                                                            // 5. Transformations & Rotation
                                                            val selBox = noteTextBoxes.find { it.id == selectedTextBoxId }
                                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(
                                                                        text = "Rotation (${selBox?.rotation?.toInt() ?: 0}°)",
                                                                        color = WarmCream.copy(alpha = 0.85f),
                                                                        fontSize = 12.sp,
                                                                        fontWeight = FontWeight.SemiBold
                                                                    )
                                                                    if (selBox != null && selBox.rotation != 0f) {
                                                                        Text(
                                                                            text = "Reset 0°",
                                                                            color = LightMint,
                                                                            fontSize = 11.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            modifier = Modifier
                                                                                .clickable {
                                                                                    onUpdateTextBox(selBox.copy(rotation = 0f))
                                                                                }
                                                                                .padding(4.dp)
                                                                        )
                                                                    }
                                                                }
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                ) {
                                                                    val rotOptions = listOf(0f to "0°", 90f to "90°", 180f to "180°", 270f to "270°")
                                                                    rotOptions.forEach { (rDeg, label) ->
                                                                        val isSelRot = (selBox?.rotation ?: 0f) == rDeg
                                                                        Surface(
                                                                            onClick = {
                                                                                if (selBox != null) onUpdateTextBox(selBox.copy(rotation = rDeg))
                                                                            },
                                                                            shape = RoundedCornerShape(8.dp),
                                                                            color = if (isSelRot) OceanBlue.copy(alpha = 0.45f) else DarkSurfaceVariant,
                                                                            border = BorderStroke(1.dp, if (isSelRot) LightMint else Color.White.copy(alpha = 0.12f)),
                                                                            modifier = Modifier.weight(1f).height(32.dp).stylusHoverEffect()
                                                                        ) {
                                                                            Box(contentAlignment = Alignment.Center) {
                                                                                Text(label, color = if (isSelRot) LightMint else WarmCream, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                if (selBox != null) {
                                                                    HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.6f))
                                                                    Surface(
                                                                        onClick = {
                                                                            onDeleteTextBox(selBox.id)
                                                                            selectedTextBoxId = null
                                                                            activeInfinitePopup = null
                                                                        },
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        color = Color(0xFFE53935).copy(alpha = 0.18f),
                                                                        border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.7f)),
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .height(36.dp)
                                                                            .stylusHoverEffect()
                                                                    ) {
                                                                        Row(
                                                                            modifier = Modifier.fillMaxSize(),
                                                                            horizontalArrangement = Arrangement.Center,
                                                                            verticalAlignment = Alignment.CenterVertically
                                                                        ) {
                                                                            Icon(
                                                                                imageVector = Icons.Filled.Delete,
                                                                                contentDescription = "Delete Text Box",
                                                                                tint = Color(0xFFFF5252),
                                                                                modifier = Modifier.size(16.dp)
                                                                            )
                                                                            Spacer(Modifier.width(6.dp))
                                                                            Text(
                                                                                text = "Delete Selected Text Box",
                                                                                color = Color(0xFFFF5252),
                                                                                fontSize = 12.sp,
                                                                                fontWeight = FontWeight.Bold
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
                                    }
                                }
                                }

                                val renderToolbarButtons: @Composable (Boolean) -> Unit = { isVert ->
                                    val handlePenClick: (AnnotationTool, String) -> Unit = { tool, key ->
                                        if (currentTool == tool && !isTextMode && !isLassoMode) {
                                            activeInfinitePopup = if (activeInfinitePopup == key) null else key
                                        } else {
                                            currentTool = tool
                                            isTextMode = false
                                            isLassoMode = false
                                            activeInfinitePopup = null
                                        }
                                    }

                                    if (isVert) {
                                        Column(
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp, vertical = 8.dp)
                                                .verticalScroll(rememberScrollState()),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            // Drag Grip
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 34.dp, height = 22.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.White.copy(alpha = 0.08f))
                                                    .then(toolbarDragModifier),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DragIndicator,
                                                    contentDescription = "Drag to Move / Dock Toolbar",
                                                    tint = WarmCream.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            HorizontalDivider(modifier = Modifier.width(22.dp).padding(vertical = 2.dp), color = DarkSurfaceVariant)

                                            // Inking Pens
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.Edit,
                                                label = "Pen",
                                                isSelected = currentTool == AnnotationTool.PEN && !isTextMode && !isLassoMode,
                                                isPopupOpen = activeInfinitePopup == "PEN",
                                                activeColor = currentColor,
                                                onClick = { handlePenClick(AnnotationTool.PEN, "PEN") }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.Brush,
                                                label = "Fountain",
                                                isSelected = currentTool == AnnotationTool.FOUNTAIN && !isTextMode && !isLassoMode,
                                                isPopupOpen = activeInfinitePopup == "FOUNTAIN",
                                                activeColor = currentColor,
                                                onClick = { handlePenClick(AnnotationTool.FOUNTAIN, "FOUNTAIN") }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.Create,
                                                label = "Calligraphy",
                                                isSelected = currentTool == AnnotationTool.CALLIGRAPHY && !isTextMode && !isLassoMode,
                                                isPopupOpen = activeInfinitePopup == "CALLIGRAPHY",
                                                activeColor = currentColor,
                                                onClick = { handlePenClick(AnnotationTool.CALLIGRAPHY, "CALLIGRAPHY") }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.BorderColor,
                                                label = "Highlighter",
                                                isSelected = currentTool == AnnotationTool.HIGHLIGHTER && !isTextMode && !isLassoMode,
                                                isPopupOpen = activeInfinitePopup == "HIGHLIGHTER",
                                                activeColor = currentColor,
                                                onClick = { handlePenClick(AnnotationTool.HIGHLIGHTER, "HIGHLIGHTER") }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.Star,
                                                label = "Laser",
                                                isSelected = currentTool == AnnotationTool.LASER && !isTextMode && !isLassoMode,
                                                isPopupOpen = activeInfinitePopup == "LASER",
                                                activeColor = null,
                                                onClick = { handlePenClick(AnnotationTool.LASER, "LASER") }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = if (eraserMode == EraserMode.PRECISE) Icons.Filled.TrackChanges else Icons.Filled.Bolt,
                                                label = if (eraserMode == EraserMode.PRECISE) "Precise" else "Stroke",
                                                isSelected = currentTool == AnnotationTool.ERASER && !isTextMode && !isLassoMode,
                                                isPopupOpen = activeInfinitePopup == "ERASER",
                                                activeColor = null,
                                                onClick = { handlePenClick(AnnotationTool.ERASER, "ERASER") }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.HighlightAlt,
                                                label = "Lasso",
                                                isSelected = isLassoMode,
                                                isPopupOpen = activeInfinitePopup == "LASSO",
                                                activeColor = null,
                                                onClick = {
                                                    if (isLassoMode) {
                                                        activeInfinitePopup = if (activeInfinitePopup == "LASSO") null else "LASSO"
                                                    } else {
                                                        isLassoMode = true
                                                        isTextMode = false
                                                        activeInfinitePopup = null
                                                    }
                                                    selectedStrokeIndices = emptySet(); selectionBounds = null; selectionSize = null; lassoPath = emptyList()
                                                }
                                            )

                                            HorizontalDivider(modifier = Modifier.width(22.dp).padding(vertical = 2.dp), color = DarkSurfaceVariant)

                                            // Creation Tools
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.Category,
                                                label = "Shape Mode",
                                                isSelected = isShapeModeEnabled,
                                                isPopupOpen = false,
                                                activeColor = null,
                                                onClick = onToggleShapeMode
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.TextFields,
                                                label = "Text Box",
                                                isSelected = isTextMode,
                                                isPopupOpen = activeInfinitePopup == "TEXT",
                                                activeColor = null,
                                                onClick = {
                                                    if (isTextMode) {
                                                        activeInfinitePopup = if (activeInfinitePopup == "TEXT") null else "TEXT"
                                                    } else {
                                                        isTextMode = true
                                                        isLassoMode = false
                                                        activeInfinitePopup = "TEXT"
                                                    }
                                                }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.Dashboard,
                                                label = "Templates",
                                                isSelected = activeInfinitePopup == "TEMPLATES" || showTemplateInsert,
                                                isPopupOpen = activeInfinitePopup == "TEMPLATES",
                                                activeColor = null,
                                                onClick = {
                                                    activeInfinitePopup = if (activeInfinitePopup == "TEMPLATES") null else "TEMPLATES"
                                                }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.Palette,
                                                label = "Background & Paper",
                                                isSelected = activeInfinitePopup == "BACKGROUND",
                                                isPopupOpen = activeInfinitePopup == "BACKGROUND",
                                                activeColor = noteBackgroundColor,
                                                onClick = {
                                                    activeInfinitePopup = if (activeInfinitePopup == "BACKGROUND") null else "BACKGROUND"
                                                }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.Face,
                                                label = "Stamps",
                                                isSelected = false,
                                                isPopupOpen = false,
                                                activeColor = null,
                                                onClick = onStampsClick
                                            )

                                            HorizontalDivider(modifier = Modifier.width(22.dp).padding(vertical = 2.dp), color = DarkSurfaceVariant)

                                            // Quick Undo / Redo / Export / Clear
                                            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(34.dp).stylusHoverEffect()) {
                                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (canUndo) WarmCream else Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(34.dp).stylusHoverEffect()) {
                                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (canRedo) WarmCream else Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = onExport, modifier = Modifier.size(34.dp).stylusHoverEffect()) {
                                                Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export PDF", tint = OceanBlueLight, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = onClear, modifier = Modifier.size(34.dp).stylusHoverEffect()) {
                                                Icon(Icons.Filled.LayersClear, contentDescription = "Clear Canvas", tint = WarmCream.copy(alpha = 0.75f), modifier = Modifier.size(18.dp))
                                            }

                                            HorizontalDivider(modifier = Modifier.width(22.dp).padding(vertical = 2.dp), color = DarkSurfaceVariant)

                                            // Collapse Button
                                            IconButton(
                                                onClick = {
                                                    isToolbarCollapsed = true
                                                    activeInfinitePopup = null
                                                },
                                                modifier = Modifier.size(34.dp).stylusHoverEffect()
                                            ) {
                                                Icon(
                                                    imageVector = if (toolbarDock == ToolbarDock.LEFT) Icons.Filled.KeyboardArrowLeft else Icons.Filled.KeyboardArrowRight,
                                                    contentDescription = "Collapse to Bubble",
                                                    tint = WarmCream,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                                .horizontalScroll(rememberScrollState()),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            // Drag Grip
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 22.dp, height = 34.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.White.copy(alpha = 0.08f))
                                                    .then(toolbarDragModifier),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DragIndicator,
                                                    contentDescription = "Drag to Move / Dock Toolbar",
                                                    tint = WarmCream.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 2.dp), color = DarkSurfaceVariant)

                                            // Inking Pens
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.Edit,
                                                label = "Pen",
                                                isSelected = currentTool == AnnotationTool.PEN && !isTextMode && !isLassoMode,
                                                isPopupOpen = activeInfinitePopup == "PEN",
                                                activeColor = currentColor,
                                                onClick = { handlePenClick(AnnotationTool.PEN, "PEN") }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.Brush,
                                                label = "Fountain",
                                                isSelected = currentTool == AnnotationTool.FOUNTAIN && !isTextMode && !isLassoMode,
                                                isPopupOpen = activeInfinitePopup == "FOUNTAIN",
                                                activeColor = currentColor,
                                                onClick = { handlePenClick(AnnotationTool.FOUNTAIN, "FOUNTAIN") }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.Create,
                                                label = "Calligraphy",
                                                isSelected = currentTool == AnnotationTool.CALLIGRAPHY && !isTextMode && !isLassoMode,
                                                isPopupOpen = activeInfinitePopup == "CALLIGRAPHY",
                                                activeColor = currentColor,
                                                onClick = { handlePenClick(AnnotationTool.CALLIGRAPHY, "CALLIGRAPHY") }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.BorderColor,
                                                label = "Highlighter",
                                                isSelected = currentTool == AnnotationTool.HIGHLIGHTER && !isTextMode && !isLassoMode,
                                                isPopupOpen = activeInfinitePopup == "HIGHLIGHTER",
                                                activeColor = currentColor,
                                                onClick = { handlePenClick(AnnotationTool.HIGHLIGHTER, "HIGHLIGHTER") }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.Star,
                                                label = "Laser",
                                                isSelected = currentTool == AnnotationTool.LASER && !isTextMode && !isLassoMode,
                                                isPopupOpen = activeInfinitePopup == "LASER",
                                                activeColor = null,
                                                onClick = { handlePenClick(AnnotationTool.LASER, "LASER") }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = if (eraserMode == EraserMode.PRECISE) Icons.Filled.TrackChanges else Icons.Filled.Bolt,
                                                label = if (eraserMode == EraserMode.PRECISE) "Precise" else "Stroke",
                                                isSelected = currentTool == AnnotationTool.ERASER && !isTextMode && !isLassoMode,
                                                isPopupOpen = activeInfinitePopup == "ERASER",
                                                activeColor = null,
                                                onClick = { handlePenClick(AnnotationTool.ERASER, "ERASER") }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.HighlightAlt,
                                                label = "Lasso",
                                                isSelected = isLassoMode,
                                                isPopupOpen = activeInfinitePopup == "LASSO",
                                                activeColor = null,
                                                onClick = {
                                                    if (isLassoMode) {
                                                        activeInfinitePopup = if (activeInfinitePopup == "LASSO") null else "LASSO"
                                                    } else {
                                                        isLassoMode = true
                                                        isTextMode = false
                                                        activeInfinitePopup = null
                                                    }
                                                    selectedStrokeIndices = emptySet(); selectionBounds = null; selectionSize = null; lassoPath = emptyList()
                                                }
                                            )

                                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 2.dp), color = DarkSurfaceVariant)

                                            // Creation Tools
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.Category,
                                                label = "Shape Mode",
                                                isSelected = isShapeModeEnabled,
                                                isPopupOpen = false,
                                                activeColor = null,
                                                onClick = onToggleShapeMode
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.TextFields,
                                                label = "Text Box",
                                                isSelected = isTextMode,
                                                isPopupOpen = activeInfinitePopup == "TEXT",
                                                activeColor = null,
                                                onClick = {
                                                    if (isTextMode) {
                                                        activeInfinitePopup = if (activeInfinitePopup == "TEXT") null else "TEXT"
                                                    } else {
                                                        isTextMode = true
                                                        isLassoMode = false
                                                        activeInfinitePopup = "TEXT"
                                                    }
                                                }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.Dashboard,
                                                label = "Templates",
                                                isSelected = activeInfinitePopup == "TEMPLATES" || showTemplateInsert,
                                                isPopupOpen = activeInfinitePopup == "TEMPLATES",
                                                activeColor = null,
                                                onClick = {
                                                    activeInfinitePopup = if (activeInfinitePopup == "TEMPLATES") null else "TEMPLATES"
                                                }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.Palette,
                                                label = "Background & Paper",
                                                isSelected = activeInfinitePopup == "BACKGROUND",
                                                isPopupOpen = activeInfinitePopup == "BACKGROUND",
                                                activeColor = noteBackgroundColor,
                                                onClick = {
                                                    activeInfinitePopup = if (activeInfinitePopup == "BACKGROUND") null else "BACKGROUND"
                                                }
                                            )
                                            RefinedNoteToolbarButton(
                                                icon = Icons.Filled.Face,
                                                label = "Stamps",
                                                isSelected = false,
                                                isPopupOpen = false,
                                                activeColor = null,
                                                onClick = onStampsClick
                                            )

                                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 2.dp), color = DarkSurfaceVariant)

                                            // Quick Undo / Redo / Export / Clear
                                            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(34.dp).stylusHoverEffect()) {
                                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (canUndo) WarmCream else Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(34.dp).stylusHoverEffect()) {
                                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (canRedo) WarmCream else Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = onExport, modifier = Modifier.size(34.dp).stylusHoverEffect()) {
                                                Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export PDF", tint = OceanBlueLight, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = onClear, modifier = Modifier.size(34.dp).stylusHoverEffect()) {
                                                Icon(Icons.Filled.LayersClear, contentDescription = "Clear Canvas", tint = WarmCream.copy(alpha = 0.75f), modifier = Modifier.size(18.dp))
                                            }

                                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 2.dp), color = DarkSurfaceVariant)

                                            // Collapse Button
                                            IconButton(
                                                onClick = {
                                                    isToolbarCollapsed = true
                                                    activeInfinitePopup = null
                                                },
                                                modifier = Modifier.size(34.dp).stylusHoverEffect()
                                            ) {
                                                Icon(
                                                    imageVector = if (toolbarDock == ToolbarDock.TOP) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                                    contentDescription = "Collapse to Bubble",
                                                    tint = WarmCream,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isVertical) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (toolbarDock == ToolbarDock.RIGHT) {
                                            popupCardContent()
                                            Surface(
                                                shape = RoundedCornerShape(24.dp),
                                                color = DarkSurface.copy(alpha = 0.96f),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                                shadowElevation = 16.dp
                                            ) {
                                                renderToolbarButtons(true)
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(24.dp),
                                                color = DarkSurface.copy(alpha = 0.96f),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                                shadowElevation = 16.dp
                                            ) {
                                                renderToolbarButtons(true)
                                            }
                                            popupCardContent()
                                        }
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (toolbarDock == ToolbarDock.TOP) {
                                            Surface(
                                                shape = RoundedCornerShape(24.dp),
                                                color = DarkSurface.copy(alpha = 0.96f),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                                shadowElevation = 16.dp
                                            ) {
                                                renderToolbarButtons(false)
                                            }
                                            popupCardContent()
                                        } else {
                                            popupCardContent()
                                            Surface(
                                                shape = RoundedCornerShape(24.dp),
                                                color = DarkSurface.copy(alpha = 0.96f),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                                shadowElevation = 16.dp
                                            ) {
                                                renderToolbarButtons(false)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Resize handle (active only in windowed mode)
                    if (!isFullScreen) {
                        val minWidthPx = with(density) { 380.dp.toPx() }
                        val minHeightPx = with(density) { 380.dp.toPx() }
                        val maxWidthPx = with(density) { (configuration.screenWidthDp.dp - 20.dp).toPx() }
                        val maxHeightPx = with(density) { (configuration.screenHeightDp.dp - 20.dp).toPx() }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(44.dp)
                                .background(
                                    color = Color.Gray.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(topStart = 20.dp, bottomEnd = 14.dp)
                                )
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        windowSize = Offset(
                                            x = (windowSize.x + dragAmount.x).coerceIn(minWidthPx, maxWidthPx),
                                            y = (windowSize.y + dragAmount.y).coerceIn(minHeightPx, maxHeightPx)
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.NetworkCell, contentDescription = "Resize", tint = WarmCream.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ray casting point-in-polygon test.
 */
private fun pointInPolygon(x: Float, y: Float, polygon: List<AnnotationPoint>): Boolean {
    var inside = false
    val n = polygon.size
    var j = n - 1
    for (i in 0 until n) {
        val pi = polygon[i]
        val pj = polygon[j]
        if ((pi.y > y) != (pj.y > y) &&
            x < (pj.x - pi.x) * (y - pi.y) / (pj.y - pi.y) + pi.x
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

@Composable
private fun TemplateButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFF2D6A9F).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .stylusHoverEffect(hoverScale = 1.12f)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = WarmCream, fontSize = 10.sp)
    }
}

@Composable
private fun RefinedNoteToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    isPopupOpen: Boolean,
    activeColor: Color? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
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
                modifier = Modifier.size(18.dp)
            )
            if (activeColor != null && (isSelected || label.contains("Background"))) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                        .border(0.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
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

private fun renderSelectionToBitmap(
    strokes: List<AnnotationStroke>,
    indices: Set<Int>,
    bounds: Offset,
    size: Offset,
    getImagePath: (Long) -> String?
): Bitmap {
    val w = size.x.toInt().coerceIn(60, 1600)
    val h = size.y.toInt().coerceIn(60, 1600)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }
    canvas.save()
    canvas.translate(-bounds.x, -bounds.y)
    indices.forEach { idx ->
        if (idx in strokes.indices) {
            val stroke = strokes[idx]
            if (stroke.penType == 2 && stroke.points.size >= 2) {
                val path = getImagePath(stroke.color)
                if (path != null) {
                    val imgBmp = BitmapFactory.decodeFile(path)
                    if (imgBmp != null) {
                        val srcRect = android.graphics.Rect(0, 0, imgBmp.width, imgBmp.height)
                        val dstRect = android.graphics.RectF(
                            stroke.points[0].x,
                            stroke.points[0].y,
                            stroke.points[0].x + stroke.points[1].x,
                            stroke.points[0].y + stroke.points[1].y
                        )
                        canvas.drawBitmap(imgBmp, srcRect, dstRect, null)
                        imgBmp.recycle()
                    }
                }
            } else if (stroke.points.size > 1) {
                paint.color = stroke.color.toInt()
                paint.strokeWidth = stroke.strokeWidth
                if (stroke.isHighlighter) {
                    paint.alpha = 100
                }
                val path = android.graphics.Path()
                path.moveTo(stroke.points[0].x, stroke.points[0].y)
                for (i in 1 until stroke.points.size) {
                    path.lineTo(stroke.points[i].x, stroke.points[i].y)
                }
                canvas.drawPath(path, paint)
            }
        }
    }
    canvas.restore()
    return bmp
}

/**
 * Pre-compiled Skia render data for completed strokes in InfiniteNotePad.
 * Eliminates per-frame path allocations and re-calculations.
 */
private class CachedNoteStroke(
    val stroke: AnnotationStroke,
    val baseColor: Color,
    val isUniformPath: Boolean = false,
    val uniformPath: Path? = null,
    val uniformWidth: Float = 0f,
    val isDot: Boolean = false,
    val dotCenter: Offset = Offset.Zero,
    val dotRadius: Float = 0f,
    val segments: List<Pair<Path, Float>> = emptyList()
) {
    fun draw(drawScope: DrawScope, isSelected: Boolean) {
        val drawColor = if (isSelected) Color(0x553498DBL) else baseColor
        val extraWidth = if (isSelected) 6f else 0f

        if (isDot) {
            drawScope.drawCircle(
                color = drawColor,
                radius = dotRadius + (if (isSelected) 3f else 0f),
                center = dotCenter
            )
            return
        }

        if (isUniformPath && uniformPath != null) {
            drawScope.drawPath(
                path = uniformPath,
                color = drawColor,
                style = Stroke(
                    width = uniformWidth + extraWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            return
        }

        for (i in segments.indices) {
            val (path, wSeg) = segments[i]
            drawScope.drawPath(
                path = path,
                color = drawColor,
                style = Stroke(
                    width = wSeg + extraWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

private fun buildCachedNoteStroke(stroke: AnnotationStroke): CachedNoteStroke {
    val n = stroke.points.size
    if (n == 0) return CachedNoteStroke(stroke, Color.Transparent)
    val baseColor = Color(stroke.color.toInt()).copy(alpha = if (stroke.isHighlighter) 0.4f else 1f)

    if (n == 1) {
        val p = stroke.points[0]
        val radius = if (!stroke.isHighlighter) (stroke.strokeWidth * 0.5f).coerceAtLeast(1.5f) else (stroke.strokeWidth / 2f)
        return CachedNoteStroke(
            stroke = stroke,
            baseColor = baseColor,
            isDot = true,
            dotCenter = Offset(p.x, p.y),
            dotRadius = radius
        )
    }

    if (stroke.isHighlighter) {
        val path = Path()
        val first = stroke.points.first()
        var prevX = first.x; var prevY = first.y
        path.moveTo(prevX, prevY)
        for (i in 1 until n) {
            val p = stroke.points[i]
            val x = p.x; val y = p.y
            val midX = (prevX + x) / 2f; val midY = (prevY + y) / 2f
            if (i == 1) path.lineTo(midX, midY) else path.quadraticTo(prevX, prevY, midX, midY)
            prevX = x; prevY = y
        }
        path.lineTo(prevX, prevY)
        return CachedNoteStroke(
            stroke = stroke,
            baseColor = baseColor,
            isUniformPath = true,
            uniformPath = path,
            uniformWidth = stroke.strokeWidth
        )
    }

    // ALL WRITING PENS: Sharp tips from both front and back ends
    val xs = FloatArray(n)
    val ys = FloatArray(n)
    val cumDist = FloatArray(n)
    xs[0] = stroke.points[0].x
    ys[0] = stroke.points[0].y
    cumDist[0] = 0f
    for (i in 1 until n) {
        xs[i] = stroke.points[i].x
        ys[i] = stroke.points[i].y
        cumDist[i] = cumDist[i - 1] + hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1])
    }
    val totalLen = cumDist[n - 1]

    if (totalLen < 3.5f) {
        return CachedNoteStroke(
            stroke = stroke,
            baseColor = baseColor,
            isDot = true,
            dotCenter = Offset(xs[0], ys[0]),
            dotRadius = (stroke.strokeWidth * 0.5f).coerceAtLeast(1.5f)
        )
    }

    val maxTaperFraction = 0.45f
    val frontTaperDist = (stroke.strokeWidth * 3.2f).coerceIn(12f, 48f).coerceAtMost(totalLen * maxTaperFraction)
    val backTaperDist = (stroke.strokeWidth * 3.8f).coerceIn(15f, 56f).coerceAtMost(totalLen * maxTaperFraction)

    fun getTaperFactor(d: Float): Float {
        var fFront = 1f
        if (d < frontTaperDist && frontTaperDist > 0f) {
            val r = (d / frontTaperDist).coerceIn(0f, 1f)
            fFront = r.toDouble().pow(0.85).toFloat()
        }
        var fBack = 1f
        val dBack = totalLen - d
        if (dBack < backTaperDist && backTaperDist > 0f) {
            val r = (dBack / backTaperDist).coerceIn(0f, 1f)
            fBack = r.toDouble().pow(0.85).toFloat()
        }
        return minOf(fFront, fBack).coerceIn(0.04f, 1f)
    }

    val segs = ArrayList<Pair<Path, Float>>()
    var prevX = xs[0]
    var prevY = ys[0]
    var bodyPath: Path? = null

    for (i in 1 until n) {
        val p = stroke.points[i]
        val x = xs[i]; val y = ys[i]
        val midX = (prevX + x) / 2f; val midY = (prevY + y) / 2f

        val baseW = when (stroke.penType) {
            1 -> stroke.strokeWidth * (0.35f + p.pressure * 1.35f)
            3 -> {
                val dx = x - prevX; val dy = y - prevY
                val angle = kotlin.math.atan2(dy, dx)
                val nibFactor = 0.45f + 0.55f * kotlin.math.abs(kotlin.math.sin(angle - 0.785f)).toFloat()
                stroke.strokeWidth * nibFactor * (0.5f + p.pressure * 0.8f)
            }
            else -> stroke.strokeWidth * (0.6f + p.pressure * 0.6f)
        }

        val isNearFront = cumDist[i] <= frontTaperDist * 1.2f
        val isNearBack = totalLen - cumDist[i - 1] <= backTaperDist * 1.2f

        if (isNearFront || isNearBack) {
            if (bodyPath != null) {
                segs.add(Pair(bodyPath, stroke.strokeWidth))
                bodyPath = null
            }

            val segMidDist = (cumDist[i - 1] + cumDist[i]) / 2f
            val tf = getTaperFactor(segMidDist)
            val wSeg = (baseW * tf).coerceIn(0.75f, stroke.strokeWidth * 2.2f)

            val seg = Path()
            seg.moveTo(prevX, prevY)
            if (i == 1) seg.lineTo(midX, midY) else seg.quadraticTo(prevX, prevY, midX, midY)
            segs.add(Pair(seg, wSeg))
        } else {
            if (bodyPath == null) {
                bodyPath = Path()
                bodyPath.moveTo(prevX, prevY)
            }
            bodyPath.quadraticTo(prevX, prevY, midX, midY)
        }

        prevX = x
        prevY = y
    }

    if (bodyPath != null) {
        bodyPath.lineTo(prevX, prevY)
        segs.add(Pair(bodyPath, stroke.strokeWidth))
        bodyPath = null
    }

    if (n >= 2) {
        val lastX = xs[n - 1]
        val lastY = ys[n - 1]
        val startMidX = (xs[n - 2] + lastX) / 2f
        val startMidY = (ys[n - 2] + lastY) / 2f
        val baseW = when (stroke.penType) {
            1 -> stroke.strokeWidth * (0.35f + stroke.points.last().pressure * 1.35f)
            3 -> stroke.strokeWidth * 0.8f * (0.5f + stroke.points.last().pressure * 0.8f)
            else -> stroke.strokeWidth * (0.6f + stroke.points.last().pressure * 0.6f)
        }
        val wSeg = (baseW * getTaperFactor(totalLen)).coerceIn(0.75f, stroke.strokeWidth * 2.2f)
        val lastSeg = Path()
        lastSeg.moveTo(startMidX, startMidY)
        lastSeg.lineTo(lastX, lastY)
        segs.add(Pair(lastSeg, wSeg))
    }

    return CachedNoteStroke(
        stroke = stroke,
        baseColor = baseColor,
        segments = segs
    )
}
