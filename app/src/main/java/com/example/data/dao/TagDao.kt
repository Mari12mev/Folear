package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BookTagCrossRef
import com.example.data.model.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCrossRef(crossRef: BookTagCrossRef)

    @Query("DELETE FROM book_tag_cross_ref WHERE bookId = :bookId AND tagId = :tagId")
    suspend fun removeCrossRef(bookId: Long, tagId: Long)

    @Query("DELETE FROM book_tag_cross_ref WHERE bookId = :bookId")
    suspend fun removeAllTagsForBook(bookId: Long)

    @Query("DELETE FROM book_tag_cross_ref WHERE tagId = :tagId")
    suspend fun removeAllBooksFromTag(tagId: Long)

    @Query("SELECT tagId FROM book_tag_cross_ref WHERE bookId = :bookId")
    fun getTagIdsForBook(bookId: Long): Flow<List<Long>>

    @Query("SELECT tagId FROM book_tag_cross_ref WHERE bookId = :bookId")
    suspend fun getTagIdsForBookSync(bookId: Long): List<Long>

    @Query("SELECT t.* FROM tags t INNER JOIN book_tag_cross_ref x ON t.id = x.tagId WHERE x.bookId = :bookId")
    fun getTagsForBook(bookId: Long): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags")
    suspend fun getAllTagsSync(): List<TagEntity>

    @Query("SELECT * FROM book_tag_cross_ref")
    suspend fun getAllCrossRefsSync(): List<BookTagCrossRef>
}
