package com.kinsync.android.activityrecognition

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityTransitionRecordDao {
    /** Transitions already stored are skipped, so a repeated delivery is not counted twice. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<ActivityTransitionRecord>)

    /** The newest [limit] transitions, newest first. */
    @Query("SELECT * FROM activity_transitions ORDER BY timestampEpochMillis DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ActivityTransitionRecord>>

    /** Transitions inside [fromEpochMillis, toEpochMillis), oldest first. */
    @Query(
        "SELECT * FROM activity_transitions " +
            "WHERE timestampEpochMillis >= :fromEpochMillis AND timestampEpochMillis < :toEpochMillis " +
            "ORDER BY timestampEpochMillis, id",
    )
    fun observeBetween(fromEpochMillis: Long, toEpochMillis: Long): Flow<List<ActivityTransitionRecord>>

    /** The last transition before [epochMillis]: what the elder was doing when a day started. */
    @Query(
        "SELECT * FROM activity_transitions WHERE timestampEpochMillis < :epochMillis " +
            "ORDER BY timestampEpochMillis DESC, id DESC LIMIT 1",
    )
    fun observeLatestBefore(epochMillis: Long): Flow<ActivityTransitionRecord?>

    @Query("SELECT COUNT(*) FROM activity_transitions")
    suspend fun count(): Int
}
