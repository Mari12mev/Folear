package com.example.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.example.data.model.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object BookFileProcessor {

    suspend fun importFileFromUri(context: Context, uri: Uri): BookEntity? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var fileName = "arquivo_${System.currentTimeMillis()}"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val format = detectFormat(fileName, contentResolver.getType(uri))
            val booksDir = File(context.filesDir, "books").apply { mkdirs() }
            val coversDir = File(context.filesDir, "covers").apply { mkdirs() }

            val cleanName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(booksDir, "${System.currentTimeMillis()}_$cleanName")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            fileSize = targetFile.length()
            val title = fileName.substringBeforeLast(".")
            var totalPages = 1
            var coverPath: String? = null

            when (format) {
                "PDF" -> {
                    try {
                        ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                            PdfRenderer(pfd).use { renderer ->
                                totalPages = renderer.pageCount.coerceAtLeast(1)
                                if (totalPages > 0) {
                                    val page = renderer.openPage(0)
                                    val width = (page.width * 1.5f).toInt().coerceIn(300, 1200)
                                    val height = (page.height * 1.5f).toInt().coerceIn(400, 1600)
                                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                    bitmap.eraseColor(Color.WHITE)
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    page.close()

                                    val coverFile = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(coverFile).use { out ->
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                    }
                                    bitmap.recycle()
                                    coverPath = coverFile.absolutePath
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                "CBZ" -> {
                    try {
                        val images = getZipImageEntries(targetFile)
                        totalPages = images.size.coerceAtLeast(1)
                        if (images.isNotEmpty()) {
                            ZipFile(targetFile).use { zip ->
                                val firstEntry = images.first()
                                zip.getInputStream(firstEntry)?.use { imgStream ->
                                    val bitmap = android.graphics.BitmapFactory.decodeStream(imgStream)
                                    if (bitmap != null) {
                                        val coverFile = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
                                        FileOutputStream(coverFile).use { out ->
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                        }
                                        bitmap.recycle()
                                        coverPath = coverFile.absolutePath
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                "EPUB" -> {
                    coverPath = extractEpubCoverOrGenerate(context, targetFile, title, coversDir)
                    totalPages = estimateEpubPages(targetFile)
                }
                "CBR" -> {
                    // Comic CBR format - generate a stylized cover placeholder
                    coverPath = generateComicCoverBitmap(title, coversDir, "CBR HQ")
                    totalPages = 24
                }
            }

            if (coverPath == null) {
                coverPath = generateBookCoverBitmap(title, "Autor", coversDir, format)
            }

            BookEntity(
                title = title,
                author = "Importado",
                filePath = targetFile.absolutePath,
                originalFileName = fileName,
                format = format,
                totalPages = totalPages,
                currentPage = 1,
                lastReadTimestamp = System.currentTimeMillis(),
                coverImagePath = coverPath,
                originalCoverPath = coverPath,
                fileSizeBytes = fileSize,
                readingStatus = "NOT_STARTED"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun detectFormat(fileName: String, mimeType: String?): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".pdf") || mimeType == "application/pdf" -> "PDF"
            lower.endsWith(".epub") || mimeType == "application/epub+zip" -> "EPUB"
            lower.endsWith(".cbz") || lower.endsWith(".zip") -> "CBZ"
            lower.endsWith(".cbr") || lower.endsWith(".rar") -> "CBR"
            else -> "PDF"
        }
    }

    private fun getZipImageEntries(file: File): List<ZipEntry> {
        val results = mutableListOf<ZipEntry>()
        ZipFile(file).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name.lowercase()
                if (!entry.isDirectory && (name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                            name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif"))) {
                    results.add(entry)
                }
            }
        }
        return results.sortedWith(NaturalOrderComparator())
    }

    private fun estimateEpubPages(file: File): Int {
        var count = 0
        try {
            ZipFile(file).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name.lowercase()
                    if (name.endsWith(".html") || name.endsWith(".xhtml") || name.endsWith(".htm")) {
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            count = 1
        }
        return (count * 12).coerceAtLeast(15) // Approximate readable pages per chapter
    }

    private fun extractEpubCoverOrGenerate(context: Context, epubFile: File, title: String, coversDir: File): String {
        try {
            ZipFile(epubFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name.lowercase()
                    if (name.contains("cover") && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))) {
                        zip.getInputStream(entry)?.use { stream ->
                            val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                            if (bitmap != null) {
                                val coverFile = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
                                FileOutputStream(coverFile).use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                }
                                bitmap.recycle()
                                return coverFile.absolutePath
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return generateBookCoverBitmap(title, "EPUB", coversDir, "EPUB")
    }

    fun generateBookCoverBitmap(title: String, author: String, coversDir: File, badge: String): String {
        val width = 600
        val height = 900
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Palette
        val bgColors = listOf(0xFF261E1A.toInt(), 0xFF352B25.toInt(), 0xFF1E272C.toInt(), 0xFF2C2234.toInt())
        val selectedBg = bgColors[title.hashCode().mod(bgColors.size)]
        canvas.drawColor(selectedBg)

        // Accent border & frame
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE07A5F.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        canvas.drawRoundRect(RectF(30f, 30f, width - 30f, height - 30f), 24f, 24f, framePaint)

        // Top badge
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE07A5F.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(60f, 60f, 180f, 110f), 12f, 12f, badgeBgPaint)
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(badge, 120f, 96f, badgeTextPaint)

        // Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF7EFE9.toInt()
            textSize = 46f
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
        }
        val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD49B4B.toInt()
            textSize = 30f
            textAlign = Paint.Align.LEFT
        }

        // Draw title wrapped
        val words = title.split(" ")
        var y = 340f
        var currentLine = ""
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (titlePaint.measureText(testLine) < 480f) {
                currentLine = testLine
            } else {
                canvas.drawText(currentLine, 60f, y, titlePaint)
                y += 58f
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, 60f, y, titlePaint)
        }

        canvas.drawText("por $author", 60f, y + 60f, authorPaint)

        // Decorative book spine lines
        val spinePaint = Paint().apply {
            color = 0x33FFFFFF
            strokeWidth = 3f
        }
        canvas.drawLine(50f, 30f, 50f, height - 30f, spinePaint)
        canvas.drawLine(56f, 30f, 56f, height - 30f, spinePaint)

        val coverFile = File(coversDir, "cover_${System.currentTimeMillis()}_${title.hashCode()}.jpg")
        FileOutputStream(coverFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        bitmap.recycle()
        return coverFile.absolutePath
    }

    private fun generateComicCoverBitmap(title: String, coversDir: File, badge: String): String {
        val width = 600
        val height = 900
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(0xFF1F1C28.toInt())

        // Comic header strip
        val stripPaint = Paint().apply {
            color = 0xFFE07A5F.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 140f, stripPaint)

        val headerText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 50f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("COMICS & MANGÁ", width / 2f, 90f, headerText)

        // Comic panels outline
        val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF2CC8F.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRoundRect(RectF(40f, 170f, width - 40f, 520f), 16f, 16f, panelPaint)
        canvas.drawRoundRect(RectF(40f, 540f, width - 40f, 840f), 16f, 16f, panelPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title, width / 2f, 360f, titlePaint)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF81B29A.toInt()
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("FORMATO $badge • ED. ESPECIAL", width / 2f, 700f, badgePaint)

        val coverFile = File(coversDir, "comic_cover_${System.currentTimeMillis()}.jpg")
        FileOutputStream(coverFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        bitmap.recycle()
        return coverFile.absolutePath
    }

    // Seeds sample books into the library if empty
    suspend fun seedSampleLibrary(context: Context): List<BookEntity> = withContext(Dispatchers.IO) {
        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }

        val samples = mutableListOf<BookEntity>()

        // 1. Comic / Mangá CBZ sample
        try {
            val comicFile = File(booksDir, "amostra_guardiao_dos_sonhos.cbz")
            if (!comicFile.exists()) {
                createSampleComicCbz(comicFile)
            }
            val comicCover = File(coversDir, "cover_comic_sample.jpg")
            if (!comicCover.exists()) {
                val images = getZipImageEntries(comicFile)
                if (images.isNotEmpty()) {
                    ZipFile(comicFile).use { zip ->
                        zip.getInputStream(images.first())?.use { s ->
                            val b = android.graphics.BitmapFactory.decodeStream(s)
                            if (b != null) {
                                FileOutputStream(comicCover).use { out ->
                                    b.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                }
                                b.recycle()
                            }
                        }
                    }
                }
            }

            samples.add(
                BookEntity(
                    title = "O Guardião dos Sonhos #01",
                    author = "Studio Aurora & Mangá",
                    filePath = comicFile.absolutePath,
                    originalFileName = "amostra_guardiao_dos_sonhos.cbz",
                    format = "CBZ",
                    totalPages = 10,
                    currentPage = 3,
                    lastReadTimestamp = System.currentTimeMillis() - 3600000,
                    coverImagePath = comicCover.absolutePath,
                    originalCoverPath = comicCover.absolutePath,
                    fileSizeBytes = comicFile.length(),
                    readingStatus = "READING"
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. EPUB Book sample
        try {
            val epubFile = File(booksDir, "amostra_pequeno_viajante.epub")
            if (!epubFile.exists()) {
                createSampleEpub(epubFile)
            }
            val epubCover = generateBookCoverBitmap("O Pequeno Viajante das Estrelas", "Antoine de Laurent", coversDir, "EPUB")
            samples.add(
                BookEntity(
                    title = "O Pequeno Viajante das Estrelas",
                    author = "Antoine de Laurent",
                    filePath = epubFile.absolutePath,
                    originalFileName = "amostra_pequeno_viajante.epub",
                    format = "EPUB",
                    totalPages = 48,
                    currentPage = 14,
                    lastReadTimestamp = System.currentTimeMillis() - 7200000,
                    coverImagePath = epubCover,
                    originalCoverPath = epubCover,
                    fileSizeBytes = epubFile.length(),
                    readingStatus = "READING"
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. PDF Book sample
        try {
            val pdfFile = File(booksDir, "amostra_guia_leitura_folhear.pdf")
            if (!pdfFile.exists()) {
                createSamplePdf(pdfFile)
            }
            val pdfCover = generateBookCoverBitmap("Guia Completo do Leitor Moderno", "Equipe Folhear", coversDir, "PDF")
            samples.add(
                BookEntity(
                    title = "Guia Completo do Leitor Moderno",
                    author = "Equipe Folhear",
                    filePath = pdfFile.absolutePath,
                    originalFileName = "amostra_guia_leitura_folhear.pdf",
                    format = "PDF",
                    totalPages = 12,
                    currentPage = 1,
                    lastReadTimestamp = 0L,
                    coverImagePath = pdfCover,
                    originalCoverPath = pdfCover,
                    fileSizeBytes = pdfFile.length(),
                    readingStatus = "NOT_STARTED"
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        samples
    }

    private fun createSamplePdf(file: File) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 points

        val pageTitles = listOf(
            "Bem-vindo ao Folhear" to "Seu novo espaço aconchegante para devorar histórias, mangás e quadrinhos com visual moderno e acolhedor.",
            "Recursos do Leitor" to "Leitura em tela cheia sem distrações, modo de página única ou página dupla para HQs, e salvar automaticamente o progresso.",
            "Pastas e Organização" to "Crie coleções personalizadas como Fantasia, Mangás e Lendo Agora. Cada item pode estar em várias pastas simultaneamente.",
            "Metas de Leitura" to "Acompanhe seu ritmo diário com metas como 'terminar um livro em 7 dias' ou 'ler 20 páginas por dia'."
        )

        for (i in 0 until 12) {
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Page background
            canvas.drawColor(0xFFFAF6F0.toInt())

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF261D19.toInt()
                textSize = 28f
                isFakeBoldText = true
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF4A3E38.toInt()
                textSize = 16f
            }
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFE07A5F.toInt()
                textSize = 14f
                isFakeBoldText = true
            }
            val pageNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF8A7A72.toInt()
                textSize = 12f
                textAlign = Paint.Align.CENTER
            }

            canvas.drawText("FOLHEAR • GUIA DO LEITOR", 50f, 60f, headerPaint)
            val info = pageTitles[i % pageTitles.size]
            canvas.drawText(info.first, 50f, 130f, titlePaint)

            canvas.drawText(info.second, 50f, 180f, bodyPaint)

            // Paragraphs
            val lorem = "A leitura enriquece a mente e expande a imaginação. Com o Folhear, você tem controle total sobre suas obras favoritas com modo noturno relaxante para os olhos, modo de tela cheia sem distrações e salvamento contínuo da sua jornada."
            val words = lorem.split(" ")
            var y = 230f
            var line = ""
            for (w in words) {
                val test = if (line.isEmpty()) w else "$line $w"
                if (bodyPaint.measureText(test) < 490f) {
                    line = test
                } else {
                    canvas.drawText(line, 50f, y, bodyPaint)
                    y += 24f
                    line = w
                }
            }
            if (line.isNotEmpty()) canvas.drawText(line, 50f, y, bodyPaint)

            // Page bottom number
            canvas.drawText("Página ${i + 1} de 12", 297f, 800f, pageNumPaint)

            document.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun createSampleComicCbz(file: File) {
        val zipOut = ZipOutputStream(FileOutputStream(file))

        val comicPages = listOf(
            ComicPageSpec("Capa: O Guardião dos Sonhos", 0xFF14121A.toInt(), true),
            ComicPageSpec("Capítulo 1: O Despertar da Lua", 0xFF1C1929.toInt(), false),
            ComicPageSpec("Página 2: Ruas de Neo-Kyoto", 0xFF231F33.toInt(), false),
            ComicPageSpec("Página 3: O Primeiro Confronto", 0xFF2B1F2A.toInt(), false),
            ComicPageSpec("Página 4: Chamas Celestiais (Spread)", 0xFF3D2124.toInt(), false),
            ComicPageSpec("Página 5: O Portal do Crepúsculo", 0xFF202B33.toInt(), false),
            ComicPageSpec("Página 6: Segredos Antigos", 0xFF1B2E28.toInt(), false),
            ComicPageSpec("Página 7: O Golpe Final", 0xFF352C1E.toInt(), false),
            ComicPageSpec("Página 8: Vitória e Mistério", 0xFF241E33.toInt(), false),
            ComicPageSpec("Contracapa: Continua no Vol. 2", 0xFF14121A.toInt(), true)
        )

        for ((index, spec) in comicPages.withIndex()) {
            val fileName = "page_${String.format("%03d", index + 1)}.png"
            val bitmap = renderComicPageBitmap(index + 1, spec)
            zipOut.putNextEntry(ZipEntry(fileName))
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, zipOut)
            zipOut.closeEntry()
            bitmap.recycle()
        }

        zipOut.close()
    }

    private data class ComicPageSpec(val title: String, val bgColor: Int, val isCover: Boolean)

    private fun renderComicPageBitmap(pageNum: Int, spec: ComicPageSpec): Bitmap {
        val width = 800
        val height = 1200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(spec.bgColor)

        if (spec.isCover) {
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFF2CC8F.toInt()
                textSize = 52f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFE07A5F.toInt()
                textSize = 34f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("O GUARDIÃO DOS SONHOS", width / 2f, 380f, titlePaint)
            canvas.drawText("EDIÇÃO ESPECIAL #01", width / 2f, 440f, subPaint)

            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("HQ & MANGÁ NACIONAL", width / 2f, 900f, badgePaint)
        } else {
            // Draw comic panels
            val panelBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFF8F0.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
            val panelFill = Paint().apply {
                color = 0x22FFFFFF
                style = Paint.Style.FILL
            }

            // Panel 1 (Top Wide)
            val p1 = RectF(40f, 60f, width - 40f, 380f)
            canvas.drawRoundRect(p1, 12f, 12f, panelFill)
            canvas.drawRoundRect(p1, 12f, 12f, panelBorder)

            // Panel 2 & 3 (Middle Split)
            val p2 = RectF(40f, 410f, (width / 2f) - 15f, 750f)
            val p3 = RectF((width / 2f) + 15f, 410f, width - 40f, 750f)
            canvas.drawRoundRect(p2, 12f, 12f, panelFill)
            canvas.drawRoundRect(p2, 12f, 12f, panelBorder)
            canvas.drawRoundRect(p3, 12f, 12f, panelFill)
            canvas.drawRoundRect(p3, 12f, 12f, panelBorder)

            // Panel 4 (Bottom Action)
            val p4 = RectF(40f, 780f, width - 40f, 1100f)
            canvas.drawRoundRect(p4, 12f, 12f, panelFill)
            canvas.drawRoundRect(p4, 12f, 12f, panelBorder)

            // Dialogue bubble
            val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(80f, 90f, 380f, 170f), 20f, 20f, bubblePaint)
            val speechText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 20f
                isFakeBoldText = true
            }
            canvas.drawText("— O portal está abrindo!", 100f, 140f, speechText)

            // Sound effect text
            val sfxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFF2CC8F.toInt()
                textSize = 44f
                isFakeBoldText = true
            }
            canvas.drawText("KRAAAASH!", 260f, 920f, sfxPaint)

            // Title & page footer
            val pageNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFA0968F.toInt()
                textSize = 20f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Página $pageNum", width / 2f, 1150f, pageNumPaint)
        }

        return bitmap
    }

    private fun createSampleEpub(file: File) {
        val zipOut = ZipOutputStream(FileOutputStream(file))

        // Mimetype
        zipOut.putNextEntry(ZipEntry("mimetype"))
        zipOut.write("application/epub+zip".toByteArray())
        zipOut.closeEntry()

        // Container XML
        zipOut.putNextEntry(ZipEntry("META-INF/container.xml"))
        val containerXml = """
            <?xml version="1.0"?>
            <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                <rootfiles>
                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                </rootfiles>
            </container>
        """.trimIndent()
        zipOut.write(containerXml.toByteArray())
        zipOut.closeEntry()

        // Chapters content
        val chapters = listOf(
            "chapter1.xhtml" to ("Prólogo: O Céu Noturno" to "Nas noites límpidas de outono, as estrelas pareciam sussurrar histórias para quem soubesse ouvir. O pequeno viajante ajustava seu telescópio de latão polido enquanto o vento fresco soprava entre os pinheiros."),
            "chapter2.xhtml" to ("Capítulo 1: O Encontro nas Dunas" to "A vastidão dourada do deserto se estendia até a linha do horizonte. Foi lá que ele avistou a pequena raposa de olhos atentos, que guardava o segredo mais precioso das dunas: o tempo que você dedica ao que ama torna-o único no universo."),
            "chapter3.xhtml" to ("Capítulo 2: A Rosa Carmim" to "Ela era orgulhosa e vestia pétalas aveludadas de um vermelho profundo. Exigia quatro biombos para se proteger das correntes de ar e reclamava do orvalho da manhã, mas seu aroma doce perfumava todo o asteroide."),
            "chapter4.xhtml" to ("Capítulo 3: Lições do Vento" to "O viajante compreendeu então que a verdadeira jornada não é cruzar galáxias inteiras, mas aprender a olhar com o coração. O essencial, como sempre diziam os sábios, é invisível aos olhos.")
        )

        for ((fileName, pair) in chapters) {
            zipOut.putNextEntry(ZipEntry("OEBPS/$fileName"))
            val (cTitle, cText) = pair
            val xhtml = """
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>$cTitle</title></head>
                <body>
                    <h2>$cTitle</h2>
                    <p>$cText</p>
                    <p>Cada página folheada é um passo adiante num mundo infinito de possibilidades. Cultivar o hábito diário de leitura transforma mentes e constrói futuros brilhantes.</p>
                </body>
                </html>
            """.trimIndent()
            zipOut.write(xhtml.toByteArray())
            zipOut.closeEntry()
        }

        zipOut.close()
    }
}

class NaturalOrderComparator : Comparator<ZipEntry> {
    override fun compare(e1: ZipEntry, e2: ZipEntry): Int {
        return extractNumber(e1.name).compareTo(extractNumber(e2.name)).let {
            if (it != 0) it else e1.name.compareTo(e2.name, ignoreCase = true)
        }
    }

    private fun extractNumber(s: String): Long {
        val numStr = s.filter { it.isDigit() }
        return numStr.toLongOrNull() ?: Long.MAX_VALUE
    }
}
