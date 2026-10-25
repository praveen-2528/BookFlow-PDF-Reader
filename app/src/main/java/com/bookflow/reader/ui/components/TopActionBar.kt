package com.bookflow.reader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookflow.reader.ui.theme.DarkSurface
import com.bookflow.reader.ui.theme.DarkSurfaceVariant
import com.bookflow.reader.ui.theme.DeepBlue
import com.bookflow.reader.ui.theme.LightMint
import com.bookflow.reader.ui.theme.OceanBlue
import com.bookflow.reader.ui.theme.WarmCream
import com.bookflow.reader.util.stylusHoverEffect

@Composable
fun TopActionBar(
    isVisible: Boolean,
    title: String,
    isBookmarked: Boolean,
    isAnnotationMode: Boolean,
    isCropMode: Boolean = false,
    // Annotation parameters retained for API compatibility
    currentTool: AnnotationTool = AnnotationTool.PEN,
    eraserMode: EraserMode = EraserMode.STROKE,
    currentColor: Color = Color(0xFFE53935),
    strokeWidth: Float = 4f,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onToolSelected: (AnnotationTool) -> Unit = {},
    onEraserModeSelected: (EraserMode) -> Unit = {},
    onColorSelected: (Color) -> Unit = {},
    onStrokeWidthChanged: (Float) -> Unit = {},
    onClearPageClick: () -> Unit = {},
    onUndoClick: () -> Unit = {},
    onRedoClick: () -> Unit = {},
    onMoreColorsClick: () -> Unit = {},
    // Existing reader callbacks
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onAnnotationClick: () -> Unit,
    onInfiniteNoteClick: () -> Unit,
    onGoToPageClick: () -> Unit = {},
    onCropClick: () -> Unit = {},
    onRevisionBookClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onAnnotationSummaryClick: () -> Unit = {},
    onVerticalScrollToggle: () -> Unit = {},
    isVerticalScrollMode: Boolean = false,
    onToggleDualPage: () -> Unit = {},
    isDualPage: Boolean = false,
    onTocClick: () -> Unit = {},
    onReadingStatsClick: () -> Unit = {},
    onInkReplayClick: () -> Unit = {},
    onQuickRecallIndexClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (isAnnotationMode) {
            // Minimized Floating Done Capsule (No screen-obstructing bar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    onClick = onAnnotationClick,
                    shape = RoundedCornerShape(22.dp),
                    color = DarkSurface.copy(alpha = 0.90f),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                OceanBlue.copy(alpha = 0.85f),
                                LightMint.copy(alpha = 0.5f)
                            )
                        )
                    ),
                    shadowElevation = 8.dp,
                    modifier = Modifier.stylusHoverEffect(hoverScale = 1.08f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Done Annotating",
                            tint = OceanBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Done",
                            color = WarmCream,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                        )
                    }
                }
            }
        } else {
            // Standard Full Top Bar for Normal Reading Mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface.copy(alpha = 0.92f))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Back button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .stylusHoverEffect()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = WarmCream
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 2. Book Title
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = WarmCream,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // In Normal Reading Mode:
                // 3. Bookmark
                val bookmarkScale by animateFloatAsState(
                    targetValue = if (isBookmarked) 1.25f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "bookmark_scale"
                )
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier
                        .size(40.dp)
                        .stylusHoverEffect()
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) OceanBlue else WarmCream,
                        modifier = Modifier.scale(bookmarkScale)
                    )
                }

                // 4. Infinite Canvas Button (BESIDE ANNOTATION!)
                IconButton(
                    onClick = onInfiniteNoteClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                        .stylusHoverEffect()
                ) {
                    Icon(
                        imageVector = Icons.Filled.DriveFileRenameOutline,
                        contentDescription = "Infinite Notes Canvas",
                        tint = OceanBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 5. Quick Recall Index Button
                IconButton(
                    onClick = onQuickRecallIndexClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                        .stylusHoverEffect()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = "Quick Recall Index",
                        tint = LightMint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 6. Annotate Toggle Button
                IconButton(
                    onClick = onAnnotationClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isAnnotationMode) DeepBlue else DarkSurfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, if (isAnnotationMode) OceanBlue else Color.Transparent, CircleShape)
                        .stylusHoverEffect()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Draw,
                        contentDescription = "Annotate",
                        tint = if (isAnnotationMode) WarmCream else WarmCream.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 7. Options / Overflow Menu (Contains View Modes, Table of Contents, and reader tools)
                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                            .stylusHoverEffect()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Options",
                            tint = WarmCream,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                    modifier = Modifier
                        .background(
                            color = DarkSurface.copy(alpha = 0.96f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 4.dp)
                ) {
                    // 1. Page Count View Layout: 1-Page View vs 2-Page View (Spread)
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = if (isDualPage) "2-Page View (Active)" else "2-Page View",
                                    color = WarmCream,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isDualPage) "Currently showing 2 pages side-by-side" else "Tap to view 2 pages side-by-side",
                                    color = WarmCream.copy(alpha = 0.65f),
                                    fontSize = 11.sp
                                )
                            }
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(OceanBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDualPage) Icons.Filled.AutoStories else Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = OceanBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDualPage) DeepBlue.copy(alpha = 0.45f) else DarkSurfaceVariant,
                                border = BorderStroke(1.dp, if (isDualPage) OceanBlue.copy(alpha = 0.5f) else Color.Transparent)
                            ) {
                                Text(
                                    text = if (isDualPage) "2 Pages" else "1 Page",
                                    color = WarmCream,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        },
                        onClick = {
                            showOverflowMenu = false
                            onToggleDualPage()
                        }
                    )

                    // 2. Flow/Scroll Type: Continuous Scroll vs Page View (Flip)
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = if (isVerticalScrollMode) "Continuous Scroll (Active)" else "Continuous Scroll View",
                                    color = WarmCream,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isVerticalScrollMode) "Continuous vertical reading (Tap for Paged)" else "Tap to switch to Continuous scroll",
                                    color = WarmCream.copy(alpha = 0.65f),
                                    fontSize = 11.sp
                                )
                            }
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(OceanBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isVerticalScrollMode) Icons.Filled.SwapVert else Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = OceanBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isVerticalScrollMode) DeepBlue.copy(alpha = 0.45f) else DarkSurfaceVariant,
                                border = BorderStroke(1.dp, if (isVerticalScrollMode) OceanBlue.copy(alpha = 0.5f) else Color.Transparent)
                            ) {
                                Text(
                                    text = if (isVerticalScrollMode) "Continuous" else "Paged",
                                    color = WarmCream,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        },
                        onClick = {
                            showOverflowMenu = false
                            onVerticalScrollToggle()
                        }
                    )

                    HorizontalDivider(
                        color = DarkSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    DropdownMenuItem(
                        text = { Text("Quick Recall Index", color = WarmCream) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = OceanBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onQuickRecallIndexClick()
                        }
                    )
                    // Table of Contents (Shifted from top bar to options as requested)
                    DropdownMenuItem(
                        text = { Text("Table of Contents", color = WarmCream) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = OceanBlue)
                        },
                        onClick = {
                            showOverflowMenu = false
                            onTocClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Go to Page…", color = WarmCream) },
                        leadingIcon = {
                            Icon(Icons.Filled.FindInPage, contentDescription = null, tint = OceanBlue)
                        },
                        onClick = {
                            showOverflowMenu = false
                            onGoToPageClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Search Annotations", color = WarmCream) },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = OceanBlue)
                        },
                        onClick = {
                            showOverflowMenu = false
                            onSearchClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Annotation Summary", color = WarmCream) },
                        leadingIcon = {
                            Icon(Icons.Filled.Description, contentDescription = null, tint = OceanBlue)
                        },
                        onClick = {
                            showOverflowMenu = false
                            onAnnotationSummaryClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Revision Book", color = WarmCream) },
                        leadingIcon = {
                            Icon(Icons.Filled.AutoStories, contentDescription = null, tint = OceanBlue)
                        },
                        onClick = {
                            showOverflowMenu = false
                            onRevisionBookClick()
                        }
                    )
                    HorizontalDivider(
                        color = DarkSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    DropdownMenuItem(
                        text = { Text(if (isCropMode) "Exit Crop Mode" else "Crop to Notebook", color = WarmCream) },
                        leadingIcon = {
                            Icon(Icons.Filled.ContentCut, contentDescription = null, tint = if (isCropMode) OceanBlue else WarmCream)
                        },
                        onClick = {
                            showOverflowMenu = false
                            onCropClick()
                        }
                    )
                    HorizontalDivider(
                        color = DarkSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    DropdownMenuItem(
                        text = { Text("📊 Reading Statistics", color = WarmCream) },
                        leadingIcon = {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = OceanBlue)
                        },
                        onClick = {
                            showOverflowMenu = false
                            onReadingStatsClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🎬 Ink Replay", color = WarmCream) },
                        leadingIcon = {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = OceanBlue)
                        },
                        onClick = {
                            showOverflowMenu = false
                            onInkReplayClick()
                        }
                    )
                }
            }
        }
    }
}
}


