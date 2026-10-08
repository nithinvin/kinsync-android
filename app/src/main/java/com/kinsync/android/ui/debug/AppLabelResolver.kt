package com.kinsync.android.ui.debug

import android.content.Context
import android.content.pm.PackageManager

/** Turns a package name into the app name the elder knows. */
fun interface AppLabelResolver {
    fun labelFor(packageName: String): String
}

/**
 * Looks the name up with the package manager. Falls back to the package name for apps this app
 * cannot see (Android 11+ only shows launcher apps, see the manifest's `<queries>`).
 */
class PackageManagerAppLabelResolver(context: Context) : AppLabelResolver {

    private val packageManager = context.packageManager
    private val cache = mutableMapOf<String, String>()

    @Synchronized
    override fun labelFor(packageName: String): String = cache.getOrPut(packageName) {
        try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (exception: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
