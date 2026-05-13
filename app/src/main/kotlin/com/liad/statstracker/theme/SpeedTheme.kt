package com.liad.statstracker.theme

import android.graphics.Canvas
import androidx.compose.ui.graphics.Color

data class UiColors(
    val bg: Color,
    val primary: Color,
    val accent: Color,
    val dim: Color,
)

interface SpeedTheme {
    val uiColors: UiColors
    fun render(canvas: Canvas, width: Int, height: Int, render: RenderState)
}
