package com.kinsync.android.usage

/**
 * Turns foreground/background events into per-app foreground intervals (FR-2.2).
 *
 * Pure Kotlin with no Android types, so every case is covered by JVM tests.
 */
object AppUsageIntervalBuilder {

    /**
     * Two intervals of the same app this close together are one visit (for example, moving
     * from one screen of the app to another pauses one activity just before resuming the next).
     */
    const val MERGE_GAP_MILLIS = 2_000L

    /**
     * Result of one build.
     *
     * @property intervals finished intervals, sorted by start time.
     * @property earliestOpenStartEpochMillis start of the oldest activity still in the
     *   foreground at the end of the events, or null when none is open. The next collection
     *   must read events again from this time so that interval is finished later.
     */
    data class Result(
        val intervals: List<AppUsageInterval>,
        val earliestOpenStartEpochMillis: Long?,
    )

    /**
     * Builds intervals from [events] (any order).
     *
     * - A background event without an earlier foreground event for the same activity is
     *   skipped: its start is outside the events, so its length is unknown.
     * - A second foreground event for an activity that is already open keeps the first start.
     * - Events with an empty package name, and intervals that end before they start or have
     *   zero length, are skipped as malformed.
     * - An interval that crosses midnight stays one interval; splitting by day is done when
     *   totals are computed ([AppUsageTotals]).
     */
    fun build(events: List<UsageEventRecord>): Result {
        val openStarts = mutableMapOf<ActivityKey, Long>()
        val closed = mutableListOf<AppUsageInterval>()

        for (event in events.sortedBy { it.timestampEpochMillis }) {
            when (event.kind) {
                UsageEventKind.FOREGROUND -> {
                    if (event.packageName.isNotBlank()) {
                        openStarts.putIfAbsent(event.activityKey(), event.timestampEpochMillis)
                    }
                }
                UsageEventKind.BACKGROUND -> {
                    val start = openStarts.remove(event.activityKey())
                    if (start != null) {
                        closed.addIfValid(event.packageName, start, event.timestampEpochMillis)
                    }
                }
                UsageEventKind.DEVICE_SHUTDOWN -> {
                    for ((key, start) in openStarts) {
                        closed.addIfValid(key.packageName, start, event.timestampEpochMillis)
                    }
                    openStarts.clear()
                }
            }
        }

        return Result(
            intervals = mergeCloseIntervals(closed),
            earliestOpenStartEpochMillis = openStarts.values.minOrNull(),
        )
    }

    private fun MutableList<AppUsageInterval>.addIfValid(packageName: String, start: Long, end: Long) {
        if (end > start) {
            add(AppUsageInterval(packageName = packageName, startEpochMillis = start, endEpochMillis = end))
        }
    }

    /** Joins intervals of the same app that overlap or are at most [MERGE_GAP_MILLIS] apart. */
    private fun mergeCloseIntervals(intervals: List<AppUsageInterval>): List<AppUsageInterval> {
        val merged = mutableListOf<AppUsageInterval>()
        for ((_, appIntervals) in intervals.groupBy { it.packageName }) {
            var current: AppUsageInterval? = null
            for (interval in appIntervals.sortedBy { it.startEpochMillis }) {
                val previous = current
                if (previous != null && interval.startEpochMillis - previous.endEpochMillis <= MERGE_GAP_MILLIS) {
                    current = previous.copy(endEpochMillis = maxOf(previous.endEpochMillis, interval.endEpochMillis))
                } else {
                    if (previous != null) {
                        merged.add(previous)
                    }
                    current = interval
                }
            }
            if (current != null) {
                merged.add(current)
            }
        }
        return merged.sortedBy { it.startEpochMillis }
    }

    private data class ActivityKey(val packageName: String, val className: String?)

    private fun UsageEventRecord.activityKey() = ActivityKey(packageName, className)
}
