package com.reminder.app.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.reminder.app.data.Reminder
import java.util.TimeZone

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val isPrimary: Boolean,
)

data class ImportedEvent(
    val eventId: Long,
    val calendarId: Long,
    val title: String,
    val description: String,
    val dtStart: Long,
)

class CalendarSyncManager(private val context: Context) {

    fun hasPermission(): Boolean {
        val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
        return read && write
    }

    fun listCalendars(): List<CalendarInfo> {
        if (!hasPermission()) return emptyList()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE,
        )

        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE} = 1 AND " +
                    "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
                arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
                "${CalendarContract.Calendars.IS_PRIMARY} DESC",
            )?.use { cursor ->
                val result = mutableListOf<CalendarInfo>()
                val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val accountNameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val accountTypeIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
                val displayNameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val isPrimaryIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)
                while (cursor.moveToNext()) {
                    result.add(
                        CalendarInfo(
                            id = cursor.getLong(idIndex),
                            displayName = cursor.getString(displayNameIndex) ?: "",
                            accountName = cursor.getString(accountNameIndex) ?: "",
                            accountType = cursor.getString(accountTypeIndex) ?: "",
                            isPrimary = cursor.getInt(isPrimaryIndex) != 0,
                        ),
                    )
                }
                result
            } ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun importEvents(calendarIds: Set<Long>, sinceMillis: Long): List<ImportedEvent> {
        if (!hasPermission() || calendarIds.isEmpty()) return emptyList()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
        )

        val placeholders = calendarIds.joinToString(",") { "?" }
        val selection = "${CalendarContract.Events.CALENDAR_ID} IN ($placeholders) AND " +
            "${CalendarContract.Events.DTSTART} >= ? AND " +
            "${CalendarContract.Events.DELETED} != 1"
        val selectionArgs = (calendarIds.map { it.toString() } + sinceMillis.toString()).toTypedArray()

        return try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC",
            )?.use { cursor ->
                val result = mutableListOf<ImportedEvent>()
                val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val calendarIdIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID)
                val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val descriptionIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
                val dtStartIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                while (cursor.moveToNext()) {
                    result.add(
                        ImportedEvent(
                            eventId = cursor.getLong(idIndex),
                            calendarId = cursor.getLong(calendarIdIndex),
                            title = cursor.getString(titleIndex) ?: "",
                            description = cursor.getString(descriptionIndex) ?: "",
                            dtStart = cursor.getLong(dtStartIndex),
                        ),
                    )
                }
                result
            } ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun registerObserver(handler: Handler, onChange: () -> Unit): ContentObserver? {
        if (!hasPermission()) return null
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                onChange()
            }
        }
        return try {
            context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
            observer
        } catch (e: SecurityException) {
            null
        }
    }

    fun unregisterObserver(observer: ContentObserver) {
        try {
            context.contentResolver.unregisterContentObserver(observer)
        } catch (e: Exception) {
            // no-op
        }
    }

    fun upsertEvent(reminder: Reminder, calendarId: Long): Long? {
        if (!hasPermission()) return null

        val title = if (reminder.isDone) "✅ ${reminder.title}" else reminder.title

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, reminder.description)
            put(CalendarContract.Events.DTSTART, reminder.dueAt)
            put(CalendarContract.Events.DTEND, reminder.dueAt + 60 * 60 * 1000L)
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
}
