package com.bookflow.reader.util

import com.bookflow.reader.data.model.AnnotationPoint
import com.bookflow.reader.data.model.AnnotationStroke
import kotlin.math.*

object ShapeRecognizer {

    /**
     * Detects if the current user stroke is a scratch-out / scribble gesture
     * intended to erase underlying annotations or mistake.
     *
     * Criteria:
     * 1. At least 3-4 acute directional reversals (zigzagging back and forth).
     * 2. Stroke density is high (total path length is significantly greater than
     *    the bounding box diagonal, meaning dense local scribbling rather than cursive writing).
     */
    fun isScratchOut(points: List<AnnotationPoint>, widthPx: Float = 1f, heightPx: Float = 1f): Boolean {
        // A genuine scratch-out scribble requires multiple rapid zigzags
        if (points.size < 20) return false

        // Project into screen space coordinates
        val pts = points.map {
            val px = if (widthPx > 1f) it.x * widthPx else it.x
            val py = if (heightPx > 1f) it.y * heightPx else it.y
            px to py
        }

        val minX = pts.minOf { it.first }
        val maxX = pts.maxOf { it.first }
        val minY = pts.minOf { it.second }
        val maxY = pts.maxOf { it.second }
        val bbW = maxX - minX
        val bbH = maxY - minY
        val bbDiag = sqrt(bbW * bbW + bbH * bbH)

        // Must be a scribble sized region (20px to 450px)
        if (bbDiag < 20f || bbDiag > 450f) return false

        // Downsample points to filter sensor jitter (minimum 8px step)
        val sampled = mutableListOf(pts.first())
        for (i in 1 until pts.size) {
            val prev = sampled.last()
            val cur = pts[i]
            val d = hypot(cur.first - prev.first, cur.second - prev.second)
            if (d >= 8f) {
                sampled.add(cur)
            }
        }
        if (sampled.size < 14) return false

        var hReversals = 0
        var vReversals = 0
        var lastDxSign = 0
        var lastDySign = 0
        var totalPath = 0f

        for (i in 1 until sampled.size) {
            val dx = sampled[i].first - sampled[i - 1].first
            val dy = sampled[i].second - sampled[i - 1].second
            totalPath += hypot(dx, dy)

            if (abs(dx) > 10f) {
                val sign = if (dx > 0) 1 else -1
                if (lastDxSign != 0 && sign != lastDxSign) {
                    hReversals++
                }
                lastDxSign = sign
            }
            if (abs(dy) > 10f) {
                val sign = if (dy > 0) 1 else -1
                if (lastDySign != 0 && sign != lastDySign) {
                    vReversals++
                }
                lastDySign = sign
            }
        }

        val maxReversals = maxOf(hReversals, vReversals)
        // A real scratch-out scribble has at least 6 directional reversals (3 full back-and-forth cycles)
        // and very high stroke density over the same small bounding region.
        val isDense = totalPath > bbDiag * 4.0f
        return maxReversals >= 6 && isDense
    }

    /**
     * Recognizes geometric shapes (line, arrow, circle, rectangle, triangle)
     * and snaps the stroke into clean vector geometry.
     * Works seamlessly in both normalized [0..1] and screen pixel coordinates.
     */
    fun recognizeAndSnap(stroke: AnnotationStroke): AnnotationStroke {
        if (stroke.points.size < 3) return stroke

        val points = stroke.points
        val pathLength = totalPathLength(points)

        // Check whether coordinates are normalized [0..1] or pixel space
        val isNormalized = points.all { it.x in -0.1f..1.1f && it.y in -0.1f..1.1f }
        val minPathLength = if (isNormalized) 0.02f else 15f
        if (pathLength < minPathLength) return stroke

        val isClosedShape = isClosedLoop(points, pathLength)

        return when {
            isClosedShape && isCircle(points, isNormalized) -> snapToCircle(stroke)
            isClosedShape && isTriangle(points, isNormalized) -> snapToTriangle(stroke)
            isClosedShape && isRectangleLike(points, isNormalized) -> snapToRectangle(stroke)
            isArrow(points, pathLength, isNormalized) -> snapToArrow(stroke)
            isLine(points, pathLength, isNormalized) -> snapToLine(stroke)
            isClosedShape -> snapToCircle(stroke)
            else -> stroke
        }
    }

