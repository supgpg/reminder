package com.reminder.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueAt: Long,
    val postponeCount: Int = 0,
    val isDone: Boolean = false,
)
