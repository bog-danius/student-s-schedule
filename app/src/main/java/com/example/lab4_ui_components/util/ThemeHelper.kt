package com.example.lab4_ui_components.util

import androidx.appcompat.app.AppCompatDelegate
import com.example.lab4_ui_components.data.Prefs

object ThemeHelper {
    fun apply(prefs: Prefs) {
        AppCompatDelegate.setDefaultNightMode(
            when {
                prefs.autoTheme -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                prefs.darkTheme -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }
}
