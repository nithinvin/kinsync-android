package com.kinsync.android.movement

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kinsync.android.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovementEventDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: MovementEventDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.movementEventDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeLatest_emptyDatabase_isNull() = runTest {
        assertNull(dao.observeLatest().first())
    }

    @Test
    fun observeLatest_returnsTheMostRecentEvenIfInsertedFirst() = runTest {
        dao.insert(MovementEvent(timestampEpochMillis = 300L))
        dao.insert(MovementEvent(timestampEpochMillis = 100L))

        assertEquals(300L, dao.observeLatest().first()?.timestampEpochMillis)
        assertEquals(2, dao.count())
    }

    @Test
    fun observeBetween_returnsOnlyThePeriodOldestFirst() = runTest {
        dao.insert(MovementEvent(timestampEpochMillis = 250L))
        dao.insert(MovementEvent(timestampEpochMillis = 99L))
        dao.insert(MovementEvent(timestampEpochMillis = 100L))
        // The end of the period is not part of it.
        dao.insert(MovementEvent(timestampEpochMillis = 300L))

        assertEquals(listOf(100L, 250L), dao.observeBetween(100L, 300L).first().map { it.timestampEpochMillis })
    }
}
