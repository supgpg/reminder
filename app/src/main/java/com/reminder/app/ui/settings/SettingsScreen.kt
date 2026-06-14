package com.reminder.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reminder.app.R
import com.reminder.app.ReminderApplication
import com.reminder.app.data.SpiceLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ReminderApplication
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
    val spiceLevel by viewModel.spiceLevel.collectAsState()
    val calendarSyncEnabled by viewModel.calendarSyncEnabled.collectAsState()
    val selectedCalendarIds by viewModel.selectedCalendarIds.collectAsState()
    val availableCalendars by viewModel.availableCalendars.collectAsState()

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.READ_CALENDAR] == true &&
            result[Manifest.permission.WRITE_CALENDAR] == true
        if (granted) {
            viewModel.enableCalendarSync()
        }
    }

    if (calendarSyncEnabled) {
        LaunchedEffect(Unit) {
            viewModel.loadCalendars()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                text = stringResource(R.string.spice_level_title),
                style = MaterialTheme.typography.titleMedium,
            )

            SpiceLevelOption(
                label = stringResource(R.string.spice_level_mild),
                selected = spiceLevel == SpiceLevel.MILD,
                onClick = { viewModel.setSpiceLevel(SpiceLevel.MILD) },
            )

            SpiceLevelOption(
                label = stringResource(R.string.spice_level_spicy),
                selected = spiceLevel == SpiceLevel.SPICY,
                onClick = { viewModel.setSpiceLevel(SpiceLevel.SPICY) },
            )

            Text(
                text = stringResource(R.string.calendar_sync_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp),
            )

            Text(
                text = stringResource(R.string.calendar_sync_description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )

            if (calendarSyncEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.calendar_sync_switch_label),
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Switch(
                        checked = calendarSyncEnabled,
                        onCheckedChange = { enabled -> viewModel.setCalendarSyncEnabled(enabled) },
                    )
                }

                Text(
                    text = stringResource(R.string.calendar_select_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )

                availableCalendars.forEach { calendarInfo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = calendarInfo.id in selectedCalendarIds,
                                onClick = {
                                    viewModel.toggleCalendar(
                                        calendarInfo.id,
                                        calendarInfo.id !in selectedCalendarIds,
                                    )
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = calendarInfo.id in selectedCalendarIds,
                            onCheckedChange = { checked -> viewModel.toggleCalendar(calendarInfo.id, checked) },
                        )
                        Text(text = "${calendarInfo.displayName} (${calendarInfo.accountName})")
                    }
                }

                Button(
                    onClick = { viewModel.refresh() },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Text(text = stringResource(R.string.calendar_refresh), modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                Button(onClick = {
                    val readGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_CALENDAR,
                    ) == PackageManager.PERMISSION_GRANTED
                    val writeGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_CALENDAR,
                    ) == PackageManager.PERMISSION_GRANTED

                    if (readGranted && writeGranted) {
                        viewModel.enableCalendarSync()
                    } else {
                        calendarPermissionLauncher.launch(
                            arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
                        )
                    }
                }) {
                    Text(stringResource(R.string.calendar_sync_button))
                }
            }
        }
    }
}

@Composable
private fun SpiceLevelOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}
