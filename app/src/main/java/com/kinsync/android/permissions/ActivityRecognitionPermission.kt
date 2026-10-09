package com.kinsync.android.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * `ACTIVITY_RECOGNITION` is a runtime permission from Android 10 (FR-2.3). On older versions
 * the Google Play services permission in the manifest is granted at install time.
 */
object ActivityRecognitionPermission {

    /** The permission to ask for at runtime, or null where none is needed (before Android 10). */
    val runtimePermission: String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACTIVITY_RECOGNITION else null

    fun isGranted(context: Context): Boolean {
        val permission = runtimePermission ?: return true
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /** KinSync's page in Settings, for when Android no longer shows the permission dialog. */
    fun appSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
}
