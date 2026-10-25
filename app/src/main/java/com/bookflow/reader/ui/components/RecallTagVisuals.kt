package com.bookflow.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.ui.theme.WarmCream

data class RecallTagOption(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

object RecallTagVisuals {
    val ALL_TAGS = listOf(
        RecallTagOption(
            key = "recall",
            label = "Must Recall",
            icon = Icons.Filled.Psychology,
            color = Color(0xFFFF5252)
        ),
        RecallTagOption(
            key = "important",
            label = "Important",
            icon = Icons.Filled.Star,
            color = Color(0xFFFFB300)
        ),
        RecallTagOption(
            key = "concept",
            label = "Key Concept",
            icon = Icons.Filled.Lightbulb,
            color = Color(0xFF4FC3F7)
        ),
        RecallTagOption(
            key = "doubt",
            label = "Doubt / Review",
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            color = Color(0xFFFF8A65)
        ),
        RecallTagOption(
            key = "summary",
            label = "Summary",
            icon = Icons.AutoMirrored.Filled.Article,
            color = Color(0xFF81C784)
        ),
        RecallTagOption(
            key = "exam",
            label = "Exam Focus",
            icon = Icons.Filled.TrackChanges,
            color = Color(0xFFBA68C8)
        ),
        RecallTagOption(
            key = "formula",
            label = "Quick Formula",
            icon = Icons.Filled.Bolt,
            color = Color(0xFFFFD54F)
        ),
        RecallTagOption(
            key = "deep_dive",
            label = "Deep Dive",
            icon = Icons.Filled.Search,
            color = Color(0xFF4DB6AC)
        )
    )

    fun getOption(keyOrEmoji: String): RecallTagOption {
        val byKey = ALL_TAGS.firstOrNull { it.key.equals(keyOrEmoji, ignoreCase = true) }
        if (byKey != null) return byKey

        // Fallback mapping for existing saved emoji strings
        return when (keyOrEmoji) {
            "🧠" -> ALL_TAGS[0]
            "⭐" -> ALL_TAGS[1]
            "💡" -> ALL_TAGS[2]
            "❓" -> ALL_TAGS[3]
            "📝" -> ALL_TAGS[4]
            "🎯" -> ALL_TAGS[5]
            "⚡" -> ALL_TAGS[6]
            "🔍" -> ALL_TAGS[7]
            else -> ALL_TAGS[0]
        }
    }
}

@Composable
fun RecallTagBadge(
    tagKey: String,
    label: String? = null,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    val option = RecallTagVisuals.getOption(tagKey)
    val displayLabel = label ?: option.label

    Row(
        modifier = modifier
            .background(option.color.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .border(1.dp, option.color.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = displayLabel,
            tint = option.color,
            modifier = Modifier.size(14.dp)
        )
        if (showLabel) {
            Text(
                text = displayLabel,
                color = WarmCream,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
