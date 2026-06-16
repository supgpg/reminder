package com.reminder.app.ui.smartadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reminder.app.R
import com.reminder.app.ReminderApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAddScreen(onBack: () -> Unit, onNavigateToAddEdit: () -> Unit) {
    val app = LocalContext.current.applicationContext as ReminderApplication
    val viewModel: SmartAddViewModel = viewModel(factory = SmartAddViewModel.factory(app))
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.navigateToAddEdit) {
        if (uiState.navigateToAddEdit) onNavigateToAddEdit()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.smart_add_title)) },
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
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = uiState.input,
                onValueChange = viewModel::updateInput,
                label = { Text(stringResource(R.string.smart_add_input_label)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = viewModel::analyze,
                enabled = uiState.input.isNotBlank() && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.smart_add_analyze))
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }

            if (uiState.showFallbackMessage) {
                Text(
                    text = stringResource(R.string.smart_add_fallback_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
