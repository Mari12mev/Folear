package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY chapterIndex ASC, createdAt ASC")
    fun getAnnotationsForBook(bookId: Long): Flow<List<AnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: AnnotationEntity): Long

    @Update
    suspend fun updateAnnotation(annotation: AnnotationEntity)

    @Delete
    suspend fun deleteAnnotation(annotation: AnnotationEntity)

    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    suspend fun getAnnotationsForBookSync(bookId: Long): List<AnnotationEntity>

    @Query("SELECT * FROM annotations")
    suspend fun getAllAnnotationsSync(): List<AnnotationEntity>
}
