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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liad.statstracker.domain.PerformanceRun
import com.liad.statstracker.domain.PerformanceType
import com.liad.statstracker.domain.TripStats
import com.liad.statstracker.theme.UiColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(
    uiColors: UiColors,
    tripStats: TripStats,
    history: List<PerformanceRun>,
    onResetTrip: () -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(uiColors.bg)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("STATS", color = uiColors.primary, fontSize = 32.sp, fontWeight = FontWeight.Black)

        SectionHeader("Trip", uiColors)
        StatSpeedRow("Max speed", tripStats.maxKmh.toInt(), uiColors)
        StatSpeedRow("Average", tripStats.avgKmh.toInt(), uiColors)
        StatRow("Distance", "%.1f km".format(tripStats.distanceKm), uiColors)
        StatRow("Moving time", formatDuration(tripStats.durationSec), uiColors)
        StatRow("Max G-force", "%.2f G".format(tripStats.maxGforce), uiColors)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallButton("RESET TRIP", uiColors.accent, onResetTrip)
            SmallButton("CLEAR RUNS", uiColors.accent, onClearHistory)
        }

        SectionHeader("Performance runs", uiColors)
        if (history.isEmpty()) {
            Text("No runs yet. Try one from the menu.", color = uiColors.dim, fontSize = 13.sp)
        } else {
            BestRow(history, uiColors)
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(history) { run -> RunRow(run, uiColors) }
                }
            }
        }

        SmallButton("← BACK", uiColors.primary, onBack)
    }
}

@Composable
private fun SectionHeader(text: String, uiColors: UiColors) {
    Text(text, color = uiColors.accent, fontSize = 14.sp, fontWeight = FontWeight.Black)
}

@Composable
private fun StatRow(label: String, value: String, uiColors: UiColors) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = uiColors.dim, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = uiColors.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatSpeedRow(label: String, kmh: Int, uiColors: UiColors) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = uiColors.dim, fontSize = 14.sp, modifier = Modifier.weight(1f))
        SpeedReading(kmh = kmh, numberSize = 26.sp, unitSize = 11.sp, primaryColor = uiColors.primary, accentColor = uiColors.accent)
    }
}

@Composable
private fun BestRow(history: List<PerformanceRun>, uiColors: UiColors) {
    val bests = PerformanceType.entries.mapNotNull { type ->
        history.filter { it.type == type }.minByOrNull { it.timeSec }?.let { type to it }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("BEST", color = uiColors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        bests.forEach { (type, run) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(type.displayName, color = uiColors.dim, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("%.2f s".format(run.timeSec), color = uiColors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RunRow(run: PerformanceRun, uiColors: UiColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, uiColors.dim.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(run.type.displayName, color = uiColors.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(formatDate(run.timestampMs), color = uiColors.dim, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("%.2f s".format(run.timeSec), color = uiColors.accent, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text("peak ${run.peakKmh.toInt()}", color = uiColors.dim, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SmallButton(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = label,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

private fun formatDuration(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%dh %02dm".format(h, m) else "%dm %02ds".format(m, s)
}

private val dateFmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
private fun formatDate(ms: Long): String = dateFmt.format(Date(ms))
