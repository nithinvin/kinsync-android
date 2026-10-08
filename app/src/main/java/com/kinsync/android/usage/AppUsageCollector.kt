package com.kinsync.android.usage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Reads new usage events and stores them as foreground intervals (FR-2.2).
 *
 * Each run reads from the stored cursor up to now. The cursor then moves to now, or back to
 * the start of an app that is still in the foreground, so that interval is finished by a later
 * run. Intervals read twice replace themselves (see [AppUsageInterval]).
 */
class AppUsageCollector(
    private val eventSource: UsageEventSource,
    private val dao: AppUsageIntervalDao,
    private val cursorStore: AppUsageCursorStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** The periodic and the on-app-open collection can overlap; they run one after the other. */
    private val mutex = Mutex()

    /** Collects new intervals and returns how many were stored. */
    suspend fun collect(): Int = mutex.withLock {
        val now = clock()
        val from = collectFrom(now)
        val result = AppUsageIntervalBuilder.build(eventSource.events(from, now))
        if (result.intervals.isNotEmpty()) {
            dao.upsertAll(result.intervals)
        }
        cursorStore.write(result.earliestOpenStartEpochMillis ?: now)
        result.intervals.size
    }

    /**
     * Never reads further back than [LOOKBACK_LIMIT_MILLIS]: on the first run, after the clock
     * moved backwards, or when an app seems to have stayed in the foreground for a whole day
     * (a missing background event), which would otherwise hold the cursor back forever.
     */
    private fun collectFrom(now: Long): Long {
        val earliest = now - LOOKBACK_LIMIT_MILLIS
        val stored = cursorStore.read()
        return if (stored == null || stored > now) earliest else maxOf(stored, earliest)
    }

    companion object {
        const val LOOKBACK_LIMIT_MILLIS = 24L * 60 * 60 * 1000
    }
}
