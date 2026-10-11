package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadTimestamp DESC, dateAdded DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE readingStatus = 'READING' OR (currentPage > 1 AND currentPage < totalPages) ORDER BY lastReadTimestamp DESC")
    fun getInProgressBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE lastReadTimestamp > 0 ORDER BY lastReadTimestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 10): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isFavorite = 1 ORDER BY lastReadTimestamp DESC, title ASC")
    fun getFavoriteBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookByIdSync(id: Long): BookEntity?

    @Query("""
        SELECT b.* FROM books b 
        INNER JOIN book_folder_cross_ref x ON b.id = x.bookId 
        WHERE x.folderId = :folderId 
        ORDER BY b.lastReadTimestamp DESC, b.dateAdded DESC
    """)
    fun getBooksByFolder(folderId: Long): Flow<List<BookEntity>>

    @Query("""
        SELECT b.* FROM books b 
        INNER JOIN book_tag_cross_ref x ON b.id = x.bookId 
        WHERE x.tagId = :tagId 
        ORDER BY b.lastReadTimestamp DESC, b.dateAdded DESC
    """)
    fun getBooksByTag(tagId: Long): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("UPDATE books SET currentPage = :page, totalPages = :totalPages, lastReadTimestamp = :timestamp, readingStatus = :status WHERE id = :bookId")
    suspend fun updateReadingProgress(bookId: Long, page: Int, totalPages: Int, timestamp: Long, status: String)

    @Query("UPDATE books SET coverImagePath = :coverPath WHERE id = :bookId")
    suspend fun updateCover(bookId: Long, coverPath: String)

    @Query("UPDATE books SET coverImagePath = originalCoverPath WHERE id = :bookId")
    suspend fun restoreOriginalCover(bookId: Long)

    @Query("UPDATE books SET comicReadingMode = :mode WHERE id = :bookId")
    suspend fun updateComicReadingMode(bookId: Long, mode: String)

    @Query("UPDATE books SET cropBorders = :crop WHERE id = :bookId")
    suspend fun updateCropBorders(bookId: Long, crop: Boolean)

    @Query("UPDATE books SET isFavorite = :isFav WHERE id = :bookId")
    suspend fun updateFavorite(bookId: Long, isFav: Boolean)

    @Query("UPDATE books SET title = :title, author = :author WHERE id = :bookId")
    suspend fun updateBookDetails(bookId: Long, title: String, author: String)

    @Query("UPDATE books SET readingStatus = :status WHERE id = :bookId")
    suspend fun updateReadingStatus(bookId: Long, status: String)

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    @Query("SELECT * FROM books")
    suspend fun getAllBooksSync(): List<BookEntity>
}
