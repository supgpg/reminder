package com.reminder.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY isDone ASC, dueAt ASC")
    fun observeAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("UPDATE reminders SET postponeCount = postponeCount + 1, dueAt = :newDueAt WHERE id = :id")
    suspend fun postpone(id: Long, newDueAt: Long)

    @Query("UPDATE reminders SET isDone = 1 WHERE id = :id")
    suspend fun markDone(id: Long)
}
