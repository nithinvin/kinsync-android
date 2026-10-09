package com.kinsync.android.ui.timeline

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kinsync.android.AppContainer
import com.kinsync.android.activityrecognition.ActivityTransitionRecord
import com.kinsync.android.activityrecognition.ActivityTransitionRecordDao
import com.kinsync.android.collector.UnlockEvent
import com.kinsync.android.collector.UnlockEventDao
import com.kinsync.android.movement.MovementEvent
import com.kinsync.android.movement.MovementEventDao
import com.kinsync.android.summary.HomeScreenApps
import com.kinsync.android.timeline.DayTimeline
import com.kinsync.android.ui.common.AppLabelResolver
import com.kinsync.android.ui.common.PackageManagerAppLabelResolver
import com.kinsync.android.ui.common.minuteTicks
import com.kinsync.android.usage.AppUsageInterval
import com.kinsync.android.usage.AppUsageIntervalDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One day's timeline with app names resolved, ready to show. */
data class TimelineUiState(
    val day: LocalDate,
    val isToday: Boolean,
    val timeline: DayTimeline,
    /** App name for every package on the timeline. */
    val appLabels: Map<String, String>,
)

class TimelineViewModel(
    private val unlockDao: UnlockEventDao,
    private val appUsageDao: AppUsageIntervalDao,
    private val movementDao: MovementEventDao,
    private val activityTransitionDao: ActivityTransitionRecordDao,
    private val appLabelResolver: AppLabelResolver,
    private val homeScreenPackages: Set<String>,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val selectedDay = MutableStateFlow(today())

    /** The day the elder picked; today when the screen opens. */
    val day: StateFlow<LocalDate> = selectedDay.asStateFlow()

    /**
     * The timeline of the picked day. Null until the first one is read; after the elder picks
     * another day it still holds the previous day until the new one is ready (see
     * [TimelineUiState.day]).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TimelineUiState?> = selectedDay
        .flatMapLatest { day -> timelineFor(day) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun showPreviousDay() {
        selectedDay.update { it.minusDays(1) }
    }

    /** Moves one day later, but never past today. */
    fun showNextDay() {
        val today = today()
        selectedDay.update { day -> if (day < today) day.plusDays(1) else day }
    }

    fun today(): LocalDate = Instant.ofEpochMilli(clock()).atZone(zoneId).toLocalDate()

    private fun timelineFor(day: LocalDate): Flow<TimelineUiState> {
        val dayStart = day.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEnd = day.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val isToday = day == today()
        // Only today changes as time passes; an earlier day is drawn once.
        val ticks = if (isToday) minuteTicks(clock) else flowOf(clock())
        val stored = combine(
            unlockDao.observeBetween(dayStart, dayEnd),
            appUsageDao.observeOverlapping(dayStart, dayEnd),
            movementDao.observeBetween(dayStart, dayEnd),
            activityTransitionDao.observeLatestBefore(dayStart),
            activityTransitionDao.observeBetween(dayStart, dayEnd),
        ) { unlockEvents, intervals, movements, stateBefore, transitions ->
            StoredDay(unlockEvents, intervals, movements, stateBefore, transitions)
        }
        return combine(stored, ticks) { storedDay, nowEpochMillis ->
            val timeline = DayTimeline.of(
                dayStartEpochMillis = dayStart,
                dayEndEpochMillis = dayEnd,
                nowEpochMillis = nowEpochMillis,
                unlockEvents = storedDay.unlockEvents,
                appUsageIntervals = storedDay.appUsageIntervals,
                homeScreenPackages = homeScreenPackages,
                activityStateBefore = storedDay.activityStateBefore,
                activityTransitions = storedDay.activityTransitions,
                movementTimestamps = storedDay.movements.map { it.timestampEpochMillis },
            )
            val packages = timeline.sessions.flatMap { session -> session.apps.map { it.packageName } }.toSet()
            TimelineUiState(
                day = day,
                isToday = isToday,
                timeline = timeline,
                appLabels = packages.associateWith { appLabelResolver.labelFor(it) },
            )
        }
    }

    /** What the database holds for one day, before it is turned into a timeline. */
    private data class StoredDay(
        val unlockEvents: List<UnlockEvent>,
        val appUsageIntervals: List<AppUsageInterval>,
        val movements: List<MovementEvent>,
        val activityStateBefore: ActivityTransitionRecord?,
        val activityTransitions: List<ActivityTransitionRecord>,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }

    class Factory(
        private val container: AppContainer,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TimelineViewModel::class.java)) {
                "Unknown ViewModel class: $modelClass"
            }
            return TimelineViewModel(
                unlockDao = container.database.unlockEventDao(),
                appUsageDao = container.database.appUsageIntervalDao(),
                movementDao = container.database.movementEventDao(),
                activityTransitionDao = container.database.activityTransitionRecordDao(),
                appLabelResolver = PackageManagerAppLabelResolver(appContext),
                homeScreenPackages = HomeScreenApps.packages(appContext),
            ) as T
        }
    }
}
