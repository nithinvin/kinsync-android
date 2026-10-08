package com.kinsync.android.ui.debug

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kinsync.android.AppContainer
import com.kinsync.android.collector.UnlockEvent
import com.kinsync.android.collector.UnlockEventDao
import com.kinsync.android.network.HealthApiClient
import com.kinsync.android.network.HealthCheckResult
import com.kinsync.android.permissions.UsageAccessPermission
import com.kinsync.android.usage.AppUsageIntervalDao
import com.kinsync.android.usage.AppUsageTotals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** One app's foreground time today, with the name shown to the elder. */
data class AppUsageRow(
    val packageName: String,
    val label: String,
    val totalMillis: Long,
)

class DebugViewModel(
    dao: UnlockEventDao,
    appUsageDao: AppUsageIntervalDao,
    private val healthApiClient: HealthApiClient,
    appLabelResolver: AppLabelResolver,
    /** Whether usage access is granted; read once when the screen opens. */
    val isUsageAccessGranted: Boolean,
    zoneId: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    val events: StateFlow<List<UnlockEvent>> = dao.observeRecentEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val today = LocalDate.now(zoneId)
    private val todayStartEpochMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
    private val tomorrowStartEpochMillis = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

    /** Foreground time per app today, largest first. Only intervals already collected count. */
    val appUsageToday: StateFlow<List<AppUsageRow>> = appUsageDao
        .observeOverlapping(todayStartEpochMillis, tomorrowStartEpochMillis)
        .map { intervals ->
            AppUsageTotals.within(intervals, todayStartEpochMillis, tomorrowStartEpochMillis).map { total ->
                AppUsageRow(total.packageName, appLabelResolver.labelFor(total.packageName), total.totalMillis)
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val _healthStatus = MutableStateFlow<HealthCheckResult?>(null)
    val healthStatus: StateFlow<HealthCheckResult?> = _healthStatus.asStateFlow()

    init {
        viewModelScope.launch {
            _healthStatus.value = healthApiClient.checkHealth()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }

    class Factory(
        private val container: AppContainer,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DebugViewModel::class.java)) {
                "Unknown ViewModel class: $modelClass"
            }
            return DebugViewModel(
                dao = container.database.unlockEventDao(),
                appUsageDao = container.database.appUsageIntervalDao(),
                healthApiClient = container.healthApiClient,
                appLabelResolver = PackageManagerAppLabelResolver(appContext),
                isUsageAccessGranted = UsageAccessPermission.isGranted(appContext),
            ) as T
        }
    }
}
