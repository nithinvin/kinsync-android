package com.kinsync.android.movement

import org.junit.Assert.assertEquals
import org.junit.Test

class LastMovedStatusTest {

    @Test
    fun recordedMovement_showsItsTime() {
        val status = LastMovedStatus.of(isSensorAvailable = true, latest = MovementEvent(timestampEpochMillis = 42L))

        assertEquals(LastMovedStatus.MovedAt(42L), status)
    }

    @Test
    fun sensorButNoMovementYet_isNotYet() {
        assertEquals(LastMovedStatus.NotYet, LastMovedStatus.of(isSensorAvailable = true, latest = null))
    }

    @Test
    fun noSensor_isNotAvailable() {
        assertEquals(LastMovedStatus.NotAvailable, LastMovedStatus.of(isSensorAvailable = false, latest = null))
    }

    @Test
    fun movementStoredEarlier_isShownEvenIfTheSensorIsMissingNow() {
        // Edge case: data restored onto, or kept across, a phone without the sensor.
        val status = LastMovedStatus.of(isSensorAvailable = false, latest = MovementEvent(timestampEpochMillis = 7L))

        assertEquals(LastMovedStatus.MovedAt(7L), status)
    }
}
