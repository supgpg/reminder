package com.reminder.app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.ReminderApplication
import com.reminder.app.data.ReminderRepository
import com.reminder.app.data.ReportStats
import com.reminder.app.network.ReportRepository
import com.reminder.app.ui.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReportPeriod {
    DAILY,
    WEEKLY,
}

class ReportViewModel(
    private val repository: ReminderRepository,
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val _period = MutableStateFlow(ReportPeriod.DAILY)
    val period: StateFlow<ReportPeriod> = _period.asStateFlow()

    private val _stats = MutableStateFlow<ReportStats?>(null)
    val stats: StateFlow<ReportStats?> = _stats.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadReport()
    }

    fun setPeriod(period: ReportPeriod) {
        if (_period.value == period) return
        _period.value = period
        loadReport()
    }

    fun loadReport() {
        viewModelScope.launch {
            _isLoading.value = true
            val (start, end, label) = periodRange(_period.value)
            val computedStats = repository.getReportStats(label, start, end)
            _stats.value = computedStats
            _feedback.value = reportRepository.generateFeedback(computedStats)
            _isLoading.value = false
        }
    }

    private fun periodRange(period: ReportPeriod): Triple<Long, Long, String> {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return when (period) {
            ReportPeriod.DAILY -> {
                val start = startOfToday.timeInMillis
                Triple(start, start + DAY_MILLIS, "오늘")
            }
            ReportPeriod.WEEKLY -> {
                startOfToday.firstDayOfWeek = Calendar.MONDAY
                startOfToday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val start = startOfToday.timeInMillis
                Triple(start, start + 7 * DAY_MILLIS, "이번 주")
            }
        }
    }

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

        fun factory(app: ReminderApplication) = simpleViewModelFactory {
            ReportViewModel(app.repository, app.reportRepository)
        }
    }
}
