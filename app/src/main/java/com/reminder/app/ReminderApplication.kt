package com.reminder.app

import android.app.Application
import com.reminder.app.calendar.CalendarSyncManager
import com.reminder.app.data.PersonaPreferences
import com.reminder.app.data.ReminderDatabase
import com.reminder.app.data.ReminderRepository
import com.reminder.app.network.NaggingRepository
import com.reminder.app.network.PostponeConsultantRepository
import com.reminder.app.network.SmartScheduleRepository
import com.reminder.app.network.TaskDecompositionRepository
import com.reminder.app.notification.AlarmScheduler
import com.reminder.app.notification.NaggingNotifier

class ReminderApplication : Application() {

    val personaPreferences: PersonaPreferences by lazy { PersonaPreferences(this) }

    val calendarSyncManager: CalendarSyncManager by lazy { CalendarSyncManager(this) }

    val repository: ReminderRepository by lazy {
        ReminderRepository(
            ReminderDatabase.getInstance(this).reminderDao(),
            calendarSyncManager,
            personaPreferences,
        )
    }

    val naggingRepository: NaggingRepository by lazy { NaggingRepository() }

    val taskDecompositionRepository: TaskDecompositionRepository by lazy { TaskDecompositionRepository() }

    val postponeConsultantRepository: PostponeConsultantRepository by lazy { PostponeConsultantRepository() }

    val smartScheduleRepository: SmartScheduleRepository by lazy { SmartScheduleRepository() }

    val alarmScheduler: AlarmScheduler by lazy { AlarmScheduler(this) }

    override fun onCreate() {
        super.onCreate()
        // Pre-create the notification channel so it exists before any alarm fires.
        NaggingNotifier(this)
    }
}
