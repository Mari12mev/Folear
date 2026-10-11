package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {
    @Query("SELECT * FROM reading_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ReadingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSessionEntity): Long

    @Query("SELECT SUM(durationSeconds) FROM reading_sessions")
    fun getTotalReadingTimeSeconds(): Flow<Long?>

    @Query("SELECT SUM(pagesRead) FROM reading_sessions")
    fun getTotalPagesRead(): Flow<Int?>

    @Query("SELECT * FROM reading_sessions WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getSessionsSince(sinceTimestamp: Long): List<ReadingSessionEntity>

    @Query("SELECT DISTINCT dateEpochDay FROM reading_sessions ORDER BY dateEpochDay DESC")
    fun getDistinctReadingDays(): Flow<List<Long>>

    @Query("SELECT * FROM reading_sessions")
    suspend fun getAllSessionsSync(): List<ReadingSessionEntity>
}
