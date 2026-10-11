package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String = "Autor desconhecido",
    val filePath: String,
    val originalFileName: String,
    val format: String, // "PDF", "EPUB", "CBZ", "CBR"
    val totalPages: Int = 1,
    val currentPage: Int = 1,
    val lastReadTimestamp: Long = 0L,
    val coverImagePath: String? = null,
    val originalCoverPath: String? = null,
    val fileSizeBytes: Long = 0L,
    val readingStatus: String = "NOT_STARTED", // "NOT_STARTED", "READING", "COMPLETED"
    val dateAdded: Long = System.currentTimeMillis(),
    val comicReadingMode: String = "LTR", // "LTR", "RTL", "VERTICAL_PAGED", "WEBTOON"
    val isFavorite: Boolean = false,
    val cropBorders: Boolean = false
) {
    val progressPercentage: Int
        get() = if (totalPages > 0) {
            ((currentPage.toFloat() / totalPages.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else 0
}
