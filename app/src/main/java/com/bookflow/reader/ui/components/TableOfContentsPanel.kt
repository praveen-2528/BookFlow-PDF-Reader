package com.bookflow.reader.ui.components

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.ui.theme.DarkSurface
import com.bookflow.reader.ui.theme.DeepBlue
import com.bookflow.reader.ui.theme.LightMint
import com.bookflow.reader.ui.theme.OceanBlue
import com.bookflow.reader.ui.theme.WarmCream

data class TocEntry(
    val title: String,
    val pageIndex: Int,
    val level: Int = 0,
    val children: List<TocEntry> = emptyList()
)

@Composable
fun TableOfContentsPanel(
    isVisible: Boolean,
    tocEntries: List<TocEntry>,
    currentPage: Int,
    pageOffset: Int,
    onPageSelected: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val panelShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { -it }),
        exit = slideOutHorizontally(targetOffsetX = { -it }),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(320.dp)
                .shadow(24.dp, panelShape)
                .background(
                    color = DarkSurface.copy(alpha = 0.88f),
                    shape = panelShape
                )
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                LightMint.copy(alpha = 0.45f),
                                OceanBlue.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    ),
                    shape = panelShape
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Table of Contents",
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmCream,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = WarmCream)
                }
            }
            
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            
            if (tocEntries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No table of contents found\nin this PDF",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(tocEntries) { entry ->
                        TocItem(
                            entry = entry,
                            currentPage = currentPage,
                            pageOffset = pageOffset,
                            onPageSelected = onPageSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TocItem(
    entry: TocEntry,
    currentPage: Int,
    pageOffset: Int,
    onPageSelected: (Int) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    val isCurrentChapter = entry.pageIndex == currentPage
    val indent = (entry.level * 16).dp
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPageSelected(entry.pageIndex) }
                .background(
                    if (isCurrentChapter) DeepBlue.copy(alpha = 0.2f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(start = 12.dp + indent, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level indicator dot
            if (entry.level > 0) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(OceanBlue.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Text(
                text = entry.title,
                color = if (isCurrentChapter) OceanBlue else WarmCream,
                fontSize = if (entry.level == 0) 14.sp else 13.sp,
                fontWeight = if (entry.level == 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            val displayPage = entry.pageIndex - pageOffset + 1
            Text(
                text = if (displayPage > 0) "$displayPage" else "",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
            
            if (entry.children.isNotEmpty()) {
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Toggle",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        if (isExpanded) {
            entry.children.forEach { child ->
                TocItem(
                    entry = child,
                    currentPage = currentPage,
                    pageOffset = pageOffset,
                    onPageSelected = onPageSelected
                )
            }
        }
    }
}
