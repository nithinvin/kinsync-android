package com.kinsync.android.data

import androidx.room.TypeConverter
import com.kinsync.android.activityrecognition.CoarseActivity
import com.kinsync.android.activityrecognition.TransitionKind
import com.kinsync.android.collector.UnlockEventType

class Converters {
    @TypeConverter
    fun fromEventType(value: UnlockEventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): UnlockEventType = UnlockEventType.valueOf(value)

    @TypeConverter
    fun fromCoarseActivity(value: CoarseActivity): String = value.name

    @TypeConverter
    fun toCoarseActivity(value: String): CoarseActivity = CoarseActivity.valueOf(value)

    @TypeConverter
    fun fromTransitionKind(value: TransitionKind): String = value.name

    @TypeConverter
    fun toTransitionKind(value: String): TransitionKind = TransitionKind.valueOf(value)
}
