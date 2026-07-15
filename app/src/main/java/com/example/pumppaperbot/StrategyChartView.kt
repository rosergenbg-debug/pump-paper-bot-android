package com.example.pumppaperbot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

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
    private val scenarioPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#58A6FF")
        strokeWidth = 3f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(9f, 7f), 0f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9D1D9")
        textSize = 28f
    }
    private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B949E")
        textSize = 21f
    }
    private val gaugeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9D1D9")
        textSize = 19f
        textAlign = Paint.Align.CENTER
    }
    private val buyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#32C789")
        style = Paint.Style.FILL
    }
    private val sellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4D6D")
        style = Paint.Style.FILL
    }
    private val neutralPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6E7681")
        style = Paint.Style.FILL
    }
    private val bodyRect = RectF()

    private var bundle: ChartBundle? = null
    private var title: String = ""
    private var historyOffsetBars = 0
    private var downX = 0f
    private var downY = 0f
    private var dragStartOffset = 0
    private var draggingHorizontally = false

    init {
        isClickable = true
        contentDescription = "График PUMP/EUR. Проведите пальцем влево или вправо, чтобы посмотреть историю."
    }

    fun setData(title: String, data: ChartBundle) {
        this.title = title
        this.bundle = data
        historyOffsetBars = historyOffsetBars.coerceIn(0, max(0, data.candles.size - visibleBars(data)))
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val data = bundle ?: return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                dragStartOffset = historyOffsetBars
                draggingHorizontally = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (!draggingHorizontally && abs(dx) > 12f && abs(dx) > abs(dy)) {
                    draggingHorizontally = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (draggingHorizontally) {
                    val usableWidth = max(1f, width - 130f)
                    val step = usableWidth / visibleBars(data)
                    val movedBars = (dx / max(step, 1f)).toInt()
                    historyOffsetBars = (dragStartOffset + movedBars)
                        .coerceIn(0, max(0, data.candles.size - visibleBars(data)))
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (!draggingHorizontally) performClick()
                draggingHorizontally = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                draggingHorizontally = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 16f, 16f, bgPaint)

        val data = bundle
        if (data == null || data.candles.size < 4) {
            canvas.drawText(title.ifBlank { "Ждем данные графика" }, 24f, 48f, textPaint)
            canvas.drawText("Нажмите ПРОВЕРИТЬ или ЗАПУСТИТЬ", 24f, 84f, mutedPaint)
            return
        }

        val left = 24f
        val top = 82f
        val gaugeLeft = width - 98f
        val right = gaugeLeft - 12f
        val bottom = height - 38f
        val chartHeight = bottom - top
        val chartWidth = right - left
        val visibleCount = visibleBars(data)
        val endExclusive = (data.candles.size - historyOffsetBars).coerceAtLeast(visibleCount)
        val start = (endExclusive - visibleCount).coerceAtLeast(0)
        val candles = data.candles.subList(start, endExclusive)
        val futureBars = if (historyOffsetBars == 0) 10 else 0
        val step = chartWidth / (visibleCount + futureBars)
        val candleRight = left + visibleCount * step
        val lineValues = (data.fast.subList(start.coerceAtMost(data.fast.size), endExclusive.coerceAtMost(data.fast.size)) +
            data.slow.subList(start.coerceAtMost(data.slow.size), endExclusive.coerceAtMost(data.slow.size))).filterNotNull()
        val minPrice = min(candles.minOf { it.low }, lineValues.minOrNull() ?: candles.minOf { it.low })
        val maxPrice = max(candles.maxOf { it.high }, lineValues.maxOrNull() ?: candles.maxOf { it.high })
        val padding = max((maxPrice - minPrice) * 0.08, 0.00000001)
        val paddedMin = minPrice - padding
        val paddedMax = maxPrice + padding
        val span = max(paddedMax - paddedMin, 0.00000001)

        fun x(index: Int): Float = left + index * step + step / 2f
        fun y(price: Double): Float = top + ((paddedMax - price) / span).toFloat() * chartHeight

        canvas.drawText(title, 24f, 31f, textPaint)
        val scrollText = if (historyOffsetBars > 0) "назад: $historyOffsetBars свечей • тяните ↔" else "живой край • тяните график назад ↔"
        canvas.drawText(scrollText, 24f, 58f, mutedPaint)
        canvas.drawText(data.subtitle, 24f, 78f, mutedPaint)

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
            val halfWidth = max(1.5f, step * 0.32f)
            bodyRect.set(cx - halfWidth, bodyTop, cx + halfWidth, max(bodyBottom, bodyTop + 2f))
            canvas.drawRoundRect(bodyRect, 2f, 2f, paint)
        }

        drawIndicator(canvas, data.fast, start, visibleCount, ::x, ::y, fastPaint)
        drawIndicator(canvas, data.slow, start, visibleCount, ::x, ::y, slowPaint)
        drawTrades(canvas, data.trades, candles, ::x, ::y)
        if (historyOffsetBars == 0) drawScenario(canvas, data, start, visibleCount, step, candleRight, ::x, ::y)
        drawReadinessGauge(canvas, data, gaugeLeft, top, width - 8f, bottom)
        postInvalidateDelayed(850L)
    }

    private fun visibleBars(data: ChartBundle): Int = min(120, data.candles.size)

    private fun drawScenario(
        canvas: Canvas,
        data: ChartBundle,
        start: Int,
        visibleCount: Int,
        step: Float,
        candleRight: Float,
        x: (Int) -> Float,
        y: (Double) -> Float
    ) {
        val lastClose = data.candles.lastOrNull()?.close ?: return
        val lastFastIndex = data.fast.indexOfLast { it != null }
        val currentFast = data.fast.getOrNull(lastFastIndex) ?: return
        val previousFast = data.fast.getOrNull((lastFastIndex - 6).coerceAtLeast(0)) ?: currentFast
        val rawSlope = if (previousFast > 0.0) (currentFast / previousFast - 1.0) / 6.0 else 0.0
        val slope = rawSlope.coerceIn(-0.006, 0.006)
        val path = Path()
        path.moveTo(x(visibleCount - 1), y(lastClose))
        for (i in 1..9) {
            val projected = (lastClose * (1.0 + slope * i)).coerceIn(lastClose * 0.95, lastClose * 1.05)
            path.lineTo(candleRight + (i - 0.5f) * step, y(projected))
        }
        canvas.drawPath(path, scenarioPaint)
        canvas.drawText("сценарий", candleRight + 4f, y(lastClose) - 8f, mutedPaint)

        val pulse = (9f + 4f * ((sin(System.currentTimeMillis() / 350.0) + 1.0) / 2.0)).toFloat()
        val markerPaint = if (data.readinessScore < 0) sellPaint else buyPaint
        canvas.drawCircle(x(visibleCount - 1), y(lastClose), pulse, markerPaint)
    }

    private fun drawReadinessGauge(
        canvas: Canvas,
        data: ChartBundle,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, neutralPaint)
        val middle = (top + bottom) / 2f
        canvas.drawLine(left + 6f, middle, right - 6f, middle, gridPaint)
        canvas.drawText("+100", (left + right) / 2f, top + 20f, gaugeTextPaint)
        canvas.drawText("0", (left + right) / 2f, middle + 7f, gaugeTextPaint)
        canvas.drawText("−100", (left + right) / 2f, bottom - 7f, gaugeTextPaint)

        val usable = (bottom - top) / 2f - 26f
        if (data.readinessScore < 0) {
            val amount = abs(data.readinessScore).coerceIn(0, 100) / 100f * usable
            bodyRect.set(left + 27f, middle, right - 27f, middle + amount)
            canvas.drawRoundRect(bodyRect, 5f, 5f, sellPaint)
            canvas.drawText("В", (left + right) / 2f, bottom - 28f, gaugeTextPaint)
        } else if (data.aggressive) {
            drawBuyColumn(canvas, left + 12f, middle, usable, data.trendReadiness, "О")
            drawBuyColumn(canvas, right - 32f, middle, usable, data.shockReadiness, "А")
        } else {
            drawBuyColumn(canvas, (left + right) / 2f - 10f, middle, usable, data.trendReadiness, "О")
        }

        val score = data.readinessScore.coerceIn(-100, 100)
        val markerY = if (score >= 0) middle - usable * score / 100f else middle + usable * abs(score) / 100f
        val markerPaint = if (score < 0) sellPaint else buyPaint
        val pulse = (7f + 2f * ((sin(System.currentTimeMillis() / 350.0) + 1.0) / 2.0)).toFloat()
        canvas.drawCircle(right - 10f, markerY, pulse, markerPaint)
    }

    private fun drawBuyColumn(canvas: Canvas, x: Float, middle: Float, usable: Float, value: Int, label: String) {
        val amount = value.coerceIn(0, 100) / 100f * usable
        bodyRect.set(x, middle - amount, x + 20f, middle)
        canvas.drawRoundRect(bodyRect, 5f, 5f, buyPaint)
        canvas.drawText(label, x + 10f, middle + 27f, gaugeTextPaint)
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
