package com.reminder.app.ui.addedit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    reminderId: Long?,
    onDone: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as ReminderApplication
    val context = LocalContext.current
    val viewModel: AddEditReminderViewModel = viewModel(
        factory = AddEditReminderViewModel.factory(app, reminderId),
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onDone()
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = uiState.dueAt }
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.KOREA)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (reminderId == null) R.string.add_reminder else R.string.edit_reminder,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text(stringResource(R.string.title_label)) },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = { Text(stringResource(R.string.description_label)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            calendar.set(year, month, dayOfMonth)
                            viewModel.updateDueAt(calendar.timeInMillis)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH),
                    ).show()
                }) {
                    Text("${stringResource(R.string.due_date_label)}: ${dateFormatter.format(Date(uiState.dueAt))}")
                }

                OutlinedButton(onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                            calendar.set(Calendar.MINUTE, minute)
                            calendar.set(Calendar.SECOND, 0)
                            viewModel.updateDueAt(calendar.timeInMillis)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true,
                    ).show()
                }) {
                    Text("${stringResource(R.string.due_time_label)}: ${timeFormatter.format(Date(uiState.dueAt))}")
                }
            }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }

            if (reminderId != null) {
                TextButton(onClick = viewModel::delete, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}
