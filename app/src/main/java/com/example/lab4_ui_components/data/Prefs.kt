package com.example.lab4_ui_components.data

import android.content.Context
import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private class BoolPref(val sp: SharedPreferences, val key: String, val def: Boolean) :
    ReadWriteProperty<Any?, Boolean> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = sp.getBoolean(key, def)
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) =
        sp.edit().putBoolean(key, value).apply()
}

private class StrPref(val sp: SharedPreferences, val key: String, val def: String) :
    ReadWriteProperty<Any?, String> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = sp.getString(key, def) ?: def
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) =
        sp.edit().putString(key, value).apply()
}

private class LongPref(val sp: SharedPreferences, val key: String, val def: Long) :
    ReadWriteProperty<Any?, Long> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = sp.getLong(key, def)
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) =
        sp.edit().putLong(key, value).apply()
}

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var darkTheme by BoolPref(sp, "dark", false)
    var autoTheme by BoolPref(sp, "auto_theme", true)
    var lessonReminders by BoolPref(sp, "lesson_reminders", true)
    var taskReminders by BoolPref(sp, "task_reminders", true)
    var sound by BoolPref(sp, "sound", true)
    var vibration by BoolPref(sp, "vibration", true)

    var profileName by StrPref(sp, "profile_name", "Архипцев Богдан")
    var group by StrPref(sp, "profile_group", "АСОИР-231")
    var course by StrPref(sp, "profile_course", "3 курс")
    var specialty by StrPref(sp, "profile_specialty", "ПОИТ")

    var seeded by BoolPref(sp, "seeded", false)
    var lastSeededDay by LongPref(sp, "last_seeded_day", -1L)
    var lastSync by LongPref(sp, "last_sync", 0L)

    /** Имя для приветствия: "Фамилия Имя" -> "Имя". */
    fun firstName(): String {
        val parts = profileName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            parts.size >= 2 -> parts[1]
            parts.size == 1 -> parts[0]
            else -> "студент"
        }
    }

    fun initials(): String {
        val parts = profileName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val s = parts.take(2).joinToString("") { it.first().uppercase() }
        return s.ifEmpty { "?" }
    }
}
