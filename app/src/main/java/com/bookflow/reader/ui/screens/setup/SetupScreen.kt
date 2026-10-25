package com.bookflow.reader.ui.screens.setup

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bookflow.reader.ui.theme.DarkBackground
import com.bookflow.reader.ui.theme.DarkSurface
import com.bookflow.reader.ui.theme.DarkSurfaceVariant
import com.bookflow.reader.ui.theme.DeepBlue
import com.bookflow.reader.ui.theme.OceanBlue
import com.bookflow.reader.ui.theme.WarmCream
import com.bookflow.reader.util.PdfBitmapPool
import kotlin.math.min

@Composable
fun SetupScreen(navController: NavController, bookUri: String) {
    val context = LocalContext.current
    val pdfBitmapPool = remember { PdfBitmapPool() }
    val thumbnails = remember { mutableStateMapOf<Int, ImageBitmap>() }
    var selectedPageIndex by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoadingThumbnails by remember { mutableStateOf(true) }
    var manualPageInput by remember { mutableStateOf("") }

    LaunchedEffect(bookUri) {
        val uri = Uri.parse(bookUri)
        try {
            val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            if (fileDescriptor != null) {
                pdfBitmapPool.openPdf(fileDescriptor)
                totalPages = pdfBitmapPool.pageCount
                // Expanded from 20 to 50 pages for large academic textbooks with long prefaces
                val pagesToRender = min(50, totalPages)
                for (i in 0 until pagesToRender) {
                    val bitmap = pdfBitmapPool.renderPage(i, targetWidth = 350, targetHeight = 500)
                    if (bitmap != null) {
                        thumbnails[i] = bitmap
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoadingThumbnails = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pdfBitmapPool.close()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Where does Page 1 start?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = WarmCream,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        Text(
            text = "Tap the cover or title page where numbered Chapter 1 begins, or type the index.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Manual Input or Quick Jump Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = manualPageInput,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) {
                        manualPageInput = input
                        val target = input.toIntOrNull()
                        if (target != null && target in 0 until totalPages.coerceAtLeast(1)) {
                            selectedPageIndex = target
                        }
                    }
                },
                placeholder = { Text("Jump to index…", color = Color.Gray, fontSize = 13.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .width(180.dp)
                    .height(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WarmCream,
                    unfocusedTextColor = WarmCream,
                    focusedBorderColor = OceanBlue,
                    unfocusedBorderColor = DarkSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceVariant
            ) {
                Text(
                    text = "Selected: Page ${selectedPageIndex + 1} (Offset: $selectedPageIndex)",
                    color = OceanBlue,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                )
            }
        }

        // Adaptive Thumbnail Grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnails.isEmpty() && isLoadingThumbnails) {
                CircularProgressIndicator(color = OceanBlue)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(thumbnails.size) { index ->
                        val isSelected = selectedPageIndex == index
                        val bitmap = thumbnails[index]

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(0.72f)
                                    .scale(if (isSelected) 1.05f else 1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) OceanBlue else DarkSurfaceVariant,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .background(DarkSurface)
                                    .clickable { selectedPageIndex = index }
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = "Page $index",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(OceanBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Page ${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) OceanBlue else Color.Gray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Confirm Button
        Button(
            onClick = {
                navController.navigate("reader/${Uri.encode(bookUri)}/$selectedPageIndex") {
                    popUpTo("home")
                }
            },
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(52.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(DeepBlue, OceanBlue))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Start Reading",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        TextButton(
            onClick = {
                navController.navigate("reader/${Uri.encode(bookUri)}/0") {
                    popUpTo("home")
                }
            }
        ) {
            Text("Skip (Start from Beginning at Page 1)", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
