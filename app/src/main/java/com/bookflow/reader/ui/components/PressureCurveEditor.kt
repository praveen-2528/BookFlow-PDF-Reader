package com.bookflow.reader.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.ui.theme.*
import kotlin.math.abs

data class PressureCurve(
    val cp1x: Float = 0.25f,
    val cp1y: Float = 0.25f,
    val cp2x: Float = 0.75f,
    val cp2y: Float = 0.75f
) {
    fun apply(inputPressure: Float): Float {
        // Find t using binary search to match x (inputPressure)
        var t = 0.5f
        var lower = 0f
        var upper = 1f
        for (i in 0 until 15) {
            val cx = 3 * (1 - t) * (1 - t) * t * cp1x + 3 * (1 - t) * t * t * cp2x + t * t * t
            if (abs(cx - inputPressure) < 0.001f) break
            if (cx < inputPressure) {
                lower = t
                t = (t + upper) / 2
            } else {
                upper = t
                t = (t + lower) / 2
            }
        }
        val cy = 3 * (1 - t) * (1 - t) * t * cp1y + 3 * (1 - t) * t * t * cp2y + t * t * t
        return cy.coerceIn(0f, 2f)
    }
}

@Composable
fun PressureCurveEditor(
    currentCurve: PressureCurve,
    onCurveChanged: (PressureCurve) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var curve by remember { mutableStateOf(currentCurve) }
    
    val surfaceColor = try { DarkSurface.copy(alpha = 0.95f) } catch(e: Exception) { Color(0xFF1E1E1E).copy(alpha = 0.95f) }
    val accentColor = try { OceanBlue } catch(e: Exception) { Color(0xFF3498DB) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .padding(16.dp)
            .width(320.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pressure Curve", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            TextButton(onClick = onDismiss) {
                Text("Done", color = accentColor)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.DarkGray, RoundedCornerShape(8.dp))
                .padding(16.dp) // Padding for labels
        ) {
            // Y Axis Labels
            Text("Thick", color = Color.White, fontSize = 10.sp, modifier = Modifier.align(Alignment.TopStart))
            Text("Thin", color = Color.White, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomStart))
            
            // X Axis Labels
            Text("Light", color = Color.White, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomStart).offset(y = 12.dp))
            Text("Heavy", color = Color.White, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomEnd).offset(y = 12.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, bottom = 24.dp) // Space for labels
            ) {
                var dragPoint by remember { mutableStateOf(-1) }
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val cp1Px = Offset(curve.cp1x * size.width, size.height - (curve.cp1y / 2f) * size.height)
                                    val cp2Px = Offset(curve.cp2x * size.width, size.height - (curve.cp2y / 2f) * size.height)
                                    
                                    val dist1 = (offset - cp1Px).getDistance()
                                    val dist2 = (offset - cp2Px).getDistance()
                                    
                                    dragPoint = when {
                                        dist1 < 40f -> 1
                                        dist2 < 40f -> 2
                                        else -> -1
                                    }
                                },
                                onDragEnd = { dragPoint = -1 },
                                onDragCancel = { dragPoint = -1 },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (dragPoint != -1) {
                                        val newX = (change.position.x / size.width).coerceIn(0f, 1f)
                                        // Y maps from 0 to 2, so at size.height y=0, at 0 y=2
                                        val newY = (1f - (change.position.y / size.height)) * 2f
                                        
                                        if (dragPoint == 1) {
                                            curve = curve.copy(cp1x = newX, cp1y = newY)
                                        } else {
                                            curve = curve.copy(cp2x = newX, cp2y = newY)
                                        }
                                        onCurveChanged(curve)
                                    }
                                }
                            )
                        }
                ) {
                    // Draw grid
                    val gridSteps = 4
                    for (i in 0..gridSteps) {
                        val x = i * size.width / gridSteps
                        val y = i * size.height / gridSteps
                        drawLine(Color.White.copy(alpha = 0.2f), Offset(x, 0f), Offset(x, size.height))
                        drawLine(Color.White.copy(alpha = 0.2f), Offset(0f, y), Offset(size.width, y))
                    }

                    // Convert to px coordinates. Y axis is inverted and scaled to 2.0
                    val startPoint = Offset(0f, size.height)
                    val endPoint = Offset(size.width, size.height / 2f) // end at y=1 -> height/2
                    
                    val cp1 = Offset(curve.cp1x * size.width, size.height - (curve.cp1y / 2f) * size.height)
                    val cp2 = Offset(curve.cp2x * size.width, size.height - (curve.cp2y / 2f) * size.height)

                    // Draw connection lines
                    drawLine(Color.Gray, startPoint, cp1, strokeWidth = 2.dp.toPx())
                    drawLine(Color.Gray, endPoint, cp2, strokeWidth = 2.dp.toPx())

                    // Draw curve
                    val path = Path().apply {
                        moveTo(startPoint.x, startPoint.y)
                        cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, endPoint.x, endPoint.y)
                    }
                    drawPath(path, color = accentColor, style = Stroke(width = 3.dp.toPx()))

                    // Draw control points
                    drawCircle(Color.White, radius = 6.dp.toPx(), center = cp1)
                    drawCircle(Color.White, radius = 6.dp.toPx(), center = cp2)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val presetModifier = Modifier.weight(1f).padding(horizontal = 2.dp)
            Button(
                onClick = { curve = PressureCurve(0.25f, 0.25f, 0.75f, 0.75f); onCurveChanged(curve) },
                modifier = presetModifier,
                contentPadding = PaddingValues(2.dp)
            ) { Text("Linear", fontSize = 12.sp) }
            
            Button(
                onClick = { curve = PressureCurve(0f, 1f, 1f, 1f); onCurveChanged(curve) },
                modifier = presetModifier,
                contentPadding = PaddingValues(2.dp)
            ) { Text("Soft", fontSize = 12.sp) }
            
            Button(
                onClick = { curve = PressureCurve(1f, 0f, 1f, 1f); onCurveChanged(curve) },
                modifier = presetModifier,
                contentPadding = PaddingValues(2.dp)
            ) { Text("Firm", fontSize = 12.sp) }
            
            Button(
                onClick = { curve = PressureCurve(0.5f, 0f, 0.5f, 1f); onCurveChanged(curve) },
                modifier = presetModifier,
                contentPadding = PaddingValues(2.dp)
            ) { Text("S-Curve", fontSize = 12.sp) }
        }
    }
}
