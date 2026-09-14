package com.kinsync.android.consent

import kotlinx.coroutines.flow.Flow

/** Elder-facing consent state (FR-7.1, FR-7.3). */
data class ConsentState(
    val hasConsented: Boolean,
    val onboardingComplete: Boolean,
)

/**
 * Tracks whether the elder has consented to passive collection and finished the onboarding
 * permission flow. Backed by local storage only — nothing here is transmitted off-device.
 */
interface ConsentManager {
    fun observeState(): Flow<ConsentState>

    /** Synchronous read for contexts without coroutine support (e.g. BroadcastReceiver). */
    fun currentState(): ConsentState

    suspend fun grantConsent()

    suspend fun completeOnboarding()

    /** Stops collection and clears local consent/onboarding flags (FR-7.3). */
    suspend fun revokeConsentAndReset()
}
