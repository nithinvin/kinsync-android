package com.kinsync.android.data

import androidx.room.TypeConverter
import com.kinsync.android.collector.UnlockEventType

class Converters {
    @TypeConverter
    fun fromEventType(value: UnlockEventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): UnlockEventType = UnlockEventType.valueOf(value)
}
