package com.reminder.app.data

import com.reminder.app.calendar.CalendarSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ReminderRepository(
    private val dao: ReminderDao,
    private val calendarSyncManager: CalendarSyncManager,
    private val personaPreferences: PersonaPreferences,
) {

    fun observeAll(): Flow<List<Reminder>> = dao.observeAll()

    suspend fun getById(id: Long): Reminder? = dao.getById(id)

    suspend fun upsert(reminder: Reminder): Long {
        val savedId = if (reminder.id == 0L) {
            dao.insert(reminder)
        } else {
            dao.update(reminder)
            reminder.id
        }

        if (isSyncActive()) {
            val saved = dao.getById(savedId) ?: reminder.copy(id = savedId)
            syncToCalendar(saved)
        }

        return savedId
    }

    suspend fun delete(reminder: Reminder) {
        dao.delete(reminder)

        if (isSyncActive()) {
            reminder.calendarEventId?.let { calendarSyncManager.deleteEvent(it) }
        }
    }

    suspend fun postpone(id: Long, newDueAt: Long) {
        dao.postpone(id, newDueAt)

        if (isSyncActive()) {
            dao.getById(id)?.let { syncToCalendar(it) }
        }
    }

    suspend fun markDone(id: Long) {
        dao.markDone(id)

        if (isSyncActive()) {
            dao.getById(id)?.let { syncToCalendar(it) }
        }
    }

    suspend fun syncAllToCalendar() {
        if (!isSyncActive()) return

        dao.getAllOnce().forEach { syncToCalendar(it) }
    }

    private suspend fun syncToCalendar(reminder: Reminder) {
        val eventId = calendarSyncManager.upsertEvent(reminder)
        if (eventId != null && eventId != reminder.calendarEventId) {
            dao.updateCalendarEventId(reminder.id, eventId)
        }
    }

    private suspend fun isSyncActive(): Boolean {
        return personaPreferences.calendarSyncEnabled.first() && calendarSyncManager.hasPermission()
    }
}
