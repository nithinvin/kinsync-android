package com.kinsync.android.activityrecognition

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.google.android.gms.location.ActivityTransitionResult
import com.kinsync.android.KinSyncApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives the transitions requested by [ActivityTransitionRegistrar] and stores them in Room.
 * Activity is a Phase-2 signal, so it is stored only under the current consent text.
 */
class ActivityTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return

        val container = (context.applicationContext as KinSyncApplication).container
        if (!container.consentManager.currentState().canCollect) return

        val nowEpochMillis = System.currentTimeMillis()
        val nowElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        val records = result.transitionEvents.mapNotNull { event ->
            ActivityTransitionMapper.toRecord(
                activityType = event.activityType,
                transitionType = event.transitionType,
                eventElapsedRealtimeNanos = event.elapsedRealTimeNanos,
                nowEpochMillis = nowEpochMillis,
                nowElapsedRealtimeNanos = nowElapsedRealtimeNanos,
            )
        }
        if (records.isEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.database.activityTransitionRecordDao().insertAll(records)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
