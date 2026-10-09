package com.kinsync.android.activityrecognition

/** What the app can say about the elder's current activity. */
sealed interface CurrentActivityStatus {
    /**
     * The activity-recognition permission is off, so nothing new is recorded. Older transitions
     * are not shown as "current", because they may be long out of date.
     */
    data object NoPermission : CurrentActivityStatus

    /** The permission is on, but no transition has been recorded yet. */
    data object NotYet : CurrentActivityStatus

    /** The latest transition started [activity]. */
    data class Doing(val activity: CoarseActivity, val sinceEpochMillis: Long) : CurrentActivityStatus

    /** The latest transition ended [activity] and nothing new has started since. */
    data class Stopped(val activity: CoarseActivity, val atEpochMillis: Long) : CurrentActivityStatus

    companion object {
        fun of(isPermissionGranted: Boolean, latest: ActivityTransitionRecord?): CurrentActivityStatus = when {
            !isPermissionGranted -> NoPermission
            latest == null -> NotYet
            latest.kind == TransitionKind.ENTER -> Doing(latest.activity, latest.timestampEpochMillis)
            else -> Stopped(latest.activity, latest.timestampEpochMillis)
        }
    }
}
