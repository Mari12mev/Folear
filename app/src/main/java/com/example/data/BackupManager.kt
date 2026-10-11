package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookEntity
import com.example.data.model.BookFolderCrossRef
import com.example.data.model.BookTagCrossRef
import com.example.data.model.BookmarkEntity
import com.example.data.model.FolderEntity
import com.example.data.model.ReadingGoalEntity
import com.example.data.model.ReadingSessionEntity
import com.example.data.model.TagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object BackupManager {

    suspend fun exportBackup(context: Context, uri: Uri, db: AppDatabase): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("app", "Folhear")
            root.put("version", 2)
            root.put("timestamp", System.currentTimeMillis())

            // Folders
            val folders = db.folderDao().getAllFoldersSync()
            val foldersArray = JSONArray()
            for (f in folders) {
                val obj = JSONObject()
                obj.put("id", f.id)
                obj.put("name", f.name)
                obj.put("iconName", f.iconName)
                obj.put("colorHex", f.colorHex)
                obj.put("isSystem", f.isSystem)
                foldersArray.put(obj)
            }
            root.put("folders", foldersArray)

            // Tags
            val tags = db.tagDao().getAllTagsSync()
            val tagsArray = JSONArray()
            for (t in tags) {
                val obj = JSONObject()
                obj.put("id", t.id)
                obj.put("name", t.name)
                obj.put("colorHex", t.colorHex)
                tagsArray.put(obj)
            }
            root.put("tags", tagsArray)

            // Books metadata
            val books = db.bookDao().getAllBooksSync()
            val booksArray = JSONArray()
            for (b in books) {
                val obj = JSONObject()
                obj.put("id", b.id)
                obj.put("title", b.title)
                obj.put("author", b.author)
                obj.put("originalFileName", b.originalFileName)
                obj.put("format", b.format)
                obj.put("totalPages", b.totalPages)
                obj.put("currentPage", b.currentPage)
                obj.put("readingStatus", b.readingStatus)
                obj.put("comicReadingMode", b.comicReadingMode)
                obj.put("isFavorite", b.isFavorite)
                obj.put("cropBorders", b.cropBorders)
                obj.put("lastReadTimestamp", b.lastReadTimestamp)
                booksArray.put(obj)
            }
            root.put("books", booksArray)

            // Bookmarks
            val bookmarks = db.bookmarkDao().getAllBookmarksSync()
            val bookmarksArray = JSONArray()
            for (bm in bookmarks) {
                val obj = JSONObject()
                obj.put("bookId", bm.bookId)
                obj.put("pageOrChapter", bm.pageOrChapter)
                obj.put("title", bm.title)
                obj.put("note", bm.note ?: "")
                obj.put("createdAt", bm.createdAt)
                bookmarksArray.put(obj)
            }
            root.put("bookmarks", bookmarksArray)

            // Annotations
            val annotations = db.annotationDao().getAllAnnotationsSync()
            val annotationsArray = JSONArray()
            for (an in annotations) {
                val obj = JSONObject()
                obj.put("bookId", an.bookId)
                obj.put("chapterIndex", an.chapterIndex)
                obj.put("chapterTitle", an.chapterTitle)
                obj.put("selectedText", an.selectedText)
                obj.put("note", an.note ?: "")
                obj.put("colorHex", an.colorHex)
                obj.put("createdAt", an.createdAt)
                annotationsArray.put(obj)
            }
            root.put("annotations", annotationsArray)

            // Reading Goals
            val goals = db.readingGoalDao().getAllGoalsSync()
            val goalsArray = JSONArray()
            for (g in goals) {
                val obj = JSONObject()
                obj.put("title", g.title)
                obj.put("goalType", g.goalType)
                obj.put("bookId", g.bookId ?: -1)
                obj.put("bookTitle", g.bookTitle ?: "")
                obj.put("targetPages", g.targetPages)
                obj.put("targetDays", g.targetDays)
                obj.put("startDate", g.startDate)
                obj.put("targetEndDate", g.targetEndDate)
                obj.put("pagesReadSoFar", g.pagesReadSoFar)
                obj.put("isCompleted", g.isCompleted)
                goalsArray.put(obj)
            }
            root.put("readingGoals", goalsArray)

            // Reading Sessions
            val sessions = db.readingSessionDao().getAllSessionsSync()
            val sessionsArray = JSONArray()
            for (s in sessions) {
                val obj = JSONObject()
                obj.put("bookId", s.bookId)
                obj.put("bookTitle", s.bookTitle)
                obj.put("dateEpochDay", s.dateEpochDay)
                obj.put("durationSeconds", s.durationSeconds)
                obj.put("pagesRead", s.pagesRead)
                obj.put("timestamp", s.timestamp)
                sessionsArray.put(obj)
            }
            root.put("readingSessions", sessionsArray)

            // Write JSON to destination URI
            context.contentResolver.openOutputStream(uri)?.use { out ->
                OutputStreamWriter(out).use { writer ->
                    writer.write(root.toString(2))
                }
            } ?: return@withContext false

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importBackup(context: Context, uri: Uri, db: AppDatabase): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            } ?: return@withContext false

            val root = JSONObject(jsonString)

            // Import Folders
            if (root.has("folders")) {
                val array = root.getJSONArray("folders")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    db.folderDao().insertFolder(
                        FolderEntity(
                            name = obj.getString("name"),
                            iconName = obj.optString("iconName", "folder"),
                            colorHex = obj.optString("colorHex", "#E07A5F"),
                            isSystem = obj.optBoolean("isSystem", false)
                        )
                    )
                }
            }

            // Import Tags
            if (root.has("tags")) {
                val array = root.getJSONArray("tags")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    db.tagDao().insertTag(
                        TagEntity(
                            name = obj.getString("name"),
                            colorHex = obj.optString("colorHex", "#E07A5F")
                        )
                    )
                }
            }

            // Import Bookmarks
            if (root.has("bookmarks")) {
                val array = root.getJSONArray("bookmarks")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    db.bookmarkDao().insertBookmark(
                        BookmarkEntity(
                            bookId = obj.getLong("bookId"),
                            pageOrChapter = obj.getInt("pageOrChapter"),
                            title = obj.getString("title"),
                            note = if (obj.has("note")) obj.getString("note") else null,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Import Annotations
            if (root.has("annotations")) {
                val array = root.getJSONArray("annotations")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    db.annotationDao().insertAnnotation(
                        AnnotationEntity(
                            bookId = obj.getLong("bookId"),
                            chapterIndex = obj.getInt("chapterIndex"),
                            chapterTitle = obj.optString("chapterTitle", "Capítulo"),
                            selectedText = obj.getString("selectedText"),
                            note = if (obj.has("note")) obj.getString("note") else null,
                            colorHex = obj.optString("colorHex", "#F2CC8F"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Import Reading Goals
            if (root.has("readingGoals")) {
                val array = root.getJSONArray("readingGoals")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val bookId = obj.optLong("bookId", -1L)
                    db.readingGoalDao().insertGoal(
                        ReadingGoalEntity(
                            title = obj.getString("title"),
                            goalType = obj.optString("goalType", "READ_PAGES_IN_DAYS"),
                            bookId = if (bookId > 0) bookId else null,
                            bookTitle = obj.optString("bookTitle", null),
                            targetPages = obj.getInt("targetPages"),
                            targetDays = obj.getInt("targetDays"),
                            startDate = obj.optLong("startDate", System.currentTimeMillis()),
                            targetEndDate = obj.optLong("targetEndDate", System.currentTimeMillis() + 604800000L),
                            pagesReadSoFar = obj.optInt("pagesReadSoFar", 0),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                }
            }

            // Import Sessions
            if (root.has("readingSessions")) {
                val array = root.getJSONArray("readingSessions")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    db.readingSessionDao().insertSession(
                        ReadingSessionEntity(
                            bookId = obj.getLong("bookId"),
                            bookTitle = obj.optString("bookTitle", "Livro"),
                            dateEpochDay = obj.getLong("dateEpochDay"),
                            durationSeconds = obj.optLong("durationSeconds", 0L),
                            pagesRead = obj.optInt("pagesRead", 0),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
