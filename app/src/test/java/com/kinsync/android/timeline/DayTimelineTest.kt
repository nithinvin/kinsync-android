package com.kinsync.android.timeline

import com.kinsync.android.activityrecognition.ActivityTransitionRecord
import com.kinsync.android.activityrecognition.CoarseActivity
import com.kinsync.android.activityrecognition.TransitionKind
import com.kinsync.android.collector.UnlockEvent
import com.kinsync.android.collector.UnlockEventType
import com.kinsync.android.usage.AppUsageInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DayTimelineTest {

    private val dayStart = 1_000L
    private val dayEnd = 9_000L

    private fun timeline(
        now: Long = 20_000L,
        unlockEvents: List<UnlockEvent> = emptyList(),
        apps: List<AppUsageInterval> = emptyList(),
        homeScreenPackages: Set<String> = emptySet(),
        stateBefore: ActivityTransitionRecord? = null,
        transitions: List<ActivityTransitionRecord> = emptyList(),
        movements: List<Long> = emptyList(),
    ) = DayTimeline.of(
        dayStartEpochMillis = dayStart,
        dayEndEpochMillis = dayEnd,
        nowEpochMillis = now,
        unlockEvents = unlockEvents,
        appUsageIntervals = apps,
        homeScreenPackages = homeScreenPackages,
        activityStateBefore = stateBefore,
        activityTransitions = transitions,
        movementTimestamps = movements,
    )

    @Test
    fun entries_areInTimeOrderWithActivityFirstOnATie() {
        val result = timeline(
            unlockEvents = listOf(
                UnlockEvent(eventType = UnlockEventType.SCREEN_ON, timestampEpochMillis = 3_000),
                UnlockEvent(eventType = UnlockEventType.SCREEN_OFF, timestampEpochMillis = 3_500),
            ),
            transitions = listOf(
                ActivityTransitionRecord(
                    activity = CoarseActivity.WALKING,
                    kind = TransitionKind.ENTER,
                    timestampEpochMillis = 3_000,
                ),
            ),
            movements = listOf(3_000L, 2_000L),
        )

        assertEquals(
            listOf(
                TimelineEntry.Moved::class,
                TimelineEntry.Activity::class,
                TimelineEntry.Phone::class,
            ),
            result.entries.map { it::class },
        )
        assertEquals(listOf(2_000L, 3_000L, 3_000L), result.entries.map { it.startEpochMillis })
    }

    @Test
    fun homeScreen_isNotListedAsAnApp() {
        val result = timeline(
            unlockEvents = listOf(
                UnlockEvent(eventType = UnlockEventType.USER_PRESENT, timestampEpochMillis = 2_000),
                UnlockEvent(eventType = UnlockEventType.SCREEN_OFF, timestampEpochMillis = 4_000),
            ),
            apps = listOf(
                AppUsageInterval(packageName = "com.miui.home", startEpochMillis = 2_000, endEpochMillis = 2_500),
                AppUsageInterval(packageName = "com.whatsapp", startEpochMillis = 2_500, endEpochMillis = 3_500),
            ),
            homeScreenPackages = setOf("com.miui.home"),
        )

        assertEquals(listOf("com.whatsapp"), result.sessions.single().apps.map { it.packageName })
    }

    @Test
    fun today_endsAtNow() {
        val result = timeline(
            now = 4_000L,
            transitions = listOf(
                ActivityTransitionRecord(
                    activity = CoarseActivity.STILL,
                    kind = TransitionKind.ENTER,
                    timestampEpochMillis = 2_000,
                ),
            ),
        )

        assertEquals(4_000L, result.countUntilEpochMillis)
        assertEquals(4_000L, result.activityPeriods.single().endEpochMillis)
        assertTrue(result.activityPeriods.single().isOpen)
    }

    @Test
    fun nothingRecorded_isEmpty() {
        assertTrue(timeline().isEmpty)
        assertTrue(timeline().entries.isEmpty())
    }
}
