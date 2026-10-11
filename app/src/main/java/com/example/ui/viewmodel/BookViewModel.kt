package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.FolderEntity
import com.example.data.model.ReadingGoalEntity
import com.example.data.model.ReadingSessionEntity
import com.example.data.model.TagEntity
import com.example.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookViewModel(application: Application) : AndroidViewModel(application) {
    val repository = BookRepository(AppDatabase.getInstance(application))

    // Dark mode state - dark as default
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Incognito mode: pauses history and stats recording
    private val _isIncognitoMode = MutableStateFlow(false)
    val isIncognitoMode: StateFlow<Boolean> = _isIncognitoMode.asStateFlow()

    // Navigation tab
    private val _currentTab = MutableStateFlow("library") // "library", "folders", "goals", "stats"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Currently active reader book
    private val _readingBook = MutableStateFlow<BookEntity?>(null)
    val readingBook: StateFlow<BookEntity?> = _readingBook.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Folder & Tag Filters
    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    private val _selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagId: StateFlow<Long?> = _selectedTagId.asStateFlow()

    // Reading Status Filter: "ALL", "UNREAD", "READING", "COMPLETED", "FAVORITES"
    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    // Sort Option: "LAST_READ", "TITLE", "AUTHOR", "DATE_ADDED", "PROGRESS"
    private val _sortOption = MutableStateFlow("LAST_READ")
    val sortOption: StateFlow<String> = _sortOption.asStateFlow()

    // Dialog & UI State
    val showNewGoalDialog = MutableStateFlow(false)
    val showNewFolderDialog = MutableStateFlow(false)
    val showNewTagDialog = MutableStateFlow(false)
    val editingFolder = MutableStateFlow<FolderEntity?>(null)
    val managingBookFolders = MutableStateFlow<BookEntity?>(null)
    val managingBookTags = MutableStateFlow<BookEntity?>(null)
    val editingBookDetails = MutableStateFlow<BookEntity?>(null)
    val changingCoverBook = MutableStateFlow<BookEntity?>(null)
    val showCbrInfoDialog = MutableStateFlow(false)
    val showBackupDialog = MutableStateFlow(false)
    val isImporting = MutableStateFlow(false)

    // Data streams
    val allFolders: StateFlow<List<FolderEntity>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<TagEntity>> = repository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inProgressBooks: StateFlow<List<BookEntity>> = repository.inProgressBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentHistory: StateFlow<List<BookEntity>> = repository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoals: StateFlow<List<ReadingGoalEntity>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalReadingTimeSeconds: StateFlow<Long> = repository.totalReadingTimeSeconds
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalPagesRead: StateFlow<Int> = repository.totalPagesRead
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Streak calculation (consecutive days)
    val streakDays: StateFlow<Int> = repository.distinctReadingDays.map { days ->
        if (days.isEmpty()) return@map 0
        val sorted = days.sortedDescending()
        val today = System.currentTimeMillis() / 86_400_000L
        var streak = 0
        var expected = if (sorted.contains(today)) today else today - 1
        for (day in sorted) {
            if (day == expected) {
                streak++
                expected--
            } else if (day < expected) {
                break
            }
        }
        streak
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private data class FilterState(
        val folderId: Long?,
        val tagId: Long?,
        val status: String,
        val sort: String,
        val query: String
    )

    private val filterState = combine(
        _selectedFolderId,
        _selectedTagId,
        _statusFilter,
        _sortOption,
        _searchQuery
    ) { folderId, tagId, status, sort, query ->
        FilterState(folderId, tagId, status, sort, query)
    }

    // Filtered and Sorted Books for Library
    val displayedBooks: StateFlow<List<BookEntity>> = combine(
        repository.allBooks,
        filterState
    ) { books, filter ->
        var list = books

        // Filter by Folder
        if (filter.folderId != null) {
            list = list.filter { repository.getFolderIdsForBookSync(it.id).contains(filter.folderId) }
        }

        // Filter by Tag
        if (filter.tagId != null) {
            list = list.filter { repository.getTagIdsForBookSync(it.id).contains(filter.tagId) }
        }

        // Filter by Status / Favorites
        list = when (filter.status) {
            "UNREAD" -> list.filter { it.currentPage <= 1 && it.progressPercentage == 0 }
            "READING" -> list.filter { it.currentPage > 1 && it.progressPercentage < 100 }
            "COMPLETED" -> list.filter { it.progressPercentage >= 100 || it.readingStatus == "COMPLETED" }
            "FAVORITES" -> list.filter { it.isFavorite }
            else -> list
        }

        // Search Filter
        if (filter.query.isNotBlank()) {
            val q = filter.query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) || it.author.lowercase().contains(q)
            }
        }

        // Sorting
        list = when (filter.sort) {
            "TITLE" -> list.sortedBy { it.title.lowercase() }
            "AUTHOR" -> list.sortedBy { it.author.lowercase() }
            "DATE_ADDED" -> list.sortedByDescending { it.dateAdded }
            "PROGRESS" -> list.sortedByDescending { it.progressPercentage }
            else -> list.sortedByDescending { it.lastReadTimestamp }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded(getApplication())
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun toggleIncognito() {
        _isIncognitoMode.value = !_isIncognitoMode.value
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFolderFilter(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    fun selectTagFilter(tagId: Long?) {
        _selectedTagId.value = tagId
    }

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    fun setSortOption(sort: String) {
        _sortOption.value = sort
    }

    fun openReader(book: BookEntity) {
        _readingBook.value = book
    }

    fun closeReader() {
        _readingBook.value = null
    }

    fun importBook(uri: Uri) {
        viewModelScope.launch {
            isImporting.value = true
            val imported = repository.importBook(getApplication(), uri)
            isImporting.value = false
            if (imported != null && imported.format == "CBR") {
                showCbrInfoDialog.value = true
            }
        }
    }

    fun updateReadingProgress(bookId: Long, page: Int, totalPages: Int) {
        viewModelScope.launch {
            repository.updateReadingProgress(bookId, page, totalPages, isIncognito = _isIncognitoMode.value)
            if (_readingBook.value?.id == bookId) {
                _readingBook.value = _readingBook.value?.copy(
                    currentPage = page,
                    totalPages = totalPages,
                    readingStatus = if (page >= totalPages) "COMPLETED" else "READING"
                )
            }
        }
    }

    fun logReadingSession(bookId: Long, bookTitle: String, durationSeconds: Long, pagesRead: Int) {
        viewModelScope.launch {
            repository.logReadingSession(bookId, bookTitle, durationSeconds, pagesRead, isIncognito = _isIncognitoMode.value)
        }
    }

    fun setComicReadingMode(bookId: Long, mode: String) {
        viewModelScope.launch {
            repository.updateComicReadingMode(bookId, mode)
            if (_readingBook.value?.id == bookId) {
                _readingBook.value = _readingBook.value?.copy(comicReadingMode = mode)
            }
        }
    }

    fun setCropBorders(bookId: Long, crop: Boolean) {
        viewModelScope.launch {
            repository.updateCropBorders(bookId, crop)
            if (_readingBook.value?.id == bookId) {
                _readingBook.value = _readingBook.value?.copy(cropBorders = crop)
            }
        }
    }

    fun toggleFavorite(book: BookEntity) {
        viewModelScope.launch {
            val newFav = !book.isFavorite
            repository.toggleFavorite(book.id, newFav)
            if (_readingBook.value?.id == book.id) {
                _readingBook.value = _readingBook.value?.copy(isFavorite = newFav)
            }
        }
    }

    fun toggleCompleted(book: BookEntity) {
        viewModelScope.launch {
            val isNowCompleted = book.readingStatus != "COMPLETED"
            val newPage = if (isNowCompleted) book.totalPages else 1
            val newStatus = if (isNowCompleted) "COMPLETED" else "READING"
            repository.updateReadingProgress(book.id, newPage, book.totalPages, isIncognito = _isIncognitoMode.value)
        }
    }

    fun updateBookDetails(bookId: Long, title: String, author: String) {
        viewModelScope.launch {
            repository.updateBookDetails(bookId, title, author)
            editingBookDetails.value = null
            if (_readingBook.value?.id == bookId) {
                _readingBook.value = _readingBook.value?.copy(title = title, author = author)
            }
        }
    }

    fun updateCover(bookId: Long, newCoverPath: String) {
        viewModelScope.launch {
            repository.updateCover(bookId, newCoverPath)
            if (_readingBook.value?.id == bookId) {
                _readingBook.value = _readingBook.value?.copy(coverImagePath = newCoverPath)
            }
        }
    }

    fun restoreOriginalCover(bookId: Long) {
        viewModelScope.launch {
            repository.restoreOriginalCover(bookId)
            val updated = repository.getBookByIdSync(bookId)
            if (_readingBook.value?.id == bookId && updated != null) {
                _readingBook.value = updated
            }
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            if (_readingBook.value?.id == book.id) {
                _readingBook.value = null
            }
            repository.deleteBook(book)
        }
    }

    // Folders
    fun createFolder(name: String, icon: String, colorHex: String) {
        viewModelScope.launch {
            repository.createFolder(name, icon, colorHex)
            showNewFolderDialog.value = false
        }
    }

    fun updateFolder(folder: FolderEntity) {
        viewModelScope.launch {
            repository.updateFolder(folder)
            editingFolder.value = null
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch {
            if (_selectedFolderId.value == folder.id) {
                _selectedFolderId.value = null
            }
            repository.deleteFolder(folder)
        }
    }

    suspend fun getFolderIdsForBook(bookId: Long): List<Long> {
        return repository.getFolderIdsForBookSync(bookId)
    }

    fun setBookFolders(bookId: Long, folderIds: List<Long>) {
        viewModelScope.launch {
            repository.setBookFolders(bookId, folderIds)
            managingBookFolders.value = null
        }
    }

    // Tags
    fun createTag(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.createTag(name, colorHex)
            showNewTagDialog.value = false
        }
    }

    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            if (_selectedTagId.value == tag.id) {
                _selectedTagId.value = null
            }
            repository.deleteTag(tag)
        }
    }

    suspend fun getTagIdsForBook(bookId: Long): List<Long> {
        return repository.getTagIdsForBookSync(bookId)
    }

    fun setBookTags(bookId: Long, tagIds: List<Long>) {
        viewModelScope.launch {
            repository.setBookTags(bookId, tagIds)
            managingBookTags.value = null
        }
    }

    // Bookmarks
    fun addBookmark(bookId: Long, pageOrChapter: Int, title: String, note: String? = null) {
        viewModelScope.launch {
            repository.addBookmark(bookId, pageOrChapter, title, note)
        }
    }

    fun removeBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            repository.removeBookmark(bookmark)
        }
    }

    fun getBookmarks(bookId: Long) = repository.getBookmarksForBook(bookId)

    // Annotations (Highlights & Notes)
    fun addAnnotation(bookId: Long, chapterIndex: Int, chapterTitle: String, selectedText: String, note: String?, colorHex: String) {
        viewModelScope.launch {
            repository.addAnnotation(bookId, chapterIndex, chapterTitle, selectedText, note, colorHex)
        }
    }

    fun deleteAnnotation(annotation: AnnotationEntity) {
        viewModelScope.launch {
            repository.deleteAnnotation(annotation)
        }
    }

    fun getAnnotations(bookId: Long) = repository.getAnnotationsForBook(bookId)

    // Reading Goals
    fun createReadingGoal(title: String, goalType: String, bookId: Long?, bookTitle: String?, targetPages: Int, targetDays: Int) {
        viewModelScope.launch {
            repository.createReadingGoal(title, goalType, bookId, bookTitle, targetPages, targetDays)
            showNewGoalDialog.value = false
        }
    }

    fun addGoalPages(goal: ReadingGoalEntity, additionalPages: Int) {
        viewModelScope.launch {
            val newPages = (goal.pagesReadSoFar + additionalPages).coerceAtLeast(0)
            val isCompleted = newPages >= goal.targetPages
            repository.updateGoal(goal.copy(pagesReadSoFar = newPages, isCompleted = isCompleted))
        }
    }

    fun toggleGoalCompleted(goal: ReadingGoalEntity) {
        viewModelScope.launch {
            val updated = !goal.isCompleted
            repository.toggleGoalCompleted(goal.id, updated)
        }
    }

    fun deleteGoal(goal: ReadingGoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    // Backup & Restore
    fun exportBackup(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.exportBackup(getApplication(), uri)
            onResult(success)
        }
    }

    fun importBackup(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.importBackup(getApplication(), uri)
            onResult(success)
        }
    }
}
