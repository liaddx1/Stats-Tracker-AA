package com.liad.statstracker.phone.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private val DefaultPrimary = Color(0xFF00F0FF)
private val DefaultAccent = Color(0xFFFF2A6D)

@Composable
fun SpeedReading(
    kmh: Int,
    numberSize: TextUnit = 28.sp,
    unitSize: TextUnit = 11.sp,
    primaryColor: Color = DefaultPrimary,
    accentColor: Color = DefaultAccent,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier,
    ) {
        Text(
            text = kmh.toString(),
            color = primaryColor,
            fontSize = numberSize,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            style = TextStyle(
                shadow = Shadow(
                    color = primaryColor,
                    offset = Offset.Zero,
                    blurRadius = 18f,
                ),
            ),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "KM/H",
            color = accentColor,
            fontSize = unitSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.4.em,
        )
    }
}
