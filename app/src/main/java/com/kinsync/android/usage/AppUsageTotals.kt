package com.kinsync.android.usage

/** Time one app spent in the foreground during a period. */
data class AppUsageTotal(
    val packageName: String,
    val totalMillis: Long,
)

/** Adds up foreground time per app inside a period, such as one day. */
object AppUsageTotals {

    /**
     * Foreground time per app inside [fromEpochMillis, toEpochMillis), largest first.
     *
     * Only the part of each interval inside the period counts, so an interval that crosses
     * midnight is split between the two days.
     */
    fun within(
        intervals: List<AppUsageInterval>,
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): List<AppUsageTotal> {
        val totals = mutableMapOf<String, Long>()
        for (interval in intervals) {
            val start = maxOf(interval.startEpochMillis, fromEpochMillis)
            val end = minOf(interval.endEpochMillis, toEpochMillis)
            if (end > start) {
                totals[interval.packageName] = (totals[interval.packageName] ?: 0L) + (end - start)
            }
        }
        return totals
            .map { (packageName, totalMillis) -> AppUsageTotal(packageName, totalMillis) }
            .sortedWith(compareByDescending<AppUsageTotal> { it.totalMillis }.thenBy { it.packageName })
    }
}
