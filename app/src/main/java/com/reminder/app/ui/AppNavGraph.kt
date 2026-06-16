package com.reminder.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reminder.app.R
import com.reminder.app.ui.addedit.AddEditReminderScreen
import com.reminder.app.ui.decompose.TaskDecompositionScreen
import com.reminder.app.ui.freetime.FreeTimeScheduleScreen
import com.reminder.app.ui.reminderlist.ReminderListScreen
import com.reminder.app.ui.report.ReportScreen
import com.reminder.app.ui.settings.SettingsScreen
import com.reminder.app.ui.smartadd.SmartAddScreen

object Routes {
    const val MAIN = "main"
    const val ADD_EDIT = "add_edit"
    const val DECOMPOSE = "decompose"
    const val SMART_ADD = "smart_add"
    const val FREE_TIME = "free_time"
    const val REMINDER_ID_ARG = "reminderId"
    val ADD_EDIT_ROUTE = "$ADD_EDIT?$REMINDER_ID_ARG={$REMINDER_ID_ARG}"

    fun addEdit(reminderId: Long? = null): String =
        if (reminderId == null) ADD_EDIT else "$ADD_EDIT?$REMINDER_ID_ARG=$reminderId"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                onAddClick = { navController.navigate(Routes.addEdit()) },
                onReminderClick = { id -> navController.navigate(Routes.addEdit(id)) },
                onSmartAddClick = { navController.navigate(Routes.SMART_ADD) },
                onDecomposeClick = { navController.navigate(Routes.DECOMPOSE) },
                onFreeTimeClick = { navController.navigate(Routes.FREE_TIME) },
            )
        }

        composable(Routes.DECOMPOSE) {
            TaskDecompositionScreen(onDone = { navController.popBackStack() })
        }

        composable(Routes.FREE_TIME) {
            FreeTimeScheduleScreen(onDone = { navController.popBackStack() })
        }

        composable(Routes.SMART_ADD) {
            SmartAddScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAddEdit = {
                    navController.navigate(Routes.addEdit()) {
                        popUpTo(Routes.SMART_ADD) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.ADD_EDIT_ROUTE,
            arguments = listOf(
                navArgument(Routes.REMINDER_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = 0L
                },
            ),
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getLong(Routes.REMINDER_ID_ARG) ?: 0L
            AddEditReminderScreen(
                reminderId = if (reminderId == 0L) null else reminderId,
                onDone = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun MainScreen(
    onAddClick: () -> Unit,
    onReminderClick: (Long) -> Unit,
    onSmartAddClick: () -> Unit,
    onDecomposeClick: () -> Unit,
    onFreeTimeClick: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_list)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_report)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> ReminderListScreen(
                    onAddClick = onAddClick,
                    onReminderClick = onReminderClick,
                    onSmartAddClick = onSmartAddClick,
                    onDecomposeClick = onDecomposeClick,
                    onFreeTimeClick = onFreeTimeClick,
                )
                1 -> ReportScreen()
                2 -> SettingsScreen()
            }
        }
    }
}
