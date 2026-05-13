package com.liad.statstracker.domain

data class SensorState(
    val gforceX: Float = 0f,
    val gforceY: Float = 0f,
    val headingDeg: Float? = null,
)
