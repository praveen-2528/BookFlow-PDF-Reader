package com.bookflow.reader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.data.model.StickyNote
import kotlin.math.roundToInt

@Composable
fun StickyNoteOverlay(
    stickyNotes: List<StickyNote>,
    pageWidth: Float,
    pageHeight: Float,
    onUpdateNote: (StickyNote) -> Unit,
    onDeleteNote: (String) -> Unit,
    onToggleExpand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        stickyNotes.forEach { note ->
            val posX = note.x * pageWidth
            val posY = note.y * pageHeight
            val noteColor = Color(note.color.toInt())

            if (note.isExpanded) {
                // Expanded sticky note card with frosted glass depth
                Box(
                    modifier = Modifier
                        .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
                        .width(260.dp)
                        .shadow(14.dp, RoundedCornerShape(16.dp), spotColor = noteColor.copy(alpha = 0.6f))
                        .background(noteColor.copy(alpha = 0.88f), RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.65f),
                                    noteColor.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Header with page ref and close
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Page ${note.pageIndex + 1}",
                                color = Color.DarkGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row {
                                IconButton(
                                    onClick = { onDeleteNote(note.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.DarkGray.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onToggleExpand(note.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Collapse",
                                        tint = Color.DarkGray.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Editable text area
                        var textValue by remember(note.id) { mutableStateOf(note.text) }
                        BasicTextField(
                            value = textValue,
                            onValueChange = { newText ->
                                textValue = newText
                                onUpdateNote(note.copy(text = newText))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp, max = 200.dp),
                            textStyle = TextStyle(
                                color = Color.DarkGray,
                                fontSize = 13.sp
                            ),
                            cursorBrush = SolidColor(Color.DarkGray),
                            decorationBox = { innerTextField ->
                                if (textValue.isEmpty()) {
                                    Text(
                                        text = "Type your note here...",
                                        color = Color.Gray.copy(alpha = 0.5f),
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            } else {
                // Collapsed sticky note jewel glass icon
                Box(
                    modifier = Modifier
                        .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
                        .size(32.dp)
                        .shadow(6.dp, RoundedCornerShape(8.dp), spotColor = noteColor.copy(alpha = 0.5f))
                        .background(noteColor.copy(alpha = 0.92f), RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.8f), noteColor.copy(alpha = 0.4f))
                            ),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onToggleExpand(note.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.EditNote,
                        contentDescription = "Sticky Note",
                        tint = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StickyNoteColorPicker(
    onColorSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        0xFFFFF176L to "Yellow",
        0xFFEF9A9AL to "Red",
        0xFFA5D6A7L to "Green",
        0xFF90CAF9L to "Blue",
        0xFFCE93D8L to "Purple",
        0xFFFFCC80L to "Orange"
    )
    
    Row(
        modifier = modifier
            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { (colorLong, _) ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(colorLong.toInt()))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    .clickable {
                        onColorSelected(colorLong)
                        onDismiss()
                    }
            )
        }
    }
}
