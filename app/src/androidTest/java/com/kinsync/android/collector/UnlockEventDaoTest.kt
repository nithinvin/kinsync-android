package com.kinsync.android.collector

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kinsync.android.data.AppDatabase
import com.kinsync.android.data.Converters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnlockEventDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: UnlockEventDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.unlockEventDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndObserve_returnsInsertedEventMostRecentFirst() = runTest {
        dao.insert(UnlockEvent(eventType = UnlockEventType.SCREEN_ON, timestampEpochMillis = 100L))
        dao.insert(UnlockEvent(eventType = UnlockEventType.USER_PRESENT, timestampEpochMillis = 200L))

        val events = dao.observeRecentEvents().first()

        assertEquals(2, events.size)
        assertEquals(UnlockEventType.USER_PRESENT, events.first().eventType)
    }

    @Test
    fun observeRecentEvents_emptyDatabase_returnsEmptyList() = runTest {
        val events = dao.observeRecentEvents().first()

        assertTrue(events.isEmpty())
    }

    @Test
    fun observeRecentEvents_respectsLimit() = runTest {
        repeat(5) { index ->
            dao.insert(UnlockEvent(eventType = UnlockEventType.SCREEN_OFF, timestampEpochMillis = index.toLong()))
        }

        val events = dao.observeRecentEvents(limit = 2).first()

        assertEquals(2, events.size)
    }

    @Test
    fun count_matchesNumberOfInsertedEvents() = runTest {
        dao.insert(UnlockEvent(eventType = UnlockEventType.SCREEN_ON, timestampEpochMillis = 1L))
        dao.insert(UnlockEvent(eventType = UnlockEventType.SCREEN_OFF, timestampEpochMillis = 2L))

        assertEquals(2, dao.count())
    }
}
