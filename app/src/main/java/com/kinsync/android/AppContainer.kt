package com.kinsync.android

import android.content.Context
import com.kinsync.android.consent.ConsentManager
import com.kinsync.android.consent.SharedPreferencesConsentManager
import com.kinsync.android.data.AppDatabase
import com.kinsync.android.network.BackendConfig
import com.kinsync.android.network.HealthApiClient

/**
 * Minimal manual dependency container for Phase-1 (no DI framework yet, per CONSTITUTION.md §VII).
 */
interface AppContainer {
    val database: AppDatabase
    val consentManager: ConsentManager
    val healthApiClient: HealthApiClient
}

class DefaultAppContainer(private val appContext: Context) : AppContainer {
    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(appContext)
    }

    override val consentManager: ConsentManager by lazy {
        SharedPreferencesConsentManager(appContext)
    }

    override val healthApiClient: HealthApiClient by lazy {
        HealthApiClient(BackendConfig.requireHttps(BuildConfig.KINSYNC_BASE_URL))
    }
}
