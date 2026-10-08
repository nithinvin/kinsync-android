package com.kinsync.android.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/** Reads usage events for a time range. An interface so the collector can be tested on the JVM. */
fun interface UsageEventSource {
    fun events(fromEpochMillis: Long, toEpochMillis: Long): List<UsageEventRecord>
}

/**
 * Reads events from `UsageStatsManager.queryEvents()`. Needs the "Usage access" special
 * permission (`PACKAGE_USAGE_STATS`); without it Android returns no events.
 */
class UsageStatsManagerEventSource(context: Context) : UsageEventSource {

    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)

    override fun events(fromEpochMillis: Long, toEpochMillis: Long): List<UsageEventRecord> {
        val usageEvents = usageStatsManager.queryEvents(fromEpochMillis, toEpochMillis) ?: return emptyList()
        val records = mutableListOf<UsageEventRecord>()
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val kind = kindOf(event.eventType) ?: continue
            records.add(
                UsageEventRecord(
                    packageName = event.packageName.orEmpty(),
                    className = event.className,
                    timestampEpochMillis = event.timeStamp,
                    kind = kind,
                ),
            )
        }
        return records
    }

    private fun kindOf(eventType: Int): UsageEventKind? = when (eventType) {
        // ACTIVITY_RESUMED and ACTIVITY_PAUSED (API 29) have the same values as the older
        // MOVE_TO_FOREGROUND and MOVE_TO_BACKGROUND, so these work on every supported version.
        @Suppress("DEPRECATION")
        UsageEvents.Event.MOVE_TO_FOREGROUND -> UsageEventKind.FOREGROUND
        @Suppress("DEPRECATION")
        UsageEvents.Event.MOVE_TO_BACKGROUND -> UsageEventKind.BACKGROUND
        DEVICE_SHUTDOWN_EVENT -> UsageEventKind.DEVICE_SHUTDOWN
        else -> null
    }

    private companion object {
        /** `UsageEvents.Event.DEVICE_SHUTDOWN` (API 28); written out so it also compiles for API 26. */
        const val DEVICE_SHUTDOWN_EVENT = 26
    }
}
