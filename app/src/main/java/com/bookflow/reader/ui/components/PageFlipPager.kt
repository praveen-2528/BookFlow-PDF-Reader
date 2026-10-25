package com.bookflow.reader.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bookflow.reader.ui.theme.PageWhite
import com.bookflow.reader.ui.theme.SpineShadow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil

@Composable
fun PageFlipPager(
    totalPages: Int,
    currentPage: Int,
    isDualPage: Boolean,
    onPageChanged: (Int) -> Unit,
    onTap: () -> Unit,
    userScrollEnabled: Boolean = true,
    renderPage: suspend (Int) -> ImageBitmap?,
    annotationOverlay: @Composable (pageIndex: Int) -> Unit,
    transitionStyle: Int = 0, // 0=3D Curl, 1=Slide, 2=Fade, 3=None
    modifier: Modifier = Modifier
) {
    val pagerPageCount = if (isDualPage) ceil(totalPages / 2.0).toInt() else totalPages
    val currentPagerPage = if (isDualPage) currentPage / 2 else currentPage
    
    val pagerState = rememberPagerState(
        initialPage = currentPagerPage,
        pageCount = { pagerPageCount }
    )
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(currentPage, isDualPage) {
        val target = if (isDualPage) currentPage / 2 else currentPage
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }
    
    LaunchedEffect(pagerState, isDualPage) {
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            val targetPage = if (isDualPage) settledPage * 2 else settledPage
            if (targetPage != currentPage) {
                onPageChanged(targetPage)
            }
        }
    }
    
    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        userScrollEnabled = userScrollEnabled,
        modifier = modifier
            .fillMaxSize()
            .then(
                if (userScrollEnabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val w = size.width
                            // Reduce tap zones from w/3 (33%) to w/10 (10%) to prevent accidental page turns while drawing
                            if (offset.x < w / 10) {
                                coroutineScope.launch {
                                    if (pagerState.currentPage > 0) {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            } else if (offset.x > w - (w / 10)) {
                                coroutineScope.launch {
                                    if (pagerState.currentPage < pagerPageCount - 1) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            } else {
                                onTap()
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) { page ->
        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
        val absOffset = abs(pageOffset).coerceIn(0f, 1f)
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    when (transitionStyle) {
                        0 -> {
                            // 3D Curl (book page flip)
                            translationX = -pageOffset * size.width
                            cameraDistance = 16 * density
                            if (pageOffset < 0f && pageOffset >= -1f) {
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                                rotationY = (pageOffset * 180f).coerceIn(-180f, 0f)
                                alpha = if (pageOffset <= -0.5f) 0f else 1f
                            } else if (pageOffset > 0f && pageOffset <= 1f) {
                                alpha = 1f; rotationY = 0f
                            } else {
                                alpha = 1f; rotationY = 0f
                            }
                        }
                        1 -> {
                            // Slide (default HorizontalPager behavior — no override needed)
                            // Just add subtle depth scale
                            scaleX = 1f - 0.05f * absOffset
                            scaleY = 1f - 0.05f * absOffset
                        }
                        2 -> {
                            // Fade (crossfade between pages)
                            translationX = -pageOffset * size.width
                            alpha = 1f - absOffset
                        }
                        3 -> {
                            // None (instant, no animation)
                            translationX = -pageOffset * size.width
                            alpha = if (absOffset > 0.5f) 0f else 1f
                        }
                    }
                }
        ) {
            // Shadow on the flipping page for realism
            val flipShadowAlpha = if (pageOffset < 0f && pageOffset >= -0.5f) {
                (-pageOffset * 0.5f).coerceIn(0f, 0.3f)
            } else if (pageOffset > 0f) {
                (pageOffset * 0.3f).coerceIn(0f, 0.15f)
            } else 0f
            if (isDualPage) {
                val leftPageIndex = page * 2
                val rightPageIndex = page * 2 + 1
                
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(PageWhite)) {
                        PageContent(
                            pageIndex = leftPageIndex,
                            renderPage = renderPage,
                            annotationOverlay = annotationOverlay,
                            userScrollEnabled = userScrollEnabled
                        )
                    }
                    if (rightPageIndex < totalPages) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(Color.Transparent, SpineShadow, Color.Transparent)))
                        )
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(PageWhite)) {
                            PageContent(
                                pageIndex = rightPageIndex,
                                renderPage = renderPage,
                                annotationOverlay = annotationOverlay,
                                userScrollEnabled = userScrollEnabled
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(PageWhite))
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(PageWhite)) {
                    PageContent(
                        pageIndex = page,
                        renderPage = renderPage,
                        annotationOverlay = annotationOverlay,
                        userScrollEnabled = userScrollEnabled
                    )
                }
            }
            
            if (flipShadowAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = flipShadowAlpha))
                )
            }
        }
    }
}

@Composable
private fun PageContent(
    pageIndex: Int,
    renderPage: suspend (Int) -> ImageBitmap?,
    annotationOverlay: @Composable (pageIndex: Int) -> Unit,
    userScrollEnabled: Boolean = true
) {
    var bitmap by remember(pageIndex) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(pageIndex) { mutableStateOf(true) }
    
    var scale by remember(pageIndex) { mutableStateOf(1f) }
    var offset by remember(pageIndex) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    
    LaunchedEffect(pageIndex) {
        isLoading = true
        bitmap = renderPage(pageIndex)
        isLoading = false
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(userScrollEnabled) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.filter { it.pressed }
                        
                        // Check if stylus is touching the screen
                        val hasStylus = activePointers.any { 
                            it.type == PointerType.Stylus || it.type == PointerType.Eraser 
                        }
                        
                        if (hasStylus) {
                            // Stylus is active (writing/erasing).
                            // Palm Rejection: Consume any simultaneous finger touch (resting palm) so it doesn't move/zoom/flip the page!
                            // Stylus change itself is not consumed here, allowing AnnotationCanvas to process ink.
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
                                // Zoom or 2-finger pan
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                
                                if (scale > 1f) {
                                    val maxX = (size.width * (scale - 1)) / 2f
                                    val maxY = (size.height * (scale - 1)) / 2f
                                    offset = androidx.compose.ui.geometry.Offset(
                                        x = (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y * scale).coerceIn(-maxY, maxY)
                                    )
                                } else {
                                    offset = androidx.compose.ui.geometry.Offset.Zero
                                }
                                
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else if (touchPointers.size == 1 && scale > 1f && userScrollEnabled) {
                                // 1-finger pan ONLY when zoomed in and user scroll is enabled
                                val pan = event.calculatePan()
                                val maxX = (size.width * (scale - 1)) / 2f
                                val maxY = (size.height * (scale - 1)) / 2f
                                offset = androidx.compose.ui.geometry.Offset(
                                    x = (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                                    y = (offset.y + pan.y * scale).coerceIn(-maxY, maxY)
                                )
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                            // If touchPointers.size == 1 && scale == 1f, do NOTHING (do not consume),
                            // allowing the 1-finger horizontal swipe to propagate to HorizontalPager to flip pages!
                        }
                    } while (event.changes.any { it.pressed })
                }
            }, 
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            ) {
                bitmap?.let { img ->
                    Image(
                        bitmap = img,
                        contentDescription = "Page $pageIndex",
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                annotationOverlay(pageIndex)
            }
        }
    }
}
