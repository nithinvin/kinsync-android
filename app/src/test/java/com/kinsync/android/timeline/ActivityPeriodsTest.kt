package com.kinsync.android.timeline

import com.kinsync.android.activityrecognition.ActivityTransitionRecord
import com.kinsync.android.activityrecognition.CoarseActivity
import com.kinsync.android.activityrecognition.CoarseActivity.STILL
import com.kinsync.android.activityrecognition.CoarseActivity.WALKING
import com.kinsync.android.activityrecognition.TransitionKind
import com.kinsync.android.activityrecognition.TransitionKind.ENTER
import com.kinsync.android.activityrecognition.TransitionKind.EXIT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The replay rules are also covered, through the totals, by ActivityDurationsTest. */
class ActivityPeriodsTest {

    private val dayStart = 1_000L
    private val dayEnd = 2_000L

    private fun t(activity: CoarseActivity, kind: TransitionKind, at: Long) =
        ActivityTransitionRecord(activity = activity, kind = kind, timestampEpochMillis = at)

    private fun periods(
        transitions: List<ActivityTransitionRecord>,
        before: ActivityTransitionRecord? = null,
        to: Long = dayEnd,
    ) = ActivityPeriods.within(before, transitions, dayStart, to)

    @Test
    fun walkBetweenTwoStillPeriods_givesThreePeriodsInOrder() {
        val transitions = listOf(
            t(STILL, ENTER, 1_100),
            t(STILL, EXIT, 1_400),
            t(WALKING, ENTER, 1_400),
            t(WALKING, EXIT, 1_500),
            t(STILL, ENTER, 1_500),
        )

        assertEquals(
            listOf(
                ActivityPeriod(STILL, 1_100, 1_400, isOpen = false),
                ActivityPeriod(WALKING, 1_400, 1_500, isOpen = false),
                ActivityPeriod(STILL, 1_500, dayEnd, isOpen = true),
            ),
            periods(transitions),
        )
    }

    @Test
    fun activityInProgressAtMidnight_startsAtTheStartOfTheDay() {
        val before = t(STILL, ENTER, 200)

        assertEquals(
            listOf(ActivityPeriod(STILL, dayStart, 1_300, isOpen = false)),
            periods(listOf(t(STILL, EXIT, 1_300)), before),
        )
    }

    @Test
    fun activityInProgressNow_isOpenUntilNow() {
        assertEquals(
            listOf(ActivityPeriod(WALKING, 1_100, 1_300, isOpen = true)),
            periods(listOf(t(WALKING, ENTER, 1_100)), to = 1_300),
        )
    }

    @Test
    fun enterAndExitAtTheSameTime_leavesNoEmptyPeriod() {
        val transitions = listOf(t(WALKING, ENTER, 1_200), t(WALKING, EXIT, 1_200))

        // EXIT is handled first, so the ENTER starts a walk that is still going on.
        assertEquals(listOf(ActivityPeriod(WALKING, 1_200, dayEnd, isOpen = true)), periods(transitions))
    }

    @Test
    fun noTransitions_isEmpty() {
        assertTrue(periods(emptyList()).isEmpty())
    }
}
