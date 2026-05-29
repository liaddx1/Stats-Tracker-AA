package com.liad.statstracker.car

import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.liad.statstracker.data.LocationProvider
import com.liad.statstracker.data.SensorRepository
import com.liad.statstracker.data.SpeedRepository
import com.liad.statstracker.data.TripStatsRepository
import com.liad.statstracker.theme.RenderState
import com.liad.statstracker.theme.SpeedAnimator
import com.liad.statstracker.theme.SpeedTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SpeedSurfaceRenderer(
    private val carContext: CarContext,
    lifecycle: Lifecycle,
    private val speedRepo: SpeedRepository,
    private val sensorRepo: SensorRepository,
    private val locationProvider: LocationProvider,
    private val statsRepo: TripStatsRepository,
    theme: SpeedTheme,
    private val onMenuVisibilityChanged: () -> Unit,
) : SurfaceCallback, DefaultLifecycleObserver {

    @Volatile private var currentTheme: SpeedTheme = theme

    fun setTheme(theme: SpeedTheme) { currentTheme = theme }

    @Volatile var isMenuVisible: Boolean = false
        private set
    @Volatile private var menuHideAtNs: Long = 0L

    fun showMenu() {
        menuHideAtNs = System.nanoTime() + MENU_VISIBLE_DURATION_NS
        if (!isMenuVisible) {
            isMenuVisible = true
            onMenuVisibilityChanged()
        }
    }

    fun hideMenu() {
        menuHideAtNs = 0L
        if (isMenuVisible) {
            isMenuVisible = false
            onMenuVisibilityChanged()
        }
    }

    private val lifecycle = lifecycle
    private val animator = SpeedAnimator()
    private val frameHandler = Handler(Looper.getMainLooper())

    private var surface: Surface? = null
    @Volatile private var stableArea: Rect? = null
    private var speedJob: Job? = null
    private var sensorJob: Job? = null
    private var availJob: Job? = null
    private var statsJob: Job? = null
    private var lastFrameNs: Long = 0
    private var running = false

    // Driven by a main-thread Handler rather than Choreographer. Choreographer
    // frame callbacks are tied to the phone display's VSYNC and stop firing once
    // the phone screen turns off (locks), which froze the car surface on its last
    // frame. The main Looper keeps delivering delayed messages with the screen
    // off, so the car surface keeps rendering while the phone is locked.
    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            val nowNs = System.nanoTime()
            if (isMenuVisible && nowNs > menuHideAtNs) {
                hideMenu()
            }
            val rawDt = if (lastFrameNs == 0L) 0f else (nowNs - lastFrameNs) / 1_000_000_000f
            lastFrameNs = nowNs
            val dt = if (rawDt > 0.1f) 0f else rawDt
            drawFrame(animator.tick(dt))
            frameHandler.postDelayed(this, FRAME_INTERVAL_MS)
        }
    }

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(this)
        speedJob = lifecycle.coroutineScope.launch {
            try {
                speedRepo.state.collectLatest { animator.setSpeed(it) }
            } catch (_: Exception) {
            }
        }
        sensorJob = lifecycle.coroutineScope.launch {
            try {
                sensorRepo.state.collectLatest { animator.setSensors(it) }
            } catch (_: Exception) {
            }
        }
        availJob = lifecycle.coroutineScope.launch {
            try {
                locationProvider.available.collectLatest { animator.setLocationAvailable(it) }
            } catch (_: Exception) {
            }
        }
        statsJob = lifecycle.coroutineScope.launch {
            try {
                statsRepo.state.collectLatest { animator.setStats(it) }
            } catch (_: Exception) {
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        running = true
        lastFrameNs = 0
        frameHandler.removeCallbacks(frameRunnable)
        frameHandler.post(frameRunnable)
    }

    override fun onPause(owner: LifecycleOwner) {
        running = false
        frameHandler.removeCallbacks(frameRunnable)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        running = false
        frameHandler.removeCallbacks(frameRunnable)
        speedJob?.cancel()
        sensorJob?.cancel()
        availJob?.cancel()
        statsJob?.cancel()
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        surface = surfaceContainer.surface
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        this.stableArea = stableArea
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        surface = null
    }

    override fun onClick(x: Float, y: Float) {
        showMenu()
    }

    private fun drawFrame(render: RenderState) {
        val s = surface ?: return
        val canvas = try {
            s.lockCanvas(null)
        } catch (_: IllegalStateException) {
            return
        } ?: return
        try {
            currentTheme.render(canvas, canvas.width, canvas.height, render)
        } finally {
            s.unlockCanvasAndPost(canvas)
        }
    }

    private companion object {
        const val MENU_VISIBLE_DURATION_NS = 5_000_000_000L
        const val FRAME_INTERVAL_MS = 16L
    }
}
