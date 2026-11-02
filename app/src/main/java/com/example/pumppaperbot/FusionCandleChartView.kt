package com.example.pumppaperbot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Synced 48-bar OHLC chart. Keeps all candles inside the chart bounds,
 * leaves a visible right margin, and displays current/min/max in a left rail.
 */
internal class FusionCandleChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val background = Color.rgb(16, 24, 32)
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(49, 60, 70); strokeWidth = dp(.7f)
    }
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(175, 182, 193); textSize = sp(9f)
        textAlign = Paint.Align.CENTER
    }
    private val scaleText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(235, 240, 245); textSize = sp(9.5f)
        textAlign = Paint.Align.RIGHT
    }
    private val currentText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = sp(10f)
        textAlign = Paint.Align.RIGHT; isFakeBoldText = true
    }
    private val percentText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(232, 197, 88); textSize = sp(8.5f)
        textAlign = Paint.Align.LEFT
    }
    private val percentGuide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(145, 241, 194, 69)
        strokeWidth = dp(.65f)
        pathEffect = DashPathEffect(floatArrayOf(dp(3f), dp(4f)), 0f)
    }
    private val liveGuide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(242, 217, 88)
        strokeWidth = dp(.95f)
        pathEffect = DashPathEffect(floatArrayOf(dp(4f), dp(3f)), 0f)
    }
    private val up = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(23, 204, 109); strokeWidth = dp(.9f)
    }
    private val down = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 86, 102); strokeWidth = dp(.9f)
    }
    private val entryLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(90, 154, 253)
        strokeWidth = dp(.9f)
        pathEffect = DashPathEffect(floatArrayOf(dp(5f), dp(4f)), 0f)
    }
    private val periodText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(14.5f)
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
    }
    private val rect = RectF()
    private var candles: List<FusionLiveMarket.Candle> = emptyList()
    private var entry: Double? = null
    private var livePrice: Double? = null
    private var btc = false
    private var candleMillis = 30L * 60_000L
    private var windowEnd = 0L

    fun render(
        data: List<FusionLiveMarket.Candle>,
        intervalMillis: Long,
        currentTime: Long,
        isBitcoin: Boolean,
        entryPrice: Double? = null,
        currentPrice: Double? = null
    ) {
        require(intervalMillis > 0L)
        candleMillis = intervalMillis
        windowEnd = currentTime
        btc = isBitcoin
        val lower = (currentTime / intervalMillis) * intervalMillis - 47L * intervalMillis
        candles = data.asSequence()
            .filter { it.timestamp in lower..currentTime }
            .filter { it.open > 0.0 && it.high > 0.0 && it.low > 0.0 && it.close > 0.0 }
            .filter { it.low <= min(it.open, it.close) && it.high >= max(it.open, it.close) }
            .sortedBy { it.timestamp }
            .distinctBy { it.timestamp }
            .toList().takeLast(48)
        entry = entryPrice?.takeIf { it.isFinite() && it > 0.0 }
        livePrice = currentPrice?.takeIf { it.isFinite() && it > 0.0 }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(background)
        val bars = candles
        if (bars.isEmpty()) {
            canvas.drawText("Нет свежих свечей Bitpanda", width / 2f, height / 2f, timePaint)
            return
        }

        // Separate left price axis; right margin is >= five millimeters on phone screens.
        val left = dp(if (btc) 78f else 88f)
        val right = width - dp(40f)
        // Keep a header strip free for percentage change over the visible interval.
        val top = dp(43f)
        val bottom = height - dp(30f)
        if (right - left < dp(45f) || bottom - top < dp(60f)) return

        val rawMin = min(bars.minOf { it.low }, livePrice ?: Double.POSITIVE_INFINITY)
        val rawMax = max(bars.maxOf { it.high }, livePrice ?: 0.0)
        // Include all wicks and current price, with modest vertical headroom.
        val rawSpan = max(rawMax - rawMin, rawMax * 0.00045)
        val minP = (rawMin - rawSpan * .11).coerceAtLeast(0.0)
        val maxP = rawMax + rawSpan * .11
        val span = max(maxP - minP, 1e-12)
        fun y(value: Double): Float =
            (bottom - (value - minP) / span * (bottom - top)).toFloat()

        val intervalStart = (windowEnd / candleMillis) * candleMillis
        val start = intervalStart - 47L * candleMillis
        val periodEnd = intervalStart + candleMillis
        val barWidth = (right - left) / 48f
        fun x(timestamp: Long): Float =
            left + ((timestamp - start).toDouble() / candleMillis).toFloat() * barWidth + barWidth * .5f

        for (i in 0..4) {
            val yy = top + (bottom - top) * i / 4f
            canvas.drawLine(left, yy, right, yy, grid)
            val xx = left + (right - left) * i / 4f
            canvas.drawLine(xx, top, xx, bottom, grid)
            val at = start + (periodEnd - start) * i / 4L
            canvas.drawText(SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(at)),
                xx.coerceIn(left + dp(15f), right - dp(15f)), bottom + dp(17f), timePaint)
        }

        val current = livePrice ?: bars.last().close
        val firstPrice = bars.first().open
        val intervalHours = 48L * candleMillis / 3_600_000L
        val periodPercent = if (firstPrice > 0.0)
            (current / firstPrice - 1.0) * 100.0 else 0.0
        periodText.color = if (periodPercent >= 0.0) up.color else down.color
        canvas.drawText(
            String.format(Locale.GERMANY, "ЗА %d Ч: %+.2f%%", intervalHours, periodPercent),
            left + dp(2f), dp(24f), periodText
        )

        // Reference lines are based on the live/current price, and extend over
        // the ENTIRE visible price range rather than stopping at +/-4%.
        val lowPercent = floor((minP / current - 1.0) * 100.0)
            .toInt().coerceAtLeast(-99)
        val highPercent = ceil((maxP / current - 1.0) * 100.0)
            .toInt().coerceAtMost(10000)
        val pixelsPerPercent = (bottom - top) * current * .01 / span
        val minSpacing = dp(13f)
        val legibilityStep = ceil(minSpacing / pixelsPerPercent.coerceAtLeast(0.0001))
            .toInt().coerceAtLeast(1)
        val densityStep = ceil((highPercent - lowPercent).toDouble() / 120.0)
            .toInt().coerceAtLeast(1)
        val step = max(legibilityStep, densityStep)
        var percent = Math.floorDiv(lowPercent, step) * step
        var lines = 0
        while (percent <= highPercent && lines < 125) {
            if (percent != 0) {
                val level = current * (1.0 + percent / 100.0)
                if (level > minP && level < maxP) {
                    val yy = y(level)
                    if (yy >= top + dp(6f) && yy <= bottom - dp(6f)) {
                        canvas.drawLine(left, yy, right, yy, percentGuide)
                        canvas.drawText("${if (percent > 0) "+" else ""}$percent%",
                            right + dp(3f), yy + dp(3f), percentText)
                    }
                }
            }
            percent += step
            lines++
        }

        val openPrice = entry
        if (openPrice != null && openPrice in minP..maxP)
            canvas.drawLine(left, y(openPrice), right, y(openPrice), entryLine)

        val clip = canvas.save()
        canvas.clipRect(left, top, right, bottom)
        for (c in bars) {
            if (c.timestamp < start || c.timestamp >= periodEnd) continue
            val cx = x(c.timestamp)
            val brush = if (c.close >= c.open) up else down
            canvas.drawLine(cx, y(c.high), cx, y(c.low), brush)
            val upper = y(max(c.open, c.close))
            val lower = y(min(c.open, c.close))
            val half = (barWidth * .35f).coerceIn(dp(.55f), dp(5f))
            rect.set(cx - half, upper, cx + half, max(lower, upper + dp(.85f)))
            canvas.drawRect(rect, brush)
        }
        canvas.restoreToCount(clip)

        // Current-price line and left-side labels are drawn AFTER candles so they remain legible.
        val liveY = y(current).coerceIn(top, bottom)
        canvas.drawLine(left, liveY, right, liveY, liveGuide)
        val axisRight = left - dp(5f)
        canvas.drawText("MAX " + format(rawMax), axisRight, top + dp(9f), scaleText)
        canvas.drawText("MIN " + format(rawMin), axisRight, bottom - dp(2f), scaleText)
        val labelY = liveY.coerceIn(top + dp(23f), bottom - dp(20f))
        canvas.drawText("NOW", axisRight, labelY - dp(9f), currentText)
        canvas.drawText(format(current), axisRight, labelY + dp(4f), currentText)
    }

    private fun format(price: Double): String =
        if (btc) String.format(Locale.GERMANY, "%,.0f", price)
        else String.format(Locale.GERMANY, "%.6f", price)

    private fun dp(n: Float) = n * resources.displayMetrics.density
    private fun sp(n: Float) = n * resources.displayMetrics.scaledDensity
}
