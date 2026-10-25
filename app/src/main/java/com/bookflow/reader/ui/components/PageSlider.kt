package com.bookflow.reader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.ui.theme.DarkSurface
import com.bookflow.reader.ui.theme.DarkSurfaceVariant
import com.bookflow.reader.ui.theme.DeepBlue
import com.bookflow.reader.ui.theme.OceanBlue
import com.bookflow.reader.ui.theme.WarmCream

@Composable
fun PageSlider(
    isVisible: Boolean,
    currentPage: Int,
    totalPages: Int,
    pageOffset: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1) return

    // Debounced scrubber state: local state tracks drag; onValueChangeFinished commits to reader
    var scrubPage by remember(currentPage) { mutableFloatStateOf(currentPage.toFloat()) }

    // Sync with external page changes if user flipped pages via gestures
    LaunchedEffect(currentPage) {
        scrubPage = currentPage.toFloat()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(horizontal = 8.dp)
                .navigationBarsPadding()
                .shadow(16.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface.copy(alpha = 0.94f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val scrubbedInt = scrubPage.toInt().coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                val displayPage = scrubbedInt - pageOffset + 1
                val totalDisplayPages = (totalPages - pageOffset).coerceAtLeast(1)

                val displayPageStr = when {
                    displayPage <= 0 -> {
                        val roman = toRoman(scrubbedInt + 1)
                        if (roman.isNotEmpty()) roman else "Preface"
                    }
                    else -> displayPage.toString()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page $displayPageStr of $totalDisplayPages",
                        color = WarmCream,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    val pct = ((scrubbedInt.toFloat() / (totalPages - 1).coerceAtLeast(1)) * 100).toInt()
                    Text(
                        text = "$pct%",
                        color = OceanBlue,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Slider(
                    value = scrubPage,
                    onValueChange = { scrubPage = it },
                    onValueChangeFinished = {
                        onPageSelected(scrubPage.toInt().coerceIn(0, totalPages - 1))
                    },
                    valueRange = 0f..((totalPages - 1).coerceAtLeast(1).toFloat()),
                    colors = SliderDefaults.colors(
                        thumbColor = OceanBlue,
                        activeTrackColor = DeepBlue,
                        inactiveTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun toRoman(number: Int): String {
    if (number <= 0) return ""
    val values = listOf(
        1000 to "m", 900 to "cm", 500 to "d", 400 to "cd",
        100 to "c", 90 to "xc", 50 to "l", 40 to "xl",
        10 to "x", 9 to "ix", 5 to "v", 4 to "iv", 1 to "i"
    )
    var num = number
    val result = StringBuilder()
    for ((value, numeral) in values) {
        while (num >= value) {
            result.append(numeral)
            num -= value
        }
    }
    return result.toString()
}
