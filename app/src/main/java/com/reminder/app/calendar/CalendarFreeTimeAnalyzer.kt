package com.reminder.app.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class BusyBlock(val title: String, val dtStart: Long, val dtEnd: Long)

object CalendarFreeTimeAnalyzer {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.KOREA)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd(E) HH:mm", Locale.KOREA)

    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun queryBusyBlocks(context: Context, fromMillis: Long, toMillis: Long): List<BusyBlock> {
        if (!hasPermission(context)) return emptyList()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
        )

        val selection = "${CalendarContract.Events.DTSTART} >= ? AND " +
            "${CalendarContract.Events.DTSTART} <= ? AND " +
            "${CalendarContract.Events.DELETED} != 1 AND " +
            "${CalendarContract.Events.ALL_DAY} = 0"

        return try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                arrayOf(fromMillis.toString(), toMillis.toString()),
                "${CalendarContract.Events.DTSTART} ASC",
            )?.use { cursor ->
                val result = mutableListOf<BusyBlock>()
                val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val dtStartIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val dtEndIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                while (cursor.moveToNext()) {
                    val dtEnd = cursor.getLong(dtEndIndex)
                    val dtStart = cursor.getLong(dtStartIndex)
                    val duration = dtEnd - dtStart
                    if (duration >= TimeUnit.MINUTES.toMillis(5)) {
                        result.add(
                            BusyBlock(
                                title = cursor.getString(titleIndex) ?: "일정",
                                dtStart = dtStart,
                                dtEnd = dtEnd,
                            ),
                        )
                    }
                }
                result
            } ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun formatBusyBlocksForPrompt(blocks: List<BusyBlock>, now: Long): String {
        if (blocks.isEmpty()) return "특별한 일정 없음"

        val today = Calendar.getInstance().apply { timeInMillis = now }
        val todayDate = today.get(Calendar.DAY_OF_MONTH)

        return blocks.joinToString(", ") { block ->
            val blockDay = Calendar.getInstance().apply { timeInMillis = block.dtStart }
            val prefix = if (blockDay.get(Calendar.DAY_OF_MONTH) == todayDate) "오늘" else "내일"
            "${prefix} ${timeFormat.format(Date(block.dtStart))}-${timeFormat.format(Date(block.dtEnd))} ${block.title}"
        }
    }

    fun todayWindowMillis(): Pair<Long, Long> {
        val from = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val to = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        return from to to
    }

    fun nowFormatted(now: Long): String = dateTimeFormat.format(Date(now))
}
