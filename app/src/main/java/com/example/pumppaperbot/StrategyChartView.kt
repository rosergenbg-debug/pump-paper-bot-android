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
import kotlin.math.floor
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
        strokeWidth = dp(1f)
    }
    private val candleUpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#32C789") }
    private val candleDownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF4D6D") }
    private val fastPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD84D")
        strokeWidth = dp(1.5f)
        style = Paint.Style.STROKE
    }
    private val slowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B58CFF")
        strokeWidth = dp(1.5f)
        style = Paint.Style.STROKE
    }
    private val scenarioPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#58A6FF")
        strokeWidth = dp(1.25f)
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(dp(4f), dp(3f)), 0f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9D1D9")
        textSize = sp(13f)
        isFakeBoldText = true
    }
    private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B949E")
        textSize = sp(9.5f)
    }
    private val gaugeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9D1D9")
        textSize = sp(9.5f)
        textAlign = Paint.Align.CENTER
    }
    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B949E")
        textSize = sp(10f)
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val markerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(10f)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2F81F7")
        strokeWidth = dp(2f)
        style = Paint.Style.FILL_AND_STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val connectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E879F9")
        strokeWidth = dp(2f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val partialConnectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A855F7")
        strokeWidth = dp(1.5f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        pathEffect = DashPathEffect(floatArrayOf(dp(5f), dp(3f)), 0f)
    }
    private val connectionTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(10f)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val profitBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B8238636")
        style = Paint.Style.FILL
    }
    private val lossBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B8B62324")
        style = Paint.Style.FILL
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
    private val priceTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(10.5f)
        textAlign = Paint.Align.RIGHT
        isFakeBoldText = true
    }
    private val priceBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B31F6FEB")
        style = Paint.Style.FILL
    }
    private val currentPriceLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B58A6FF")
        strokeWidth = dp(1f)
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(dp(4f), dp(3f)), 0f)
    }
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#58A6FF")
        strokeWidth = dp(1.5f)
        style = Paint.Style.STROKE
    }
    private val selectionBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0222730")
        style = Paint.Style.FILL
    }
    private val selectionTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(10.5f)
        isFakeBoldText = true
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
    private var selectedCandleTime: Long? = null
    private var selectionTouchX = 0f
    private var selectionTouchY = 0f
    private var lastVisibleCandles: List<PumpCandle> = emptyList()
    private var lastPlotLeft = 0f
    private var lastPlotRight = 0f
    private var lastPlotTop = 0f
    private var lastPlotBottom = 0f
    private var lastPlotStep = 1f

    init {
        isClickable = true
        contentDescription = "График PUMP/EUR. Тяните влево или вправо для истории. Удерживайте палец на свече, чтобы временно увидеть цену, время и разницу с текущей ценой."
    }

    fun setData(title: String, data: ChartBundle) {
        this.title = title
        this.bundle = data
        if (selectedCandleTime != null && data.candles.none { it.closeTime == selectedCandleTime }) {
            selectedCandleTime = null
        }
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

    fun focusOnTimeRange(startTime: Long, endTime: Long): Int {
        val data = bundle ?: return visibleBarLimit
        if (data.candles.isEmpty()) return visibleBarLimit
        val startIndex = data.candles.indexOfFirst { it.closeTime >= startTime }
            .let { if (it < 0) data.candles.lastIndex else it }
        val endIndex = data.candles.indexOfFirst { it.closeTime >= endTime }
            .let { if (it < 0) data.candles.lastIndex else it }
        val focus = tradeFocusWindow(startIndex, endIndex, data.candles.size)
        visibleBarLimit = focus.visibleBars
        historyOffsetBars = (data.candles.size - focus.endExclusive).coerceIn(0, maxHistoryOffset())
        invalidate()
        notifyHistoryChanged()
        return visibleBarLimit
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val data = bundle ?: return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                dragStartOffset = historyOffsetBars
                draggingHorizontally = false
                selectCandleAt(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (!draggingHorizontally && abs(dx) > dp(5f) && abs(dx) > abs(dy)) {
                    draggingHorizontally = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    clearCandleSelection()
                }
                if (draggingHorizontally) {
                    val movedBars = (dx / max(lastPlotStep, 1f)).toInt()
                    historyOffsetBars = (dragStartOffset + movedBars)
                        .coerceIn(0, max(0, data.candles.size - visibleBars(data)))
                    invalidate()
                    notifyHistoryChanged()
                } else {
                    selectCandleAt(event.x, event.y)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                clearCandleSelection()
                if (!draggingHorizontally) performClick()
                draggingHorizontally = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                clearCandleSelection()
                draggingHorizontally = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun selectCandleAt(touchX: Float, touchY: Float) {
        if (lastVisibleCandles.isEmpty()) return
        if (touchX !in lastPlotLeft..lastPlotRight || touchY !in lastPlotTop..lastPlotBottom) {
            clearCandleSelection()
            return
        }
        val localIndex = floor((touchX - lastPlotLeft) / max(lastPlotStep, 1f)).toInt()
            .coerceIn(0, lastVisibleCandles.lastIndex)
        val candle = lastVisibleCandles[localIndex]
        selectedCandleTime = candle.closeTime
        selectionTouchX = touchX
        selectionTouchY = touchY
        invalidate()
    }

    private fun clearCandleSelection() {
        if (selectedCandleTime == null) return
        selectedCandleTime = null
        invalidate()
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
            canvas.drawText(title.ifBlank { "Ждем данные графика" }, dp(8f), dp(20f), textPaint)
            canvas.drawText("Нажмите ПРОВЕРИТЬ или ЗАПУСТИТЬ", dp(8f), dp(36f), mutedPaint)
            return
        }

        val left = dp(7f)
        val top = dp(44f)
        val gaugeLeft = width - dp(42f)
        val right = if (data.showReadinessGauge) gaugeLeft - dp(4f) else width - dp(6f)
        val bottom = height - dp(18f)
        val chartHeight = bottom - top
        val chartWidth = right - left
        val visibleCount = visibleBars(data)
        val endExclusive = (data.candles.size - historyOffsetBars).coerceAtLeast(visibleCount)
        val start = (endExclusive - visibleCount).coerceAtLeast(0)
        val candles = data.candles.subList(start, endExclusive)
        val futureBars = if (historyOffsetBars == 0) 10 else 0
        val step = chartWidth / (visibleCount + futureBars)
        val candleRight = left + visibleCount * step
        lastVisibleCandles = candles
        lastPlotLeft = left
        lastPlotRight = right
        lastPlotTop = top
        lastPlotBottom = bottom
        lastPlotStep = step
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

        canvas.drawText(title, dp(8f), dp(15f), textPaint)
        val scrollText = if (historyOffsetBars > 0) "назад: $historyOffsetBars свечей • тяните ↔" else "живой край • тяните график назад ↔"
        canvas.drawText(scrollText, dp(8f), dp(27f), mutedPaint)
        canvas.drawText(data.subtitle, dp(8f), dp(38f), mutedPaint)

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
            val halfWidth = max(dp(0.7f), step * 0.32f)
            bodyRect.set(cx - halfWidth, bodyTop, cx + halfWidth, max(bodyBottom, bodyTop + dp(0.8f)))
            canvas.drawRoundRect(bodyRect, dp(0.7f), dp(0.7f), paint)
        }

        drawIndicator(canvas, data.fast, start, visibleCount, ::x, ::y, fastPaint)
        drawIndicator(canvas, data.slow, start, visibleCount, ::x, ::y, slowPaint)
        drawTrades(canvas, data.trades, candles, data.aggressive, ::x, ::y)
        if (historyOffsetBars == 0) drawScenario(canvas, data, start, visibleCount, step, candleRight, ::x, ::y)
        if (data.showReadinessGauge) drawReadinessGauge(canvas, data, gaugeLeft, top, width - dp(2f), bottom)
        drawPriceScale(canvas, data, paddedMin, paddedMax, left, right, top, bottom, ::y)
        drawSelectedCandle(canvas, data, candles, left, right, top, bottom, ::x, ::y)
        drawDates(canvas, candles, ::x, bottom + dp(12f))
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

    private fun drawPriceScale(
        canvas: Canvas,
        data: ChartBundle,
        scaleMin: Double,
        scaleMax: Double,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        y: (Double) -> Float
    ) {
        val labelLeft = left + dp(2f)
        drawPriceBadgeFromLeft(canvas, "ВЕРХ ${formatPrice(scaleMax)}", labelLeft, top + dp(13f))
        drawPriceBadgeFromLeft(canvas, "НИЗ ${formatPrice(scaleMin)}", labelLeft, bottom - dp(4f))

        val latestPrice = data.candles.lastOrNull()?.close ?: return
        val actualY = y(latestPrice)
        val currentY = actualY.coerceIn(top, bottom)
        canvas.drawLine(left, currentY, right, currentY, currentPriceLinePaint)
        val direction = when {
            latestPrice > scaleMax -> "↑ "
            latestPrice < scaleMin -> "↓ "
            else -> ""
        }
        val currentText = "СЕЙЧАС $direction${formatPrice(latestPrice)}"
        val currentBaseline = when {
            latestPrice > scaleMax -> top + dp(30f)
            latestPrice < scaleMin -> bottom - dp(21f)
            else -> currentY.coerceIn(top + dp(30f), bottom - dp(21f))
        }
        drawPriceBadgeFromLeft(canvas, currentText, labelLeft, currentBaseline)
    }

    private fun drawPriceBadge(canvas: Canvas, text: String, right: Float, baseline: Float) {
        val horizontalPadding = dp(4f)
        val verticalPadding = dp(3f)
        val width = priceTextPaint.measureText(text)
        bodyRect.set(
            right - width - horizontalPadding * 2f,
            baseline - priceTextPaint.textSize - verticalPadding,
            right,
            baseline + verticalPadding
        )
        canvas.drawRoundRect(bodyRect, dp(3f), dp(3f), priceBadgePaint)
        canvas.drawText(text, right - horizontalPadding, baseline, priceTextPaint)
    }

    private fun drawPriceBadgeFromLeft(canvas: Canvas, text: String, left: Float, baseline: Float) {
        val right = left + priceTextPaint.measureText(text) + dp(8f)
        drawPriceBadge(canvas, text, right, baseline)
    }

    private fun drawSelectedCandle(
        canvas: Canvas,
        data: ChartBundle,
        candles: List<PumpCandle>,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        x: (Int) -> Float,
        y: (Double) -> Float
    ) {
        val selectedTime = selectedCandleTime ?: return
        val selectedIndex = candles.indexOfFirst { it.closeTime == selectedTime }
        if (selectedIndex < 0) return
        val candle = candles[selectedIndex]
        val latest = data.candles.lastOrNull() ?: return
        val selectedX = x(selectedIndex)
        val selectedY = y(candle.close).coerceIn(top, bottom)
        canvas.drawLine(selectedX, top, selectedX, bottom, selectionPaint)
        canvas.drawLine(left, selectedY, right, selectedY, selectionPaint)
        canvas.drawCircle(selectedX, selectedY, dp(4f), arrowPaint)

        val change = if (candle.close > 0.0) (latest.close / candle.close - 1.0) * 100.0 else 0.0
        val elapsed = (latest.closeTime - candle.closeTime).coerceAtLeast(0L)
        val tooltipWidth = min(dp(205f), right - left - dp(6f))
        val tooltipHeight = dp(54f)
        val fingerClearance = dp(24f)
        val tooltipPosition = chartTooltipPosition(
            selectionTouchX,
            selectionTouchY,
            left,
            right,
            top,
            bottom,
            tooltipWidth,
            tooltipHeight,
            fingerClearance
        )
        val tooltipLeft = tooltipPosition.left
        val tooltipTop = tooltipPosition.top
        bodyRect.set(tooltipLeft, tooltipTop, tooltipLeft + tooltipWidth, tooltipTop + tooltipHeight)
        canvas.drawRoundRect(bodyRect, dp(5f), dp(5f), selectionBadgePaint)

        val textX = tooltipLeft + dp(6f)
        val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN).format(Date(candle.closeTime))
        canvas.drawText(date, textX, tooltipTop + dp(14f), selectionTextPaint)
        canvas.drawText(
            String.format(Locale.GERMAN, "%s → сейчас %+.2f%%", formatPrice(candle.close), change),
            textX,
            tooltipTop + dp(30f),
            selectionTextPaint
        )
        canvas.drawText(
            "До последней свечи: ${formatLongDuration(elapsed)}",
            textX,
            tooltipTop + dp(46f),
            selectionTextPaint
        )
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
        canvas.drawText("сценарий", candleRight + dp(2f), y(lastClose) - dp(3f), mutedPaint)

        val pulse = dp((3f + 1.4f * ((sin(System.currentTimeMillis() / 350.0) + 1.0) / 2.0)).toFloat())
        val markerPaint = if (data.directionScore < 0) sellPaint else buyPaint
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
        canvas.drawRoundRect(left, top, right, bottom, dp(4f), dp(4f), neutralPaint)
        val middle = (top + bottom) / 2f
        canvas.drawLine(left + dp(2f), middle, right - dp(2f), middle, gridPaint)
        canvas.drawText("+100", (left + right) / 2f, top + dp(11f), gaugeTextPaint)
        canvas.drawText("0", (left + right) / 2f, middle + dp(4f), gaugeTextPaint)
        canvas.drawText("−100", (left + right) / 2f, bottom - dp(3f), gaugeTextPaint)

        val usable = (bottom - top) / 2f - dp(16f)
        val score = data.directionScore.coerceIn(-100, 100)
        val amount = abs(score) / 100f * usable
        if (score > 0) {
            bodyRect.set(left + dp(8f), middle - amount, right - dp(8f), middle)
            canvas.drawRoundRect(bodyRect, dp(2f), dp(2f), buyPaint)
        } else if (score < 0) {
            bodyRect.set(left + dp(8f), middle, right - dp(8f), middle + amount)
            canvas.drawRoundRect(bodyRect, dp(2f), dp(2f), sellPaint)
        }
        canvas.drawText("Э${data.energyScore}", (left + right) / 2f, top + dp(24f), gaugeTextPaint)
        canvas.drawText("Р${data.lateEntryRisk}", (left + right) / 2f, bottom - dp(15f), gaugeTextPaint)
        val markerY = if (score >= 0) middle - usable * score / 100f else middle + usable * abs(score) / 100f
        val markerPaint = if (score < 0) sellPaint else buyPaint
        val pulse = dp((2.5f + 0.8f * ((sin(System.currentTimeMillis() / 350.0) + 1.0) / 2.0)).toFloat())
        canvas.drawCircle(right - dp(3f), markerY, pulse, markerPaint)
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
        aggressive: Boolean,
        x: (Int) -> Float,
        y: (Double) -> Float
    ) {
        fun visibleIndex(time: Long): Int? {
            if (candles.isEmpty() || time < candles.first().openTime || time > candles.last().closeTime) return null
            val index = candles.indexOfFirst { it.closeTime >= time }
            return index.takeIf { it >= 0 }
        }

        completedTradeConnections(trades).forEachIndexed { connectionIndex, connection ->
            val entryIndex = visibleIndex(connection.entry.time) ?: return@forEachIndexed
            val exitIndex = visibleIndex(connection.exit.time) ?: return@forEachIndexed
            val entryX = x(entryIndex)
            val entryY = y(connection.entry.price)
            val exitX = x(exitIndex)
            val exitY = y(connection.exit.price)
            drawElbowConnection(canvas, entryX, entryY, exitX, exitY, connectionPaint)

            connection.partialExits.forEach { partial ->
                val partialIndex = visibleIndex(partial.time) ?: return@forEach
                drawElbowConnection(canvas, entryX, entryY, x(partialIndex), y(partial.price), partialConnectionPaint)
            }

            val duration = formatDuration(connection.durationMillis)
            val label = String.format(Locale.GERMAN, "%+.2f%% • %s", connection.profitPercent, duration)
            val halfWidth = connectionTextPaint.measureText(label) / 2f + dp(4f)
            val centerX = ((entryX + exitX) / 2f).coerceIn(halfWidth + dp(3f), width - halfWidth - dp(3f))
            val baseY = (entryY + if (connectionIndex % 2 == 0) -dp(9f) else dp(14f))
                .coerceIn(dp(58f), height - dp(27f))
            bodyRect.set(centerX - halfWidth, baseY - dp(9f), centerX + halfWidth, baseY + dp(3f))
            canvas.drawRoundRect(bodyRect, dp(3f), dp(3f), if (connection.profitEur >= 0.0) profitBadgePaint else lossBadgePaint)
            canvas.drawText(label, centerX, baseY, connectionTextPaint)
        }

        val profilePaint = if (aggressive) sellPaint else buyPaint
        trades.sortedBy { it.time }.forEach { trade ->
            val index = visibleIndex(trade.time) ?: return@forEach
            val cx = x(index)
            val cy = y(trade.price)
            if (trade.action == "BUY") {
                canvas.drawCircle(cx, cy + dp(13f), dp(3.5f), profilePaint)
                canvas.drawLine(cx, cy + dp(13f), cx, cy + dp(4f), arrowPaint)
                val arrow = Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx - dp(5f), cy + dp(6f))
                    lineTo(cx + dp(5f), cy + dp(6f))
                    close()
                }
                canvas.drawPath(arrow, arrowPaint)
                canvas.drawText("ВХОД", cx, cy + dp(23f), markerTextPaint)
            } else {
                canvas.drawCircle(cx, cy - dp(13f), dp(3.5f), profilePaint)
                canvas.drawLine(cx, cy - dp(13f), cx, cy - dp(4f), arrowPaint)
                val arrow = Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx - dp(5f), cy - dp(6f))
                    lineTo(cx + dp(5f), cy - dp(6f))
                    close()
                }
                canvas.drawPath(arrow, arrowPaint)
                val partialLabel = if (trade.reason.startsWith("40%")) "ВЫХОД 40%" else "ВЫХОД 50%"
                canvas.drawText(if (trade.action == "SELL_HALF") partialLabel else "ВЫХОД", cx, cy - dp(18f), markerTextPaint)
            }
        }
    }

    private fun drawElbowConnection(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        paint: Paint
    ) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, startY)
            lineTo(endX, endY)
        }
        canvas.drawPath(path, paint)
        canvas.drawCircle(endX, startY, dp(2f), arrowPaint)
    }

    private fun formatDuration(durationMillis: Long): String {
        val totalMinutes = durationMillis / 60_000L
        val days = totalMinutes / (24L * 60L)
        val hours = totalMinutes / 60L % 24L
        val minutes = totalMinutes % 60L
        return when {
            days > 0L -> "${days}д ${hours}ч"
            hours > 0L -> "${hours}ч ${minutes}м"
            else -> "${minutes}м"
        }
    }

    private fun formatLongDuration(durationMillis: Long): String {
        val totalMinutes = durationMillis / 60_000L
        val days = totalMinutes / (24L * 60L)
        val hours = totalMinutes / 60L % 24L
        val minutes = totalMinutes % 60L
        return when {
            days > 0L -> "${days} д ${hours} ч"
            hours > 0L -> "${hours} ч ${minutes} мин"
            else -> "${minutes} мин"
        }
    }

    private fun formatPrice(price: Double): String = String.format(Locale.GERMAN, "€%.8f", price)

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity
}
