package com.kinsync.android.summary

import com.kinsync.android.activityrecognition.ActivityTransitionRecord
import com.kinsync.android.activityrecognition.CoarseActivity
import com.kinsync.android.activityrecognition.TransitionKind
import com.kinsync.android.collector.UnlockEvent
import com.kinsync.android.collector.UnlockEventType
import com.kinsync.android.usage.AppUsageInterval
import com.kinsync.android.usage.AppUsageTotal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailySummaryTest {

    private val dayStart = 10_000L
    private val dayEnd = 20_000L

    private fun interval(app: String, start: Long, end: Long) =
        AppUsageInterval(packageName = app, startEpochMillis = start, endEpochMillis = end)

    private fun summary(
        now: Long = dayEnd,
        unlockEvents: List<UnlockEvent> = emptyList(),
        intervals: List<AppUsageInterval> = emptyList(),
        transitions: List<ActivityTransitionRecord> = emptyList(),
    ) = DailySummary.of(
        dayStartEpochMillis = dayStart,
        dayEndEpochMillis = dayEnd,
        nowEpochMillis = now,
        unlockEvents = unlockEvents,
        appUsageIntervals = intervals,
        homeScreenPackages = setOf("launcher"),
        activityStateBefore = null,
        activityTransitions = transitions,
    )

    @Test
    fun aTypicalDay_isAddedUp() {
        val result = summary(
            now = 15_000,
            unlockEvents = listOf(UnlockEvent(eventType = UnlockEventType.USER_PRESENT, timestampEpochMillis = 11_000)),
            intervals = listOf(interval("launcher", 11_000, 11_500), interval("whatsapp", 11_500, 12_000)),
            transitions = listOf(
                ActivityTransitionRecord(activity = CoarseActivity.WALKING, kind = TransitionKind.ENTER, timestampEpochMillis = 14_000),
            ),
        )

        assertEquals(UnlockSummary(firstUnlockEpochMillis = 11_000, unlockCount = 1), result.unlocks)
        assertEquals(500L, result.screenTimeMillis)
        assertEquals(listOf(AppUsageTotal("whatsapp", 500)), result.topApps)
        // Walking since 14:00 counts only until "now", not until the end of the day.
        assertEquals(mapOf(CoarseActivity.WALKING to 1_000L), result.activityMillis)
    }

    @Test
    fun topApps_areTheThreeLargestWithoutTheHomeScreen() {
        val result = summary(
            intervals = listOf(
                interval("launcher", 10_000, 19_000),
                interval("a", 10_000, 10_100),
                interval("b", 10_100, 10_400),
                interval("c", 10_400, 10_600),
                interval("d", 10_600, 10_650),
            ),
        )

        assertEquals(listOf("b", "c", "a"), result.topApps.map { it.packageName })
    }

    @Test
    fun emptyDay_hasZerosAndNoApps() {
        val result = summary()

        assertEquals(UnlockSummary(firstUnlockEpochMillis = null, unlockCount = 0), result.unlocks)
        assertEquals(0L, result.screenTimeMillis)
        assertTrue(result.topApps.isEmpty())
        assertTrue(result.activityMillis.isEmpty())
    }
}
