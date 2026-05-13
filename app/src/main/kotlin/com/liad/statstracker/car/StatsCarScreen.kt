package com.liad.statstracker.car

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.liad.statstracker.data.PerformanceRepository
import com.liad.statstracker.data.TripStatsRepository
import com.liad.statstracker.domain.PerformanceType
import java.util.Locale

class StatsCarScreen(carContext: CarContext) : Screen(carContext) {

    private val statsRepo = TripStatsRepository.get(carContext)
    private val perfRepo = PerformanceRepository.get(carContext)

    override fun onGetTemplate(): Template {
        val trip = statsRepo.state.value
        val history = perfRepo.history.value

        val list = ItemList.Builder().apply {
            addItem(
                Row.Builder()
                    .setTitle("Trip — Max ${trip.maxKmh.toInt()} km/h")
                    .addText("Avg ${trip.avgKmh.toInt()} km/h")
                    .build()
            )
            addItem(
                Row.Builder()
                    .setTitle(String.format(Locale.US, "%.1f km", trip.distanceKm))
                    .addText("${formatDuration(trip.durationSec)} moving")
                    .build()
            )
            addItem(
                Row.Builder()
                    .setTitle(String.format(Locale.US, "%.2f G", trip.maxGforce))
                    .addText("Max G-force")
                    .build()
            )
            PerformanceType.entries.forEach { type ->
                val best = history.filter { it.type == type }.minByOrNull { it.timeSec }
                val title = if (best != null) {
                    String.format(Locale.US, "%.2f s", best.timeSec)
                } else {
                    "—"
                }
                val subtitle = if (best != null) {
                    "Best ${type.displayName} · peak ${best.peakKmh.toInt()} km/h"
                } else {
                    "Best ${type.displayName}"
                }
                addItem(Row.Builder().setTitle(title).addText(subtitle).build())
            }
            addItem(
                Row.Builder()
                    .setTitle("Clear performance runs")
                    .setOnClickListener {
                        perfRepo.clearHistory()
                        CarToast.makeText(carContext, "Runs cleared", CarToast.LENGTH_SHORT).show()
                        invalidate()
                    }
                    .build()
            )
        }.build()

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("RESET")
                    .setOnClickListener {
                        statsRepo.reset()
                        CarToast.makeText(carContext, "Trip reset", CarToast.LENGTH_SHORT).show()
                        invalidate()
                    }
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setTitle("STATS")
            .setHeaderAction(Action.BACK)
            .setSingleList(list)
            .setActionStrip(actionStrip)
            .build()
    }

    private fun formatDuration(sec: Long): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) String.format(Locale.US, "%dh %02dm", h, m)
        else String.format(Locale.US, "%dm %02ds", m, s)
    }
}
