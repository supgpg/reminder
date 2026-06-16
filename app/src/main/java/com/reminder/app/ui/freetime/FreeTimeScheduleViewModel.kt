package com.reminder.app.ui.freetime

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.ReminderApplication
import com.reminder.app.calendar.CalendarFreeTimeAnalyzer
import com.reminder.app.network.FreeTimeScheduleRepository
import com.reminder.app.network.ScheduleSuggestion
import com.reminder.app.notification.AlarmScheduler
import com.reminder.app.ui.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SuggestionItem(
    val suggestion: ScheduleSuggestion,
    val selected: Boolean = true,
)

data class FreeTimeScheduleUiState(
    val isLoading: Boolean = false,
    val suggestions: List<SuggestionItem> = emptyList(),
    val isDone: Boolean = false,
    val noCalendarPermission: Boolean = false,
    val analyzed: Boolean = false,
)

class FreeTimeScheduleViewModel(
    private val app: ReminderApplication,
    private val freeTimeRepo: FreeTimeScheduleRepository,
    private val alarmScheduler: AlarmScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FreeTimeScheduleUiState())
    val uiState: StateFlow<FreeTimeScheduleUiState> = _uiState.asStateFlow()

    fun analyze(context: Context) {
        _uiState.update { it.copy(isLoading = true, analyzed = false) }
        viewModelScope.launch {
            if (!CalendarFreeTimeAnalyzer.hasPermission(context)) {
                _uiState.update { it.copy(isLoading = false, noCalendarPermission = true) }
                return@launch
            }

            val now = System.currentTimeMillis()
            val (from, to) = CalendarFreeTimeAnalyzer.todayWindowMillis()
            val busyBlocks = CalendarFreeTimeAnalyzer.queryBusyBlocks(context, from, to)
            val busyText = CalendarFreeTimeAnalyzer.formatBusyBlocksForPrompt(busyBlocks, now)
            val nowFormatted = CalendarFreeTimeAnalyzer.nowFormatted(now)

            val pending = app.repository.getPendingReminders().take(10)

            val suggestions = freeTimeRepo.suggest(pending, busyText, nowFormatted)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    suggestions = suggestions.map { s -> SuggestionItem(s) },
                    analyzed = true,
                )
            }
        }
    }

    fun toggle(index: Int) {
        _uiState.update { state ->
            val updated = state.suggestions.toMutableList()
            val item = updated[index]
            updated[index] = item.copy(selected = !item.selected)
            state.copy(suggestions = updated)
        }
    }

    fun apply() {
        val toApply = _uiState.value.suggestions.filter { it.selected }
        if (toApply.isEmpty()) {
            _uiState.update { it.copy(isDone = true) }
            return
        }
        viewModelScope.launch {
            toApply.forEach { item ->
                val s = item.suggestion
                app.repository.reschedule(s.reminderId, s.suggestedDueAt)
                val reminder = app.repository.getById(s.reminderId) ?: return@forEach
                alarmScheduler.schedule(reminder)
            }
            _uiState.update { it.copy(isDone = true) }
        }
    }

    companion object {
        fun factory(app: ReminderApplication) = simpleViewModelFactory {
            FreeTimeScheduleViewModel(
                app = app,
                freeTimeRepo = FreeTimeScheduleRepository(),
                alarmScheduler = app.alarmScheduler,
            )
        }
    }
}
