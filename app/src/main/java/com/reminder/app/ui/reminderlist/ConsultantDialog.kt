package com.reminder.app.ui.reminderlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reminder.app.R
import com.reminder.app.ReminderApplication
import com.reminder.app.data.PostponeReason

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultantDialog(reminderId: Long, onDismiss: () -> Unit) {
    val app = LocalContext.current.applicationContext as ReminderApplication
    val viewModel: ConsultantViewModel = viewModel(
        key = "consultant_$reminderId",
        factory = ConsultantViewModel.factory(app, reminderId),
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.step) {
        if (uiState.step is ConsultantStep.Done) onDismiss()
    }

    if (uiState.step is ConsultantStep.Done) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.consultant_title)) },
        text = {
            when (val step = uiState.step) {
                is ConsultantStep.Loading -> CircularProgressIndicator()
                is ConsultantStep.Question -> {
                    Column {
                        Text(step.text)
                        Spacer(modifier = Modifier.height(12.dp))
                        PostponeReason.entries.forEach { reason ->
                            TextButton(
                                onClick = { viewModel.selectReason(reason) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(reason.label)
                            }
                        }
                    }
                }
                is ConsultantStep.Suggestion -> {
                    Column {
                        Text(step.suggestion.newTitle, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(step.suggestion.advice, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is ConsultantStep.Done -> Unit
            }
        },
        confirmButton = {
            if (uiState.step is ConsultantStep.Suggestion) {
                TextButton(onClick = viewModel::accept) {
                    Text(stringResource(R.string.consultant_accept))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.consultant_dismiss))
            }
        },
    )
}
