package com.kinsync.android.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageDurationTest {

    @Test
    fun underOneMinute() {
        assertEquals(UsageDuration.UnderAMinute, UsageDuration.of(59_999))
    }

    @Test
    fun zeroOrNegative_isUnderOneMinute() {
        assertEquals(UsageDuration.UnderAMinute, UsageDuration.of(0))
        assertEquals(UsageDuration.UnderAMinute, UsageDuration.of(-5_000))
    }

    @Test
    fun minutesAreRoundedDown() {
        assertEquals(UsageDuration.Minutes(59), UsageDuration.of(59 * 60_000L + 59_999))
    }

    @Test
    fun oneHourOrMore_showsHoursAndMinutes() {
        assertEquals(UsageDuration.HoursAndMinutes(1, 0), UsageDuration.of(60 * 60_000L))
        assertEquals(UsageDuration.HoursAndMinutes(2, 5), UsageDuration.of(125 * 60_000L))
    }
}
