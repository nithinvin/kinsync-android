package com.kinsync.android.movement

/** What the app can say about when the phone last moved. */
sealed interface LastMovedStatus {
    /** The phone has no significant-motion sensor. */
    data object NotAvailable : LastMovedStatus

    /** The sensor works, but no movement has been recorded yet. */
    data object NotYet : LastMovedStatus

    data class MovedAt(val timestampEpochMillis: Long) : LastMovedStatus

    companion object {
        fun of(isSensorAvailable: Boolean, latest: MovementEvent?): LastMovedStatus = when {
            latest != null -> MovedAt(latest.timestampEpochMillis)
            !isSensorAvailable -> NotAvailable
            else -> NotYet
        }
    }
}
