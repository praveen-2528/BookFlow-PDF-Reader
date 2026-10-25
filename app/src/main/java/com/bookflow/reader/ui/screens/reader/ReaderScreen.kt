package com.bookflow.reader.ui.screens.reader

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.key
import androidx.navigation.NavController
import com.bookflow.reader.ui.components.AnnotationCanvas
import com.bookflow.reader.ui.components.AnnotationTool
import com.bookflow.reader.ui.components.EraserMode
import com.bookflow.reader.ui.components.AnnotationToolbar
import com.bookflow.reader.ui.components.CropSelectionOverlay
import com.bookflow.reader.ui.components.GoToPageDialog
import com.bookflow.reader.ui.components.PageFlipPager
import com.bookflow.reader.ui.components.PageSlider
import com.bookflow.reader.ui.components.PdfTextSelectionOverlay
import com.bookflow.reader.ui.components.StickyNoteOverlay
import com.bookflow.reader.ui.components.StylusRadialMenu
import com.bookflow.reader.ui.components.TableOfContentsPanel
import com.bookflow.reader.ui.components.TopActionBar
import com.bookflow.reader.ui.screens.revision.RevisionBookScreen
import com.bookflow.reader.ui.theme.DarkBackground
import kotlinx.coroutines.delay

@Composable
fun ReaderScreen(
    bookUri: String,
    pageOffset: Int,
    navController: NavController,
    viewModel: ReaderViewModel,
    highlightCrop: String? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    val configuration = LocalConfiguration.current
    val isDualPage = uiState.isDualPageMode ?: false
    
    var isAnnotationToolbarCollapsed by remember { mutableStateOf(false) }
    var radialMenuAnchor by remember { mutableStateOf<Offset?>(null) }

    // Dynamic Environment Theming: Extract ambient color from current PDF page
    val currentAmbientColor = remember(uiState.currentPage) {
        viewModel.getAmbientColorForPage(uiState.currentPage)
    }
    val animatedAmbientColor by animateColorAsState(
        targetValue = currentAmbientColor,
        animationSpec = tween(700),
        label = "AmbilightColor"
    )

    LaunchedEffect(highlightCrop) {
        if (!highlightCrop.isNullOrBlank()) {
            val parts = highlightCrop.split(",").mapNotNull { it.trim().toFloatOrNull() }
            if (parts.size >= 4) {
                viewModel.triggerLaserHighlight(parts)
            }
        }
    }

    LaunchedEffect(uiState.isAnnotationMode) {
        if (uiState.isAnnotationMode) {
            isAnnotationToolbarCollapsed = false
        }
    }

    LaunchedEffect(bookUri) {
        val uri = Uri.parse(bookUri)
        viewModel.loadPdf(context, uri, pageOffset)
    }

    // Auto-reload notes when returning to Reader from floating mode or other apps
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.reloadInfiniteNotes()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(
        uiState.isUiVisible,
        uiState.isAnnotationMode,
        uiState.showTableOfContents,
        uiState.showGoToPageDialog,
        uiState.showAnnotationSummary,
        uiState.showAnnotationSearch,
        uiState.showReadingStats,
        uiState.showInkReplay
    ) {
        if (uiState.isUiVisible && !uiState.isAnnotationMode && 
            !uiState.showTableOfContents && !uiState.showGoToPageDialog && 
            !uiState.showAnnotationSummary && !uiState.showAnnotationSearch &&
            !uiState.showReadingStats && !uiState.showInkReplay
        ) {
            delay(6000)
            viewModel.setUiVisibility(false)
        }
    }

    // Show Revision Book as full-screen overlay
    if (uiState.showRevisionBook) {
        RevisionBookScreen(
            bookTitle = uiState.bookTitle,
            entries = uiState.revisionEntries,
            pageOffset = uiState.pageOffset,
            onGoToPage = { page ->
                viewModel.hideRevisionBook()
                viewModel.setCurrentPage(page)
            },
            onBack = { viewModel.hideRevisionBook() }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Dynamic Environment Ambilight Glow bleeding soft light behind document
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedAmbientColor.copy(alpha = 0.35f),
                            animatedAmbientColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        radius = 1400f
                    )
                )
        )

        if (uiState.isVerticalScrollMode) {
            com.bookflow.reader.ui.components.VerticalScrollPager(
                totalPages = uiState.totalPages,
                currentPage = uiState.currentPage,
                isDualPage = isDualPage,
                onPageChanged = { viewModel.setCurrentPage(it) },
                onTap = { viewModel.toggleUiVisibility() },
                userScrollEnabled = !uiState.isAnnotationMode,
                renderPage = { pageIndex -> viewModel.renderPage(pageIndex) },
                annotationOverlay = { pageIndex ->
                    key(pageIndex) {
                        val annotationsVersion = uiState.annotationsVersion
                        val pageStrokes = remember(pageIndex, annotationsVersion) {
                            viewModel.getAnnotationsForPage(pageIndex)
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            AnnotationCanvas(
                                modifier = Modifier.fillMaxSize(),
                                strokes = pageStrokes,
                                isEnabled = uiState.isAnnotationMode,
                                currentColor = Color(uiState.annotationColor.toInt()),
                                currentStrokeWidth = uiState.strokeWidth,
                                isHighlighter = uiState.annotationTool == AnnotationTool.HIGHLIGHTER,
                                isEraser = uiState.annotationTool == AnnotationTool.ERASER,
                                eraserMode = uiState.eraserMode,
                                isRectHighlight = uiState.annotationTool == AnnotationTool.RECT_HIGHLIGHT,
                                isLassoMode = uiState.annotationTool == AnnotationTool.LASSO,
                                isLaserMode = uiState.annotationTool == AnnotationTool.LASER,
                                stylusOnly = uiState.isStylusOnlyMode,
                                penType = when (uiState.annotationTool) {
                                    AnnotationTool.FOUNTAIN -> 1
                                    AnnotationTool.CALLIGRAPHY -> 2
                                    AnnotationTool.RECT_HIGHLIGHT -> 3
                                    else -> 0
                                },
                                onStrokeCompleted = { stroke -> viewModel.addStrokeForPage(pageIndex, stroke) },
                                onEraseAt = { x, y -> viewModel.eraseAtForPage(pageIndex, x, y) },
                                onMoveStrokes = { indices, dx, dy -> viewModel.movePageStrokes(pageIndex, indices, dx, dy) },
                                onInteractionStart = { if (!isAnnotationToolbarCollapsed) isAnnotationToolbarCollapsed = true },
                                onUndo = { viewModel.undo() },
                                onRadialMenuRequested = { anchor -> radialMenuAnchor = anchor }
                            )

                            PdfTextSelectionOverlay(
                                modifier = Modifier.fillMaxSize(),
                                pageIndex = pageIndex,
                                pageText = viewModel.getPageText(pageIndex),
                                isAnnotationMode = uiState.isAnnotationMode,
                                onHighlightText = { rects, _ -> viewModel.highlightTextRects(pageIndex, rects) },
                                onQuickRecall = { text, rect -> viewModel.startRecallFromText(pageIndex, text, rect) },
                                onAddStickyNote = { text -> viewModel.addStickyNoteFromText(pageIndex, text) },
                                onTap = { viewModel.toggleUiVisibility() }
                            )

                            StickyNoteOverlay(
                                stickyNotes = viewModel.getStickyNotesForPage(pageIndex),
                                pageWidth = 1f,
                                pageHeight = 1f,
                                onUpdateNote = { viewModel.updateStickyNote(it) },
                                onDeleteNote = { viewModel.deleteStickyNote(it) },
                                onToggleExpand = { viewModel.toggleStickyNoteExpand(it) }
                            )

                            if (pageIndex == uiState.currentPage && uiState.laserHighlightRect != null) {
                                com.bookflow.reader.ui.components.LaserHighlightOverlay(
                                    cropRect = uiState.laserHighlightRect,
                                    pageAspectRatio = viewModel.getPageAspectRatio(pageIndex),
                                    onDismiss = { viewModel.clearLaserHighlight() }
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PageFlipPager(
                totalPages = uiState.totalPages,
                currentPage = uiState.currentPage,
                isDualPage = isDualPage,
                onPageChanged = { viewModel.setCurrentPage(it) },
                onTap = { viewModel.toggleUiVisibility() },
                userScrollEnabled = !uiState.isAnnotationMode,
                renderPage = { pageIndex -> viewModel.renderPage(pageIndex) },
                transitionStyle = uiState.pageTransitionStyle,
                annotationOverlay = { pageIndex ->
                    key(pageIndex) {
                        val annotationsVersion = uiState.annotationsVersion
                        val pageStrokes = remember(pageIndex, annotationsVersion) {
                            viewModel.getAnnotationsForPage(pageIndex)
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            AnnotationCanvas(
                                modifier = Modifier.fillMaxSize(),
                                strokes = pageStrokes,
                                isEnabled = uiState.isAnnotationMode,
                                currentColor = Color(uiState.annotationColor.toInt()),
                                currentStrokeWidth = uiState.strokeWidth,
                                isHighlighter = uiState.annotationTool == AnnotationTool.HIGHLIGHTER,
                                isEraser = uiState.annotationTool == AnnotationTool.ERASER,
                                eraserMode = uiState.eraserMode,
                                isRectHighlight = uiState.annotationTool == AnnotationTool.RECT_HIGHLIGHT,
                                isLassoMode = uiState.annotationTool == AnnotationTool.LASSO,
                                isLaserMode = uiState.annotationTool == AnnotationTool.LASER,
                                stylusOnly = uiState.isStylusOnlyMode,
                                penType = when (uiState.annotationTool) {
                                    AnnotationTool.FOUNTAIN -> 1
                                    AnnotationTool.CALLIGRAPHY -> 2
                                    AnnotationTool.RECT_HIGHLIGHT -> 3
                                    else -> 0
                                },
                                onStrokeCompleted = { stroke -> viewModel.addStrokeForPage(pageIndex, stroke) },
                                onEraseAt = { x, y -> viewModel.eraseAtForPage(pageIndex, x, y) },
                                onMoveStrokes = { indices, dx, dy -> viewModel.movePageStrokes(pageIndex, indices, dx, dy) },
                                onInteractionStart = { if (!isAnnotationToolbarCollapsed) isAnnotationToolbarCollapsed = true },
                                onUndo = { viewModel.undo() },
                                onRadialMenuRequested = { anchor -> radialMenuAnchor = anchor }
                            )

                            PdfTextSelectionOverlay(
                                modifier = Modifier.fillMaxSize(),
                                pageIndex = pageIndex,
                                pageText = viewModel.getPageText(pageIndex),
                                isAnnotationMode = uiState.isAnnotationMode,
                                onHighlightText = { rects, _ -> viewModel.highlightTextRects(pageIndex, rects) },
                                onQuickRecall = { text, rect -> viewModel.startRecallFromText(pageIndex, text, rect) },
                                onAddStickyNote = { text -> viewModel.addStickyNoteFromText(pageIndex, text) },
                                onTap = { viewModel.toggleUiVisibility() }
                            )

                            StickyNoteOverlay(
                                stickyNotes = viewModel.getStickyNotesForPage(pageIndex),
                                pageWidth = 1f,
                                pageHeight = 1f,
                                onUpdateNote = { viewModel.updateStickyNote(it) },
                                onDeleteNote = { viewModel.deleteStickyNote(it) },
                                onToggleExpand = { viewModel.toggleStickyNoteExpand(it) }
                            )

                            com.bookflow.reader.ui.components.AudioNotesOverlay(
                                audioNotes = viewModel.getAudioNotesForPage(pageIndex),
                                onAddNote = { x, y -> viewModel.addAudioNote(pageIndex, x, y) },
                                onUpdateNote = { viewModel.updateAudioNote(it) },
                                onDeleteNote = { viewModel.deleteAudioNote(it) }
                            )

                            if (pageIndex == uiState.currentPage && uiState.laserHighlightRect != null) {
                                com.bookflow.reader.ui.components.LaserHighlightOverlay(
                                    cropRect = uiState.laserHighlightRect,
                                    pageAspectRatio = viewModel.getPageAspectRatio(pageIndex),
                                    onDismiss = { viewModel.clearLaserHighlight() }
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        com.bookflow.reader.ui.components.ReadingProgressBar(
            currentPage = uiState.currentPage,
            totalPages = uiState.totalPages,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
        )

        // Crop selection overlay
        if (uiState.isSelectCropMode) {
            CropSelectionOverlay(
                modifier = Modifier.fillMaxSize(),
                onCropSelected = { left, top, right, bottom ->
                    viewModel.captureCropRegion(left, top, right, bottom)
                },
                onMarkRecall = { left, top, right, bottom ->
                    viewModel.captureCropForRecall(left, top, right, bottom)
                },
                onCancel = { viewModel.toggleSelectCropMode() }
            )
        }
        
        TopActionBar(
            isVisible = uiState.isUiVisible || uiState.isAnnotationMode,
            title = uiState.bookTitle,
            isBookmarked = uiState.isBookmarked,
            isAnnotationMode = uiState.isAnnotationMode,
            isCropMode = uiState.isSelectCropMode,
            currentTool = uiState.annotationTool,
            eraserMode = uiState.eraserMode,
            currentColor = Color(uiState.annotationColor.toInt()),
            strokeWidth = uiState.strokeWidth,
            canUndo = uiState.undoCount > 0,
            canRedo = uiState.redoCount > 0,
            onToolSelected = { viewModel.setAnnotationTool(it) },
            onEraserModeSelected = { viewModel.setEraserMode(it) },
            onColorSelected = { viewModel.setAnnotationColor(it.toArgb().toLong()) },
            onStrokeWidthChanged = { viewModel.setStrokeWidth(it) },
            onClearPageClick = { viewModel.clearPageAnnotations(uiState.currentPage) },
            onUndoClick = { viewModel.undo() },
            onRedoClick = { viewModel.redo() },
            onMoreColorsClick = { viewModel.toggleHslColorPicker() },
            onBackClick = { navController.popBackStack() },
            onBookmarkClick = { 
                com.bookflow.reader.util.HapticHelper.click(context)
                viewModel.toggleBookmark()
            },
            onAnnotationClick = { 
                com.bookflow.reader.util.HapticHelper.tick(context)
                viewModel.toggleAnnotationMode()
            },
            onInfiniteNoteClick = { viewModel.toggleInfiniteNote() },
            onGoToPageClick = { viewModel.showGoToPageDialog() },
            onCropClick = { viewModel.toggleSelectCropMode() },
            onRevisionBookClick = { viewModel.showRevisionBook() },
            onSearchClick = { viewModel.toggleAnnotationSearch() },
            onAnnotationSummaryClick = { viewModel.toggleAnnotationSummary() },
            onVerticalScrollToggle = { viewModel.toggleVerticalScrollMode() },
            isVerticalScrollMode = uiState.isVerticalScrollMode,
            onToggleDualPage = { viewModel.toggleDualPageMode(false) },
            isDualPage = isDualPage,
            onTocClick = { 
                viewModel.loadTableOfContents(context)
                viewModel.toggleTableOfContents()
            },
            onReadingStatsClick = { viewModel.toggleReadingStats() },
            onInkReplayClick = { viewModel.toggleInkReplay() },
            onQuickRecallIndexClick = { viewModel.openQuickRecallIndex() },
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Frosted focus backdrop when Table of Contents is open
        if (uiState.showTableOfContents) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.toggleTableOfContents() }
                    )
            )
        }

        // Table of Contents Panel
        TableOfContentsPanel(
            isVisible = uiState.showTableOfContents,
            tocEntries = uiState.tocEntries,
            currentPage = uiState.currentPage,
            pageOffset = uiState.pageOffset,
            onPageSelected = { page ->
                viewModel.setCurrentPage(page)
                viewModel.toggleTableOfContents()
            },
            onClose = { viewModel.toggleTableOfContents() },
            modifier = Modifier.align(Alignment.CenterStart)
        )
        
        if (uiState.isUiVisible && !uiState.isAnnotationMode) {
            PageSlider(
                isVisible = true,
                currentPage = uiState.currentPage,
                totalPages = uiState.totalPages,
                pageOffset = uiState.pageOffset,
                onPageSelected = { viewModel.setCurrentPage(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }

        // Bottom Annotation Toolbar
        AnnotationToolbar(
            isVisible = uiState.isAnnotationMode,
            isCollapsed = isAnnotationToolbarCollapsed,
            onToggleCollapse = { isAnnotationToolbarCollapsed = !isAnnotationToolbarCollapsed },
            currentTool = uiState.annotationTool,
            eraserMode = uiState.eraserMode,
            currentColor = Color(uiState.annotationColor.toInt()),
            strokeWidth = uiState.strokeWidth,
            canUndo = uiState.undoCount > 0,
            canRedo = uiState.redoCount > 0,
            onToolSelected = { viewModel.setAnnotationTool(it) },
            onEraserModeSelected = { viewModel.setEraserMode(it) },
            onColorSelected = { viewModel.setAnnotationColor(it.toArgb().toLong()) },
            onStrokeWidthChanged = { viewModel.setStrokeWidth(it) },
            onClearPageClick = { viewModel.clearPageAnnotations(uiState.currentPage) },
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onMoreColorsClick = { viewModel.toggleHslColorPicker() },
            onPressureCurveClick = { viewModel.togglePressureCurveEditor() },
            isStylusOnly = uiState.isStylusOnlyMode,
            onToggleStylusOnly = { viewModel.toggleStylusOnlyMode() },
            onClose = { viewModel.toggleAnnotationMode() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        
        if (uiState.isInfiniteNoteOpen) {
            com.bookflow.reader.ui.components.InfiniteNotePad(
                strokes = uiState.infiniteNotes,
                canUndo = uiState.infiniteUndoCount > 0,
                canRedo = uiState.infiniteRedoCount > 0,
                canvasTemplate = uiState.canvasTemplate,
                isShapeModeEnabled = uiState.isShapeModeEnabled,
                noteTextBoxes = uiState.noteTextBoxes,
                showTemplateInsert = uiState.showTemplateInsert,
                noteTitle = if (uiState.bookTitle.isNotBlank()) "${uiState.bookTitle.take(24)} • Notes" else "In-Book Notes",
                onAddStroke = { stroke -> viewModel.addInfiniteStroke(stroke) },
                onEraseAt = { x, y -> viewModel.eraseInfiniteAt(x, y) },
                onEraseStrokeAt = { x, y, radius, mode, pageIndex -> viewModel.eraseInfiniteWithOptions(x, y, radius, mode, pageIndex) },
                onEraseGestureComplete = { viewModel.finishInfiniteEraserGesture() },
                onClear = { viewModel.clearInfiniteNotes() },
                onUndo = { viewModel.undoInfinite() },
                onRedo = { viewModel.redoInfinite() },
                onExport = { viewModel.exportInfiniteNoteToPdf(context) },
                onClose = { viewModel.toggleInfiniteNote() },
                onCycleTemplate = { viewModel.cycleCanvasTemplate() },
                onToggleShapeMode = { viewModel.toggleShapeMode() },
                onAddTextBox = { x, y -> viewModel.addNoteTextBox(x, y) },
                onAddTextBoxObj = { viewModel.addNoteTextBox(it) },
                onUpdateTextBox = { viewModel.updateNoteTextBox(it) },
                onDeleteTextBox = { viewModel.deleteNoteTextBox(it) },
                onToggleTemplateInsert = { viewModel.toggleTemplateInsert() },
                onInsertTemplate = { viewModel.insertTemplateStrokes(it) },
                onMoveStrokes = { indices, dx, dy -> viewModel.moveInfiniteStrokes(indices, dx, dy) },
                onDeleteStrokes = { viewModel.deleteInfiniteStrokes(it) },
                onScaleStrokes = { indices, scaleFactor, ox, oy -> viewModel.scaleInfiniteStrokes(indices, scaleFactor, ox, oy) },
                onStampsClick = { viewModel.toggleStickerPicker() },
                getImagePath = { hash -> viewModel.getNotebookImagePath(hash) },
                isGlobalNotebook = uiState.isGlobalNotebookMode,
                onToggleGlobalNotebook = { viewModel.toggleGlobalNotebookMode() },
                onMarkRecallSelection = { bitmap -> viewModel.startRecallFromBitmap(bitmap) },
                stylusOnly = uiState.isStylusOnlyMode,
                onFloatOverApps = {
                    val hasOverlayPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        android.provider.Settings.canDrawOverlays(context)
                    } else true

                    if (!hasOverlayPermission) {
                        try {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                            android.widget.Toast.makeText(
                                context,
                                "Please grant 'Display over other apps' to float your notebook",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(
                                context,
                                "Could not open overlay settings: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        com.bookflow.reader.service.FloatingNoteData.strokes = uiState.infiniteNotes
                        com.bookflow.reader.service.FloatingNoteData.textBoxes = uiState.noteTextBoxes
                        com.bookflow.reader.service.FloatingNoteData.canvasTemplate = uiState.canvasTemplate
                        com.bookflow.reader.service.FloatingNoteData.bookUri = uiState.bookUri
                        com.bookflow.reader.service.FloatingNoteData.bookTitle = uiState.bookTitle
                        com.bookflow.reader.service.FloatingNoteData.isGlobalMode = uiState.isGlobalNotebookMode
                        com.bookflow.reader.service.FloatingNoteData.isStylusOnly = uiState.isStylusOnlyMode

                        viewModel.toggleInfiniteNote()
                        com.bookflow.reader.service.FloatingNoteService.start(context)
                        android.widget.Toast.makeText(
                            context,
                            "Floating notebook active! You can now use any other app.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
        
        // Go to Page Dialog
        if (uiState.showGoToPageDialog) {
            GoToPageDialog(
                currentPage = uiState.currentPage,
                totalPages = uiState.totalPages,
                pageOffset = uiState.pageOffset,
                bookmarks = uiState.bookmarks,
                onGoToPage = { page ->
                    viewModel.setCurrentPage(page)
                },
                onDismiss = { viewModel.dismissGoToPageDialog() }
            )
        }

        // --- New Modals ---

        if (uiState.showAnnotationSummary) {
            com.bookflow.reader.ui.components.AnnotationSummaryPanel(
                isVisible = uiState.showAnnotationSummary,
                annotatedPages = viewModel.getAnnotatedPages(),
                colorTagLabels = uiState.colorTags.associate { it.color to it.label },
                selectedColorFilter = null,
                onPageSelected = { viewModel.setCurrentPage(it); viewModel.toggleAnnotationSummary() },
                onColorFilterSelected = { /* Handle filter */ },
                onDismiss = { viewModel.toggleAnnotationSummary() }
            )
        }

        if (uiState.showAnnotationSearch) {
            com.bookflow.reader.ui.components.AnnotationSearchPanel(
                isVisible = uiState.showAnnotationSearch,
                bookmarks = uiState.bookmarks,
                annotatedPages = viewModel.getAnnotatedPages(),
                colorTags = uiState.colorTags,
                onPageSelected = { viewModel.setCurrentPage(it); viewModel.toggleAnnotationSearch() },
                onDismiss = { viewModel.toggleAnnotationSearch() }
            )
        }

        if (uiState.showColorTagManager) {
            com.bookflow.reader.ui.components.ColorTagManager(
                colorTags = uiState.colorTags,
                onSave = { viewModel.updateColorTags(it) },
                onDismiss = { viewModel.toggleColorTagManager() }
            )
        }

        if (uiState.showHslColorPicker) {
            com.bookflow.reader.ui.components.HslColorPicker(
                currentColor = Color(uiState.annotationColor.toInt()),
                onColorSelected = { 
                    viewModel.setAnnotationColor(it.toArgb().toLong())
                    viewModel.toggleHslColorPicker()
                },
                onDismiss = { viewModel.toggleHslColorPicker() }
            )
        }

        if (uiState.showPressureCurveEditor) {
            com.bookflow.reader.ui.components.PressureCurveEditor(
                currentCurve = uiState.pressureCurve,
                onCurveChanged = { viewModel.updatePressureCurve(it) },
                onDismiss = { viewModel.togglePressureCurveEditor() }
            )
        }

        if (uiState.showStickerPicker) {
            com.bookflow.reader.ui.components.StickerPicker(
                onStickerSelected = { emoji ->
                    // Handle sticker insert - add a text box with the emoji
                    viewModel.addNoteTextBox(0.5f, 0.5f) // rough center
                    // ideally we'd update the text of that text box, but we'd need to modify the view model a bit
                    viewModel.toggleStickerPicker()
                },
                onDismiss = { viewModel.toggleStickerPicker() }
            )
        }

        // Reading Statistics Dashboard
        com.bookflow.reader.ui.components.ReadingStatsDashboard(
            isVisible = uiState.showReadingStats,
            stats = uiState.readingStats,
            onClose = { viewModel.toggleReadingStats() }
        )
        
        // Ink Replay Overlay
        com.bookflow.reader.ui.components.InkReplayOverlay(
            isVisible = uiState.showInkReplay,
            strokes = uiState.currentAnnotations,
            onClose = { viewModel.toggleInkReplay() }
        )

        // Mark Recall Dialog (Prompt tag & description)
        if (uiState.showMarkRecallDialog) {
            com.bookflow.reader.ui.components.MarkRecallDialog(
                pageIndex = uiState.currentPage,
                imagePath = uiState.pendingRecallCropPath,
                bitmap = uiState.pendingRecallCropBitmap,
                onSave = { emoji, label, description, tagKey ->
                    viewModel.savePendingQuickRecall(emoji, label, description, tagKey)
                },
                onDismiss = { viewModel.dismissMarkRecallDialog() }
            )
        }

        // Quick Recall Index & Navigation Panel
        com.bookflow.reader.ui.components.QuickRecallIndexPanel(
            isVisible = uiState.showQuickRecallIndex,
            items = uiState.quickRecallItems,
            currentBookUri = uiState.bookUri,
            onNavigateToItem = { bookUri, pageIndex, cropRect ->
                viewModel.navigateToRecallItem(bookUri, pageIndex, cropRect)
            },
            onDeleteItem = { id ->
                viewModel.deleteQuickRecallItem(id)
            },
            onDismiss = { viewModel.closeQuickRecallIndex() }
        )

        // Zero-UI Stylus Radial Menu blooming at pen tip
        radialMenuAnchor?.let { anchor ->
            StylusRadialMenu(
                anchorPosition = anchor,
                currentTool = uiState.annotationTool,
                onToolSelected = { selectedTool ->
                    viewModel.setAnnotationTool(selectedTool)
                    if (!uiState.isAnnotationMode) {
                        viewModel.toggleAnnotationMode()
                    }
                    radialMenuAnchor = null
                },
                onUndo = {
                    viewModel.undo()
                    radialMenuAnchor = null
                },
                onDismiss = {
                    radialMenuAnchor = null
                }
            )
        }
    }
}
