package com.liad.statstracker.domain

data class PerformanceRun(
    val type: PerformanceType,
    val timeSec: Float,
    val peakKmh: Float,
    val timestampMs: Long,
)
