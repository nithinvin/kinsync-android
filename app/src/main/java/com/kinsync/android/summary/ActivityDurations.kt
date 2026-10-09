package com.kinsync.android.summary

import com.kinsync.android.activityrecognition.ActivityTransitionRecord
import com.kinsync.android.activityrecognition.CoarseActivity
import com.kinsync.android.activityrecognition.TransitionKind

/** Time spent still, walking and in a vehicle during a period. */
object ActivityDurations {

    /**
     * Replays the transitions to find how long each activity lasted inside
     * [fromEpochMillis, toEpochMillis).
     *
     * [stateBefore] is the last transition before the period. If it started an activity, that
     * activity is counted from the start of the period (for example "still" overnight).
     *
     * - A transition into the activity already in progress is a repeat and is ignored.
     * - A new activity starting ends the previous one, even without its EXIT.
     * - An EXIT of the current activity ends it; time until the next ENTER is not counted.
     * - An EXIT of another activity is ignored.
     * - At the same time, EXIT is handled before ENTER, as Play services reports them.
     * - An activity still in progress counts until [toEpochMillis].
     */
    fun within(
        stateBefore: ActivityTransitionRecord?,
        transitions: List<ActivityTransitionRecord>,
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): Map<CoarseActivity, Long> {
        val totals = mutableMapOf<CoarseActivity, Long>()
        if (toEpochMillis <= fromEpochMillis) return totals

        var current: CoarseActivity? = stateBefore
            ?.takeIf { it.kind == TransitionKind.ENTER && it.timestampEpochMillis < fromEpochMillis }
            ?.activity
        var since = fromEpochMillis

        fun close(endEpochMillis: Long) {
            val activity = current ?: return
            val duration = endEpochMillis - since
            if (duration > 0) {
                totals[activity] = (totals[activity] ?: 0L) + duration
            }
        }

        val ordered = transitions
            .filter { it.timestampEpochMillis in fromEpochMillis until toEpochMillis }
            .sortedWith(compareBy({ it.timestampEpochMillis }, { if (it.kind == TransitionKind.EXIT) 0 else 1 }))
        for (transition in ordered) {
            when (transition.kind) {
                TransitionKind.ENTER -> if (transition.activity != current) {
                    close(transition.timestampEpochMillis)
                    current = transition.activity
                    since = transition.timestampEpochMillis
                }
                TransitionKind.EXIT -> if (transition.activity == current) {
                    close(transition.timestampEpochMillis)
                    current = null
                }
            }
        }
        close(toEpochMillis)
        return totals
    }
}
