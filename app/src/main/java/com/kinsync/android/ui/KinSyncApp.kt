package com.kinsync.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.kinsync.android.ui.onboarding.ActivityRecognitionScreen
import com.kinsync.android.ui.onboarding.BatteryOptimizationScreen
import com.kinsync.android.ui.onboarding.ConsentScreen
import com.kinsync.android.ui.onboarding.UsageAccessScreen
import com.kinsync.android.ui.summary.SummaryScreen
import com.kinsync.android.ui.summary.SummaryViewModel
import com.kinsync.android.ui.timeline.TimelineScreen
import com.kinsync.android.ui.timeline.TimelineViewModel
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

    // Installing an app update stops the monitoring service. Opening the app restarts it when
    // the elder has already consented and finished onboarding (starting it twice is harmless).
    LaunchedEffect(Unit) {
        if (startDestination == KinSyncDestinations.SUMMARY) {
            MonitoringService.start(context)
        }
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
                    if (next == KinSyncDestinations.SUMMARY) {
                        // Re-consent: permissions are already granted, so resume collection.
                        MonitoringService.start(context)
                    }
                    navController.navigate(next) {
                        if (next == KinSyncDestinations.SUMMARY) {
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
                onContinue = { navController.navigate(KinSyncDestinations.ACTIVITY_RECOGNITION) },
            )
        }
        composable(KinSyncDestinations.ACTIVITY_RECOGNITION) {
            ActivityRecognitionScreen(
                onContinue = {
                    val next = KinSyncDestinations.afterActivityRecognitionFor(
                        container.consentManager.currentState(),
                    )
                    if (next == KinSyncDestinations.SUMMARY) {
                        // Opened from the summary or debug screen: register for activity
                        // transitions now that the permission may be on (starting the service
                        // twice is harmless).
                        MonitoringService.start(context)
                        navController.popBackStack()
                    } else {
                        navController.navigate(next)
                    }
                },
            )
        }
        composable(KinSyncDestinations.BATTERY_OPTIMIZATION) {
            BatteryOptimizationScreen(
                onContinue = {
                    coroutineScope.launch { container.consentManager.completeOnboarding() }
                    MonitoringService.start(context)
                    navController.navigate(KinSyncDestinations.SUMMARY) {
                        popUpTo(KinSyncDestinations.CONSENT) { inclusive = true }
                    }
                },
            )
        }
        composable(KinSyncDestinations.SUMMARY) {
            val summaryViewModel: SummaryViewModel = viewModel(
                factory = SummaryViewModel.Factory(container, context.applicationContext),
            )
            SummaryScreen(
                viewModel = summaryViewModel,
                onAllowActivityRecognition = {
                    navController.navigate(KinSyncDestinations.ACTIVITY_RECOGNITION)
                },
                onOpenTimeline = { navController.navigate(KinSyncDestinations.TIMELINE) },
                onOpenDetails = { navController.navigate(KinSyncDestinations.DEBUG) },
            )
        }
        composable(KinSyncDestinations.TIMELINE) {
            val timelineViewModel: TimelineViewModel = viewModel(
                factory = TimelineViewModel.Factory(container, context.applicationContext),
            )
            TimelineScreen(viewModel = timelineViewModel)
        }
        composable(KinSyncDestinations.DEBUG) {
            val debugViewModel: DebugViewModel = viewModel(
                factory = DebugViewModel.Factory(container, context.applicationContext),
            )
            DebugEventListScreen(
                viewModel = debugViewModel,
                onAllowActivityRecognition = {
                    navController.navigate(KinSyncDestinations.ACTIVITY_RECOGNITION)
                },
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
