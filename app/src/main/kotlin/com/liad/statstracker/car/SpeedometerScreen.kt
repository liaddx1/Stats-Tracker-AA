package com.liad.statstracker.car

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import com.liad.statstracker.data.LocationProvider
import com.liad.statstracker.data.SensorRepository
import com.liad.statstracker.data.SpeedRepository
import com.liad.statstracker.data.TripStatsRepository
import com.liad.statstracker.theme.CyberpunkTheme
import com.liad.statstracker.theme.F1Theme
import com.liad.statstracker.theme.MinimalistTheme
import com.liad.statstracker.theme.SpeedTheme

class SpeedometerScreen(carContext: CarContext) : Screen(carContext) {

    private val statsRepo = TripStatsRepository.get(carContext)

    private val themes: List<SpeedTheme> = listOf(CyberpunkTheme(), F1Theme(), MinimalistTheme())
    private val themeNames = listOf("CYBERPUNK", "F1", "MINIMAL")
    private var themeIndex = 0

    private val renderer = SpeedSurfaceRenderer(
        carContext = carContext,
        lifecycle = lifecycle,
        speedRepo = SpeedRepository.get(carContext),
        sensorRepo = SensorRepository.get(carContext),
        locationProvider = LocationProvider.get(carContext),
        statsRepo = statsRepo,
        theme = themes[themeIndex],
        onMenuVisibilityChanged = { invalidate() },
    )

    private fun hiddenStrip(): ActionStrip = ActionStrip.Builder()
        .addAction(Action.APP_ICON)
        .build()

    private fun visibleStrip(): ActionStrip = ActionStrip.Builder()
        .addAction(
            Action.Builder()
                .setTitle("THEME")
                .setOnClickListener {
                    themeIndex = (themeIndex + 1) % themes.size
                    renderer.setTheme(themes[themeIndex])
                    CarToast.makeText(carContext, themeNames[themeIndex], CarToast.LENGTH_SHORT).show()
                    renderer.showMenu()
                }
                .build()
        )
        .addAction(
            Action.Builder()
                .setTitle("MEASURE")
                .setOnClickListener {
                    renderer.hideMenu()
                    screenManager.push(PerformanceMenuCarScreen(carContext))
                }
                .build()
        )
        .addAction(
            Action.Builder()
                .setTitle("STATS")
                .setOnClickListener {
                    renderer.hideMenu()
                    screenManager.push(StatsCarScreen(carContext))
                }
                .build()
        )
        .addAction(
            Action.Builder()
                .setTitle("RESET")
                .setOnClickListener {
                    statsRepo.reset()
                    CarToast.makeText(carContext, "Trip reset", CarToast.LENGTH_SHORT).show()
                    renderer.hideMenu()
                }
                .build()
        )
        .build()

    override fun onGetTemplate(): Template {
        val actionStrip = if (renderer.isMenuVisible) visibleStrip() else hiddenStrip()
        val bg = 0xFF050810.toInt()
        return NavigationTemplate.Builder()
            .setActionStrip(actionStrip)
            .setBackgroundColor(CarColor.createCustom(bg, bg))
            .build()
    }
}
