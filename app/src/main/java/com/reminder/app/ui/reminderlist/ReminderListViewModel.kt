package com.reminder.app.ui.reminderlist

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.ReminderApplication
import com.reminder.app.calendar.CalendarSyncManager
import com.reminder.app.data.Reminder
import com.reminder.app.data.ReminderRepository
import com.reminder.app.notification.AlarmScheduler
import com.reminder.app.notification.NaggingNotifier
import com.reminder.app.ui.simpleViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderListViewModel(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val notifier: NaggingNotifier,
    private val calendarSyncManager: CalendarSyncManager,
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val calendarObserver: ContentObserver = calendarSyncManager.registerObserver(
        Handler(Looper.getMainLooper()),
    ) {
        viewModelScope.launch { repository.refreshCalendarSync() }
    }

    init {
        viewModelScope.launch { repository.refreshCalendarSync() }
    }

    fun refresh() {
        viewModelScope.launch { repository.refreshCalendarSync() }
    }

    fun markDone(reminder: Reminder) {
        viewModelScope.launch {
            repository.markDone(reminder.id)
            alarmScheduler.cancel(reminder.id)
            notifier.cancel(reminder.id)
        }
    }

    fun delete(reminder: Reminder) {
        viewModelScope.launch {
            repository.delete(reminder)
            alarmScheduler.cancel(reminder.id)
            notifier.cancel(reminder.id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        calendarSyncManager.unregisterObserver(calendarObserver)
    }

    companion object {
        fun factory(app: ReminderApplication) = simpleViewModelFactory {
            ReminderListViewModel(app.repository, app.alarmScheduler, NaggingNotifier(app), app.calendarSyncManager)
        }
    }
}
