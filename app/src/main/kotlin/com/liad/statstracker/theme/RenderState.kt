package com.liad.statstracker.theme

data class RenderState(
    val kmh: Float = 0f,
    val hasFix: Boolean = false,
    val maxKmh: Float = 240f,
    val headingDeg: Float? = null,
    val gforceX: Float = 0f,
    val gforceY: Float = 0f,
    val accelKmhPerSec: Float = 0f,
    val locationAvailable: Boolean = true,
    val tripMaxKmh: Float = 0f,
    val tripAvgKmh: Float = 0f,
    val tripDistanceKm: Float = 0f,
)
