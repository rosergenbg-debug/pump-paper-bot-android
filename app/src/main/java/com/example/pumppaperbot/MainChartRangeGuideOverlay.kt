package com.example.pumppaperbot

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import java.lang.reflect.Field
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * V6.8 presentation overlay for the main PUMP/EUR chart.
 * It does not change market data, strategy logic, chart history or execution authority.
 */
internal object MainChartRangeGuideOverlay {
    fun install(chart: StrategyChartView) {
        if (chart.foreground is MainRangeGuideDrawable) return
        chart.foreground = MainRangeGuideDrawable(chart)
    }
}

private class MainRangeGuideDrawable(
    private val chart: StrategyChartView
) : Drawable() {
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2F81F7")
        strokeWidth = dp(1.25f)
        style = Paint.Style.STROKE
        alpha = 225
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#58A6FF")
        textSize = sp(8.5f)
        textAlign = Paint.Align.RIGHT
        isFakeBoldText = true
    }

    override fun draw(canvas: Canvas) {
        runCatching {
            val data = fields.bundle.get(chart) as? ChartBundle ?: return@runCatching
            if (data.candles.size < 4) return@runCatching

            val visibleLimit = fields.visibleBarLimit.getInt(chart).coerceIn(24, 240)
            val visibleCount = min(visibleLimit, data.candles.size)
            val maxOffset = max(0, data.candles.size - visibleCount)
            val historyOffset = fields.historyOffsetBars.getInt(chart).coerceIn(0, maxOffset)
            val endExclusive = (data.candles.size - historyOffset).coerceAtLeast(visibleCount)
            val start = (endExclusive - visibleCount).coerceAtLeast(0)
            val visible = data.candles.subList(start, endExclusive)
            val shift = fields.verticalShiftFraction.getFloat(chart)
            val window = MainChartViewportPolicy.candleWindow(visible, shift) ?: return@runCatching
            val levels = RangeGuidePolicy.levels(data.candles.last().close) ?: return@runCatching

            val left = fields.lastPlotLeft.getFloat(chart)
            val right = fields.lastPlotRight.getFloat(chart)
            val top = fields.lastPlotTop.getFloat(chart)
            val bottom = fields.lastPlotBottom.getFloat(chart)
            if (right <= left || bottom <= top) return@runCatching

            val minPrice = window.minPrice
            val maxPrice = window.maxPrice
            val logarithmic = minPrice > 0.0 && maxPrice / minPrice > 1.35
            val span = if (logarithmic) {
                max(ln(maxPrice) - ln(minPrice), 0.00000001)
            } else {
                max(maxPrice - minPrice, 0.00000001)
            }
            fun y(price: Double): Float {
                val fraction = if (logarithmic && price > 0.0) {
                    (ln(maxPrice) - ln(price)) / span
                } else {
                    (maxPrice - price) / span
                }
                return top + fraction.toFloat() * (bottom - top)
            }

            drawGuide(canvas, left, right, top, bottom, y(levels.upper), "+1,5%")
            drawGuide(canvas, left, right, top, bottom, y(levels.lower), "−1,5%")
        }
    }

    private fun drawGuide(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        guideY: Float,
        label: String
    ) {
        if (guideY !in top..bottom) return
        canvas.drawLine(left, guideY, right, guideY, linePaint)
        val baseline = (guideY - dp(3f)).coerceIn(top + labelPaint.textSize, bottom - dp(2f))
        canvas.drawText(label, right - dp(3f), baseline, labelPaint)
    }

    override fun setAlpha(alpha: Int) {
        linePaint.alpha = alpha
        labelPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        linePaint.colorFilter = colorFilter
        labelPaint.colorFilter = colorFilter
    }

    @Suppress("DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun dp(value: Float): Float = value * chart.resources.displayMetrics.density
    private fun sp(value: Float): Float = value * chart.resources.displayMetrics.scaledDensity

    private object fields {
        val bundle = field("bundle")
        val historyOffsetBars = field("historyOffsetBars")
        val visibleBarLimit = field("visibleBarLimit")
        val verticalShiftFraction = field("verticalShiftFraction")
        val lastPlotLeft = field("lastPlotLeft")
        val lastPlotRight = field("lastPlotRight")
        val lastPlotTop = field("lastPlotTop")
        val lastPlotBottom = field("lastPlotBottom")

        private fun field(name: String): Field =
            StrategyChartView::class.java.getDeclaredField(name).apply { isAccessible = true }
    }
}
