package com.kinsync.android.activityrecognition

import com.google.android.gms.location.DetectedActivity

/**
 * The only activities KinSync asks Google Play services about (FR-2.3). Running, cycling and
 * "tilting" are left out on purpose: the daily pattern only needs still / walking / travelling.
 */
enum class CoarseActivity(val detectedActivityType: Int) {
    STILL(DetectedActivity.STILL),
    WALKING(DetectedActivity.WALKING),
    IN_VEHICLE(DetectedActivity.IN_VEHICLE),
    ;

    companion object {
        /** Null for activity types KinSync does not record. */
        fun fromDetectedActivityType(type: Int): CoarseActivity? =
            entries.firstOrNull { it.detectedActivityType == type }
    }
}
