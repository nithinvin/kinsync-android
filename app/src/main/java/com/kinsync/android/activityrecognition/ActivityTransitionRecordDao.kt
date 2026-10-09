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

    @Query("SELECT COUNT(*) FROM activity_transitions")
    suspend fun count(): Int
}
