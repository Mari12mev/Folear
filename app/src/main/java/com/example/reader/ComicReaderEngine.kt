package com.example.reader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.math.max
import kotlin.math.min

class ComicReaderEngine(private val filePath: String) {
    private var imageEntries: List<String> = emptyList()
    var pageCount: Int = 0
        private set

    // Memory cache for smooth, instant page turning (up to 20 pages in RAM)
    private val memoryCache: LruCache<Int, Bitmap> = object : LruCache<Int, Bitmap>(20) {
        override fun sizeOf(key: Int, value: Bitmap): Int = 1
    }

    suspend fun open(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext false
            val entries = mutableListOf<ZipEntry>()
            ZipFile(file).use { zip ->
                val it = zip.entries()
                while (it.hasMoreElements()) {
                    val entry = it.nextElement()
                    val name = entry.name.lowercase()
                    if (!entry.isDirectory && (name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                                name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif"))) {
                        entries.add(entry)
                    }
                }
            }
            val sorted = entries.sortedWith(NaturalOrderComparator())
            imageEntries = sorted.map { it.name }
            pageCount = imageEntries.size
            return@withContext pageCount > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun loadPageBitmap(pageIndex: Int, autoCropBorders: Boolean = false): Bitmap? = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= imageEntries.size) return@withContext null

        // Check memory cache first
        val cached = memoryCache.get(pageIndex)
        if (cached != null && !cached.isRecycled) {
            return@withContext if (autoCropBorders) cropWhiteBorders(cached) else cached
        }

        try {
            val file = File(filePath)
            ZipFile(file).use { zip ->
                val entryName = imageEntries[pageIndex]
                val entry = zip.getEntry(entryName) ?: return@withContext null
                zip.getInputStream(entry).use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        memoryCache.put(pageIndex, bitmap)
                        return@withContext if (autoCropBorders) cropWhiteBorders(bitmap) else bitmap
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    // Pre-cache nearby pages in background to guarantee smooth, zero-stutter paging
    suspend fun preloadPages(currentPageIndex: Int, count: Int = 2) = withContext(Dispatchers.IO) {
        val targets = listOf(currentPageIndex - 1, currentPageIndex + 1, currentPageIndex + 2)
            .filter { it in 0 until pageCount && memoryCache.get(it) == null }

        for (idx in targets.take(count)) {
            loadPageBitmap(idx, autoCropBorders = false)
        }
    }

    // Detect and crop pure white/monochrome empty borders from comic/manga pages
    fun cropWhiteBorders(source: Bitmap): Bitmap {
        try {
            val width = source.width
            val height = source.height
            if (width < 50 || height < 50) return source

            var top = 0
            var bottom = height - 1
            var left = 0
            var right = width - 1

            // Threshold for considering a pixel "white/border"
            fun isBorderColor(pixel: Int): Boolean {
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                return (r > 240 && g > 240 && b > 240) || (r < 15 && g < 15 && b < 15) // white or pitch black border
            }

            // Sample step for speed
            val step = 4

            // Scan top
            topLoop@ while (top < height / 4) {
                for (x in 0 until width step step) {
                    if (!isBorderColor(source.getPixel(x, top))) break@topLoop
                }
                top += step
            }

            // Scan bottom
            bottomLoop@ while (bottom > height * 3 / 4) {
                for (x in 0 until width step step) {
                    if (!isBorderColor(source.getPixel(x, bottom))) break@bottomLoop
                }
                bottom -= step
            }

            // Scan left
            leftLoop@ while (left < width / 4) {
                for (y in top until bottom step step) {
                    if (!isBorderColor(source.getPixel(left, y))) break@leftLoop
                }
                left += step
            }

            // Scan right
            rightLoop@ while (right > width * 3 / 4) {
                for (y in top until bottom step step) {
                    if (!isBorderColor(source.getPixel(right, y))) break@rightLoop
                }
                right -= step
            }

            val cropWidth = right - left
            val cropHeight = bottom - top

            // Only crop if border found is significant (at least 15px trimmed on any side)
            if (cropWidth > 50 && cropHeight > 50 && (top > 10 || (height - 1 - bottom) > 10 || left > 10 || (width - 1 - right) > 10)) {
                return Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return source
    }

    fun close() {
        memoryCache.evictAll()
    }
}
