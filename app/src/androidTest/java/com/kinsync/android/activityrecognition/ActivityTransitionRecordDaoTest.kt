package com.kinsync.android.activityrecognition

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kinsync.android.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityTransitionRecordDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ActivityTransitionRecordDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.activityTransitionRecordDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun record(activity: CoarseActivity, kind: TransitionKind, at: Long) =
        ActivityTransitionRecord(activity = activity, kind = kind, timestampEpochMillis = at)

    @Test
    fun observeRecent_emptyDatabase_isEmpty() = runTest {
        assertTrue(dao.observeRecent(10).first().isEmpty())
    }

    @Test
    fun observeRecent_isNewestFirstAndLimited() = runTest {
        dao.insertAll(
            listOf(
                record(CoarseActivity.STILL, TransitionKind.EXIT, 100L),
                record(CoarseActivity.WALKING, TransitionKind.ENTER, 100L),
                record(CoarseActivity.WALKING, TransitionKind.EXIT, 300L),
                record(CoarseActivity.STILL, TransitionKind.ENTER, 300L),
            ),
        )

        val recent = dao.observeRecent(3).first()

        assertEquals(3, recent.size)
        // Same time: the one inserted later (the ENTER) comes first, so it is the current activity.
        assertEquals(record(CoarseActivity.STILL, TransitionKind.ENTER, 300L), recent[0].copy(id = 0))
        assertEquals(record(CoarseActivity.WALKING, TransitionKind.EXIT, 300L), recent[1].copy(id = 0))
    }

    @Test
    fun repeatedDelivery_isStoredOnce() = runTest {
        val walking = record(CoarseActivity.WALKING, TransitionKind.ENTER, 200L)

        dao.insertAll(listOf(walking))
        dao.insertAll(listOf(walking, record(CoarseActivity.WALKING, TransitionKind.EXIT, 400L)))

        assertEquals(2, dao.count())
    }

    @Test
    fun observeBetween_returnsOnlyTheDayOldestFirst() = runTest {
        dao.insertAll(
            listOf(
                record(CoarseActivity.WALKING, TransitionKind.ENTER, 1_500L),
                record(CoarseActivity.STILL, TransitionKind.ENTER, 900L),
                record(CoarseActivity.STILL, TransitionKind.EXIT, 1_000L),
                record(CoarseActivity.STILL, TransitionKind.ENTER, 2_000L),
            ),
        )

        val day = dao.observeBetween(1_000L, 2_000L).first()

        assertEquals(listOf(1_000L, 1_500L), day.map { it.timestampEpochMillis })
    }

    @Test
    fun observeLatestBefore_isTheStateAtTheStartOfTheDay() = runTest {
        dao.insertAll(
            listOf(
                record(CoarseActivity.WALKING, TransitionKind.EXIT, 800L),
                record(CoarseActivity.STILL, TransitionKind.ENTER, 800L),
                record(CoarseActivity.WALKING, TransitionKind.ENTER, 1_200L),
            ),
        )

        assertEquals(record(CoarseActivity.STILL, TransitionKind.ENTER, 800L), dao.observeLatestBefore(1_000L).first()?.copy(id = 0))
        assertEquals(null, dao.observeLatestBefore(800L).first())
    }
}
