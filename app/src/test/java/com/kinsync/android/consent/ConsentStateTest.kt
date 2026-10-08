package com.kinsync.android.consent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsentStateTest {

    @Test
    fun freshInstall_isNotCurrentAndDoesNotNeedReconsent() {
        val state = ConsentState(hasConsented = false, onboardingComplete = false)

        assertFalse(state.isConsentCurrent)
        assertFalse(state.needsReconsent)
    }

    @Test
    fun agreedToCurrentVersion_isCurrent() {
        val state = ConsentState(
            hasConsented = true,
            onboardingComplete = true,
            consentedVersion = CURRENT_CONSENT_VERSION,
        )

        assertTrue(state.isConsentCurrent)
        assertFalse(state.needsReconsent)
    }

    @Test
    fun phaseOneInstall_needsReconsent() {
        val state = ConsentState(
            hasConsented = true,
            onboardingComplete = true,
            consentedVersion = LEGACY_CONSENT_VERSION,
        )

        assertFalse(state.isConsentCurrent)
        assertTrue(state.needsReconsent)
    }

    @Test
    fun agreedButOnboardingNotFinished_doesNotNeedReconsent() {
        val state = ConsentState(
            hasConsented = true,
            onboardingComplete = false,
            consentedVersion = CURRENT_CONSENT_VERSION,
        )

        assertTrue(state.isConsentCurrent)
        assertFalse(state.needsReconsent)
    }

    @Test
    fun currentVersionWithoutConsentFlag_isNotCurrent() {
        // Malformed storage: a version number but no consent flag must never count as consent.
        val state = ConsentState(
            hasConsented = false,
            onboardingComplete = true,
            consentedVersion = CURRENT_CONSENT_VERSION,
        )

        assertFalse(state.isConsentCurrent)
        assertTrue(state.needsReconsent)
    }

    @Test
    fun newerVersionThanApp_isCurrent() {
        // Edge case: after a downgrade the stored version can be higher than the app's.
        val state = ConsentState(
            hasConsented = true,
            onboardingComplete = true,
            consentedVersion = CURRENT_CONSENT_VERSION + 1,
        )

        assertTrue(state.isConsentCurrent)
    }

    @Test
    fun canCollect_onlyWithCurrentConsentAndFinishedOnboarding() {
        val current = ConsentState(hasConsented = true, onboardingComplete = true, consentedVersion = CURRENT_CONSENT_VERSION)

        assertTrue(current.canCollect)
        assertFalse(current.copy(onboardingComplete = false).canCollect)
        assertFalse(current.copy(consentedVersion = LEGACY_CONSENT_VERSION).canCollect)
        assertFalse(current.copy(hasConsented = false).canCollect)
    }
}
