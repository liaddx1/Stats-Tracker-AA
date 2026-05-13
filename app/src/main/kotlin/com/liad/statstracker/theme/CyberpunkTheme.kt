package com.liad.statstracker.theme



import android.graphics.Bitmap

import android.graphics.BitmapShader

import android.graphics.Canvas

import android.graphics.Paint

import android.graphics.RectF

import android.graphics.Shader

import android.graphics.Typeface

import java.util.Locale

import kotlin.math.abs

import kotlin.math.cos

import kotlin.math.min

import kotlin.math.sin

import kotlin.math.sqrt

import kotlin.math.tanh



class CyberpunkTheme : SpeedTheme {

    override val uiColors = UiColors(
        bg      = androidx.compose.ui.graphics.Color(0xFF050810),
        primary = androidx.compose.ui.graphics.Color(0xFF00F0FF),
        accent  = androidx.compose.ui.graphics.Color(0xFFFF2A6D),
        dim     = androidx.compose.ui.graphics.Color(0xFFB8C7D4),
    )



    private val bg = 0xFF050810.toInt()

    private val grid = 0xFF0F2030.toInt()

    private val cyan = 0xFF00F0FF.toInt()

    private val cyanDim = 0x4000F0FF.toInt()

    private val cyanFaint = 0x2000F0FF.toInt()

    private val magenta = 0xFFFF2A6D.toInt()

    private val yellow = 0xFFF7E96B.toInt()

    private val red = 0xFFFF3055.toInt()

    private val tickMinor = 0xFF6B7E8C.toInt()

    private val tickMajor = 0xFFB8C7D4.toInt()

    private val labelDim = 0xFF6B7E8C.toInt()

    private val scanLine = 0x14_00F0FF.toInt()



    private val arcStartDeg = 135f

    private val arcSweepDeg = 270f



    private val gridPaint = Paint().apply {

        style = Paint.Style.STROKE

        strokeWidth = 1f

        color = grid

    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        style = Paint.Style.STROKE

        strokeCap = Paint.Cap.ROUND

    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        style = Paint.Style.STROKE

        strokeCap = Paint.Cap.ROUND

    }

    private val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        textAlign = Paint.Align.CENTER

        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

