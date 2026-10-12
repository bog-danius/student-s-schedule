package com.example.lab4_ui_components.data

import android.content.Context
import com.example.lab4_ui_components.util.toMillis
import com.example.lab4_ui_components.work.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class Repository(
    private val context: Context,
    private val db: AppDatabase,
    private val prefs: Prefs
) {
    private val seedMutex = Mutex()

    val tasks: Flow<List<Task>> = db.taskDao().observeAll()
    val news: Flow<List<News>> = db.newsDao().observeAll()
    val notifications: Flow<List<AppNotification>> = db.notificationDao().observeAll()

    fun lessonsOn(date: LocalDate): Flow<List<Lesson>> = db.lessonDao().observeByDate(date.toEpochDay())
    suspend fun lesson(id: Long): Lesson? = db.lessonDao().getById(id)
    suspend fun task(id: Long): Task? = db.taskDao().getById(id)

    suspend fun saveTask(task: Task): Long {
        val dao = db.taskDao()
        val id = if (task.id == 0L) dao.insert(task) else { dao.update(task); task.id }
        ReminderScheduler.scheduleTask(context, task.copy(id = id))
        return id
    }

    suspend fun setDone(task: Task, done: Boolean) {
        val updated = task.copy(done = done)
        db.taskDao().update(updated)
        ReminderScheduler.scheduleTask(context, updated) // отменит напоминание, если done
    }

    suspend fun deleteTask(task: Task) {
        db.taskDao().delete(task)
        ReminderScheduler.cancelTask(context, task.id)
    }

    suspend fun clearNotifications() = db.notificationDao().clear()

    /** Первичное наполнение и подгрузка расписания вперёд. */
    suspend fun ensureSeed() = seedMutex.withLock {
        if (!prefs.seeded) {
            val today = LocalDate.now()
            fun at(d: LocalDate, h: Int) = LocalDateTime.of(d, LocalTime.of(h, 0)).toMillis()
            listOf(
                Task(title = "Лабораторная работа №5", description = "Реализовать RecyclerView и обработку кликов.", dueAt = at(today, 18)),
                Task(title = "Подготовить отчёт", description = "Отчёт по лабораторной работе.", dueAt = at(today, 12), done = true),
                Task(title = "Подготовиться к БД", description = "Повторить нормализацию и SQL-запросы.", dueAt = at(today.plusDays(1), 14)),
                Task(title = "Повторить Kotlin", description = "Коллекции, лямбды, корутины.", dueAt = at(today.plusDays(1), 19))
            ).forEach { saveTask(it) }

            db.newsDao().insertAll(
                listOf(
                    News(title = "Опубликовано новое расписание", text = "Проверьте изменения занятий на следующую учебную неделю.", date = today.toEpochDay()),
                    News(title = "Изменение кабинета", text = "Занятие «Базы данных» перенесено в кабинет 215.", date = today.minusDays(1).toEpochDay()),
                    News(title = "Напоминания", text = "Теперь приложение может напоминать о занятиях и задачах.", date = today.minusDays(2).toEpochDay())
                )
            )
            prefs.seeded = true
        }
        topUpLessons()
    }

    suspend fun sync() {
        ensureSeed()
        ReminderScheduler.rescheduleLessons(context)
        prefs.lastSync = System.currentTimeMillis()
    }

    private suspend fun topUpLessons() {
        val today = LocalDate.now()
        val end = today.plusDays(28).toEpochDay()
        val from = if (prefs.lastSeededDay < 0) today.minusDays(7).toEpochDay() else prefs.lastSeededDay + 1
        if (from > end) return
        val list = mutableListOf<Lesson>()
        for (day in from..end) list += LessonTemplates.forDay(LocalDate.ofEpochDay(day))
        db.lessonDao().insertAll(list)
        prefs.lastSeededDay = end
    }
}
