package com.bookflow.reader.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bookflow.reader.data.model.QuickRecallItem
import com.bookflow.reader.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QuickRecallIndexPanel(
    isVisible: Boolean,
    items: List<QuickRecallItem>,
    currentBookUri: String? = null,
    onNavigateToItem: (bookUri: String, pageIndex: Int, cropRect: List<Float>?) -> Unit,
    onDeleteItem: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyCurrentBook by remember { mutableStateOf(false) }

    // Filter items
    val filteredItems = remember(items, selectedTagFilter, searchQuery, showOnlyCurrentBook, currentBookUri) {
        items.filter { item ->
            val matchesBook = !showOnlyCurrentBook || currentBookUri == null || item.bookUri == currentBookUri
            val matchesTag = selectedTagFilter == null ||
                    item.tagKey.equals(selectedTagFilter, ignoreCase = true) ||
                    item.emoji == selectedTagFilter
            val matchesSearch = searchQuery.isBlank() ||
                    item.description.contains(searchQuery, ignoreCase = true) ||
                    item.label.contains(searchQuery, ignoreCase = true) ||
                    item.bookTitle.contains(searchQuery, ignoreCase = true)
            matchesBook && matchesTag && matchesSearch
        }
    }

    val availableTags = remember(items) {
        val keys = items.map { if (it.tagKey.isNotBlank()) it.tagKey else it.emoji }.distinct()
        keys.map { RecallTagVisuals.getOption(it) }.distinctBy { it.key }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(26.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(OceanBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                text = "Quick Recall Index",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = WarmCream
                            )
                            Text(
                                text = "${items.size} memory anchors saved across books",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = WarmCream)
                    }
                }

                // Search Bar + Book Scope Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search by description, topic, or book...", fontSize = 13.sp, color = Color.Gray) },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OceanBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedTextColor = WarmCream,
                            unfocusedTextColor = WarmCream
                        )
                    )

                    if (currentBookUri != null) {
                        FilterChip(
                            selected = showOnlyCurrentBook,
                            onClick = { showOnlyCurrentBook = !showOnlyCurrentBook },
                            label = { Text("Current Book Only", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OceanBlue.copy(alpha = 0.25f),
                                selectedLabelColor = OceanBlue
                            )
                        )
                    }
                }

                // Vector Tag Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedTagFilter == null,
                            onClick = { selectedTagFilter = null },
                            label = { Text("All (${items.size})", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OceanBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    items(availableTags) { tagOption ->
                        val count = items.count {
                            it.tagKey.equals(tagOption.key, ignoreCase = true) ||
                            RecallTagVisuals.getOption(it.emoji).key == tagOption.key
                        }
                        val isSelected = selectedTagFilter == tagOption.key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTagFilter = if (isSelected) null else tagOption.key },
                            leadingIcon = {
                                Icon(
                                    imageVector = tagOption.icon,
                                    contentDescription = tagOption.label,
                                    tint = if (isSelected) Color.White else tagOption.color,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = { Text("${tagOption.label} ($count)", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = tagOption.color,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // List of Recall Items
                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Psychology,
                                contentDescription = null,
                                tint = OceanBlue.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty() || selectedTagFilter != null) "No matching recall anchors found" else "No quick recall items yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmCream
                            )
                            Text(
                                text = "Use the Crop or Lasso tool in any PDF or Note and tap 'Mark Recall' with a tag!",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            QuickRecallCard(
                                item = item,
                                onNavigate = {
                                    onNavigateToItem(item.bookUri, item.pageIndex, item.cropRect)
                                    onDismiss()
                                },
                                onDelete = { onDeleteItem(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickRecallCard(
    item: QuickRecallItem,
    onNavigate: () -> Unit,
    onDelete: () -> Unit
) {
    val imageBitmap = remember(item.imagePath) {
        if (!item.imagePath.isNullOrBlank()) {
            val file = File(item.imagePath)
            if (file.exists()) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            } else null
        } else null
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedDate = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkSurfaceVariant.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onNavigate() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Snippet Thumbnail
            if (imageBitmap != null) {
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 80.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Recall Snippet",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(2.dp)
                    )
                }
            } else {
                val tagOption = RecallTagVisuals.getOption(if (item.tagKey.isNotBlank()) item.tagKey else item.emoji)
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 70.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tagOption.color.copy(alpha = 0.18f))
                        .border(1.dp, tagOption.color.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tagOption.icon,
                        contentDescription = tagOption.label,
                        tint = tagOption.color,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Info Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Top Tag Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecallTagBadge(
                        tagKey = if (item.tagKey.isNotBlank()) item.tagKey else item.emoji,
                        label = item.label
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "Page ${item.pageIndex + 1}",
                            color = WarmCream,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (item.bookTitle.isNotBlank()) {
                        Text(
                            text = "• ${item.bookTitle}",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                // Description
                Text(
                    text = item.description,
                    color = WarmCream,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Date
                Text(
                    text = formattedDate,
                    color = Color.Gray.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }

            // Navigation Button & Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onNavigate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OceanBlue.copy(alpha = 0.25f),
                        contentColor = OceanBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Navigate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Gray.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
