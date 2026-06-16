package com.reminder.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reminder.app.ReminderApplication
import com.reminder.app.data.AppUsageStatsCollector
import com.reminder.app.network.FallbackNagging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 알람 시간이 되면 호출된다. 먼저 폴백 문구로 알림을 즉시 표시하고,
 * 백그라운드에서 Claude API를 호출해 잔소리 문구를 받아오면 알림을 갱신한다.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val app = context.applicationContext as ReminderApplication
        val notifier = NaggingNotifier(context)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = app.repository.getById(reminderId)
                if (reminder == null || reminder.isDone) return@launch

                notifier.show(reminder, FallbackNagging.pick(reminder, app.personaPreferences.spiceLevel.first()))

                val spiceLevel = app.personaPreferences.spiceLevel.first()
                val usageContext = AppUsageStatsCollector.getRecentUsageSummary(context)
                val message = app.naggingRepository.generateMessage(reminder, spiceLevel, usageContext)
                notifier.show(reminder, message)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
