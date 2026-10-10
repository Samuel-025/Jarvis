package com.example.core.phone

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

/** Local reminders stay on-device and are never sent to an AI provider. */
object ReminderScheduler {
    const val EXTRA_TITLE = "com.example.jarvis.REMINDER_TITLE"
    const val EXTRA_ID = "com.example.jarvis.REMINDER_ID"
    private const val CHANNEL_ID = "jarvis_reminders"

    fun schedule(context: Context, delayMillis: Long, title: String): Boolean {
        if (delayMillis <= 0L || delayMillis > 30L * 24L * 60L * 60L * 1000L || title.isBlank()) return false
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val id = (System.currentTimeMillis() xor title.hashCode().toLong()).toInt().let { if (it == 0) 1 else it }
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title.take(240))
            putExtra(EXTRA_ID, id)
        }
        val pending = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMillis, pending)
        return true
    }

    internal fun showNotification(context: Context, id: Int, title: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Jarvis reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders explicitly scheduled in Jarvis"
            })
        }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(context, id, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Jarvis reminder")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(id, notification)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: return
        ReminderScheduler.showNotification(context, intent.getIntExtra(ReminderScheduler.EXTRA_ID, 1), title)
    }
}
