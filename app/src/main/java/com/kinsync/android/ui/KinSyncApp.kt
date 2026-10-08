package com.kinsync.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kinsync.android.AppContainer
import com.kinsync.android.collector.MonitoringService
import com.kinsync.android.ui.debug.DebugEventListScreen
import com.kinsync.android.ui.debug.DebugViewModel
import com.kinsync.android.ui.onboarding.BatteryOptimizationScreen
import com.kinsync.android.ui.onboarding.ConsentScreen
import com.kinsync.android.ui.onboarding.UsageAccessScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Composable
fun KinSyncApp(container: AppContainer) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val coroutineScope = remember { CoroutineScope(SupervisorJob()) }

    // Read once, synchronously, so the first frame already opens the right screen.
    val startDestination = remember {
        KinSyncDestinations.startDestinationFor(container.consentManager.currentState())
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(KinSyncDestinations.CONSENT) {
            val consentState by container.consentManager.observeState()
                .collectAsState(initial = container.consentManager.currentState())
            ConsentScreen(
                isReconsent = consentState.needsReconsent,
                onAgree = {
                    val next = KinSyncDestinations.afterConsentFor(consentState)
                    coroutineScope.launch { container.consentManager.grantConsent() }
                    if (next == KinSyncDestinations.DEBUG) {
                        // Re-consent: permissions are already granted, so resume collection.
                        MonitoringService.start(context)
                    }
                    navController.navigate(next) {
                        if (next == KinSyncDestinations.DEBUG) {
                            popUpTo(KinSyncDestinations.CONSENT) { inclusive = true }
                        }
                    }
                },
                onDecline = {
                    coroutineScope.launch { container.consentManager.revokeConsentAndReset() }
                    MonitoringService.stop(context)
                },
            )
        }
        composable(KinSyncDestinations.USAGE_ACCESS) {
            UsageAccessScreen(
                onContinue = { navController.navigate(KinSyncDestinations.BATTERY_OPTIMIZATION) },
            )
        }
        composable(KinSyncDestinations.BATTERY_OPTIMIZATION) {
            BatteryOptimizationScreen(
                onContinue = {
                    coroutineScope.launch { container.consentManager.completeOnboarding() }
                    MonitoringService.start(context)
                    navController.navigate(KinSyncDestinations.DEBUG) {
                        popUpTo(KinSyncDestinations.CONSENT) { inclusive = true }
                    }
                },
            )
        }
        composable(KinSyncDestinations.DEBUG) {
            val debugViewModel: DebugViewModel = viewModel(factory = DebugViewModel.Factory(container))
            DebugEventListScreen(
                viewModel = debugViewModel,
                onRevokeConsent = {
                    coroutineScope.launch { container.consentManager.revokeConsentAndReset() }
                    MonitoringService.stop(context)
                    navController.navigate(KinSyncDestinations.CONSENT) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
