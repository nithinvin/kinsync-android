package com.kinsync.android.activityrecognition

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.DetectedActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivityTransitionMapperTest {

    private val nowEpochMillis = 1_760_000_000_000L
    private val nowElapsedRealtimeNanos = 50_000_000_000L // 50 s after boot

    private fun map(
        activityType: Int = DetectedActivity.WALKING,
        transitionType: Int = ActivityTransition.ACTIVITY_TRANSITION_ENTER,
        eventElapsedRealtimeNanos: Long = nowElapsedRealtimeNanos,
    ) = ActivityTransitionMapper.toRecord(
        activityType = activityType,
        transitionType = transitionType,
        eventElapsedRealtimeNanos = eventElapsedRealtimeNanos,
        nowEpochMillis = nowEpochMillis,
        nowElapsedRealtimeNanos = nowElapsedRealtimeNanos,
    )

    @Test
    fun walkingEnter_isStoredWithActivityKindAndTime() {
        val record = map()

        assertEquals(CoarseActivity.WALKING, record?.activity)
        assertEquals(TransitionKind.ENTER, record?.kind)
        assertEquals(nowEpochMillis, record?.timestampEpochMillis)
    }

    @Test
    fun everyRecordedActivityAndKind_isMapped() {
        val activities = mapOf(
            DetectedActivity.STILL to CoarseActivity.STILL,
            DetectedActivity.WALKING to CoarseActivity.WALKING,
            DetectedActivity.IN_VEHICLE to CoarseActivity.IN_VEHICLE,
        )
        val kinds = mapOf(
            ActivityTransition.ACTIVITY_TRANSITION_ENTER to TransitionKind.ENTER,
            ActivityTransition.ACTIVITY_TRANSITION_EXIT to TransitionKind.EXIT,
        )

        activities.forEach { (activityType, activity) ->
            kinds.forEach { (transitionType, kind) ->
                val record = map(activityType = activityType, transitionType = transitionType)
                assertEquals(activity, record?.activity)
                assertEquals(kind, record?.kind)
            }
        }
    }

    @Test
    fun earlierEvent_isConvertedFromTimeSinceBootToWallClock() {
        // The transition happened 30 s before now.
        val record = map(eventElapsedRealtimeNanos = nowElapsedRealtimeNanos - 30_000_000_000L)

        assertEquals(nowEpochMillis - 30_000L, record?.timestampEpochMillis)
    }

    @Test
    fun eventSlightlyInTheFuture_isTreatedAsNow() {
        val record = map(eventElapsedRealtimeNanos = nowElapsedRealtimeNanos + 5_000_000L)

        assertEquals(nowEpochMillis, record?.timestampEpochMillis)
    }

    @Test
    fun activityKinSyncDoesNotRecord_isDropped() {
        assertNull(map(activityType = DetectedActivity.RUNNING))
        assertNull(map(activityType = DetectedActivity.ON_BICYCLE))
        assertNull(map(activityType = DetectedActivity.UNKNOWN))
    }

    @Test
    fun unknownTransitionType_isDropped() {
        assertNull(map(transitionType = 99))
    }

    @Test
    fun negativeActivityType_isDropped() {
        // Malformed input: never a valid Play services activity.
        assertNull(map(activityType = -1))
    }
}
