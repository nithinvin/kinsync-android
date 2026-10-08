package com.kinsync.android.movement

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener

/**
 * Notices when the phone is moved, using Android's significant-motion sensor (FR-2.7).
 *
 * The sensor is a one-shot hardware trigger: it fires once and switches itself off, so it costs
 * almost no battery and is re-armed after each trigger. There is no continuous accelerometer
 * sampling and no step counting.
 */
class SignificantMotionDetector(context: Context) {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)
    private var onMoved: ((Long) -> Unit)? = null

    private val listener = object : TriggerEventListener() {
        override fun onTrigger(event: TriggerEvent?) {
            onMoved?.invoke(System.currentTimeMillis())
            arm()
        }
    }

    /** False on phones without a significant-motion sensor; "last moved" is then unavailable. */
    val isAvailable: Boolean
        get() = sensor != null

    /** Starts listening; [onMoved] gets the time of every movement. Does nothing without a sensor. */
    fun start(onMoved: (Long) -> Unit) {
        this.onMoved = onMoved
        arm()
    }

    fun stop() {
        onMoved = null
        if (sensor != null) {
            sensorManager?.cancelTriggerSensor(listener, sensor)
        }
    }

    private fun arm() {
        if (sensor != null && onMoved != null) {
            sensorManager?.requestTriggerSensor(listener, sensor)
        }
    }

    companion object {
        fun isAvailable(context: Context): Boolean =
            context.getSystemService(SensorManager::class.java)
                ?.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION) != null
    }
}
