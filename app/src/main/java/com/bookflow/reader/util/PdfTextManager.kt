package com.bookflow.reader.util

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class PdfWordBox(
    val id: Int,
    val text: String,
    val rect: Rect // Normalized 0f..1f (left, top, right, bottom)
)

data class PdfLineBox(
    val text: String,
    val rect: Rect,
    val words: List<PdfWordBox>
)

data class PageTextContent(
    val pageIndex: Int,
    val lines: List<PdfLineBox>,
    val words: List<PdfWordBox>,
    val fullText: String
)

object PdfTextManager {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val cache = LruCache<Int, PageTextContent>(30)

    fun getCached(pageIndex: Int): PageTextContent? = cache.get(pageIndex)

    suspend fun extractTextFromBitmap(pageIndex: Int, bitmap: Bitmap): PageTextContent = withContext(Dispatchers.Default) {
        cache.get(pageIndex)?.let { return@withContext it }

        val image = InputImage.fromBitmap(bitmap, 0)
        val bmpW = bitmap.width.toFloat()
        val bmpH = bitmap.height.toFloat()

        val visionText = suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    continuation.resume(result)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }

        if (visionText == null || visionText.textBlocks.isEmpty()) {
            val empty = PageTextContent(pageIndex, emptyList(), emptyList(), "")
            cache.put(pageIndex, empty)
            return@withContext empty
        }

        val linesList = mutableListOf<PdfLineBox>()
        val wordsList = mutableListOf<PdfWordBox>()
        var wordIdCounter = 0

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val lineBox = line.boundingBox
                val normLineRect = if (lineBox != null) {
                    Rect(
                        left = (lineBox.left / bmpW).coerceIn(0f, 1f),
                        top = (lineBox.top / bmpH).coerceIn(0f, 1f),
                        right = (lineBox.right / bmpW).coerceIn(0f, 1f),
                        bottom = (lineBox.bottom / bmpH).coerceIn(0f, 1f)
                    )
                } else {
                    Rect.Zero
                }

                val lineWords = mutableListOf<PdfWordBox>()
                for (element in line.elements) {
                    val elemBox = element.boundingBox
                    val normElemRect = if (elemBox != null) {
                        Rect(
                            left = (elemBox.left / bmpW).coerceIn(0f, 1f),
                            top = (elemBox.top / bmpH).coerceIn(0f, 1f),
                            right = (elemBox.right / bmpW).coerceIn(0f, 1f),
                            bottom = (elemBox.bottom / bmpH).coerceIn(0f, 1f)
                        )
                    } else {
                        normLineRect
                    }

                    val wb = PdfWordBox(
                        id = wordIdCounter++,
                        text = element.text,
                        rect = normElemRect
                    )
                    lineWords.add(wb)
                    wordsList.add(wb)
                }

                linesList.add(
                    PdfLineBox(
                        text = line.text,
                        rect = normLineRect,
                        words = lineWords
                    )
                )
            }
        }

        val result = PageTextContent(
            pageIndex = pageIndex,
            lines = linesList,
            words = wordsList,
            fullText = visionText.text
        )
        cache.put(pageIndex, result)
        result
    }

    fun findWordAt(pageText: PageTextContent, normPos: Offset): PdfWordBox? {
        // Expand hit-test bounds slightly for finger / stylus ease of tap
        val pad = 0.008f
        return pageText.words.firstOrNull { wb ->
            normPos.x >= wb.rect.left - pad && normPos.x <= wb.rect.right + pad &&
            normPos.y >= wb.rect.top - pad && normPos.y <= wb.rect.bottom + pad
        }
    }

    fun getWordsBetween(pageText: PageTextContent, startWord: PdfWordBox, endWord: PdfWordBox): List<PdfWordBox> {
        val startId = minOf(startWord.id, endWord.id)
        val endId = maxOf(startWord.id, endWord.id)
        return pageText.words.filter { it.id in startId..endId }
    }
}
