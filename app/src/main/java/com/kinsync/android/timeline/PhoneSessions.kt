package com.kinsync.android.timeline

import com.kinsync.android.collector.UnlockEvent
import com.kinsync.android.collector.UnlockEventType
import com.kinsync.android.usage.AppUsageInterval
import com.kinsync.android.usage.AppUsageTotal
import com.kinsync.android.usage.AppUsageTotals

/** One stretch of the screen being on, from turning on (or unlocking) until it turned off. */
data class PhoneSession(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    /** The first unlock in the session; null when the screen only lit up, for example for a notification. */
    val firstUnlockEpochMillis: Long?,
    /** True when the screen had not turned off by the end of the period. */
    val isOpen: Boolean,
    /** Foreground time per app during the session, most used first. */
    val apps: List<AppUsageTotal>,
) {
    val durationMillis: Long
        get() = endEpochMillis - startEpochMillis

    val isUnlocked: Boolean
        get() = firstUnlockEpochMillis != null
}

/** Groups the screen and unlock events into sessions, with the apps used in each (FR-2.1, FR-2.2). */
object PhoneSessions {

    /**
     * Builds the sessions inside [fromEpochMillis, toEpochMillis), oldest first.
     *
     * - `SCREEN_ON` starts a session; `SCREEN_OFF` ends it.
     * - An unlock (`USER_PRESENT`) marks the session as unlocked. An unlock with no session
     *   open (the screen-on event was missed) starts one.
     * - A `SCREEN_ON` while a session is open means the `SCREEN_OFF` was missed (for example the
     *   service was stopped), so the open session ends there and a new one starts.
     * - A `SCREEN_OFF` with no session open is ignored.
     * - A session still open lasts until [toEpochMillis] and is marked open.
     *
     * [appIntervals] are cut to each session, so an app shows in every session it was used in.
     */
    fun within(
        events: List<UnlockEvent>,
        appIntervals: List<AppUsageInterval>,
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): List<PhoneSession> {
        val sessions = mutableListOf<PhoneSession>()
        var openStart: Long? = null
        var firstUnlock: Long? = null

        fun close(endEpochMillis: Long, isOpen: Boolean) {
            val start = openStart ?: return
            sessions += PhoneSession(
                startEpochMillis = start,
                endEpochMillis = endEpochMillis,
                firstUnlockEpochMillis = firstUnlock,
                isOpen = isOpen,
                apps = AppUsageTotals.within(appIntervals, start, endEpochMillis),
            )
            openStart = null
            firstUnlock = null
        }

        val ordered = events
            .filter { it.timestampEpochMillis in fromEpochMillis until toEpochMillis }
            .sortedBy { it.timestampEpochMillis }
        for (event in ordered) {
            val time = event.timestampEpochMillis
            when (event.eventType) {
                UnlockEventType.SCREEN_ON -> {
                    close(time, isOpen = false)
                    openStart = time
                }
                UnlockEventType.USER_PRESENT -> {
                    if (openStart == null) {
                        openStart = time
                    }
                    if (firstUnlock == null) {
                        firstUnlock = time
                    }
                }
                UnlockEventType.SCREEN_OFF -> close(time, isOpen = false)
            }
        }
        if (toEpochMillis > fromEpochMillis) {
            close(toEpochMillis, isOpen = true)
        }
        return sessions
    }
}
