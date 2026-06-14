package com.reminder.app.ui.smartadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.ReminderApplication
import com.reminder.app.network.SmartScheduleRepository
import com.reminder.app.ui.AddEditPrefill
import com.reminder.app.ui.addedit.AddEditUiState
import com.reminder.app.ui.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SmartAddUiState(
    val input: String = "",
    val isLoading: Boolean = false,
    val showFallbackMessage: Boolean = false,
    val navigateToAddEdit: Boolean = false,
)

class SmartAddViewModel(
    private val smartScheduleRepository: SmartScheduleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartAddUiState())
    val uiState: StateFlow<SmartAddUiState> = _uiState.asStateFlow()

    fun updateInput(input: String) {
        _uiState.value = _uiState.value.copy(input = input)
    }

    fun analyze() {
        val input = _uiState.value.input.trim()
        if (input.isBlank()) return

        _uiState.value = _uiState.value.copy(isLoading = true, showFallbackMessage = false)
        viewModelScope.launch {
            val result = smartScheduleRepository.parse(input)
            if (result != null) {
                AddEditPrefill.set(title = result.title, description = result.description, dueAt = result.dueAt)
                _uiState.value = _uiState.value.copy(isLoading = false, navigateToAddEdit = true)
            } else {
                AddEditPrefill.set(title = input, description = "", dueAt = AddEditUiState.defaultDueAt())
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showFallbackMessage = true,
                    navigateToAddEdit = true,
                )
            }
        }
    }

    companion object {
        fun factory(app: ReminderApplication) = simpleViewModelFactory {
            SmartAddViewModel(app.smartScheduleRepository)
        }
    }
}
