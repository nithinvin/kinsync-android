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
import com.kinsync.android.collector.UnlockEvent
import com.kinsync.android.network.HealthCheckResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Debug/list screen proving the collection pipeline works end-to-end (plan.md Phase-1). */
@Composable
fun DebugEventListScreen(
    viewModel: DebugViewModel,
    onRevokeConsent: () -> Unit,
) {
    val events by viewModel.events.collectAsState()
    val health by viewModel.healthStatus.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text(stringResource(R.string.debug_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        HealthStatusBanner(health)
        Spacer(Modifier.height(8.dp))
        if (events.isEmpty()) {
            Text(stringResource(R.string.debug_empty), style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(events, key = { it.id }) { event -> UnlockEventRow(event) }
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
private fun UnlockEventRow(event: UnlockEvent) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    ListItem(
        headlineContent = { Text(event.eventType.name) },
        supportingContent = { Text(formatter.format(Date(event.timestampEpochMillis))) },
    )
}
