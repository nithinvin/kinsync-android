package com.kinsync.android.consent

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedPreferencesConsentManager(context: Context) : ConsentManager {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val state = MutableStateFlow(readState())

    override fun observeState() = state.asStateFlow()

    override fun currentState(): ConsentState = readState()

    override suspend fun grantConsent() {
        prefs.edit().putBoolean(KEY_HAS_CONSENTED, true).apply()
        state.value = readState()
    }

    override suspend fun completeOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
        state.value = readState()
    }

    override suspend fun revokeConsentAndReset() {
        prefs.edit()
            .putBoolean(KEY_HAS_CONSENTED, false)
            .putBoolean(KEY_ONBOARDING_COMPLETE, false)
            .apply()
        state.value = readState()
    }

    private fun readState() = ConsentState(
        hasConsented = prefs.getBoolean(KEY_HAS_CONSENTED, false),
        onboardingComplete = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false),
    )

    private companion object {
        const val PREFS_NAME = "kinsync_consent"
        const val KEY_HAS_CONSENTED = "has_consented"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }
}
