package com.kinsync.android.summary

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * The home-screen (launcher) app on this phone. Time on the home screen is not app use, so
 * the summary leaves it out (the manifest's `<queries>` makes launchers visible).
 */
object HomeScreenApps {

    /** Package shown when Android has no default home app and would ask the user to choose. */
    private const val CHOOSER_PACKAGE = "android"

    fun packages(context: Context): Set<String> {
        val packageManager = context.packageManager
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

        val defaultHome = packageManager
            .resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
        if (defaultHome != null && defaultHome != CHOOSER_PACKAGE) {
            return setOf(defaultHome)
        }

        // No default yet: take every home app, but not apps with their own icon in the app
        // list. Settings, for example, has a fallback home screen used only while booting.
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val appsWithIcon = packageManager
            .queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo.packageName }
            .toSet()
        return packageManager
            .queryIntentActivities(homeIntent, 0)
            .map { it.activityInfo.packageName }
            .filter { it !in appsWithIcon }
            .toSet()
    }
}
