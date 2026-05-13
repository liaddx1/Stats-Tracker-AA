package com.liad.statstracker.domain

data class TripStats(
    val maxKmh: Float = 0f,
    val avgKmh: Float = 0f,
    val distanceKm: Float = 0f,
    val durationSec: Long = 0L,
    val maxGforce: Float = 0f,
)
