package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max

@Entity(tableName = "reading_goals")
data class ReadingGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val goalType: String = "FINISH_BOOK_IN_DAYS", // "FINISH_BOOK_IN_DAYS" or "READ_PAGES_IN_DAYS"
    val bookId: Long? = null,
    val bookTitle: String? = null,
    val targetPages: Int = 100,
    val targetDays: Int = 7,
    val startDate: Long = System.currentTimeMillis(),
    val targetEndDate: Long = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7),
    val pagesReadSoFar: Int = 0,
    val isCompleted: Boolean = false
) {
    val daysRemaining: Int
        get() {
            val now = System.currentTimeMillis()
            val diffMs = targetEndDate - now
            return max(0, ceil(diffMs.toDouble() / TimeUnit.DAYS.toMillis(1).toDouble()).toInt())
        }

    val daysPassed: Int
        get() {
            val now = System.currentTimeMillis()
            val diffMs = max(0L, now - startDate)
            return ceil(diffMs.toDouble() / TimeUnit.DAYS.toMillis(1).toDouble()).toInt().coerceAtLeast(1)
        }

    // Required daily rhythm based on remaining days and remaining pages
    val requiredDailyRhythm: Int
        get() {
            val remainingPages = max(0, targetPages - pagesReadSoFar)
            val remDays = max(1, daysRemaining)
            return ceil(remainingPages.toDouble() / remDays.toDouble()).toInt()
        }

    // Expected pages by current day to be on pace
    val expectedPagesSoFar: Int
        get() {
            val totalDays = max(1, targetDays)
            val dailyExpected = targetPages.toDouble() / totalDays.toDouble()
            return (dailyExpected * daysPassed).toInt().coerceAtMost(targetPages)
        }

    val isOnPace: Boolean
        get() {
            if (isCompleted || pagesReadSoFar >= targetPages) return true
            return pagesReadSoFar >= expectedPagesSoFar
        }

    val progressPercentage: Int
        get() = if (targetPages > 0) {
            ((pagesReadSoFar.toFloat() / targetPages.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else 0
}
