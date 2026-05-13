package com.liad.statstracker.theme



import android.os.SystemClock

import com.liad.statstracker.domain.SensorState

import com.liad.statstracker.domain.SpeedState

import com.liad.statstracker.domain.TripStats

import kotlin.math.abs

import kotlin.math.exp



class SpeedAnimator(

    private val speedOmega: Float = 3f,

    private val speedZeta: Float = 1f,

    private val headingTau: Float = 0.2f,

    ) {

    private var displayedKmh = 0f

    private var velocityKmhPerSec = 0f

    private var displayedHeading = 0f

    private var speedTarget = 0f

    private var headingTarget = 0f

    private var hasFix = false

    private var hasHeading = false

    private var gx = 0f

    private var gy = 0f

    private var locationAvailable = true

    private var stats: TripStats = TripStats()

    private var prevSpeedKmh = 0f

    private var prevSpeedTimestampMs = 0L

    private var smoothedAccelKmhPerSec = 0f

    private var displayedAccelKmhPerSec = 0f



    fun setSpeed(state: SpeedState) {

        when (state) {

            is SpeedState.Active -> {

                val nowMs = SystemClock.elapsedRealtime()

                if (hasFix && prevSpeedTimestampMs > 0L) {

                    val dt = (nowMs - prevSpeedTimestampMs) / 1000f

                    if (dt in 0.1f..5f) {

                        val rawAccel = (state.speedKmh - prevSpeedKmh) / dt

                        val alpha = 1f - exp(-dt / ACCEL_SMOOTH_TAU_S)

                        smoothedAccelKmhPerSec += (rawAccel - smoothedAccelKmhPerSec) * alpha

                    }

                }

                prevSpeedKmh = state.speedKmh

                prevSpeedTimestampMs = nowMs

                hasFix = true

                speedTarget = state.speedKmh

                if (state.bearingDeg != null && state.speedKmh > 5f) {

                    headingTarget = state.bearingDeg

                    hasHeading = true

                }

            }

            SpeedState.NoFix -> {

                hasFix = false

                speedTarget = 0f

                smoothedAccelKmhPerSec = 0f

                displayedAccelKmhPerSec = 0f

                prevSpeedTimestampMs = 0L

            }

        }

    }



    fun setSensors(sensors: SensorState) {

        gx = sensors.gforceX

        gy = sensors.gforceY

        if (!isMovingFix() && sensors.headingDeg != null) {

            headingTarget = sensors.headingDeg

            hasHeading = true

        }

    }



    fun setLocationAvailable(available: Boolean) {

        locationAvailable = available

    }



    fun setStats(stats: TripStats) {

        this.stats = stats

    }



    fun tick(dtSeconds: Float, maxKmh: Float = 240f): RenderState {

        if (dtSeconds > 0f) {

            val k = speedOmega * speedOmega

            val c = 2f * speedZeta * speedOmega

            val springAccel = -k * (displayedKmh - speedTarget) - c * velocityKmhPerSec

            velocityKmhPerSec += springAccel * dtSeconds

            displayedKmh += velocityKmhPerSec * dtSeconds



            if (abs(speedTarget - displayedKmh) < 0.05f && abs(velocityKmhPerSec) < 0.1f) {

                displayedKmh = speedTarget

                velocityKmhPerSec = 0f

            }



            val accelAlpha = 1f - exp(-dtSeconds / ACCEL_DISPLAY_TAU_S)

            displayedAccelKmhPerSec += (smoothedAccelKmhPerSec - displayedAccelKmhPerSec) * accelAlpha



            if (hasHeading) {

                val ah = (1f - exp(-dtSeconds / headingTau)).coerceIn(0f, 1f)

                var diff = headingTarget - displayedHeading

                while (diff > 180f) diff -= 360f

                while (diff < -180f) diff += 360f

                displayedHeading = ((displayedHeading + diff * ah) % 360f + 360f) % 360f

            }

        }



        return RenderState(

            kmh = displayedKmh,

            hasFix = hasFix,

            maxKmh = maxKmh,

            headingDeg = if (hasHeading) displayedHeading else null,

            gforceX = gx,

            gforceY = gy,

            accelKmhPerSec = displayedAccelKmhPerSec,

            locationAvailable = locationAvailable,

            tripMaxKmh = stats.maxKmh,

            tripAvgKmh = stats.avgKmh,

            tripDistanceKm = stats.distanceKm,

            )

    }



    private fun isMovingFix(): Boolean = hasFix && speedTarget > 5f

    companion object {

        private const val ACCEL_SMOOTH_TAU_S = 1.5f

        private const val ACCEL_DISPLAY_TAU_S = 0.15f

    }

}