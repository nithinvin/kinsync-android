package com.kinsync.android.timeline

import com.kinsync.android.activityrecognition.ActivityTransitionRecord
import com.kinsync.android.activityrecognition.CoarseActivity
import com.kinsync.android.activityrecognition.TransitionKind

/** One stretch of being still, walking or in a vehicle. */
data class ActivityPeriod(
    val activity: CoarseActivity,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    /**
     * True when the activity had not ended by the end of the period. For today this means it is
     * still going on.
     */
    val isOpen: Boolean,
) {
    val durationMillis: Long
        get() = endEpochMillis - startEpochMillis
}

/** Turns the stored activity transitions into periods (FR-2.3, FR-2.5). */
object ActivityPeriods {

    /**
     * Replays the transitions to find when each activity started and ended inside
     * [fromEpochMillis, toEpochMillis), oldest first.
     *
     * [stateBefore] is the last transition before the period. If it started an activity, that
     * activity is counted from the start of the period (for example "still" overnight).
     *
     * - A transition into the activity already in progress is a repeat and is ignored.
     * - A new activity starting ends the previous one, even without its EXIT.
     * - An EXIT of the current activity ends it; time until the next ENTER is not counted.
     * - An EXIT of another activity is ignored.
     * - At the same time, EXIT is handled before ENTER, as Play services reports them.
     * - An activity still in progress lasts until [toEpochMillis] and is marked open.
     * - Periods of zero length are left out.
     */
    fun within(
        stateBefore: ActivityTransitionRecord?,
        transitions: List<ActivityTransitionRecord>,
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): List<ActivityPeriod> {
        val periods = mutableListOf<ActivityPeriod>()
        if (toEpochMillis <= fromEpochMillis) return periods

        var current: CoarseActivity? = stateBefore
            ?.takeIf { it.kind == TransitionKind.ENTER && it.timestampEpochMillis < fromEpochMillis }
            ?.activity
        var since = fromEpochMillis

        fun close(endEpochMillis: Long, isOpen: Boolean) {
            val activity = current ?: return
            if (endEpochMillis > since) {
                periods += ActivityPeriod(activity, since, endEpochMillis, isOpen)
            }
        }

        val ordered = transitions
            .filter { it.timestampEpochMillis in fromEpochMillis until toEpochMillis }
            .sortedWith(compareBy({ it.timestampEpochMillis }, { if (it.kind == TransitionKind.EXIT) 0 else 1 }))
        for (transition in ordered) {
            when (transition.kind) {
                TransitionKind.ENTER -> if (transition.activity != current) {
                    close(transition.timestampEpochMillis, isOpen = false)
                    current = transition.activity
                    since = transition.timestampEpochMillis
                }
                TransitionKind.EXIT -> if (transition.activity == current) {
                    close(transition.timestampEpochMillis, isOpen = false)
                    current = null
                }
            }
        }
        close(toEpochMillis, isOpen = true)
        return periods
    }
}
