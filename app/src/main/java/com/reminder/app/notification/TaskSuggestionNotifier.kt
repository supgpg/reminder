package com.reminder.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.reminder.app.R
import java.util.concurrent.atomic.AtomicInteger

class TaskSuggestionNotifier(private val context: Context) {

    init { createChannel() }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_suggestion_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notif_suggestion_channel_desc)
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }

    fun suggest(title: String, dueAt: Long, description: String) {
        val id = counter.incrementAndGet()

        val addPi = PendingIntent.getBroadcast(
            context, id,
            Intent(context, TaskSuggestionReceiver::class.java).apply {
                action = ACTION_ADD
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_DUE_AT, dueAt)
                putExtra(EXTRA_DESC, description)
                putExtra(EXTRA_NOTIF_ID, id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismissPi = PendingIntent.getBroadcast(
            context, id + 10_000,
            Intent(context, TaskSuggestionReceiver::class.java).apply {
                action = ACTION_DISMISS
                putExtra(EXTRA_NOTIF_ID, id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val bodyText = if (description.isNotBlank()) "$title\n$description" else title
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_task_detected))
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.notif_task_add), addPi)
            .addAction(0, context.getString(R.string.notif_task_dismiss), dismissPi)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "task_suggestions"
        const val ACTION_ADD = "com.reminder.app.ADD_SUGGESTED_TASK"
        const val ACTION_DISMISS = "com.reminder.app.DISMISS_SUGGESTED_TASK"
        const val EXTRA_TITLE = "extra_sug_title"
        const val EXTRA_DUE_AT = "extra_sug_due_at"
        const val EXTRA_DESC = "extra_sug_desc"
        const val EXTRA_NOTIF_ID = "extra_sug_notif_id"
        private val counter = AtomicInteger(50_000)
    }
}
