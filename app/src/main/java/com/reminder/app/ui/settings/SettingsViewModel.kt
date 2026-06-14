package com.reminder.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.ReminderApplication
import com.reminder.app.data.PersonaPreferences
import com.reminder.app.data.ReminderRepository
import com.reminder.app.data.SpiceLevel
import com.reminder.app.ui.simpleViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val personaPreferences: PersonaPreferences,
    private val repository: ReminderRepository,
) : ViewModel() {

    val spiceLevel: StateFlow<SpiceLevel> = personaPreferences.spiceLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SpiceLevel.SPICY)

    val calendarSyncEnabled: StateFlow<Boolean> = personaPreferences.calendarSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setSpiceLevel(level: SpiceLevel) {
        viewModelScope.launch {
            personaPreferences.setSpiceLevel(level)
        }
    }

    fun enableCalendarSync() {
        viewModelScope.launch {
            personaPreferences.setCalendarSyncEnabled(true)
            repository.syncAllToCalendar()
        }
    }

    fun setCalendarSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            personaPreferences.setCalendarSyncEnabled(enabled)
            if (enabled) {
                repository.syncAllToCalendar()
            }
        }
    }

    companion object {
        fun factory(app: ReminderApplication) = simpleViewModelFactory {
            SettingsViewModel(app.personaPreferences, app.repository)
        }
    }
}
