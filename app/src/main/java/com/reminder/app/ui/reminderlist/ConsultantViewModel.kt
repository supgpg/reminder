package com.reminder.app.ui.reminderlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.ReminderApplication
import com.reminder.app.data.PostponeReason
import com.reminder.app.data.Reminder
import com.reminder.app.data.ReminderRepository
import com.reminder.app.network.ConsultantSuggestion
import com.reminder.app.network.PostponeConsultantRepository
import com.reminder.app.notification.AlarmScheduler
import com.reminder.app.ui.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ConsultantStep {
    data object Loading : ConsultantStep
    data class Question(val text: String) : ConsultantStep
    data class Suggestion(val suggestion: ConsultantSuggestion) : ConsultantStep
    data object Done : ConsultantStep
}

data class ConsultantUiState(
    val step: ConsultantStep = ConsultantStep.Loading,
    val reminder: Reminder? = null,
)

class ConsultantViewModel(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val postponeConsultantRepository: PostponeConsultantRepository,
    private val reminderId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsultantUiState())
    val uiState: StateFlow<ConsultantUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val reminder = repository.getById(reminderId)
            if (reminder == null) {
                _uiState.value = _uiState.value.copy(step = ConsultantStep.Done)
                return@launch
            }
            val question = postponeConsultantRepository.getDiagnosisQuestion(reminder)
            _uiState.value = ConsultantUiState(step = ConsultantStep.Question(question), reminder = reminder)
        }
    }

    fun selectReason(reason: PostponeReason) {
        val reminder = _uiState.value.reminder ?: return
        _uiState.value = _uiState.value.copy(step = ConsultantStep.Loading)
        viewModelScope.launch {
            val suggestion = postponeConsultantRepository.getSuggestion(reminder, reason)
            _uiState.value = _uiState.value.copy(step = ConsultantStep.Suggestion(suggestion))
        }
    }

    fun accept() {
        val reminder = _uiState.value.reminder ?: return
        val step = _uiState.value.step
        if (step !is ConsultantStep.Suggestion) return

        viewModelScope.launch {
            val updated = reminder.copy(
                title = step.suggestion.newTitle,
                postponeCount = 0,
                dueAt = System.currentTimeMillis() + TEN_MINUTES_MILLIS,
            )
            val savedId = repository.upsert(updated)
            alarmScheduler.schedule(updated.copy(id = savedId))
            _uiState.value = _uiState.value.copy(step = ConsultantStep.Done)
        }
    }

    fun dismiss() {
        _uiState.value = _uiState.value.copy(step = ConsultantStep.Done)
    }

    companion object {
        const val THRESHOLD = 2
        private const val TEN_MINUTES_MILLIS = 10 * 60 * 1000L

        fun factory(app: ReminderApplication, reminderId: Long) = simpleViewModelFactory {
            ConsultantViewModel(app.repository, app.alarmScheduler, app.postponeConsultantRepository, reminderId)
        }
    }
}
