package com.kinsync.android.collector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Captures screen-lock/unlock broadcasts (FR-2.1). Must be registered dynamically — Android
 * blocks manifest-declared receivers for these implicit system broadcasts since API 26.
 */
class UnlockEventReceiver(
    private val onEvent: (UnlockEventType, Long) -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventType = when (intent.action) {
            Intent.ACTION_SCREEN_ON -> UnlockEventType.SCREEN_ON
            Intent.ACTION_SCREEN_OFF -> UnlockEventType.SCREEN_OFF
            Intent.ACTION_USER_PRESENT -> UnlockEventType.USER_PRESENT
            else -> return
        }
        onEvent(eventType, System.currentTimeMillis())
    }

    companion object {
        fun intentFilter(): IntentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
    }
}
