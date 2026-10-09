package com.kinsync.android.timeline

import com.kinsync.android.activityrecognition.ActivityTransitionRecord
import com.kinsync.android.collector.UnlockEvent
import com.kinsync.android.usage.AppUsageInterval

/** One line on the "My day" timeline. */
sealed interface TimelineEntry {
    val startEpochMillis: Long

    data class Phone(val session: PhoneSession) : TimelineEntry {
        override val startEpochMillis: Long
            get() = session.startEpochMillis
    }

    data class Activity(val period: ActivityPeriod) : TimelineEntry {
        override val startEpochMillis: Long
            get() = period.startEpochMillis
    }

    data class Moved(val burst: MovementBurst) : TimelineEntry {
        override val startEpochMillis: Long
            get() = burst.firstEpochMillis
    }
}

/** Everything recorded in one day, on one 24-hour timeline (FR-2.5). */
data class DayTimeline(
    val dayStartEpochMillis: Long,
    val dayEndEpochMillis: Long,
    /** The end of what can have happened: the end of the day, or now for today. */
    val countUntilEpochMillis: Long,
    val sessions: List<PhoneSession>,
    val activityPeriods: List<ActivityPeriod>,
    val movements: List<MovementBurst>,
) {
    /**
     * All sessions, activity periods and movements in time order. When two start at the same
     * time, the activity comes first, then the phone, then the movement.
     */
    val entries: List<TimelineEntry> by lazy {
        val all = activityPeriods.map { TimelineEntry.Activity(it) } +
            sessions.map { TimelineEntry.Phone(it) } +
            movements.map { TimelineEntry.Moved(it) }
        // sortedBy is stable, so the order above breaks ties.
        all.sortedBy { it.startEpochMillis }
    }

    val isEmpty: Boolean
        get() = sessions.isEmpty() && activityPeriods.isEmpty() && movements.isEmpty()

    companion object {
        /**
         * Builds the timeline for the day [dayStartEpochMillis, dayEndEpochMillis). For today,
         * only the part until [nowEpochMillis] counts, so a session or activity in progress is
         * not drawn into the future. Time on the home screen ([homeScreenPackages]) is not app use.
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
            movementTimestamps: List<Long>,
        ): DayTimeline {
            val countUntil = minOf(dayEndEpochMillis, nowEpochMillis)
            val appIntervals = appUsageIntervals.filter { it.packageName !in homeScreenPackages }
            return DayTimeline(
                dayStartEpochMillis = dayStartEpochMillis,
                dayEndEpochMillis = dayEndEpochMillis,
                countUntilEpochMillis = countUntil,
                sessions = PhoneSessions.within(unlockEvents, appIntervals, dayStartEpochMillis, countUntil),
                activityPeriods = ActivityPeriods.within(
                    stateBefore = activityStateBefore,
                    transitions = activityTransitions,
                    fromEpochMillis = dayStartEpochMillis,
                    toEpochMillis = countUntil,
                ),
                movements = MovementBursts.within(movementTimestamps, dayStartEpochMillis, countUntil),
            )
        }
    }
}
