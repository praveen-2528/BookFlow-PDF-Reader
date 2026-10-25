package com.bookflow.reader.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.ui.theme.PageWhite
import com.bookflow.reader.ui.theme.SpineShadow
import kotlin.math.ceil

@Composable
fun VerticalScrollPager(
    totalPages: Int,
    currentPage: Int,
    isDualPage: Boolean = false,
    onPageChanged: (Int) -> Unit,
    onTap: () -> Unit,
    renderPage: suspend (Int) -> ImageBitmap?,
    annotationOverlay: @Composable (pageIndex: Int) -> Unit,
    userScrollEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val rowCount = if (isDualPage) ceil(totalPages / 2.0).toInt() else totalPages

    LaunchedEffect(listState, isDualPage) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                -1
            } else {
                val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset / 2
                var closestIndex = visibleItemsInfo.first().index
                var minDistance = Int.MAX_VALUE
                for (item in visibleItemsInfo) {
                    val itemCenter = item.offset + item.size / 2
                    val distance = kotlin.math.abs(itemCenter - viewportCenter)
                    if (distance < minDistance) {
                        minDistance = distance
                        closestIndex = item.index
                    }
                }
                closestIndex
            }
        }.collect { rowIndex ->
            if (rowIndex != -1) {
                val targetPage = if (isDualPage) rowIndex * 2 else rowIndex
                if (targetPage != currentPage && targetPage < totalPages) {
                    onPageChanged(targetPage)
                }
            }
        }
    }

    LaunchedEffect(currentPage, isDualPage) {
        if (!listState.isScrollInProgress) {
            val targetRow = if (isDualPage) currentPage / 2 else currentPage
            val visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.none { it.index == targetRow }) {
                listState.scrollToItem(targetRow)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (userScrollEnabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onTap() }
                        )
                    }
                } else {
                    Modifier
                }
            )
            .pointerInput(userScrollEnabled) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.filter { it.pressed }
                        val hasStylus = activePointers.any {
                            it.type == PointerType.Stylus || it.type == PointerType.Eraser
                        }
                        if (hasStylus) {
                            // Stylus is active (writing/erasing).
                            // Palm Rejection: consume any simultaneous finger touch (resting palm) so it doesn't move/zoom/scroll!
                            event.changes.forEach { change ->
                                if (change.type == PointerType.Touch && change.positionChanged()) {
                                    change.consume()
                                }
                            }
                        } else {
                            val touchPointers = activePointers.filter {
                                it.type == PointerType.Touch || it.type == PointerType.Mouse
                            }
                            if (touchPointers.size >= 2) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    val maxX = (size.width * (scale - 1)) / 2f
                                    val maxY = (size.height * (scale - 1)) / 2f
                                    offsetX = (offsetX + pan.x * scale).coerceIn(-maxX, maxX)
                                    offsetY = (offsetY + pan.y * scale).coerceIn(-maxY, maxY)
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else if (touchPointers.size == 1 && scale > 1f && userScrollEnabled) {
                                val pan = event.calculatePan()
                                val maxX = (size.width * (scale - 1)) / 2f
                                val maxY = (size.height * (scale - 1)) / 2f
                                offsetX = (offsetX + pan.x * scale).coerceIn(-maxX, maxX)
                                offsetY = (offsetY + pan.y * scale).coerceIn(-maxY, maxY)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                            // If touchPointers.size == 1 && scale == 1f, do nothing so LazyColumn scrolls vertically!
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            }
    ) {
        LazyColumn(
            state = listState,
            userScrollEnabled = userScrollEnabled,
            modifier = Modifier.fillMaxSize()
        ) {
            items(rowCount, key = { it }) { rowIndex ->
                if (isDualPage) {
                    val leftPageIndex = rowIndex * 2
                    val rightPageIndex = rowIndex * 2 + 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(PageWhite)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentHeight()
                        ) {
                            SinglePageItem(
                                pageIndex = leftPageIndex,
                                renderPage = renderPage,
                                annotationOverlay = annotationOverlay
                            )
                        }
                        if (rightPageIndex < totalPages) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(Brush.horizontalGradient(listOf(Color.Transparent, SpineShadow, Color.Transparent)))
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .wrapContentHeight()
                            ) {
                                SinglePageItem(
                                    pageIndex = rightPageIndex,
                                    renderPage = renderPage,
                                    annotationOverlay = annotationOverlay
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(PageWhite)
                            )
                        }
                    }
                } else {
                    SinglePageItem(
                        pageIndex = rowIndex,
                        renderPage = renderPage,
                        annotationOverlay = annotationOverlay
                    )
                }

                if (rowIndex < rowCount - 1) {
                    HorizontalDivider(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun SinglePageItem(
    pageIndex: Int,
    renderPage: suspend (Int) -> ImageBitmap?,
    annotationOverlay: @Composable (pageIndex: Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(PageWhite)
    ) {
        val imageBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = pageIndex) {
            value = renderPage(pageIndex)
        }

        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = "Page ${pageIndex + 1}",
                contentScale = ContentScale.FillWidth,
                filterQuality = FilterQuality.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(64.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        annotationOverlay(pageIndex)

        Text(
            text = "Page ${pageIndex + 1}",
            color = Color.Black.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        )
    }
}
