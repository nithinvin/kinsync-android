package com.kinsync.android.collector

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kinsync.android.AppContainer
import com.kinsync.android.KinSyncApplication
import com.kinsync.android.R
import com.kinsync.android.activityrecognition.ActivityTransitionRegistrar
import com.kinsync.android.movement.MovementEvent
import com.kinsync.android.movement.MovementEventDao
import com.kinsync.android.movement.SignificantMotionDetector
import com.kinsync.android.usage.AppUsageCollectionWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps [UnlockEventReceiver] and the [SignificantMotionDetector] alive across app-process states so the
 * dead-man's-switch data (Phase-2+) is never missing a day (NFR-2). Runs as `specialUse` since no
 * standard foreground-service type covers passive wellbeing monitoring.
 *
 * Every path that starts monitoring (onboarding, app open, reboot, app update) goes through
 * [start], so starting the service also schedules the periodic app-usage collection and
 * registers for activity transitions, and [stop] cancels both.
 */
class MonitoringService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var container: AppContainer
    private lateinit var dao: UnlockEventDao
    private lateinit var movementDao: MovementEventDao
    private lateinit var receiver: UnlockEventReceiver
    private lateinit var motionDetector: SignificantMotionDetector
    private var isReceiverRegistered = false
    private var isMotionDetectorStarted = false

    override fun onCreate() {
        super.onCreate()
        container = (application as KinSyncApplication).container
        dao = container.database.unlockEventDao()
        movementDao = container.database.movementEventDao()
        receiver = UnlockEventReceiver(::recordEvent)
        motionDetector = SignificantMotionDetector(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        registerReceiverIfNeeded()
        startMotionDetectorIfNeeded()
        AppUsageCollectionWorker.schedule(this)
        ActivityTransitionRegistrar(this).registerIfPermitted()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (isReceiverRegistered) {
            unregisterReceiver(receiver)
            isReceiverRegistered = false
        }
        motionDetector.stop()
        isMotionDetectorStarted = false
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun registerReceiverIfNeeded() {
        if (isReceiverRegistered) return
        ContextCompat.registerReceiver(
            this,
            receiver,
            UnlockEventReceiver.intentFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        isReceiverRegistered = true
    }

    private fun startMotionDetectorIfNeeded() {
        if (isMotionDetectorStarted) return
        motionDetector.start(::recordMovement)
        isMotionDetectorStarted = true
    }

    /**
     * Movement is a Phase-2 signal, so it is recorded only under the current consent text. The
     * service can be restarted by Android (`START_STICKY`) without going through a consent check.
     */
    private fun recordMovement(timestampEpochMillis: Long) {
        if (!container.consentManager.currentState().canCollect) return
        serviceScope.launch {
            movementDao.insert(MovementEvent(timestampEpochMillis = timestampEpochMillis))
        }
    }

    private fun recordEvent(eventType: UnlockEventType, timestampEpochMillis: Long) {
        serviceScope.launch {
            dao.insert(UnlockEvent(eventType = eventType, timestampEpochMillis = timestampEpochMillis))
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.monitoring_notification_channel_name),
            NotificationManager.IMPORTANCE_MIN,
        )
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.monitoring_notification_title))
            .setContentText(getString(R.string.monitoring_notification_body))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "kinsync_monitoring"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, MonitoringService::class.java))
        }

        fun stop(context: Context) {
            AppUsageCollectionWorker.cancel(context)
            ActivityTransitionRegistrar(context).unregister()
            context.stopService(Intent(context, MonitoringService::class.java))
        }
    }
}
