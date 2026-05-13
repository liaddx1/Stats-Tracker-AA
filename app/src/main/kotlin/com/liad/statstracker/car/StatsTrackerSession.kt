package com.liad.statstracker.car

import android.content.Intent
import android.content.pm.PackageManager
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.core.content.ContextCompat
import com.liad.statstracker.service.TripService

class StatsTrackerSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        val granted = ContextCompat.checkSelfPermission(
            carContext, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) TripService.start(carContext)
        return SpeedometerScreen(carContext)
    }
}
