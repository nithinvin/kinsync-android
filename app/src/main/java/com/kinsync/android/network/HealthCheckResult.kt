package com.kinsync.android.network

/** Result of the Phase-1 stretch-goal `/health` connectivity check (plan.md §Phase-1). */
sealed interface HealthCheckResult {
    data class Success(val rawBody: String) : HealthCheckResult
    data class Failure(val reason: String) : HealthCheckResult
}
