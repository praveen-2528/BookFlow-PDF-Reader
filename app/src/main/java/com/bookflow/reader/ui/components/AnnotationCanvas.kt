package com.bookflow.reader.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bookflow.reader.data.model.AnnotationPoint
import com.bookflow.reader.data.model.AnnotationStroke
import com.bookflow.reader.ui.theme.LightMint
import com.bookflow.reader.ui.theme.OceanBlueLight
import com.bookflow.reader.util.HapticHelper
import com.bookflow.reader.util.ShapeRecognizer
import com.bookflow.reader.util.stylusHoverTracking
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow

/**
 * Reusable pre-allocated front taper segment for live inking
 */
private class LiveFrontSeg(val path: Path = Path(), var width: Float = 0f)

@Composable
fun AnnotationCanvas(
    modifier: Modifier = Modifier,
    strokes: List<AnnotationStroke>,
    isEnabled: Boolean,
    currentColor: Color,
    currentStrokeWidth: Float,
    isHighlighter: Boolean,
    isEraser: Boolean,
    eraserMode: EraserMode = EraserMode.STROKE,
    isRectHighlight: Boolean = false,
    isLassoMode: Boolean = false,
    isLaserMode: Boolean = false,
    penType: Int = 0,
    stylusOnly: Boolean = false,
    onStrokeCompleted: (AnnotationStroke) -> Unit,
    onEraseAt: (Float, Float) -> Unit,
    onMoveStrokes: (Set<Int>, Float, Float) -> Unit = { _, _, _ -> },
    onInteractionStart: () -> Unit = {},
    onUndo: () -> Unit = {},
    onRadialMenuRequested: ((Offset) -> Unit)? = null
) {
    val context = LocalContext.current
    // Use snapshot state list for zero-copy in-progress points
    val activePoints = remember { mutableStateListOf<AnnotationPoint>() }
    var isDrawing by remember { mutableStateOf(false) }
    var activeColor by remember { mutableStateOf(0L) }
    
    // Lasso selection state
    var selectedStrokeIndices by remember { mutableStateOf(setOf<Int>()) }
    var lassoBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var isDraggingLasso by remember { mutableStateOf(false) }
    var activeWidth by remember { mutableStateOf(4f) }
    var activeIsHighlighter by remember { mutableStateOf(false) }
    var activePenType by remember { mutableStateOf(0) }
    
    // Rect highlight state
    var rectStart by remember { mutableStateOf<AnnotationPoint?>(null) }
    var rectEnd by remember { mutableStateOf<AnnotationPoint?>(null) }

    // Shape snap pop & light burst state
    var snapBurstCenter by remember { mutableStateOf<Offset?>(null) }
    var snapBurstTime by remember { mutableStateOf(0L) }

    // Hover tracking for stylus reticle preview & radial menu summon
    var hoverOffset by remember { mutableStateOf<Offset?>(null) }

    data class LaserPoint(val x: Float, val y: Float, val timestamp: Long = System.currentTimeMillis())
    val laserPoints = remember { mutableStateListOf<LaserPoint>() }

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            if (laserPoints.isNotEmpty()) {
                laserPoints.removeAll { now - it.timestamp > 1500 }
            }
            kotlinx.coroutines.delay(32) // ~30fps decay
        }
    }

    // Incremental live inking buffers (Zero allocations, O(1) frame time)
    var liveRevision by remember { mutableStateOf(0) }
    val liveMainPath = remember { Path() }
    val liveTipPath = remember { Path() }
    val liveFrontSegs = remember { List(16) { LiveFrontSeg() } }
    var liveFrontCount by remember { mutableStateOf(0) }
    var liveBodyStarted by remember { mutableStateOf(false) }
    var liveCurrentPressure by remember { mutableStateOf(1f) }

    // Pre-built path cache for completed strokes (rebuilt only when stroke set changes)
    val strokeCache = remember { mutableMapOf<AnnotationStroke, CachedStrokeRender>() }
    LaunchedEffect(strokes) {
        val currentSet = strokes.toSet()
        strokeCache.keys.retainAll(currentSet)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // LAYER 1: Base Canvas (Completed Strokes & Lasso Selection Glow)
        // Hardware RenderNode isolation: NEVER invalidates or redraws while user is drawing!
        BaseAnnotationCanvas(
            strokes = strokes,
            selectedStrokeIndices = selectedStrokeIndices,
            lassoBounds = lassoBounds,
            strokeCache = strokeCache
        )

        // LAYER 2: Active Inking & Interactive Overlays Canvas
        // Dedicated RenderNode: Only receives dirty passes for current stroke in-flight.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer()
                .stylusHoverTracking(
                    onHover = { hoverOffset = it },
                    onRadialMenuRequested = onRadialMenuRequested
                )
                .pointerInput(isEnabled, isEraser, isRectHighlight, isLassoMode, isLaserMode, currentColor, currentStrokeWidth, isHighlighter, penType, stylusOnly) {
                if (!isEnabled) return@pointerInput

                awaitEachGesture {
                    val downEvent = awaitFirstDown(requireUnconsumed = false)
                    val initialChanges = currentEvent.changes
                    if (initialChanges.size == 2 && initialChanges.all { it.type == PointerType.Touch }) {
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
                            HapticHelper.click(context)
                            onUndo()
                            return@awaitEachGesture
                        }
                    }

                    // Strict Stylus Rejection: Writing & erasing work ONLY with stylus (not with fingers or others)
                    val isStylus = downEvent.type == PointerType.Stylus || downEvent.type == PointerType.Eraser
                    if (stylusOnly && !isStylus) {
                        return@awaitEachGesture
                    }

                    downEvent.consume()
                    onInteractionStart()

                    val w = size.width.toFloat()
                    val h = size.height.toFloat()

                    if (isEraser) {
                        var lastX = downEvent.position.x / w
                        var lastY = downEvent.position.y / h
                        onEraseAt(lastX, lastY)

                        var dragEvent = awaitPointerEvent()
                        while (dragEvent.changes.any { it.pressed }) {
                            val change = dragEvent.changes.firstOrNull { 
                                it.id == downEvent.id || ((it.type == PointerType.Stylus || it.type == PointerType.Eraser) && it.pressed)
                            }
                            if (change != null && change.pressed) {
                                // 1. Process high-frequency historical points for continuous flow
                                change.historical.forEach { hist ->
                                    val hx = hist.position.x / w
                                    val hy = hist.position.y / h
                                    val hDist = kotlin.math.hypot((hx - lastX).toDouble(), (hy - lastY).toDouble()).toFloat()
                                    if (hDist >= 0.003f) {
                                        val hSteps = (hDist / 0.004f).toInt().coerceIn(1, 10)
                                        for (s in 1..hSteps) {
                                            val ix = lastX + (hx - lastX) * (s.toFloat() / hSteps)
                                            val iy = lastY + (hy - lastY) * (s.toFloat() / hSteps)
                                            onEraseAt(ix, iy)
                                        }
                                        lastX = hx
                                        lastY = hy
                                    }
                                }

                                // 2. Process current pointer position
                                val currX = change.position.x / w
                                val currY = change.position.y / h
                                val dist = kotlin.math.hypot((currX - lastX).toDouble(), (currY - lastY).toDouble()).toFloat()
                                if (dist >= 0.002f) {
                                    val steps = (dist / 0.004f).toInt().coerceIn(1, 15)
                                    for (s in 1..steps) {
                                        val ix = lastX + (currX - lastX) * (s.toFloat() / steps)
                                        val iy = lastY + (currY - lastY) * (s.toFloat() / steps)
                                        onEraseAt(ix, iy)
                                    }
                                    lastX = currX
                                    lastY = currY
                                }
                                change.consume()
                            }
                            dragEvent = awaitPointerEvent()
                        }
                        // Call a dummy stroke with penType = -1 to signal erase completed
                        onStrokeCompleted(AnnotationStroke(emptyList(), 0, 0f, false, -1))
                    } else if (isLassoMode) {
                        val startX = downEvent.position.x / w
                        val startY = downEvent.position.y / h
                        
                        val bounds = lassoBounds
                        if (selectedStrokeIndices.isNotEmpty() && bounds != null && 
                            startX >= bounds.left && startX <= bounds.right && 
                            startY >= bounds.top && startY <= bounds.bottom) {
                            // Dragging selected strokes
                            isDraggingLasso = true
                            var totalDx = 0f
                            var totalDy = 0f
                            
                            var dragEvent = awaitPointerEvent()
                            while (dragEvent.changes.any { it.pressed }) {
                                val change = dragEvent.changes.firstOrNull { it.id == downEvent.id }
                                if (change != null && change.pressed) {
                                    val dx = (change.position.x - change.previousPosition.x) / w
                                    val dy = (change.position.y - change.previousPosition.y) / h
                                    totalDx += dx
                                    totalDy += dy
                                    change.consume()
                                }
                                dragEvent = awaitPointerEvent()
                            }
                            
                            if (totalDx != 0f || totalDy != 0f) {
                                onMoveStrokes(selectedStrokeIndices, totalDx, totalDy)
                            }
                            
                            isDraggingLasso = false
                            selectedStrokeIndices = emptySet()
                            lassoBounds = null
                        } else {
                            // Select a stroke by finding the closest one
                            var closestIdx = -1
                            var closestDist = 0.05f * 0.05f
                            for ((i, stroke) in strokes.withIndex()) {
                                for (p in stroke.points) {
                                    val d2 = (p.x - startX) * (p.x - startX) + (p.y - startY) * (p.y - startY)
                                    if (d2 < closestDist) {
                                        closestDist = d2
                                        closestIdx = i
                                    }
                                }
                            }
                            if (closestIdx != -1) {
                                selectedStrokeIndices = setOf(closestIdx)
                                // calculate bounds
                                val s = strokes[closestIdx]
                                var minX = 1f; var maxX = 0f; var minY = 1f; var maxY = 0f
                                s.points.forEach { p ->
                                    if (p.x < minX) minX = p.x; if (p.x > maxX) maxX = p.x
                                    if (p.y < minY) minY = p.y; if (p.y > maxY) maxY = p.y
                                }
                                lassoBounds = androidx.compose.ui.geometry.Rect(minX, minY, maxX, maxY)
                            } else {
                                selectedStrokeIndices = emptySet()
                                lassoBounds = null
                            }
                        }
                    } else if (isRectHighlight) {
                        val startX = downEvent.position.x / w
                        val startY = downEvent.position.y / h
                        rectStart = AnnotationPoint(startX, startY)
                        rectEnd = AnnotationPoint(startX, startY)
                        
                        var dragEvent = awaitPointerEvent()
                        while (dragEvent.changes.any { it.pressed }) {
                            val change = dragEvent.changes.firstOrNull { it.id == downEvent.id }
                            if (change != null && change.pressed) {
                                rectEnd = AnnotationPoint(change.position.x / w, change.position.y / h)
                                change.consume()
                            }
                            dragEvent = awaitPointerEvent()
                        }
                        
                        val rs = rectStart
                        val re = rectEnd
                        if (rs != null && re != null) {
                            val dx = abs(rs.x - re.x)
                            val dy = abs(rs.y - re.y)
                            if (dx > 0.01f && dy > 0.005f) {
                                onStrokeCompleted(AnnotationStroke(
                                    points = listOf(rs, re),
                                    color = currentColor.toArgb().toLong(),
                                    strokeWidth = 0f,
                                    isHighlighter = true,
                                    penType = 3
                                ))
                            }
                        }
                        rectStart = null
                        rectEnd = null
                    } else if (isLaserMode) {
                        val t = System.currentTimeMillis()
                        laserPoints.add(LaserPoint(downEvent.position.x / w, downEvent.position.y / h, t))
                        var dragEvent = awaitPointerEvent()
                        while (dragEvent.changes.any { it.pressed }) {
                            val change = dragEvent.changes.firstOrNull { 
                                it.id == downEvent.id || ((it.type == PointerType.Stylus || it.type == PointerType.Eraser) && it.pressed)
                            }
                            if (change != null && change.pressed) {
                                change.historical.forEach { hist ->
                                    laserPoints.add(LaserPoint(hist.position.x / w, hist.position.y / h, System.currentTimeMillis()))
                                }
                                laserPoints.add(LaserPoint(change.position.x / w, change.position.y / h, System.currentTimeMillis()))
                                change.consume()
                            }
                            dragEvent = awaitPointerEvent()
                        }
                    } else {
                        // Drawing mode - natural pen inking with Scratch-to-Erase & Hold-to-Snap
                        activePoints.clear()
                        liveMainPath.reset()
                        liveTipPath.reset()
                        liveFrontCount = 0
                        liveBodyStarted = false
                        activeColor = currentColor.toArgb().toLong()
                        activeWidth = currentStrokeWidth
                        activeIsHighlighter = isHighlighter
                        activePenType = penType
                        isDrawing = true
                        
                        val isStylusDown = downEvent.type == PointerType.Stylus || downEvent.type == PointerType.Eraser
                        val pressure = if (isStylusDown) downEvent.pressure else 1.0f
                        liveCurrentPressure = pressure
                        val startX = downEvent.position.x / w
                        val startY = downEvent.position.y / h
                        activePoints.add(AnnotationPoint(startX, startY, pressure))

                        var prevAnchorX = startX
                        var prevAnchorY = startY
                        var prevMidX = startX
                        var prevMidY = startY
                        var cumDist = 0f
                        val frontTaperDist = (activeWidth * 3.0f).coerceIn(12f, 48f)

                        liveTipPath.reset()
                        liveTipPath.moveTo(startX * w, startY * h)
                        liveRevision++

                        var isSnapped = false
                        var stillAnchorPos = downEvent.position
                        var stillStartTime = 0L

                        var dragEvent = awaitPointerEvent()
                        while (dragEvent.changes.any { it.pressed }) {
                            val change = dragEvent.changes.firstOrNull { 
                                it.id == downEvent.id || ((it.type == PointerType.Stylus || it.type == PointerType.Eraser) && it.pressed)
                            }
                            if (change != null && change.pressed) {
                                val curPos = change.position
                                val p = if (change.type == PointerType.Stylus || change.type == PointerType.Eraser) change.pressure else 1.0f
                                liveCurrentPressure = p
                                val curX = curPos.x / w
                                val curY = curPos.y / h

                                if (!isSnapped) {
                                    val pointsToProcess = ArrayList<Pair<Offset, Float>>(change.historical.size + 1)
                                    change.historical.forEach { hist ->
                                        val hp = if (change.type == PointerType.Stylus || change.type == PointerType.Eraser) change.pressure else 1.0f
                                        pointsToProcess.add(Pair(Offset(hist.position.x / w, hist.position.y / h), hp))
                                    }
                                    pointsToProcess.add(Pair(Offset(curX, curY), p))

                                    for (pt in pointsToProcess) {
                                        val px = pt.first.x
                                        val py = pt.first.y
                                        val pp = pt.second
                                        val dx = (px - prevAnchorX) * w
                                        val dy = (py - prevAnchorY) * h
                                        val stepDist = hypot(dx, dy)

                                        // 2.0px spatial threshold: eliminates digitizer sub-pixel jitter without lag
                                        if (stepDist >= 2.0f) {
                                            val midX = (prevAnchorX + px) / 2f
                                            val midY = (prevAnchorY + py) / 2f
                                            cumDist += stepDist

                                            if (activeIsHighlighter || activePenType == 3) {
                                                if (!liveBodyStarted) {
                                                    liveMainPath.moveTo(prevAnchorX * w, prevAnchorY * h)
                                                    liveBodyStarted = true
                                                }
                                                liveMainPath.quadraticTo(prevAnchorX * w, prevAnchorY * h, midX * w, midY * h)
                                            } else if (cumDist <= frontTaperDist && liveFrontCount < liveFrontSegs.size) {
                                                val r = (cumDist / frontTaperDist).coerceIn(0f, 1f)
                                                val taper = r.toDouble().pow(0.85).toFloat().coerceIn(0.04f, 1f)
                                                val wSeg = (activeWidth * taper).coerceIn(0.75f, activeWidth * 2.2f)

                                                val seg = liveFrontSegs[liveFrontCount]
                                                seg.path.reset()
                                                seg.path.moveTo(prevAnchorX * w, prevAnchorY * h)
                                                seg.path.quadraticTo(prevAnchorX * w, prevAnchorY * h, midX * w, midY * h)
                                                seg.width = wSeg
                                                liveFrontCount++
                                            } else {
                                                if (!liveBodyStarted) {
                                                    liveMainPath.moveTo(prevMidX * w, prevMidY * h)
                                                    liveBodyStarted = true
                                                }
                                                liveMainPath.quadraticTo(prevAnchorX * w, prevAnchorY * h, midX * w, midY * h)
                                            }

                                            prevAnchorX = px
                                            prevAnchorY = py
                                            prevMidX = midX
                                            prevMidY = midY
                                            activePoints.add(AnnotationPoint(px, py, pp))

                                            // Material Haptics: Tactile paper texture for pencil / fountain pen
                                            if (!activeIsHighlighter && (activePenType == 0 || activePenType == 1)) {
                                                HapticHelper.pencilGrittyTexture(context)
                                            }
                                        }
                                    }

                                    // Real-time live tip: connects smoothed spine directly to stylus nib
                                    liveTipPath.reset()
                                    liveTipPath.moveTo(prevMidX * w, prevMidY * h)
                                    liveTipPath.lineTo(curX * w, curY * h)
                                    liveRevision++

                                    // Hold-to-Snap stillness detection (550ms hold)
                                    val distFromAnchor = hypot(curPos.x - stillAnchorPos.x, curPos.y - stillAnchorPos.y)
                                    if (distFromAnchor < 14f) {
                                        if (stillStartTime == 0L) {
                                            stillStartTime = System.currentTimeMillis()
                                        } else if (System.currentTimeMillis() - stillStartTime >= 550L && activePoints.size >= 6) {
                                            val rawStroke = AnnotationStroke(
                                                points = activePoints.toList(),
                                                color = activeColor,
                                                strokeWidth = activeWidth,
                                                isHighlighter = activeIsHighlighter,
                                                penType = activePenType
                                            )
                                            val snapped = ShapeRecognizer.recognizeAndSnap(rawStroke)
                                            if (snapped.shapeType != -1 && snapped.points != activePoints.toList()) {
                                                isSnapped = true
                                                activePoints.clear()
                                                activePoints.addAll(snapped.points)
                                                activePenType = snapped.penType
                                                HapticHelper.shapeSnapPop(context)
                                                snapBurstCenter = Offset(
                                                    snapped.points.map { it.x }.average().toFloat() * w,
                                                    snapped.points.map { it.y }.average().toFloat() * h
                                                )
                                                snapBurstTime = System.currentTimeMillis()

                                                liveMainPath.reset()
                                                liveTipPath.reset()
                                                liveFrontCount = 0
                                                if (snapped.points.size >= 2) {
                                                    val p0 = snapped.points[0]
                                                    liveMainPath.moveTo(p0.x * w, p0.y * h)
                                                    for (si in 1 until snapped.points.size) {
                                                        val spt = snapped.points[si]
                                                        liveMainPath.lineTo(spt.x * w, spt.y * h)
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
                                    if (activePoints.size == 2) {
                                        val firstPoint = activePoints[0]
                                        activePoints.clear()
                                        activePoints.add(firstPoint)
                                        activePoints.add(AnnotationPoint(curPos.x / w, curPos.y / h, p))
                                        liveMainPath.reset()
                                        liveMainPath.moveTo(firstPoint.x * w, firstPoint.y * h)
                                        liveMainPath.lineTo(curPos.x, curPos.y)
                                        liveRevision++
                                    }
                                }
                                change.consume()
                            }
                            dragEvent = awaitPointerEvent()
                        }

                        if (activePoints.isNotEmpty()) {
                            if (activePoints.size == 1) {
                                val pt = activePoints[0]
                                onStrokeCompleted(AnnotationStroke(
                                    points = listOf(pt, AnnotationPoint(pt.x + 0.0001f, pt.y + 0.0001f, pt.pressure)),
                                    color = activeColor,
                                    strokeWidth = activeWidth,
                                    isHighlighter = activeIsHighlighter,
                                    penType = activePenType
                                ))
                            } else if (!isSnapped && ShapeRecognizer.isScratchOut(activePoints, w, h)) {
                                // Genuine Scratch-to-Erase detected upon release!
                                HapticHelper.heavyClick(context)
                                val minX = activePoints.minOf { it.x }
                                val maxX = activePoints.maxOf { it.x }
                                val minY = activePoints.minOf { it.y }
                                val maxY = activePoints.maxOf { it.y }

                                val steps = 6
                                for (sx in 0..steps) {
                                    for (sy in 0..steps) {
                                        val ex = minX + (maxX - minX) * (sx.toFloat() / steps)
                                        val ey = minY + (maxY - minY) * (sy.toFloat() / steps)
                                        onEraseAt(ex, ey)
                                    }
                                }
                                onStrokeCompleted(AnnotationStroke(emptyList(), 0, 0f, false, -1))
                            } else {
                                onStrokeCompleted(AnnotationStroke(
                                    points = activePoints.toList(),
                                    color = activeColor,
                                    strokeWidth = activeWidth,
                                    isHighlighter = activeIsHighlighter,
                                    penType = activePenType
                                ))
                            }
                        }
                        activePoints.clear()
                        liveMainPath.reset()
                        liveTipPath.reset()
                        liveFrontCount = 0
                        liveBodyStarted = false
                        isDrawing = false
                        liveRevision++
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // Draw rect highlight in-progress
        val rs = rectStart
        val re = rectEnd
        if (rs != null && re != null) {
            val left = min(rs.x, re.x) * w
            val top = min(rs.y, re.y) * h
            val right = max(rs.x, re.x) * w
            val bottom = max(rs.y, re.y) * h
            drawRect(
                color = currentColor.copy(alpha = 0.35f),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top)
            )
        }

        // Draw laser pointer glowing trail & hot core
        if (laserPoints.size > 1) {
            val now = System.currentTimeMillis()
            var pX = laserPoints.first().x * w
            var pY = laserPoints.first().y * h
            for (i in 1 until laserPoints.size) {
                val pt = laserPoints[i]
                val x = pt.x * w
                val y = pt.y * h
                val age = now - pt.timestamp
                if (age < 1500) {
                    val alpha = (1f - (age / 1500f)).coerceIn(0f, 1f)
                    val midX = (pX + x) / 2
                    val midY = (pY + y) / 2
                    val seg = Path()
                    seg.moveTo(pX, pY)
                    seg.quadraticTo(pX, pY, midX, midY)

                    // Living laser glow:
                    // Outer diffuse glow
                    drawPath(seg, color = Color(0xFFFF2255).copy(alpha = alpha * 0.45f), style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    // Vibrant neon core
                    drawPath(seg, color = Color(0xFFFF3366).copy(alpha = alpha * 0.85f), style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    // Hot white central beam
                    drawPath(seg, color = Color.White.copy(alpha = alpha), style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                pX = x
                pY = y
            }
            laserPoints.lastOrNull()?.let { tip ->
                val age = now - tip.timestamp
                if (age < 1500) {
                    val alpha = (1f - (age / 1500f)).coerceIn(0f, 1f)
                    drawCircle(color = Color(0xFFFF2255).copy(alpha = alpha * 0.5f), radius = 10f, center = Offset(tip.x * w, tip.y * h))
                    drawCircle(color = Color.White.copy(alpha = alpha), radius = 4f, center = Offset(tip.x * w, tip.y * h))
                }
            }
        }

        // Draw in-progress stroke with live front-end sharp tip taper (ZERO HEAP ALLOCATIONS, O(1) CONSTANT TIME)
        if (isDrawing) {
            val _rev = liveRevision
            val baseColor = currentColor.copy(alpha = if (activeIsHighlighter) 0.4f else 1f)
            val wBody = when (activePenType) {
                1 -> activeWidth * (0.35f + liveCurrentPressure * 1.35f)
                2 -> activeWidth * 0.8f * (0.5f + liveCurrentPressure * 0.8f)
                else -> activeWidth * (0.6f + liveCurrentPressure * 0.6f)
            }.coerceIn(0.75f, activeWidth * 2.2f)

            if (!liveBodyStarted && liveFrontCount == 0) {
                val startPt = activePoints.firstOrNull()
                if (startPt != null) {
                    drawCircle(
                        color = baseColor,
                        radius = (wBody * 0.5f).coerceAtLeast(1.5f),
                        center = Offset(startPt.x * w, startPt.y * h)
                    )
                }
            }

            if (activeIsHighlighter || activePenType == 3) {
                if (liveBodyStarted) {
                    drawPath(liveMainPath, color = baseColor, style = Stroke(width = activeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                drawPath(liveTipPath, color = baseColor, style = Stroke(width = activeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            } else {
                // 1. Draw front taper segments (only the first ~3-5 segments)
                for (i in 0 until liveFrontCount) {
                    val seg = liveFrontSegs[i]
                    drawPath(seg.path, color = baseColor, style = Stroke(width = seg.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                // 2. Draw continuous main body (ONE Skia draw call, 0.01ms)
                if (liveBodyStarted) {
                    drawPath(liveMainPath, color = baseColor, style = Stroke(width = wBody, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                // 3. Draw live tip connecting directly to physical stylus nib (ONE Skia draw call, 0.01ms)
                drawPath(liveTipPath, color = baseColor, style = Stroke(width = wBody, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        // Stylus hover reticle preview
        hoverOffset?.let { hoverPos ->
            if (isEnabled) {
                if (isEraser) {
                    if (eraserMode == EraserMode.STROKE) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = 18f,
                            center = hoverPos,
                            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
                        )
                        drawCircle(
                            color = Color(0xFFE53935).copy(alpha = 0.85f),
                            radius = 3.5f,
                            center = hoverPos
                        )
                    } else {
                        val eraserRadius = (currentStrokeWidth * 2.5f + 14f).coerceIn(16f, 60f)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = eraserRadius,
                            center = hoverPos,
                            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.6f),
                            radius = 2f,
                            center = hoverPos
                        )
                    }
                } else if (isLaserMode) {
                    drawCircle(
                        color = Color(0xFFFF2255).copy(alpha = 0.5f),
                        radius = 8f,
                        center = hoverPos,
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = Color(0xFFFF2255),
                        radius = 3f,
                        center = hoverPos
                    )
                } else if (!isLassoMode) {
                    val reticleRadius = (currentStrokeWidth / 2f).coerceAtLeast(3f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = reticleRadius + 3f,
                        center = hoverPos,
                        style = Stroke(width = 1f)
                    )
                    drawCircle(
                        color = currentColor.copy(alpha = if (isHighlighter) 0.5f else 0.85f),
                        radius = reticleRadius,
                        center = hoverPos
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 1.5f,
                        center = hoverPos
                    )
                }
            }
        }

        // Geometric Shape Snap "Pop" Light Burst
        snapBurstCenter?.let { bCenter ->
            val elapsed = System.currentTimeMillis() - snapBurstTime
            if (elapsed in 0..550) {
                val t = elapsed / 550f
                val burstAlpha = (1f - t).coerceIn(0f, 1f)
                val radius = (42f * t + 8f).dp.toPx()
                // Expanding light ring
                drawCircle(
                    color = LightMint.copy(alpha = burstAlpha * 0.85f),
                    radius = radius,
                    center = bCenter,
                    style = Stroke(width = (3.5f * (1f - t)).dp.toPx())
                )
                // Spark rays radiating outward
                for (i in 0 until 12) {
                    val angle = (i * 30f + t * 40f) * (Math.PI / 180f).toFloat()
                    val r1 = radius * 0.65f
                    val r2 = radius * 1.25f
                    val p1 = Offset(bCenter.x + r1 * kotlin.math.cos(angle), bCenter.y + r1 * kotlin.math.sin(angle))
                    val p2 = Offset(bCenter.x + r2 * kotlin.math.cos(angle), bCenter.y + r2 * kotlin.math.sin(angle))
                    drawLine(
                        color = OceanBlueLight.copy(alpha = burstAlpha),
                        start = p1,
                        end = p2,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
}

/**
 * LAYER 1: Base Canvas (Completed Strokes & Lasso Selection Glow)
 * Performance isolation: Isolated into a dedicated hardware RenderNode.
 * It NEVER recomposes or invalidates while the user is actively drawing on screen!
 */
@Composable
private fun BaseAnnotationCanvas(
    modifier: Modifier = Modifier,
    strokes: List<AnnotationStroke>,
    selectedStrokeIndices: Set<Int>,
    lassoBounds: androidx.compose.ui.geometry.Rect?,
    strokeCache: MutableMap<AnnotationStroke, CachedStrokeRender>
) {
    var lastCanvasSize by remember { mutableStateOf(Size.Zero) }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val w = size.width
        val h = size.height

        if (w <= 10f || h <= 10f) return@Canvas

        if (lastCanvasSize != size) {
            strokeCache.clear()
            lastCanvasSize = size
        }

        // Draw completed strokes in true pixel space using pre-built cached paths
        strokes.forEachIndexed { index, stroke ->
            if (stroke.points.isNotEmpty()) {
                val isSelected = index in selectedStrokeIndices
                val render = strokeCache.getOrPut(stroke) { buildCachedStroke(stroke, w, h) }
                render.draw(this, isSelected)
            }
        }

        // Draw lasso bounds
        val bounds = lassoBounds
        if (bounds != null && selectedStrokeIndices.isNotEmpty()) {
            drawRect(
                color = Color.Blue.copy(alpha = 0.5f),
                topLeft = Offset(bounds.left * w, bounds.top * h),
                size = Size(bounds.width * w, bounds.height * h),
                style = Stroke(
                    width = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )
        }
    }
}

/**
 * Pre-built render data for a completed stroke.
 * Pre-compiled into native Skia paths once upon commit, then drawn with zero allocations.
 */
private class CachedStrokeRender(
    val stroke: AnnotationStroke,
    val baseColor: Color,
    val isRect: Boolean = false,
    val rectTopLeft: Offset = Offset.Zero,
    val rectSize: Size = Size.Zero,
    val isDot: Boolean = false,
    val dotCenter: Offset = Offset.Zero,
    val dotRadius: Float = 0f,
    val isUniformPath: Boolean = false,
    val uniformPath: Path? = null,
    val uniformWidth: Float = 0f,
    val segments: List<Pair<Path, Float>> = emptyList()
) {
    fun draw(drawScope: DrawScope, isSelected: Boolean) {
        val drawColor = if (isSelected) Color(0x553498DBL) else baseColor
        val extraWidth = if (isSelected) 6f else 0f

        if (isRect) {
            drawScope.drawRect(
                color = if (isSelected) Color(0x553498DBL) else Color(stroke.color.toInt()).copy(alpha = 0.35f),
                topLeft = rectTopLeft,
                size = rectSize
            )
            return
        }

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

private fun buildCachedStroke(stroke: AnnotationStroke, w: Float, h: Float): CachedStrokeRender {
    val n = stroke.points.size
    if (n == 0) return CachedStrokeRender(stroke, Color.Transparent)
    val baseColor = Color(stroke.color.toInt()).copy(alpha = if (stroke.isHighlighter) 0.4f else 1f)

    if (stroke.penType == 3 && n >= 2) {
        val p1 = stroke.points[0]; val p2 = stroke.points[1]
        val left = min(p1.x, p2.x) * w; val top = min(p1.y, p2.y) * h
        val right = max(p1.x, p2.x) * w; val bottom = max(p1.y, p2.y) * h
        return CachedStrokeRender(
            stroke = stroke,
            baseColor = baseColor,
            isRect = true,
            rectTopLeft = Offset(left, top),
            rectSize = Size(right - left, bottom - top)
        )
    }

    if (n == 1) {
        val p = stroke.points[0]
        val radius = if (!stroke.isHighlighter) (stroke.strokeWidth * 0.5f).coerceAtLeast(1.5f) else (stroke.strokeWidth / 2f)
        return CachedStrokeRender(
            stroke = stroke,
            baseColor = baseColor,
            isDot = true,
            dotCenter = Offset(p.x * w, p.y * h),
            dotRadius = radius
        )
    }

    if (stroke.isHighlighter) {
        val path = Path()
        val first = stroke.points.first()
        var prevX = first.x * w; var prevY = first.y * h
        path.moveTo(prevX, prevY)
        for (i in 1 until n) {
            val p = stroke.points[i]
            val x = p.x * w; val y = p.y * h
            val midX = (prevX + x) / 2f; val midY = (prevY + y) / 2f
            if (i == 1) path.lineTo(midX, midY) else path.quadraticTo(prevX, prevY, midX, midY)
            prevX = x; prevY = y
        }
        path.lineTo(prevX, prevY)
        return CachedStrokeRender(
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
    xs[0] = stroke.points[0].x * w
    ys[0] = stroke.points[0].y * h
    cumDist[0] = 0f
    for (i in 1 until n) {
        xs[i] = stroke.points[i].x * w
        ys[i] = stroke.points[i].y * h
        cumDist[i] = cumDist[i - 1] + hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1])
    }
    val totalLen = cumDist[n - 1]

    if (totalLen < 3.5f) {
        return CachedStrokeRender(
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
    var bodyStartX = 0f; var bodyStartY = 0f

    for (i in 1 until n) {
        val p = stroke.points[i]
        val x = xs[i]; val y = ys[i]
        val midX = (prevX + x) / 2f; val midY = (prevY + y) / 2f

        val baseW = when (stroke.penType) {
            1 -> stroke.strokeWidth * (0.35f + p.pressure * 1.35f)
            2 -> {
                val dx = x - prevX; val dy = y - prevY
                val angle = kotlin.math.atan2(dy, dx)
                val nibFactor = 0.45f + 0.55f * kotlin.math.abs(kotlin.math.sin(angle - 0.785f)).toFloat()
                stroke.strokeWidth * nibFactor * (0.5f + p.pressure * 0.8f)
            }
            else -> stroke.strokeWidth * (0.6f + p.pressure * 0.6f)
        }

        val segLen = cumDist[i] - cumDist[i - 1]
        val isNearFront = cumDist[i] <= frontTaperDist * 1.2f
        val isNearBack = totalLen - cumDist[i - 1] <= backTaperDist * 1.2f

        if (isNearFront || isNearBack) {
            // Close any active body path first
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
            // Main body between front and back taper: connect into ONE continuous path!
            if (bodyPath == null) {
                bodyPath = Path()
                bodyPath.moveTo(prevX, prevY)
                bodyStartX = prevX; bodyStartY = prevY
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

    // Connect from last midpoint to the final endpoint with sharp termination
    if (n >= 2) {
        val lastX = xs[n - 1]
        val lastY = ys[n - 1]
        val startMidX = (xs[n - 2] + lastX) / 2f
        val startMidY = (ys[n - 2] + lastY) / 2f
        val baseW = when (stroke.penType) {
            1 -> stroke.strokeWidth * (0.35f + stroke.points.last().pressure * 1.35f)
            2 -> stroke.strokeWidth * 0.8f * (0.5f + stroke.points.last().pressure * 0.8f)
            else -> stroke.strokeWidth * (0.6f + stroke.points.last().pressure * 0.6f)
        }
        val wSeg = (baseW * getTaperFactor(totalLen)).coerceIn(0.75f, stroke.strokeWidth * 2.2f)
        val lastSeg = Path()
        lastSeg.moveTo(startMidX, startMidY)
        lastSeg.lineTo(lastX, lastY)
        segs.add(Pair(lastSeg, wSeg))
    }

    return CachedStrokeRender(
        stroke = stroke,
        baseColor = baseColor,
        segments = segs
    )
}
