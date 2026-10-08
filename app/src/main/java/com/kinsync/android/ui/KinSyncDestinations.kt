package com.kinsync.android.ui

import com.kinsync.android.consent.ConsentState

/** Route constants for the onboarding → debug-screen flow. */
object KinSyncDestinations {
    const val CONSENT = "consent"
    const val USAGE_ACCESS = "usage_access"
    const val BATTERY_OPTIMIZATION = "battery_optimization"
    const val DEBUG = "debug"

    /**
     * First screen for the given consent state: the main screen only when onboarding is done and
     * the elder agreed to the current consent text; otherwise the consent screen.
     */
    fun startDestinationFor(state: ConsentState): String =
        if (state.onboardingComplete && state.isConsentCurrent) DEBUG else CONSENT

    /**
     * Screen to open after the elder agrees on the consent screen. Someone re-consenting after an
     * upgrade has already granted the onboarding permissions, so they go straight back to the
     * main screen.
     */
    fun afterConsentFor(state: ConsentState): String =
        if (state.onboardingComplete) DEBUG else USAGE_ACCESS
}
