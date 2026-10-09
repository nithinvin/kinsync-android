package com.kinsync.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kinsync.android.activityrecognition.ActivityTransitionRecord
import com.kinsync.android.activityrecognition.ActivityTransitionRecordDao
import com.kinsync.android.collector.UnlockEvent
import com.kinsync.android.collector.UnlockEventDao
import com.kinsync.android.movement.MovementEvent
import com.kinsync.android.movement.MovementEventDao
import com.kinsync.android.usage.AppUsageInterval
import com.kinsync.android.usage.AppUsageIntervalDao

@Database(
    entities = [
        UnlockEvent::class,
        AppUsageInterval::class,
        MovementEvent::class,
        ActivityTransitionRecord::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun unlockEventDao(): UnlockEventDao

    abstract fun appUsageIntervalDao(): AppUsageIntervalDao

    abstract fun movementEventDao(): MovementEventDao

    abstract fun activityTransitionRecordDao(): ActivityTransitionRecordDao

    companion object {
        private const val DATABASE_NAME = "kinsync.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(*Migrations.ALL).build().also { instance = it }
            }
    }
}
