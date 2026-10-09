package com.kinsync.android.summary

import com.kinsync.android.collector.UnlockEvent
import com.kinsync.android.collector.UnlockEventType

/** How often, and from when, the elder unlocked the phone in a period. */
data class UnlockSummary(
    /** Null when the phone was not unlocked in the period. */
    val firstUnlockEpochMillis: Long?,
    val unlockCount: Int,
) {
    companion object {
        /**
         * Counts unlocks (`USER_PRESENT`) inside [fromEpochMillis, toEpochMillis). Screen on/off
         * events are not unlocks: the screen also turns on for notifications.
         */
        fun of(events: List<UnlockEvent>, fromEpochMillis: Long, toEpochMillis: Long): UnlockSummary {
            val unlockTimes = events
                .filter { it.eventType == UnlockEventType.USER_PRESENT }
                .map { it.timestampEpochMillis }
                .filter { it in fromEpochMillis until toEpochMillis }
            return UnlockSummary(firstUnlockEpochMillis = unlockTimes.minOrNull(), unlockCount = unlockTimes.size)
        }
    }
}
