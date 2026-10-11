package com.example.reader

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfReaderEngine(private val filePath: String) {
    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    var pageCount: Int = 0
        private set

    suspend fun open(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext false
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pfd?.let {
                renderer = PdfRenderer(it)
                pageCount = renderer?.pageCount ?: 0
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    }

    suspend fun renderPage(pageIndex: Int, scale: Float = 1.5f): Bitmap? = withContext(Dispatchers.IO) {
        if (renderer == null || pageIndex < 0 || pageIndex >= pageCount) return@withContext null
        try {
            val page = renderer!!.openPage(pageIndex)
            val width = (page.width * scale).toInt().coerceAtLeast(100)
            val height = (page.height * scale).toInt().coerceAtLeast(100)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            return@withContext bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun close() {
        try {
            renderer?.close()
            pfd?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            renderer = null
            pfd = null
        }
    }
}
