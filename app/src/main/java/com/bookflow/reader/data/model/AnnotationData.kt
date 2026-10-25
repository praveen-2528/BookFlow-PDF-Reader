package com.bookflow.reader.data.model

data class AnnotationPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

data class AnnotationStroke(
    val points: List<AnnotationPoint>,
    val color: Long,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false,
    val penType: Int = 0, // 0=Ballpoint, 1=Fountain, 2=Image, 3=RectHighlight, 4=Shape
    val shapeType: Int = -1, // -1=none, 0=line, 1=circle, 2=rectangle, 3=triangle, 4=arrow
    val timestamp: Long = System.currentTimeMillis(),
    val pageIndex: Int = 0
)

data class PageAnnotations(
    val pageIndex: Int,
    val strokes: MutableList<AnnotationStroke> = mutableListOf()
)

data class NoteTextBox(
    val id: String = System.currentTimeMillis().toString(),
    val pageIndex: Int = 0,
    val x: Float,
    val y: Float,
    val width: Float = 300f,
    val height: Float = 100f,
    val text: String = "",
    val fontSize: Float = 18f,
    val color: Long = 0xFF2D2D2D,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val fontFamily: String = "DEFAULT", // "DEFAULT", "SERIF", "MONOSPACE", "CURSIVE", "SANS_SERIF"
    val textAlign: String = "LEFT", // "LEFT", "CENTER", "RIGHT"
    val rotation: Float = 0f
)

data class StickyNote(
    val id: String = System.currentTimeMillis().toString(),
    val pageIndex: Int,
    val x: Float,
    val y: Float,
    val text: String = "",
    val color: Long = 0xFFFFF176, // Yellow
    val isExpanded: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class NoteBlock(
    val id: String,
    val isText: Boolean,
    val textContent: String = "",
    val strokes: List<AnnotationStroke> = emptyList()
)

enum class CanvasTemplate {
    DOT_GRID, RULED_LINES, GRAPH_PAPER, BLANK, CORNELL
}

data class RevisionEntry(
    val pageIndex: Int,
    val annotationType: String, // "highlight", "pen", "sticky", "rect_highlight"
    val stroke: AnnotationStroke? = null,
    val stickyNote: StickyNote? = null,
    val timestamp: Long = 0L
)

data class AudioNote(
    val id: String = System.currentTimeMillis().toString(),
    val pageIndex: Int,
    val x: Float,
    val y: Float,
    val filePath: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class QuickRecallItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bookUri: String,
    val bookTitle: String,
    val pageIndex: Int,
    val tagKey: String = "recall",
    val emoji: String = "🧠",
    val label: String = "Must Recall",
    val description: String = "",
    val imagePath: String? = null,
    val cropRect: List<Float>? = null, // [normLeft, normTop, normRight, normBottom]
    val timestamp: Long = System.currentTimeMillis()
)

data class UniversalNoteMeta(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "Untitled Note",
    val updatedAt: Long = System.currentTimeMillis(),
    val strokeCount: Int = 0,
    val template: String = "DOT_GRID"
)

data class BookNoteMeta(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bookUri: String,
    val bookTitle: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val strokeCount: Int = 0,
    val pageAnnotationCount: Int = 0,
    val template: String = "DOT_GRID"
)

