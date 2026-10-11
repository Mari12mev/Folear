package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val bookTitle: String,
    val dateEpochDay: Long, // Epoch day (System.currentTimeMillis() / 86_400_000L)
    val durationSeconds: Long = 0L,
    val pagesRead: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
