package com.kinsync.android.summary

import com.kinsync.android.activityrecognition.ActivityTransitionRecord
import com.kinsync.android.activityrecognition.CoarseActivity
import com.kinsync.android.activityrecognition.CoarseActivity.IN_VEHICLE
import com.kinsync.android.activityrecognition.CoarseActivity.STILL
import com.kinsync.android.activityrecognition.CoarseActivity.WALKING
import com.kinsync.android.activityrecognition.TransitionKind
import com.kinsync.android.activityrecognition.TransitionKind.ENTER
import com.kinsync.android.activityrecognition.TransitionKind.EXIT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityDurationsTest {

    private val dayStart = 1_000L
    private val dayEnd = 2_000L

    private fun t(activity: CoarseActivity, kind: TransitionKind, at: Long) =
        ActivityTransitionRecord(activity = activity, kind = kind, timestampEpochMillis = at)

    private fun durations(
        transitions: List<ActivityTransitionRecord>,
        before: ActivityTransitionRecord? = null,
        to: Long = dayEnd,
    ) = ActivityDurations.within(before, transitions, dayStart, to)

    @Test
    fun walkBetweenTwoStillPeriods_isSplitCorrectly() {
        // Play services reports the EXIT and the next ENTER at the same time.
        val transitions = listOf(
            t(STILL, ENTER, 1_100),
            t(STILL, EXIT, 1_400),
            t(WALKING, ENTER, 1_400),
            t(WALKING, EXIT, 1_500),
            t(STILL, ENTER, 1_500),
        )

        assertEquals(mapOf(STILL to 300L + 500L, WALKING to 100L), durations(transitions))
    }

    @Test
    fun activityInProgressAtMidnight_countsFromTheStartOfTheDay() {
        val before = t(STILL, ENTER, 200)

        assertEquals(mapOf(STILL to 500L, WALKING to 500L), durations(listOf(t(WALKING, ENTER, 1_500)), before))
    }

    @Test
    fun exitBeforeMidnight_meansNothingWasInProgress() {
        val before = t(IN_VEHICLE, EXIT, 900)

        assertEquals(mapOf(WALKING to 100L), durations(listOf(t(WALKING, ENTER, 1_900)), before))
    }

    @Test
    fun repeatedEnter_isCountedOnce() {
        // Seen on the demo phone: the same "still started" stored twice, 1 ms apart.
        val transitions = listOf(t(STILL, ENTER, 1_500), t(STILL, ENTER, 1_501))

        assertEquals(mapOf(STILL to 500L), durations(transitions))
    }

    @Test
    fun newActivityWithoutExit_endsThePreviousOne() {
        val transitions = listOf(t(WALKING, ENTER, 1_000), t(IN_VEHICLE, ENTER, 1_300))

        assertEquals(mapOf(WALKING to 300L, IN_VEHICLE to 700L), durations(transitions))
    }

    @Test
    fun timeAfterAnExit_isNotCounted() {
        val transitions = listOf(t(WALKING, ENTER, 1_000), t(WALKING, EXIT, 1_200), t(STILL, ENTER, 1_800))

        assertEquals(mapOf(WALKING to 200L, STILL to 200L), durations(transitions))
    }

    @Test
    fun exitOfAnotherActivity_isIgnored() {
        val transitions = listOf(t(STILL, ENTER, 1_000), t(WALKING, EXIT, 1_200))

        assertEquals(mapOf(STILL to 1_000L), durations(transitions))
    }

    @Test
    fun today_countsOnlyUntilNow() {
        assertEquals(mapOf(WALKING to 200L), durations(listOf(t(WALKING, ENTER, 1_100)), to = 1_300))
    }

    @Test
    fun noTransitionsAndNothingBefore_isEmpty() {
        assertTrue(durations(emptyList()).isEmpty())
    }

    @Test
    fun transitionsOutsideThePeriod_areIgnored() {
        val transitions = listOf(t(WALKING, ENTER, 2_500), t(STILL, ENTER, 500))

        assertTrue(durations(transitions).isEmpty())
    }

    @Test
    fun emptyPeriod_isEmpty() {
        // Malformed input: the period ends before it starts (e.g. the clock was set back).
        assertTrue(durations(listOf(t(WALKING, ENTER, 1_100)), to = 900).isEmpty())
    }
}
