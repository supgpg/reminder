package com.reminder.app.ui.decompose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reminder.app.R
import com.reminder.app.ReminderApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDecompositionScreen(onDone: () -> Unit) {
    val app = LocalContext.current.applicationContext as ReminderApplication
    val viewModel: TaskDecompositionViewModel = viewModel(factory = TaskDecompositionViewModel.factory(app))
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.decompose_title)) },
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
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = uiState.goal,
                onValueChange = viewModel::updateGoal,
                label = { Text(stringResource(R.string.decompose_goal_label)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = viewModel::analyze,
                enabled = uiState.goal.isNotBlank() && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.decompose_analyze))
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            } else if (uiState.subtasks.isEmpty()) {
                Text(
                    text = stringResource(R.string.decompose_empty_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(uiState.subtasks.size) { index ->
                        val item = uiState.subtasks[index]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = item.selected, onCheckedChange = { viewModel.toggle(index) })
                            OutlinedTextField(
                                value = item.text,
                                onValueChange = { viewModel.updateText(index, it) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                        }
                    }
                }

                Button(onClick = viewModel::confirm, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.decompose_add_selected))
                }
            }
        }
    }
}
