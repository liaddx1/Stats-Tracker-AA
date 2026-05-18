package com.liad.statstracker.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

private val Bg = Color(0xFF050810)
private val Cyan = Color(0xFF00F0FF)
private val Magenta = Color(0xFFFF2A6D)
private val DimText = Color(0xFFB8C7D4)

@Composable
fun SetupScreen(onGrantLocation: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text("STATS TRACKER", color = Cyan, fontSize = 36.sp, fontWeight = FontWeight.Black)
        Text("Cyberpunk speedometer for Android Auto", color = Magenta, fontSize = 14.sp)

        SectionHeader("Location access required")
        Text(
            "Speed comes from GPS. Grant precise location to start the speedometer. " +
                "Choose \"Allow all the time\" so the app keeps tracking when your phone " +
                "screen is locked in the car.",
            color = DimText, fontSize = 14.sp
        )
        CyberButton(text = "GRANT LOCATION", onClick = onGrantLocation)

        SectionHeader("Android Auto setup")
        Step("1", "Enable Developer Mode in Android Auto",
            "Open the Android Auto app on your phone → Settings → scroll to bottom → tap \"Version\" 10 times.")
        Step("2", "Allow unknown sources",
            "Back in Android Auto Settings, top-right menu → Developer settings → enable \"Unknown sources\".")
        Step("3", "Connect your phone",
            "Plug into your car's USB port. \"Stats Tracker\" appears in the Android Auto launcher.")

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun Step(num: String, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(num, color = Magenta, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Column {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(body, color = DimText, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CyberButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Cyan,
        fontSize = 14.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, Cyan, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
    )
}
