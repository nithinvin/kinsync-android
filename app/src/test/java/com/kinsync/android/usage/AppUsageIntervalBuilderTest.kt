package com.kinsync.android.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUsageIntervalBuilderTest {

    private fun foreground(time: Long, app: String = MAPS, activity: String = "Main") =
        UsageEventRecord(app, activity, time, UsageEventKind.FOREGROUND)

    private fun background(time: Long, app: String = MAPS, activity: String = "Main") =
        UsageEventRecord(app, activity, time, UsageEventKind.BACKGROUND)

    private fun shutdown(time: Long) = UsageEventRecord("android", null, time, UsageEventKind.DEVICE_SHUTDOWN)

    private fun interval(app: String, start: Long, end: Long) =
        AppUsageInterval(packageName = app, startEpochMillis = start, endEpochMillis = end)

    @Test
    fun foregroundThenBackground_buildsOneInterval() {
        val result = AppUsageIntervalBuilder.build(listOf(foreground(1_000), background(61_000)))

        assertEquals(listOf(interval(MAPS, 1_000, 61_000)), result.intervals)
        assertNull(result.earliestOpenStartEpochMillis)
    }

    @Test
    fun twoAppsOneAfterTheOther_buildsAnIntervalEach() {
        val result = AppUsageIntervalBuilder.build(
            listOf(
                foreground(1_000, MAPS),
                background(10_000, MAPS),
                foreground(10_100, CHAT),
                background(30_000, CHAT),
            ),
        )

        assertEquals(listOf(interval(MAPS, 1_000, 10_000), interval(CHAT, 10_100, 30_000)), result.intervals)
    }

    @Test
    fun eventsOutOfOrder_areSortedFirst() {
        val result = AppUsageIntervalBuilder.build(listOf(background(61_000), foreground(1_000)))

        assertEquals(listOf(interval(MAPS, 1_000, 61_000)), result.intervals)
    }

    @Test
    fun noEvents_buildsNothing() {
        val result = AppUsageIntervalBuilder.build(emptyList())

        assertTrue(result.intervals.isEmpty())
        assertNull(result.earliestOpenStartEpochMillis)
    }

    @Test
    fun backgroundWithoutForeground_isSkipped() {
        // The app came to the foreground before the events start, so the length is unknown.
        val result = AppUsageIntervalBuilder.build(listOf(background(5_000), foreground(9_000, CHAT), background(20_000, CHAT)))

        assertEquals(listOf(interval(CHAT, 9_000, 20_000)), result.intervals)
    }

    @Test
    fun appStillInForeground_isNotStoredAndHoldsTheCursor() {
        val result = AppUsageIntervalBuilder.build(
            listOf(foreground(1_000, CHAT), background(5_000, CHAT), foreground(7_000, MAPS)),
        )

        assertEquals(listOf(interval(CHAT, 1_000, 5_000)), result.intervals)
        assertEquals(7_000L, result.earliestOpenStartEpochMillis)
    }

    @Test
    fun severalAppsStillOpen_cursorIsTheOldestStart() {
        val result = AppUsageIntervalBuilder.build(listOf(foreground(9_000, CHAT), foreground(4_000, MAPS)))

        assertEquals(4_000L, result.earliestOpenStartEpochMillis)
    }

    @Test
    fun repeatedForeground_keepsTheFirstStart() {
        val result = AppUsageIntervalBuilder.build(listOf(foreground(1_000), foreground(3_000), background(9_000)))

        assertEquals(listOf(interval(MAPS, 1_000, 9_000)), result.intervals)
    }

    @Test
    fun zeroLengthInterval_isSkipped() {
        val result = AppUsageIntervalBuilder.build(listOf(foreground(1_000), background(1_000)))

        assertTrue(result.intervals.isEmpty())
    }

    @Test
    fun blankPackageName_isSkipped() {
        val result = AppUsageIntervalBuilder.build(listOf(foreground(1_000, app = ""), background(5_000, app = "")))

        assertTrue(result.intervals.isEmpty())
        assertNull(result.earliestOpenStartEpochMillis)
    }

    @Test
    fun shutdown_closesEveryOpenApp() {
        val result = AppUsageIntervalBuilder.build(listOf(foreground(1_000, MAPS), foreground(2_000, CHAT), shutdown(8_000)))

        assertEquals(listOf(interval(MAPS, 1_000, 8_000), interval(CHAT, 2_000, 8_000)), result.intervals)
        assertNull(result.earliestOpenStartEpochMillis)
    }

    @Test
    fun movingBetweenScreensOfOneApp_isMergedIntoOneVisit() {
        val result = AppUsageIntervalBuilder.build(
            listOf(
                foreground(1_000, activity = "List"),
                background(5_000, activity = "List"),
                foreground(5_300, activity = "Detail"),
                background(9_000, activity = "Detail"),
            ),
        )

        assertEquals(listOf(interval(MAPS, 1_000, 9_000)), result.intervals)
    }

    @Test
    fun twoVisitsFarApart_stayTwoIntervals() {
        val gap = AppUsageIntervalBuilder.MERGE_GAP_MILLIS + 1
        val result = AppUsageIntervalBuilder.build(
            listOf(foreground(1_000), background(5_000), foreground(5_000 + gap), background(20_000)),
        )

        assertEquals(listOf(interval(MAPS, 1_000, 5_000), interval(MAPS, 5_000 + gap, 20_000)), result.intervals)
    }

    @Test
    fun overlappingActivitiesOfOneApp_areMerged() {
        // Split screen or picture-in-picture: two activities of one app in the foreground at once.
        val result = AppUsageIntervalBuilder.build(
            listOf(
                foreground(1_000, activity = "A"),
                foreground(2_000, activity = "B"),
                background(6_000, activity = "A"),
                background(9_000, activity = "B"),
            ),
        )

        assertEquals(listOf(interval(MAPS, 1_000, 9_000)), result.intervals)
    }

    @Test
    fun intervalAcrossMidnight_staysOneInterval() {
        val midnight = 1_760_000_000_000L
        val result = AppUsageIntervalBuilder.build(listOf(foreground(midnight - 600_000), background(midnight + 300_000)))

        assertEquals(listOf(interval(MAPS, midnight - 600_000, midnight + 300_000)), result.intervals)
    }

    private companion object {
        const val MAPS = "com.example.maps"
        const val CHAT = "com.example.chat"
    }
}
