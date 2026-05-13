package com.liad.statstracker.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.liad.statstracker.data.PerformanceRepository
import com.liad.statstracker.domain.PerformanceState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

class PerformanceRunCarScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private val perfRepo = PerformanceRepository.get(carContext)
    private var stateJob: Job? = null
    private var lastSignature: String = ""

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        stateJob = lifecycle.coroutineScope.launch {
            perfRepo.state.collect { state ->
                val sig = signature(state)
                if (sig != lastSignature) {
                    lastSignature = sig
                    invalidate()
                }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        stateJob?.cancel()
        stateJob = null
    }

    override fun onGetTemplate(): Template {
        val state = perfRepo.state.value
        val terminal = state is PerformanceState.Finished || state is PerformanceState.Aborted

        val pane = Pane.Builder().apply {
            addRow(buildRow(state))
            addAction(
                Action.Builder()
                    .setTitle(if (terminal) "DONE" else "CANCEL")
                    .setOnClickListener {
                        perfRepo.cancel()
                        perfRepo.dismiss()
                        screenManager.pop()
                    }
                    .build()
            )
        }.build()

        return PaneTemplate.Builder(pane)
            .setTitle(state.type?.displayName ?: "MEASURE")
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun buildRow(state: PerformanceState): Row = when (state) {
        is PerformanceState.Arming -> Row.Builder()
            .setTitle("ARMING — ${state.secondsLeft}")
            .addText("Get ready · ${state.type.displayName}")
            .build()

        is PerformanceState.Waiting -> {
            val title = if (state.type.isFromZero) {
                "Waiting for launch"
            } else {
                "Cross ${state.type.entryKmh.toInt()} km/h to start"
            }
            Row.Builder()
                .setTitle(title)
                .addText("${state.currentKmh.toInt()} km/h")
                .build()
        }

        is PerformanceState.Measuring -> Row.Builder()
            .setTitle(String.format(Locale.US, "Measuring · %.2f s", state.elapsedSec))
            .addText("${state.currentKmh.toInt()} km/h")
            .build()

        is PerformanceState.Finished -> Row.Builder()
            .setTitle(String.format(Locale.US, "%.2f s", state.run.timeSec))
            .addText("${state.type.displayName} · peak ${state.run.peakKmh.toInt()} km/h")
            .build()

        is PerformanceState.Aborted -> Row.Builder()
            .setTitle("ABORTED")
            .addText(state.reason)
            .build()

        PerformanceState.Idle -> Row.Builder()
            .setTitle("Ready")
            .addText("Pick a measurement")
            .build()
    }

    private fun signature(state: PerformanceState): String = when (state) {
        is PerformanceState.Arming -> "arming-${state.secondsLeft}"
        is PerformanceState.Waiting -> "waiting-${(state.currentKmh / 5f).toInt()}"
        is PerformanceState.Measuring -> "measuring-${state.elapsedSec.toInt()}"
        is PerformanceState.Finished -> "finished-${state.run.timestampMs}"
        is PerformanceState.Aborted -> "aborted-${state.reason}"
        PerformanceState.Idle -> "idle"
    }
}
