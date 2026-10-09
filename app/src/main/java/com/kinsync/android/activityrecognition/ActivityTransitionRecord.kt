package com.kinsync.android.activityrecognition

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The elder started or stopped being still, walking or in a vehicle. Only the activity, the
 * kind of change and the time are stored: no location, no sensor samples (FR-2.3, NFR-1).
 *
 * Google Play services can deliver the same transition twice (for example after the app
 * registers again), so a transition is unique on activity, kind and time.
 */
@Entity(
    tableName = "activity_transitions",
    indices = [Index(value = ["activity", "kind", "timestampEpochMillis"], unique = true)],
)
data class ActivityTransitionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activity: CoarseActivity,
    val kind: TransitionKind,
    val timestampEpochMillis: Long,
)
