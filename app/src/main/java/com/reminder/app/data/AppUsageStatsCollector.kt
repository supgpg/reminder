package com.reminder.app.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.concurrent.TimeUnit

object AppUsageStatsCollector {

    private val EXCLUDED_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.google.android.gms",
        "com.google.android.googlequicksearchbox",
        "com.android.settings",
        "com.sec.android.app.launcher",
        "com.samsung.android.honeyboard",
        "com.samsung.android.inputmethod",
        "com.google.android.inputmethod.latin",
    )

    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getRecentUsageSummary(context: Context, windowMinutes: Int = 120): String? {
        if (!hasPermission(context)) return null

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.MINUTES.toMillis(windowMinutes.toLong())

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime,
        ) ?: return null

        val filtered = stats
            .filter { it.packageName !in EXCLUDED_PACKAGES && it.packageName != context.packageName }
            .filter { it.totalTimeInForeground > 60_000L }
            .sortedByDescending { it.totalTimeInForeground }
            .take(3)

        if (filtered.isEmpty()) return null

        return filtered.joinToString(", ") { stat ->
            val appName = getAppLabel(context, stat.packageName)
            "$appName ${formatDuration(stat.totalTimeInForeground)}"
        }
    }

    private fun getAppLabel(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    private fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
            hours > 0 -> "${hours}시간"
            else -> "${minutes}분"
        }
    }
}
