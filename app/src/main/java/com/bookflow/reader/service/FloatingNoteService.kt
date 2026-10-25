package com.bookflow.reader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.bookflow.reader.MainActivity
import com.bookflow.reader.data.model.AnnotationStroke
import com.bookflow.reader.data.model.CanvasTemplate
import com.bookflow.reader.data.model.NoteTextBox
import com.bookflow.reader.data.repository.BookRepository
import com.bookflow.reader.ui.components.EraserMode
import com.bookflow.reader.ui.components.InfiniteNotePad
import com.bookflow.reader.ui.theme.BookFlowDarkScheme
import com.bookflow.reader.ui.theme.BookFlowTypography
import com.bookflow.reader.ui.theme.WarmCream
import com.bookflow.reader.util.NoteEraserHelper

enum class OverlayWindowState {
    WINDOWED,
    BUBBLE,
    FULLSCREEN
}

/**
 * Singleton data bridge between the BookFlow app and FloatingNoteService.
 */
object FloatingNoteData {
    var strokes: List<AnnotationStroke> = emptyList()
    var textBoxes: List<NoteTextBox> = emptyList()
    var canvasTemplate: CanvasTemplate = CanvasTemplate.DOT_GRID
    var bookUri: String = ""
    var bookTitle: String = ""
    var universalNoteId: String = ""
    var isGlobalMode: Boolean = false
    var isStylusOnly: Boolean = true
    var isActive: Boolean = false

    fun clear() {
        strokes = emptyList()
        textBoxes = emptyList()
        canvasTemplate = CanvasTemplate.DOT_GRID
        bookUri = ""
        bookTitle = ""
        universalNoteId = ""
        isGlobalMode = false
        isStylusOnly = true
        isActive = false
    }
}

/**
 * FloatingNoteService — Hosts InfiniteNotePad as a true system overlay window
 * (TYPE_APPLICATION_OVERLAY) floating over all other apps on the device.
 */
class FloatingNoteService : Service() {

