package com.kinsync.android.timeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementBurstsTest {

    private val minute = 60_000L
    private val dayStart = 0L
    private val dayEnd = 24 * 60 * minute

    private fun bursts(timestamps: List<Long>) = MovementBursts.within(timestamps, dayStart, dayEnd)

    @Test
    fun movesCloseTogether_formOneBurst() {
        assertEquals(
            listOf(MovementBurst(firstEpochMillis = 60 * minute, lastEpochMillis = 75 * minute, count = 3)),
            bursts(listOf(60 * minute, 69 * minute, 75 * minute)),
        )
    }

    @Test
    fun aGapOfTenMinutesOrMore_startsANewBurst() {
        assertEquals(
            listOf(MovementBurst(60 * minute, 60 * minute, 1), MovementBurst(70 * minute, 70 * minute, 1)),
            bursts(listOf(70 * minute, 60 * minute)),
        )
    }

    @Test
    fun movesOutsideTheDay_areIgnored() {
        assertTrue(bursts(listOf(-1L, dayEnd)).isEmpty())
    }
}
