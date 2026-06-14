package com.reminder.app.data

data class ReportStats(
    val periodLabel: String,
    val totalCount: Int,
    val completedCount: Int,
    val completionRate: Float,
    val morningTotal: Int,
    val morningCompleted: Int,
    val afternoonTotal: Int,
    val afternoonCompleted: Int,
    val postponedCount: Int,
    val avgPostponeCount: Float,
    val caughtUpCount: Int,
)
