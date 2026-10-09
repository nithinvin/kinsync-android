package com.kinsync.android.summary

import com.kinsync.android.usage.AppUsageInterval

/** Total time any app was in the foreground in a period. */
object ScreenTime {

    /**
     * Foreground time inside [fromEpochMillis, toEpochMillis), leaving out [excludedPackages]
     * (the home screen, like Android's own screen-time count). Overlapping intervals, such as
     * split screen or an app switch recorded with a small overlap, are counted once.
     */
    fun total(
        intervals: List<AppUsageInterval>,
        fromEpochMillis: Long,
        toEpochMillis: Long,
        excludedPackages: Set<String>,
    ): Long {
        val clipped = intervals
            .filter { it.packageName !in excludedPackages }
            .map { maxOf(it.startEpochMillis, fromEpochMillis) to minOf(it.endEpochMillis, toEpochMillis) }
            .filter { (start, end) -> end > start }
            .sortedBy { (start, _) -> start }

        var total = 0L
        var runStart = Long.MIN_VALUE
        var runEnd = Long.MIN_VALUE
        for ((start, end) in clipped) {
            if (start > runEnd) {
                if (runEnd > runStart) {
                    total += runEnd - runStart
                }
                runStart = start
                runEnd = end
            } else if (end > runEnd) {
                runEnd = end
            }
        }
        if (runEnd > runStart) {
            total += runEnd - runStart
        }
        return total
    }
}
