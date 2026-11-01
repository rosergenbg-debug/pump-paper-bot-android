package com.example.pumppaperbot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Full-width time chart. Price labels occupy a separate left axis,
 * with no overlays over the newest candle on the right.
 */
internal class FusionPriceChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#34404A"); strokeWidth = dp(0.8f)
    }
    private val axis = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9EAAB8")
        textSize = sp(10f)
        textAlign = Paint.Align.RIGHT
    }
    private val time = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9EAAB8")
        textSize = sp(10f)
        textAlign = Paint.Align.CENTER
    }
    private val series = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#11CE77")
        strokeWidth = dp(2.1f)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }
    private val active = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#11CE77")
        style = Paint.Style.FILL
    }
    private val buyLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#78A8FF")
        strokeWidth = dp(1.3f)
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(dp(5f), dp(5f)), 0f)
    }

    private var candles: List<FusionLiveMarket.Candle> = emptyList()
    private var currentPrice: Double? = null
    private var entryPrice: Double? = null
    private var lastUpdated: Long = 0L
    private var euroBtc = false

    fun render(
        data: List<FusionLiveMarket.Candle>,
        latest: Double,
        updatedAt: Long,
        btc: Boolean,
        entry: Double? = null
    ) {
        candles = data
        currentPrice = latest.takeIf { it.isFinite() && it > 0.0 }
        entryPrice = entry?.takeIf { it.isFinite() && it > 0.0 }
        lastUpdated = updatedAt
        euroBtc = btc
        series.color = Color.parseColor(if (btc) "#FFAA37" else "#11CE77")
        active.color = series.color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#101820"))
        val data = candles
        if (data.size < 2) {
            canvas.drawText("Нет свечей Bitpanda", width / 2f, height / 2f, time)
            return
        }
        val left = dp(if (euroBtc) 63f else 68f)
        val right = width - dp(13f)
        val top = dp(12f)
        val bottom = height - dp(28f)
        if (right <= left || bottom <= top) return

        val values = data.map { it.close }
        val lowRaw = minOf(data.minOf { it.low }, currentPrice ?: Double.MAX_VALUE)
        val highRaw = maxOf(data.maxOf { it.high }, currentPrice ?: 0.0)
        val spanRaw = max(highRaw - lowRaw, highRaw * 0.0008)
        val low = (lowRaw - spanRaw * 0.06).coerceAtLeast(0.0)
        val high = highRaw + spanRaw * 0.06
        val span = max(high - low, 1e-12)

        fun y(value: Double): Float =
            (bottom - (value - low) / span * (bottom - top)).toFloat()
        val first = data.first().timestamp
        val end = max(data.last().timestamp, lastUpdated).coerceAtLeast(first + 1L)
        fun x(t: Long): Float =
            (left + (t - first).toDouble() / (end - first) * (right - left)).toFloat()

        for (i in 0..4) {
            val price = high - span * i / 4.0
            val yy = y(price)
            canvas.drawLine(left, yy, right, yy, grid)
            canvas.drawText(format(price), left - dp(6f), yy + dp(3f), axis)

            val xx = left + (right - left) * i / 4f
            canvas.drawLine(xx, top, xx, bottom, grid)
            val stamp = first + (end - first) * i / 4L
            val label = SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(stamp))
            canvas.drawText(label, xx, bottom + dp(17f), time)
        }
        val buy = entryPrice
        if (buy != null && buy in low..high) {
            canvas.drawLine(left, y(buy), right, y(buy), buyLine)
        }

        val path = Path()
        data.forEachIndexed { index, c ->
            if (index == 0) path.moveTo(x(c.timestamp), y(c.close))
            else path.lineTo(x(c.timestamp), y(c.close))
        }
        val live = currentPrice
        if (live != null && lastUpdated >= data.last().timestamp) {
            path.lineTo(x(lastUpdated), y(live))
        }
        val saved = canvas.save()
        canvas.clipRect(left, top, right, bottom)
        canvas.drawPath(path, series)
        if (live != null) canvas.drawCircle(x(max(lastUpdated, data.last().timestamp)), y(live), dp(3.5f), active)
        canvas.restoreToCount(saved)
    }

    private fun format(v: Double) =
        if (euroBtc) String.format(Locale.GERMANY, "%,.0f", v)
        else String.format(Locale.GERMANY, "%.6f", v)

    private fun dp(n: Float) = n * resources.displayMetrics.density
    private fun sp(n: Float) = n * resources.displayMetrics.scaledDensity
}
