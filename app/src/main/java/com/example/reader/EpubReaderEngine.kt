package com.example.reader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.zip.ZipFile

data class EpubChapter(
    val index: Int,
    val title: String,
    val contentHtml: String,
    val plainText: String,
    val paragraphs: List<String> = emptyList()
)

data class EpubSearchResult(
    val chapterIndex: Int,
    val chapterTitle: String,
    val snippet: String,
    val matchPosition: Int
)

class EpubReaderEngine(private val filePath: String) {
    var chapters: List<EpubChapter> = emptyList()
        private set

    val chapterCount: Int
        get() = chapters.size

    suspend fun open(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext false

            val parsedChapters = mutableListOf<EpubChapter>()
            ZipFile(file).use { zip ->
                val entries = zip.entries()
                val htmlEntries = mutableListOf<String>()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name.lowercase()
                    if (!entry.isDirectory && (name.endsWith(".xhtml") || name.endsWith(".html") || name.endsWith(".htm")) && !name.contains("toc")) {
                        htmlEntries.add(entry.name)
                    }
                }

                htmlEntries.sort()

                for ((idx, entryName) in htmlEntries.withIndex()) {
                    val entry = zip.getEntry(entryName) ?: continue
                    val content = zip.getInputStream(entry).use { stream ->
                        BufferedReader(InputStreamReader(stream)).readText()
                    }

                    val title = extractTitleFromHtml(content) ?: "Capítulo ${idx + 1}"
                    val plain = stripHtmlTags(content)
                    val paragraphs = plain.split("\n\n", "\r\n\r\n").map { it.trim() }.filter { it.isNotBlank() }

                    parsedChapters.add(
                        EpubChapter(
                            index = idx,
                            title = title,
                            contentHtml = content,
                            plainText = plain,
                            paragraphs = if (paragraphs.isNotEmpty()) paragraphs else listOf(plain)
                        )
                    )
                }
            }

            chapters = parsedChapters
            return@withContext chapters.isNotEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Fast full-text in-book search across all chapters
    suspend fun searchInBook(query: String): List<EpubSearchResult> = withContext(Dispatchers.Default) {
        if (query.isBlank() || chapters.isEmpty()) return@withContext emptyList()
        val results = mutableListOf<EpubSearchResult>()
        val cleanQuery = query.trim().lowercase()

        for (chapter in chapters) {
            val text = chapter.plainText
            val textLower = text.lowercase()
            var startIndex = 0

            while (startIndex < textLower.length) {
                val matchIndex = textLower.indexOf(cleanQuery, startIndex)
                if (matchIndex == -1) break

                val snippetStart = (matchIndex - 45).coerceAtLeast(0)
                val snippetEnd = (matchIndex + cleanQuery.length + 55).coerceAtMost(text.length)
                val snippet = "..." + text.substring(snippetStart, snippetEnd).replace("\n", " ") + "..."

                results.add(
                    EpubSearchResult(
                        chapterIndex = chapter.index,
                        chapterTitle = chapter.title,
                        snippet = snippet,
                        matchPosition = matchIndex
                    )
                )

                if (results.size >= 50) return@withContext results // limit for responsiveness
                startIndex = matchIndex + cleanQuery.length
            }
        }
        results
    }

    private fun extractTitleFromHtml(html: String): String? {
        val titleMatch = "<title>(.*?)</title>".toRegex(RegexOption.IGNORE_CASE).find(html)
        if (titleMatch != null && titleMatch.groupValues[1].isNotBlank()) {
            return titleMatch.groupValues[1].trim()
        }
        val h1Match = "<h[1-2][^>]*>(.*?)</h[1-2]>".toRegex(RegexOption.IGNORE_CASE).find(html)
        if (h1Match != null && h1Match.groupValues[1].isNotBlank()) {
            return stripHtmlTags(h1Match.groupValues[1]).trim()
        }
        return null
    }

    private fun stripHtmlTags(html: String): String {
        return html
            .replace("<style[^>]*>[\\s\\S]*?</style>".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("<script[^>]*>[\\s\\S]*?</script>".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("(?i)<br\\s*/?>".toRegex(), "\n")
            .replace("(?i)</p>".toRegex(), "\n\n")
            .replace("<[^>]+>".toRegex(), " ")
            .replace("&nbsp;".toRegex(), " ")
            .replace("&amp;".toRegex(), "&")
            .replace("&lt;".toRegex(), "<")
            .replace("&gt;".toRegex(), ">")
            .replace("&quot;".toRegex(), "\"")
            .replace("&#39;".toRegex(), "'")
            .replace("[ \\t]+".toRegex(), " ")
            .trim()
    }

    fun close() {}
}
