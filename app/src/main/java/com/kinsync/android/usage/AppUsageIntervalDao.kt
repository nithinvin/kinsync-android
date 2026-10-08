package com.kinsync.android.usage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageIntervalDao {
    /** Inserts the intervals; an interval with the same app and start time replaces the old row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(intervals: List<AppUsageInterval>)

    /** Intervals that overlap [fromEpochMillis, toEpochMillis), oldest first. */
    @Query(
        "SELECT * FROM app_usage_intervals " +
            "WHERE endEpochMillis > :fromEpochMillis AND startEpochMillis < :toEpochMillis " +
            "ORDER BY startEpochMillis",
    )
    fun observeOverlapping(fromEpochMillis: Long, toEpochMillis: Long): Flow<List<AppUsageInterval>>

    @Query("SELECT COUNT(*) FROM app_usage_intervals")
    suspend fun count(): Int
}
