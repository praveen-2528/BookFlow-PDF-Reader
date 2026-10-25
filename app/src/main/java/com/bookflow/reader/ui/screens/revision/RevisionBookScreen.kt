package com.bookflow.reader.ui.screens.revision

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.data.model.AnnotationStroke
import com.bookflow.reader.data.model.RevisionEntry
import com.bookflow.reader.data.model.StickyNote
import com.bookflow.reader.ui.theme.*

@Composable
fun RevisionBookScreen(
    bookTitle: String,
    entries: List<RevisionEntry>,
    pageOffset: Int,
    onGoToPage: (Int) -> Unit,
    onBack: () -> Unit
) {
    // Group entries by page
    val groupedEntries = entries.groupBy { it.pageIndex }.toSortedMap()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F0E8)) // Warm paper color
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WarmCream)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📖 Revision Book",
                    color = WarmCream,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = bookTitle,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${groupedEntries.size} pages annotated",
                color = OceanBlue,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        if (groupedEntries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📝", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No annotations yet", color = Color.Gray, fontSize = 16.sp)
                    Text("Start annotating to build your revision book!", color = Color.Gray.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }
        } else {
            // Scrollable visual revision book
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                groupedEntries.forEach { (pageIndex, pageEntries) ->
                    // Page header badge
                    item(key = "header_$pageIndex") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(DeepBlue, RoundedCornerShape(8.dp))
                                    .clickable { onGoToPage(pageIndex) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "📄 Page ${pageIndex + 1 + pageOffset}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("tap to jump ↗", color = OceanBlue, fontSize = 11.sp,
                                modifier = Modifier.clickable { onGoToPage(pageIndex) })
                        }
                    }

                    // Visual annotation canvas for this page
                    item(key = "canvas_$pageIndex") {
                        val strokeEntries = pageEntries.filter { it.stroke != null }
                        val stickyEntries = pageEntries.filter { it.stickyNote != null }

                        // Render all strokes visually on a canvas
                        if (strokeEntries.isNotEmpty()) {
                            val allStrokes = strokeEntries.mapNotNull { it.stroke }

                            // Calculate bounding box of all strokes
                            var minX = Float.MAX_VALUE
                            var minY = Float.MAX_VALUE
                            var maxX = Float.MIN_VALUE
                            var maxY = Float.MIN_VALUE
                            allStrokes.forEach { stroke ->
                                stroke.points.forEach { p ->
                                    if (p.x < minX) minX = p.x
                                    if (p.y < minY) minY = p.y
                                    if (p.x > maxX) maxX = p.x
                                    if (p.y > maxY) maxY = p.y
                                }
                            }

                            // Add padding around content
                            val padding = 30f
                            minX -= padding
                            minY -= padding
                            maxX += padding
                            maxY += padding

                            val contentW = (maxX - minX).coerceAtLeast(100f)
                            val contentH = (maxY - minY).coerceAtLeast(60f)

                            // Render at a reasonable height, preserving aspect ratio
                            val canvasWidthDp = 600
                            val scaleFactor = canvasWidthDp / contentW
                            val canvasHeightDp = (contentH * scaleFactor).coerceIn(80f, 500f)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(canvasHeightDp.dp)
                                        .padding(8.dp)
                                ) {
                                    val drawScale = size.width / contentW
                                    allStrokes.forEach { stroke ->
                                        drawRevisionStroke(stroke, minX, minY, drawScale)
                                    }
                                }
                            }
                        }

                        // Render sticky notes as visible cards
                        stickyEntries.forEach { entry ->
                            val note = entry.stickyNote ?: return@forEach
                            if (note.text.isNotBlank()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(note.color.toInt()).copy(alpha = 0.9f)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "📌 Sticky Note",
                                            color = Color.DarkGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = note.text,
                                            color = Color.DarkGray,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Subtle divider between pages
                    item(key = "div_$pageIndex") {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color.LightGray.copy(alpha = 0.4f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws a single AnnotationStroke onto the revision book canvas,
 * translating from world coordinates to canvas coordinates.
 */
private fun DrawScope.drawRevisionStroke(
    stroke: AnnotationStroke,
    originX: Float,
    originY: Float,
    drawScale: Float
) {
    if (stroke.points.isEmpty()) return

    val baseColor = Color(stroke.color.toInt())
    val finalColor = if (stroke.isHighlighter) baseColor.copy(alpha = 0.4f) else baseColor
    val sw = (stroke.strokeWidth * drawScale).coerceIn(1f, 12f)

    when (stroke.penType) {
        3 -> {
            // Rect highlight
            if (stroke.points.size >= 2) {
                val p0 = stroke.points[0]
                val p1 = stroke.points[1]
                val x0 = (p0.x - originX) * drawScale
                val y0 = (p0.y - originY) * drawScale
                val x1 = (p1.x - originX) * drawScale
                val y1 = (p1.y - originY) * drawScale
                drawRect(
                    color = finalColor.copy(alpha = 0.35f),
                    topLeft = Offset(minOf(x0, x1), minOf(y0, y1)),
                    size = Size(kotlin.math.abs(x1 - x0), kotlin.math.abs(y1 - y0))
                )
            }
        }
        4 -> {
            // Shape (connected straight lines)
            val path = Path()
            val first = stroke.points.first()
            path.moveTo((first.x - originX) * drawScale, (first.y - originY) * drawScale)
            for (i in 1 until stroke.points.size) {
                val p = stroke.points[i]
                path.lineTo((p.x - originX) * drawScale, (p.y - originY) * drawScale)
            }
            drawPath(path, color = finalColor, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        1 -> {
            // Fountain pen
            var prevX = (stroke.points.first().x - originX) * drawScale
            var prevY = (stroke.points.first().y - originY) * drawScale
            for (i in 1 until stroke.points.size) {
                val p = stroke.points[i]
                val x = (p.x - originX) * drawScale
                val y = (p.y - originY) * drawScale
                val segmentPath = Path()
                segmentPath.moveTo(prevX, prevY)
                val midX = (prevX + x) / 2; val midY = (prevY + y) / 2
                if (i == 1) segmentPath.lineTo(midX, midY) else segmentPath.quadraticTo(prevX, prevY, midX, midY)
                val widthMul = 0.3f + p.pressure * 1.5f
                drawPath(segmentPath, color = finalColor, style = Stroke(width = sw * widthMul, cap = StrokeCap.Round, join = StrokeJoin.Round))
                prevX = x; prevY = y
            }
        }
        else -> {
            // Ballpoint / Highlighter — smooth quadratic curves
            val path = Path()
            val first = stroke.points.first()
            var prevX = (first.x - originX) * drawScale
            var prevY = (first.y - originY) * drawScale
            path.moveTo(prevX, prevY)
            if (stroke.points.size == 1) {
                path.lineTo(prevX + 0.1f, prevY + 0.1f)
            }
            for (i in 1 until stroke.points.size) {
                val p = stroke.points[i]
                val x = (p.x - originX) * drawScale
                val y = (p.y - originY) * drawScale
                val midX = (prevX + x) / 2; val midY = (prevY + y) / 2
                if (i == 1) path.lineTo(midX, midY) else path.quadraticTo(prevX, prevY, midX, midY)
                prevX = x; prevY = y
            }
            path.lineTo(prevX, prevY)
            drawPath(path, color = finalColor, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
