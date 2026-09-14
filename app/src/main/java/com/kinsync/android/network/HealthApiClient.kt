package com.kinsync.android.network

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Talks to the kinsync-api backend. Phase-1 only ever calls `/health` — no elder activity data is
 * sent (NFR-1). Callers MUST pass an `https://` [baseUrl]; validated by [AppContainer] at startup
 * rather than here, so this class stays trivially testable against a plain-HTTP MockWebServer.
 */
class HealthApiClient(
    private val baseUrl: String,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    suspend fun checkHealth(): HealthCheckResult = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/health").get().build()
        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    HealthCheckResult.Success(body)
                } else {
                    HealthCheckResult.Failure("HTTP ${response.code}")
                }
            }
        } catch (error: IOException) {
            HealthCheckResult.Failure(error.message ?: "Network error")
        }
    }
}
