package com.bookflow.reader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class ReadingSession(
    val bookUri: String,
    val startTime: Long,
    val endTime: Long,
    val startPage: Int,
    val endPage: Int
) {
    val durationMinutes: Int get() = ((endTime - startTime) / 60000).toInt()
    val pagesRead: Int get() = kotlin.math.abs(endPage - startPage)
}

data class ReadingStats(
    val totalReadingMinutes: Int = 0,
    val totalPagesRead: Int = 0,
    val totalSessions: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val todayMinutes: Int = 0,
    val weeklyMinutes: List<Int> = List(7) { 0 }, // Mon-Sun
    val recentSessions: List<ReadingSession> = emptyList()
)

@Composable
fun ReadingStatsDashboard(
    isVisible: Boolean,
    stats: ReadingStats,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onClose)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.85f)
                    .fillMaxHeight(0.75f)
                    .background(DarkSurface.copy(alpha = 0.97f), RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {} // block click-through
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Reading Statistics",
                        style = MaterialTheme.typography.headlineSmall,
                        color = WarmCream,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, "Close", tint = WarmCream)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Stats cards row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Filled.Timer,
                        value = formatTime(stats.totalReadingMinutes),
                        label = "Total Time",
                        color = DeepBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Filled.MenuBook,
                        value = "${stats.totalPagesRead}",
                        label = "Pages Read",
                        color = OceanBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Filled.TrendingUp,
                        value = "${stats.currentStreak}",
                        label = "Day Streak",
                        color = Color(0xFF2ECC71),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Weekly activity bar chart
                Text("This Week", color = WarmCream, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                val maxMinutes = (stats.weeklyMinutes.maxOrNull() ?: 1).coerceAtLeast(1)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    stats.weeklyMinutes.forEachIndexed { idx, minutes ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            val barHeight = if (maxMinutes > 0) (minutes.toFloat() / maxMinutes * 80f).coerceAtLeast(4f) else 4f
                            
                            if (minutes > 0) {
                                Text(
                                    "${minutes}m",
                                    color = Color.Gray,
                                    fontSize = 9.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(barHeight.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (minutes > 0) OceanBlue.copy(alpha = 0.7f + (minutes.toFloat() / maxMinutes) * 0.3f)
                                        else Color.Gray.copy(alpha = 0.2f)
                                    )
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(dayLabels[idx], color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Today's summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = OceanBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Today", color = WarmCream, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${formatTime(stats.todayMinutes)} reading · ${stats.totalSessions} sessions",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFF39C12),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "${stats.currentStreak}",
                            color = Color(0xFFF39C12),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(DarkSurfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon, contentDescription = label,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = WarmCream, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

private fun formatTime(minutes: Int): String {
    return when {
        minutes < 60 -> "${minutes}m"
        minutes < 1440 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${minutes / 1440}d ${(minutes % 1440) / 60}h"
    }
}
