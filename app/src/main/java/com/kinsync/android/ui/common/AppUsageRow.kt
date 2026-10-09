package com.kinsync.android.ui.common

/** One app's foreground time in a period, with the name shown to the elder. */
data class AppUsageRow(
    val packageName: String,
    val label: String,
    val totalMillis: Long,
)
