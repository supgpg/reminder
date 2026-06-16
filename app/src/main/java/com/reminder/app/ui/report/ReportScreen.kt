package com.reminder.app.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reminder.app.R
import com.reminder.app.ReminderApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as ReminderApplication
    val viewModel: ReportViewModel = viewModel(factory = ReportViewModel.factory(app))
    val period by viewModel.period.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            PeriodSelector(period = period, onPeriodChange = viewModel::setPeriod)

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                stats?.let { s ->
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(
                                    R.string.report_completion_rate,
                                    (s.completionRate * 100).toInt(),
                                    s.completedCount,
                                    s.totalCount,
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.report_morning_afternoon,
                                    s.morningCompleted,
                                    s.morningTotal,
                                    s.afternoonCompleted,
                                    s.afternoonTotal,
                                ),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.report_postponed,
                                    s.postponedCount,
                                    s.avgPostponeCount,
                                ),
                            )
                            if (s.caughtUpCount > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = stringResource(R.string.report_caught_up, s.caughtUpCount))
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.report_ai_feedback_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = feedback ?: "", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(period: ReportPeriod, onPeriodChange: (ReportPeriod) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = period == ReportPeriod.DAILY,
            onClick = { onPeriodChange(ReportPeriod.DAILY) },
            label = { Text(stringResource(R.string.report_period_daily)) },
        )
        FilterChip(
            selected = period == ReportPeriod.WEEKLY,
            onClick = { onPeriodChange(ReportPeriod.WEEKLY) },
            label = { Text(stringResource(R.string.report_period_weekly)) },
        )
    }
}
