package com.kinsync.android.usage

import android.content.Context

/** Remembers up to which time usage events have been turned into intervals. */
interface AppUsageCursorStore {
    /** Time from which the next collection reads events, or null before the first collection. */
    fun read(): Long?

    fun write(epochMillis: Long)
}

class SharedPreferencesAppUsageCursorStore(context: Context) : AppUsageCursorStore {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun read(): Long? =
        if (preferences.contains(KEY_COLLECTED_FROM)) preferences.getLong(KEY_COLLECTED_FROM, 0L) else null

    override fun write(epochMillis: Long) {
        preferences.edit().putLong(KEY_COLLECTED_FROM, epochMillis).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "kinsync_app_usage"
        const val KEY_COLLECTED_FROM = "next_collection_from_epoch_millis"
    }
}
