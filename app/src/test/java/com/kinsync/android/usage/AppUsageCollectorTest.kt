package com.kinsync.android.usage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUsageCollectorTest {

    private class FakeDao : AppUsageIntervalDao {
        val stored = mutableMapOf<Pair<String, Long>, AppUsageInterval>()

        override suspend fun upsertAll(intervals: List<AppUsageInterval>) {
            intervals.forEach { stored[it.packageName to it.startEpochMillis] = it }
        }

        override fun observeOverlapping(fromEpochMillis: Long, toEpochMillis: Long): Flow<List<AppUsageInterval>> =
            flowOf(stored.values.toList())

        override suspend fun count(): Int = stored.size
    }

    private class FakeCursorStore(var value: Long? = null) : AppUsageCursorStore {
        override fun read(): Long? = value

        override fun write(epochMillis: Long) {
            value = epochMillis
        }
    }

    /** Remembers each requested range and returns the events inside it. */
    private class FakeSource(private val allEvents: List<UsageEventRecord>) : UsageEventSource {
        val requestedRanges = mutableListOf<LongRange>()

        override fun events(fromEpochMillis: Long, toEpochMillis: Long): List<UsageEventRecord> {
            requestedRanges.add(fromEpochMillis..toEpochMillis)
            return allEvents.filter { it.timestampEpochMillis in fromEpochMillis until toEpochMillis }
        }
    }

    private val dao = FakeDao()
    private val cursor = FakeCursorStore()
    private var now = NOW

    private fun collector(source: UsageEventSource) = AppUsageCollector(source, dao, cursor) { now }

    private fun event(time: Long, kind: UsageEventKind, app: String = APP) =
        UsageEventRecord(app, "Main", time, kind)

    @Test
    fun firstRun_readsTheLastDayAndMovesTheCursorToNow() = runTest {
        val source = FakeSource(emptyList())

        collector(source).collect()

        assertEquals(NOW - AppUsageCollector.LOOKBACK_LIMIT_MILLIS, source.requestedRanges.single().first)
        assertEquals(NOW, cursor.value)
    }

    @Test
    fun finishedIntervals_areStored() = runTest {
        val source = FakeSource(
            listOf(event(NOW - 5_000, UsageEventKind.FOREGROUND), event(NOW - 1_000, UsageEventKind.BACKGROUND)),
        )

        val stored = collector(source).collect()

        assertEquals(1, stored)
        assertEquals(4_000L, dao.stored.values.single().durationMillis)
    }

    @Test
    fun appStillOpen_isFinishedByTheNextRun() = runTest {
        val events = listOf(
            event(NOW - 5_000, UsageEventKind.FOREGROUND),
            event(NOW + 10_000, UsageEventKind.BACKGROUND),
        )
        val source = FakeSource(events)
        val collector = collector(source)

        collector.collect()
        assertTrue(dao.stored.isEmpty())
        assertEquals(NOW - 5_000, cursor.value)

        now = NOW + 60_000
        collector.collect()

        assertEquals(NOW - 5_000, source.requestedRanges.last().first)
        assertEquals(15_000L, dao.stored.values.single().durationMillis)
        assertEquals(NOW + 60_000, cursor.value)
    }

    @Test
    fun overlappingRuns_doNotDuplicateIntervals() = runTest {
        val events = listOf(
            event(NOW - 9_000, UsageEventKind.FOREGROUND, app = "other"),
            event(NOW - 5_000, UsageEventKind.FOREGROUND),
            event(NOW - 1_000, UsageEventKind.BACKGROUND),
            event(NOW + 2_000, UsageEventKind.BACKGROUND, app = "other"),
        )
        val collector = collector(FakeSource(events))

        collector.collect()
        now = NOW + 60_000
        collector.collect()

        assertEquals(2, dao.count())
    }

    @Test
    fun veryOldCursor_isLimitedToTheLastDay() = runTest {
        cursor.value = NOW - 10 * AppUsageCollector.LOOKBACK_LIMIT_MILLIS
        val source = FakeSource(emptyList())

        collector(source).collect()

        assertEquals(NOW - AppUsageCollector.LOOKBACK_LIMIT_MILLIS, source.requestedRanges.single().first)
    }

    @Test
    fun cursorInTheFuture_afterTheClockMovedBack_readsTheLastDay() = runTest {
        cursor.value = NOW + 3_600_000
        val source = FakeSource(emptyList())

        collector(source).collect()

        assertEquals(NOW - AppUsageCollector.LOOKBACK_LIMIT_MILLIS, source.requestedRanges.single().first)
        assertEquals(NOW, cursor.value)
    }

    private companion object {
        const val NOW = 1_760_000_000_000L
        const val APP = "com.example.maps"
    }
}
