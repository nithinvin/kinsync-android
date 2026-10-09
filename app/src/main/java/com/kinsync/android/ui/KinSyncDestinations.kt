package com.kinsync.android.ui

import com.kinsync.android.consent.ConsentState

/** Route constants for the onboarding → debug-screen flow. */
object KinSyncDestinations {
    const val CONSENT = "consent"
    const val USAGE_ACCESS = "usage_access"
    const val ACTIVITY_RECOGNITION = "activity_recognition"
    const val BATTERY_OPTIMIZATION = "battery_optimization"
    const val DEBUG = "debug"

    /**
     * First screen for the given consent state: the main screen only when onboarding is done and
     * the elder agreed to the current consent text; otherwise the consent screen.
     */
    fun startDestinationFor(state: ConsentState): String =
        if (state.canCollect) DEBUG else CONSENT

    /**
     * Screen to open after the elder agrees on the consent screen. Someone re-consenting after an
     * upgrade has already granted the onboarding permissions, so they go straight back to the
     * main screen.
     */
    fun afterConsentFor(state: ConsentState): String =
        if (state.onboardingComplete) DEBUG else USAGE_ACCESS

    /**
     * Screen to open after the activity-recognition screen. During onboarding the next step is
     * the battery screen; from the main screen (onboarding already done) it goes back there.
     */
    fun afterActivityRecognitionFor(state: ConsentState): String =
        if (state.onboardingComplete) DEBUG else BATTERY_OPTIMIZATION
}
