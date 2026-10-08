package com.kinsync.android.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kinsync.android.R

/** Signals listed on the consent screen; keep in step with CURRENT_CONSENT_VERSION. */
private val CONSENT_SIGNALS = listOf(
    R.string.consent_signal_unlocks,
    R.string.consent_signal_app_usage,
    R.string.consent_signal_last_moved,
    R.string.consent_signal_activity,
)

/**
 * Plain-language consent (FR-7.1). Shown as the first onboarding step and again after an
 * upgrade that adds new signals ([isReconsent]). Declining stops all collection (FR-7.3).
 */
@Composable
fun ConsentScreen(
    isReconsent: Boolean,
    onAgree: () -> Unit,
    onDecline: () -> Unit,
) {
    var hasDeclined by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        val title = if (isReconsent) R.string.consent_title_updated else R.string.consent_title
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        if (isReconsent) {
            Text(stringResource(R.string.consent_updated_intro), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
        }
        Text(stringResource(R.string.consent_intro), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        CONSENT_SIGNALS.forEach { signal ->
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                Text("•", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(signal), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.consent_privacy), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onAgree, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.consent_agree))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                hasDeclined = true
                onDecline()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.consent_decline))
        }
        if (hasDeclined) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.consent_declined_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
