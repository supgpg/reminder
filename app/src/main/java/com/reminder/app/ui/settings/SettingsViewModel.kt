package com.reminder.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.ReminderApplication
import com.reminder.app.calendar.CalendarInfo
import com.reminder.app.calendar.CalendarSyncManager
import com.reminder.app.data.AppUsageStatsCollector
import com.reminder.app.data.PersonaPreferences
import com.reminder.app.data.ReminderRepository
import com.reminder.app.data.SpiceLevel
import com.reminder.app.ui.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val personaPreferences: PersonaPreferences,
    private val repository: ReminderRepository,
    private val calendarSyncManager: CalendarSyncManager,
    private val appContext: Context,
) : ViewModel() {

    val spiceLevel: StateFlow<SpiceLevel> = personaPreferences.spiceLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SpiceLevel.SPICY)

    val calendarSyncEnabled: StateFlow<Boolean> = personaPreferences.calendarSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val selectedCalendarIds: StateFlow<Set<Long>> = personaPreferences.selectedCalendarIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _availableCalendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
    val availableCalendars: StateFlow<List<CalendarInfo>> = _availableCalendars.asStateFlow()

    fun setSpiceLevel(level: SpiceLevel) {
        viewModelScope.launch {
            personaPreferences.setSpiceLevel(level)
        }
    }

    fun loadCalendars() {
        _availableCalendars.value = calendarSyncManager.listCalendars()
    }

    fun enableCalendarSync() {
        viewModelScope.launch {
            personaPreferences.setCalendarSyncEnabled(true)
            loadCalendars()
            repository.refreshCalendarSync()
        }
    }

    fun setCalendarSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            personaPreferences.setCalendarSyncEnabled(enabled)
            if (enabled) {
                loadCalendars()
                repository.refreshCalendarSync()
            }
        }
    }

    fun toggleCalendar(id: Long, selected: Boolean) {
        viewModelScope.launch {
            val current = selectedCalendarIds.value
            val updated = if (selected) current + id else current - id
            personaPreferences.setSelectedCalendarIds(updated)
            repository.refreshCalendarSync()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshCalendarSync()
        }
    }

    private val _notificationListenerGranted = MutableStateFlow(false)
    val notificationListenerGranted: StateFlow<Boolean> = _notificationListenerGranted.asStateFlow()

    val notificationMiningEnabled: StateFlow<Boolean> = personaPreferences.notificationMiningEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun refreshListenerStatus(granted: Boolean) {
        _notificationListenerGranted.value = granted
    }

    fun setNotificationMiningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            personaPreferences.setNotificationMiningEnabled(enabled)
        }
    }

    private val _usageStatsGranted = MutableStateFlow(false)
    val usageStatsGranted: StateFlow<Boolean> = _usageStatsGranted.asStateFlow()

    fun refreshUsageStatsStatus() {
        _usageStatsGranted.value = AppUsageStatsCollector.hasPermission(appContext)
    }

    companion object {
        fun factory(app: ReminderApplication) = simpleViewModelFactory {
            SettingsViewModel(app.personaPreferences, app.repository, app.calendarSyncManager, app)
        }
    }
}
