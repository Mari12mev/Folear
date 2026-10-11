package com.example.data.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "book_tag_cross_ref",
    primaryKeys = ["bookId", "tagId"],
    indices = [Index(value = ["tagId"]), Index(value = ["bookId"])]
)
data class BookTagCrossRef(
    val bookId: Long,
    val tagId: Long
)
