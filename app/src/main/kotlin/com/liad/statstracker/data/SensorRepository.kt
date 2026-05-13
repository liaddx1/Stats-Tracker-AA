package com.liad.statstracker.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.liad.statstracker.domain.SensorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlin.math.exp

class SensorRepository private constructor(context: Context) {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val state: SharedFlow<SensorState> = callbackFlow {
        var gx = 0f
        var gy = 0f
        var heading: Float? = null
        var lastAccelTimeNs = 0L
        var lastEmitMs = 0L

        val rotMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        val targetX = event.values[0] / GRAVITY_MS2
                        val targetY = event.values[1] / GRAVITY_MS2
                        val dt = if (lastAccelTimeNs == 0L) 0f
                        else (event.timestamp - lastAccelTimeNs) / 1_000_000_000f
                        lastAccelTimeNs = event.timestamp
                        if (dt > 0f) {
                            val alpha = (1f - exp(-dt / ACCEL_TAU_S)).coerceIn(0f, 1f)
                            gx += (targetX - gx) * alpha
                            gy += (targetY - gy) * alpha
                        } else {
                            gx = targetX
                            gy = targetY
                        }
                    }
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                        SensorManager.getOrientation(rotMatrix, orientation)
                        var az = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        if (az < 0f) az += 360f
                        heading = az
                    }
                }

                val now = System.currentTimeMillis()
                if (now - lastEmitMs > EMIT_THROTTLE_MS) {
                    lastEmitMs = now
                    trySend(SensorState(gx, gy, heading))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        rotation?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        linearAccel?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }

        trySend(SensorState())

        awaitClose { sensorManager.unregisterListener(listener) }
    }.shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    companion object {
        private const val GRAVITY_MS2 = 9.81f
        private const val ACCEL_TAU_S = 0.15f
        private const val EMIT_THROTTLE_MS = 33L

        @Volatile
        private var instance: SensorRepository? = null

        fun get(context: Context): SensorRepository =
            instance ?: synchronized(this) {
                instance ?: SensorRepository(context).also { instance = it }
            }
    }
}
