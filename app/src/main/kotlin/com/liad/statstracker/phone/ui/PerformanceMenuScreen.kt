package com.liad.statstracker.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liad.statstracker.domain.PerformanceType
import com.liad.statstracker.theme.UiColors

@Composable
fun PerformanceMenuScreen(
    uiColors: UiColors,
    onSelect: (PerformanceType) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(uiColors.bg)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text("PERFORMANCE", color = uiColors.primary, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Text("Pick a measurement", color = uiColors.accent, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))

        PerformanceType.entries.forEach { type ->
            MenuItem(
                title = type.displayName,
                subtitle = subtitleFor(type),
                uiColors = uiColors,
                onClick = { onSelect(type) },
            )
        }

        Spacer(Modifier.height(8.dp))
        BackButton(uiColors, onBack)
    }
}

private fun subtitleFor(type: PerformanceType): String = when {
    type.isFromZero -> "Standing start · 3s countdown"
    else -> "Rolling start · auto-trigger at ${type.entryKmh.toInt()} km/h"
}

@Composable
private fun MenuItem(title: String, subtitle: String, uiColors: UiColors, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, uiColors.primary, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(title, color = uiColors.primary, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = uiColors.dim, fontSize = 12.sp)
    }
}

@Composable
private fun BackButton(uiColors: UiColors, onBack: () -> Unit) {
    Text(
        text = "← BACK",
        color = uiColors.accent,
        fontSize = 14.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, uiColors.accent, RoundedCornerShape(4.dp))
            .clickable(onClick = onBack)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
