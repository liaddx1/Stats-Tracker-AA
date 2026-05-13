package com.liad.statstracker.domain

enum class PerformanceType(
    val id: String,
    val displayName: String,
    val entryKmh: Float,
    val exitKmh: Float,
) {
    ZERO_TO_100("0_100", "0 → 100 km/h", 0f, 100f),
    EIGHTY_TO_120("80_120", "80 → 120 km/h", 80f, 120f),
    SIXTY_TO_130("60_130", "60 → 130 km/h", 60f, 130f);

    val isFromZero: Boolean get() = entryKmh == 0f

    companion object {
        fun fromId(id: String): PerformanceType? = entries.firstOrNull { it.id == id }
    }
}