    companion object {
        private const val CHANNEL_ID = "floating_note_channel"
        private const val NOTIFICATION_ID = 2001
        const val ACTION_STOP = "com.bookflow.reader.service.STOP_FLOATING"

        fun start(context: Context) {
            val intent = Intent(context, FloatingNoteService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingNoteService::class.java))
        }
    }

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private lateinit var repository: BookRepository
    private val lifecycleOwner = ServiceLifecycleOwner()

    // Window layout params state
    private lateinit var windowParams: WindowManager.LayoutParams
    private var screenWidth = 1200
    private var screenHeight = 800

    private var windowX = 100
    private var windowY = 100
    private var windowWidth = 850
    private var windowHeight = 650

    private var bubbleX = 100
    private var bubbleY = 200
    private var bubbleSizePx = 64

    private var currentWindowState by mutableStateOf(OverlayWindowState.WINDOWED)

    override fun onCreate() {
        super.onCreate()
        repository = BookRepository(applicationContext)
        FloatingNoteData.isActive = true

        val metrics = resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        val density = metrics.density

        // Default proportional size: ~70% screen width, 75% screen height
        windowWidth = (screenWidth * 0.70f).toInt().coerceAtLeast((400 * density).toInt())
        windowHeight = (screenHeight * 0.75f).toInt().coerceAtLeast((350 * density).toInt())
        windowX = ((screenWidth - windowWidth) / 2).coerceAtLeast(0)
        windowY = ((screenHeight - windowHeight) / 3).coerceAtLeast(50)

        bubbleSizePx = (64 * density).toInt()
        bubbleX = screenWidth - bubbleSizePx - 20
        bubbleY = (screenHeight * 0.35f).toInt()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        lifecycleOwner.onCreate()
        showOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        lifecycleOwner.onDestroy()
        removeOverlay()
        FloatingNoteData.isActive = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Notepad Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the floating notepad active over other apps"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val returnIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, returnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingNoteService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val noteName = if (FloatingNoteData.isGlobalMode) {
            "Universal Notebook"
        } else if (FloatingNoteData.bookTitle.isNotBlank()) {
            FloatingNoteData.bookTitle.take(24)
        } else {
            "Floating Notepad"
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Floating Note • $noteName")
                .setContentText("Tap to open BookFlow • Floating over all apps")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(
                    Notification.Action.Builder(
                        null, "Close Notepad", stopPendingIntent
                    ).build()
                )
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Floating Note • $noteName")
                .setContentText("Tap to open BookFlow")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        }
    }

    private fun updateWindowLayout() {
        if (windowManager == null || composeView == null) return

        when (currentWindowState) {
            OverlayWindowState.WINDOWED -> {
                windowParams.width = windowWidth
                windowParams.height = windowHeight
                windowParams.x = windowX
                windowParams.y = windowY
            }
            OverlayWindowState.BUBBLE -> {
                windowParams.width = bubbleSizePx
                windowParams.height = bubbleSizePx
                windowParams.x = bubbleX
                windowParams.y = bubbleY
            }
            OverlayWindowState.FULLSCREEN -> {
                windowParams.width = WindowManager.LayoutParams.MATCH_PARENT
                windowParams.height = WindowManager.LayoutParams.MATCH_PARENT
                windowParams.x = 0
                windowParams.y = 0
            }
        }
        try {
            windowManager?.updateViewLayout(composeView, windowParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        windowParams = WindowManager.LayoutParams(
            windowWidth,
            windowHeight,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = windowX
            this.y = windowY
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                MaterialTheme(
                    colorScheme = BookFlowDarkScheme,
                    typography = BookFlowTypography
                ) {
                    FloatingOverlayHost()
                }
            }
        }

        lifecycleOwner.onStart()
        windowManager?.addView(composeView, windowParams)
        lifecycleOwner.onResume()
    }

    private fun removeOverlay() {
        try {
            composeView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        composeView = null
    }

    private fun returnToBookFlow() {
        val intent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    @Composable
    private fun FloatingOverlayHost() {
        when (currentWindowState) {
            OverlayWindowState.BUBBLE -> {
                FloatingBubbleView(
                    onExpand = {
                        currentWindowState = OverlayWindowState.WINDOWED
                        updateWindowLayout()
                    },
                    onClose = {
                        stopSelf()
                    },
                    onDrag = { dx, dy ->
                        bubbleX = (bubbleX + dx.toInt()).coerceIn(0, screenWidth - bubbleSizePx)
                        bubbleY = (bubbleY + dy.toInt()).coerceIn(0, screenHeight - bubbleSizePx)
                        updateWindowLayout()
                    }
                )
            }
            OverlayWindowState.WINDOWED, OverlayWindowState.FULLSCREEN -> {
                FloatingNotepadContent(
                    isFullScreen = currentWindowState == OverlayWindowState.FULLSCREEN,
                    onToggleFullScreen = {
                        currentWindowState = if (currentWindowState == OverlayWindowState.FULLSCREEN) {
                            OverlayWindowState.WINDOWED
                        } else {
                            OverlayWindowState.FULLSCREEN
                        }
                        updateWindowLayout()
                    },
                    onMinimizeToBubble = {
                        currentWindowState = OverlayWindowState.BUBBLE
                        updateWindowLayout()
                    },
                    onDragWindow = { dx, dy ->
                        if (currentWindowState == OverlayWindowState.WINDOWED) {
                            windowX = (windowX + dx.toInt()).coerceIn(0, screenWidth - 100)
                            windowY = (windowY + dy.toInt()).coerceIn(0, screenHeight - 100)
                            updateWindowLayout()
                        }
                    },
                    onResizeWindow = { dw, dh ->
                        if (currentWindowState == OverlayWindowState.WINDOWED) {
                            windowWidth = (windowWidth + dw.toInt()).coerceIn(400, screenWidth)
                            windowHeight = (windowHeight + dh.toInt()).coerceIn(350, screenHeight)
                            updateWindowLayout()
                        }
                    },
                    onReturnToApp = {
                        returnToBookFlow()
                    },
                    onClose = {
                        stopSelf()
                    }
                )
            }
        }
    }

    @Composable
    private fun FloatingBubbleView(
        onExpand: () -> Unit,
        onClose: () -> Unit,
        onDrag: (Float, Float) -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF790D16),
                                Color(0xFF1E1417)
                            )
                        )
                    )
                    .border(2.dp, Color(0xFFE5D3AF).copy(alpha = 0.8f), CircleShape)
                    .clickable { onExpand() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.EditNote,
                    contentDescription = "Expand Floating Notes",
                    tint = WarmCream,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }

    @Composable
    private fun FloatingNotepadContent(
        isFullScreen: Boolean,
        onToggleFullScreen: () -> Unit,
        onMinimizeToBubble: () -> Unit,
        onDragWindow: (Float, Float) -> Unit,
        onResizeWindow: (Float, Float) -> Unit,
        onReturnToApp: () -> Unit,
        onClose: () -> Unit
    ) {
        var strokes by remember { mutableStateOf(FloatingNoteData.strokes) }
        var textBoxes by remember { mutableStateOf(FloatingNoteData.textBoxes) }
        var canvasTemplate by remember { mutableStateOf(FloatingNoteData.canvasTemplate) }
        var isShapeModeEnabled by remember { mutableStateOf(false) }
        var showTemplateInsert by remember { mutableStateOf(false) }

        val undoStack = remember { mutableListOf<List<AnnotationStroke>>() }
        val redoStack = remember { mutableListOf<List<AnnotationStroke>>() }
        var undoCount by remember { mutableStateOf(0) }
        var redoCount by remember { mutableStateOf(0) }

        var eraserGestureStartSnapshot by remember { mutableStateOf<List<AnnotationStroke>?>(null) }

        fun saveNotes(notes: List<AnnotationStroke>) {
            FloatingNoteData.strokes = notes
            Thread {
                try {
                    if (FloatingNoteData.universalNoteId.isNotBlank()) {
                        repository.saveUniversalNoteStrokes(FloatingNoteData.universalNoteId, notes)
                    } else if (FloatingNoteData.bookUri.isNotBlank()) {
                        repository.saveInfiniteNotes(
                            FloatingNoteData.bookUri,
                            notes,
                            FloatingNoteData.bookTitle
                        )
                    } else if (FloatingNoteData.isGlobalMode) {
                        repository.saveGlobalInfiniteNotes(notes)
                    } else {
                        repository.saveGlobalInfiniteNotes(notes)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }

        fun saveTextBoxes(boxes: List<NoteTextBox>) {
            FloatingNoteData.textBoxes = boxes
            Thread {
                try {
                    if (FloatingNoteData.universalNoteId.isNotBlank()) {
                        repository.saveUniversalNoteTextBoxes(FloatingNoteData.universalNoteId, boxes)
                    } else if (FloatingNoteData.bookUri.isNotBlank()) {
                        repository.saveNoteTextBoxes(FloatingNoteData.bookUri, boxes)
                    } else {
                        repository.saveUniversalNoteTextBoxes("default_universal_note", boxes)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }

        val noteTitle = if (FloatingNoteData.isGlobalMode) {
            "Universal Notebook"
        } else if (FloatingNoteData.bookTitle.isNotBlank()) {
            "${FloatingNoteData.bookTitle.take(24)} • Floating"
        } else {
            "Floating Notebook"
        }

        InfiniteNotePad(
            strokes = strokes,
            canUndo = undoCount > 0,
            canRedo = redoCount > 0,
            canvasTemplate = canvasTemplate,
            isShapeModeEnabled = isShapeModeEnabled,
            noteTextBoxes = textBoxes,
            showTemplateInsert = showTemplateInsert,
            noteTitle = noteTitle,
            initialFullScreen = isFullScreen,
            stylusOnly = FloatingNoteData.isStylusOnly,
            isGlobalNotebook = FloatingNoteData.isGlobalMode,
            isOverlayMode = true,
            onMinimizeToBubble = {
                FloatingNoteData.strokes = strokes
                FloatingNoteData.textBoxes = textBoxes
                FloatingNoteData.canvasTemplate = canvasTemplate
                saveNotes(strokes)
                saveTextBoxes(textBoxes)
                onMinimizeToBubble()
            },
            onDragOverlay = onDragWindow,
            onResizeOverlay = onResizeWindow,
            onReturnToApp = {
                FloatingNoteData.strokes = strokes
                FloatingNoteData.textBoxes = textBoxes
                FloatingNoteData.canvasTemplate = canvasTemplate
                saveNotes(strokes)
                saveTextBoxes(textBoxes)
                onReturnToApp()
            },
            onAddStroke = { stroke ->
                undoStack.add(strokes.toList())
                redoStack.clear()
                val updated = strokes + stroke
                strokes = updated
                undoCount = undoStack.size
                redoCount = 0
                saveNotes(updated)
            },
            onEraseAt = { x, y ->
                val (result, didChange) = NoteEraserHelper.eraseStrokesAt(
                    strokes, x, y, 24f, EraserMode.STROKE
                )
                if (didChange) {
                    strokes = result
                    saveNotes(result)
                }
            },
            onEraseStrokeAt = { x, y, radius, mode, pageIndex ->
                if (eraserGestureStartSnapshot == null) {
                    eraserGestureStartSnapshot = strokes.toList()
                }
                val (result, didChange) = NoteEraserHelper.eraseStrokesAt(
                    strokes, x, y, radius, mode, pageIndex
                )
                if (didChange) {
                    strokes = result
                }
            },
            onEraseGestureComplete = {
                val snapshot = eraserGestureStartSnapshot
                if (snapshot != null && snapshot != strokes) {
                    undoStack.add(snapshot)
                    redoStack.clear()
                    undoCount = undoStack.size
                    redoCount = 0
                    saveNotes(strokes)
                }
                eraserGestureStartSnapshot = null
            },
            onClear = {
                if (strokes.isNotEmpty()) {
                    undoStack.add(strokes.toList())
                    redoStack.clear()
                    strokes = emptyList()
                    undoCount = undoStack.size
                    redoCount = 0
                    saveNotes(emptyList())
                }
            },
            onUndo = {
                if (undoStack.isNotEmpty()) {
                    redoStack.add(strokes.toList())
                    val restored = undoStack.removeAt(undoStack.lastIndex)
                    strokes = restored
                    undoCount = undoStack.size
                    redoCount = redoStack.size
                    saveNotes(restored)
                }
            },
            onRedo = {
                if (redoStack.isNotEmpty()) {
                    undoStack.add(strokes.toList())
                    val restored = redoStack.removeAt(redoStack.lastIndex)
                    strokes = restored
                    undoCount = undoStack.size
                    redoCount = redoStack.size
                    saveNotes(restored)
                }
            },
            onExport = {
                onReturnToApp()
            },
            onClose = {
                FloatingNoteData.strokes = strokes
                FloatingNoteData.textBoxes = textBoxes
                FloatingNoteData.canvasTemplate = canvasTemplate
                saveNotes(strokes)
                saveTextBoxes(textBoxes)
                onClose()
            },
            onCycleTemplate = {
                val values = CanvasTemplate.entries.toTypedArray()
                val nextIdx = (values.indexOf(canvasTemplate) + 1) % values.size
                canvasTemplate = values[nextIdx]
                FloatingNoteData.canvasTemplate = canvasTemplate
            },
            onToggleShapeMode = {
                isShapeModeEnabled = !isShapeModeEnabled
            },
            onAddTextBox = { x, y ->
                val newBox = NoteTextBox(x = x, y = y)
                val updated = textBoxes + newBox
                textBoxes = updated
                saveTextBoxes(updated)
            },
            onAddTextBoxObj = { box ->
                val updated = textBoxes + box
                textBoxes = updated
                saveTextBoxes(updated)
            },
            onUpdateTextBox = { updated ->
                val newList = textBoxes.map { if (it.id == updated.id) updated else it }
                textBoxes = newList
                saveTextBoxes(newList)
            },
            onDeleteTextBox = { id ->
                val updated = textBoxes.filter { it.id != id }
                textBoxes = updated
                saveTextBoxes(updated)
            },
            onToggleTemplateInsert = {
                showTemplateInsert = !showTemplateInsert
            },
            onInsertTemplate = { templateStrokes ->
                undoStack.add(strokes.toList())
                redoStack.clear()
                val updated = strokes + templateStrokes
                strokes = updated
                undoCount = undoStack.size
                redoCount = 0
                saveNotes(updated)
            },
            onMoveStrokes = { indices, dx, dy ->
                undoStack.add(strokes.toList())
                redoStack.clear()
                val updated = strokes.mapIndexed { idx, stroke ->
                    if (idx in indices) {
                        stroke.copy(points = stroke.points.map {
                            it.copy(x = it.x + dx, y = it.y + dy)
                        })
                    } else stroke
                }
                strokes = updated
                undoCount = undoStack.size
                redoCount = 0
                saveNotes(updated)
            },
            onScaleStrokes = { indices, scaleFactor, originX, originY ->
                undoStack.add(strokes.toList())
                redoStack.clear()
                val updated = strokes.mapIndexed { idx, stroke ->
                    if (idx in indices) {
                        stroke.copy(points = stroke.points.map {
                            it.copy(
                                x = originX + (it.x - originX) * scaleFactor,
                                y = originY + (it.y - originY) * scaleFactor
                            )
                        })
                    } else stroke
                }
                strokes = updated
                undoCount = undoStack.size
                redoCount = 0
                saveNotes(updated)
            },
            onDeleteStrokes = { indices ->
                undoStack.add(strokes.toList())
                redoStack.clear()
                val updated = strokes.filterIndexed { idx, _ -> idx !in indices }
                strokes = updated
                undoCount = undoStack.size
                redoCount = 0
                saveNotes(updated)
            },
            onStampsClick = {
                onReturnToApp()
            },
            getImagePath = { hash ->
                repository.getNotebookImagePath(hash)
            },
            onToggleGlobalNotebook = null,
            onMarkRecallSelection = null
        )
    }
}

/**
 * Custom LifecycleOwner + SavedStateRegistryOwner for hosting Compose in a Service.
 */
private class ServiceLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}
