package com.kinsync.android.summary

import com.kinsync.android.activityrecognition.ActivityTransitionRecord
import com.kinsync.android.activityrecognition.CoarseActivity
import com.kinsync.android.timeline.ActivityPeriods

/** Time spent still, walking and in a vehicle during a period. */
object ActivityDurations {

    /**
     * How long each activity lasted inside [fromEpochMillis, toEpochMillis). The transitions are
     * replayed by [ActivityPeriods.within], which lists the rules. An activity still in progress
     * counts until [toEpochMillis]; an activity that did not happen is missing from the map.
     */
    fun within(
        stateBefore: ActivityTransitionRecord?,
        transitions: List<ActivityTransitionRecord>,
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): Map<CoarseActivity, Long> {
        val totals = mutableMapOf<CoarseActivity, Long>()
        for (period in ActivityPeriods.within(stateBefore, transitions, fromEpochMillis, toEpochMillis)) {
            totals[period.activity] = (totals[period.activity] ?: 0L) + period.durationMillis
        }
        return totals
    }
}
