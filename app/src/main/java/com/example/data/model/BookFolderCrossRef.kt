package com.example.data.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "book_folder_cross_ref",
    primaryKeys = ["bookId", "folderId"],
    indices = [Index(value = ["folderId"]), Index(value = ["bookId"])]
)
data class BookFolderCrossRef(
    val bookId: Long,
    val folderId: Long
)
