package com.kinsync.android.consent

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesConsentManagerTest {

    private lateinit var context: android.content.Context
    private lateinit var manager: SharedPreferencesConsentManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("kinsync_consent", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        manager = SharedPreferencesConsentManager(context)
    }

    @Test
    fun initialState_isNotConsentedAndNotComplete() {
        val state = manager.currentState()

        assertFalse(state.hasConsented)
        assertFalse(state.onboardingComplete)
    }

    @Test
    fun grantConsent_setsHasConsentedButNotOnboardingComplete() = runTest {
        manager.grantConsent()

        val state = manager.currentState()
        assertTrue(state.hasConsented)
        assertFalse(state.onboardingComplete)
    }

    @Test
    fun completeOnboarding_setsBothFlags() = runTest {
        manager.grantConsent()
        manager.completeOnboarding()

        val state = manager.currentState()
        assertTrue(state.hasConsented)
        assertTrue(state.onboardingComplete)
    }

    @Test
    fun revokeConsentAndReset_clearsBothFlags() = runTest {
        manager.grantConsent()
        manager.completeOnboarding()

        manager.revokeConsentAndReset()

        val state = manager.currentState()
        assertFalse(state.hasConsented)
        assertFalse(state.onboardingComplete)
    }

    @Test
    fun initialState_hasNoConsentVersion() {
        assertEquals(0, manager.currentState().consentedVersion)
    }

    @Test
    fun grantConsent_storesCurrentVersion() = runTest {
        manager.grantConsent()
        manager.completeOnboarding()

        val state = manager.currentState()
        assertEquals(CURRENT_CONSENT_VERSION, state.consentedVersion)
        assertTrue(state.isConsentCurrent)
        assertFalse(state.needsReconsent)
    }

    @Test
    fun phaseOnePrefsWithoutVersion_readAsLegacyAndNeedReconsent() {
        // Exactly what a Phase-1 install left behind: both flags, no version key.
        writePrefs { putBoolean("has_consented", true).putBoolean("onboarding_complete", true) }
        val upgraded = SharedPreferencesConsentManager(context)

        val state = upgraded.currentState()
        assertEquals(LEGACY_CONSENT_VERSION, state.consentedVersion)
        assertTrue(state.needsReconsent)
    }

    @Test
    fun reconsentAfterUpgrade_makesConsentCurrentAndKeepsOnboarding() = runTest {
        writePrefs { putBoolean("has_consented", true).putBoolean("onboarding_complete", true) }
        val upgraded = SharedPreferencesConsentManager(context)

        upgraded.grantConsent()

        val state = upgraded.currentState()
        assertTrue(state.isConsentCurrent)
        assertTrue(state.onboardingComplete)
        assertFalse(state.needsReconsent)
    }

    @Test
    fun revokeConsentAndReset_clearsVersion() = runTest {
        manager.grantConsent()

        manager.revokeConsentAndReset()

        assertEquals(0, manager.currentState().consentedVersion)
    }

    @Test
    fun observeState_emitsUpdatedVersionAfterGrant() = runTest {
        manager.grantConsent()

        assertEquals(CURRENT_CONSENT_VERSION, manager.observeState().first().consentedVersion)
    }

    private fun writePrefs(block: android.content.SharedPreferences.Editor.() -> android.content.SharedPreferences.Editor) {
        context.getSharedPreferences("kinsync_consent", android.content.Context.MODE_PRIVATE)
            .edit()
            .block()
            .commit()
    }
}
