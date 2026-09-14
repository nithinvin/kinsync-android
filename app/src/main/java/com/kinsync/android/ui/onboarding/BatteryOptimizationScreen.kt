package com.kinsync.android.ui.onboarding

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kinsync.android.R
import com.kinsync.android.permissions.BatteryOptimizationPermission

/**
 * Third onboarding step — battery-optimization allowlist prompt (plan.md Phase-1) plus, on
 * API 33+, the runtime `POST_NOTIFICATIONS` permission needed to show the monitoring notification.
 */
@Composable
fun BatteryOptimizationScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    var ignoringOptimizations by remember {
        mutableStateOf(BatteryOptimizationPermission.isIgnoringBatteryOptimizations(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* no-op: foreground service still runs even if the user declines the notification */ }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                ignoringOptimizations = BatteryOptimizationPermission.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.battery_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.battery_body), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))
        OutlinedButton(
            onClick = { context.startActivity(BatteryOptimizationPermission.requestIntent(context)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.battery_open_settings))
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                onContinue()
            },
            enabled = ignoringOptimizations,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.battery_continue))
        }
    }
}
