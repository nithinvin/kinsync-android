package com.kinsync.android.consent

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesConsentManagerTest {

    private lateinit var manager: SharedPreferencesConsentManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
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
}
