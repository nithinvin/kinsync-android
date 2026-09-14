package com.kinsync.android.network

/** Startup-time validation for backend configuration (assertive programming, CONSTITUTION.md §V). */
object BackendConfig {
    fun requireHttps(baseUrl: String): String {
        require(baseUrl.startsWith("https://")) {
            "KINSYNC_BASE_URL must be HTTPS (spec.md NFR-4), got: $baseUrl"
        }
        return baseUrl
    }
}
