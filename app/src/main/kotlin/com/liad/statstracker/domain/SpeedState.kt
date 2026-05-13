package com.liad.statstracker.domain

sealed interface SpeedState {
    data object NoFix : SpeedState

    data class Active(
        val speedKmh: Float,
        val accuracyKmh: Float,
        val bearingDeg: Float?,
    ) : SpeedState
}
