package com.reminder.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reminder.app.ReminderApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 알림의 "완료" 액션. 할 일을 완료 처리하고 알람/알림을 정리한다.
 */
class DoneActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val app = context.applicationContext as ReminderApplication
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.repository.markDone(reminderId)
                app.alarmScheduler.cancel(reminderId)
                NaggingNotifier(context).cancel(reminderId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
