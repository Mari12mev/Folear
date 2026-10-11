package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ReadingGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingGoalDao {
    @Query("SELECT * FROM reading_goals ORDER BY isCompleted ASC, targetEndDate ASC")
    fun getAllGoals(): Flow<List<ReadingGoalEntity>>

    @Query("SELECT * FROM reading_goals WHERE isCompleted = 0 ORDER BY targetEndDate ASC")
    fun getActiveGoals(): Flow<List<ReadingGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: ReadingGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: ReadingGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: ReadingGoalEntity)

    @Query("UPDATE reading_goals SET pagesReadSoFar = :pages, isCompleted = :isCompleted WHERE id = :goalId")
    suspend fun updateProgress(goalId: Long, pages: Int, isCompleted: Boolean)

    @Query("UPDATE reading_goals SET isCompleted = :isCompleted WHERE id = :goalId")
    suspend fun setCompleted(goalId: Long, isCompleted: Boolean)

    @Query("SELECT * FROM reading_goals")
    suspend fun getAllGoalsSync(): List<ReadingGoalEntity>
}
