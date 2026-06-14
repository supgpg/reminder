package com.reminder.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.ReminderApplication
import com.reminder.app.data.PersonaPreferences
import com.reminder.app.data.SpiceLevel
import com.reminder.app.ui.simpleViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val personaPreferences: PersonaPreferences) : ViewModel() {

    val spiceLevel: StateFlow<SpiceLevel> = personaPreferences.spiceLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SpiceLevel.SPICY)

    fun setSpiceLevel(level: SpiceLevel) {
        viewModelScope.launch {
            personaPreferences.setSpiceLevel(level)
        }
    }

    companion object {
        fun factory(app: ReminderApplication) = simpleViewModelFactory {
            SettingsViewModel(app.personaPreferences)
        }
    }
}
