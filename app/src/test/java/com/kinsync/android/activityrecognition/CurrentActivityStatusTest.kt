package com.kinsync.android.activityrecognition

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrentActivityStatusTest {

    private fun record(activity: CoarseActivity, kind: TransitionKind, at: Long) =
        ActivityTransitionRecord(activity = activity, kind = kind, timestampEpochMillis = at)

    @Test
    fun latestEnter_isTheCurrentActivity() {
        val status = CurrentActivityStatus.of(true, record(CoarseActivity.WALKING, TransitionKind.ENTER, 10L))

        assertEquals(CurrentActivityStatus.Doing(CoarseActivity.WALKING, 10L), status)
    }

    @Test
    fun latestExit_saysWhatStopped() {
        val status = CurrentActivityStatus.of(true, record(CoarseActivity.IN_VEHICLE, TransitionKind.EXIT, 20L))

        assertEquals(CurrentActivityStatus.Stopped(CoarseActivity.IN_VEHICLE, 20L), status)
    }

    @Test
    fun permissionOnButNothingYet_isNotYet() {
        assertEquals(CurrentActivityStatus.NotYet, CurrentActivityStatus.of(true, latest = null))
    }

    @Test
    fun permissionOff_isNoPermission() {
        assertEquals(CurrentActivityStatus.NoPermission, CurrentActivityStatus.of(false, latest = null))
    }

    @Test
    fun permissionOffWithOldData_doesNotClaimACurrentActivity() {
        // Edge case: the elder turned the permission off; the last stored "walking" may be days old.
        val status = CurrentActivityStatus.of(false, record(CoarseActivity.WALKING, TransitionKind.ENTER, 5L))

        assertEquals(CurrentActivityStatus.NoPermission, status)
    }
}
