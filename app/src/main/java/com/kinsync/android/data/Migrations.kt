package com.kinsync.android.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations. Every schema change gets a real migration so a phone that is updated
 * in place keeps the history it has already collected (no destructive fallback).
 */
object Migrations {

    /** Phase-2 M2: adds app-usage intervals; Phase-1 unlock events are kept as they are. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `app_usage_intervals` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`packageName` TEXT NOT NULL, " +
                    "`startEpochMillis` INTEGER NOT NULL, " +
                    "`endEpochMillis` INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_app_usage_intervals_packageName_startEpochMillis` " +
                    "ON `app_usage_intervals` (`packageName`, `startEpochMillis`)",
            )
        }
    }

    val ALL = arrayOf(MIGRATION_1_2)
}
