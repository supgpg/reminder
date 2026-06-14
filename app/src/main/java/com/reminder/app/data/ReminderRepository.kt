package com.reminder.app.data

import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val dao: ReminderDao) {

    fun observeAll(): Flow<List<Reminder>> = dao.observeAll()

    suspend fun getById(id: Long): Reminder? = dao.getById(id)

    suspend fun upsert(reminder: Reminder): Long {
        return if (reminder.id == 0L) {
            dao.insert(reminder)
        } else {
            dao.update(reminder)
            reminder.id
        }
    }

    suspend fun delete(reminder: Reminder) = dao.delete(reminder)

    suspend fun postpone(id: Long, newDueAt: Long) = dao.postpone(id, newDueAt)

    suspend fun markDone(id: Long) = dao.markDone(id)
}
