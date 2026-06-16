package com.reminder.app.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.reminder.app.ReminderApplication
import com.reminder.app.network.NotificationMiningRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class TaskExtractionService : NotificationListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val handler = CoroutineExceptionHandler { _, _ -> }

    private val recentKeys = object : LinkedHashMap<String, Long>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>) = size > 64
    }
    private var hourlyCalls = 0
    private var hourBucketStart = System.currentTimeMillis()

    private val miningRepo by lazy { NotificationMiningRepository() }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return
        if (pkg in EXCLUDED_PACKAGES) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString()?.trim() ?: return
        val text = (
            extras.getCharSequence("android.bigText")
                ?: extras.getCharSequence("android.text")
        )?.toString()?.trim() ?: ""

        if (title.length + text.length < 15) return

        val now = System.currentTimeMillis()
        synchronized(recentKeys) {
            val key = "$pkg:$title"
            val last = recentKeys[key]
            if (last != null && now - last < 30 * 60_000L) return
            recentKeys[key] = now

            if (now - hourBucketStart > 3_600_000L) { hourlyCalls = 0; hourBucketStart = now }
            if (hourlyCalls >= 30) return
            hourlyCalls++
        }

        scope.launch(handler) {
            val app = applicationContext as? ReminderApplication ?: return@launch
            if (!app.personaPreferences.notificationMiningEnabled.first()) return@launch

            val result = miningRepo.extract(title, text) ?: return@launch

            TaskSuggestionNotifier(applicationContext).suggest(
                title = result.title,
                dueAt = result.dueAt ?: defaultDueAt(),
                description = result.description,
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun defaultDueAt(): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    companion object {
        private val EXCLUDED_PACKAGES = setOf(
            "com.reminder.app",
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.sec.android.app.launcher",
        )
    }
}
