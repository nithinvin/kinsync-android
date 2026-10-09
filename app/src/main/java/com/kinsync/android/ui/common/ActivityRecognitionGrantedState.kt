package com.kinsync.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kinsync.android.permissions.ActivityRecognitionPermission

/**
 * Whether the activity-recognition permission is on. Checked again whenever the screen comes
 * back, for example after the permission screen or Settings.
 */
@Composable
fun rememberActivityRecognitionGranted(): Boolean {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(ActivityRecognitionPermission.isGranted(context)) }
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
    return isGranted
}
