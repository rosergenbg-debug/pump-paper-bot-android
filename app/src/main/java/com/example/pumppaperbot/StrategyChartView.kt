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
import kotlin.math.ln
import kotlin.math.sin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B949E")
        textSize = 18f
        textAlign = Paint.Align.CENTER
    }
    private val markerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 16f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2F81F7")
        strokeWidth = 6f
        style = Paint.Style.FILL_AND_STROKE
        strokeCap = Paint.Cap.ROUND
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
    private var historyListener: ((Int, Long, Long) -> Unit)? = null
    private var visibleBarLimit = 120

    init {
        isClickable = true
        contentDescription = "График PUMP/EUR. Проведите пальцем влево или вправо, чтобы посмотреть историю."
    }

    fun setData(title: String, data: ChartBundle) {
        this.title = title
        this.bundle = data
        historyOffsetBars = historyOffsetBars.coerceIn(0, max(0, data.candles.size - visibleBars(data)))
        invalidate()
        post { notifyHistoryChanged() }
    }

    fun maxHistoryOffset(): Int {
        val data = bundle ?: return 0
        return max(0, data.candles.size - visibleBars(data))
    }

    fun setHistoryOffsetBars(offset: Int) {
        historyOffsetBars = offset.coerceIn(0, maxHistoryOffset())
        invalidate()
        notifyHistoryChanged()
    }

    fun setOnHistoryWindowChanged(listener: ((Int, Long, Long) -> Unit)?) {
        historyListener = listener
        notifyHistoryChanged()
    }

    fun setVisibleBarLimit(limit: Int) {
        visibleBarLimit = limit.coerceIn(24, 240)
        historyOffsetBars = historyOffsetBars.coerceIn(0, maxHistoryOffset())
        invalidate()
        notifyHistoryChanged()
    }

    fun currentVisibleBarLimit(): Int = visibleBarLimit

    fun centerOnTime(time: Long) {
        val data = bundle ?: return
        if (data.candles.isEmpty()) return
        val index = data.candles.indexOfFirst { it.closeTime >= time }.let { if (it < 0) data.candles.lastIndex else it }
        val visible = visibleBars(data)
        val desiredEnd = (index + visible / 2).coerceIn(visible, data.candles.size)
        setHistoryOffsetBars(data.candles.size - desiredEnd)
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
                    notifyHistoryChanged()
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
        val right = if (data.showReadinessGauge) gaugeLeft - 12f else width - 20f
        val bottom = height - 62f
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
        val logarithmic = paddedMin > 0.0 && paddedMax / paddedMin > 1.35
        val span = if (logarithmic) {
            max(ln(paddedMax) - ln(paddedMin), 0.00000001)
        } else {
            max(paddedMax - paddedMin, 0.00000001)
        }

        fun x(index: Int): Float = left + index * step + step / 2f
        fun y(price: Double): Float {
            val fraction = if (logarithmic && price > 0.0) {
                (ln(paddedMax) - ln(price)) / span
            } else {
                (paddedMax - price) / span
            }
            return top + fraction.toFloat() * chartHeight
        }

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
        drawDates(canvas, candles, ::x, bottom + 25f)
        if (historyOffsetBars == 0) drawScenario(canvas, data, start, visibleCount, step, candleRight, ::x, ::y)
        if (data.showReadinessGauge) drawReadinessGauge(canvas, data, gaugeLeft, top, width - 8f, bottom)
        postInvalidateDelayed(850L)
    }

    private fun visibleBars(data: ChartBundle): Int = min(visibleBarLimit, data.candles.size)

    private fun notifyHistoryChanged() {
        val data = bundle ?: return
        if (data.candles.isEmpty()) return
        val visible = visibleBars(data)
        val endExclusive = (data.candles.size - historyOffsetBars).coerceIn(visible, data.candles.size)
        val start = (endExclusive - visible).coerceAtLeast(0)
        historyListener?.invoke(
            historyOffsetBars,
            data.candles[start].openTime,
            data.candles[endExclusive - 1].closeTime
        )
    }

    private fun drawDates(canvas: Canvas, candles: List<PumpCandle>, x: (Int) -> Float, y: Float) {
        if (candles.isEmpty()) return
        val formatter = SimpleDateFormat("dd.MM", Locale.GERMAN)
        val indexes = listOf(0, candles.lastIndex / 3, candles.lastIndex * 2 / 3, candles.lastIndex).distinct()
        indexes.forEach { index ->
            canvas.drawText(formatter.format(Date(candles[index].closeTime)), x(index), y, datePaint)
        }
    }

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
        var activeMode = StrategyV2.MODE_TREND
        trades.sortedBy { it.time }.forEach { trade ->
            if (trade.action == "BUY") {
                activeMode = when {
                    trade.reason.contains("Чувствитель", ignoreCase = true) -> StrategyV2.MODE_EXHAUSTION
                    trade.reason.contains("шок", ignoreCase = true) || trade.reason.contains("импульс", ignoreCase = true) -> StrategyV2.MODE_SHOCK
                    else -> StrategyV2.MODE_TREND
                }
            }
            val index = candles.indexOfLast { it.closeTime <= trade.time }
            if (index < 0) return@forEach
            val cx = x(index)
            val cy = y(trade.price)
            val modePaint = if (activeMode == StrategyV2.MODE_EXHAUSTION || activeMode == StrategyV2.MODE_SHOCK) sellPaint else buyPaint
            if (trade.action == "BUY") {
                canvas.drawCircle(cx, cy + 34f, 9f, modePaint)
                canvas.drawLine(cx, cy + 34f, cx, cy + 9f, arrowPaint)
                val arrow = Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx - 12f, cy + 15f)
                    lineTo(cx + 12f, cy + 15f)
                    close()
                }
                canvas.drawPath(arrow, arrowPaint)
                canvas.drawText("ВХОД", cx, cy + 58f, markerTextPaint)
            } else {
                canvas.drawCircle(cx, cy - 34f, 9f, modePaint)
                canvas.drawLine(cx, cy - 34f, cx, cy - 9f, arrowPaint)
                val arrow = Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx - 12f, cy - 15f)
                    lineTo(cx + 12f, cy - 15f)
                    close()
                }
                canvas.drawPath(arrow, arrowPaint)
                val partialLabel = if (trade.reason.startsWith("40%")) "ВЫХОД 40%" else "ВЫХОД 50%"
                canvas.drawText(if (trade.action == "SELL_HALF") partialLabel else "ВЫХОД", cx, cy - 48f, markerTextPaint)
                if (trade.action != "SELL_HALF") activeMode = StrategyV2.MODE_TREND
            }
        }
    }
}
