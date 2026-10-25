package com.bookflow.reader.ui.components

import com.bookflow.reader.data.model.AnnotationPoint
import com.bookflow.reader.data.model.AnnotationStroke
import kotlin.math.*

object SketchTemplates {

    private val templateColor = 0xFF555555L
    private val templateWidth = 2f

    fun getFlowchartRect(cx: Float, cy: Float, w: Float = 200f, h: Float = 80f): AnnotationStroke {
        return AnnotationStroke(
            points = listOf(
                AnnotationPoint(cx - w/2, cy - h/2),
                AnnotationPoint(cx + w/2, cy - h/2),
                AnnotationPoint(cx + w/2, cy + h/2),
                AnnotationPoint(cx - w/2, cy + h/2),
                AnnotationPoint(cx - w/2, cy - h/2)
            ),
            color = templateColor, strokeWidth = templateWidth, penType = 4, shapeType = 2
        )
    }

    fun getFlowchartDiamond(cx: Float, cy: Float, w: Float = 160f, h: Float = 120f): AnnotationStroke {
        return AnnotationStroke(
            points = listOf(
                AnnotationPoint(cx, cy - h/2),
                AnnotationPoint(cx + w/2, cy),
                AnnotationPoint(cx, cy + h/2),
                AnnotationPoint(cx - w/2, cy),
                AnnotationPoint(cx, cy - h/2)
            ),
            color = templateColor, strokeWidth = templateWidth, penType = 4, shapeType = 2
        )
    }

    fun getFlowchartOval(cx: Float, cy: Float, rx: Float = 100f, ry: Float = 50f): AnnotationStroke {
        val points = (0..36).map { i ->
            val angle = (i * 10.0 * PI / 180.0)
            AnnotationPoint(
                x = cx + rx * cos(angle).toFloat(),
                y = cy + ry * sin(angle).toFloat()
            )
        }
        return AnnotationStroke(
            points = points,
            color = templateColor, strokeWidth = templateWidth, penType = 4, shapeType = 1
        )
    }

    fun getTable(cx: Float, cy: Float, rows: Int = 3, cols: Int = 3, cellW: Float = 100f, cellH: Float = 60f): List<AnnotationStroke> {
        val strokes = mutableListOf<AnnotationStroke>()
        val startX = cx - (cols * cellW) / 2
        val startY = cy - (rows * cellH) / 2
        
        // Horizontal lines
        for (r in 0..rows) {
            val y = startY + r * cellH
            strokes.add(AnnotationStroke(
                points = listOf(
                    AnnotationPoint(startX, y),
                    AnnotationPoint(startX + cols * cellW, y)
                ),
                color = templateColor, strokeWidth = templateWidth, penType = 4, shapeType = 0
            ))
        }
        // Vertical lines
        for (c in 0..cols) {
            val x = startX + c * cellW
            strokes.add(AnnotationStroke(
                points = listOf(
                    AnnotationPoint(x, startY),
                    AnnotationPoint(x, startY + rows * cellH)
                ),
                color = templateColor, strokeWidth = templateWidth, penType = 4, shapeType = 0
            ))
        }
        return strokes
    }

    fun getCoordinateAxes(cx: Float, cy: Float, length: Float = 300f): List<AnnotationStroke> {
        val strokes = mutableListOf<AnnotationStroke>()
        // X axis
        strokes.add(AnnotationStroke(
            points = listOf(
                AnnotationPoint(cx - length/2, cy),
                AnnotationPoint(cx + length/2, cy)
            ),
            color = templateColor, strokeWidth = 3f, penType = 4, shapeType = 0
        ))
        // Y axis
        strokes.add(AnnotationStroke(
            points = listOf(
                AnnotationPoint(cx, cy + length/2),
                AnnotationPoint(cx, cy - length/2)
            ),
            color = templateColor, strokeWidth = 3f, penType = 4, shapeType = 0
        ))
        // X arrow head
        strokes.add(AnnotationStroke(
            points = listOf(
                AnnotationPoint(cx + length/2 - 15f, cy - 10f),
                AnnotationPoint(cx + length/2, cy),
                AnnotationPoint(cx + length/2 - 15f, cy + 10f)
            ),
            color = templateColor, strokeWidth = 3f, penType = 4, shapeType = 0
        ))
        // Y arrow head
        strokes.add(AnnotationStroke(
            points = listOf(
                AnnotationPoint(cx - 10f, cy - length/2 + 15f),
                AnnotationPoint(cx, cy - length/2),
                AnnotationPoint(cx + 10f, cy - length/2 + 15f)
            ),
            color = templateColor, strokeWidth = 3f, penType = 4, shapeType = 0
        ))
        // Grid ticks
        val tickSpacing = length / 10
        for (i in -4..4) {
            if (i == 0) continue
            val x = cx + i * tickSpacing
            strokes.add(AnnotationStroke(
                points = listOf(
                    AnnotationPoint(x, cy - 5f),
                    AnnotationPoint(x, cy + 5f)
                ),
                color = templateColor, strokeWidth = 1.5f, penType = 4, shapeType = 0
            ))
            val y = cy + i * tickSpacing
            strokes.add(AnnotationStroke(
                points = listOf(
                    AnnotationPoint(cx - 5f, y),
                    AnnotationPoint(cx + 5f, y)
                ),
                color = templateColor, strokeWidth = 1.5f, penType = 4, shapeType = 0
            ))
        }
        return strokes
    }

