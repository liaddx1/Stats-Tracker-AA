package com.liad.statstracker.phone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liad.statstracker.data.LocationProvider
import com.liad.statstracker.data.PerformanceRepository
import com.liad.statstracker.data.SensorRepository
import com.liad.statstracker.data.SpeedRepository
import com.liad.statstracker.data.TripStatsRepository
import com.liad.statstracker.domain.PerformanceRun
import com.liad.statstracker.domain.PerformanceState
import com.liad.statstracker.domain.SensorState
import com.liad.statstracker.domain.SpeedState
import com.liad.statstracker.domain.TripStats
import com.liad.statstracker.phone.ui.PerformanceMenuScreen
import com.liad.statstracker.phone.ui.PerformanceRunScreen
import com.liad.statstracker.phone.ui.SetupScreen
import com.liad.statstracker.phone.ui.SpeedometerView
import com.liad.statstracker.phone.ui.StatsScreen
import com.liad.statstracker.service.TripService
import com.liad.statstracker.theme.CyberpunkTheme
import com.liad.statstracker.theme.F1Theme
import com.liad.statstracker.theme.MinimalistTheme
import com.liad.statstracker.theme.SpeedTheme

private enum class Screen { Speedometer, PerformanceMenu, PerformanceRun, Stats }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (hasFineLocation()) TripService.start(this)
        setContent { Root() }
    }

    @Composable
    private fun Root() {
        var hasPerm by remember { mutableStateOf(hasFineLocation()) }
        val notificationLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }
        val backgroundLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasPerm = granted
            if (granted) {
                TripService.start(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocation()) {
                    backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
        }

        if (!hasPerm) {
            SetupScreen(onGrantLocation = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) })
            return
        }

        val themes = remember<List<SpeedTheme>> { listOf(CyberpunkTheme(), F1Theme(), MinimalistTheme()) }
        var themeIndex by remember { mutableIntStateOf(0) }
        var screen by remember { mutableStateOf(Screen.Speedometer) }

        val speedState by SpeedRepository.get(this).state
            .collectAsStateWithLifecycle(initialValue = SpeedState.NoFix)
        val sensorState by SensorRepository.get(this).state
            .collectAsStateWithLifecycle(initialValue = SensorState())
        val locationAvailable by LocationProvider.get(this).available
            .collectAsStateWithLifecycle()
        val statsRepo = remember { TripStatsRepository.get(this) }
        val tripStats by statsRepo.state
            .collectAsStateWithLifecycle(initialValue = TripStats())
        val perfRepo = remember { PerformanceRepository.get(this) }
        val perfState by perfRepo.state
            .collectAsStateWithLifecycle(initialValue = PerformanceState.Idle)
        val perfHistory by perfRepo.history
            .collectAsStateWithLifecycle(initialValue = emptyList<PerformanceRun>())

        when (screen) {
            Screen.Speedometer -> SpeedometerView(
                speedState = speedState,
                sensorState = sensorState,
                locationAvailable = locationAvailable,
                tripStats = tripStats,
                onResetTrip = { statsRepo.reset() },
                onCycleTheme = { themeIndex = (themeIndex + 1) % themes.size },
                onOpenMenu = { screen = Screen.PerformanceMenu },
                onOpenStats = { screen = Screen.Stats },
                theme = themes[themeIndex],
            )
            Screen.PerformanceMenu -> PerformanceMenuScreen(
                uiColors = themes[themeIndex].uiColors,
                onSelect = { type ->
                    perfRepo.start(type)
                    screen = Screen.PerformanceRun
                },
                onBack = { screen = Screen.Speedometer },
            )
            Screen.PerformanceRun -> PerformanceRunScreen(
                uiColors = themes[themeIndex].uiColors,
                state = perfState,
                onCancel = {
                    perfRepo.cancel()
                    perfRepo.dismiss()
                    screen = Screen.PerformanceMenu
                },
                onDismiss = {
                    perfRepo.dismiss()
                    screen = Screen.Stats
                },
            )
            Screen.Stats -> StatsScreen(
                uiColors = themes[themeIndex].uiColors,
                tripStats = tripStats,
                history = perfHistory,
                onResetTrip = { statsRepo.reset() },
                onClearHistory = { perfRepo.clearHistory() },
                onBack = { screen = Screen.Speedometer },
            )
        }
    }

    private fun hasFineLocation(): Boolean =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasBackgroundLocation(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}
