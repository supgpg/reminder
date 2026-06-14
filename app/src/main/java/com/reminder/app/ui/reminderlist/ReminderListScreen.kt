package com.reminder.app.ui.reminderlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reminder.app.R
import com.reminder.app.ReminderApplication
import com.reminder.app.data.Reminder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    onAddClick: () -> Unit,
    onReminderClick: (Reminder) -> Unit,
    onSettingsClick: () -> Unit,
    onSmartAddClick: () -> Unit,
    onDecomposeClick: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as ReminderApplication
    val viewModel: ReminderListViewModel = viewModel(factory = ReminderListViewModel.factory(app))
    val reminders by viewModel.reminders.collectAsState()
    var consultantReminderId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reminder_list_title)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(onClick = onSmartAddClick) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = stringResource(R.string.smart_add_fab))
                }
                SmallFloatingActionButton(onClick = onDecomposeClick) {
                    Icon(Icons.Filled.Checklist, contentDescription = stringResource(R.string.decompose_fab))
                }
                FloatingActionButton(onClick = onAddClick) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_reminder))
                }
            }
        },
    ) { padding ->
        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.empty_list_message),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        onClick = { onReminderClick(reminder) },
                        onDone = { viewModel.markDone(reminder) },
                        onDelete = { viewModel.delete(reminder) },
                        onConsultClick = { consultantReminderId = reminder.id },
                    )
                }
            }
        }
    }

    consultantReminderId?.let { reminderId ->
        ConsultantDialog(reminderId = reminderId, onDismiss = { consultantReminderId = null })
    }
}

@Composable
private fun ReminderRow(
    reminder: Reminder,
    onClick: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit,
    onConsultClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = reminder.title, style = MaterialTheme.typography.titleMedium)
                if (reminder.description.isNotBlank()) {
                    Text(text = reminder.description, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = formatDateTime(reminder.dueAt),
                    style = MaterialTheme.typography.labelMedium,
                )
                if (reminder.postponeCount > 0) {
                    Text(
                        text = stringResource(R.string.postpone_count_format, reminder.postponeCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            if (reminder.postponeCount >= ConsultantViewModel.THRESHOLD) {
                IconButton(onClick = onConsultClick) {
                    Icon(Icons.Filled.Psychology, contentDescription = stringResource(R.string.consultant_button))
                }
            }
            IconButton(onClick = onDone) {
                Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.action_done))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}

private fun formatDateTime(epochMillis: Long): String {
    val formatter = SimpleDateFormat("MM/dd (E) HH:mm", Locale.KOREA)
    return formatter.format(Date(epochMillis))
}
