package com.example.lab4_ui_components.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE date = :day ORDER BY startMinutes")
    fun observeByDate(day: Long): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun getById(id: Long): Lesson?

    @Query("SELECT * FROM lessons WHERE date BETWEEN :from AND :to ORDER BY date, startMinutes")
    suspend fun getBetween(from: Long, to: Long): List<Lesson>

    @Insert
    suspend fun insertAll(items: List<Lesson>)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY done, dueAt")
    fun observeAll(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<AppNotification>>

    @Insert
    suspend fun insert(item: AppNotification)

    @Query("DELETE FROM notifications")
    suspend fun clear()
}

@Dao
interface NewsDao {
    @Query("SELECT * FROM news ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<News>>

    @Insert
    suspend fun insertAll(items: List<News>)
}
