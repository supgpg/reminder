package com.reminder.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.reminder.app.ReminderApplication
import com.reminder.app.data.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskSuggestionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(TaskSuggestionNotifier.EXTRA_NOTIF_ID, -1)
        if (notifId != -1) NotificationManagerCompat.from(context).cancel(notifId)

        if (intent.action != TaskSuggestionNotifier.ACTION_ADD) return

        val title = intent.getStringExtra(TaskSuggestionNotifier.EXTRA_TITLE) ?: return
        val dueAt = intent.getLongExtra(TaskSuggestionNotifier.EXTRA_DUE_AT, System.currentTimeMillis())
        val desc = intent.getStringExtra(TaskSuggestionNotifier.EXTRA_DESC) ?: ""

        val app = context.applicationContext as ReminderApplication
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = Reminder(title = title, description = desc, dueAt = dueAt)
                val id = app.repository.upsert(reminder)
                app.alarmScheduler.schedule(reminder.copy(id = id))
            } finally {
                pending.finish()
            }
        }
    }
}
