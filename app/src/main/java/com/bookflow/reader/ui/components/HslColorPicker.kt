package com.bookflow.reader.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.ui.theme.*

@Composable
fun HslColorPicker(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }
    var alpha by remember { mutableFloatStateOf(currentColor.alpha) }

    // Initialize from currentColor
    LaunchedEffect(currentColor) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(
            AndroidColor.argb(
                (currentColor.alpha * 255).toInt(),
                (currentColor.red * 255).toInt(),
                (currentColor.green * 255).toInt(),
                (currentColor.blue * 255).toInt()
            ),
            hsv
        )
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        alpha = currentColor.alpha
    }

    val selectedColor = remember(hue, saturation, value, alpha) {
        val hsv = floatArrayOf(hue, saturation, value)
        val c = AndroidColor.HSVToColor(hsv)
        Color(c).copy(alpha = alpha)
    }

    var recentColors by remember {
        mutableStateOf(List(8) { Color.Transparent })
    }

    val presets = listOf(
        Color(0xFFE74C3C), Color(0xFF3498DB), Color(0xFF2ECC71), Color(0xFFF1C40F),
        Color(0xFFE67E22), Color(0xFF9B59B6), Color(0xFF2D2D2D), Color(0xFFFFFFFF)
    )

    // Using DarkSurface if available, else a fallback
    val surfaceColor = try {
        DarkSurface.copy(alpha = 0.95f)
    } catch (e: Exception) {
        Color(0xFF1E1E1E).copy(alpha = 0.95f)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .padding(16.dp)
            .width(300.dp)
    ) {
        Text("Color Picker", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // SB Pad
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                        value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        saturation = (offset.x / size.width).coerceIn(0f, 1f)
                        value = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hueColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                
                // Base hue background
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, hueColor)
                    )
                )
                // Black gradient overlay
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )
                
                val currentX = saturation * size.width
                val currentY = (1f - value) * size.height
                
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(currentX, currentY),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hue Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        hue = ((change.position.x / size.width).coerceIn(0f, 1f)) * 360f
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        hue = ((offset.x / size.width).coerceIn(0f, 1f)) * 360f
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val rainbow = listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                    Color.Blue, Color.Magenta, Color.Red
                )
                drawRect(
                    brush = Brush.horizontalGradient(colors = rainbow)
                )
                
                val currentX = (hue / 360f) * size.width
                drawCircle(
                    color = Color.White,
                    radius = 12.dp.toPx(),
                    center = Offset(currentX, size.height / 2),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Opacity Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Opacity", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(60.dp))
            Slider(
                value = alpha,
                onValueChange = { alpha = it },
                modifier = Modifier.weight(1f)
            )
            Text(
                "${(alpha * 100).toInt()}%", 
                color = Color.White, 
                fontSize = 12.sp,
                modifier = Modifier.width(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preview and Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(selectedColor)
                    .border(2.dp, Color.White, CircleShape)
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.White)
                }
                Button(
                    onClick = {
                        val currentRecents = recentColors.toMutableList()
                        currentRecents.add(0, selectedColor)
                        if (currentRecents.size > 8) currentRecents.removeLast()
                        recentColors = currentRecents
                        onColorSelected(selectedColor)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = try { OceanBlue } catch (e: Exception) { Color(0xFF3498DB) })
                ) {
                    Text("Select")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Presets
        Text("Presets", color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            presets.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable {
                            val hsv = FloatArray(3)
                            AndroidColor.colorToHSV(
                                AndroidColor.argb(
                                    (color.alpha * 255).toInt(),
                                    (color.red * 255).toInt(),
                                    (color.green * 255).toInt(),
                                    (color.blue * 255).toInt()
                                ),
                                hsv
                            )
                            hue = hsv[0]
                            saturation = hsv[1]
                            value = hsv[2]
                            alpha = color.alpha
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Recents
        Text("Recent", color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            recentColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, Color.Gray, CircleShape)
                        .clickable {
                            if (color != Color.Transparent) {
                                val hsv = FloatArray(3)
                                AndroidColor.colorToHSV(
                                    AndroidColor.argb(
                                        (color.alpha * 255).toInt(),
                                        (color.red * 255).toInt(),
                                        (color.green * 255).toInt(),
                                        (color.blue * 255).toInt()
                                    ),
                                    hsv
                                )
                                hue = hsv[0]
                                saturation = hsv[1]
                                value = hsv[2]
                                alpha = color.alpha
                            }
                        }
                )
            }
        }
    }
}
