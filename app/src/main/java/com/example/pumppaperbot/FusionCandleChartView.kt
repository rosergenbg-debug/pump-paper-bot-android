package com.example.pumppaperbot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Bitpanda OHLC candlesticks, with a full day visible at 30m (48 bars).
 * A shared interval/time window is supplied by FusionDashboardActivity for BTC and PUMP.
 * No labels or order-flow gauges overlap the candles.
 */
internal class FusionCandleChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(52, 62, 72); strokeWidth = dp(.75f)
    }
    private val axis = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(165, 176, 189); textSize = sp(10f); textAlign = Paint.Align.RIGHT
    }
    private val time = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(165, 176, 189); textSize = sp(10f); textAlign = Paint.Align.CENTER
    }
    private val up = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(21, 211, 116) }
    private val down = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 85, 104) }
    private val entryLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(98, 159, 247); strokeWidth = dp(1.0f)
    }
    private val rect = RectF()

    private var candles: List<FusionLiveMarket.Candle> = emptyList()
    private var entry: Double? = null
    private var btc = false
    private var candleMillis = 30L * 60_000L
    private var windowEnd = 0L

    fun render(
        data: List<FusionLiveMarket.Candle>,
        intervalMillis: Long,
        currentTime: Long,
        isBitcoin: Boolean,
        entryPrice: Double? = null
    ) {
        require(intervalMillis > 0L)
        val cutoff = currentTime - 24L * 60L * 60_000L
        candles = data.asSequence()
            .filter { it.timestamp >= cutoff && it.timestamp <= currentTime }
            .filter { it.open > 0.0 && it.high > 0.0 && it.low > 0.0 && it.close > 0.0 }
            .filter { it.low <= min(it.open, it.close) && it.high >= max(it.open, it.close) }
            .sortedBy { it.timestamp }
            .distinctBy { it.timestamp }
            .toList()
            .takeLast((24L * 60L * 60_000L / intervalMillis).toInt() + 1)
        this.candleMillis = intervalMillis
        this.windowEnd = currentTime
        btc = isBitcoin
        entry = entryPrice?.takeIf { it.isFinite() && it > 0.0 }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(16, 24, 32))
        val points = candles
        if (points.isEmpty()) {
            canvas.drawText("Нет свежих свечей Bitpanda", width / 2f, height / 2f, time)
            return
        }
        val left = dp(if (btc) 65f else 75f)
        val right = width - dp(9f)
        val top = dp(14f)
        val bottom = height - dp(30f)
        if (right <= left || bottom <= top) return

        val rawLow = points.minOf { it.low }
        val rawHigh = points.maxOf { it.high }
        val range = max(rawHigh - rawLow, rawHigh * .0005)
        val minP = max(0.0, rawLow - range * .07)
        val maxP = rawHigh + range * .07
        val span = max(maxP - minP, 1e-12)
        fun yy(price: Double): Float =
            (bottom - (price - minP) / span * (bottom - top)).toFloat()

        // Fixed 24-hour clock-aligned viewport: five minutes = up to 288 candles;
        // fifteen = 96; thirty = 48; one hour = 24.
        val intervalStart = (windowEnd / candleMillis) * candleMillis
        val start = intervalStart - (24L * 60L * 60_000L) + candleMillis
        val end = intervalStart + candleMillis
        val widthPerBar = (right - left) / (24L * 60L * 60_000L / candleMillis).toFloat()
        fun xx(timestamp: Long): Float =
            left + ((timestamp - start).toDouble() / candleMillis).toFloat() * widthPerBar + widthPerBar / 2f

        for (i in 0..4) {
            val y = top + (bottom - top) * i / 4f
            canvas.drawLine(left, y, right, y, grid)
            canvas.drawText(format(maxP - span * i / 4.0), left - dp(5f), y + dp(3f), axis)

            val x = left + (right - left) * i / 4f
            canvas.drawLine(x, top, x, bottom, grid)
            val at = start + (end - start) * i / 4L
            canvas.drawText(SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(at)), x.coerceIn(left + dp(15f), right - dp(15f)), bottom + dp(17f), time)
        }

        val entryValue = entry
        if (entryValue != null && entryValue in minP..maxP) {
            canvas.drawLine(left, yy(entryValue), right, yy(entryValue), entryLine)
        }

        val saved = canvas.save()
        canvas.clipRect(left, top, right, bottom)
        points.forEach { c ->
            if (c.timestamp < start || c.timestamp >= end) return@forEach
            val x = xx(c.timestamp)
            val color = if (c.close >= c.open) up else down
            val high = yy(c.high)
            val low = yy(c.low)
            canvas.drawLine(x, high, x, low, color)
            val y1 = yy(max(c.open, c.close))
            val y2 = yy(min(c.open, c.close))
            val half = (widthPerBar * .35f).coerceIn(dp(.7f), dp(6f))
            rect.set(x - half, y1, x + half, max(y2, y1 + dp(.8f)))
            canvas.drawRect(rect, color)
        }
        canvas.restoreToCount(saved)
    }

    private fun format(price: Double): String =
        if (btc) String.format(Locale.GERMANY, "%,.0f", price)
        else String.format(Locale.GERMANY, "%.6f", price)

    private fun dp(n: Float) = n * resources.displayMetrics.density
    private fun sp(n: Float) = n * resources.displayMetrics.scaledDensity
}
