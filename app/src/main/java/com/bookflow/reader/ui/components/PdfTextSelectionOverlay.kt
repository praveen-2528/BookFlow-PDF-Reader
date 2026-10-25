package com.bookflow.reader.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.ui.theme.DarkBackground
import com.bookflow.reader.ui.theme.DarkSurface
import com.bookflow.reader.ui.theme.OceanBlue
import com.bookflow.reader.ui.theme.WarmCream
import com.bookflow.reader.util.HapticHelper
import com.bookflow.reader.util.PageTextContent
import com.bookflow.reader.util.PdfTextManager
import com.bookflow.reader.util.PdfWordBox

@Composable
fun PdfTextSelectionOverlay(
    modifier: Modifier = Modifier,
    pageIndex: Int,
    pageText: PageTextContent?,
    isAnnotationMode: Boolean,
    onHighlightText: (List<Rect>, String) -> Unit,
    onQuickRecall: (String, Rect) -> Unit,
    onAddStickyNote: (String) -> Unit,
    onTap: () -> Unit = {}
) {
    if (pageText == null || pageText.words.isEmpty() || isAnnotationMode) return

    val context = LocalContext.current
    var startWord by remember { mutableStateOf<PdfWordBox?>(null) }
    var endWord by remember { mutableStateOf<PdfWordBox?>(null) }
    var isSelecting by remember { mutableStateOf(false) }

    val selectedWords = remember(startWord, endWord, pageText) {
        val s = startWord
        val e = endWord
        if (s != null && e != null) {
            PdfTextManager.getWordsBetween(pageText, s, e)
        } else {
            emptyList()
        }
    }

    val selectedText = remember(selectedWords) {
        selectedWords.joinToString(" ") { it.text }
    }

    // Calculate selection bounding box in normalized space
    val selectionBounds = remember(selectedWords) {
        if (selectedWords.isEmpty()) null
        else {
            val minX = selectedWords.minOf { it.rect.left }
            val maxX = selectedWords.maxOf { it.rect.right }
            val minY = selectedWords.minOf { it.rect.top }
            val maxY = selectedWords.maxOf { it.rect.bottom }
            Rect(minX, minY, maxX, maxY)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val w = maxWidth.value
        val h = maxHeight.value

        // Gestures to initiate selection via Long-press & Drag
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(pageText) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            val normPos = Offset(offset.x / size.width, offset.y / size.height)
                            val word = PdfTextManager.findWordAt(pageText, normPos)
                            if (word != null) {
                                HapticHelper.click(context)
                                startWord = word
                                endWord = word
                                isSelecting = true
                            }
                        },
                        onTap = {
                            if (selectedWords.isNotEmpty()) {
                                startWord = null
                                endWord = null
                                isSelecting = false
                            } else {
                                onTap()
                            }
                        }
                    )
                }
                .pointerInput(pageText, isSelecting) {
                    if (isSelecting) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                val normPos = Offset(change.position.x / size.width, change.position.y / size.height)
                                val word = PdfTextManager.findWordAt(pageText, normPos)
                                if (word != null && word != endWord) {
                                    endWord = word
                                    HapticHelper.tick(context)
                                }
                                change.consume()
                            }
                        )
                    }
                }
        )

        // Draw selection highlight rects
        if (selectedWords.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sw = size.width
                val sh = size.height
                val highlightColor = Color(0x663498DB) // Soft vibrant translucent blue

                for (word in selectedWords) {
                    val r = word.rect
                    drawRect(
                        color = highlightColor,
                        topLeft = Offset(r.left * sw, r.top * sh),
                        size = Size((r.right - r.left) * sw, (r.bottom - r.top) * sh)
                    )
                }

                // Draw start & end handle pins
                startWord?.let { swrd ->
                    val handleX = swrd.rect.left * sw
                    val handleY = swrd.rect.bottom * sh
                    drawCircle(
                        color = Color(0xFF2980B9),
                        radius = 8f,
                        center = Offset(handleX, handleY + 6f)
                    )
                }

                endWord?.let { ewrd ->
                    val handleX = ewrd.rect.right * sw
                    val handleY = ewrd.rect.bottom * sh
                    drawCircle(
                        color = Color(0xFF2980B9),
                        radius = 8f,
                        center = Offset(handleX, handleY + 6f)
                    )
                }
            }
        }

        // Floating Action Bar on Selection
        if (selectedWords.isNotEmpty() && selectionBounds != null) {
            val sb = selectionBounds
            val barTopPx = ((sb.top * h) - 60).coerceAtLeast(10f)
            val barLeftPx = ((sb.left + sb.right) / 2f * w - 160).coerceIn(10f, (w - 330).coerceAtLeast(10f))

            Box(
                modifier = Modifier
                    .offset { IntOffset(barLeftPx.dp.roundToPx(), barTopPx.dp.roundToPx()) }
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .background(DarkSurface.copy(alpha = 0.95f), RoundedCornerShape(24.dp))
                    .border(1.dp, OceanBlue.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // COPY ACTION
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("PDF Text", selectedText)
                            clipboard.setPrimaryClip(clip)
                            HapticHelper.click(context)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            startWord = null
                            endWord = null
                            isSelecting = false
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = WarmCream, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", color = WarmCream, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // HIGHLIGHT ACTION
                    TextButton(
                        onClick = {
                            val rects = selectedWords.map { it.rect }
                            onHighlightText(rects, selectedText)
                            HapticHelper.click(context)
                            Toast.makeText(context, "Text Highlighted", Toast.LENGTH_SHORT).show()
                            startWord = null
                            endWord = null
                            isSelecting = false
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.FormatPaint, contentDescription = "Highlight", tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Highlight", color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // QUICK RECALL ACTION
                    TextButton(
                        onClick = {
                            onQuickRecall(selectedText, sb)
                            HapticHelper.heavyClick(context)
                            startWord = null
                            endWord = null
                            isSelecting = false
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = "Recall", tint = OceanBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Recall", color = OceanBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // NOTE ACTION
                    TextButton(
                        onClick = {
                            onAddStickyNote(selectedText)
                            HapticHelper.click(context)
                            startWord = null
                            endWord = null
                            isSelecting = false
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = "Note", tint = WarmCream, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Note", color = WarmCream, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