    private fun isClosedLoop(points: List<AnnotationPoint>, pathLength: Float): Boolean {
        if (points.size < 5) return false
        val first = points.first()
        val last = points.last()
        val dist = distance(first.x, first.y, last.x, last.y)
        return dist < pathLength * 0.28f
    }

    private fun isLine(points: List<AnnotationPoint>, pathLength: Float, isNormalized: Boolean): Boolean {
        if (points.size < 2) return false
        val first = points.first()
        val last = points.last()
        val directDist = distance(first.x, first.y, last.x, last.y)
        val minDirect = if (isNormalized) 0.015f else 12f
        return directDist > minDirect && pathLength < directDist * 1.35f
    }

    private fun isArrow(points: List<AnnotationPoint>, pathLength: Float, isNormalized: Boolean): Boolean {
        if (points.size < 6) return false
        val minDirect = if (isNormalized) 0.03f else 25f
        val first = points.first()
        val last = points.last()
        val directDist = distance(first.x, first.y, last.x, last.y)
        if (directDist < minDirect) return false

        // Arrow head detection: check if stroke ends with an acute hook/barb pointing backwards
        val tailIndex = (points.size * 0.75f).toInt().coerceIn(1, points.size - 2)
        val shaftDirX = points[tailIndex].x - first.x
        val shaftDirY = points[tailIndex].y - first.y
        val headDirX = last.x - points[tailIndex].x
        val headDirY = last.y - points[tailIndex].y

        val dot = shaftDirX * headDirX + shaftDirY * headDirY
        val len1 = hypot(shaftDirX, shaftDirY)
        val len2 = hypot(headDirX, headDirY)
        if (len1 > 0f && len2 > 0f) {
            val cosAngle = dot / (len1 * len2)
            // Hook turns sharply backward (cos < -0.3 means > 107 degrees turn)
            if (cosAngle < -0.3f && pathLength < directDist * 2.2f) {
                return true
            }
        }
        return false
    }

    private fun isCircle(points: List<AnnotationPoint>, isNormalized: Boolean): Boolean {
        val cx = points.map { it.x }.average().toFloat()
        val cy = points.map { it.y }.average().toFloat()
        val radii = points.map { distance(it.x, it.y, cx, cy) }
        val avgRadius = radii.average().toFloat()
        val minRadius = if (isNormalized) 0.008f else 6f
        if (avgRadius < minRadius) return false
        val variance = radii.map { (it - avgRadius).pow(2) }.average()
        val stdDev = sqrt(variance)
        return stdDev / avgRadius < 0.35f
    }

    private fun isRectangleLike(points: List<AnnotationPoint>, isNormalized: Boolean): Boolean {
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        val w = maxX - minX
        val h = maxY - minY
        val minDim = if (isNormalized) 0.015f else 12f
        if (w < minDim || h < minDim) return false

        val edgeThreshold = maxOf(w, h) * 0.22f
        var nearEdgeCount = 0
        points.forEach { p ->
            val nearLeft = abs(p.x - minX) < edgeThreshold
            val nearRight = abs(p.x - maxX) < edgeThreshold
            val nearTop = abs(p.y - minY) < edgeThreshold
            val nearBottom = abs(p.y - maxY) < edgeThreshold
            if (nearLeft || nearRight || nearTop || nearBottom) nearEdgeCount++
        }
        return nearEdgeCount.toFloat() / points.size > 0.68f
    }

    private fun isTriangle(points: List<AnnotationPoint>, isNormalized: Boolean): Boolean {
        if (points.size < 6) return false
        val minDim = if (isNormalized) 0.02f else 15f
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        if ((maxX - minX) < minDim || (maxY - minY) < minDim) return false

        // Detect corners: find 3 most extreme points farthest from the centroid
        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        val distances = points.map { distance(it.x, it.y, cx, cy) }
        val peaks = mutableListOf<Int>()
        val step = maxOf(2, points.size / 12)
        for (i in step until points.size - step) {
            val cur = distances[i]
            if (cur > distances[i - step] && cur > distances[i + step]) {
                peaks.add(i)
            }
        }
        return peaks.size in 3..4
    }

