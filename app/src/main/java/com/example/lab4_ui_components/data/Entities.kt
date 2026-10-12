package com.example.lab4_ui_components.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "lessons", indices = [Index("date")])
data class Lesson(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String,
    val room: String,
    val teacher: String,
    val description: String,
    val date: Long,          // LocalDate.toEpochDay()
    val startMinutes: Int,   // минуты от начала суток
    val endMinutes: Int
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val dueAt: Long,         // epoch millis
    val done: Boolean = false,
    val remind: Boolean = true
)

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val text: String,
    val timestamp: Long
)

@Entity(tableName = "news")
data class News(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val text: String,
    val date: Long           // epoch day
)
