package com.kinsync.android.usage

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One stretch of time an app was in the foreground (FR-2.2). Stored on this phone only; app
 * names never leave the device (NFR-1).
 *
 * The same interval can be built twice when collection runs overlap, so
 * (packageName, startEpochMillis) is unique and a repeat replaces the earlier row.
 */
@Entity(
    tableName = "app_usage_intervals",
    indices = [Index(value = ["packageName", "startEpochMillis"], unique = true)],
)
data class AppUsageInterval(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
) {
    val durationMillis: Long
        get() = endEpochMillis - startEpochMillis
}
