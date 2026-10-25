package com.bookflow.reader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnotationSearchPanel(
    isVisible: Boolean,
    bookmarks: List<Int>,
    annotatedPages: List<AnnotatedPageInfo>,
    colorTags: List<ColorTag>,
    onPageSelected: (Int) -> Unit,
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
                var searchQuery by remember { mutableStateOf("") }
                var selectedTabIndex by remember { mutableStateOf(0) }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search pages, bookmarks...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DeepBlue,
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val tabs = listOf("All", "Bookmarks", "By Color")
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = DeepBlue,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = DeepBlue
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    text = title,
                                    color = if (selectedTabIndex == index) DeepBlue else Color.LightGray
                                ) 
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                when (selectedTabIndex) {
                    0 -> AllTabContent(
                        searchQuery = searchQuery,
                        bookmarks = bookmarks,
                        annotatedPages = annotatedPages,
                        onPageSelected = onPageSelected
                    )
                    1 -> BookmarksTabContent(
                        searchQuery = searchQuery,
                        bookmarks = bookmarks,
                        onPageSelected = onPageSelected
                    )
                    2 -> ByColorTabContent(
                        annotatedPages = annotatedPages,
                        colorTags = colorTags,
                        onPageSelected = onPageSelected
                    )
                }
            }
        }
    }
}

@Composable
fun AllTabContent(
    searchQuery: String,
    bookmarks: List<Int>,
    annotatedPages: List<AnnotatedPageInfo>,
    onPageSelected: (Int) -> Unit
) {
    val allItems = remember(bookmarks, annotatedPages) {
        val items = mutableListOf<CombinedItem>()
        bookmarks.forEach { page ->
            items.add(CombinedItem.BookmarkItemType(page))
        }
        annotatedPages.forEach { pageInfo ->
            items.add(CombinedItem.AnnotationItemType(pageInfo))
        }
        items.distinctBy { 
            when (it) {
                is CombinedItem.BookmarkItemType -> it.pageIndex
                is CombinedItem.AnnotationItemType -> it.pageInfo.pageIndex
            }
        }.sortedBy { 
            when (it) {
                is CombinedItem.BookmarkItemType -> it.pageIndex
                is CombinedItem.AnnotationItemType -> it.pageInfo.pageIndex
            }
        }
    }

    val filteredItems = allItems.filter { item ->
        val pageNumber = when (item) {
            is CombinedItem.BookmarkItemType -> item.pageIndex + 1
            is CombinedItem.AnnotationItemType -> item.pageInfo.pageIndex + 1
        }
        searchQuery.isEmpty() || pageNumber.toString().contains(searchQuery)
    }

    if (filteredItems.isEmpty()) {
        EmptyStateMessage("No results found")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredItems) { item ->
                when (item) {
                    is CombinedItem.BookmarkItemType -> BookmarkItem(pageIndex = item.pageIndex, onClick = { onPageSelected(item.pageIndex) })
                    is CombinedItem.AnnotationItemType -> AnnotatedItem(pageInfo = item.pageInfo, onClick = { onPageSelected(item.pageInfo.pageIndex) })
                }
            }
        }
    }
}

sealed class CombinedItem {
    data class BookmarkItemType(val pageIndex: Int) : CombinedItem()
    data class AnnotationItemType(val pageInfo: AnnotatedPageInfo) : CombinedItem()
}

@Composable
fun BookmarksTabContent(
    searchQuery: String,
    bookmarks: List<Int>,
    onPageSelected: (Int) -> Unit
) {
    val filteredBookmarks = bookmarks.filter { page ->
        val pageNum = page + 1
        searchQuery.isEmpty() || pageNum.toString().contains(searchQuery)
    }

    if (filteredBookmarks.isEmpty()) {
        EmptyStateMessage("No bookmarks yet")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredBookmarks) { pageIndex ->
                BookmarkItem(pageIndex = pageIndex, onClick = { onPageSelected(pageIndex) })
            }
        }
    }
}

@Composable
fun ByColorTabContent(
    annotatedPages: List<AnnotatedPageInfo>,
    colorTags: List<ColorTag>,
    onPageSelected: (Int) -> Unit
) {
    var selectedColor by remember { mutableStateOf<Long?>(null) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorTags.forEach { tag ->
                        FilterChip(
                            selected = selectedColor == tag.color,
                            onClick = { 
                                selectedColor = if (selectedColor == tag.color) null else tag.color
                            },
                            label = { Text(tag.label) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(tag.color))
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
            
            val filteredPages = annotatedPages.filter { page ->
                selectedColor == null || page.colorTags.contains(selectedColor)
            }
            
            if (filteredPages.isEmpty()) {
                item {
                    EmptyStateMessage("No annotations for this color")
                }
            } else {
                items(filteredPages) { page ->
                    AnnotatedItem(pageInfo = page, onClick = { onPageSelected(page.pageIndex) })
                }
            }
        }
    }
}

@Composable
fun BookmarkItem(pageIndex: Int, onClick: () -> Unit) {
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF1C40F))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Page ${pageIndex + 1}", fontWeight = FontWeight.Bold, color = Color.White)
                Text("Bookmark", fontSize = 12.sp, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun AnnotatedItem(pageInfo: AnnotatedPageInfo, onClick: () -> Unit) {
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Page ${pageInfo.pageIndex + 1}", fontWeight = FontWeight.Bold, color = Color.White)
                Text("${pageInfo.strokeCount} annotations", fontSize = 12.sp, color = Color.LightGray)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                pageInfo.colorTags.forEach { colorVal ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(colorVal))
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = Color.Gray)
    }
}
