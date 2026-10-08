package com.kinsync.android.collector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kinsync.android.KinSyncApplication

/**
 * Installing an app update stops [MonitoringService]. Android then sends this app
 * `MY_PACKAGE_REPLACED`, so monitoring resumes without anyone opening the app, but only when
 * the elder agreed to the current consent text (FR-7.1).
 */
class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val container = (context.applicationContext as KinSyncApplication).container
        if (container.consentManager.currentState().canCollect) {
            MonitoringService.start(context)
        }
    }
}