    private fun snapToLine(stroke: AnnotationStroke): AnnotationStroke {
        val first = stroke.points.first()
        val last = stroke.points.last()
        return stroke.copy(
            points = listOf(first, last),
            penType = 4,
            shapeType = 0
        )
    }

    private fun snapToArrow(stroke: AnnotationStroke): AnnotationStroke {
        val first = stroke.points.first()
        // Calculate main shaft direction
        val tailIndex = (stroke.points.size * 0.75f).toInt().coerceIn(1, stroke.points.size - 1)
        val tip = stroke.points[tailIndex]

        val dx = tip.x - first.x
        val dy = tip.y - first.y
        val shaftLen = hypot(dx, dy)
        if (shaftLen < 0.001f) return snapToLine(stroke)

        val barbLen = shaftLen * 0.22f
        val angle = atan2(dy, dx)
        val barbAngle1 = angle + Math.PI.toFloat() * 0.82f // 147 degrees back
        val barbAngle2 = angle - Math.PI.toFloat() * 0.82f

        val barb1 = AnnotationPoint(
            x = tip.x + barbLen * cos(barbAngle1),
            y = tip.y + barbLen * sin(barbAngle1)
        )
        val barb2 = AnnotationPoint(
            x = tip.x + barbLen * cos(barbAngle2),
            y = tip.y + barbLen * sin(barbAngle2)
        )

        // Path: start -> tip -> barb1 -> tip -> barb2
        val arrowPoints = listOf(first, tip, barb1, tip, barb2)
        return stroke.copy(
            points = arrowPoints,
            penType = 4,
            shapeType = 4
        )
    }

    private fun snapToCircle(stroke: AnnotationStroke): AnnotationStroke {
        val cx = stroke.points.map { it.x }.average().toFloat()
        val cy = stroke.points.map { it.y }.average().toFloat()
        val avgRadius = stroke.points.map { distance(it.x, it.y, cx, cy) }.average().toFloat()

        val circlePoints = (0..36).map { i ->
            val angle = (i * 10.0 * PI / 180.0)
            AnnotationPoint(
                x = cx + avgRadius * cos(angle).toFloat(),
                y = cy + avgRadius * sin(angle).toFloat()
            )
        }
        return stroke.copy(
            points = circlePoints,
            penType = 4,
            shapeType = 1
        )
    }

    private fun snapToRectangle(stroke: AnnotationStroke): AnnotationStroke {
        val minX = stroke.points.minOf { it.x }
        val maxX = stroke.points.maxOf { it.x }
        val minY = stroke.points.minOf { it.y }
        val maxY = stroke.points.maxOf { it.y }

        val rectPoints = listOf(
            AnnotationPoint(minX, minY),
            AnnotationPoint(maxX, minY),
            AnnotationPoint(maxX, maxY),
            AnnotationPoint(minX, maxY),
            AnnotationPoint(minX, minY)
        )
        return stroke.copy(
            points = rectPoints,
            penType = 4,
            shapeType = 2
        )
    }

    private fun snapToTriangle(stroke: AnnotationStroke): AnnotationStroke {
        val minX = stroke.points.minOf { it.x }
        val maxX = stroke.points.maxOf { it.x }
        val minY = stroke.points.minOf { it.y }
        val maxY = stroke.points.maxOf { it.y }
        val midX = (minX + maxX) / 2f

        val triPoints = listOf(
            AnnotationPoint(midX, minY),
            AnnotationPoint(maxX, maxY),
            AnnotationPoint(minX, maxY),
            AnnotationPoint(midX, minY)
        )
        return stroke.copy(
            points = triPoints,
            penType = 4,
            shapeType = 3
        )
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
    }

    private fun totalPathLength(points: List<AnnotationPoint>): Float {
        var len = 0f
        for (i in 1 until points.size) {
            len += distance(points[i - 1].x, points[i - 1].y, points[i].x, points[i].y)
        }
        return len
    }
}
