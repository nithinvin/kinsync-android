package com.kinsync.android.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kinsync.android.R
import com.kinsync.android.activityrecognition.CoarseActivity
import com.kinsync.android.movement.LastMovedStatus
import com.kinsync.android.ui.common.rememberActivityRecognitionGranted
import com.kinsync.android.ui.common.usageDurationText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The elder's day at a glance: the app's main screen (FR-2.5 precursor). */
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel,
    onAllowActivityRecognition: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val lastMoved by viewModel.lastMoved.collectAsState()

    SummaryContent(
        today = viewModel.today,
        state = state,
        lastMoved = lastMoved,
        isUsageAccessGranted = viewModel.isUsageAccessGranted,
        isActivityRecognitionGranted = rememberActivityRecognitionGranted(),
        onAllowActivityRecognition = onAllowActivityRecognition,
        onOpenTimeline = onOpenTimeline,
        onOpenDetails = onOpenDetails,
    )
}

/** Stateless screen content, so it can be shown in tests without a database. */
@Composable
fun SummaryContent(
    today: LocalDate,
    state: SummaryUiState?,
    lastMoved: LastMovedStatus,
    isUsageAccessGranted: Boolean,
    isActivityRecognitionGranted: Boolean,
    onAllowActivityRecognition: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenDetails: () -> Unit,
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state == null) {
                item(key = "loading") { Text(stringResource(R.string.summary_loading)) }
            } else {
                item(key = "phone_use") { PhoneUseCard(state, today, zoneId) }
                item(key = "top_apps") { TopAppsCard(state, isUsageAccessGranted) }
            }
            item(key = "last_moved") { LastMovedCard(lastMoved, today, zoneId) }
            if (state != null) {
                item(key = "activity") {
                    ActivityCard(state, isActivityRecognitionGranted, onAllowActivityRecognition)
                }
            }
            item(key = "updated_note") {
                Text(stringResource(R.string.summary_updated_note), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onOpenTimeline, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.summary_open_timeline))
        }
        OutlinedButton(onClick = onOpenDetails, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.summary_open_details))
        }
    }
}

@Composable
private fun SummaryCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/** One "label …… value" line inside a card. */
@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PhoneUseCard(state: SummaryUiState, today: LocalDate, zoneId: ZoneId) {
    val unlocks = state.summary.unlocks
    SummaryCard(stringResource(R.string.summary_phone_use_title)) {
        val firstUnlock = unlocks.firstUnlockEpochMillis
        SummaryLine(
            stringResource(R.string.summary_first_unlock),
            if (firstUnlock == null) {
                stringResource(R.string.summary_not_yet)
            } else {
                timeText(firstUnlock, today, zoneId)
            },
        )
        SummaryLine(stringResource(R.string.summary_unlock_count), unlocks.unlockCount.toString())
        SummaryLine(stringResource(R.string.summary_screen_time), usageDurationText(state.summary.screenTimeMillis))
    }
}

@Composable
private fun TopAppsCard(state: SummaryUiState, isUsageAccessGranted: Boolean) {
    SummaryCard(stringResource(R.string.summary_top_apps_title)) {
        when {
            !isUsageAccessGranted -> Text(stringResource(R.string.debug_app_usage_no_access))
            state.topApps.isEmpty() -> Text(stringResource(R.string.summary_top_apps_empty))
            else -> state.topApps.forEach { row -> SummaryLine(row.label, usageDurationText(row.totalMillis)) }
        }
    }
}

@Composable
private fun LastMovedCard(status: LastMovedStatus, today: LocalDate, zoneId: ZoneId) {
    SummaryCard(stringResource(R.string.debug_last_moved_title)) {
        Text(
            when (status) {
                LastMovedStatus.NotAvailable -> stringResource(R.string.debug_last_moved_not_available)
                LastMovedStatus.NotYet -> stringResource(R.string.debug_last_moved_not_yet)
                is LastMovedStatus.MovedAt -> timeText(status.timestampEpochMillis, today, zoneId)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ActivityCard(
    state: SummaryUiState,
    isActivityRecognitionGranted: Boolean,
    onAllowActivityRecognition: () -> Unit,
) {
    val activityMillis = state.summary.activityMillis
    SummaryCard(stringResource(R.string.summary_activity_title)) {
        if (activityMillis.isNotEmpty()) {
            CoarseActivity.entries.forEach { activity ->
                val millis = activityMillis[activity] ?: 0L
                SummaryLine(
                    activityLabel(activity),
                    if (millis > 0L) usageDurationText(millis) else stringResource(R.string.summary_activity_none),
                )
            }
        }
        if (!isActivityRecognitionGranted) {
            Text(stringResource(R.string.debug_activity_no_permission))
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onAllowActivityRecognition, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.debug_activity_allow))
            }
        } else if (activityMillis.isEmpty()) {
            Text(stringResource(R.string.summary_activity_empty))
        }
    }
}

@Composable
private fun activityLabel(activity: CoarseActivity): String = when (activity) {
    CoarseActivity.STILL -> stringResource(R.string.summary_activity_still)
    CoarseActivity.WALKING -> stringResource(R.string.summary_activity_walking)
    CoarseActivity.IN_VEHICLE -> stringResource(R.string.summary_activity_in_vehicle)
}

/** "08:59" for today, "8 Oct 2026, 21:10" for an earlier day. */
@Composable
private fun timeText(epochMillis: Long, today: LocalDate, zoneId: ZoneId): String {
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
    val time = dateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    return if (dateTime.toLocalDate() == today) {
        time
    } else {
        stringResource(
            R.string.summary_time_earlier_day,
            dateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            time,
        )
    }
}
