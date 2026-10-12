package com.example.lab4_ui_components.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.lab4_ui_components.App
import com.example.lab4_ui_components.data.Task
import com.example.lab4_ui_components.util.toMillis
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val K_KIND = "kind"
    const val K_ID = "id"
    const val K_TITLE = "title"
    const val K_AT = "at"
    const val K_TEXT = "text"
    const val KIND_LESSON = "lesson"
    const val KIND_TASK = "task"

    private const val LEAD_MIN = 15L

    fun scheduleTask(context: Context, task: Task) {
        val wm = WorkManager.getInstance(context)
        val name = "task_${task.id}"
        wm.cancelUniqueWork(name)
        if (!task.remind || task.done) return
        val now = System.currentTimeMillis()
        if (task.dueAt <= now) return
        val delay = maxOf(0L, task.dueAt - LEAD_MIN * 60_000 - now)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(K_KIND to KIND_TASK, K_ID to task.id, K_TITLE to task.title, K_AT to task.dueAt)
            )
            .build()
        wm.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelTask(context: Context, id: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("task_$id")
    }

    /** Планирует напоминания на занятия ближайших 7 дней (включение/выключение проверяется в момент показа). */
    suspend fun rescheduleLessons(context: Context) {
        val app = context.applicationContext as App
        val today = LocalDate.now()
        val lessons = app.database.lessonDao().getBetween(today.toEpochDay(), today.plusDays(7).toEpochDay())
        val wm = WorkManager.getInstance(app)
        val now = System.currentTimeMillis()
        for (l in lessons) {
            val start = LocalDateTime.of(
                LocalDate.ofEpochDay(l.date), LocalTime.of(l.startMinutes / 60, l.startMinutes % 60)
            ).toMillis()
            val delay = start - LEAD_MIN * 60_000 - now
            if (delay <= 0) continue
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        K_KIND to KIND_LESSON, K_ID to l.id, K_TITLE to l.title,
                        K_AT to start, K_TEXT to com.example.lab4_ui_components.util.Fmt.room(l.room)
                    )
                )
                .build()
            wm.enqueueUniqueWork("lesson_${l.id}", ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun scheduleDailyRefresh(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyRefreshWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork("daily_refresh", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
