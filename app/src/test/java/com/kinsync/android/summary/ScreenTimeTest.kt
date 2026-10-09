package com.kinsync.android.summary

import com.kinsync.android.usage.AppUsageInterval
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTimeTest {

    private fun interval(app: String, start: Long, end: Long) =
        AppUsageInterval(packageName = app, startEpochMillis = start, endEpochMillis = end)

    @Test
    fun separateIntervals_addUp() {
        val total = ScreenTime.total(
            listOf(interval("a", 0, 100), interval("b", 200, 250)),
            fromEpochMillis = 0,
            toEpochMillis = 1_000,
            excludedPackages = emptySet(),
        )

        assertEquals(150L, total)
    }

    @Test
    fun overlappingIntervals_countOnce() {
        // Split screen: two apps in the foreground at the same time.
        val total = ScreenTime.total(
            listOf(interval("a", 0, 100), interval("b", 50, 150), interval("c", 60, 80)),
            fromEpochMillis = 0,
            toEpochMillis = 1_000,
            excludedPackages = emptySet(),
        )

        assertEquals(150L, total)
    }

    @Test
    fun homeScreen_isLeftOut() {
        val total = ScreenTime.total(
            listOf(interval("launcher", 0, 500), interval("a", 500, 600)),
            fromEpochMillis = 0,
            toEpochMillis = 1_000,
            excludedPackages = setOf("launcher"),
        )

        assertEquals(100L, total)
    }

    @Test
    fun intervalsAcrossTheDayEdges_countOnlyInsideTheDay() {
        val total = ScreenTime.total(
            listOf(interval("a", 900, 1_100), interval("b", 1_950, 2_300)),
            fromEpochMillis = 1_000,
            toEpochMillis = 2_000,
            excludedPackages = emptySet(),
        )

        assertEquals(150L, total)
    }

    @Test
    fun noIntervals_isZero() {
        assertEquals(0L, ScreenTime.total(emptyList(), 0, 1_000, emptySet()))
    }

    @Test
    fun malformedIntervalEndingBeforeItStarts_isIgnored() {
        val total = ScreenTime.total(
            listOf(interval("a", 500, 400), interval("b", 0, 10)),
            fromEpochMillis = 0,
            toEpochMillis = 1_000,
            excludedPackages = emptySet(),
        )

        assertEquals(10L, total)
    }
}
