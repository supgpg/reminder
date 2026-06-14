package com.reminder.app.ui.reminderlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.ReminderApplication
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
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    companion object {
        fun factory(app: ReminderApplication) = simpleViewModelFactory {
            ReminderListViewModel(app.repository, app.alarmScheduler, NaggingNotifier(app))
        }
    }
}
