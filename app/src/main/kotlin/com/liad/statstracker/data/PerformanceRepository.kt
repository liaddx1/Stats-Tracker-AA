package com.liad.statstracker.data

import android.content.Context
import com.liad.statstracker.domain.PerformanceRun
import com.liad.statstracker.domain.PerformanceState
import com.liad.statstracker.domain.PerformanceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class PerformanceRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow<PerformanceState>(PerformanceState.Idle)
    val state: StateFlow<PerformanceState> = _state.asStateFlow()

    private val _history = MutableStateFlow(loadHistory())
    val history: StateFlow<List<PerformanceRun>> = _history.asStateFlow()

    private var armingJob: Job? = null
    private var locationJob: Job? = null

    private var startTimeMs: Long = 0L
    private var peakKmh: Float = 0f
    private var prevSampleMs: Long = 0L
    private var prevSampleKmh: Float = -1f

    fun start(type: PerformanceType) {
        cancelInternal(emitAborted = false)
        peakKmh = 0f
        startTimeMs = 0L
        prevSampleMs = 0L
        prevSampleKmh = -1f

        if (type.isFromZero) {
            armingJob = scope.launch {
                for (s in ARMING_SECONDS downTo 1) {
                    _state.value = PerformanceState.Arming(type, s)
                    delay(1_000L)
                }
                _state.value = PerformanceState.Waiting(type, 0f)
            }
        } else {
            _state.value = PerformanceState.Waiting(type, 0f)
        }

        locationJob = scope.launch {
            LocationProvider.get(appContext).location.collect { loc ->
                val kmh = (loc.speed * 3.6f).coerceAtLeast(0f)
                onSample(System.currentTimeMillis(), kmh)
            }
        }
    }

    fun cancel() {
        cancelInternal(emitAborted = true)
    }

    fun dismiss() {
        cancelInternal(emitAborted = false)
        _state.value = PerformanceState.Idle
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun cancelInternal(emitAborted: Boolean) {
        armingJob?.cancel()
        armingJob = null
        locationJob?.cancel()
        locationJob = null
        if (emitAborted) {
            val current = _state.value
            val type = current.type
            if (type != null && current !is PerformanceState.Finished && current !is PerformanceState.Aborted) {
                _state.value = PerformanceState.Aborted(type, "Cancelled")
            }
        }
    }

    private fun onSample(nowMs: Long, kmh: Float) {
        val current = _state.value
        when (current) {
            is PerformanceState.Arming -> {
                if (kmh > MOVE_DURING_ARMING_KMH) {
                    finishAborted(current.type, "Moved before launch")
                }
            }
            is PerformanceState.Waiting -> handleWaiting(current, nowMs, kmh)
            is PerformanceState.Measuring -> handleMeasuring(current, nowMs, kmh)
            else -> Unit
        }
        prevSampleMs = nowMs
        prevSampleKmh = kmh
    }

    private fun handleWaiting(s: PerformanceState.Waiting, nowMs: Long, kmh: Float) {
        val type = s.type
        if (type.isFromZero) {
            if (kmh > MOTION_THRESHOLD_KMH) {
                startTimeMs = nowMs
                peakKmh = kmh
                _state.value = PerformanceState.Measuring(type, 0f, kmh)
            } else {
                _state.value = PerformanceState.Waiting(type, kmh)
            }
        } else {
            if (prevSampleKmh in 0f..type.entryKmh && kmh >= type.entryKmh) {
                val crossingMs = interpolateCrossing(
                    prevSampleMs, prevSampleKmh, nowMs, kmh, type.entryKmh
                )
                startTimeMs = crossingMs
                peakKmh = kmh
                val elapsed = (nowMs - startTimeMs).coerceAtLeast(0L) / 1000f
                _state.value = PerformanceState.Measuring(type, elapsed, kmh)
            } else {
                _state.value = PerformanceState.Waiting(type, kmh)
            }
        }
    }

    private fun handleMeasuring(s: PerformanceState.Measuring, nowMs: Long, kmh: Float) {
        val type = s.type
        if (kmh > peakKmh) peakKmh = kmh

        val droppedBelowEntry = if (type.isFromZero) {
            kmh < ABORT_BELOW_KMH && (nowMs - startTimeMs) > 500L
        } else {
            kmh < (type.entryKmh - HYSTERESIS_KMH)
        }
        if (droppedBelowEntry) {
            finishAborted(type, "Speed dropped")
            return
        }

        if (prevSampleKmh in 0f..type.exitKmh && kmh >= type.exitKmh) {
            val crossingMs = interpolateCrossing(
                prevSampleMs, prevSampleKmh, nowMs, kmh, type.exitKmh
            )
            val timeSec = (crossingMs - startTimeMs).coerceAtLeast(0L) / 1000f
            val run = PerformanceRun(
                type = type,
                timeSec = timeSec,
                peakKmh = peakKmh,
                timestampMs = System.currentTimeMillis(),
            )
            saveRun(run)
            cancelInternal(emitAborted = false)
            _state.value = PerformanceState.Finished(type, run)
            return
        }

        val elapsed = (nowMs - startTimeMs).coerceAtLeast(0L) / 1000f
        _state.value = PerformanceState.Measuring(type, elapsed, kmh)
    }

    private fun finishAborted(type: PerformanceType, reason: String) {
        cancelInternal(emitAborted = false)
        _state.value = PerformanceState.Aborted(type, reason)
    }

    private fun interpolateCrossing(
        t1Ms: Long, v1: Float, t2Ms: Long, v2: Float, threshold: Float,
    ): Long {
        if (t1Ms <= 0L || v2 == v1) return t2Ms
        val frac = ((threshold - v1) / (v2 - v1)).coerceIn(0f, 1f)
        return t1Ms + (frac * (t2Ms - t1Ms)).toLong()
    }

    private fun saveRun(run: PerformanceRun) {
        val updated = (listOf(run) + _history.value).take(MAX_HISTORY)
        _history.value = updated
        val arr = JSONArray()
        updated.forEach { r ->
            arr.put(
                JSONObject()
                    .put("type", r.type.id)
                    .put("time", r.timeSec.toDouble())
                    .put("peak", r.peakKmh.toDouble())
                    .put("ts", r.timestampMs)
            )
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    private fun loadHistory(): List<PerformanceRun> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val type = PerformanceType.fromId(o.getString("type")) ?: continue
                    add(
                        PerformanceRun(
                            type = type,
                            timeSec = o.getDouble("time").toFloat(),
                            peakKmh = o.getDouble("peak").toFloat(),
                            timestampMs = o.getLong("ts"),
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val PREFS = "performance_runs"
        private const val KEY_HISTORY = "history"
        private const val MAX_HISTORY = 50
        private const val ARMING_SECONDS = 3
        private const val MOVE_DURING_ARMING_KMH = 1.5f
        private const val MOTION_THRESHOLD_KMH = 1.5f
        private const val ABORT_BELOW_KMH = 0.5f
        private const val HYSTERESIS_KMH = 5f

        @Volatile
        private var instance: PerformanceRepository? = null

        fun get(context: Context): PerformanceRepository =
            instance ?: synchronized(this) {
                instance ?: PerformanceRepository(context).also { instance = it }
            }
    }
}
