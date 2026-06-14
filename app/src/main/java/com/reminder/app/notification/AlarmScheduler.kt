package com.reminder.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.reminder.app.data.Reminder

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: Reminder, triggerAtMillis: Long = reminder.dueAt) {
        val pendingIntent = buildPendingIntent(reminder.id)
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun scheduleSnooze(reminder: Reminder) {
        schedule(reminder, System.currentTimeMillis() + SNOOZE_DELAY_MILLIS)
    }

    fun cancel(reminderId: Long) {
        alarmManager.cancel(buildPendingIntent(reminderId))
    }

    private fun buildPendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val SNOOZE_DELAY_MILLIS = 10 * 60 * 1000L
    }
}
