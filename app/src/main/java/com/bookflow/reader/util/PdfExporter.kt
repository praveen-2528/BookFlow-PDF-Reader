package com.bookflow.reader.util

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.bookflow.reader.data.model.AnnotationStroke
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object PdfExporter {
    fun exportNotesToPdf(
        context: Context,
        strokes: List<AnnotationStroke>,
        imagePaths: Map<Long, String>,
        fileName: String = "BookFlow_Notes"
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        for (stroke in strokes) {
            paint.color = stroke.color.toInt()
            paint.strokeWidth = stroke.strokeWidth
            if (stroke.isHighlighter) {
                paint.alpha = 100 // Semi-transparent
            } else {
                paint.alpha = 255
            }

            if (stroke.points.isNotEmpty()) {
                val path = Path()
                path.moveTo(stroke.points.first().x, stroke.points.first().y)
                var currentX = stroke.points.first().x
                var currentY = stroke.points.first().y

                for (i in 1 until stroke.points.size) {
                    val point = stroke.points[i]
                    val midX = (currentX + point.x) / 2
                    val midY = (currentY + point.y) / 2
                    path.quadTo(currentX, currentY, midX, midY)
                    currentX = point.x
                    currentY = point.y
                }
                path.lineTo(currentX, currentY)
                canvas.drawPath(path, paint)
            }
        }

        imagePaths.forEach { (_, path) ->
            val bitmap = BitmapFactory.decodeFile(path)
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, 0f, 0f, null)
            }
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "$fileName.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF using"))
    }
}
