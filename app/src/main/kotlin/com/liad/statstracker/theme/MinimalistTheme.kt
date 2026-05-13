package com.liad.statstracker.theme

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

class MinimalistTheme : SpeedTheme {

    override val uiColors = UiColors(
        bg      = androidx.compose.ui.graphics.Color(0xFF000000),
        primary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        accent  = androidx.compose.ui.graphics.Color(0xFF4A4A4A),
        dim     = androidx.compose.ui.graphics.Color(0xFF4A4A4A),
    )

    private val bg = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val dim = 0xFF4A4A4A.toInt()
    private val faint = 0xFF1C1C1C.toInt()

    private val arcStartDeg = 135f
    private val arcSweepDeg = 270f
    private val arcRect = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        color = faint
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = white
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
        color = dim
    }
    private val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        color = white
    }
    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        letterSpacing = 0.35f
        color = dim
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        color = dim
    }
    private val compassLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    private val compassLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        color = dim
    }
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val statsLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        letterSpacing = 0.10f
        color = dim
    }
    private val statsValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        color = white
    }

    private companion object {
        const val COMPASS_ZONE_FRACTION = 0.13f
        const val GFORCE_DOT_FRACTION = 0.13f
    }

    override fun render(canvas: Canvas, width: Int, height: Int, render: RenderState) {
        canvas.drawColor(bg)

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

        drawTrackArc(canvas, cx, cy, radius)
        drawSpeedArc(canvas, cx, cy, radius, render)
        drawTicks(canvas, cx, cy, radius, render.maxKmh)
        drawTickLabels(canvas, cx, cy, radius, render.maxKmh)
        drawCenterNumber(canvas, cx, cy, radius, render)
        val unitY = drawUnit(canvas, cx, cy, radius)

        drawCompass(canvas, width, height, render.headingDeg)
        drawGforce(canvas, width, height, render.gforceX, render.gforceY)
        drawAccelBar(canvas, width, height, render.accelKmhPerSec)
        drawStats(canvas, width, height, render)
        drawStatus(canvas, cx, unitY, radius, render)
    }

    private fun drawTrackArc(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        arcRect.set(cx - r, cy - r, cx + r, cy + r)
        trackPaint.strokeWidth = r * 0.012f
        canvas.drawArc(arcRect, arcStartDeg, arcSweepDeg, false, trackPaint)
    }

    private fun drawSpeedArc(canvas: Canvas, cx: Float, cy: Float, r: Float, render: RenderState) {
        val frac = (render.kmh / render.maxKmh).coerceIn(0f, 1f)
        if (frac <= 0.001f) return
        arcRect.set(cx - r, cy - r, cx + r, cy + r)
        arcPaint.strokeWidth = r * 0.018f
        canvas.drawArc(arcRect, arcStartDeg, frac * arcSweepDeg, false, arcPaint)
    }

    private fun drawTicks(canvas: Canvas, cx: Float, cy: Float, r: Float, maxKmh: Float) {
        // Major ticks only — keeps it clean
        val outerR = r * 0.985f
        val innerR = outerR - r * 0.038f
        tickPaint.strokeWidth = r * 0.010f
        var kmh = 0f
        while (kmh <= maxKmh + 0.01f) {
            val frac = kmh / maxKmh
            val rad = Math.toRadians((arcStartDeg + frac * arcSweepDeg).toDouble())
            canvas.drawLine(
                cx + (outerR * cos(rad)).toFloat(), cy + (outerR * sin(rad)).toFloat(),
                cx + (innerR * cos(rad)).toFloat(), cy + (innerR * sin(rad)).toFloat(), tickPaint,
            )
            kmh += 40f
        }
    }

    private fun drawTickLabels(canvas: Canvas, cx: Float, cy: Float, r: Float, maxKmh: Float) {
        labelPaint.textSize = r * 0.09f
        val labelR = r * 0.79f
        var kmh = 0f
        while (kmh <= maxKmh + 0.01f) {
            val frac = kmh / maxKmh
            val rad = Math.toRadians((arcStartDeg + frac * arcSweepDeg).toDouble())
            val x = cx + (labelR * cos(rad)).toFloat()
            val y = cy + (labelR * sin(rad)).toFloat() + labelPaint.textSize * 0.35f
            canvas.drawText(kmh.toInt().toString(), x, y, labelPaint)
            kmh += 40f
        }
    }

    private fun drawCenterNumber(canvas: Canvas, cx: Float, cy: Float, r: Float, render: RenderState) {
        val text = if (render.hasFix) render.kmh.toInt().toString() else "--"
        val baseSize = r * 0.72f
        numberPaint.textSize = if (text.length >= 3) baseSize * 0.82f else baseSize
        canvas.drawText(text, cx, cy + numberPaint.textSize * 0.32f, numberPaint)
    }

    private fun drawUnit(canvas: Canvas, cx: Float, cy: Float, r: Float): Float {
        unitPaint.textSize = r * 0.11f
        val y = cy + r * 0.52f
        canvas.drawText("KM/H", cx, y, unitPaint)
        return y
    }

    private fun drawCompass(canvas: Canvas, w: Int, h: Int, headingDeg: Float?) {
        val zoneH = h * COMPASS_ZONE_FRACTION
        val stripBottom = zoneH * 0.55f
        val cardinalSize = zoneH * 0.36f
        val interSize = zoneH * 0.26f
        val numericSize = zoneH * 0.25f
        val tickCardinal = zoneH * 0.12f
        val tickInter = zoneH * 0.08f
        val tickMinorH = zoneH * 0.05f
        val labelBaseline = stripBottom - zoneH * 0.04f
        val numericBaseline = zoneH * 0.94f
        val centerX = w / 2f
        val visibleSpan = 130f
        val pxPerDeg = w / visibleSpan

        compassLinePaint.color = faint
        compassLinePaint.strokeWidth = 0.8f
        canvas.drawLine(0f, stripBottom, w.toFloat(), stripBottom, compassLinePaint)

        val heading = headingDeg
        if (heading == null) {
            compassLabelPaint.color = dim
            compassLabelPaint.textSize = numericSize
            canvas.drawText("—", centerX, numericBaseline, compassLabelPaint)
            return
        }

        for (deg in 0 until 360 step 15) {
            var dx = deg - heading
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
            compassLinePaint.color = if (isCardinal) dim else faint
            canvas.drawLine(x, stripBottom, x, stripBottom + tickH, compassLinePaint)
            if (isInter) {
                compassLabelPaint.textSize = if (isCardinal) cardinalSize else interSize
                compassLabelPaint.color = if (isCardinal) white else dim
                canvas.drawText(cardinalLabel(deg), x, labelBaseline, compassLabelPaint)
            }
        }

        compassLinePaint.color = white
        compassLinePaint.strokeWidth = zoneH * 0.03f
        canvas.drawLine(centerX, 0f, centerX, stripBottom + tickCardinal + zoneH * 0.04f, compassLinePaint)
        compassLinePaint.strokeWidth = 0.8f

        compassLabelPaint.textSize = numericSize
        compassLabelPaint.color = white
        canvas.drawText(String.format(Locale.US, "%03d°", heading.toInt()), centerX, numericBaseline, compassLabelPaint)
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
        val mag = sqrt(gx * gx + gy * gy)
        val fraction = tanh(mag / 2f) * (1f - GFORCE_DOT_FRACTION - 0.04f)
        val unitX = if (mag > 0f) gx / mag else 0f
        val unitY = if (mag > 0f) gy / mag else 0f

        bubblePaint.style = Paint.Style.STROKE
        bubblePaint.strokeWidth = 0.8f
        bubblePaint.color = faint
        canvas.drawCircle(cx, cy, r, bubblePaint)
        canvas.drawCircle(cx, cy, r * 0.5f, bubblePaint)
        canvas.drawLine(cx - r, cy, cx + r, cy, bubblePaint)
        canvas.drawLine(cx, cy - r, cx, cy + r, bubblePaint)

        val dotX = cx + unitX * fraction * r
        val dotY = cy - unitY * fraction * r
        val saved = canvas.save()
        canvas.clipPath(android.graphics.Path().apply {
            addCircle(cx, cy, r, android.graphics.Path.Direction.CW)
        })
        bubblePaint.style = Paint.Style.FILL
        bubblePaint.color = white
        canvas.drawCircle(dotX, dotY, r * GFORCE_DOT_FRACTION, bubblePaint)
        canvas.restoreToCount(saved)

        labelPaint.textSize = r * 0.30f
        canvas.drawText(String.format(Locale.US, "%.2fG", mag), cx, cy + r + labelPaint.textSize + 6f, labelPaint)
    }

    private fun drawAccelBar(canvas: Canvas, w: Int, h: Int, accelKmhPerSec: Float) {
        val barW = min(w, h) * 0.045f
        val barH = min(w, h) * 0.22f
        val cx = w - barW * 0.5f - 48f
        val cy = h - barH * 0.5f - barW * 2.6f
        val cornerR = barW * 0.5f
        val frame = RectF(cx - barW / 2f, cy - barH / 2f, cx + barW / 2f, cy + barH / 2f)

        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = 0.8f
        rectPaint.color = faint
        canvas.drawRoundRect(frame, cornerR, cornerR, rectPaint)
        canvas.drawLine(cx - barW * 0.55f, cy, cx + barW * 0.55f, cy, rectPaint)

        val ratio = (accelKmhPerSec / 12f).coerceIn(-1f, 1f)
        if (abs(ratio) > 0.02f) {
            rectPaint.style = Paint.Style.FILL
            rectPaint.color = white
            val fillRect = if (ratio > 0) {
                RectF(cx - barW / 2f + 1f, cy - barH / 2f * ratio, cx + barW / 2f - 1f, cy)
            } else {
                RectF(cx - barW / 2f + 1f, cy, cx + barW / 2f - 1f, cy + barH / 2f * (-ratio))
            }
            canvas.drawRoundRect(fillRect, cornerR * 0.5f, cornerR * 0.5f, rectPaint)
        }

        labelPaint.textSize = barW * 0.85f
        canvas.drawText(
            String.format(Locale.US, "%+.1f", accelKmhPerSec),
            cx, cy + barH / 2f + labelPaint.textSize + 8f, labelPaint,
        )
        labelPaint.textSize = barW * 0.60f
        canvas.drawText("ACCEL", cx, cy - barH / 2f - labelPaint.textSize * 0.5f, labelPaint)
    }

    private fun drawStats(canvas: Canvas, w: Int, h: Int, render: RenderState) {
        fun fmt(v: Float) = if (v <= 0f) "--" else v.toInt().toString()
        fun fmtKm(v: Float) = if (v <= 0f) "--" else String.format(Locale.US, "%.1f", v)
        val items = listOf(
            Triple("MAX", fmt(render.tripMaxKmh), white),
            Triple("AVG", fmt(render.tripAvgKmh), white),
            Triple("KM", fmtKm(render.tripDistanceKm), white),
        )
        if (w > h) {
            val r = min(w, h) * 0.10f
            val colX = r + 32f
            val compassBottom = h * COMPASS_ZONE_FRACTION
            val gforceCy = h - r - r * 0.8f
            val available = gforceCy - r - compassBottom - 16f
            val itemH = available / items.size
            val labelSize = (itemH * 0.26f).coerceAtMost(h * 0.040f)
            val valueSize = (itemH * 0.46f).coerceAtMost(h * 0.068f)
            items.forEachIndexed { i, (label, value, color) ->
                val cy = compassBottom + i * itemH + itemH * 0.38f
                statsLabelPaint.textSize = labelSize
                canvas.drawText(label, colX, cy, statsLabelPaint)
                statsValuePaint.textSize = valueSize
                statsValuePaint.color = color
                canvas.drawText(value, colX, cy + valueSize * 1.05f, statsValuePaint)
            }
        } else {
            val compassBottom = h * COMPASS_ZONE_FRACTION
            val gaugeBottom = h * 0.96f
            val cy = compassBottom + (gaugeBottom - compassBottom) * 0.6f
            val radius = min(w * 0.48f, (gaugeBottom - compassBottom) * 0.55f)
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
                canvas.drawText(label, itemX, midY - valueSize * 0.15f, statsLabelPaint)
                statsValuePaint.textSize = valueSize
                statsValuePaint.color = color
                canvas.drawText(value, itemX, midY + valueSize * 0.85f, statsValuePaint)
            }
        }
    }

    private fun drawStatus(canvas: Canvas, cx: Float, unitY: Float, r: Float, render: RenderState) {
        if (render.hasFix) return
        statusPaint.textSize = r * 0.085f
        val text = if (!render.locationAvailable) "LOCATION OFF" else "ACQUIRING GPS"
        canvas.drawText(text, cx, unitY + statusPaint.textSize * 1.6f, statusPaint)
    }
}
