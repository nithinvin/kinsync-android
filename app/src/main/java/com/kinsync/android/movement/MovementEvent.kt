package com.kinsync.android.movement

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The phone moved noticeably (the significant-motion sensor fired). Only the time is stored:
 * no step count, no location, no sensor samples (Phase-2 decision 1, NFR-1).
 */
@Entity(tableName = "movement_events")
data class MovementEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMillis: Long,
)
