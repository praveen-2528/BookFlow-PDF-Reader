package com.bookflow.reader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.data.model.AnnotationStroke
import com.bookflow.reader.ui.theme.DarkSurface
import com.bookflow.reader.ui.theme.DeepBlue
import com.bookflow.reader.ui.theme.OceanBlue
import com.bookflow.reader.ui.theme.WarmCream
import kotlinx.coroutines.delay

@Composable
fun InkReplayOverlay(
    isVisible: Boolean,
    strokes: List<AnnotationStroke>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        var isPlaying by remember { mutableStateOf(false) }
        var replayProgress by remember { mutableFloatStateOf(0f) } // 0..1 across all strokes
        var playbackSpeed by remember { mutableFloatStateOf(1f) } // 1x, 2x, 4x
        
        // Sort strokes by timestamp
        val sortedStrokes = remember(strokes) {
            strokes.filter { it.penType != 2 }.sortedBy { it.timestamp }
        }
        
        // Total points across all strokes
        val totalPoints = remember(sortedStrokes) {
            sortedStrokes.sumOf { it.points.size }
        }
        
        // Replay animation
        LaunchedEffect(isPlaying, playbackSpeed) {
            if (isPlaying && totalPoints > 0) {
                val msPerPoint = (20 / playbackSpeed).toLong().coerceAtLeast(2)
                while (replayProgress < 1f) {
                    replayProgress += 1f / totalPoints
                    delay(msPerPoint)
                }
                replayProgress = 1f
                isPlaying = false
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
        ) {
            // Canvas for replay
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (totalPoints == 0) return@Canvas
                
                val currentPointIndex = (replayProgress * totalPoints).toInt()
                var pointsSoFar = 0
                
                for (stroke in sortedStrokes) {
                    val strokeStart = pointsSoFar
                    val strokeEnd = pointsSoFar + stroke.points.size
                    
                    if (strokeStart >= currentPointIndex) break
                    
                    val visiblePoints = if (strokeEnd <= currentPointIndex) {
                        stroke.points // Full stroke visible
                    } else {
                        stroke.points.take(currentPointIndex - strokeStart)
                    }
                    
                    if (visiblePoints.size >= 2) {
                        val baseColor = Color(stroke.color.toInt())
                        val finalColor = if (stroke.isHighlighter) baseColor.copy(alpha = 0.4f) else baseColor
                        
                        val path = Path()
                        val first = visiblePoints.first()
                        var pX = first.x * size.width
                        var pY = first.y * size.height
                        path.moveTo(pX, pY)
                        
                        for (i in 1 until visiblePoints.size) {
                            val p = visiblePoints[i]
                            val x = p.x * size.width
                            val y = p.y * size.height
                            val midX = (pX + x) / 2f
                            val midY = (pY + y) / 2f
                            if (i == 1) path.lineTo(midX, midY) else path.quadraticTo(pX, pY, midX, midY)
                            pX = x; pY = y
                        }
                        
                        drawPath(
                            path,
                            color = finalColor,
                            style = Stroke(
                                width = stroke.strokeWidth * 2f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        
                        // Glowing dot at the drawing tip (only for the last visible stroke)
                        if (strokeEnd > currentPointIndex && visiblePoints.isNotEmpty()) {
                            drawCircle(
                                color = Color.White,
                                radius = stroke.strokeWidth * 3f,
                                center = Offset(pX, pY),
                                alpha = 0.8f
                            )
                            drawCircle(
                                color = finalColor,
                                radius = stroke.strokeWidth * 5f,
                                center = Offset(pX, pY),
                                alpha = 0.3f
                            )
                        }
                    }
                    
                    pointsSoFar = strokeEnd
                }
            }
            
            // Controls bar at bottom
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .background(DarkSurface.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Play/Pause button
                IconButton(onClick = {
                    if (replayProgress >= 1f) replayProgress = 0f
                    isPlaying = !isPlaying
                }) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isPlaying) OceanBlue else WarmCream,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Restart button
                IconButton(onClick = {
                    replayProgress = 0f
                    isPlaying = true
                }) {
                    Icon(Icons.Filled.Replay, "Restart", tint = WarmCream)
                }
                
                // Progress slider
                Slider(
                    value = replayProgress,
                    onValueChange = { 
                        replayProgress = it
                        isPlaying = false 
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = OceanBlue,
                        activeTrackColor = DeepBlue
                    )
                )
                
                // Speed button
                TextButton(onClick = {
                    playbackSpeed = when (playbackSpeed) {
                        1f -> 2f
                        2f -> 4f
                        else -> 1f
                    }
                }) {
                    Text(
                        "${playbackSpeed.toInt()}x",
                        color = OceanBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                
                // Close
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, "Close", tint = WarmCream)
                }
            }
            
            // Stroke count indicator
            Text(
                "${sortedStrokes.size} strokes · ${totalPoints} points",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }
    }
}
