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
import com.kinsync.android.consent.ConsentState
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

    val consentState by container.consentManager.observeState()
        .collectAsState(initial = ConsentState(hasConsented = false, onboardingComplete = false))

    val startDestination = if (consentState.onboardingComplete) {
        KinSyncDestinations.DEBUG
    } else {
        KinSyncDestinations.CONSENT
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(KinSyncDestinations.CONSENT) {
            ConsentScreen(
                onAgree = {
                    coroutineScope.launch { container.consentManager.grantConsent() }
                    navController.navigate(KinSyncDestinations.USAGE_ACCESS)
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
