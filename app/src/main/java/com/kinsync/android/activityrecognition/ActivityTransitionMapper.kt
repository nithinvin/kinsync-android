package com.kinsync.android.activityrecognition

/**
 * Turns one transition reported by Google Play services into a [ActivityTransitionRecord].
 * Kept free of Android types so it can be tested on the JVM.
 */
object ActivityTransitionMapper {

    private const val NANOS_PER_MILLI = 1_000_000L
    private const val MILLIS_PER_SECOND = 1_000L

    /**
     * Play services gives the time of a transition as "nanoseconds since boot". It is converted
     * to wall-clock time using the current time on both clocks. A time in the future (clock
     * jitter) is treated as now.
     *
     * The two clocks are read one after the other, so the result can drift by a millisecond or
     * two. Play services sometimes delivers the same transition twice; rounding to the nearest
     * whole second gives both copies the same time, so the unique index stores it once.
     *
     * Returns null for an activity or transition kind that KinSync does not record.
     */
    fun toRecord(
        activityType: Int,
        transitionType: Int,
        eventElapsedRealtimeNanos: Long,
        nowEpochMillis: Long,
        nowElapsedRealtimeNanos: Long,
    ): ActivityTransitionRecord? {
        val activity = CoarseActivity.fromDetectedActivityType(activityType) ?: return null
        val kind = TransitionKind.fromPlayServicesType(transitionType) ?: return null
        val ageMillis = ((nowElapsedRealtimeNanos - eventElapsedRealtimeNanos) / NANOS_PER_MILLI)
            .coerceAtLeast(0L)
        return ActivityTransitionRecord(
            activity = activity,
            kind = kind,
            timestampEpochMillis = roundToNearestSecond(nowEpochMillis - ageMillis),
        )
    }

    private fun roundToNearestSecond(epochMillis: Long): Long =
        Math.floorDiv(epochMillis + MILLIS_PER_SECOND / 2, MILLIS_PER_SECOND) * MILLIS_PER_SECOND
}
