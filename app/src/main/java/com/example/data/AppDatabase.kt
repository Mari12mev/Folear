package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AnnotationDao
import com.example.data.dao.BookDao
import com.example.data.dao.BookmarkDao
import com.example.data.dao.FolderDao
import com.example.data.dao.ReadingGoalDao
import com.example.data.dao.ReadingSessionDao
import com.example.data.dao.TagDao
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookEntity
import com.example.data.model.BookFolderCrossRef
import com.example.data.model.BookTagCrossRef
import com.example.data.model.BookmarkEntity
import com.example.data.model.FolderEntity
import com.example.data.model.ReadingGoalEntity
import com.example.data.model.ReadingSessionEntity
import com.example.data.model.TagEntity

@Database(
    entities = [
        BookEntity::class,
        FolderEntity::class,
        BookFolderCrossRef::class,
        ReadingGoalEntity::class,
        TagEntity::class,
        BookTagCrossRef::class,
        BookmarkEntity::class,
        AnnotationEntity::class,
        ReadingSessionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun folderDao(): FolderDao
    abstract fun readingGoalDao(): ReadingGoalDao
    abstract fun tagDao(): TagDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun readingSessionDao(): ReadingSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "folhear_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
