package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BookFolderCrossRef
import com.example.data.model.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY isSystem DESC, name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Query("DELETE FROM book_folder_cross_ref WHERE folderId = :folderId")
    suspend fun removeAllBooksFromFolder(folderId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCrossRef(crossRef: BookFolderCrossRef)

    @Query("DELETE FROM book_folder_cross_ref WHERE bookId = :bookId AND folderId = :folderId")
    suspend fun removeCrossRef(bookId: Long, folderId: Long)

    @Query("DELETE FROM book_folder_cross_ref WHERE bookId = :bookId")
    suspend fun removeAllFoldersForBook(bookId: Long)

    @Query("SELECT folderId FROM book_folder_cross_ref WHERE bookId = :bookId")
    fun getFolderIdsForBook(bookId: Long): Flow<List<Long>>

    @Query("SELECT folderId FROM book_folder_cross_ref WHERE bookId = :bookId")
    suspend fun getFolderIdsForBookSync(bookId: Long): List<Long>

    @Query("SELECT COUNT(*) FROM book_folder_cross_ref WHERE folderId = :folderId")
    fun getBookCountInFolder(folderId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM folders")
    suspend fun getFolderCount(): Int

    @Query("SELECT * FROM folders")
    suspend fun getAllFoldersSync(): List<FolderEntity>
}
