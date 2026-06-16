package com.reminder.app.ui.reminderlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reminder.app.R
import com.reminder.app.ReminderApplication
import com.reminder.app.data.Reminder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private sealed class ListItem {
    data class Header(val label: String) : ListItem()
    data class ReminderRow(val reminder: Reminder) : ListItem()
    data object DoneHeader : ListItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    onAddClick: () -> Unit,
    onReminderClick: (Long) -> Unit,
    onSmartAddClick: () -> Unit,
    onDecomposeClick: () -> Unit,
    onFreeTimeClick: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as ReminderApplication
    val viewModel: ReminderListViewModel = viewModel(factory = ReminderListViewModel.factory(app))
    val reminders by viewModel.reminders.collectAsState()
    var consultantReminderId by remember { mutableStateOf<Long?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val pending = reminders.filter { !it.isDone }.sortedBy { it.dueAt }
    val done = reminders.filter { it.isDone }.sortedByDescending { it.completedAt ?: it.dueAt }

    val listItems = buildList {
        var lastDateKey = ""
        for (reminder in pending) {
            val dateKey = dateKey(reminder.dueAt)
            if (dateKey != lastDateKey) {
                add(ListItem.Header(dateLabel(reminder.dueAt)))
                lastDateKey = dateKey
            }
            add(ListItem.ReminderRow(reminder))
        }
        if (done.isNotEmpty()) {
            add(ListItem.DoneHeader)
            done.forEach { add(ListItem.ReminderRow(it)) }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.reminder_list_title),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.calendar_refresh))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = onSmartAddClick,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = stringResource(R.string.smart_add_fab))
                }
                SmallFloatingActionButton(
                    onClick = onDecomposeClick,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Icon(Icons.Filled.Checklist, contentDescription = stringResource(R.string.decompose_fab))
                }
                SmallFloatingActionButton(
                    onClick = onFreeTimeClick,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Icon(Icons.Filled.CalendarViewWeek, contentDescription = stringResource(R.string.freetime_fab))
                }
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_reminder))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "○",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = stringResource(R.string.empty_list_message),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 140.dp),
            ) {
                items(listItems, key = { item ->
                    when (item) {
                        is ListItem.Header -> "header_${item.label}"
                        is ListItem.ReminderRow -> "reminder_${item.reminder.id}"
                        is ListItem.DoneHeader -> "done_header"
                    }
                }) { item ->
                    when (item) {
                        is ListItem.Header -> DateHeader(label = item.label)
                        is ListItem.DoneHeader -> DoneHeader()
                        is ListItem.ReminderRow -> ReminderRow(
                            reminder = item.reminder,
                            onClick = { onReminderClick(item.reminder.id) },
                            onDone = { viewModel.markDone(item.reminder) },
                            onDelete = { viewModel.delete(item.reminder) },
                            onConsultClick = { consultantReminderId = item.reminder.id },
                        )
                    }
                }
            }
        }
    }

    consultantReminderId?.let { reminderId ->
        ConsultantDialog(reminderId = reminderId, onDismiss = { consultantReminderId = null })
    }
}

@Composable
private fun DateHeader(label: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun DoneHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "완료됨",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
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
    val isDone = reminder.isDone

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Checkbox circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .then(
                    if (isDone) Modifier.background(MaterialTheme.colorScheme.primary)
                    else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                .clickable(onClick = onDone),
            contentAlignment = Alignment.Center,
        ) {
            if (isDone) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(15.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isDone) FontWeight.Normal else FontWeight.Medium,
                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = formatTime(reminder.dueAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.secondary,
                )
                if (reminder.description.isNotBlank()) {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
            if (reminder.postponeCount > 0 && !isDone) {
                Text(
                    text = stringResource(R.string.postpone_count_format, reminder.postponeCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        // Action buttons
        if (!isDone && reminder.postponeCount >= ConsultantViewModel.THRESHOLD) {
            IconButton(onClick = onConsultClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.Psychology,
                    contentDescription = stringResource(R.string.consultant_button),
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp, end = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

private fun dateKey(epochMillis: Long): String {
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.KOREA)
    return sdf.format(Date(epochMillis))
}

private fun dateLabel(epochMillis: Long): String {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val tomorrow = today + 24 * 60 * 60 * 1000L

    return when {
        epochMillis < today + 24 * 60 * 60 * 1000 && epochMillis >= today -> {
            val dow = SimpleDateFormat("M월 d일 (E)", Locale.KOREA).format(Date(epochMillis))
            "오늘  $dow"
        }
        epochMillis < tomorrow + 24 * 60 * 60 * 1000 && epochMillis >= tomorrow -> {
            val dow = SimpleDateFormat("M월 d일 (E)", Locale.KOREA).format(Date(epochMillis))
            "내일  $dow"
        }
        epochMillis < today -> {
            val dow = SimpleDateFormat("M월 d일 (E)", Locale.KOREA).format(Date(epochMillis))
            "지남  $dow"
        }
        else -> SimpleDateFormat("M월 d일 (E)", Locale.KOREA).format(Date(epochMillis))
    }
}

private fun formatTime(epochMillis: Long): String {
    val formatter = SimpleDateFormat("a h:mm", Locale.KOREA)
    return formatter.format(Date(epochMillis))
}
