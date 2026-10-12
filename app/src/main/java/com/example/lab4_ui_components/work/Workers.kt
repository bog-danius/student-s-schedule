package com.example.lab4_ui_components.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.lab4_ui_components.App
import kotlin.math.max
import kotlin.math.roundToInt

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val kind = inputData.getString(ReminderScheduler.K_KIND) ?: return Result.success()
        val title = inputData.getString(ReminderScheduler.K_TITLE) ?: ""
        val id = inputData.getLong(ReminderScheduler.K_ID, 0L)
        val at = inputData.getLong(ReminderScheduler.K_AT, 0L)
        val minutes = max(0, ((at - System.currentTimeMillis()) / 60_000.0).roundToInt())

        when (kind) {
            ReminderScheduler.KIND_LESSON -> {
                if (!app.prefs.lessonReminders) return Result.success()
                val room = inputData.getString(ReminderScheduler.K_TEXT) ?: ""
                Notifier.post(app, (id * 2).toInt(), title, "Через $minutes мин • $room")
            }
            ReminderScheduler.KIND_TASK -> {
                if (!app.prefs.taskReminders) return Result.success()
                val task = app.database.taskDao().getById(id)
                if (task == null || task.done) return Result.success()
                val text = if (minutes == 0) "Срок задачи наступил" else "Срок через $minutes мин"
                Notifier.post(app, (id * 2 + 1).toInt(), title, text)
            }
        }
        return Result.success()
    }
}

class DailyRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as App
        app.repository.ensureSeed()
        ReminderScheduler.rescheduleLessons(app)
        return Result.success()
    }
}
