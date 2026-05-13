package com.liad.statstracker.data

import android.content.Context
import android.location.Location
import com.liad.statstracker.domain.SpeedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlin.math.abs

class SpeedRepository private constructor(context: Context) {

    private val locationProvider = LocationProvider.get(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var lastValidKmh: Float = 0f
    private var lastValidElapsedNs: Long = 0L
    private var consecutiveRejects: Int = 0

    val state: SharedFlow<SpeedState> = locationProvider.location
        .transform { loc -> toSpeedState(loc)?.let { emit(it) } }
        .onStart { emit(SpeedState.NoFix) }
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private fun toSpeedState(loc: Location): SpeedState? {
        if (!loc.hasSpeed()) return SpeedState.NoFix
        val rawKmh = (loc.speed * 3.6f).coerceAtLeast(0f)
        val hasAcc = loc.hasSpeedAccuracy()
        val accMps = if (hasAcc) loc.speedAccuracyMetersPerSecond else 0f
        val accKmh = accMps * 3.6f

        if (hasAcc && accMps > MAX_ACCURACY_MPS && consecutiveRejects < MAX_CONSECUTIVE_REJECTS) {
            consecutiveRejects++
            return null
        }

        val nowNs = loc.elapsedRealtimeNanos
        if (lastValidElapsedNs > 0L && rawKmh > STATIONARY_KMH) {
            val dtSec = (nowNs - lastValidElapsedNs) / 1_000_000_000f
            if (dtSec in MIN_DT_S..MAX_DT_S) {
                val accelKmhPerSec = abs(rawKmh - lastValidKmh) / dtSec
                if (accelKmhPerSec > MAX_ACCEL_KMH_PER_S && consecutiveRejects < MAX_CONSECUTIVE_REJECTS) {
                    consecutiveRejects++
                    return null
                }
            }
        }

        consecutiveRejects = 0
        lastValidKmh = rawKmh
        lastValidElapsedNs = nowNs

        val kmh = if (rawKmh < STATIONARY_KMH || rawKmh < accKmh * ACCURACY_MULTIPLIER) 0f else rawKmh
        val bearing = if (loc.hasBearing() && loc.speed > 0.5f) loc.bearing else null
        return SpeedState.Active(speedKmh = kmh, accuracyKmh = accKmh, bearingDeg = bearing)
    }

    companion object {
        private const val STATIONARY_KMH = 4f
        private const val ACCURACY_MULTIPLIER = 1.5f
        private const val MAX_ACCURACY_MPS = 5f
        private const val MAX_ACCEL_KMH_PER_S = 50f
        private const val MIN_DT_S = 0.05f
        private const val MAX_DT_S = 5f
        private const val MAX_CONSECUTIVE_REJECTS = 3

        @Volatile
        private var instance: SpeedRepository? = null

        fun get(context: Context): SpeedRepository =
            instance ?: synchronized(this) {
                instance ?: SpeedRepository(context).also { instance = it }
            }
    }
}
