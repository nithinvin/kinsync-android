package com.kinsync.android.usage

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kinsync.android.KinSyncApplication
import com.kinsync.android.permissions.UsageAccessPermission
import java.util.concurrent.TimeUnit

/**
 * Background job that collects app usage. Does nothing unless the elder agreed to the current
 * consent text, finished onboarding and granted usage access (FR-7.1, FR-2.2).
 */
class AppUsageCollectionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as KinSyncApplication).container
        val mayCollect = container.consentManager.currentState().canCollect &&
            UsageAccessPermission.isGranted(applicationContext)
        if (mayCollect) {
            container.appUsageCollector.collect()
        }
        return Result.success()
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "app_usage_collection_periodic"
        private const val IMMEDIATE_WORK_NAME = "app_usage_collection_now"

        /** WorkManager's shortest allowed period. */
        private const val PERIOD_MINUTES = 15L

        /**
         * Collects once now and then every 15 minutes. Safe to call repeatedly: the periodic
         * job is kept if it already exists.
         */
        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<AppUsageCollectionWorker>(PERIOD_MINUTES, TimeUnit.MINUTES).build(),
            )
            workManager.enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<AppUsageCollectionWorker>().build(),
            )
        }

        /** Stops all app-usage collection (consent revoked). */
        fun cancel(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
        }
    }
}
