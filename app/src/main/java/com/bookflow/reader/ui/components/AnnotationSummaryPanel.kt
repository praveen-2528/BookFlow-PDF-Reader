package com.bookflow.reader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.data.model.AnnotationStroke
import com.bookflow.reader.ui.theme.*

data class AnnotatedPageInfo(
    val pageIndex: Int,
    val strokeCount: Int,
    val strokes: List<AnnotationStroke>,
    val colorTags: Set<Long>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnotationSummaryPanel(
    isVisible: Boolean,
    annotatedPages: List<AnnotatedPageInfo>,
    colorTagLabels: Map<Long, String>,
    selectedColorFilter: Long?,
    onPageSelected: (Int) -> Unit,
    onColorFilterSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = DarkSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Annotation Summary",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                var sortOption by remember { mutableStateOf("By Page") }
                val sortOptions = listOf("By Page", "By Count", "By Recent")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sortOptions.forEach { option ->
                        FilterChip(
                            selected = sortOption == option,
                            onClick = { sortOption = option },
                            label = { Text(option) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                val availableColors = annotatedPages.flatMap { it.colorTags }.toSet()
                if (availableColors.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedColorFilter == null,
                            onClick = { onColorFilterSelected(null) },
                            label = { Text("All Colors") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                        availableColors.forEach { colorVal ->
                            FilterChip(
                                selected = selectedColorFilter == colorVal,
                                onClick = { onColorFilterSelected(colorVal) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color(colorVal))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(colorTagLabels[colorVal] ?: "Tag")
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DeepBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val filteredList = annotatedPages.filter { page ->
                    selectedColorFilter == null || page.colorTags.contains(selectedColorFilter)
                }
                
                val sortedList = when (sortOption) {
                    "By Count" -> filteredList.sortedByDescending { it.strokeCount }
                    "By Recent" -> filteredList.reversed()
                    else -> filteredList.sortedBy { it.pageIndex }
                }

                if (sortedList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No annotations yet", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedList) { page ->
                            AnnotationSummaryItem(
                                page = page,
                                onClick = { onPageSelected(page.pageIndex) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnnotationSummaryItem(
    page: AnnotatedPageInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Page ${page.pageIndex + 1}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${page.strokeCount} annotations",
                    fontSize = 14.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    page.colorTags.forEach { colorVal ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 160.dp)
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / 800f 
                    val scaleY = size.height / 1200f
                    val scale = minOf(scaleX, scaleY)

                    page.strokes.forEach { stroke ->
                        if (stroke.points.isNotEmpty()) {
                            val path = Path().apply {
                                val firstPoint = stroke.points.first()
                                moveTo(firstPoint.x * scale, firstPoint.y * scale)
                                for (i in 1 until stroke.points.size) {
                                    val point = stroke.points[i]
                                    val prevPoint = stroke.points[i - 1]
                                    val midX = (prevPoint.x + point.x) / 2f * scale
                                    val midY = (prevPoint.y + point.y) / 2f * scale
                                    quadraticBezierTo(
                                        prevPoint.x * scale, prevPoint.y * scale,
                                        midX, midY
                                    )
                                }
                            }
                            drawPath(
                                path = path,
                                color = Color(stroke.color),
                                style = Stroke(
                                    width = stroke.strokeWidth * scale,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
