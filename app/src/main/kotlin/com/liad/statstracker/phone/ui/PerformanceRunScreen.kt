package com.liad.statstracker.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liad.statstracker.domain.PerformanceState
import com.liad.statstracker.theme.UiColors

private val Green = Color(0xFF39FF99)

@Composable
fun PerformanceRunScreen(
    uiColors: UiColors,
    state: PerformanceState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(uiColors.bg)
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header(state, uiColors)
            Box(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.align(Alignment.Center)) { Body(state, uiColors) }
            }
            Footer(state, uiColors, onCancel, onDismiss)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Header(state: PerformanceState, uiColors: UiColors) {
    val title = when (state) {
        is PerformanceState.Arming -> state.type.displayName
        is PerformanceState.Waiting -> state.type.displayName
        is PerformanceState.Measuring -> state.type.displayName
        is PerformanceState.Finished -> state.type.displayName
        is PerformanceState.Aborted -> state.type.displayName
        PerformanceState.Idle -> ""
    }
    Column {
        Spacer(Modifier.height(8.dp))
        Text(title, color = uiColors.accent, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text(statusLabel(state), color = uiColors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun statusLabel(state: PerformanceState): String = when (state) {
    is PerformanceState.Arming -> "ARMING"
    is PerformanceState.Waiting -> if (state.type.isFromZero) "WAITING FOR LAUNCH" else "WAITING TO CROSS ${state.type.entryKmh.toInt()} KM/H"
    is PerformanceState.Measuring -> "MEASURING"
    is PerformanceState.Finished -> "RESULT"
    is PerformanceState.Aborted -> "ABORTED"
    PerformanceState.Idle -> ""
}

@Composable
private fun Body(state: PerformanceState, uiColors: UiColors) {
    when (state) {
        is PerformanceState.Arming -> Centered {
            Text(
                state.secondsLeft.toString(),
                color = uiColors.primary,
                fontSize = 200.sp,
                fontWeight = FontWeight.Black,
            )
            Text("GET READY", color = uiColors.accent, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
        is PerformanceState.Waiting -> Centered {
            val isGo = state.type.isFromZero
            Text(
                if (isGo) "GO" else "ACCELERATE",
                color = uiColors.accent,
                fontSize = if (isGo) 64.sp else 40.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
            )
            SpeedReading(kmh = state.currentKmh.toInt(), numberSize = 44.sp, unitSize = 16.sp, primaryColor = uiColors.primary, accentColor = uiColors.accent)
        }
        is PerformanceState.Measuring -> Centered {
            Text(
                formatSec(state.elapsedSec),
                color = uiColors.primary,
                fontSize = 96.sp,
                fontWeight = FontWeight.Black,
            )
            SpeedReading(kmh = state.currentKmh.toInt(), numberSize = 52.sp, unitSize = 18.sp, primaryColor = uiColors.primary, accentColor = uiColors.accent)
        }
        is PerformanceState.Finished -> Centered {
            Text(formatSec(state.run.timeSec), color = Green, fontSize = 96.sp, fontWeight = FontWeight.Black)
            Text("seconds", color = uiColors.dim, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "PEAK",
                    color = uiColors.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp, bottom = 6.dp),
                )
                SpeedReading(kmh = state.run.peakKmh.toInt(), numberSize = 32.sp, unitSize = 12.sp, primaryColor = uiColors.primary, accentColor = uiColors.accent)
            }
        }
        is PerformanceState.Aborted -> Centered {
            Text("ABORTED", color = uiColors.accent, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text(state.reason, color = uiColors.dim, fontSize = 16.sp)
        }
        PerformanceState.Idle -> Unit
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun Footer(state: PerformanceState, uiColors: UiColors, onCancel: () -> Unit, onDismiss: () -> Unit) {
    val terminal = state is PerformanceState.Finished || state is PerformanceState.Aborted
    val label = if (terminal) "DONE" else "CANCEL"
    val color = if (terminal) uiColors.primary else uiColors.accent
    val action = if (terminal) onDismiss else onCancel
    Text(
        text = label,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .clickable(onClick = action)
            .padding(horizontal = 32.dp, vertical = 12.dp),
    )
}

private fun formatSec(sec: Float): String = "%.2f".format(sec)
