package com.kinsync.android.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kinsync.android.collector.UnlockEventType
import com.kinsync.android.usage.AppUsageInterval
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate1To2_keepsUnlockEventsAndAddsAppUsageTable() = runTest {
        helper.createDatabase(TEST_DB, 1).apply {
            // A Phase-1 database exactly as the demo phone has it.
            insert(
                "unlock_events",
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("eventType", UnlockEventType.USER_PRESENT.name)
                    put("timestampEpochMillis", 1_726_300_000_000L)
                },
            )
            close()
        }

        // Fails if the migrated schema differs from the one Room expects for version 2.
        helper.runMigrationsAndValidate(TEST_DB, 2, true, Migrations.MIGRATION_1_2).close()

        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            TEST_DB,
        ).addMigrations(*Migrations.ALL).build()
        try {
            val events = database.unlockEventDao().observeRecentEvents().first()
            assertEquals(1, events.size)
            assertEquals(UnlockEventType.USER_PRESENT, events.single().eventType)

            database.appUsageIntervalDao().upsertAll(
                listOf(AppUsageInterval(packageName = "com.example.maps", startEpochMillis = 1, endEpochMillis = 2)),
            )
            assertEquals(1, database.appUsageIntervalDao().count())
        } finally {
            database.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
