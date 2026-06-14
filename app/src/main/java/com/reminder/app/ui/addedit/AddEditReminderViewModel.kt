package com.reminder.app.ui.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.ReminderApplication
import com.reminder.app.data.Reminder
import com.reminder.app.data.ReminderRepository
import com.reminder.app.notification.AlarmScheduler
import com.reminder.app.ui.AddEditPrefill
import com.reminder.app.ui.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class AddEditUiState(
    val id: Long = 0L,
    val title: String = "",
    val description: String = "",
    val dueAt: Long = defaultDueAt(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
) {
    companion object {
        fun defaultDueAt(): Long {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.HOUR_OF_DAY, 1)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }
    }
}

class AddEditReminderViewModel(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val reminderId: Long?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState(isLoading = reminderId != null))
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        if (reminderId != null) {
            viewModelScope.launch {
                repository.getById(reminderId)?.let { reminder ->
                    _uiState.value = AddEditUiState(
                        id = reminder.id,
                        title = reminder.title,
                        description = reminder.description,
                        dueAt = reminder.dueAt,
                        isLoading = false,
                    )
                } ?: run { _uiState.value = _uiState.value.copy(isLoading = false) }
            }
        } else {
            val prefill = AddEditPrefill.consume()
            _uiState.value = if (prefill != null) {
                val (title, description, dueAt) = prefill
                _uiState.value.copy(title = title, description = description, dueAt = dueAt, isLoading = false)
            } else {
                _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateDueAt(dueAt: Long) {
        _uiState.value = _uiState.value.copy(dueAt = dueAt)
    }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) return

        viewModelScope.launch {
            val reminder = Reminder(
                id = state.id,
                title = state.title.trim(),
                description = state.description.trim(),
                dueAt = state.dueAt,
            )
            val savedId = repository.upsert(reminder)
            alarmScheduler.schedule(reminder.copy(id = savedId))
            _uiState.value = state.copy(isSaved = true)
        }
    }

    fun delete() {
        val state = _uiState.value
        if (state.id == 0L) return

        viewModelScope.launch {
            repository.getById(state.id)?.let { repository.delete(it) }
            alarmScheduler.cancel(state.id)
            _uiState.value = state.copy(isSaved = true)
        }
    }

    companion object {
        fun factory(app: ReminderApplication, reminderId: Long?) = simpleViewModelFactory {
            AddEditReminderViewModel(app.repository, app.alarmScheduler, reminderId)
        }
    }
}
