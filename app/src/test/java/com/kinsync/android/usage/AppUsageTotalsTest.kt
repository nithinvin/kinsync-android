package com.kinsync.android.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUsageTotalsTest {

    private fun interval(app: String, start: Long, end: Long) =
        AppUsageInterval(packageName = app, startEpochMillis = start, endEpochMillis = end)

    @Test
    fun addsUpIntervalsPerAppLargestFirst() {
        val totals = AppUsageTotals.within(
            listOf(interval("a", 0, 100), interval("b", 100, 400), interval("a", 500, 600)),
            fromEpochMillis = 0,
            toEpochMillis = 1_000,
        )

        assertEquals(listOf(AppUsageTotal("b", 300), AppUsageTotal("a", 200)), totals)
    }

    @Test
    fun intervalAcrossMidnight_countsOnlyThePartInsideTheDay() {
        val dayStart = 1_000L
        val dayEnd = 2_000L
        val intervals = listOf(interval("a", 900, 1_100), interval("b", 1_950, 2_300))

        assertEquals(
            listOf(AppUsageTotal("a", 100), AppUsageTotal("b", 50)),
            AppUsageTotals.within(intervals, dayStart, dayEnd),
        )
    }

    @Test
    fun intervalsOutsideThePeriod_areIgnored() {
        val totals = AppUsageTotals.within(
            listOf(interval("a", 0, 100), interval("b", 2_000, 2_500)),
            fromEpochMillis = 100,
            toEpochMillis = 2_000,
        )

        assertTrue(totals.isEmpty())
    }

    @Test
    fun equalTotals_areOrderedByPackageName() {
        val totals = AppUsageTotals.within(
            listOf(interval("b", 0, 100), interval("a", 100, 200)),
            fromEpochMillis = 0,
            toEpochMillis = 1_000,
        )

        assertEquals(listOf("a", "b"), totals.map { it.packageName })
    }
}
