package com.kinsync.android.timeline

import com.kinsync.android.collector.UnlockEvent
import com.kinsync.android.collector.UnlockEventType
import com.kinsync.android.collector.UnlockEventType.SCREEN_OFF
import com.kinsync.android.collector.UnlockEventType.SCREEN_ON
import com.kinsync.android.collector.UnlockEventType.USER_PRESENT
import com.kinsync.android.usage.AppUsageInterval
import com.kinsync.android.usage.AppUsageTotal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneSessionsTest {

    private val dayStart = 1_000L
    private val dayEnd = 9_000L

    private fun e(type: UnlockEventType, at: Long) = UnlockEvent(eventType = type, timestampEpochMillis = at)

    private fun app(packageName: String, start: Long, end: Long) =
        AppUsageInterval(packageName = packageName, startEpochMillis = start, endEpochMillis = end)

    private fun sessions(
        events: List<UnlockEvent>,
        apps: List<AppUsageInterval> = emptyList(),
        to: Long = dayEnd,
    ) = PhoneSessions.within(events, apps, dayStart, to)

    @Test
    fun unlockAndUse_givesOneUnlockedSessionWithItsApps() {
        val events = listOf(e(SCREEN_ON, 2_000), e(USER_PRESENT, 2_100), e(SCREEN_OFF, 3_000))
        val apps = listOf(
            app("com.whatsapp", 2_100, 2_600),
            app("com.android.chrome", 2_600, 2_900),
            // Used in another session: not part of this one.
            app("com.kindle", 5_000, 5_500),
        )

        assertEquals(
            listOf(
                PhoneSession(
                    startEpochMillis = 2_000,
                    endEpochMillis = 3_000,
                    firstUnlockEpochMillis = 2_100,
                    isOpen = false,
                    apps = listOf(AppUsageTotal("com.whatsapp", 500), AppUsageTotal("com.android.chrome", 300)),
                ),
            ),
            sessions(events, apps),
        )
    }

    @Test
    fun screenOnWithoutUnlock_isNotUnlocked() {
        val result = sessions(listOf(e(SCREEN_ON, 2_000), e(SCREEN_OFF, 2_010)))

        assertEquals(1, result.size)
        assertEquals(false, result.single().isUnlocked)
    }

    @Test
    fun unlockWithoutScreenOn_startsASession() {
        val result = sessions(listOf(e(USER_PRESENT, 2_000), e(SCREEN_OFF, 2_500)))

        assertEquals(listOf(2_000L to 2_500L), result.map { it.startEpochMillis to it.endEpochMillis })
        assertEquals(2_000L, result.single().firstUnlockEpochMillis)
    }

    @Test
    fun twoUnlocksInOneSession_keepTheFirst() {
        val events = listOf(
            e(SCREEN_ON, 2_000),
            e(USER_PRESENT, 2_100),
            e(USER_PRESENT, 2_400),
            e(SCREEN_OFF, 3_000),
        )

        assertEquals(2_100L, sessions(events).single().firstUnlockEpochMillis)
    }

    @Test
    fun missedScreenOff_endsTheOpenSessionAtTheNextScreenOn() {
        val events = listOf(e(SCREEN_ON, 2_000), e(SCREEN_ON, 4_000), e(SCREEN_OFF, 4_500))

        assertEquals(
            listOf(2_000L to 4_000L, 4_000L to 4_500L),
            sessions(events).map { it.startEpochMillis to it.endEpochMillis },
        )
    }

    @Test
    fun screenOffWithoutSession_isIgnored() {
        assertTrue(sessions(listOf(e(SCREEN_OFF, 2_000))).isEmpty())
    }

    @Test
    fun screenStillOnNow_isOpenUntilNow() {
        val result = sessions(listOf(e(SCREEN_ON, 2_000), e(USER_PRESENT, 2_000)), to = 2_700)

        assertEquals(
            listOf(Triple(2_000L, 2_700L, true)),
            result.map { Triple(it.startEpochMillis, it.endEpochMillis, it.isOpen) },
        )
    }

    @Test
    fun eventsOutsideThePeriod_areIgnored() {
        val events = listOf(e(SCREEN_ON, 500), e(SCREEN_OFF, 600), e(SCREEN_ON, 9_500))

        assertTrue(sessions(events).isEmpty())
    }

    @Test
    fun emptyPeriod_isEmpty() {
        // Malformed input: the period ends before it starts (e.g. the clock was set back).
        assertTrue(sessions(listOf(e(SCREEN_ON, 2_000)), to = 900).isEmpty())
    }
}
