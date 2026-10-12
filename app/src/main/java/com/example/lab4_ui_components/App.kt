package com.example.lab4_ui_components

import android.app.Application
import com.example.lab4_ui_components.data.AppDatabase
import com.example.lab4_ui_components.data.Prefs
import com.example.lab4_ui_components.data.Repository
import com.example.lab4_ui_components.util.ThemeHelper
import com.example.lab4_ui_components.work.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {
    lateinit var prefs: Prefs
        private set
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: Repository by lazy { Repository(this, database, prefs) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        ThemeHelper.apply(prefs)
        appScope.launch {
            repository.ensureSeed()
            ReminderScheduler.rescheduleLessons(this@App)
        }
        ReminderScheduler.scheduleDailyRefresh(this)
    }
}
