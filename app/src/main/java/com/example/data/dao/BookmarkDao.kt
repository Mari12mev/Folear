package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY pageOrChapter ASC")
    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId AND pageOrChapter = :page")
    suspend fun removeBookmarkAtPage(bookId: Long, page: Int)

    @Query("SELECT COUNT(*) FROM bookmarks WHERE bookId = :bookId AND pageOrChapter = :page")
    suspend fun hasBookmarkAtPage(bookId: Long, page: Int): Int

    @Query("SELECT * FROM bookmarks")
    suspend fun getAllBookmarksSync(): List<BookmarkEntity>
}
