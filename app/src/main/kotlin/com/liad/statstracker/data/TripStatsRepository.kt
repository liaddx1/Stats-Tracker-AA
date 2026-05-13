package com.liad.statstracker.data

import android.content.Context
import com.liad.statstracker.domain.SpeedState
import com.liad.statstracker.domain.TripStats
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TripStatsRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var maxKmh: Float = prefs.getFloat(KEY_MAX, 0f)
    private var distanceKm: Float = prefs.getFloat(KEY_DISTANCE, 0f)
    private var movingMs: Long = prefs.getLong(KEY_MOVING_MS, 0L)
    private var maxGforce: Float = prefs.getFloat(KEY_MAX_GFORCE, 0f)

    private var lastSampleMs: Long = 0L
    private var lastPersistMs: Long = 0L

    private val _state = MutableStateFlow(currentStats())
    val state: StateFlow<TripStats> = _state.asStateFlow()

    init {
        val speedRepo = SpeedRepository.get(appContext)
        scope.launch {
            speedRepo.state.collect { state ->
                if (state is SpeedState.Active) onSpeed(state.speedKmh)
            }
        }
        val sensorRepo = SensorRepository.get(appContext)
        scope.launch {
            sensorRepo.state.collect { sensor ->
                val mag = sqrt(sensor.gforceX * sensor.gforceX + sensor.gforceY * sensor.gforceY)
                if (mag > maxGforce) {
                    maxGforce = mag
                    _state.value = currentStats()
                }
            }
        }
    }

    private fun onSpeed(kmh: Float) {
        val now = System.currentTimeMillis()
        if (lastSampleMs > 0L) {
            val dtMs = (now - lastSampleMs).coerceIn(0L, MAX_GAP_MS)
            val dtSec = dtMs / 1000f
            if (kmh > MOVING_THRESHOLD_KMH) {
                distanceKm += kmh * dtSec / 3600f
                movingMs += dtMs
            }
            if (kmh > maxKmh) maxKmh = kmh
        }
        lastSampleMs = now
        _state.value = currentStats()
        if (now - lastPersistMs > PERSIST_INTERVAL_MS) {
            lastPersistMs = now
            persist()
        }
    }

    private fun currentStats(): TripStats {
        val durationSec = movingMs / 1000L
        val avg = if (durationSec > 0L) distanceKm * 3600f / durationSec else 0f
        return TripStats(
            maxKmh = maxKmh,
            avgKmh = avg,
            distanceKm = distanceKm,
            durationSec = durationSec,
            maxGforce = maxGforce,
        )
    }

    private fun persist() {
        prefs.edit()
            .putFloat(KEY_MAX, maxKmh)
            .putFloat(KEY_DISTANCE, distanceKm)
            .putLong(KEY_MOVING_MS, movingMs)
            .putFloat(KEY_MAX_GFORCE, maxGforce)
            .apply()
    }

    fun reset() {
        maxKmh = 0f
        distanceKm = 0f
        movingMs = 0L
        lastSampleMs = 0L
        maxGforce = 0f
        _state.value = currentStats()
        persist()
    }

    companion object {
        private const val PREFS = "trip_stats"
        private const val KEY_MAX = "max_kmh"
        private const val KEY_DISTANCE = "distance_km"
        private const val KEY_MOVING_MS = "moving_ms"
        private const val KEY_MAX_GFORCE = "max_gforce"
        private const val MOVING_THRESHOLD_KMH = 0.5f
        private const val MAX_GAP_MS = 5_000L
        private const val PERSIST_INTERVAL_MS = 10_000L

        @Volatile
        private var instance: TripStatsRepository? = null

        fun get(context: Context): TripStatsRepository =
            instance ?: synchronized(this) {
                instance ?: TripStatsRepository(context).also { instance = it }
            }
    }
}
