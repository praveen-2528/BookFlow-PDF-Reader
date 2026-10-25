package com.bookflow.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class Sticker(val emoji: String, val label: String)

@Composable
fun StickerPicker(
    onStickerSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stickers = listOf(
        Sticker("⭐", "Important"), Sticker("❓", "Doubt"),
        Sticker("✅", "Done"), Sticker("📌", "Pin"),
        Sticker("🔴", "Revision"), Sticker("⚠️", "Warning"),
        Sticker("💡", "Idea"), Sticker("📝", "Note"),
        Sticker("❌", "Wrong"), Sticker("✨", "Key Point"),
        Sticker("📊", "Data"), Sticker("🔑", "Key Term"),
        Sticker("📖", "Read Again"), Sticker("🎯", "Target"),
        Sticker("⏰", "Deadline"), Sticker("💯", "Must Know")
    )

    var selectedSticker by remember { mutableStateOf<String?>(null) }
    
    val surfaceColor = try { DarkSurface.copy(alpha = 0.95f) } catch(e: Exception) { Color(0xFF1E1E1E).copy(alpha = 0.95f) }
    val accentColor = try { OceanBlue } catch(e: Exception) { Color(0xFF3498DB) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .padding(16.dp)
            .width(300.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Quick Stamps", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            items(stickers) { sticker ->
                val isSelected = selectedSticker == sticker.emoji
                
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.DarkGray.copy(alpha = 0.5f))
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) accentColor else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selectedSticker = sticker.emoji
                            onStickerSelected(sticker.emoji)
                        }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(sticker.emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            sticker.label, 
                            color = Color.White, 
                            fontSize = 8.sp, 
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
