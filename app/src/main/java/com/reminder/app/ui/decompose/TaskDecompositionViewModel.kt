package com.reminder.app.ui.decompose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.ReminderApplication
import com.reminder.app.data.Reminder
import com.reminder.app.data.ReminderRepository
import com.reminder.app.network.TaskDecompositionRepository
import com.reminder.app.notification.AlarmScheduler
import com.reminder.app.ui.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SubtaskItem(val text: String, val selected: Boolean = true)

data class TaskDecompositionUiState(
    val goal: String = "",
    val isLoading: Boolean = false,
    val subtasks: List<SubtaskItem> = emptyList(),
    val isDone: Boolean = false,
)

class TaskDecompositionViewModel(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val taskDecompositionRepository: TaskDecompositionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskDecompositionUiState())
    val uiState: StateFlow<TaskDecompositionUiState> = _uiState.asStateFlow()

    fun updateGoal(goal: String) {
        _uiState.value = _uiState.value.copy(goal = goal)
    }

    fun analyze() {
        val goal = _uiState.value.goal.trim()
        if (goal.isBlank()) return

        _uiState.value = _uiState.value.copy(isLoading = true, subtasks = emptyList())
        viewModelScope.launch {
            val subtasks = taskDecompositionRepository.decompose(goal)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                subtasks = subtasks.map { SubtaskItem(text = it) },
            )
        }
    }

    fun toggle(index: Int) {
        val subtasks = _uiState.value.subtasks.toMutableList()
        val item = subtasks.getOrNull(index) ?: return
        subtasks[index] = item.copy(selected = !item.selected)
        _uiState.value = _uiState.value.copy(subtasks = subtasks)
    }

    fun updateText(index: Int, text: String) {
        val subtasks = _uiState.value.subtasks.toMutableList()
        val item = subtasks.getOrNull(index) ?: return
        subtasks[index] = item.copy(text = text)
        _uiState.value = _uiState.value.copy(subtasks = subtasks)
    }

    fun confirm() {
        val selected = _uiState.value.subtasks.filter { it.selected && it.text.isNotBlank() }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            selected.forEachIndexed { index, item ->
                val reminder = Reminder(
                    title = item.text.trim(),
                    dueAt = now + (index + 1) * ONE_HOUR_MILLIS,
                )
                val savedId = repository.upsert(reminder)
                alarmScheduler.schedule(reminder.copy(id = savedId))
            }
            _uiState.value = _uiState.value.copy(isDone = true)
        }
    }

    companion object {
        private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L

        fun factory(app: ReminderApplication) = simpleViewModelFactory {
            TaskDecompositionViewModel(app.repository, app.alarmScheduler, app.taskDecompositionRepository)
        }
    }
}
