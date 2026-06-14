package com.reminder.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.reminder.app.MainActivity
import com.reminder.app.R
import com.reminder.app.data.Reminder

class NaggingNotifier(private val context: Context) {

    init {
        createChannelIfNeeded()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun show(reminder: Reminder, message: String) {
        val contentIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id * 10 + 1).toInt(),
            Intent(context, SnoozeActionReceiver::class.java).apply {
                putExtra(AlarmScheduler.EXTRA_REMINDER_ID, reminder.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val doneIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id * 10 + 2).toInt(),
            Intent(context, DoneActionReceiver::class.java).apply {
                putExtra(AlarmScheduler.EXTRA_REMINDER_ID, reminder.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.action_done), doneIntent)
            .addAction(0, context.getString(R.string.action_snooze), snoozeIntent)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(reminder.id.toInt(), notification)
        }
    }

    fun cancel(reminderId: Long) {
        NotificationManagerCompat.from(context).cancel(reminderId.toInt())
    }

    companion object {
        const val CHANNEL_ID = "nagging_reminders"
    }
}
