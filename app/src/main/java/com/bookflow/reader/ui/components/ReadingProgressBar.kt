package com.bookflow.reader.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.bookflow.reader.ui.theme.DarkSurface
import com.bookflow.reader.ui.theme.DeepBlue
import com.bookflow.reader.ui.theme.LightMint
import com.bookflow.reader.ui.theme.OceanBlue

@Composable
fun ReadingProgressBar(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalPages > 0) (currentPage + 1).toFloat() / totalPages.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "progress_animation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(DarkSurface.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(3.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(DeepBlue, OceanBlue, LightMint)
                    )
                )
        )
    }
}
