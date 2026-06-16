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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val syncExceptionHandler = CoroutineExceptionHandler { _, _ -> /* ignore sync errors */ }

    private var calendarObserver: ContentObserver? = null
    private var observerDebounceJob: Job? = null

    init {
        // ContentObserver only imports from calendar (never writes back) to avoid an infinite loop:
        // writing to calendar → observer fires → sync writes again → observer fires → ...
        calendarObserver = calendarSyncManager.registerObserver(Handler(Looper.getMainLooper())) {
            observerDebounceJob?.cancel()
            observerDebounceJob = viewModelScope.launch(syncExceptionHandler) {
                delay(2_000)
                repository.importFromCalendar()
            }
        }
        viewModelScope.launch(syncExceptionHandler) { repository.refreshCalendarSync() }
    }

    fun refresh() {
        viewModelScope.launch(syncExceptionHandler) { repository.refreshCalendarSync() }
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
        calendarObserver?.let { calendarSyncManager.unregisterObserver(it) }
    }

    companion object {
        fun factory(app: ReminderApplication) = simpleViewModelFactory {
            ReminderListViewModel(app.repository, app.alarmScheduler, NaggingNotifier(app), app.calendarSyncManager)
        }
    }
}
