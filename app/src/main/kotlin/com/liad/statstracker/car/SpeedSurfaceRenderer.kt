package com.liad.statstracker.car

import android.graphics.Rect
import android.view.Choreographer
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
    private val choreographer = Choreographer.getInstance()

    private var surface: Surface? = null
    @Volatile private var stableArea: Rect? = null
    private var speedJob: Job? = null
    private var sensorJob: Job? = null
    private var availJob: Job? = null
    private var statsJob: Job? = null
    private var lastFrameNs: Long = 0
    private var running = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            if (isMenuVisible && frameTimeNanos > menuHideAtNs) {
                hideMenu()
            }
            val rawDt = if (lastFrameNs == 0L) 0f else (frameTimeNanos - lastFrameNs) / 1_000_000_000f
            lastFrameNs = frameTimeNanos
            val dt = if (rawDt > 0.1f) 0f else rawDt
            drawFrame(animator.tick(dt))
            choreographer.postFrameCallback(this)
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
        choreographer.postFrameCallback(frameCallback)
    }

    override fun onPause(owner: LifecycleOwner) {
        running = false
        choreographer.removeFrameCallback(frameCallback)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        running = false
        choreographer.removeFrameCallback(frameCallback)
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
    }
}
