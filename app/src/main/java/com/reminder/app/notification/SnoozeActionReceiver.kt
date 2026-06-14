package com.reminder.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reminder.app.ReminderApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 알림의 "10분 후 다시 알림" 액션. 미룸 횟수를 늘리고 알람을 10분 뒤로 재예약한다.
 * 다음 알람에서는 더 강해진 잔소리가 표시된다.
 */
class SnoozeActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val app = context.applicationContext as ReminderApplication
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newDueAt = System.currentTimeMillis() + AlarmScheduler.SNOOZE_DELAY_MILLIS
                app.repository.postpone(reminderId, newDueAt)

                val reminder = app.repository.getById(reminderId)
                if (reminder != null) {
                    app.alarmScheduler.schedule(reminder, newDueAt)
                }
                NaggingNotifier(context).cancel(reminderId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
