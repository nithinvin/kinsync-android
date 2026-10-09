package com.kinsync.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kinsync.android.R

/** A duration in the words the elder sees, such as "1 h 05 min" or "under 1 min". */
@Composable
fun usageDurationText(millis: Long): String = when (val duration = UsageDuration.of(millis)) {
    UsageDuration.UnderAMinute -> stringResource(R.string.duration_under_a_minute)
    is UsageDuration.Minutes -> stringResource(R.string.duration_minutes, duration.minutes)
    is UsageDuration.HoursAndMinutes ->
        stringResource(R.string.duration_hours_minutes, duration.hours, duration.minutes)
}
