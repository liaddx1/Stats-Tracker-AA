package com.liad.statstracker.domain

sealed interface PerformanceState {

    val type: PerformanceType?

    data object Idle : PerformanceState {
        override val type: PerformanceType? = null
    }

    data class Arming(
        override val type: PerformanceType,
        val secondsLeft: Int,
    ) : PerformanceState

    data class Waiting(
        override val type: PerformanceType,
        val currentKmh: Float,
    ) : PerformanceState

    data class Measuring(
        override val type: PerformanceType,
        val elapsedSec: Float,
        val currentKmh: Float,
    ) : PerformanceState

    data class Finished(
        override val type: PerformanceType,
        val run: PerformanceRun,
    ) : PerformanceState

    data class Aborted(
        override val type: PerformanceType,
        val reason: String,
    ) : PerformanceState
}
