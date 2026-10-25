package com.bookflow.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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

data class ColorTag(
    val color: Long,
    val label: String
)

val DEFAULT_COLOR_TAGS = listOf(
    ColorTag(0xFFE74C3C, "Important"),
    ColorTag(0xFF3498DB, "Definition"),
    ColorTag(0xFF2ECC71, "Example"),
    ColorTag(0xFFF1C40F, "Formula"),
    ColorTag(0xFFE67E22, "Doubt"),
    ColorTag(0xFF9B59B6, "Summary")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorTagManager(
    colorTags: List<ColorTag>,
    onSave: (List<ColorTag>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editableTags by remember { mutableStateOf(colorTags.ifEmpty { DEFAULT_COLOR_TAGS }) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkSurface.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxSize()
        ) {
            Text(
                text = "Color Tags",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Assign meaning to annotation colors",
                fontSize = 14.sp,
                color = Color.LightGray
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(editableTags) { index, tag ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(tag.color))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedTextField(
                            value = tag.label,
                            onValueChange = { newLabel ->
                                val updated = editableTags.toMutableList()
                                updated[index] = tag.copy(label = newLabel)
                                editableTags = updated
                            },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = DeepBlue,
                                unfocusedBorderColor = Color.Gray
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { onSave(editableTags) },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                ) {
                    Text("Save", color = Color.White)
                }
            }
        }
    }
}
