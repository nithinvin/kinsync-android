package com.kinsync.android.activityrecognition

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.kinsync.android.permissions.ActivityRecognitionPermission

/**
 * Asks Google Play services to report when the elder starts or stops being still, walking or
 * in a vehicle (FR-2.3). Play services forgets the request after a reboot or an app update, so
 * [MonitoringService][com.kinsync.android.collector.MonitoringService] registers again every
 * time it starts. Registering twice replaces the earlier request.
 */
class ActivityTransitionRegistrar(private val context: Context) {

    /** Registers when the permission is granted; does nothing otherwise. */
    @SuppressLint("MissingPermission") // Checked just above the call.
    fun registerIfPermitted() {
        if (!ActivityRecognitionPermission.isGranted(context)) return
        ActivityRecognition.getClient(context)
            .requestActivityTransitionUpdates(transitionRequest(), pendingIntent())
            .addOnFailureListener { error -> Log.w(TAG, "Activity transition registration failed", error) }
    }

    @SuppressLint("MissingPermission") // Removing a request needs no permission in practice.
    fun unregister() {
        if (!ActivityRecognitionPermission.isGranted(context)) return
        ActivityRecognition.getClient(context)
            .removeActivityTransitionUpdates(pendingIntent())
            .addOnFailureListener { error -> Log.w(TAG, "Activity transition removal failed", error) }
    }

    private fun transitionRequest(): ActivityTransitionRequest {
        val transitions = CoarseActivity.entries.flatMap { activity ->
            TransitionKind.entries.map { kind ->
                ActivityTransition.Builder()
                    .setActivityType(activity.detectedActivityType)
                    .setActivityTransition(kind.playServicesType)
                    .build()
            }
        }
        return ActivityTransitionRequest(transitions)
    }

    private fun pendingIntent(): PendingIntent {
        // Play services adds the transitions to this intent, so it must be mutable (Android 12+).
        val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ActivityTransitionReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag,
        )
    }

    private companion object {
        const val TAG = "ActivityTransitions"
        const val REQUEST_CODE = 4001
    }
}
