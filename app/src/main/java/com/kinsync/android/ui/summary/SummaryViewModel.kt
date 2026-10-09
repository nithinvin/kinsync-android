package com.kinsync.android.ui.summary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kinsync.android.AppContainer
import com.kinsync.android.activityrecognition.ActivityTransitionRecordDao
import com.kinsync.android.collector.UnlockEventDao
import com.kinsync.android.movement.LastMovedStatus
import com.kinsync.android.movement.MovementEventDao
import com.kinsync.android.movement.SignificantMotionDetector
import com.kinsync.android.permissions.UsageAccessPermission
import com.kinsync.android.summary.DailySummary
import com.kinsync.android.summary.HomeScreenApps
import com.kinsync.android.ui.common.AppLabelResolver
import com.kinsync.android.ui.common.AppUsageRow
import com.kinsync.android.ui.common.PackageManagerAppLabelResolver
import com.kinsync.android.usage.AppUsageIntervalDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

/** The day's summary with app names resolved, ready to show. */
data class SummaryUiState(
    val summary: DailySummary,
    val topApps: List<AppUsageRow>,
)

class SummaryViewModel(
    unlockDao: UnlockEventDao,
    appUsageDao: AppUsageIntervalDao,
    movementDao: MovementEventDao,
    activityTransitionDao: ActivityTransitionRecordDao,
    appLabelResolver: AppLabelResolver,
    homeScreenPackages: Set<String>,
    /** Whether usage access is granted; read once when the screen opens. */
    val isUsageAccessGranted: Boolean,
    isMotionSensorAvailable: Boolean,
    zoneId: ZoneId = ZoneId.systemDefault(),
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val today: LocalDate = LocalDate.now(zoneId)
    private val dayStartEpochMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
    private val dayEndEpochMillis = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

    /** The current time once a minute, so an activity in progress keeps growing on screen. */
    private val minuteTicks: Flow<Long> = flow {
        while (true) {
            emit(clock())
            delay(TICK_MILLIS)
        }
    }

    /** Null until the first numbers are read from the database. */
    val state: StateFlow<SummaryUiState?> = combine(
        unlockDao.observeBetween(dayStartEpochMillis, dayEndEpochMillis),
        appUsageDao.observeOverlapping(dayStartEpochMillis, dayEndEpochMillis),
        activityTransitionDao.observeLatestBefore(dayStartEpochMillis),
        activityTransitionDao.observeBetween(dayStartEpochMillis, dayEndEpochMillis),
        minuteTicks,
    ) { unlockEvents, intervals, stateBefore, transitions, nowEpochMillis ->
        val summary = DailySummary.of(
            dayStartEpochMillis = dayStartEpochMillis,
            dayEndEpochMillis = dayEndEpochMillis,
            nowEpochMillis = nowEpochMillis,
            unlockEvents = unlockEvents,
            appUsageIntervals = intervals,
            homeScreenPackages = homeScreenPackages,
            activityStateBefore = stateBefore,
            activityTransitions = transitions,
        )
        SummaryUiState(
            summary = summary,
            topApps = summary.topApps.map { total ->
                AppUsageRow(total.packageName, appLabelResolver.labelFor(total.packageName), total.totalMillis)
            },
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val lastMoved: StateFlow<LastMovedStatus> = movementDao.observeLatest()
        .map { latest -> LastMovedStatus.of(isMotionSensorAvailable, latest) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            LastMovedStatus.of(isMotionSensorAvailable, latest = null),
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TICK_MILLIS = 60_000L
    }

    class Factory(
        private val container: AppContainer,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SummaryViewModel::class.java)) {
                "Unknown ViewModel class: $modelClass"
            }
            return SummaryViewModel(
                unlockDao = container.database.unlockEventDao(),
                appUsageDao = container.database.appUsageIntervalDao(),
                movementDao = container.database.movementEventDao(),
                activityTransitionDao = container.database.activityTransitionRecordDao(),
                appLabelResolver = PackageManagerAppLabelResolver(appContext),
                homeScreenPackages = HomeScreenApps.packages(appContext),
                isUsageAccessGranted = UsageAccessPermission.isGranted(appContext),
                isMotionSensorAvailable = SignificantMotionDetector.isAvailable(appContext),
            ) as T
        }
    }
}
