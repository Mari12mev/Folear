package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String = "folder", // "book", "bookmark", "fantasy", "manga", "star", "flame", "folder"
    val colorHex: String = "#E07A5F",
    val isSystem: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
