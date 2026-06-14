package com.reminder.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reminder.app.ui.addedit.AddEditReminderScreen
import com.reminder.app.ui.decompose.TaskDecompositionScreen
import com.reminder.app.ui.reminderlist.ReminderListScreen
import com.reminder.app.ui.settings.SettingsScreen
import com.reminder.app.ui.smartadd.SmartAddScreen

object Routes {
    const val LIST = "list"
    const val SETTINGS = "settings"
    const val ADD_EDIT = "add_edit"
    const val DECOMPOSE = "decompose"
    const val SMART_ADD = "smart_add"
    const val REMINDER_ID_ARG = "reminderId"
    val ADD_EDIT_ROUTE = "$ADD_EDIT?$REMINDER_ID_ARG={$REMINDER_ID_ARG}"

    fun addEdit(reminderId: Long? = null): String =
        if (reminderId == null) "$ADD_EDIT" else "$ADD_EDIT?$REMINDER_ID_ARG=$reminderId"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            ReminderListScreen(
                onAddClick = { navController.navigate(Routes.addEdit()) },
                onReminderClick = { reminder -> navController.navigate(Routes.addEdit(reminder.id)) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onSmartAddClick = { navController.navigate(Routes.SMART_ADD) },
                onDecomposeClick = { navController.navigate(Routes.DECOMPOSE) },
            )
        }

        composable(Routes.DECOMPOSE) {
            TaskDecompositionScreen(onDone = { navController.popBackStack() })
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

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
