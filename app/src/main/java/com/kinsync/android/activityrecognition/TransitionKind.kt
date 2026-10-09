package com.kinsync.android.activityrecognition

import com.google.android.gms.location.ActivityTransition

/** Whether the elder started (ENTER) or stopped (EXIT) an activity. */
enum class TransitionKind(val playServicesType: Int) {
    ENTER(ActivityTransition.ACTIVITY_TRANSITION_ENTER),
    EXIT(ActivityTransition.ACTIVITY_TRANSITION_EXIT),
    ;

    companion object {
        /** Null for transition types Google Play services may add in the future. */
        fun fromPlayServicesType(type: Int): TransitionKind? =
            entries.firstOrNull { it.playServicesType == type }
    }
}
