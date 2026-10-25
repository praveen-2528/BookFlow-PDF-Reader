package com.bookflow.reader.ui.screens.reader

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.ImageBitmap
import com.bookflow.reader.data.model.AnnotationPoint
import com.bookflow.reader.data.model.AnnotationStroke
import com.bookflow.reader.data.model.BookMetadata
import com.bookflow.reader.data.model.PageAnnotations
import com.bookflow.reader.data.repository.BookRepository
import com.bookflow.reader.ui.components.AnnotationTool
import com.bookflow.reader.ui.components.EraserMode
import com.bookflow.reader.util.PdfBitmapPool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.math.pow
import kotlin.math.sqrt

data class ReaderUiState(
    val isLoading: Boolean = true,
    val bookTitle: String = "",
    val bookUri: String = "",
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val pageOffset: Int = 0,
    val isAnnotationMode: Boolean = false,
    val annotationTool: AnnotationTool = AnnotationTool.PEN,
    val eraserMode: EraserMode = EraserMode.STROKE,
    val penStyle: Int = 0, // 0 = Felt/Ballpoint, 1 = Fountain, 2 = Calligraphy
    val annotationColor: Long = 0xFFE53935, // Vibrant Crimson Red like lecture teacher's pen in screenshot
    val strokeWidth: Float = 4f,
    val isUiVisible: Boolean = true,
    val currentAnnotations: List<AnnotationStroke> = emptyList(),
    val annotationsVersion: Long = 0L,
    val isBookmarked: Boolean = false,
    val undoCount: Int = 0,
    val redoCount: Int = 0,
    val infiniteUndoCount: Int = 0,
    val infiniteRedoCount: Int = 0,
    val isInfiniteNoteOpen: Boolean = false,
    val infiniteNotes: List<AnnotationStroke> = emptyList(),
    val showGoToPageDialog: Boolean = false,
    val bookmarks: List<Int> = emptyList(),
    val isSelectCropMode: Boolean = false,
    // New smart features
    val stickyNotes: List<com.bookflow.reader.data.model.StickyNote> = emptyList(),
    val noteTextBoxes: List<com.bookflow.reader.data.model.NoteTextBox> = emptyList(),
    val canvasTemplate: com.bookflow.reader.data.model.CanvasTemplate = com.bookflow.reader.data.model.CanvasTemplate.DOT_GRID,
    val isShapeModeEnabled: Boolean = false,
    val showRevisionBook: Boolean = false,
    val revisionEntries: List<com.bookflow.reader.data.model.RevisionEntry> = emptyList(),
    val showTemplateInsert: Boolean = false,
    
    // New Feature States
    val isVerticalScrollMode: Boolean = true,
    val isDualPageMode: Boolean? = false,
    val showAnnotationSummary: Boolean = false,
    val showColorTagManager: Boolean = false,
    val showAnnotationSearch: Boolean = false,
    val showHslColorPicker: Boolean = false,
    val showPressureCurveEditor: Boolean = false,
    val showStickerPicker: Boolean = false,
    val colorTags: List<com.bookflow.reader.ui.components.ColorTag> = com.bookflow.reader.ui.components.DEFAULT_COLOR_TAGS,
    val pressureCurve: com.bookflow.reader.ui.components.PressureCurve = com.bookflow.reader.ui.components.PressureCurve(),
    
    // Table of Contents
    val showTableOfContents: Boolean = false,
    val tocEntries: List<com.bookflow.reader.ui.components.TocEntry> = emptyList(),
    
    // Page transition style: 0=3D Curl, 1=Slide, 2=Fade, 3=None
    val pageTransitionStyle: Int = 0,
    
    // Reading Statistics
    val showReadingStats: Boolean = false,
    val readingStats: com.bookflow.reader.ui.components.ReadingStats = com.bookflow.reader.ui.components.ReadingStats(),
    
    // Ink Replay
    val showInkReplay: Boolean = false,
    
    // Audio Notes
    val currentAudioNotes: List<com.bookflow.reader.data.model.AudioNote> = emptyList(),

    // Quick Recall & Global Notebook
    val showQuickRecallIndex: Boolean = false,
    val quickRecallItems: List<com.bookflow.reader.data.model.QuickRecallItem> = emptyList(),
    val showMarkRecallDialog: Boolean = false,
    val pendingRecallCropBitmap: android.graphics.Bitmap? = null,
    val pendingRecallCropPath: String? = null,
    val pendingRecallCropRect: List<Float>? = null,
    val laserHighlightRect: List<Float>? = null,
    val isGlobalNotebookMode: Boolean = false,
    val isStylusOnlyMode: Boolean = false
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository(application)
    private val pdfBitmapPool = PdfBitmapPool()

    private val _uiState = MutableStateFlow(
        ReaderUiState(
            isVerticalScrollMode = repository.getPreferredVerticalScroll(),
            isDualPageMode = repository.getPreferredDualPage()
        )
    )
    val uiState = _uiState.asStateFlow()
    
    private val annotationsCache = mutableMapOf<Int, PageAnnotations>()
    private val undoStack = mutableListOf<AnnotationStroke>()
    private val redoStack = mutableListOf<AnnotationStroke>()
    
    private val infiniteUndoStack = mutableListOf<List<AnnotationStroke>>()
    private val infiniteRedoStack = mutableListOf<List<AnnotationStroke>>()
    
    // Reading session tracking
    private var sessionStartTime: Long = System.currentTimeMillis()
    private var sessionStartPage: Int = 0
    fun loadPdf(context: Context, uri: Uri, pageOffset: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, bookUri = uri.toString(), pageOffset = pageOffset) }
            
            try {
                val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                if (fileDescriptor != null) {
                    pdfBitmapPool.openPdf(fileDescriptor)
                    val totalPages = pdfBitmapPool.pageCount
                    
                    var title = "Untitled Book"
                    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            title = cursor.getString(0) ?: "Untitled Book"
                        }
                    }
                    if (title.endsWith(".pdf", ignoreCase = true)) {
                        title = title.substringBeforeLast(".")
                    }
                    if (title == "Untitled Book" || title.isBlank()) {
                        title = uri.lastPathSegment ?: "Untitled Book"
                        if (title.contains(".")) {
                            title = title.substringBeforeLast(".")
                        }
                    }
                    
                    val existingBook = repository.getBookMetadata(uri.toString())
                    val book = existingBook ?: BookMetadata(
                        uri = uri.toString(),
                        title = title,
                        totalPages = totalPages,
                        pageOffset = pageOffset
                    )
                    
                    repository.saveBookMetadata(book.copy(lastOpenedTimestamp = System.currentTimeMillis()))
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            totalPages = totalPages,
                            bookTitle = title,
                            currentPage = book.lastReadPage
                        )
                    }
                    setCurrentPage(book.lastReadPage)
                    loadStickyNotes()
                    loadNoteTextBoxes()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, bookTitle = "Error loading PDF") }
            }
        }
    }

    fun setCurrentPage(page: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(currentPage = page) }
            
            val uriStr = _uiState.value.bookUri
            val book = repository.getBookMetadata(uriStr)
            if (book != null) {
                repository.saveBookMetadata(book.copy(lastReadPage = page))
            }
            
            _uiState.update { it.copy(isBookmarked = repository.isBookmarked(uriStr, page)) }
            
            pdfBitmapPool.preloadPages(page)
            
            if (!annotationsCache.containsKey(page)) {
                annotationsCache[page] = repository.loadAnnotations(uriStr, page)
            }
            
            val currentStrokes = annotationsCache[page]?.strokes?.toList() ?: emptyList()
            undoStack.clear()
            undoStack.addAll(currentStrokes)
            redoStack.clear()
            
            _uiState.update { 
                it.copy(
                    currentAnnotations = currentStrokes,
                    undoCount = undoStack.size,
                    redoCount = redoStack.size
                ) 
            }
            updateStickyNotesState()
        }
    }

    suspend fun renderPage(pageIndex: Int): ImageBitmap? {
        return pdfBitmapPool.renderPage(pageIndex)
    }

    fun getAmbientColorForPage(pageIndex: Int): androidx.compose.ui.graphics.Color {
        return pdfBitmapPool.getAmbientColor(pageIndex) ?: androidx.compose.ui.graphics.Color(0xFF0F2B48)
    }

    fun toggleUiVisibility() {
        _uiState.update { it.copy(isUiVisible = !it.isUiVisible) }
    }
    
    fun setUiVisibility(visible: Boolean) {
        _uiState.update { it.copy(isUiVisible = visible) }
    }

    fun toggleAnnotationMode() {
        _uiState.update { it.copy(isAnnotationMode = !it.isAnnotationMode) }
    }

    fun setAnnotationTool(tool: AnnotationTool) {
        _uiState.update { it.copy(annotationTool = tool) }
    }

    fun setEraserMode(mode: EraserMode) {
        _uiState.update { it.copy(eraserMode = mode) }
    }

    fun setPenStyle(style: Int) {
        _uiState.update { it.copy(penStyle = style) }
    }

    fun setAnnotationColor(color: Long) {
        _uiState.update { it.copy(annotationColor = color) }
    }

    fun setStrokeWidth(width: Float) {
        _uiState.update { it.copy(strokeWidth = width) }
    }

    fun toggleStylusOnlyMode() {
        _uiState.update { it.copy(isStylusOnlyMode = !it.isStylusOnlyMode) }
    }

    fun addStroke(stroke: AnnotationStroke) {
        addStrokeForPage(_uiState.value.currentPage, stroke)
    }

    fun addStrokeForPage(pageIndex: Int, stroke: AnnotationStroke) {
        val uriStr = _uiState.value.bookUri
        val pageAnnotations = annotationsCache.getOrPut(pageIndex) {
            repository.loadAnnotations(uriStr, pageIndex)
        }
        
        if (stroke.penType == -1) {
            // Signal that eraser gesture completed, just save the current state
            val copyToSave = pageAnnotations.copy(strokes = pageAnnotations.strokes.toMutableList())
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveAnnotations(uriStr, copyToSave)
            }
            return
        }

        pageAnnotations.strokes.add(stroke)
        undoStack.add(stroke)
        redoStack.clear()
        
        val copyToSave = pageAnnotations.copy(strokes = pageAnnotations.strokes.toMutableList())
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveAnnotations(uriStr, copyToSave)
        }
        
        _uiState.update { 
            it.copy(
                currentAnnotations = (annotationsCache[it.currentPage]?.strokes ?: emptyList()).toList(),
                annotationsVersion = it.annotationsVersion + 1,
                undoCount = undoStack.size,
                redoCount = redoStack.size
            ) 
        }
    }

    fun eraseAt(normalizedX: Float, normalizedY: Float) {
        eraseAtForPage(_uiState.value.currentPage, normalizedX, normalizedY)
    }

    fun eraseAtForPage(pageIndex: Int, normalizedX: Float, normalizedY: Float) {
        val uriStr = _uiState.value.bookUri
        val pageAnnotations = annotationsCache.getOrPut(pageIndex) {
            repository.loadAnnotations(uriStr, pageIndex)
        }
        
        val strokes = pageAnnotations.strokes
        val mode = _uiState.value.eraserMode
        
        if (mode == EraserMode.STROKE) {
            // Stroke-based eraser: deletes entire stroke when touched anywhere on its path
            val strokeThreshold = (_uiState.value.strokeWidth * 0.002f + 0.045f).coerceIn(0.04f, 0.10f)
            val iter = strokes.iterator()
            var changed = false
            while (iter.hasNext()) {
                val stroke = iter.next()
                if (strokeIntersectsEraser(stroke, normalizedX, normalizedY, strokeThreshold)) {
                    iter.remove()
                    changed = true
                }
            }
            if (changed) {
                undoStack.removeAll { it !in pageAnnotations.strokes }
                redoStack.clear()
                _uiState.update { 
                    it.copy(
                        currentAnnotations = (annotationsCache[it.currentPage]?.strokes ?: emptyList()).toList(),
                        annotationsVersion = it.annotationsVersion + 1,
                        undoCount = undoStack.size,
                        redoCount = 0
                    ) 
                }
            }
        } else {
            // Precise / segment eraser: cuts and erases parts of stroke under the eraser circle
            val baseRadius = (_uiState.value.strokeWidth * 0.003f + 0.015f).coerceIn(0.018f, 0.07f)
            val eraseRadiusSq = baseRadius * baseRadius 
            val newStrokes = mutableListOf<AnnotationStroke>()
            var changed = false

            for (stroke in strokes) {
                if (!strokeIntersectsEraser(stroke, normalizedX, normalizedY, baseRadius)) {
                    newStrokes.add(stroke)
                } else {
                    changed = true
                    var currentSegment = mutableListOf<AnnotationPoint>()
                    val pts = stroke.points
                    for (i in pts.indices) {
                        val point = pts[i]
                        val dx = point.x - normalizedX
                        val dy = point.y - normalizedY
                        if (dx * dx + dy * dy >= eraseRadiusSq) {
                            if (currentSegment.isNotEmpty()) {
                                val prev = currentSegment.last()
                                if (distSqPointToSegment(normalizedX, normalizedY, prev.x, prev.y, point.x, point.y) < eraseRadiusSq) {
                                    if (currentSegment.size > 1) {
                                        newStrokes.add(stroke.copy(points = currentSegment.toList()))
                                    }
                                    currentSegment = mutableListOf(point)
                                } else {
                                    currentSegment.add(point)
                                }
                            } else {
                                currentSegment.add(point)
                            }
                        } else {
                            if (currentSegment.size > 1) {
                                newStrokes.add(stroke.copy(points = currentSegment.toList()))
                            }
                            currentSegment = mutableListOf()
                        }
                    }
                    if (currentSegment.size > 1) {
                        newStrokes.add(stroke.copy(points = currentSegment.toList()))
                    }
                }
            }
            
            if (changed) {
                pageAnnotations.strokes.clear()
                pageAnnotations.strokes.addAll(newStrokes)
                undoStack.removeAll { it !in newStrokes }
                redoStack.clear()
                _uiState.update { 
                    it.copy(
                        currentAnnotations = (annotationsCache[it.currentPage]?.strokes ?: emptyList()).toList(),
                        annotationsVersion = it.annotationsVersion + 1,
                        undoCount = undoStack.size,
                        redoCount = 0
                    ) 
                }
            }
        }
    }

    fun clearPageAnnotations(pageIndex: Int) {
        val uriStr = _uiState.value.bookUri
        val pageAnnotations = annotationsCache.getOrPut(pageIndex) {
            repository.loadAnnotations(uriStr, pageIndex)
        }
        if (pageAnnotations.strokes.isEmpty()) return
        
        pageAnnotations.strokes.clear()
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveAnnotations(uriStr, PageAnnotations(pageIndex))
        }
        _uiState.update { 
            it.copy(
                currentAnnotations = (annotationsCache[it.currentPage]?.strokes ?: emptyList()).toList(),
                annotationsVersion = it.annotationsVersion + 1
            )
        }
    }

    fun movePageStrokes(pageIndex: Int, indices: Set<Int>, dx: Float, dy: Float, scale: Float = 1f) {
        val pageAnnotations = annotationsCache[pageIndex] ?: return
        val newStrokes = mutableListOf<AnnotationStroke>()
        
        for ((i, stroke) in pageAnnotations.strokes.withIndex()) {
            if (i in indices) {
                // Simplified translation (ignore scale for now as it requires center point math)
                val newPoints = stroke.points.map { p ->
                    com.bookflow.reader.data.model.AnnotationPoint(p.x + dx, p.y + dy, p.pressure)
                }
                newStrokes.add(stroke.copy(points = newPoints))
            } else {
                newStrokes.add(stroke)
            }
        }
        
        pageAnnotations.strokes.clear()
        pageAnnotations.strokes.addAll(newStrokes)
        
        val copyToSave = pageAnnotations.copy(strokes = pageAnnotations.strokes.toMutableList())
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveAnnotations(_uiState.value.bookUri, copyToSave)
        }
        
        _uiState.update { 
            it.copy(
                currentAnnotations = pageAnnotations.strokes.toList(),
                annotationsVersion = it.annotationsVersion + 1
            ) 
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        
        val stroke = undoStack.removeLast()
        redoStack.add(stroke)
        
        for (pageAnno in annotationsCache.values) {
            if (pageAnno.strokes.remove(stroke)) {
                val copyToSave = pageAnno.copy(strokes = pageAnno.strokes.toMutableList())
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveAnnotations(_uiState.value.bookUri, copyToSave)
                }
                break
            }
        }
        
        _uiState.update { 
            it.copy(
                currentAnnotations = (annotationsCache[it.currentPage]?.strokes ?: emptyList()).toList(),
                annotationsVersion = it.annotationsVersion + 1,
                undoCount = undoStack.size,
                redoCount = redoStack.size
            ) 
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        
        val stroke = redoStack.removeLast()
        undoStack.add(stroke)
        
        val page = _uiState.value.currentPage
        val pageAnnotations = annotationsCache.getOrPut(page) { PageAnnotations(page) }
        pageAnnotations.strokes.add(stroke)
        
        val copyToSave = pageAnnotations.copy(strokes = pageAnnotations.strokes.toMutableList())
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveAnnotations(_uiState.value.bookUri, copyToSave)
        }
        
        _uiState.update { 
            it.copy(
                currentAnnotations = pageAnnotations.strokes.toList(),
                annotationsVersion = it.annotationsVersion + 1,
                undoCount = undoStack.size,
                redoCount = redoStack.size
            ) 
        }
    }

    fun getAnnotationsForPage(page: Int): List<AnnotationStroke> {
        if (!annotationsCache.containsKey(page)) {
            annotationsCache[page] = repository.loadAnnotations(_uiState.value.bookUri, page)
        }
        return annotationsCache[page]?.strokes?.toList() ?: emptyList()
    }


    fun toggleVerticalScrollMode() {
        val next = !_uiState.value.isVerticalScrollMode
        repository.setPreferredVerticalScroll(next)
        _uiState.update { it.copy(isVerticalScrollMode = next) }
    }

    fun toggleDualPageMode(isLandscape: Boolean) {
        _uiState.update { state ->
            val current = state.isDualPageMode ?: false
            val next = !current
            repository.setPreferredDualPage(next)
            state.copy(isDualPageMode = next)
        }
    }

    fun setDualPageMode(isDual: Boolean) {
        repository.setPreferredDualPage(isDual)
        _uiState.update { it.copy(isDualPageMode = isDual) }
    }
    fun getAnnotatedPages(): List<com.bookflow.reader.ui.components.AnnotatedPageInfo> {
        val uriStr = _uiState.value.bookUri
        val savedPages = repository.getAllAnnotations(uriStr)
        val map = savedPages.associateBy { it.pageIndex }.toMutableMap()
        annotationsCache.forEach { (page, annotations) ->
            if (annotations.strokes.isNotEmpty()) {
                map[page] = annotations
            } else {
                map.remove(page)
            }
        }
        return map.values.sortedBy { it.pageIndex }.map { pageAnno ->
            com.bookflow.reader.ui.components.AnnotatedPageInfo(
                pageIndex = pageAnno.pageIndex,
                strokeCount = pageAnno.strokes.size,
                strokes = pageAnno.strokes,
                colorTags = pageAnno.strokes.map { it.color }.toSet()
            )
        }
    }

    fun toggleAnnotationSummary() {
        _uiState.update { it.copy(showAnnotationSummary = !it.showAnnotationSummary) }
    }
    fun toggleColorTagManager() {
        _uiState.update { it.copy(showColorTagManager = !it.showColorTagManager) }
    }
    fun toggleAnnotationSearch() {
        _uiState.update { it.copy(showAnnotationSearch = !it.showAnnotationSearch) }
    }
    fun toggleHslColorPicker() {
        _uiState.update { it.copy(showHslColorPicker = !it.showHslColorPicker) }
    }
    fun togglePressureCurveEditor() {
        _uiState.update { it.copy(showPressureCurveEditor = !it.showPressureCurveEditor) }
    }
    fun toggleStickerPicker() {
        _uiState.update { it.copy(showStickerPicker = !it.showStickerPicker) }
    }
    fun updateColorTags(tags: List<com.bookflow.reader.ui.components.ColorTag>) {
        _uiState.update { it.copy(colorTags = tags, showColorTagManager = false) }
    }
    fun updatePressureCurve(curve: com.bookflow.reader.ui.components.PressureCurve) {
        _uiState.update { it.copy(pressureCurve = curve) }
    }

    fun toggleBookmark() {
        val page = _uiState.value.currentPage
        val uriStr = _uiState.value.bookUri
        val isBookmarked = _uiState.value.isBookmarked
        
        if (isBookmarked) {
            repository.removeBookmark(uriStr, page)
        } else {
            repository.saveBookmark(uriStr, page)
        }
        
        _uiState.update { it.copy(isBookmarked = !isBookmarked) }
    }

    // ---- Go to Page ----

    fun showGoToPageDialog() {
        val bookmarks = repository.getBookmarks(_uiState.value.bookUri).toList().sorted()
        _uiState.update { it.copy(showGoToPageDialog = true, bookmarks = bookmarks) }
    }

    fun dismissGoToPageDialog() {
        _uiState.update { it.copy(showGoToPageDialog = false) }
    }

    // ---- Crop & Import to Notebook ----

    fun toggleSelectCropMode() {
        _uiState.update { it.copy(isSelectCropMode = !it.isSelectCropMode) }
    }

    fun captureCropRegion(screenLeft: Float, screenTop: Float, screenRight: Float, screenBottom: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val page = _uiState.value.currentPage
                val fullBitmap = pdfBitmapPool.renderPage(page) ?: return@launch

                val bmpW = fullBitmap.width
                val bmpH = fullBitmap.height

                // Get screen dimensions from the application's display metrics
                val app = getApplication<android.app.Application>()
                val dm = app.resources.displayMetrics
                val screenW = dm.widthPixels.toFloat()
                val screenH = dm.heightPixels.toFloat()

                // The PDF page is displayed using ContentScale.Fit inside the full-screen pager.
                // Calculate the actual rendered page area (accounting for letterbox padding).
                val pageAspect = bmpW.toFloat() / bmpH.toFloat()
                val screenAspect = screenW / screenH

                val renderedW: Float
                val renderedH: Float
                val offsetX: Float
                val offsetY: Float

                if (pageAspect > screenAspect) {
                    // Page is wider than screen → horizontal fit, vertical letterbox
                    renderedW = screenW
                    renderedH = screenW / pageAspect
                    offsetX = 0f
                    offsetY = (screenH - renderedH) / 2f
                } else {
                    // Page is taller than screen → vertical fit, horizontal letterbox
                    renderedH = screenH
                    renderedW = screenH * pageAspect
                    offsetX = (screenW - renderedW) / 2f
                    offsetY = 0f
                }

                // Map screen coordinates to normalized page coordinates (0..1)
                val normLeft = ((screenLeft - offsetX) / renderedW).coerceIn(0f, 1f)
                val normTop = ((screenTop - offsetY) / renderedH).coerceIn(0f, 1f)
                val normRight = ((screenRight - offsetX) / renderedW).coerceIn(0f, 1f)
                val normBottom = ((screenBottom - offsetY) / renderedH).coerceIn(0f, 1f)

                // Convert to bitmap pixel coordinates
                val left = (normLeft * bmpW).toInt().coerceIn(0, bmpW - 1)
                val top = (normTop * bmpH).toInt().coerceIn(0, bmpH - 1)
                val right = (normRight * bmpW).toInt().coerceIn(left + 1, bmpW)
                val bottom = (normBottom * bmpH).toInt().coerceIn(top + 1, bmpH)

                val cropWidth = right - left
                val cropHeight = bottom - top

                if (cropWidth > 5 && cropHeight > 5) {
                    // Use Canvas to correctly render the ImageBitmap to an Android Bitmap
                    // (avoids BGRA/ARGB channel swapping that readPixels can cause)
                    val androidBitmap = android.graphics.Bitmap.createBitmap(bmpW, bmpH, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(androidBitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)

                    // Draw the Compose ImageBitmap onto the Android Canvas
                    val buffer = IntArray(bmpW * bmpH)
                    fullBitmap.readPixels(buffer)
                    androidBitmap.setPixels(buffer, 0, bmpW, 0, 0, bmpW, bmpH)

                    val croppedBitmap = android.graphics.Bitmap.createBitmap(androidBitmap, left, top, cropWidth, cropHeight)

                    // Save cropped bitmap to internal storage
                    val cropsDir = java.io.File(app.filesDir, "notebook_crops").also { it.mkdirs() }
                    val cropFile = java.io.File(cropsDir, "crop_${System.currentTimeMillis()}.png")
                    cropFile.outputStream().use { out ->
                        croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, out)
                    }

                    // Scale down image dimensions for notebook display (max 500px wide)
                    val displayScale = if (cropWidth > 500) 500f / cropWidth else 1f
                    val displayW = (cropWidth * displayScale)
                    val displayH = (cropHeight * displayScale)

                    // Add the crop as an image entry to notebook
                    addNotebookImage(cropFile.absolutePath, displayW, displayH)

                    androidBitmap.recycle()
                    croppedBitmap.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _uiState.update { it.copy(isSelectCropMode = false) }
        }
    }

    fun captureCropForRecall(screenLeft: Float, screenTop: Float, screenRight: Float, screenBottom: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val page = _uiState.value.currentPage
                val fullBitmap = pdfBitmapPool.renderPage(page) ?: return@launch

                val bmpW = fullBitmap.width
                val bmpH = fullBitmap.height

                val app = getApplication<android.app.Application>()
                val dm = app.resources.displayMetrics
                val screenW = dm.widthPixels.toFloat()
                val screenH = dm.heightPixels.toFloat()

                val pageAspect = bmpW.toFloat() / bmpH.toFloat()
                val screenAspect = screenW / screenH

                val renderedW: Float
                val renderedH: Float
                val offsetX: Float
                val offsetY: Float

                if (pageAspect > screenAspect) {
                    renderedW = screenW
                    renderedH = screenW / pageAspect
                    offsetX = 0f
                    offsetY = (screenH - renderedH) / 2f
                } else {
                    renderedH = screenH
                    renderedW = screenH * pageAspect
                    offsetX = (screenW - renderedW) / 2f
                    offsetY = 0f
                }

                val normLeft = ((screenLeft - offsetX) / renderedW).coerceIn(0f, 1f)
                val normTop = ((screenTop - offsetY) / renderedH).coerceIn(0f, 1f)
                val normRight = ((screenRight - offsetX) / renderedW).coerceIn(0f, 1f)
                val normBottom = ((screenBottom - offsetY) / renderedH).coerceIn(0f, 1f)

                val left = (normLeft * bmpW).toInt().coerceIn(0, bmpW - 1)
                val top = (normTop * bmpH).toInt().coerceIn(0, bmpH - 1)
                val right = (normRight * bmpW).toInt().coerceIn(left + 1, bmpW)
                val bottom = (normBottom * bmpH).toInt().coerceIn(top + 1, bmpH)

                val cropWidth = right - left
                val cropHeight = bottom - top

                if (cropWidth > 5 && cropHeight > 5) {
                    val androidBitmap = android.graphics.Bitmap.createBitmap(bmpW, bmpH, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(androidBitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)

                    val buffer = IntArray(bmpW * bmpH)
                    fullBitmap.readPixels(buffer)
                    androidBitmap.setPixels(buffer, 0, bmpW, 0, 0, bmpW, bmpH)

                    val croppedBitmap = android.graphics.Bitmap.createBitmap(androidBitmap, left, top, cropWidth, cropHeight)
                    androidBitmap.recycle()

                    val cropsDir = java.io.File(app.filesDir, "recall_crops").also { it.mkdirs() }
                    val cropFile = java.io.File(cropsDir, "recall_${System.currentTimeMillis()}.png")
                    cropFile.outputStream().use { out ->
                        croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, out)
                    }

                    val normCropRect = listOf(normLeft, normTop, normRight, normBottom)

                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isSelectCropMode = false,
                                showMarkRecallDialog = true,
                                pendingRecallCropBitmap = croppedBitmap,
                                pendingRecallCropPath = cropFile.absolutePath,
                                pendingRecallCropRect = normCropRect
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(isSelectCropMode = false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isSelectCropMode = false) }
            }
        }
    }

    fun startRecallFromBitmap(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<android.app.Application>()
            val cropsDir = java.io.File(app.filesDir, "recall_crops").also { it.mkdirs() }
            val cropFile = java.io.File(cropsDir, "recall_${System.currentTimeMillis()}.png")
            cropFile.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, out)
            }
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        showMarkRecallDialog = true,
                        pendingRecallCropBitmap = bitmap,
                        pendingRecallCropPath = cropFile.absolutePath,
                        pendingRecallCropRect = null
                    )
                }
            }
        }
    }

    fun savePendingQuickRecall(emoji: String, label: String, description: String, tagKey: String = "recall") {
        val state = _uiState.value
        val item = com.bookflow.reader.data.model.QuickRecallItem(
            id = java.util.UUID.randomUUID().toString(),
            bookUri = state.bookUri,
            bookTitle = state.bookTitle,
            pageIndex = state.currentPage,
            emoji = emoji,
            label = label,
            description = description,
            tagKey = tagKey,
            cropRect = state.pendingRecallCropRect,
            imagePath = state.pendingRecallCropPath,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveQuickRecallItem(item)
            val allItems = repository.getAllQuickRecallItems()
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        showMarkRecallDialog = false,
                        pendingRecallCropBitmap = null,
                        pendingRecallCropPath = null,
                        pendingRecallCropRect = null,
                        quickRecallItems = allItems
                    )
                }
            }
        }
    }

    fun dismissMarkRecallDialog() {
        _uiState.update {
            it.copy(
                showMarkRecallDialog = false,
                pendingRecallCropBitmap = null,
                pendingRecallCropPath = null,
                pendingRecallCropRect = null
            )
        }
    }

    fun triggerLaserHighlight(rect: List<Float>) {
        _uiState.update { it.copy(laserHighlightRect = rect) }
    }

    fun clearLaserHighlight() {
        _uiState.update { it.copy(laserHighlightRect = null) }
    }

    fun navigateToRecallItem(bookUri: String, pageIndex: Int, cropRect: List<Float>?) {
        _uiState.update {
            it.copy(
                currentPage = pageIndex,
                showQuickRecallIndex = false,
                laserHighlightRect = cropRect
            )
        }
    }

    fun getPageAspectRatio(pageIndex: Int): Float? {
        return pdfBitmapPool.getPageAspectRatio(pageIndex)
    }

    fun openQuickRecallIndex() {
        viewModelScope.launch(Dispatchers.IO) {
            val allItems = repository.getAllQuickRecallItems()
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        showQuickRecallIndex = true,
                        quickRecallItems = allItems
                    )
                }
            }
        }
    }

    fun closeQuickRecallIndex() {
        _uiState.update { it.copy(showQuickRecallIndex = false) }
    }

    fun deleteQuickRecallItem(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteQuickRecallItem(id)
            val allItems = repository.getAllQuickRecallItems()
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(quickRecallItems = allItems) }
            }
        }
    }

    fun toggleGlobalNotebookMode() {
        val nextMode = !_uiState.value.isGlobalNotebookMode
        infiniteUndoStack.clear()
        infiniteRedoStack.clear()
        viewModelScope.launch(Dispatchers.IO) {
            val notes = if (nextMode) {
                repository.loadGlobalInfiniteNotes()
            } else {
                repository.loadInfiniteNotes(_uiState.value.bookUri)
            }
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isGlobalNotebookMode = nextMode,
                        infiniteNotes = notes,
                        infiniteUndoCount = 0,
                        infiniteRedoCount = 0
                    )
                }
            }
        }
    }

    private fun saveCurrentInfiniteNotes(notes: List<AnnotationStroke>) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_uiState.value.isGlobalNotebookMode) {
                repository.saveGlobalInfiniteNotes(notes)
            } else {
                repository.saveInfiniteNotes(_uiState.value.bookUri, notes, _uiState.value.bookTitle)
            }
        }
    }

    private fun addNotebookImage(filePath: String, width: Float, height: Float) {
        val currentNotes = _uiState.value.infiniteNotes.toMutableList()
        infiniteUndoStack.add(currentNotes.toList())
        infiniteRedoStack.clear()
        
        var startX = 50f
        var startY = 50f
        val imageStrokes = currentNotes.filter { it.penType == 2 }
        if (imageStrokes.isNotEmpty()) {
            val lastImage = imageStrokes.last()
            if (lastImage.points.size >= 2) {
                startX = lastImage.points[0].x
                startY = lastImage.points[0].y + lastImage.points[1].y + 40f
            }
        }

        // Store image as a special "stroke" with imagePath encoded in a marker point
        // penType = 2 means "image", points encode position + size, color stores hash code of path
        val imageStroke = AnnotationStroke(
            points = listOf(
                AnnotationPoint(startX, startY),     // position x, y in world coords
                AnnotationPoint(width, height) // width, height of image
            ),
            color = filePath.hashCode().toLong(),
            strokeWidth = 0f,
            isHighlighter = false,
            penType = 2 // 2 = image marker
        )
        currentNotes.add(imageStroke)
        
        // Also save the filepath mapping
        repository.saveNotebookImagePath(filePath.hashCode().toLong(), filePath)
        
        _uiState.update {
            it.copy(
                infiniteNotes = currentNotes,
                infiniteUndoCount = infiniteUndoStack.size,
                infiniteRedoCount = 0,
                isInfiniteNoteOpen = true // Auto-open notebook when crop is added
            )
        }
        saveCurrentInfiniteNotes(currentNotes)
    }

    fun getNotebookImagePath(hashCode: Long): String? {
        return repository.getNotebookImagePath(hashCode)
    }

    // ---- Infinite Note Methods ----

    fun toggleInfiniteNote() {
        val willOpen = !_uiState.value.isInfiniteNoteOpen
        _uiState.update { it.copy(isInfiniteNoteOpen = willOpen) }
        if (willOpen) {
            reloadInfiniteNotes()
        }
    }

    fun reloadInfiniteNotes() {
        viewModelScope.launch(Dispatchers.IO) {
            val notes = if (_uiState.value.isGlobalNotebookMode) {
                repository.loadGlobalInfiniteNotes()
            } else {
                repository.loadInfiniteNotes(_uiState.value.bookUri)
            }
            val boxes = repository.loadNoteTextBoxes(_uiState.value.bookUri)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        infiniteNotes = notes,
                        noteTextBoxes = boxes,
                        infiniteUndoCount = 0,
                        infiniteRedoCount = 0
                    )
                }
            }
        }
    }

    fun exportInfiniteNoteToPdf(context: Context) {
        val notes = _uiState.value.infiniteNotes
        if (notes.isEmpty()) {
            android.widget.Toast.makeText(context, "Note is empty", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val imageMap = mutableMapOf<Long, String>()
            // Collect all image paths from strokes with penType 2
            notes.filter { it.penType == 2 }.forEach { stroke ->
                val hash = stroke.color
                val path = repository.getNotebookImagePath(hash)
                if (path != null) {
                    imageMap[hash] = path
                }
            }

            val file = com.bookflow.reader.util.PdfExporter.exportNotesToPdf(
                context = context,
                strokes = notes,
                imagePaths = imageMap,
                fileName = "BookFlow_${System.currentTimeMillis()}"
            )

            withContext(Dispatchers.Main) {
                if (file != null) {
                    com.bookflow.reader.util.PdfExporter.sharePdf(context, file)
                } else {
                    android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun addInfiniteStroke(stroke: AnnotationStroke) {
        val currentState = _uiState.value.infiniteNotes
        infiniteUndoStack.add(currentState.toList())
        infiniteRedoStack.clear()
        
        val currentNotes = currentState.toMutableList()
        currentNotes.add(stroke)
        _uiState.update { 
            it.copy(
                infiniteNotes = currentNotes,
                infiniteUndoCount = infiniteUndoStack.size,
                infiniteRedoCount = 0
            ) 
        }
        saveCurrentInfiniteNotes(currentNotes)
    }

    fun clearInfiniteNotes() {
        if (_uiState.value.infiniteNotes.isEmpty()) return
        infiniteUndoStack.add(_uiState.value.infiniteNotes.toList())
        infiniteRedoStack.clear()
        
        _uiState.update { 
            it.copy(
                infiniteNotes = emptyList(),
                infiniteUndoCount = infiniteUndoStack.size,
                infiniteRedoCount = 0
            ) 
        }
        saveCurrentInfiniteNotes(emptyList())
    }

    fun undoInfinite() {
        if (infiniteUndoStack.isEmpty()) return
        val currentState = _uiState.value.infiniteNotes
        infiniteRedoStack.add(currentState.toList())
        
        val previousState = infiniteUndoStack.removeLast()
        _uiState.update {
            it.copy(
                infiniteNotes = previousState,
                infiniteUndoCount = infiniteUndoStack.size,
                infiniteRedoCount = infiniteRedoStack.size
            )
        }
        saveCurrentInfiniteNotes(previousState)
    }

    fun redoInfinite() {
        if (infiniteRedoStack.isEmpty()) return
        val currentState = _uiState.value.infiniteNotes
        infiniteUndoStack.add(currentState.toList())
        
        val nextState = infiniteRedoStack.removeLast()
        _uiState.update {
            it.copy(
                infiniteNotes = nextState,
                infiniteUndoCount = infiniteUndoStack.size,
                infiniteRedoCount = infiniteRedoStack.size
            )
        }
        saveCurrentInfiniteNotes(nextState)
    }

    private var infiniteEraserSnapshot: List<AnnotationStroke>? = null

    fun eraseInfiniteWithOptions(worldX: Float, worldY: Float, radius: Float, mode: EraserMode, targetPageIndex: Int = -1) {
        val currentNotes = _uiState.value.infiniteNotes
        if (infiniteEraserSnapshot == null) {
            infiniteEraserSnapshot = currentNotes.toList()
        }
        val (updated, changed) = com.bookflow.reader.util.NoteEraserHelper.eraseStrokesAt(
            currentNotes,
            worldX,
            worldY,
            radius,
            mode,
            targetPageIndex
        )
        if (changed) {
            _uiState.update { it.copy(infiniteNotes = updated) }
        }
    }

    fun finishInfiniteEraserGesture() {
        val snapshot = infiniteEraserSnapshot
        val currentNotes = _uiState.value.infiniteNotes
        if (snapshot != null) {
            if (snapshot != currentNotes) {
                infiniteUndoStack.add(snapshot)
                infiniteRedoStack.clear()
                _uiState.update { 
                    it.copy(
                        infiniteUndoCount = infiniteUndoStack.size,
                        infiniteRedoCount = 0
                    ) 
                }
                saveCurrentInfiniteNotes(currentNotes)
            }
            infiniteEraserSnapshot = null
        }
    }

    fun eraseInfiniteAt(worldX: Float, worldY: Float) {
        eraseInfiniteWithOptions(worldX, worldY, 18f, _uiState.value.eraserMode)
        finishInfiniteEraserGesture()
    }

    // ---- Sticky Notes ----

    private val stickyNotesMap = mutableMapOf<Int, MutableList<com.bookflow.reader.data.model.StickyNote>>() // pageIndex -> notes

    fun addStickyNote(pageIndex: Int, normalizedX: Float, normalizedY: Float, color: Long) {
        val note = com.bookflow.reader.data.model.StickyNote(
            pageIndex = pageIndex,
            x = normalizedX,
            y = normalizedY,
            color = color,
            isExpanded = true
        )
        val pageNotes = stickyNotesMap.getOrPut(pageIndex) { mutableListOf() }
        pageNotes.add(note)
        updateStickyNotesState()
        saveStickyNotes()
    }

    fun updateStickyNote(updatedNote: com.bookflow.reader.data.model.StickyNote) {
        val pageNotes = stickyNotesMap[updatedNote.pageIndex] ?: return
        val index = pageNotes.indexOfFirst { it.id == updatedNote.id }
        if (index >= 0) {
            pageNotes[index] = updatedNote
            updateStickyNotesState()
            saveStickyNotes()
        }
    }

    fun deleteStickyNote(noteId: String) {
        stickyNotesMap.values.forEach { pageNotes ->
            pageNotes.removeAll { it.id == noteId }
        }
        updateStickyNotesState()
        saveStickyNotes()
    }

    fun toggleStickyNoteExpand(noteId: String) {
        stickyNotesMap.values.forEach { pageNotes ->
            val index = pageNotes.indexOfFirst { it.id == noteId }
            if (index >= 0) {
                pageNotes[index] = pageNotes[index].copy(isExpanded = !pageNotes[index].isExpanded)
            }
        }
        updateStickyNotesState()
    }

    fun getStickyNotesForPage(pageIndex: Int): List<com.bookflow.reader.data.model.StickyNote> {
        return stickyNotesMap[pageIndex] ?: emptyList()
    }

    private fun updateStickyNotesState() {
        val currentPageNotes = stickyNotesMap[_uiState.value.currentPage] ?: emptyList()
        _uiState.update { it.copy(stickyNotes = currentPageNotes) }
    }

    private fun saveStickyNotes() {
        val allNotes = stickyNotesMap.values.flatten()
        repository.saveStickyNotes(_uiState.value.bookUri, allNotes)
    }

    private fun loadStickyNotes() {
        val notes = repository.loadStickyNotes(_uiState.value.bookUri)
        stickyNotesMap.clear()
        notes.forEach { note ->
            stickyNotesMap.getOrPut(note.pageIndex) { mutableListOf() }.add(note)
        }
        updateStickyNotesState()
    }

    // ---- Canvas Template ----

    fun cycleCanvasTemplate() {
        val templates = com.bookflow.reader.data.model.CanvasTemplate.values()
        val currentIndex = templates.indexOf(_uiState.value.canvasTemplate)
        val nextTemplate = templates[(currentIndex + 1) % templates.size]
        _uiState.update { it.copy(canvasTemplate = nextTemplate) }
    }

    // ---- Shape Mode ----

    fun toggleShapeMode() {
        _uiState.update { it.copy(isShapeModeEnabled = !it.isShapeModeEnabled) }
    }

    // ---- Template Insert ----

    fun toggleTemplateInsert() {
        _uiState.update { it.copy(showTemplateInsert = !it.showTemplateInsert) }
    }

    fun insertTemplateStrokes(templateStrokes: List<AnnotationStroke>) {
        val currentNotes = _uiState.value.infiniteNotes.toMutableList()
        infiniteUndoStack.add(currentNotes.toList())
        infiniteRedoStack.clear()
        currentNotes.addAll(templateStrokes)
        _uiState.update {
            it.copy(
                infiniteNotes = currentNotes,
                infiniteUndoCount = infiniteUndoStack.size,
                infiniteRedoCount = 0,
                showTemplateInsert = false
            )
        }
        saveCurrentInfiniteNotes(currentNotes)
    }

    // ---- Note Text Boxes ----

    fun addNoteTextBox(x: Float, y: Float, pageIndex: Int = 0) {
        val textBox = com.bookflow.reader.data.model.NoteTextBox(x = x, y = y, pageIndex = pageIndex)
        val current = _uiState.value.noteTextBoxes.toMutableList()
        current.add(textBox)
        _uiState.update { it.copy(noteTextBoxes = current) }
        saveNoteTextBoxes()
    }

    fun addNoteTextBox(textBox: com.bookflow.reader.data.model.NoteTextBox) {
        val current = _uiState.value.noteTextBoxes.toMutableList()
        current.add(textBox)
        _uiState.update { it.copy(noteTextBoxes = current) }
        saveNoteTextBoxes()
    }

    fun updateNoteTextBox(updated: com.bookflow.reader.data.model.NoteTextBox) {
        val current = _uiState.value.noteTextBoxes.toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            current[index] = updated
            _uiState.update { it.copy(noteTextBoxes = current) }
            saveNoteTextBoxes()
        }
    }

    fun deleteNoteTextBox(id: String) {
        val current = _uiState.value.noteTextBoxes.filter { it.id != id }
        _uiState.update { it.copy(noteTextBoxes = current) }
        saveNoteTextBoxes()
    }

    private fun saveNoteTextBoxes() {
        repository.saveNoteTextBoxes(_uiState.value.bookUri, _uiState.value.noteTextBoxes)
    }

    private fun loadNoteTextBoxes() {
        val boxes = repository.loadNoteTextBoxes(_uiState.value.bookUri)
        _uiState.update { it.copy(noteTextBoxes = boxes) }
    }

    // ---- Revision Book ----

    fun showRevisionBook() {
        val entries = buildRevisionEntries()
        _uiState.update { it.copy(showRevisionBook = true, revisionEntries = entries) }
    }

    fun hideRevisionBook() {
        _uiState.update { it.copy(showRevisionBook = false) }
    }

    private fun buildRevisionEntries(): List<com.bookflow.reader.data.model.RevisionEntry> {
        val entries = mutableListOf<com.bookflow.reader.data.model.RevisionEntry>()

        // Collect all stroke annotations from all pages
        for (pageIndex in 0 until _uiState.value.totalPages) {
            val pageAnnotations = annotationsCache[pageIndex] ?: continue
            for (stroke in pageAnnotations.strokes) {
                val type = when {
                    stroke.penType == 3 -> "rect_highlight"
                    stroke.penType == 4 -> "shape"
                    stroke.isHighlighter -> "highlight"
                    else -> "pen"
                }
                entries.add(
                    com.bookflow.reader.data.model.RevisionEntry(
                        pageIndex = pageIndex,
                        annotationType = type,
                        stroke = stroke,
                        timestamp = stroke.timestamp
                    )
                )
            }
        }

        // Collect all sticky notes
        stickyNotesMap.forEach { (pageIndex, notes) ->
            notes.forEach { note ->
                entries.add(
                    com.bookflow.reader.data.model.RevisionEntry(
                        pageIndex = pageIndex,
                        annotationType = "sticky",
                        stickyNote = note,
                        timestamp = note.timestamp
                    )
                )
            }
        }

        return entries.sortedWith(compareBy({ it.pageIndex }, { it.timestamp }))
    }

    // ---- Lasso Move/Delete ----

    fun moveInfiniteStrokes(indices: Set<Int>, dx: Float, dy: Float) {
        val currentNotes = _uiState.value.infiniteNotes.toMutableList()
        infiniteUndoStack.add(currentNotes.toList())
        infiniteRedoStack.clear()
        indices.forEach { idx ->
            if (idx < currentNotes.size) {
                val stroke = currentNotes[idx]
                val movedPoints = if (stroke.penType == 2 && stroke.points.size >= 2) {
                    val newPoints = stroke.points.toMutableList()
                    newPoints[0] = newPoints[0].copy(x = newPoints[0].x + dx, y = newPoints[0].y + dy)
                    newPoints
                } else {
                    stroke.points.map { it.copy(x = it.x + dx, y = it.y + dy) }
                }
                currentNotes[idx] = stroke.copy(points = movedPoints)
            }
        }
        _uiState.update {
            it.copy(
                infiniteNotes = currentNotes,
                infiniteUndoCount = infiniteUndoStack.size,
                infiniteRedoCount = 0
            )
        }
        saveCurrentInfiniteNotes(currentNotes)
    }

    fun scaleInfiniteStrokes(indices: Set<Int>, scaleFactor: Float, originX: Float, originY: Float) {
        val currentNotes = _uiState.value.infiniteNotes.toMutableList()
        infiniteUndoStack.add(currentNotes.toList())
        infiniteRedoStack.clear()
        indices.forEach { idx ->
            if (idx < currentNotes.size) {
                val stroke = currentNotes[idx]
                val scaledPoints = if (stroke.penType == 2 && stroke.points.size >= 2) {
                    val newPoints = stroke.points.toMutableList()
                    val oldX = newPoints[0].x
                    val oldY = newPoints[0].y
                    val oldW = newPoints[1].x
                    val oldH = newPoints[1].y
                    newPoints[0] = newPoints[0].copy(
                        x = originX + (oldX - originX) * scaleFactor, 
                        y = originY + (oldY - originY) * scaleFactor
                    )
                    newPoints[1] = newPoints[1].copy(
                        x = oldW * scaleFactor,
                        y = oldH * scaleFactor
                    )
                    newPoints
                } else {
                    stroke.points.map { 
                        it.copy(
                            x = originX + (it.x - originX) * scaleFactor, 
                            y = originY + (it.y - originY) * scaleFactor
                        ) 
                    }
                }
                currentNotes[idx] = stroke.copy(points = scaledPoints, strokeWidth = stroke.strokeWidth * scaleFactor)
            }
        }
        _uiState.update {
            it.copy(
                infiniteNotes = currentNotes,
                infiniteUndoCount = infiniteUndoStack.size,
                infiniteRedoCount = 0
            )
        }
        saveCurrentInfiniteNotes(currentNotes)
    }

    fun deleteInfiniteStrokes(indices: Set<Int>) {
        val currentNotes = _uiState.value.infiniteNotes.toMutableList()
        infiniteUndoStack.add(currentNotes.toList())
        infiniteRedoStack.clear()
        // Remove in reverse order to maintain indices
        indices.sortedDescending().forEach { idx ->
            if (idx < currentNotes.size) {
                currentNotes.removeAt(idx)
            }
        }
        _uiState.update {
            it.copy(
                infiniteNotes = currentNotes,
                infiniteUndoCount = infiniteUndoStack.size,
                infiniteRedoCount = 0
            )
        }
        saveCurrentInfiniteNotes(currentNotes)
    }

    fun toggleTableOfContents() {
        _uiState.update { it.copy(showTableOfContents = !it.showTableOfContents) }
    }
    
    fun loadTableOfContents(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(_uiState.value.bookUri)
                val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@launch
                val renderer = android.graphics.pdf.PdfRenderer(fd)
                val entries = mutableListOf<com.bookflow.reader.ui.components.TocEntry>()
                
                // PdfRenderer doesn't expose TOC, so build a simple page-number index
                val totalPages = renderer.pageCount
                val step = when {
                    totalPages > 200 -> 20
                    totalPages > 100 -> 10
                    totalPages > 50 -> 5
                    else -> 10
                }
                for (i in 0 until totalPages step step) {
                    entries.add(com.bookflow.reader.ui.components.TocEntry(
                        title = "Page ${i + 1}",
                        pageIndex = i,
                        level = 0
                    ))
                }
                
                renderer.close()
                fd.close()
                
                _uiState.update { it.copy(tocEntries = entries) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun setPageTransitionStyle(style: Int) {
        _uiState.update { it.copy(pageTransitionStyle = style.coerceIn(0, 3)) }
    }

    fun toggleReadingStats() {
        if (!_uiState.value.showReadingStats) {
            val stats = repository.getReadingStats()
            _uiState.update { it.copy(showReadingStats = true, readingStats = stats) }
        } else {
            _uiState.update { it.copy(showReadingStats = false) }
        }
    }
    
    fun toggleInkReplay() {
        _uiState.update { it.copy(showInkReplay = !it.showInkReplay) }
    }

    // ---- Audio Notes ----
    
    fun getAudioNotesForPage(pageIndex: Int): List<com.bookflow.reader.data.model.AudioNote> {
        return _uiState.value.currentAudioNotes.filter { it.pageIndex == pageIndex }
    }

    fun addAudioNote(pageIndex: Int, x: Float, y: Float) {
        val currentNotes = _uiState.value.currentAudioNotes.toMutableList()
        currentNotes.add(
            com.bookflow.reader.data.model.AudioNote(
                pageIndex = pageIndex,
                x = x,
                y = y
            )
        )
        _uiState.update { it.copy(currentAudioNotes = currentNotes) }
        // Note: Ideally, these should be saved to the repository
    }

    fun updateAudioNote(updatedNote: com.bookflow.reader.data.model.AudioNote) {
        val currentNotes = _uiState.value.currentAudioNotes.toMutableList()
        val index = currentNotes.indexOfFirst { it.id == updatedNote.id }
        if (index != -1) {
            currentNotes[index] = updatedNote
            _uiState.update { it.copy(currentAudioNotes = currentNotes) }
            // Note: Save to repository
        }
    }

    fun deleteAudioNote(id: String) {
        val currentNotes = _uiState.value.currentAudioNotes.toMutableList()
        val index = currentNotes.indexOfFirst { it.id == id }
        if (index != -1) {
            val note = currentNotes[index]
            if (note.filePath.isNotEmpty()) {
                java.io.File(note.filePath).delete()
            }
            currentNotes.removeAt(index)
            _uiState.update { it.copy(currentAudioNotes = currentNotes) }
            // Note: Save to repository
        }
    }

    // ---- Selectable Text Layer Methods ----

    fun getPageText(pageIndex: Int): com.bookflow.reader.util.PageTextContent? {
        val cached = com.bookflow.reader.util.PdfTextManager.getCached(pageIndex)
        if (cached != null) return cached

        viewModelScope.launch {
            val bitmap = pdfBitmapPool.renderPageRawBitmap(pageIndex)
            if (bitmap != null) {
                com.bookflow.reader.util.PdfTextManager.extractTextFromBitmap(pageIndex, bitmap)
                _uiState.update { it.copy(annotationsVersion = it.annotationsVersion + 1) }
            }
        }
        return null
    }

    fun highlightTextRects(pageIndex: Int, rects: List<androidx.compose.ui.geometry.Rect>) {
        if (rects.isEmpty()) return
        for (r in rects) {
            val p1 = AnnotationPoint(r.left, r.top)
            val p2 = AnnotationPoint(r.right, r.bottom)
            val stroke = AnnotationStroke(
                points = listOf(p1, p2),
                color = 0x66FFD54FL, // Yellow translucent highlighter
                strokeWidth = 0f,
                isHighlighter = true,
                penType = 3 // Rect highlight
            )
            addStrokeForPage(pageIndex, stroke)
        }
    }

    fun startRecallFromText(pageIndex: Int, text: String, rect: androidx.compose.ui.geometry.Rect) {
        viewModelScope.launch(Dispatchers.IO) {
            val rawBmp = pdfBitmapPool.renderPageRawBitmap(pageIndex) ?: return@launch
            val bmpW = rawBmp.width
            val bmpH = rawBmp.height

            val cropX = ((rect.left - 0.02f).coerceIn(0f, 1f) * bmpW).toInt()
            val cropY = ((rect.top - 0.02f).coerceIn(0f, 1f) * bmpH).toInt()
            val cropW = (((rect.right - rect.left) + 0.04f).coerceIn(0.01f, 1f) * bmpW).toInt().coerceAtMost(bmpW - cropX)
            val cropH = (((rect.bottom - rect.top) + 0.04f).coerceIn(0.01f, 1f) * bmpH).toInt().coerceAtMost(bmpH - cropY)

            if (cropW > 10 && cropH > 10) {
                val cropped = android.graphics.Bitmap.createBitmap(rawBmp, cropX, cropY, cropW, cropH)
                val app = getApplication<Application>()
                val cropsDir = java.io.File(app.filesDir, "recall_crops").also { it.mkdirs() }
                val cropFile = java.io.File(cropsDir, "recall_${System.currentTimeMillis()}.png")
                java.io.FileOutputStream(cropFile).use { out ->
                    cropped.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, out)
                }
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            showMarkRecallDialog = true,
                            pendingRecallCropBitmap = cropped,
                            pendingRecallCropPath = cropFile.absolutePath,
                            pendingRecallCropRect = listOf(rect.left, rect.top, rect.right, rect.bottom)
                        )
                    }
                }
            }
        }
    }

    fun addStickyNoteFromText(pageIndex: Int, text: String) {
        val note = com.bookflow.reader.data.model.StickyNote(
            pageIndex = pageIndex,
            x = 0.5f,
            y = 0.5f,
            text = text,
            color = 0xFFFFF59DL,
            isExpanded = true
        )
        val pageNotes = stickyNotesMap.getOrPut(pageIndex) { mutableListOf() }
        pageNotes.add(note)
        updateStickyNotesState()
        saveStickyNotes()
    }

    override fun onCleared() {
        super.onCleared()
        // Save current reading session
        val now = System.currentTimeMillis()
        val duration = now - sessionStartTime
        if (duration > 30000 && _uiState.value.bookUri.isNotEmpty()) { // Only save if > 30 seconds
            repository.saveReadingSession(
                bookUri = _uiState.value.bookUri,
                startTime = sessionStartTime,
                endTime = now,
                startPage = sessionStartPage,
                endPage = _uiState.value.currentPage
            )
        }
        pdfBitmapPool.close()
    }

    private fun distSqPointToSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = bx - ax
        val dy = by - ay
        val lenSq = dx * dx + dy * dy
        if (lenSq <= 0.0000001f) {
            val dpx = px - ax
            val dpy = py - ay
            return dpx * dpx + dpy * dpy
        }
        val t = ((px - ax) * dx + (py - ay) * dy) / lenSq
        val clampedT = t.coerceIn(0f, 1f)
        val projX = ax + clampedT * dx
        val projY = ay + clampedT * dy
        val dpx = px - projX
        val dpy = py - projY
        return dpx * dpx + dpy * dpy
    }

    private fun strokeIntersectsEraser(stroke: AnnotationStroke, cx: Float, cy: Float, radius: Float): Boolean {
        val rSq = radius * radius
        val points = stroke.points
        if (points.isEmpty()) return false
        if (points.size == 1) {
            val dpx = points[0].x - cx
            val dpy = points[0].y - cy
            return dpx * dpx + dpy * dpy <= rSq
        }
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            if (distSqPointToSegment(cx, cy, p1.x, p1.y, p2.x, p2.y) <= rSq) {
                return true
            }
        }
        return false
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReaderViewModel(application) as T
            }
        }
    }
}
