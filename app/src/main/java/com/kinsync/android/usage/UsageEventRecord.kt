package com.kinsync.android.usage

/** What happened in one usage event, reduced to what interval building needs. */
enum class UsageEventKind {
    /** An activity of the app came to the foreground (resumed). */
    FOREGROUND,

    /** An activity of the app left the foreground (paused). */
    BACKGROUND,

    /** The phone shut down; every app still in the foreground stops there. */
    DEVICE_SHUTDOWN,
}

/**
 * One event read from `UsageStatsManager`, already mapped away from the Android types so the
 * interval logic runs as plain Kotlin in JVM tests.
 */
data class UsageEventRecord(
    val packageName: String,
    /** Activity class; one app can have several activities, each resumed and paused on its own. */
    val className: String?,
    val timestampEpochMillis: Long,
    val kind: UsageEventKind,
)
