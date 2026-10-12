package com.example.lab4_ui_components.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.lab4_ui_components.App
import com.example.lab4_ui_components.MainActivity
import com.example.lab4_ui_components.R
import com.example.lab4_ui_components.data.AppNotification

object Notifier {
    /** Сохраняет уведомление в историю и показывает его в шторке. */
    suspend fun post(context: Context, notifId: Int, title: String, text: String) {
        val app = context.applicationContext as App
        app.database.notificationDao().insert(
            AppNotification(title = title, text = text, timestamp = System.currentTimeMillis())
        )
        show(app, notifId, title, text)
    }

    private fun show(app: App, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val prefs = app.prefs
        // Звук/вибрация задаются каналом и не меняются после создания -> отдельный канал на комбинацию.
        val channelId = "reminders_${prefs.sound}_${prefs.vibration}"
        val nm = app.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(channelId) == null) {
            val ch = NotificationChannel(channelId, "Напоминания", NotificationManager.IMPORTANCE_HIGH)
            if (!prefs.sound) ch.setSound(null, null)
            ch.enableVibration(prefs.vibration)
            nm.createNotificationChannel(ch)
        }

        val intent = Intent(app, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            app, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(app, channelId)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(id, n)
    }
}
