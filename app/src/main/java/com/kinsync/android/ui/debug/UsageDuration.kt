package com.kinsync.android.ui.debug

/** A foreground time, rounded down to whole minutes, in the form the debug screen shows it. */
sealed interface UsageDuration {
    data object UnderAMinute : UsageDuration

    data class Minutes(val minutes: Long) : UsageDuration

    data class HoursAndMinutes(val hours: Long, val minutes: Long) : UsageDuration

    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val MINUTES_PER_HOUR = 60L

        fun of(millis: Long): UsageDuration {
            val totalMinutes = millis / MILLIS_PER_MINUTE
            return when {
                totalMinutes < 1 -> UnderAMinute
                totalMinutes < MINUTES_PER_HOUR -> Minutes(totalMinutes)
                else -> HoursAndMinutes(totalMinutes / MINUTES_PER_HOUR, totalMinutes % MINUTES_PER_HOUR)
            }
        }
    }
}
