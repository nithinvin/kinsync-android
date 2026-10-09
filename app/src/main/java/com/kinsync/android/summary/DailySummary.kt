package com.kinsync.android.summary

import com.kinsync.android.activityrecognition.ActivityTransitionRecord
import com.kinsync.android.activityrecognition.CoarseActivity
import com.kinsync.android.collector.UnlockEvent
import com.kinsync.android.usage.AppUsageInterval
import com.kinsync.android.usage.AppUsageTotal
import com.kinsync.android.usage.AppUsageTotals

/** Everything the "Today" screen shows, in plain numbers (FR-2.5 precursor). */
data class DailySummary(
    val unlocks: UnlockSummary,
    val screenTimeMillis: Long,
    /** The most used apps, largest first, home screen left out. */
    val topApps: List<AppUsageTotal>,
    /** Time per activity; an activity that did not happen is missing. */
    val activityMillis: Map<CoarseActivity, Long>,
) {
    companion object {
        const val TOP_APP_COUNT = 3

        /**
         * Builds the summary for the day [dayStartEpochMillis, dayEndEpochMillis). For today,
         * only the part until [nowEpochMillis] counts, so an activity in progress is not
         * counted into the future.
         */
        fun of(
            dayStartEpochMillis: Long,
            dayEndEpochMillis: Long,
            nowEpochMillis: Long,
            unlockEvents: List<UnlockEvent>,
            appUsageIntervals: List<AppUsageInterval>,
            homeScreenPackages: Set<String>,
            activityStateBefore: ActivityTransitionRecord?,
            activityTransitions: List<ActivityTransitionRecord>,
        ): DailySummary {
            val countUntil = minOf(dayEndEpochMillis, nowEpochMillis)
            val appIntervals = appUsageIntervals.filter { it.packageName !in homeScreenPackages }
            return DailySummary(
                unlocks = UnlockSummary.of(unlockEvents, dayStartEpochMillis, dayEndEpochMillis),
                screenTimeMillis = ScreenTime.total(appIntervals, dayStartEpochMillis, dayEndEpochMillis, emptySet()),
                topApps = AppUsageTotals.within(appIntervals, dayStartEpochMillis, dayEndEpochMillis)
                    .take(TOP_APP_COUNT),
                activityMillis = ActivityDurations.within(
                    stateBefore = activityStateBefore,
                    transitions = activityTransitions,
                    fromEpochMillis = dayStartEpochMillis,
                    toEpochMillis = countUntil,
                ),
            )
        }
    }
}
