package com.bookflow.reader.util

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PdfBitmapPool(
    private val maxCacheSizeMB: Int = 180
) {
    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private val renderMutex = Mutex()
    
    private val cache = object : LruCache<Int, ImageBitmap>(
        maxCacheSizeMB * 1024 * 1024
    ) {
        override fun sizeOf(key: Int, value: ImageBitmap): Int {
            return value.width * value.height * 4 // ARGB_8888 = 4 bytes per pixel
        }
    }

    val pageCount: Int
        get() = pdfRenderer?.pageCount ?: 0

    suspend fun openPdf(fileDescriptor: ParcelFileDescriptor) = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            close()
            this@PdfBitmapPool.fileDescriptor = fileDescriptor
            pdfRenderer = PdfRenderer(fileDescriptor)
        }
    }

    fun getPageAspectRatio(pageIndex: Int): Float? {
        val renderer = pdfRenderer ?: return null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
        return try {
            val page = renderer.openPage(pageIndex)
            val aspect = page.width.toFloat() / page.height.toFloat()
            page.close()
            aspect
        } catch (e: Exception) {
            null
        }
    }

    suspend fun renderPage(
        pageIndex: Int,
        targetWidth: Int = 2048,
        targetHeight: Int = 2896
    ): ImageBitmap? {
        if (pageIndex < 0 || pageIndex >= pageCount) return null
        
        // Check cache first
        cache.get(pageIndex)?.let { return it }
        
        return withContext(Dispatchers.IO) {
            renderMutex.withLock {
                // Double-check cache after acquiring lock
                cache.get(pageIndex)?.let { return@withContext it }
                
                val renderer = pdfRenderer ?: return@withContext null
                if (pageIndex >= renderer.pageCount) return@withContext null
                
                try {
                    val page = renderer.openPage(pageIndex)
                    
                    // Calculate dimensions maintaining aspect ratio
                    val pageAspect = page.width.toFloat() / page.height.toFloat()
                    val targetAspect = targetWidth.toFloat() / targetHeight.toFloat()
                    
                    val (bitmapWidth, bitmapHeight) = if (pageAspect > targetAspect) {
                        targetWidth to (targetWidth / pageAspect).toInt()
                    } else {
                        (targetHeight * pageAspect).toInt() to targetHeight
                    }
                    
                    val bitmap = Bitmap.createBitmap(
                        bitmapWidth.coerceAtLeast(1),
                        bitmapHeight.coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    
                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )
                    page.close()

                    val ambient = extractAmbientColor(bitmap)
                    ambientColorCache.put(pageIndex, ambient)

                    val imageBitmap = bitmap.asImageBitmap()
                    cache.put(pageIndex, imageBitmap)
                    imageBitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }

    private val ambientColorCache = object : LruCache<Int, androidx.compose.ui.graphics.Color>(60) {}

    fun getAmbientColor(pageIndex: Int): androidx.compose.ui.graphics.Color? {
        return ambientColorCache.get(pageIndex)
    }

    private fun extractAmbientColor(bitmap: Bitmap): androidx.compose.ui.graphics.Color {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return androidx.compose.ui.graphics.Color(0xFF0F2B48)

        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0

        val samplePoints = listOf(
            Pair(w / 8, h / 8), Pair(7 * w / 8, h / 8),
            Pair(w / 8, 7 * h / 8), Pair(7 * w / 8, 7 * h / 8),
            Pair(w / 2, h / 8), Pair(w / 2, 7 * h / 8),
            Pair(w / 8, h / 2), Pair(7 * w / 8, h / 2),
            Pair(w / 4, h / 4), Pair(3 * w / 4, h / 4),
            Pair(w / 4, 3 * h / 4), Pair(3 * w / 4, 3 * h / 4)
        )

        for ((sx, sy) in samplePoints) {
            if (sx in 0 until w && sy in 0 until h) {
                val pixel = bitmap.getPixel(sx, sy)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
                if (luminance in 15.0..242.0) {
                    rSum += r
                    gSum += g
                    bSum += b
                    count++
                }
            }
        }

        return if (count > 0) {
            androidx.compose.ui.graphics.Color(
                (rSum / count).toInt(),
                (gSum / count).toInt(),
                (bSum / count).toInt()
            )
        } else {
            androidx.compose.ui.graphics.Color(0xFF0E3A5D)
        }
    }

    fun getCachedPage(pageIndex: Int): ImageBitmap? {
        return cache.get(pageIndex)
    }

    suspend fun renderPageRawBitmap(
        pageIndex: Int,
        targetWidth: Int = 1200,
        targetHeight: Int = 1600
    ): Bitmap? = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            val renderer = pdfRenderer ?: return@withContext null
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
            try {
                val page = renderer.openPage(pageIndex)
                val pageAspect = page.width.toFloat() / page.height.toFloat()
                val targetAspect = targetWidth.toFloat() / targetHeight.toFloat()
                val (bitmapWidth, bitmapHeight) = if (pageAspect > targetAspect) {
                    targetWidth to (targetWidth / pageAspect).toInt()
                } else {
                    (targetHeight * pageAspect).toInt() to targetHeight
                }
                val bitmap = Bitmap.createBitmap(
                    bitmapWidth.coerceAtLeast(1),
                    bitmapHeight.coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun preloadPages(centerPage: Int, range: Int = 2) {
        val start = (centerPage - range).coerceAtLeast(0)
        val end = (centerPage + range).coerceAtMost(pageCount - 1)
        
        for (i in start..end) {
            if (cache.get(i) == null) {
                renderPage(i)
            }
        }
    }

    fun clearCache() {
        cache.evictAll()
    }

    fun close() {
        clearCache()
        try {
            pdfRenderer?.close()
            fileDescriptor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        pdfRenderer = null
        fileDescriptor = null
    }
}
