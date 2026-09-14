package com.kinsync.android.permissions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/** Exempts the app from battery optimization so collection can't be silently killed (NFR-2). */
object BatteryOptimizationPermission {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    // Deliberate use of the direct-request intent (normally discouraged by Play policy): the
    // elder explicitly opts in via the onboarding rationale screen, and NFR-2 requires this app
    // to survive OEM battery management for the dead-man's-switch design to work at all.
    @SuppressLint("BatteryLife")
    fun requestIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        )
}
