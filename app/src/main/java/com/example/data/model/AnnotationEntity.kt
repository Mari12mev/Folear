package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "annotations")
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val chapterTitle: String,
    val selectedText: String,
    val note: String? = null,
    val colorHex: String = "#F2CC8F", // Highlight color
    val createdAt: Long = System.currentTimeMillis()
)
