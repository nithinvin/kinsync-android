package com.kinsync.android.ui.onboarding

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
import com.kinsync.android.permissions.UsageAccessPermission

/** Second onboarding step — special-permission rationale + Settings deep-link (FR-2.2). */
@Composable
fun UsageAccessScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(UsageAccessPermission.isGranted(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = UsageAccessPermission.isGranted(context)
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
        Text(stringResource(R.string.usage_access_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.usage_access_body), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))
        OutlinedButton(
            onClick = { context.startActivity(UsageAccessPermission.settingsIntent()) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.usage_access_open_settings))
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onContinue, enabled = granted, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.usage_access_continue))
        }
    }
}
