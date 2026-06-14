package com.reminder.app.data

import com.reminder.app.calendar.CalendarSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

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
        dao.markDone(id, System.currentTimeMillis())

        if (isSyncActive()) {
            dao.getById(id)?.let { syncToCalendar(it) }
        }
    }

    suspend fun getReportStats(periodLabel: String, startMillis: Long, endMillis: Long): ReportStats {
        val all = dao.getAllOnce()
        val dueInPeriod = all.filter { it.dueAt in startMillis until endMillis }
        val morning = dueInPeriod.filter { hourOf(it.dueAt) < 12 }
        val afternoon = dueInPeriod.filter { hourOf(it.dueAt) >= 12 }
        val caughtUp = all.count {
            it.completedAt != null && it.completedAt in startMillis until endMillis && it.dueAt < startMillis
        }

        return ReportStats(
            periodLabel = periodLabel,
            totalCount = dueInPeriod.size,
            completedCount = dueInPeriod.count { it.isDone },
            completionRate = if (dueInPeriod.isEmpty()) 0f else dueInPeriod.count { it.isDone }.toFloat() / dueInPeriod.size,
            morningTotal = morning.size,
            morningCompleted = morning.count { it.isDone },
            afternoonTotal = afternoon.size,
            afternoonCompleted = afternoon.count { it.isDone },
            postponedCount = dueInPeriod.count { it.postponeCount > 0 },
            avgPostponeCount = if (dueInPeriod.isEmpty()) 0f else dueInPeriod.map { it.postponeCount }.average().toFloat(),
            caughtUpCount = caughtUp,
        )
    }

    private fun hourOf(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)

    suspend fun syncAllToCalendar() {
        if (!isSyncActive()) return

        dao.getAllOnce().forEach { syncToCalendar(it) }
    }

    suspend fun importFromCalendar() {
        if (!isSyncActive()) return

        val selected = personaPreferences.selectedCalendarIds.first()
        val existing = dao.getAllOnce()
        val byEventId = existing.mapNotNull { reminder -> reminder.calendarEventId?.let { it to reminder } }.toMap()

        val sinceMillis = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        calendarSyncManager.importEvents(selected, sinceMillis).forEach { event ->
            val known = byEventId[event.eventId]
            if (known != null) {
                if (known.title != event.title || known.description != event.description || known.dueAt != event.dtStart) {
                    dao.update(
                        known.copy(
                            title = event.title,
                            description = event.description,
                            dueAt = event.dtStart,
                        ),
                    )
                }
            } else {
                dao.insert(
                    Reminder(
                        title = event.title,
                        description = event.description,
                        dueAt = event.dtStart,
                        calendarEventId = event.eventId,
                    ),
                )
            }
        }
    }

    suspend fun refreshCalendarSync() {
        syncAllToCalendar()
        importFromCalendar()
    }

    private suspend fun syncToCalendar(reminder: Reminder) {
        val primaryCalendarId = personaPreferences.selectedCalendarIds.first().firstOrNull() ?: return
        val eventId = calendarSyncManager.upsertEvent(reminder, primaryCalendarId)
        if (eventId != null && eventId != reminder.calendarEventId) {
            dao.updateCalendarEventId(reminder.id, eventId)
        }
    }

    private suspend fun isSyncActive(): Boolean {
        return personaPreferences.calendarSyncEnabled.first() &&
            calendarSyncManager.hasPermission() &&
            personaPreferences.selectedCalendarIds.first().isNotEmpty()
    }
}
