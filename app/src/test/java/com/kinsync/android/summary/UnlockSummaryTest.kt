package com.kinsync.android.summary

import com.kinsync.android.collector.UnlockEvent
import com.kinsync.android.collector.UnlockEventType
import org.junit.Assert.assertEquals
import org.junit.Test

class UnlockSummaryTest {

    private fun event(type: UnlockEventType, at: Long) = UnlockEvent(eventType = type, timestampEpochMillis = at)

    @Test
    fun countsUnlocksAndFindsTheFirst() {
        val events = listOf(
            event(UnlockEventType.USER_PRESENT, 500),
            event(UnlockEventType.USER_PRESENT, 200),
            event(UnlockEventType.USER_PRESENT, 800),
        )

        assertEquals(UnlockSummary(firstUnlockEpochMillis = 200, unlockCount = 3), UnlockSummary.of(events, 0, 1_000))
    }

    @Test
    fun screenOnAndOff_areNotUnlocks() {
        val events = listOf(
            event(UnlockEventType.SCREEN_ON, 100),
            event(UnlockEventType.SCREEN_OFF, 150),
            event(UnlockEventType.SCREEN_ON, 300),
            event(UnlockEventType.USER_PRESENT, 310),
        )

        assertEquals(UnlockSummary(firstUnlockEpochMillis = 310, unlockCount = 1), UnlockSummary.of(events, 0, 1_000))
    }

    @Test
    fun noUnlocks_hasNoFirstUnlock() {
        assertEquals(UnlockSummary(firstUnlockEpochMillis = null, unlockCount = 0), UnlockSummary.of(emptyList(), 0, 1_000))
    }

    @Test
    fun unlocksOutsideTheDay_areIgnored() {
        // The day is [1000, 2000): an unlock exactly at its end belongs to the next day.
        val events = listOf(
            event(UnlockEventType.USER_PRESENT, 999),
            event(UnlockEventType.USER_PRESENT, 1_000),
            event(UnlockEventType.USER_PRESENT, 2_000),
        )

        assertEquals(UnlockSummary(firstUnlockEpochMillis = 1_000, unlockCount = 1), UnlockSummary.of(events, 1_000, 2_000))
    }
}
