package com.kinsync.android.consent

import kotlinx.coroutines.flow.Flow

/**
 * Version of the consent text shown on the consent screen. Bump it whenever the list of
 * collected signals changes, so an elder who agreed to an older text is asked again (FR-7.1).
 *
 * - 1: Phase-1 text (unlock and screen on/off only).
 * - 2: Phase-2 text (adds app usage, last moved and coarse activity).
 */
const val CURRENT_CONSENT_VERSION = 2

/** Consent version assumed for installs that agreed before consent was versioned (Phase-1). */
const val LEGACY_CONSENT_VERSION = 1

/** Elder-facing consent state (FR-7.1, FR-7.3). */
data class ConsentState(
    val hasConsented: Boolean,
    val onboardingComplete: Boolean,
    /** Version of the consent text the elder agreed to; 0 when they have not agreed. */
    val consentedVersion: Int = 0,
) {
    /** True when the elder agreed to the consent text currently shown in the app. */
    val isConsentCurrent: Boolean
        get() = hasConsented && consentedVersion >= CURRENT_CONSENT_VERSION

    /**
     * True when the elder finished onboarding under an older consent text and must agree to the
     * current text before any newly added signal is collected.
     */
    val needsReconsent: Boolean
        get() = onboardingComplete && !isConsentCurrent
}

/**
 * Tracks whether the elder has consented to passive collection and finished the onboarding
 * permission flow. Backed by local storage only — nothing here is transmitted off-device.
 */
interface ConsentManager {
    fun observeState(): Flow<ConsentState>

    /** Synchronous read for contexts without coroutine support (e.g. BroadcastReceiver). */
    fun currentState(): ConsentState

    /** Records agreement to the current consent text ([CURRENT_CONSENT_VERSION]). */
    suspend fun grantConsent()

    suspend fun completeOnboarding()

    /** Stops collection and clears local consent/onboarding flags (FR-7.3). */
    suspend fun revokeConsentAndReset()
}
