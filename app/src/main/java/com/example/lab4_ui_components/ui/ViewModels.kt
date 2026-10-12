package com.example.lab4_ui_components.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.lab4_ui_components.App
import com.example.lab4_ui_components.data.Lesson
import com.example.lab4_ui_components.data.Task
import kotlinx.coroutines.launch
import java.time.LocalDate

class ScheduleViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as App).repository

    val today = MutableLiveData(LocalDate.now())
    val homeDate = MutableLiveData(LocalDate.now())
    val scheduleDate = MutableLiveData(LocalDate.now())

    val todayLessons: LiveData<List<Lesson>> = today.switchMap { repo.lessonsOn(it).asLiveData() }
    val homeLessons: LiveData<List<Lesson>> = homeDate.switchMap { repo.lessonsOn(it).asLiveData() }
    val scheduleLessons: LiveData<List<Lesson>> = scheduleDate.switchMap { repo.lessonsOn(it).asLiveData() }

    /** Вызывается из onResume: если наступил новый день — сдвигаем «сегодня». */
    fun refreshToday() {
        val now = LocalDate.now()
        val old = today.value
        if (old != now) {
            today.value = now
            if (homeDate.value == old) homeDate.value = now
            if (scheduleDate.value == old) scheduleDate.value = now
        }
    }
}

sealed class TaskRow {
    data class Header(val title: String) : TaskRow()
    data class Item(val task: Task) : TaskRow()
}

class TasksViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as App).repository

    val rows: LiveData<List<TaskRow>> = repo.tasks.asLiveData().map { buildRows(it) }

    fun toggle(task: Task) {
        viewModelScope.launch { repo.setDone(task, !task.done) }
    }

    fun delete(task: Task) {
        viewModelScope.launch { repo.deleteTask(task) }
    }

    private fun buildRows(tasks: List<Task>): List<TaskRow> {
        val today = LocalDate.now()
        fun dateOf(t: Task) = java.time.Instant.ofEpochMilli(t.dueAt)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

        val active = tasks.filter { !it.done }.sortedBy { it.dueAt }
        val done = tasks.filter { it.done }.sortedByDescending { it.dueAt }

        val groups = listOf(
            "Просрочено" to active.filter { dateOf(it).isBefore(today) },
            "Сегодня" to active.filter { dateOf(it) == today },
            "Завтра" to active.filter { dateOf(it) == today.plusDays(1) },
            "Позже" to active.filter { dateOf(it).isAfter(today.plusDays(1)) },
            "Выполнено" to done
        )
        val result = mutableListOf<TaskRow>()
        for ((title, list) in groups) {
            if (list.isEmpty()) continue
            result += TaskRow.Header(title)
            list.forEach { result += TaskRow.Item(it) }
        }
        return result
    }
}
