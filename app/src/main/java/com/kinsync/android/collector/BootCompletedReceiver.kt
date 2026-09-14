package com.kinsync.android.collector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kinsync.android.KinSyncApplication

/** Restarts monitoring after reboot, but only if the elder has already consented (FR-2.4). */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val container = (context.applicationContext as KinSyncApplication).container
        if (container.consentManager.currentState().onboardingComplete) {
            MonitoringService.start(context)
        }
    }
}
