package com.reminder.app.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.reminder.app.data.Reminder
import java.util.TimeZone

class CalendarSyncManager(private val context: Context) {

    fun hasPermission(): Boolean {
        val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
        return read && write
    }

    fun upsertEvent(reminder: Reminder): Long? {
        if (!hasPermission()) return null

        val calendarId = findWritableCalendarId() ?: return null
        val title = if (reminder.isDone) "✅ ${reminder.title}" else reminder.title

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, reminder.description)
            put(CalendarContract.Events.DTSTART, reminder.dueAt)
            put(CalendarContract.Events.DTEND, reminder.dueAt)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        return try {
            val existingId = reminder.calendarEventId
            if (existingId != null && eventExists(existingId)) {
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingId)
                context.contentResolver.update(uri, values, null, null)
                existingId
            } else {
                val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                uri?.lastPathSegment?.toLongOrNull()
            }
        } catch (e: SecurityException) {
            null
        }
    }

    fun deleteEvent(eventId: Long) {
        if (!hasPermission()) return

        try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            context.contentResolver.delete(uri, null, null)
        } catch (e: SecurityException) {
            // no-op
        }
    }

    private fun eventExists(eventId: Long): Boolean {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(CalendarContract.Events._ID),
                null,
                null,
                null,
            )?.use { it.moveToFirst() } ?: false
        } catch (e: SecurityException) {
            false
        }
    }

    private fun findWritableCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.IS_PRIMARY,
        )

        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
                arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
                "${CalendarContract.Calendars.IS_PRIMARY} DESC",
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                } else {
                    null
                }
            }
        } catch (e: SecurityException) {
            null
        }
    }
}
