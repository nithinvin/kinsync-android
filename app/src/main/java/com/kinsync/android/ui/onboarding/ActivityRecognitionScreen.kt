package com.kinsync.android.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.kinsync.android.permissions.ActivityRecognitionPermission

/**
 * Asks for the `ACTIVITY_RECOGNITION` runtime permission with a plain-language reason (FR-2.3).
 * Shown during onboarding after usage access, and from the main screen when the permission is
 * off. The elder can always continue without it; only the activity signal is then missing.
 */
@Composable
fun ActivityRecognitionScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(ActivityRecognitionPermission.isGranted(context)) }
    var wasDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        isGranted = granted
        wasDenied = !granted
    }

    // The elder may turn the permission on in Settings and come back.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = ActivityRecognitionPermission.isGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ActivityRecognitionContent(
        isGranted = isGranted,
        wasDenied = wasDenied,
        onAllow = {
            val permission = ActivityRecognitionPermission.runtimePermission
            if (permission != null) {
                permissionLauncher.launch(permission)
            }
        },
        onOpenSettings = { context.startActivity(ActivityRecognitionPermission.appSettingsIntent(context)) },
        onContinue = onContinue,
    )
}

/** Stateless screen content, so it can be shown in tests without asking Android. */
@Composable
fun ActivityRecognitionContent(
    isGranted: Boolean,
    wasDenied: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.activity_permission_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.activity_permission_body), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))
        when {
            isGranted -> {
                Text(stringResource(R.string.activity_permission_granted), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.activity_permission_continue))
                }
            }
            wasDenied -> {
                Text(stringResource(R.string.activity_permission_denied), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.activity_permission_open_settings))
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.activity_permission_continue))
                }
            }
            else -> {
                Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.activity_permission_allow))
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.activity_permission_not_now))
                }
            }
        }
    }
}
