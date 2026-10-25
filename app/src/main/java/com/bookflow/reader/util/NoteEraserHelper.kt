package com.bookflow.reader.util

import com.bookflow.reader.data.model.AnnotationPoint
import com.bookflow.reader.data.model.AnnotationStroke
import com.bookflow.reader.ui.components.EraserMode
import kotlin.math.sqrt

/**
 * High-performance geometric engine for both Stroke Eraser and Precise Eraser in Notes.
 * Accurately calculates circle-segment intersections, preserves pen attributes, and splits
 * strokes cleanly at the eraser boundary.
 */
object NoteEraserHelper {

    fun distSqPointToSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = bx - ax
        val dy = by - ay
        val lenSq = dx * dx + dy * dy
        if (lenSq <= 0.0000001f) {
            val dpx = px - ax
            val dpy = py - ay
            return dpx * dpx + dpy * dpy
        }
        val t = ((px - ax) * dx + (py - ay) * dy) / lenSq
        val clampedT = t.coerceIn(0f, 1f)
        val projX = ax + clampedT * dx
        val projY = ay + clampedT * dy
        val dpx = px - projX
        val dpy = py - projY
        return dpx * dpx + dpy * dpy
    }

    fun strokeIntersectsCircle(stroke: AnnotationStroke, cx: Float, cy: Float, radius: Float): Boolean {
        val rSq = radius * radius
        val pts = stroke.points
        if (pts.isEmpty()) return false
        if (pts.size == 1) {
            val dx = pts[0].x - cx
            val dy = pts[0].y - cy
            return dx * dx + dy * dy <= rSq
        }
        for (i in 0 until pts.size - 1) {
            val p1 = pts[i]
            val p2 = pts[i + 1]
            if (distSqPointToSegment(cx, cy, p1.x, p1.y, p2.x, p2.y) <= rSq) {
                return true
            }
        }
        return false
    }

    private fun findIntersections(
        cx: Float, cy: Float, radius: Float,
        p1: AnnotationPoint, p2: AnnotationPoint
    ): Pair<Float?, Float?> {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val fx = p1.x - cx
        val fy = p1.y - cy
        val a = dx * dx + dy * dy
        if (a <= 0.000001f) return Pair(null, null)
        val b = 2f * (fx * dx + fy * dy)
        val c = fx * fx + fy * fy - radius * radius
        val discriminant = b * b - 4f * a * c
        if (discriminant < 0f) return Pair(null, null)
        val sqrtD = sqrt(discriminant)
        val t1 = (-b - sqrtD) / (2f * a)
        val t2 = (-b + sqrtD) / (2f * a)
        return Pair(t1, t2)
    }

    private fun interpolatePoint(p1: AnnotationPoint, p2: AnnotationPoint, t: Float): AnnotationPoint {
        val clampedT = t.coerceIn(0f, 1f)
        return AnnotationPoint(
            x = p1.x + clampedT * (p2.x - p1.x),
            y = p1.y + clampedT * (p2.y - p1.y),
            pressure = p1.pressure + clampedT * (p2.pressure - p1.pressure)
        )
    }

    /**
     * Erases or cuts strokes intersecting the circular eraser area at (cx, cy).
     * Returns a pair of (updatedStrokes, didChange).
     */
    fun eraseStrokesAt(
        currentStrokes: List<AnnotationStroke>,
        cx: Float,
        cy: Float,
        radius: Float,
        mode: EraserMode,
        targetPageIndex: Int = -1
    ): Pair<List<AnnotationStroke>, Boolean> {
        val rSq = radius * radius
        val newStrokes = ArrayList<AnnotationStroke>(currentStrokes.size)
        var changed = false

        for (stroke in currentStrokes) {
            if (targetPageIndex >= 0 && stroke.pageIndex != targetPageIndex) {
                newStrokes.add(stroke)
                continue
            }
            // 1. Image Stroke (penType == 2)
            if (stroke.penType == 2 && stroke.points.size >= 2) {
                val x = stroke.points[0].x
                val y = stroke.points[0].y
                val w = stroke.points[1].x
                val h = stroke.points[1].y
                if (cx >= x - radius && cx <= x + w + radius && cy >= y - radius && cy <= y + h + radius) {
                    changed = true
                    // Erased
                } else {
                    newStrokes.add(stroke)
                }
                continue
            }

            // 2. Stroke Eraser Mode: deletes whole stroke if touched anywhere
            if (mode == EraserMode.STROKE) {
                if (strokeIntersectsCircle(stroke, cx, cy, radius)) {
                    changed = true
                } else {
                    newStrokes.add(stroke)
                }
                continue
            }

            // 3. Shape Stroke (penType == 4): atomic object deletion
            if (stroke.penType == 4) {
                if (strokeIntersectsCircle(stroke, cx, cy, radius)) {
                    changed = true
                } else {
                    newStrokes.add(stroke)
                }
                continue
            }

            // 4. Precise Eraser Mode on freehand stroke: cuts & splits at eraser boundary
            if (!strokeIntersectsCircle(stroke, cx, cy, radius)) {
                newStrokes.add(stroke)
                continue
            }

            changed = true
            val pts = stroke.points
            if (pts.size < 2) {
                // Single point dot was inside eraser radius
                continue
            }

            var currentSegment = mutableListOf<AnnotationPoint>()

            for (i in 0 until pts.size - 1) {
                val p1 = pts[i]
                val p2 = pts[i + 1]

                val d1Sq = (p1.x - cx) * (p1.x - cx) + (p1.y - cy) * (p1.y - cy)
                val d2Sq = (p2.x - cx) * (p2.x - cx) + (p2.y - cy) * (p2.y - cy)
                val p1Inside = d1Sq < rSq
                val p2Inside = d2Sq < rSq

                if (i == 0 && !p1Inside) {
                    currentSegment.add(p1)
                }

                if (!p1Inside && !p2Inside) {
                    // Both outside: check if segment cuts across circle
                    val dSq = distSqPointToSegment(cx, cy, p1.x, p1.y, p2.x, p2.y)
                    if (dSq < rSq) {
                        val (t1, t2) = findIntersections(cx, cy, radius, p1, p2)
                        if (t1 != null && t2 != null && t1 in 0f..1f && t2 in 0f..1f) {
                            val ptEnter = interpolatePoint(p1, p2, t1)
                            val ptExit = interpolatePoint(p1, p2, t2)
                            currentSegment.add(ptEnter)
                            if (currentSegment.size >= 2) {
                                newStrokes.add(stroke.copy(points = currentSegment.toList()))
                            }
                            currentSegment = mutableListOf(ptExit, p2)
                        } else {
                            currentSegment.add(p2)
                        }
                    } else {
                        currentSegment.add(p2)
                    }
                } else if (!p1Inside && p2Inside) {
                    // Line enters circle: end current segment at entry intersection
                    val (t1, t2) = findIntersections(cx, cy, radius, p1, p2)
                    val t = listOfNotNull(t1, t2).firstOrNull { it in 0f..1f }
                    val ptEnter = if (t != null) interpolatePoint(p1, p2, t) else p1
                    currentSegment.add(ptEnter)
                    if (currentSegment.size >= 2) {
                        newStrokes.add(stroke.copy(points = currentSegment.toList()))
                    }
                    currentSegment = mutableListOf()
                } else if (p1Inside && !p2Inside) {
                    // Line exits circle: start new segment at exit intersection
                    val (t1, t2) = findIntersections(cx, cy, radius, p1, p2)
                    val t = listOfNotNull(t1, t2).firstOrNull { it in 0f..1f }
                    val ptExit = if (t != null) interpolatePoint(p1, p2, t) else p2
                    currentSegment = mutableListOf(ptExit, p2)
                } else {
                    // Both inside: entirely within eraser
                    if (currentSegment.size >= 2) {
                        newStrokes.add(stroke.copy(points = currentSegment.toList()))
                    }
                    currentSegment = mutableListOf()
                }
            }

            if (currentSegment.size >= 2) {
                newStrokes.add(stroke.copy(points = currentSegment.toList()))
            }
        }

        return Pair(newStrokes, changed)
    }
}
