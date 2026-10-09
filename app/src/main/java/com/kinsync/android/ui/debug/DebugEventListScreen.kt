package com.kinsync.android.ui.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kinsync.android.R
import com.kinsync.android.activityrecognition.ActivityTransitionRecord
import com.kinsync.android.activityrecognition.CoarseActivity
import com.kinsync.android.activityrecognition.CurrentActivityStatus
import com.kinsync.android.activityrecognition.TransitionKind
import com.kinsync.android.collector.UnlockEvent
import com.kinsync.android.movement.LastMovedStatus
import com.kinsync.android.network.HealthCheckResult
import com.kinsync.android.ui.common.AppUsageRow
import com.kinsync.android.ui.common.rememberActivityRecognitionGranted
import com.kinsync.android.ui.common.usageDurationText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Debug/list screen proving the collection pipeline works end-to-end (plan.md Phase-1). */
@Composable
fun DebugEventListScreen(
    viewModel: DebugViewModel,
    onAllowActivityRecognition: () -> Unit,
    onRevokeConsent: () -> Unit,
) {
    val events by viewModel.events.collectAsState()
    val appUsage by viewModel.appUsageToday.collectAsState()
    val lastMoved by viewModel.lastMoved.collectAsState()
    val recentActivity by viewModel.recentActivityTransitions.collectAsState()
    val health by viewModel.healthStatus.collectAsState()

    val isActivityRecognitionGranted = rememberActivityRecognitionGranted()

    DebugEventListContent(
        health = health,
        isUsageAccessGranted = viewModel.isUsageAccessGranted,
        appUsage = appUsage,
        lastMoved = lastMoved,
        activityStatus = CurrentActivityStatus.of(isActivityRecognitionGranted, recentActivity.firstOrNull()),
        recentActivity = recentActivity,
        events = events,
        onAllowActivityRecognition = onAllowActivityRecognition,
        onRevokeConsent = onRevokeConsent,
    )
}

/** Stateless screen content, so it can be shown in tests without a database. */
@Composable
fun DebugEventListContent(
    health: HealthCheckResult?,
    isUsageAccessGranted: Boolean,
    appUsage: List<AppUsageRow>,
    lastMoved: LastMovedStatus,
    activityStatus: CurrentActivityStatus,
    recentActivity: List<ActivityTransitionRecord>,
    events: List<UnlockEvent>,
    onAllowActivityRecognition: () -> Unit,
    onRevokeConsent: () -> Unit,
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text(stringResource(R.string.debug_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        HealthStatusBanner(health)
        Spacer(Modifier.height(8.dp))
        // One scrolling list for every section, so the button below always stays visible.
        LazyColumn(modifier = Modifier.weight(1f)) {
            item(key = "last_moved_title") { SectionTitle(stringResource(R.string.debug_last_moved_title)) }
            item(key = "last_moved") { SectionMessage(lastMovedText(lastMoved)) }
            item(key = "activity_title") { SectionTitle(stringResource(R.string.debug_activity_title)) }
            item(key = "activity_status") { SectionMessage(activityStatusText(activityStatus)) }
            if (activityStatus == CurrentActivityStatus.NoPermission) {
                item(key = "activity_allow") {
                    OutlinedButton(onClick = onAllowActivityRecognition, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.debug_activity_allow))
                    }
                }
            } else {
                items(recentActivity, key = { "activity_${it.id}" }) { record -> ActivityTransitionRow(record) }
            }
            item(key = "app_usage_title") { SectionTitle(stringResource(R.string.debug_app_usage_title)) }
            when {
                !isUsageAccessGranted -> item(key = "app_usage_no_access") {
                    SectionMessage(stringResource(R.string.debug_app_usage_no_access))
                }
                appUsage.isEmpty() -> item(key = "app_usage_empty") {
                    SectionMessage(stringResource(R.string.debug_app_usage_empty))
                }
                else -> items(appUsage, key = { "app_${it.packageName}" }) { row -> AppUsageRowItem(row) }
            }
            item(key = "unlock_events_title") { SectionTitle(stringResource(R.string.debug_unlock_events_title)) }
            if (events.isEmpty()) {
                item(key = "unlock_events_empty") { SectionMessage(stringResource(R.string.debug_empty)) }
            } else {
                items(events, key = { "unlock_${it.id}" }) { event -> UnlockEventRow(event) }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRevokeConsent, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.debug_revoke_consent))
        }
    }
}

@Composable
private fun HealthStatusBanner(health: HealthCheckResult?) {
    val text = when (health) {
        null -> stringResource(R.string.debug_backend_checking)
        is HealthCheckResult.Success -> stringResource(R.string.debug_backend_ok, health.rawBody)
        is HealthCheckResult.Failure -> stringResource(R.string.debug_backend_error, health.reason)
    }
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SectionMessage(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun AppUsageRowItem(row: AppUsageRow) {
    ListItem(
        headlineContent = { Text(row.label) },
        supportingContent = { Text(usageDurationText(row.totalMillis)) },
    )
}

@Composable
private fun lastMovedText(status: LastMovedStatus): String = when (status) {
    LastMovedStatus.NotAvailable -> stringResource(R.string.debug_last_moved_not_available)
    LastMovedStatus.NotYet -> stringResource(R.string.debug_last_moved_not_yet)
    is LastMovedStatus.MovedAt -> {
        val formatter = remember { SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault()) }
        stringResource(R.string.debug_last_moved_at, formatter.format(Date(status.timestampEpochMillis)))
    }
}

@Composable
private fun activityName(activity: CoarseActivity): String = when (activity) {
    CoarseActivity.STILL -> stringResource(R.string.activity_still)
    CoarseActivity.WALKING -> stringResource(R.string.activity_walking)
    CoarseActivity.IN_VEHICLE -> stringResource(R.string.activity_in_vehicle)
}

@Composable
private fun activityStatusText(status: CurrentActivityStatus): String {
    val formatter = remember { SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault()) }
    return when (status) {
        CurrentActivityStatus.NoPermission -> stringResource(R.string.debug_activity_no_permission)
        CurrentActivityStatus.NotYet -> stringResource(R.string.debug_activity_not_yet)
        is CurrentActivityStatus.Doing -> stringResource(
            R.string.debug_activity_doing,
            activityName(status.activity),
            formatter.format(Date(status.sinceEpochMillis)),
        )
        is CurrentActivityStatus.Stopped -> stringResource(
            R.string.debug_activity_stopped,
            activityName(status.activity),
            formatter.format(Date(status.atEpochMillis)),
        )
    }
}

@Composable
private fun ActivityTransitionRow(record: ActivityTransitionRecord) {
    val formatter = remember { SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault()) }
    val headline = when (record.kind) {
        TransitionKind.ENTER -> stringResource(R.string.debug_activity_started, activityName(record.activity))
        TransitionKind.EXIT -> stringResource(R.string.debug_activity_ended, activityName(record.activity))
    }
    ListItem(
        headlineContent = { Text(headline) },
        supportingContent = { Text(formatter.format(Date(record.timestampEpochMillis))) },
    )
}

private const val DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss"

@Composable
private fun UnlockEventRow(event: UnlockEvent) {
    val formatter = remember { SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault()) }
    ListItem(
        headlineContent = { Text(event.eventType.name) },
        supportingContent = { Text(formatter.format(Date(event.timestampEpochMillis))) },
    )
}