    fun getVennDiagram(cx: Float, cy: Float, radius: Float = 120f): List<AnnotationStroke> {
        val strokes = mutableListOf<AnnotationStroke>()
        val offset = radius * 0.5f
        
        // Left circle
        val leftPoints = (0..36).map { i ->
            val angle = (i * 10.0 * PI / 180.0)
            AnnotationPoint(
                x = cx - offset + radius * cos(angle).toFloat(),
                y = cy + radius * sin(angle).toFloat()
            )
        }
        strokes.add(AnnotationStroke(
            points = leftPoints,
            color = 0xFF3498DBL, strokeWidth = 2.5f, penType = 4, shapeType = 1
        ))
        // Right circle
        val rightPoints = (0..36).map { i ->
            val angle = (i * 10.0 * PI / 180.0)
            AnnotationPoint(
                x = cx + offset + radius * cos(angle).toFloat(),
                y = cy + radius * sin(angle).toFloat()
            )
        }
        strokes.add(AnnotationStroke(
            points = rightPoints,
            color = 0xFFE74C3CL, strokeWidth = 2.5f, penType = 4, shapeType = 1
        ))
        return strokes
    }

    fun getNumberLine(cx: Float, cy: Float, length: Float = 400f, ticks: Int = 10): List<AnnotationStroke> {
        val strokes = mutableListOf<AnnotationStroke>()
        val startX = cx - length / 2
        val endX = cx + length / 2
        
        // Main line
        strokes.add(AnnotationStroke(
            points = listOf(
                AnnotationPoint(startX, cy),
                AnnotationPoint(endX, cy)
            ),
            color = templateColor, strokeWidth = 2.5f, penType = 4, shapeType = 0
        ))
        // Arrow at end
        strokes.add(AnnotationStroke(
            points = listOf(
                AnnotationPoint(endX - 12f, cy - 8f),
                AnnotationPoint(endX, cy),
                AnnotationPoint(endX - 12f, cy + 8f)
            ),
            color = templateColor, strokeWidth = 2.5f, penType = 4, shapeType = 0
        ))
        // Ticks
        val spacing = length / ticks
        for (i in 0..ticks) {
            val x = startX + i * spacing
            strokes.add(AnnotationStroke(
                points = listOf(
                    AnnotationPoint(x, cy - 10f),
                    AnnotationPoint(x, cy + 10f)
                ),
                color = templateColor, strokeWidth = 1.5f, penType = 4, shapeType = 0
            ))
        }
        return strokes
    }

    fun getMindMapCenter(cx: Float, cy: Float): List<AnnotationStroke> {
        val strokes = mutableListOf<AnnotationStroke>()
        // Center bubble
        val centerPoints = (0..36).map { i ->
            val angle = (i * 10.0 * PI / 180.0)
            AnnotationPoint(
                x = cx + 80f * cos(angle).toFloat(),
                y = cy + 50f * sin(angle).toFloat()
            )
        }
        strokes.add(AnnotationStroke(
            points = centerPoints,
            color = 0xFF3498DBL, strokeWidth = 3f, penType = 4, shapeType = 1
        ))
        // Branch lines
        val branches = listOf(0.0, 60.0, 120.0, 180.0, 240.0, 300.0)
        for (angle in branches) {
            val rad = angle * PI / 180.0
            val ex = cx + 200f * cos(rad).toFloat()
            val ey = cy + 200f * sin(rad).toFloat()
            val sx = cx + 90f * cos(rad).toFloat()
            val sy = cy + 55f * sin(rad).toFloat()
            strokes.add(AnnotationStroke(
                points = listOf(
                    AnnotationPoint(sx, sy),
                    AnnotationPoint(ex, ey)
                ),
                color = 0xFF95A5A6L, strokeWidth = 1.5f, penType = 4, shapeType = 0
            ))
            // Small bubble at end
            val bubblePoints = (0..36).map { i ->
                val a = (i * 10.0 * PI / 180.0)
                AnnotationPoint(
                    x = ex + 50f * cos(a).toFloat(),
                    y = ey + 30f * sin(a).toFloat()
                )
            }
            strokes.add(AnnotationStroke(
                points = bubblePoints,
                color = 0xFF95A5A6L, strokeWidth = 1.5f, penType = 4, shapeType = 1
            ))
        }
        return strokes
    }
}
