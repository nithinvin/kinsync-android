package com.kinsync.android.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kinsync.android.AppContainer
import com.kinsync.android.collector.UnlockEvent
import com.kinsync.android.collector.UnlockEventDao
import com.kinsync.android.network.HealthApiClient
import com.kinsync.android.network.HealthCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DebugViewModel(
    dao: UnlockEventDao,
    private val healthApiClient: HealthApiClient,
) : ViewModel() {

    val events: StateFlow<List<UnlockEvent>> = dao.observeRecentEvents()
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

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DebugViewModel::class.java)) {
                "Unknown ViewModel class: $modelClass"
            }
            return DebugViewModel(container.database.unlockEventDao(), container.healthApiClient) as T
        }
    }
}