        color = cyan

    }

    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        textAlign = Paint.Align.CENTER

        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

        letterSpacing = 0.4f

        color = magenta

    }

    private val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        textAlign = Paint.Align.CENTER

        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        letterSpacing = 0.3f

        color = magenta

    }

    private val tickLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        textAlign = Paint.Align.CENTER

        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

        color = labelDim

    }

    private val compassLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        style = Paint.Style.STROKE

        strokeWidth = 1.5f

        color = cyanDim

    }

    private val compassLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        textAlign = Paint.Align.CENTER

        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    }

    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        style = Paint.Style.STROKE

        strokeWidth = 3f

        strokeCap = Paint.Cap.ROUND

    }

    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val bubbleLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        textAlign = Paint.Align.CENTER

        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

        color = labelDim

    }

    private val scanPaint = Paint()

    private var scanShader: BitmapShader? = null

    private val statsLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        color = labelDim
        letterSpacing = 0.18f
    }
    private val statsValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        color = cyan
    }



    private val arcRect = RectF()



    override fun render(canvas: Canvas, width: Int, height: Int, render: RenderState) {

        canvas.drawColor(bg)

        drawGrid(canvas, width, height)



        val compassZoneH = height * COMPASS_ZONE_FRACTION

        val cx = width / 2f

        val gaugeAreaTop = compassZoneH

        val gaugeAreaBottom = height * 0.96f

        val cy = gaugeAreaTop + (gaugeAreaBottom - gaugeAreaTop) * 0.50f

        val sideReserve = min(width, height) * 0.10f + 16f

        val maxRadiusFromWidth = (width / 2f - sideReserve).coerceAtLeast(0f)

        val radius = min(
            min(width * 0.48f, maxRadiusFromWidth),
            (gaugeAreaBottom - gaugeAreaTop) * 0.55f,
        )



        drawDimArc(canvas, cx, cy, radius)

        drawSpeedArc(canvas, cx, cy, radius, render)

        drawTicks(canvas, cx, cy, radius, render.maxKmh)

        drawTickLabels(canvas, cx, cy, radius, render.maxKmh)

        drawCenterNumber(canvas, cx, cy, radius, render)

        val unitBaseline = drawUnit(canvas, cx, cy, radius)



        drawCompass(canvas, width, height, render.headingDeg)

        drawGforce(canvas, width, height, render.gforceX, render.gforceY)

        drawAccelBar(canvas, width, height, render.accelKmhPerSec)



        drawStats(canvas, width, height, render)
        drawScanLines(canvas, width, height)

        drawStatus(canvas, cx, unitBaseline, radius, render)

    }



    private companion object {

        const val COMPASS_ZONE_FRACTION = 0.13f

        const val GFORCE_DOT_FRACTION = 0.13f

    }



    private fun drawGrid(canvas: Canvas, w: Int, h: Int) {

        val spacing = 36f

        var x = 0f

        while (x < w) {

            canvas.drawLine(x, 0f, x, h.toFloat(), gridPaint)

            x += spacing

        }

        var y = 0f

        while (y < h) {

            canvas.drawLine(0f, y, w.toFloat(), y, gridPaint)

            y += spacing

        }

    }



    private fun drawDimArc(canvas: Canvas, cx: Float, cy: Float, r: Float) {

        arcRect.set(cx - r, cy - r, cx + r, cy + r)

        arcPaint.color = cyanDim

        arcPaint.strokeWidth = 6f

        arcPaint.clearShadowLayer()

        canvas.drawArc(arcRect, arcStartDeg, arcSweepDeg, false, arcPaint)

    }



    private fun drawSpeedArc(canvas: Canvas, cx: Float, cy: Float, r: Float, render: RenderState) {

        val frac = (render.kmh / render.maxKmh).coerceIn(0f, 1f)

        if (frac <= 0.001f) return

        arcRect.set(cx - r, cy - r, cx + r, cy + r)



        val color = when {

            frac > 0.85f -> magenta

            frac > 0.65f -> yellow

            else -> cyan

        }

        arcPaint.color = color

        arcPaint.strokeWidth = 10f

        arcPaint.setShadowLayer(22f, 0f, 0f, color)

        canvas.drawArc(arcRect, arcStartDeg, frac * arcSweepDeg, false, arcPaint)

        arcPaint.clearShadowLayer()

    }



    private fun drawTicks(canvas: Canvas, cx: Float, cy: Float, r: Float, maxKmh: Float) {

        val minorStep = 20f

        val majorStep = 40f

        val outerR = r * 0.985f

        val majorLen = r * 0.055f

        val minorLen = r * 0.032f

        val majorStroke = (r * 0.018f).coerceAtLeast(3f)

        val minorStroke = (r * 0.008f).coerceAtLeast(1f)

        var kmh = 0f

        while (kmh <= maxKmh + 0.01f) {

            val frac = kmh / maxKmh

            val rad = Math.toRadians((arcStartDeg + frac * arcSweepDeg).toDouble())

            val isMajor = kmh % majorStep == 0f

            val tickLen = if (isMajor) majorLen else minorLen

            val innerR = outerR - tickLen

            tickPaint.strokeWidth = if (isMajor) majorStroke else minorStroke

            tickPaint.color = if (isMajor) tickMajor else tickMinor

            val x1 = cx + (outerR * cos(rad)).toFloat()

            val y1 = cy + (outerR * sin(rad)).toFloat()

            val x2 = cx + (innerR * cos(rad)).toFloat()

            val y2 = cy + (innerR * sin(rad)).toFloat()

            canvas.drawLine(x1, y1, x2, y2, tickPaint)

            kmh += minorStep

        }

    }



    private fun drawTickLabels(canvas: Canvas, cx: Float, cy: Float, r: Float, maxKmh: Float) {

        val labelR = r * 0.78f

        tickLabelPaint.textSize = r * 0.105f

        var kmh = 0f

        while (kmh <= maxKmh + 0.01f) {

            val frac = kmh / maxKmh

            val rad = Math.toRadians((arcStartDeg + frac * arcSweepDeg).toDouble())

            val x = cx + (labelR * cos(rad)).toFloat()

            val y = cy + (labelR * sin(rad)).toFloat() + tickLabelPaint.textSize * 0.35f

            canvas.drawText(kmh.toInt().toString(), x, y, tickLabelPaint)

            kmh += 40f

        }

    }



    private fun drawCenterNumber(canvas: Canvas, cx: Float, cy: Float, r: Float, render: RenderState) {

        val text = if (render.hasFix) render.kmh.toInt().toString() else "--"

        val baseSize = r * 0.72f

        numberPaint.textSize = if (text.length >= 3) baseSize * 0.82f else baseSize

        numberPaint.setShadowLayer(r * 0.06f, 0f, 0f, cyan)

        canvas.drawText(text, cx, cy + numberPaint.textSize * 0.32f, numberPaint)

        numberPaint.clearShadowLayer()

    }



    private fun drawUnit(canvas: Canvas, cx: Float, cy: Float, r: Float): Float {

        unitPaint.textSize = r * 0.14f

        val y = cy + r * 0.54f

        canvas.drawText("KM/H", cx, y, unitPaint)

        return y

    }



    private fun drawCompass(canvas: Canvas, w: Int, h: Int, headingDeg: Float?) {

        val zoneH = h * COMPASS_ZONE_FRACTION

        val stripTop = zoneH * 0.12f

        val stripBottom = zoneH * 0.55f

        val cardinalSize = zoneH * 0.40f

        val interSize = zoneH * 0.30f

        val numericSize = zoneH * 0.28f

        val tickCardinal = zoneH * 0.16f

        val tickInter = zoneH * 0.12f

        val tickMinorH = zoneH * 0.07f

        val labelBaseline = stripBottom - zoneH * 0.04f

        val numericBaseline = zoneH * 0.96f



        val centerX = w / 2f

        val visibleSpan = 130f

        val pxPerDeg = w / visibleSpan



        compassLinePaint.color = cyanDim

        compassLinePaint.strokeWidth = 1.5f

        canvas.drawLine(0f, stripBottom, w.toFloat(), stripBottom, compassLinePaint)



        val heading = headingDeg

        if (heading == null) {

            compassLabelPaint.color = labelDim

            compassLabelPaint.textSize = numericSize

            compassLabelPaint.letterSpacing = 0.3f

            canvas.drawText("HDG --", centerX, numericBaseline, compassLabelPaint)

            compassLabelPaint.letterSpacing = 0f

            return

        }



        for (deg in 0 until 360 step 15) {

            var dx = (deg - heading)

            while (dx > 180f) dx -= 360f

            while (dx < -180f) dx += 360f

            if (abs(dx) > visibleSpan / 2f) continue

            val x = centerX + dx * pxPerDeg

            val isCardinal = deg % 90 == 0

            val isInter = deg % 45 == 0



            val tickH = when {

                isCardinal -> tickCardinal

                isInter -> tickInter

                else -> tickMinorH

            }

            compassLinePaint.color = if (isCardinal) cyanDim else cyanFaint

            canvas.drawLine(x, stripBottom, x, stripBottom + tickH, compassLinePaint)



            if (isInter) {

                compassLabelPaint.textSize = if (isCardinal) cardinalSize else interSize

                compassLabelPaint.color = if (isCardinal) cyan else cyanDim

                canvas.drawText(cardinalLabel(deg), x, labelBaseline, compassLabelPaint)

            }

        }



        cursorPaint.color = magenta

        cursorPaint.strokeWidth = zoneH * 0.04f

        cursorPaint.setShadowLayer(zoneH * 0.12f, 0f, 0f, magenta)

        canvas.drawLine(centerX, stripTop, centerX, stripBottom + tickCardinal + zoneH * 0.05f, cursorPaint)

        cursorPaint.clearShadowLayer()



        compassLabelPaint.textSize = numericSize

        compassLabelPaint.color = magenta

        compassLabelPaint.letterSpacing = 0.2f

        canvas.drawText(

            String.format(Locale.US, "%03d°", heading.toInt()),

            centerX, numericBaseline, compassLabelPaint

        )

        compassLabelPaint.letterSpacing = 0f

    }



    private fun cardinalLabel(deg: Int): String = when (deg) {

        0 -> "N"

        45 -> "NE"

        90 -> "E"

        135 -> "SE"

        180 -> "S"

        225 -> "SW"

        270 -> "W"

        315 -> "NW"

        else -> deg.toString()

    }



    private fun drawGforce(canvas: Canvas, w: Int, h: Int, gx: Float, gy: Float) {

        val r = min(w, h) * 0.10f

        val cx = r + 32f

        val cy = h - r - r * 0.8f



        bubblePaint.style = Paint.Style.STROKE

        bubblePaint.strokeWidth = 2.5f

        bubblePaint.color = cyanDim

        canvas.drawCircle(cx, cy, r, bubblePaint)

        canvas.drawCircle(cx, cy, r * 0.66f, bubblePaint)

        canvas.drawCircle(cx, cy, r * 0.33f, bubblePaint)

        bubblePaint.color = cyanFaint

        canvas.drawLine(cx - r, cy, cx + r, cy, bubblePaint)

        canvas.drawLine(cx, cy - r, cx, cy + r, bubblePaint)



        val mag = sqrt(gx * gx + gy * gy)

        val saturationG = 2f

        val maxFraction = 1f - GFORCE_DOT_FRACTION - 0.04f

        val fraction = tanh(mag / saturationG) * maxFraction

        val unitX = if (mag > 0f) gx / mag else 0f

        val unitY = if (mag > 0f) gy / mag else 0f

        val dotX = cx + unitX * fraction * r

        val dotY = cy - unitY * fraction * r



        val color = when {

            mag > 0.7f -> magenta

            mag > 0.4f -> yellow

            else -> cyan

        }



        val saved = canvas.save()

        canvas.clipPath(android.graphics.Path().apply { addCircle(cx, cy, r, android.graphics.Path.Direction.CW) })

        bubblePaint.style = Paint.Style.FILL

        bubblePaint.color = color

        bubblePaint.setShadowLayer(16f, 0f, 0f, color)

        canvas.drawCircle(dotX, dotY, r * GFORCE_DOT_FRACTION, bubblePaint)

        bubblePaint.clearShadowLayer()

        canvas.restoreToCount(saved)



        bubbleLabelPaint.textSize = r * 0.34f

        bubbleLabelPaint.color = labelDim

        canvas.drawText(

            String.format(Locale.US, "%.2f G", mag),

            cx, cy + r + bubbleLabelPaint.textSize + 6f, bubbleLabelPaint

        )

    }



    private fun drawAccelBar(canvas: Canvas, w: Int, h: Int, accelKmhPerSec: Float) {

        val barW = min(w, h) * 0.045f

        val barH = min(w, h) * 0.22f

        val cx = w - barW * 0.5f - 48f

        val cy = h - barH * 0.5f - barW * 2.6f

        val cornerR = barW * 0.5f



        val frame = RectF(cx - barW / 2f, cy - barH / 2f, cx + barW / 2f, cy + barH / 2f)



        rectPaint.style = Paint.Style.STROKE

        rectPaint.strokeWidth = 2f

        rectPaint.color = cyanDim

        canvas.drawRoundRect(frame, cornerR, cornerR, rectPaint)



        rectPaint.color = cyanFaint

        rectPaint.strokeWidth = 1.5f

        canvas.drawLine(cx - barW * 0.65f, cy, cx + barW * 0.65f, cy, rectPaint)



        val maxAccel = 12f

        val ratio = (accelKmhPerSec / maxAccel).coerceIn(-1f, 1f)

        if (abs(ratio) > 0.02f) {

            val color = if (ratio > 0) cyan else red

            rectPaint.style = Paint.Style.FILL

            rectPaint.color = color

            rectPaint.setShadowLayer(14f, 0f, 0f, color)

            val fillRect = if (ratio > 0) {

                RectF(cx - barW / 2f + 2f, cy - barH / 2f * ratio, cx + barW / 2f - 2f, cy)

            } else {

                RectF(cx - barW / 2f + 2f, cy, cx + barW / 2f - 2f, cy + barH / 2f * (-ratio))

            }

            canvas.drawRoundRect(fillRect, cornerR * 0.6f, cornerR * 0.6f, rectPaint)

            rectPaint.clearShadowLayer()

        }



        bubbleLabelPaint.textSize = barW * 1.0f

        bubbleLabelPaint.color = labelDim

        bubbleLabelPaint.letterSpacing = 0f

        canvas.drawText(

            String.format(Locale.US, "%+.1f", accelKmhPerSec),

            cx, cy + barH / 2f + bubbleLabelPaint.textSize + 8f, bubbleLabelPaint

        )



        bubbleLabelPaint.textSize = barW * 0.7f

        bubbleLabelPaint.color = magenta

        bubbleLabelPaint.letterSpacing = 0.3f

        canvas.drawText("ACCEL", cx, cy - barH / 2f - bubbleLabelPaint.textSize * 0.5f, bubbleLabelPaint)

        bubbleLabelPaint.letterSpacing = 0f

    }



    private fun drawStats(canvas: Canvas, w: Int, h: Int, render: RenderState) {
        fun fmt(v: Float) = if (v <= 0f) "--" else v.toInt().toString()
        fun fmtKm(v: Float) = if (v <= 0f) "--" else String.format(Locale.US, "%.1f", v)

        val items = listOf(
            Triple("MAX", fmt(render.tripMaxKmh), cyan),
            Triple("AVG", fmt(render.tripAvgKmh), cyan),
            Triple("KM", fmtKm(render.tripDistanceKm), magenta),
        )

        if (w > h) {
            // Car: vertical column on the left, above the G-Force bubble
            val r = min(w, h) * 0.10f
            val colX = r + 32f
            val compassBottom = h * COMPASS_ZONE_FRACTION
            val gforceCy = h - r - r * 0.8f
            val available = gforceCy - r - compassBottom - 16f
            val itemH = available / items.size
            val labelSize = (itemH * 0.26f).coerceAtMost(h * 0.040f)
            val valueSize = (itemH * 0.46f).coerceAtMost(h * 0.068f)

            items.forEachIndexed { i, (label, value, color) ->
                val centerY = compassBottom + i * itemH + itemH * 0.38f
                statsLabelPaint.textSize = labelSize
                statsLabelPaint.color = labelDim
                canvas.drawText(label, colX, centerY, statsLabelPaint)
                statsValuePaint.textSize = valueSize
                statsValuePaint.color = color
                statsValuePaint.setShadowLayer(valueSize * 0.3f, 0f, 0f, color)
                canvas.drawText(value, colX, centerY + valueSize * 1.05f, statsValuePaint)
                statsValuePaint.clearShadowLayer()
            }
        } else {
            // Phone: horizontal row in the band between compass and gauge arc top
            val compassBottom = h * COMPASS_ZONE_FRACTION
            val gaugeAreaBottom = h * 0.96f
            val cy = compassBottom + (gaugeAreaBottom - compassBottom) * 0.50f
            val sideReserve = min(w, h) * 0.10f + 16f
            val maxR = (w / 2f - sideReserve).coerceAtLeast(0f)
            val radius = min(min(w * 0.48f, maxR), (gaugeAreaBottom - compassBottom) * 0.55f)
            val gaugeTop = cy - radius
            val midY = compassBottom + (gaugeTop - compassBottom) * 0.52f
            val zone = gaugeTop - compassBottom
            val labelSize = (zone * 0.16f).coerceAtMost(w * 0.040f)
            val valueSize = (zone * 0.30f).coerceAtMost(w * 0.065f)
            val itemWidth = w * 0.28f
            val cx = w / 2f

            items.forEachIndexed { i, (label, value, color) ->
                val itemX = cx + (i - 1) * itemWidth
                statsLabelPaint.textSize = labelSize
                statsLabelPaint.color = labelDim
                canvas.drawText(label, itemX, midY - valueSize * 0.15f, statsLabelPaint)
                statsValuePaint.textSize = valueSize
                statsValuePaint.color = color
                statsValuePaint.setShadowLayer(valueSize * 0.3f, 0f, 0f, color)
                canvas.drawText(value, itemX, midY + valueSize * 0.85f, statsValuePaint)
                statsValuePaint.clearShadowLayer()
            }
        }
    }

    private fun drawScanLines(canvas: Canvas, w: Int, h: Int) {

        if (scanShader == null) {

            val tile = Bitmap.createBitmap(1, 4, Bitmap.Config.ARGB_8888)

            tile.setPixel(0, 0, scanLine)

            scanShader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)

            scanPaint.shader = scanShader

        }

        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), scanPaint)

    }



    private fun drawStatus(canvas: Canvas, cx: Float, unitBaseline: Float, r: Float, render: RenderState) {

        if (render.hasFix) return

        statusPaint.textSize = r * 0.085f

        val (text, color) = if (!render.locationAvailable) {

            "● LOCATION OFF — CHECK SETTINGS" to red

        } else {

            "● ACQUIRING GPS" to magenta

        }

        statusPaint.color = color

        canvas.drawText(text, cx, unitBaseline + statusPaint.textSize * 1.6f, statusPaint)

    }

}