package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.AppDatabase
import com.example.data.BackupManager
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookEntity
import com.example.data.model.BookFolderCrossRef
import com.example.data.model.BookTagCrossRef
import com.example.data.model.BookmarkEntity
import com.example.data.model.FolderEntity
import com.example.data.model.ReadingGoalEntity
import com.example.data.model.ReadingSessionEntity
import com.example.data.model.TagEntity
import com.example.reader.BookFileProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class BookRepository(val db: AppDatabase) {
    private val bookDao = db.bookDao()
    private val folderDao = db.folderDao()
    private val goalDao = db.readingGoalDao()
    private val tagDao = db.tagDao()
    private val bookmarkDao = db.bookmarkDao()
    private val annotationDao = db.annotationDao()
    private val sessionDao = db.readingSessionDao()

    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()
    val inProgressBooks: Flow<List<BookEntity>> = bookDao.getInProgressBooks()
    val favoriteBooks: Flow<List<BookEntity>> = bookDao.getFavoriteBooks()
    val recentHistory: Flow<List<BookEntity>> = bookDao.getRecentHistory(15)
    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    val allTags: Flow<List<TagEntity>> = tagDao.getAllTags()
    val allGoals: Flow<List<ReadingGoalEntity>> = goalDao.getAllGoals()
    val activeGoals: Flow<List<ReadingGoalEntity>> = goalDao.getActiveGoals()
    val totalReadingTimeSeconds: Flow<Long?> = sessionDao.getTotalReadingTimeSeconds()
    val totalPagesRead: Flow<Int?> = sessionDao.getTotalPagesRead()
    val distinctReadingDays: Flow<List<Long>> = sessionDao.getDistinctReadingDays()

    fun getBooksByFolder(folderId: Long): Flow<List<BookEntity>> = bookDao.getBooksByFolder(folderId)
    fun getBooksByTag(tagId: Long): Flow<List<BookEntity>> = bookDao.getBooksByTag(tagId)
    fun searchBooks(query: String): Flow<List<BookEntity>> = bookDao.searchBooks(query)

    fun getFolderIdsForBook(bookId: Long): Flow<List<Long>> = folderDao.getFolderIdsForBook(bookId)
    fun getTagIdsForBook(bookId: Long): Flow<List<Long>> = tagDao.getTagIdsForBook(bookId)
    fun getTagsForBook(bookId: Long): Flow<List<TagEntity>> = tagDao.getTagsForBook(bookId)

    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>> = bookmarkDao.getBookmarksForBook(bookId)
    fun getAnnotationsForBook(bookId: Long): Flow<List<AnnotationEntity>> = annotationDao.getAnnotationsForBook(bookId)

    suspend fun getFolderIdsForBookSync(bookId: Long): List<Long> = withContext(Dispatchers.IO) {
        folderDao.getFolderIdsForBookSync(bookId)
    }

    suspend fun getTagIdsForBookSync(bookId: Long): List<Long> = withContext(Dispatchers.IO) {
        tagDao.getTagIdsForBookSync(bookId)
    }

    suspend fun getBookByIdSync(bookId: Long): BookEntity? = withContext(Dispatchers.IO) {
        bookDao.getBookByIdSync(bookId)
    }

    suspend fun importBook(context: Context, uri: Uri): BookEntity? = withContext(Dispatchers.IO) {
        val imported = BookFileProcessor.importFileFromUri(context, uri) ?: return@withContext null
        val id = bookDao.insertBook(imported)
        imported.copy(id = id)
    }

    suspend fun updateReadingProgress(bookId: Long, page: Int, totalPages: Int, isIncognito: Boolean = false) = withContext(Dispatchers.IO) {
        val status = if (page >= totalPages && totalPages > 0) "COMPLETED" else "READING"
        val timestamp = if (isIncognito) 0L else System.currentTimeMillis()
        
        val currentBook = bookDao.getBookByIdSync(bookId)
        val pagesDelta = if (currentBook != null) (page - currentBook.currentPage).coerceAtLeast(0) else 0

        bookDao.updateReadingProgress(bookId, page, totalPages, if (isIncognito) currentBook?.lastReadTimestamp ?: 0L else timestamp, status)

        // Automatically advance active goals when not in incognito mode
        if (!isIncognito && pagesDelta > 0) {
            advanceActiveGoals(bookId, currentBook?.title, pagesDelta)
        }
    }

    private suspend fun advanceActiveGoals(bookId: Long, bookTitle: String?, pagesCount: Int) {
        try {
            val goals = goalDao.getActiveGoals().first()
            for (goal in goals) {
                // If goal is for this specific book or is a general "READ_PAGES_IN_DAYS" goal
                if (goal.bookId == null || goal.bookId == bookId) {
                    val newPages = goal.pagesReadSoFar + pagesCount
                    val isDone = newPages >= goal.targetPages
                    goalDao.updateProgress(goal.id, newPages, isDone)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun logReadingSession(bookId: Long, bookTitle: String, durationSeconds: Long, pagesRead: Int, isIncognito: Boolean = false) = withContext(Dispatchers.IO) {
        if (isIncognito || durationSeconds < 5) return@withContext
        val now = System.currentTimeMillis()
        val epochDay = now / 86_400_000L
        sessionDao.insertSession(
            ReadingSessionEntity(
                bookId = bookId,
                bookTitle = bookTitle,
                dateEpochDay = epochDay,
                durationSeconds = durationSeconds,
                pagesRead = pagesRead,
                timestamp = now
            )
        )
    }

    suspend fun getSessionsSince(sinceTimestamp: Long): List<ReadingSessionEntity> = withContext(Dispatchers.IO) {
        sessionDao.getSessionsSince(sinceTimestamp)
    }

    suspend fun updateComicReadingMode(bookId: Long, mode: String) = withContext(Dispatchers.IO) {
        bookDao.updateComicReadingMode(bookId, mode)
    }

    suspend fun updateCropBorders(bookId: Long, crop: Boolean) = withContext(Dispatchers.IO) {
        bookDao.updateCropBorders(bookId, crop)
    }

    suspend fun toggleFavorite(bookId: Long, isFav: Boolean) = withContext(Dispatchers.IO) {
        bookDao.updateFavorite(bookId, isFav)
    }

    suspend fun updateBookDetails(bookId: Long, title: String, author: String) = withContext(Dispatchers.IO) {
        bookDao.updateBookDetails(bookId, title, author)
    }

    suspend fun updateReadingStatus(bookId: Long, status: String) = withContext(Dispatchers.IO) {
        bookDao.updateReadingStatus(bookId, status)
    }

    suspend fun updateCover(bookId: Long, newCoverPath: String) = withContext(Dispatchers.IO) {
        bookDao.updateCover(bookId, newCoverPath)
    }

    suspend fun restoreOriginalCover(bookId: Long) = withContext(Dispatchers.IO) {
        bookDao.restoreOriginalCover(bookId)
    }

    suspend fun deleteBook(book: BookEntity) = withContext(Dispatchers.IO) {
        bookDao.deleteBook(book)
        folderDao.removeAllFoldersForBook(book.id)
        tagDao.removeAllTagsForBook(book.id)
        try {
            File(book.filePath).delete()
            book.coverImagePath?.let { File(it).delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Folders
    suspend fun createFolder(name: String, icon: String = "folder", colorHex: String = "#E07A5F"): Long = withContext(Dispatchers.IO) {
        folderDao.insertFolder(FolderEntity(name = name, iconName = icon, colorHex = colorHex))
    }

    suspend fun updateFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        folderDao.updateFolder(folder)
    }

    suspend fun deleteFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        folderDao.removeAllBooksFromFolder(folder.id)
        folderDao.deleteFolder(folder)
    }

    suspend fun setBookFolders(bookId: Long, folderIds: List<Long>) = withContext(Dispatchers.IO) {
        folderDao.removeAllFoldersForBook(bookId)
        for (folderId in folderIds) {
            folderDao.addCrossRef(BookFolderCrossRef(bookId = bookId, folderId = folderId))
        }
    }

    // Tags
    suspend fun createTag(name: String, colorHex: String = "#E07A5F"): Long = withContext(Dispatchers.IO) {
        tagDao.insertTag(TagEntity(name = name, colorHex = colorHex))
    }

    suspend fun deleteTag(tag: TagEntity) = withContext(Dispatchers.IO) {
        tagDao.removeAllBooksFromTag(tag.id)
        tagDao.deleteTag(tag)
    }

    suspend fun setBookTags(bookId: Long, tagIds: List<Long>) = withContext(Dispatchers.IO) {
        tagDao.removeAllTagsForBook(bookId)
        for (tagId in tagIds) {
            tagDao.addCrossRef(BookTagCrossRef(bookId = bookId, tagId = tagId))
        }
    }

    // Bookmarks
    suspend fun addBookmark(bookId: Long, pageOrChapter: Int, title: String, note: String? = null) = withContext(Dispatchers.IO) {
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                bookId = bookId,
                pageOrChapter = pageOrChapter,
                title = title,
                note = note
            )
        )
    }

    suspend fun removeBookmark(bookmark: BookmarkEntity) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmark(bookmark)
    }

    suspend fun hasBookmarkAtPage(bookId: Long, page: Int): Boolean = withContext(Dispatchers.IO) {
        bookmarkDao.hasBookmarkAtPage(bookId, page) > 0
    }

    // Annotations (Highlights & Notes)
    suspend fun addAnnotation(bookId: Long, chapterIndex: Int, chapterTitle: String, selectedText: String, note: String?, colorHex: String) = withContext(Dispatchers.IO) {
        annotationDao.insertAnnotation(
            AnnotationEntity(
                bookId = bookId,
                chapterIndex = chapterIndex,
                chapterTitle = chapterTitle,
                selectedText = selectedText,
                note = note,
                colorHex = colorHex
            )
        )
    }

    suspend fun deleteAnnotation(annotation: AnnotationEntity) = withContext(Dispatchers.IO) {
        annotationDao.deleteAnnotation(annotation)
    }

    suspend fun updateAnnotation(annotation: AnnotationEntity) = withContext(Dispatchers.IO) {
        annotationDao.updateAnnotation(annotation)
    }

    // Goals
    suspend fun createReadingGoal(title: String, goalType: String, bookId: Long?, bookTitle: String?, targetPages: Int, targetDays: Int): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val endDate = now + TimeUnit.DAYS.toMillis(targetDays.toLong())
        goalDao.insertGoal(
            ReadingGoalEntity(
                title = title,
                goalType = goalType,
                bookId = bookId,
                bookTitle = bookTitle,
                targetPages = targetPages,
                targetDays = targetDays,
                startDate = now,
                targetEndDate = endDate,
                pagesReadSoFar = 0,
                isCompleted = false
            )
        )
    }

    suspend fun updateGoal(goal: ReadingGoalEntity) = withContext(Dispatchers.IO) {
        goalDao.updateGoal(goal)
    }

    suspend fun toggleGoalCompleted(goalId: Long, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        goalDao.setCompleted(goalId, isCompleted)
    }

    suspend fun deleteGoal(goal: ReadingGoalEntity) = withContext(Dispatchers.IO) {
        goalDao.deleteGoal(goal)
    }

    // Backup & Restore
    suspend fun exportBackup(context: Context, uri: Uri): Boolean = BackupManager.exportBackup(context, uri, db)
    suspend fun importBackup(context: Context, uri: Uri): Boolean = BackupManager.importBackup(context, uri, db)

    // Initial Defaults
    suspend fun initializeDefaultsIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val folderCount = folderDao.getFolderCount()
        if (folderCount == 0) {
            val f1 = folderDao.insertFolder(FolderEntity(name = "Lendo agora", iconName = "flame", colorHex = "#E07A5F", isSystem = true))
            val f2 = folderDao.insertFolder(FolderEntity(name = "Mangás & HQs", iconName = "manga", colorHex = "#F2CC8F", isSystem = false))
            val f3 = folderDao.insertFolder(FolderEntity(name = "Fantasia", iconName = "fantasy", colorHex = "#81B29A", isSystem = false))
            val f4 = folderDao.insertFolder(FolderEntity(name = "Favoritos", iconName = "star", colorHex = "#E07A5F", isSystem = false))

            // Default tags
            val t1 = tagDao.insertTag(TagEntity(name = "Ficção", colorHex = "#81B29A"))
            val t2 = tagDao.insertTag(TagEntity(name = "Ação", colorHex = "#E07A5F"))
            val t3 = tagDao.insertTag(TagEntity(name = "Clássico", colorHex = "#F2CC8F"))

            // Seed sample books
            val samples = BookFileProcessor.seedSampleLibrary(context)
            for (sample in samples) {
                val bookId = bookDao.insertBook(sample)
                if (sample.format == "CBZ") {
                    folderDao.addCrossRef(BookFolderCrossRef(bookId, f1))
                    folderDao.addCrossRef(BookFolderCrossRef(bookId, f2))
                    tagDao.addCrossRef(BookTagCrossRef(bookId, t2))
                } else if (sample.format == "EPUB") {
                    folderDao.addCrossRef(BookFolderCrossRef(bookId, f1))
                    folderDao.addCrossRef(BookFolderCrossRef(bookId, f3))
                    tagDao.addCrossRef(BookTagCrossRef(bookId, t1))
                }
            }

            // Seed sample goals
            val now = System.currentTimeMillis()
            goalDao.insertGoal(
                ReadingGoalEntity(
                    title = "Terminar Mangá Guardião dos Sonhos",
                    goalType = "FINISH_BOOK_IN_DAYS",
                    bookId = 1L,
                    bookTitle = "O Guardião dos Sonhos #01",
                    targetPages = 10,
                    targetDays = 3,
                    startDate = now,
                    targetEndDate = now + TimeUnit.DAYS.toMillis(3),
                    pagesReadSoFar = 3,
                    isCompleted = false
                )
            )

            // Seed initial reading session for streak and stats demo
            val todayEpochDay = now / 86_400_000L
            sessionDao.insertSession(
                ReadingSessionEntity(
                    bookId = 1L,
                    bookTitle = "O Guardião dos Sonhos #01",
                    dateEpochDay = todayEpochDay,
                    durationSeconds = 1200L, // 20 min
                    pagesRead = 3,
                    timestamp = now
                )
            )
            sessionDao.insertSession(
                ReadingSessionEntity(
                    bookId = 2L,
                    bookTitle = "O Pequeno Viajante das Estrelas",
                    dateEpochDay = todayEpochDay - 1,
                    durationSeconds = 1800L, // 30 min
                    pagesRead = 14,
                    timestamp = now - 86_400_000L
                )
            )
        }
    }
}
