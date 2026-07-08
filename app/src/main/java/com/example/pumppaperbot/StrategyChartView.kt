package com.example.pumppaperbot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class StrategyChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0D1117") }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#30363D")
        strokeWidth = 1f
    }
    private val candleUpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#32C789") }
    private val candleDownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF4D6D") }
    private val fastPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD84D")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val slowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B58CFF")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9D1D9")
        textSize = 28f
    }
    private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B949E")
        textSize = 22f
    }
    private val buyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#32C789")
        style = Paint.Style.FILL
    }
    private val sellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4D6D")
        style = Paint.Style.FILL
    }
    private val bodyRect = RectF()

    private var bundle: ChartBundle? = null
    private var title: String = ""

    fun setData(title: String, data: ChartBundle) {
        this.title = title
        this.bundle = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 16f, 16f, bgPaint)

        val data = bundle
        if (data == null || data.candles.size < 4) {
            canvas.drawText(title.ifBlank { "Waiting for chart data" }, 24f, 48f, textPaint)
            canvas.drawText("Press CHECK NOW or START", 24f, 84f, mutedPaint)
            return
        }

        val left = 24f
        val top = 70f
        val right = width - 20f
        val bottom = height - 32f
        val chartHeight = bottom - top
        val chartWidth = right - left
        val visibleCount = min(150, data.candles.size)
        val start = data.candles.size - visibleCount
        val candles = data.candles.subList(start, data.candles.size)
        val lineValues = (data.fast.drop(start) + data.slow.drop(start)).filterNotNull()
        val minPrice = min(candles.minOf { it.low }, lineValues.minOrNull() ?: candles.minOf { it.low })
        val maxPrice = max(candles.maxOf { it.high }, lineValues.maxOrNull() ?: candles.maxOf { it.high })
        val span = max(maxPrice - minPrice, 0.00000001)
        val step = chartWidth / visibleCount

        fun x(index: Int): Float = left + index * step + step / 2f
        fun y(price: Double): Float = top + ((maxPrice - price) / span).toFloat() * chartHeight

        canvas.drawText(title, 24f, 34f, textPaint)
        canvas.drawText("${data.subtitle}   Yellow fast / Purple slow", 24f, 62f, mutedPaint)

        for (i in 0..4) {
            val gy = top + chartHeight / 4f * i
            canvas.drawLine(left, gy, right, gy, gridPaint)
        }

        candles.forEachIndexed { index, candle ->
            val paint = if (candle.close >= candle.open) candleUpPaint else candleDownPaint
            val cx = x(index)
            canvas.drawLine(cx, y(candle.high), cx, y(candle.low), paint)
            val bodyTop = y(max(candle.open, candle.close))
            val bodyBottom = y(min(candle.open, candle.close))
            val halfWidth = max(2f, step * 0.32f)
            bodyRect.set(cx - halfWidth, bodyTop, cx + halfWidth, max(bodyBottom, bodyTop + 2f))
            canvas.drawRoundRect(bodyRect, 2f, 2f, paint)
        }

        drawIndicator(canvas, data.fast, start, visibleCount, ::x, ::y, fastPaint)
        drawIndicator(canvas, data.slow, start, visibleCount, ::x, ::y, slowPaint)
        drawTrades(canvas, data.trades, candles, ::x, ::y)
    }

    private fun drawIndicator(
        canvas: Canvas,
        values: List<Double?>,
        start: Int,
        visibleCount: Int,
        x: (Int) -> Float,
        y: (Double) -> Float,
        paint: Paint
    ) {
        val path = Path()
        var started = false
        for (i in 0 until visibleCount) {
            val value = values.getOrNull(start + i) ?: continue
            if (!started) {
                path.moveTo(x(i), y(value))
                started = true
            } else {
                path.lineTo(x(i), y(value))
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun drawTrades(
        canvas: Canvas,
        trades: List<TradeEvent>,
        candles: List<PumpCandle>,
        x: (Int) -> Float,
        y: (Double) -> Float
    ) {
        trades.forEach { trade ->
            val index = candles.indexOfLast { it.closeTime <= trade.time }
            if (index < 0) return@forEach
            val cx = x(index)
            val cy = y(trade.price)
            if (trade.action == "BUY") {
                canvas.drawCircle(cx, cy, 10f, buyPaint)
            } else {
                val path = Path()
                path.moveTo(cx, cy + 11f)
                path.lineTo(cx - 11f, cy - 9f)
                path.lineTo(cx + 11f, cy - 9f)
                path.close()
                canvas.drawPath(path, sellPaint)
            }
        }
    }
}
