package com.bookflow.reader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bookflow.reader.data.model.AudioNote
import com.bookflow.reader.ui.theme.AnnotationRed
import com.bookflow.reader.ui.theme.DarkSurface
import com.bookflow.reader.ui.theme.DeepBlue
import com.bookflow.reader.ui.theme.OceanBlue
import com.bookflow.reader.ui.theme.WarmCream
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun AudioNotesOverlay(
    audioNotes: List<AudioNote>,
    onAddNote: (Float, Float) -> Unit,
    onUpdateNote: (AudioNote) -> Unit,
    onDeleteNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        audioNotes.forEach { note ->
            AudioNoteWidget(
                note = note,
                onUpdate = onUpdateNote,
                onDelete = { onDeleteNote(note.id) }
            )
        }
    }
}

@Composable
fun AudioNoteWidget(
    note: AudioNote,
    onUpdate: (AudioNote) -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(note.x) }
    var offsetY by remember { mutableFloatStateOf(note.y) }
    var isExpanded by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val audioHelper = remember { com.bookflow.reader.util.AudioRecorderHelper(context) }
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    
    DisposableEffect(Unit) {
        onDispose { audioHelper.release() }
    }
    
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onUpdate(note.copy(x = offsetX, y = offsetY)) }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        if (!isExpanded) {
            // Minimized mic icon
            FloatingActionButton(
                onClick = { isExpanded = true },
                containerColor = DeepBlue,
                contentColor = WarmCream,
                modifier = Modifier.size(48.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Mic, contentDescription = "Audio Note")
            }
        } else {
            // Expanded player/recorder
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Record / Play button
                    val iconScale by animateFloatAsState(if (isRecording) 1.2f else 1f)
                    
                    IconButton(
                        onClick = {
                            if (note.filePath.isEmpty() && !isRecording) {
                                // Start recording
                                val fileName = "note_${note.id}.m4a"
                                val path = audioHelper.startRecording(fileName)
                                if (path != null) {
                                    isRecording = true
                                    onUpdate(note.copy(filePath = path))
                                }
                            } else if (isRecording) {
                                // Stop recording
                                audioHelper.stopRecording()
                                isRecording = false
                            } else if (note.filePath.isNotEmpty() && !isPlaying) {
                                // Play
                                audioHelper.startPlaying(note.filePath) {
                                    isPlaying = false
                                }
                                isPlaying = true
                            } else if (isPlaying) {
                                // Stop playing
                                audioHelper.stopPlaying()
                                isPlaying = false
                            }
                        },
                        modifier = Modifier.scale(iconScale)
                    ) {
                        Icon(
                            imageVector = when {
                                isRecording -> Icons.Filled.Stop
                                note.filePath.isEmpty() -> Icons.Filled.Mic
                                isPlaying -> Icons.Filled.Stop
                                else -> Icons.Filled.PlayArrow
                            },
                            contentDescription = "Action",
                            tint = if (isRecording) AnnotationRed else OceanBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Status text
                    Text(
                        text = when {
                            isRecording -> "Recording... ${formatTime(recordingDuration)}"
                            note.filePath.isEmpty() -> "Tap to record"
                            isPlaying -> "Playing..."
                            else -> "Voice Note"
                        },
                        color = WarmCream,
                        modifier = Modifier.width(120.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Delete
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Gray)
                    }
                    
                    // Close
                    IconButton(onClick = { isExpanded = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = WarmCream)
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
