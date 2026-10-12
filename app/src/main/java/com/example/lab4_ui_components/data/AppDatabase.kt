package com.example.lab4_ui_components.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Lesson::class, Task::class, AppNotification::class, News::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao
    abstract fun taskDao(): TaskDao
    abstract fun notificationDao(): NotificationDao
    abstract fun newsDao(): NewsDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "student_schedule.db"
            ).build().also { instance = it }
        }
    }
}
