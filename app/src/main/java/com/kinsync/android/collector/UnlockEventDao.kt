package com.kinsync.android.collector

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockEventDao {
    @Insert
    suspend fun insert(event: UnlockEvent): Long

    @Query("SELECT * FROM unlock_events ORDER BY timestampEpochMillis DESC LIMIT :limit")
    fun observeRecentEvents(limit: Int = MAX_DEBUG_SCREEN_EVENTS): Flow<List<UnlockEvent>>

    /** Events inside [fromEpochMillis, toEpochMillis), oldest first. */
    @Query(
        "SELECT * FROM unlock_events " +
            "WHERE timestampEpochMillis >= :fromEpochMillis AND timestampEpochMillis < :toEpochMillis " +
            "ORDER BY timestampEpochMillis",
    )
    fun observeBetween(fromEpochMillis: Long, toEpochMillis: Long): Flow<List<UnlockEvent>>

    @Query("SELECT COUNT(*) FROM unlock_events")
    suspend fun count(): Int

    private companion object {
        const val MAX_DEBUG_SCREEN_EVENTS = 200
    }
}
