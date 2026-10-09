package com.kinsync.android.ui

import com.kinsync.android.consent.CURRENT_CONSENT_VERSION
import com.kinsync.android.consent.ConsentState
import com.kinsync.android.consent.LEGACY_CONSENT_VERSION
import org.junit.Assert.assertEquals
import org.junit.Test

class KinSyncDestinationsTest {

    private val freshInstall = ConsentState(hasConsented = false, onboardingComplete = false)

    private val phaseOneInstall = ConsentState(
        hasConsented = true,
        onboardingComplete = true,
        consentedVersion = LEGACY_CONSENT_VERSION,
    )

    private val fullyOnboarded = ConsentState(
        hasConsented = true,
        onboardingComplete = true,
        consentedVersion = CURRENT_CONSENT_VERSION,
    )

    @Test
    fun startDestination_freshInstall_isConsent() {
        assertEquals(KinSyncDestinations.CONSENT, KinSyncDestinations.startDestinationFor(freshInstall))
    }

    @Test
    fun startDestination_phaseOneInstall_isConsent() {
        assertEquals(KinSyncDestinations.CONSENT, KinSyncDestinations.startDestinationFor(phaseOneInstall))
    }

    @Test
    fun startDestination_fullyOnboarded_isDebug() {
        assertEquals(KinSyncDestinations.DEBUG, KinSyncDestinations.startDestinationFor(fullyOnboarded))
    }

    @Test
    fun startDestination_consentedButOnboardingUnfinished_isConsent() {
        val state = ConsentState(
            hasConsented = true,
            onboardingComplete = false,
            consentedVersion = CURRENT_CONSENT_VERSION,
        )

        assertEquals(KinSyncDestinations.CONSENT, KinSyncDestinations.startDestinationFor(state))
    }

    @Test
    fun afterConsent_freshInstall_continuesOnboarding() {
        assertEquals(KinSyncDestinations.USAGE_ACCESS, KinSyncDestinations.afterConsentFor(freshInstall))
    }

    @Test
    fun afterConsent_reconsent_returnsToMainScreen() {
        assertEquals(KinSyncDestinations.DEBUG, KinSyncDestinations.afterConsentFor(phaseOneInstall))
    }

    @Test
    fun afterActivityRecognition_duringOnboarding_goesToBatteryScreen() {
        val consentedOnly = ConsentState(
            hasConsented = true,
            onboardingComplete = false,
            consentedVersion = CURRENT_CONSENT_VERSION,
        )

        assertEquals(
            KinSyncDestinations.BATTERY_OPTIMIZATION,
            KinSyncDestinations.afterActivityRecognitionFor(consentedOnly),
        )
    }

    @Test
    fun afterActivityRecognition_fromMainScreen_returnsToMainScreen() {
        assertEquals(KinSyncDestinations.DEBUG, KinSyncDestinations.afterActivityRecognitionFor(fullyOnboarded))
    }
}
