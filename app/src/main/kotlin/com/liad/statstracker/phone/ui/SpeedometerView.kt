package com.liad.statstracker.phone.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liad.statstracker.domain.SensorState
import com.liad.statstracker.domain.SpeedState
import com.liad.statstracker.domain.TripStats
import com.liad.statstracker.theme.RenderState
import com.liad.statstracker.theme.SpeedAnimator
import com.liad.statstracker.theme.SpeedTheme

@Composable
fun SpeedometerView(
    speedState: SpeedState,
    sensorState: SensorState,
    locationAvailable: Boolean,
    tripStats: TripStats,
    onResetTrip: () -> Unit,
    onCycleTheme: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenStats: () -> Unit,
    theme: SpeedTheme,
    modifier: Modifier = Modifier,
) {
    val animator = remember { SpeedAnimator() }
    var render by remember { mutableStateOf(RenderState()) }

    LaunchedEffect(speedState) { animator.setSpeed(speedState) }
    LaunchedEffect(sensorState) { animator.setSensors(sensorState) }
    LaunchedEffect(locationAvailable) { animator.setLocationAvailable(locationAvailable) }
    LaunchedEffect(tripStats) { animator.setStats(tripStats) }

    LaunchedEffect(Unit) {
        var lastNs = 0L
        while (true) {
            withFrameNanos { frameNs ->
                // Treat any gap > 100ms as a fresh start — covers app returning from
                // background, where withFrameNanos pauses with stale lastNs and would
                // otherwise feed a huge dt into the spring integrator.
                val rawDt = if (lastNs == 0L) 0f else (frameNs - lastNs) / 1_000_000_000f
                lastNs = frameNs
                val dt = if (rawDt > 0.1f) 0f else rawDt
                render = animator.tick(dt)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            val w = size.width.toInt()
            val h = size.height.toInt()
            theme.render(drawContext.canvas.nativeCanvas, w, h, render)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            val btnColor = Color(0xFFFF2A6D)
            listOf(
                "⟳ RESET" to onResetTrip,
                "◈ THEME" to onCycleTheme,
                "▶ MEASURE" to onOpenMenu,
                "▤ STATS" to onOpenStats,
            ).forEach { (label, action) ->
                Text(
                    text = label,
                    color = btnColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, btnColor, RoundedCornerShape(4.dp))
                        .clickable(onClick = action)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
