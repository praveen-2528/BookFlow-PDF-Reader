package com.bookflow.reader.ui.components

import android.content.ContentValues
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bookflow.reader.data.model.AnnotationStroke
import com.bookflow.reader.data.model.NoteBlock
import com.bookflow.reader.ui.theme.DarkSurface
import com.bookflow.reader.ui.theme.OceanBlue
import com.bookflow.reader.ui.theme.WarmCream

enum class CanvasTool { DRAW, ERASE, SCROLL }

@Composable
fun NotebookSheet(
    blocks: List<NoteBlock>,
    onUpdateText: (String, String) -> Unit,
    onAddStroke: (String, AnnotationStroke) -> Unit,
    onEraseAt: (String, Float, Float) -> Unit,
    onAddBlock: (Boolean) -> Unit,
    onDeleteBlock: (String) -> Unit,
    onDismiss: () -> Unit,
    bookTitle: String,
    currentColor: Color = Color(0xFFE74C3C)
) {
    val context = LocalContext.current
    var tool by remember { mutableStateOf(CanvasTool.DRAW) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("My Notes", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    
                    // Main Toolbar
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        IconButton(onClick = { tool = CanvasTool.DRAW }) {
                            Icon(Icons.Filled.Create, "Draw", tint = if (tool == CanvasTool.DRAW) OceanBlue else WarmCream)
                        }
                        IconButton(onClick = { tool = CanvasTool.ERASE }) {
                            Icon(Icons.Filled.Delete, "Erase", tint = if (tool == CanvasTool.ERASE) OceanBlue else WarmCream)
                        }
                        IconButton(onClick = { tool = CanvasTool.SCROLL }) {
                            Icon(Icons.Filled.OpenWith, "Scroll", tint = if (tool == CanvasTool.SCROLL) OceanBlue else WarmCream)
                        }
                    }

                    // Export Tools
                    Row {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                val plainText = blocks.filter { it.isText }.joinToString("\n\n") { it.textContent }
                                putExtra(Intent.EXTRA_TEXT, "Notes from $bookTitle:\n\n$plainText")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Text Notes"))
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share Text", tint = Color.White)
                        }
                        IconButton(onClick = {
                            try {
                                val pdfDocument = PdfDocument()
                                val A4_WIDTH = 595f
                                val A4_HEIGHT = 842f
                                
                                var pageIdx = 1
                                var pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH.toInt(), A4_HEIGHT.toInt(), pageIdx).create()
                                var page = pdfDocument.startPage(pageInfo)
                                var currentY = 40f
                                
                                val textPaint = TextPaint().apply {
                                    color = android.graphics.Color.BLACK
                                    textSize = 14f
                                }
                                
                                for (block in blocks) {
                                    if (block.isText && block.textContent.isNotBlank()) {
                                        val sl = StaticLayout.Builder.obtain(block.textContent, 0, block.textContent.length, textPaint, (A4_WIDTH - 80).toInt()).build()
                                        if (currentY + sl.height > A4_HEIGHT - 40f) {
                                            pdfDocument.finishPage(page)
                                            pageIdx++
                                            pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH.toInt(), A4_HEIGHT.toInt(), pageIdx).create()
                                            page = pdfDocument.startPage(pageInfo)
                                            currentY = 40f
                                        }
                                        page.canvas.save()
                                        page.canvas.translate(40f, currentY)
                                        sl.draw(page.canvas)
                                        page.canvas.restore()
                                        currentY += sl.height + 20f
                                    } else if (!block.isText) {
                                        val blockHeight = 300f
                                        if (currentY + blockHeight > A4_HEIGHT - 40f) {
                                            pdfDocument.finishPage(page)
                                            pageIdx++
                                            pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH.toInt(), A4_HEIGHT.toInt(), pageIdx).create()
                                            page = pdfDocument.startPage(pageInfo)
                                            currentY = 40f
                                        }
                                        
                                        // Draw box
                                        val boxPaint = android.graphics.Paint().apply {
                                            color = android.graphics.Color.LTGRAY
                                            style = android.graphics.Paint.Style.STROKE
                                            strokeWidth = 1f
                                        }
                                        page.canvas.drawRect(40f, currentY, A4_WIDTH - 40f, currentY + blockHeight, boxPaint)
                                        
                                        page.canvas.save()
                                        page.canvas.translate(40f, currentY)
                                        block.strokes.forEach { stroke ->
                                            val path = android.graphics.Path()
                                            stroke.points.forEachIndexed { i, p ->
                                                val x = p.x * (A4_WIDTH - 80)
                                                val y = p.y * blockHeight
                                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                            }
                                            val paint = android.graphics.Paint().apply {
                                                color = stroke.color.toInt()
                                                strokeWidth = stroke.strokeWidth
                                                style = android.graphics.Paint.Style.STROKE
                                                strokeCap = android.graphics.Paint.Cap.ROUND
                                                strokeJoin = android.graphics.Paint.Join.ROUND
                                            }
                                            page.canvas.drawPath(path, paint)
                                        }
                                        page.canvas.restore()
                                        currentY += blockHeight + 20f
                                    }
                                }
                                pdfDocument.finishPage(page)
                                
                                val contentValues = ContentValues().apply {
                                    put(MediaStore.MediaColumns.DISPLAY_NAME, "Notes_${bookTitle}.pdf")
                                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
                                }
                                val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                                uri?.let {
                                    context.contentResolver.openOutputStream(it)?.use { out ->
                                        pdfDocument.writeTo(out)
                                    }
                                    Toast.makeText(context, "Saved block-based PDF to Documents!", Toast.LENGTH_SHORT).show()
                                }
                                pdfDocument.close()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "Save as PDF", tint = Color.White)
                        }
                    }
                }
                
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 16.dp))
                
                // Content Blocks
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(blocks) { block ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (block.isText) {
                                OutlinedTextField(
                                    value = block.textContent,
                                    onValueChange = { onUpdateText(block.id, it) },
                                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 100.dp),
                                    textStyle = LocalTextStyle.current.copy(lineHeight = 24.sp),
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        cursorColor = Color.White,
                                        disabledIndicatorColor = Color.DarkGray,
                                        unfocusedIndicatorColor = Color.DarkGray
                                    ),
                                    placeholder = { Text("Type notes here...", color = Color.Gray) }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                ) {
                                    AnnotationCanvas(
                                        modifier = Modifier.fillMaxSize(),
                                        strokes = block.strokes,
                                        isEnabled = tool != CanvasTool.SCROLL,
                                        currentColor = currentColor,
                                        currentStrokeWidth = 4f,
                                        isHighlighter = false,
                                        isEraser = tool == CanvasTool.ERASE,
                                        onStrokeCompleted = { onAddStroke(block.id, it) },
                                        onEraseAt = { x, y -> onEraseAt(block.id, x, y) }
                                    )
                                }
                            }
                            
                            // Delete Button
                            if (blocks.size > 1) {
                                IconButton(
                                    onClick = { onDeleteBlock(block.id) },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete Block", tint = Color.Gray)
                                }
                            }
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onAddBlock(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                            ) {
                                Icon(Icons.Filled.FormatColorText, "Add Text")
                                Spacer(Modifier.width(8.dp))
                                Text("Add Text")
                            }
                            Spacer(Modifier.width(16.dp))
                            Button(
                                onClick = { onAddBlock(false) },
                                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                            ) {
                                Icon(Icons.Filled.Create, "Add Canvas")
                                Spacer(Modifier.width(8.dp))
                                Text("Add Canvas")
                            }
                        }
                    }
                }
            }
        }
    }
}
