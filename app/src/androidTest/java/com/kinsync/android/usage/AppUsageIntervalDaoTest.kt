package com.kinsync.android.usage

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kinsync.android.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppUsageIntervalDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AppUsageIntervalDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.appUsageIntervalDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun interval(app: String, start: Long, end: Long) =
        AppUsageInterval(packageName = app, startEpochMillis = start, endEpochMillis = end)

    @Test
    fun sameAppAndStart_replacesTheEarlierRow() = runTest {
        dao.upsertAll(listOf(interval("a", 100, 200)))
        dao.upsertAll(listOf(interval("a", 100, 500)))

        val stored = dao.observeOverlapping(0, 1_000).first()
        assertEquals(1, stored.size)
        assertEquals(500L, stored.single().endEpochMillis)
    }

    @Test
    fun sameStartDifferentApps_areBothKept() = runTest {
        dao.upsertAll(listOf(interval("a", 100, 200), interval("b", 100, 200)))

        assertEquals(2, dao.count())
    }

    @Test
    fun observeOverlapping_returnsOnlyIntervalsTouchingTheRangeOldestFirst() = runTest {
        dao.upsertAll(
            listOf(
                interval("before", 0, 1_000),
                interval("acrossStart", 900, 1_100),
                interval("inside", 1_500, 1_600),
                interval("acrossEnd", 1_900, 2_100),
                interval("after", 2_000, 2_500),
            ),
        )

        val stored = dao.observeOverlapping(1_000, 2_000).first()

        assertEquals(listOf("acrossStart", "inside", "acrossEnd"), stored.map { it.packageName })
    }
}
