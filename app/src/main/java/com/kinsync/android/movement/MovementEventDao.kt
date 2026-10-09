package com.kinsync.android.movement

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovementEventDao {
    @Insert
    suspend fun insert(event: MovementEvent): Long

    /** The most recent movement, or null before the phone has moved once. */
    @Query("SELECT * FROM movement_events ORDER BY timestampEpochMillis DESC LIMIT 1")
    fun observeLatest(): Flow<MovementEvent?>

    /** Movements inside [fromEpochMillis, toEpochMillis), oldest first. */
    @Query(
        "SELECT * FROM movement_events " +
            "WHERE timestampEpochMillis >= :fromEpochMillis AND timestampEpochMillis < :toEpochMillis " +
            "ORDER BY timestampEpochMillis",
    )
    fun observeBetween(fromEpochMillis: Long, toEpochMillis: Long): Flow<List<MovementEvent>>

    @Query("SELECT COUNT(*) FROM movement_events")
    suspend fun count(): Int
}
