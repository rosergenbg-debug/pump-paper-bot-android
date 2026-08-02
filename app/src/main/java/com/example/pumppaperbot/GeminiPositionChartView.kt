package com.example.pumppaperbot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** A compact chart dedicated to the currently open Gemini paper position. */
class GeminiPositionChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#30363D")
        strokeWidth = dp(1f)
    }
    private val entryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D2A8FF")
        strokeWidth = dp(1.5f)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9D1D9")
        textSize = dp(11f)
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private var values: List<Double> = emptyList()
    private var entryPrice = 0.0
    private var currentPrice = 0.0
    private var inPosition = false

    fun setPosition(
        candles: List<PumpCandle>,
        entryTime: Long,
        entry: Double,
        current: Double,
        active: Boolean
    ) {
        inPosition = active && entry > 0.0 && current > 0.0
        entryPrice = entry
        currentPrice = current
        values = if (inPosition) {
            val closes = candles
                .filter { it.closeTime >= entryTime && it.close > 0.0 }
                .map { it.close }
                .takeLast(MAX_POINTS)
                .toMutableList()
            if (closes.isEmpty() || abs(closes.first() - entry) / entry > 0.000001) {
                closes.add(0, entry)
            }
            if (closes.isEmpty() || abs(closes.last() - current) / current > 0.000001) {
                closes.add(current)
            }
            closes.takeLast(MAX_POINTS)
        } else {
            emptyList()
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#101820"))
        val left = dp(12f)
        val right = width - dp(12f)
        val top = dp(18f)
        val bottom = height - dp(24f)
        if (right <= left || bottom <= top) return

        for (i in 0..3) {
            val y = top + (bottom - top) * i / 3f
            canvas.drawLine(left, y, right, y, gridPaint)
        }

        if (!inPosition || values.size < 2) {
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = dp(14f)
            textPaint.color = Color.parseColor("#79C0FF")
            canvas.drawText("График начнётся после покупки DeepSeek", width / 2f, height / 2f, textPaint)
            return
        }

        var low = min(values.minOrNull() ?: entryPrice, entryPrice)
        var high = max(values.maxOrNull() ?: entryPrice, entryPrice)
        val rawRange = high - low
        val padding = max(rawRange * 0.15, entryPrice * 0.001)
        low -= padding
        high += padding
        val range = (high - low).coerceAtLeast(entryPrice * 0.0001)
        fun x(index: Int): Float = left + (right - left) * index / (values.size - 1).toFloat()
        fun y(value: Double): Float = bottom - ((value - low) / range).toFloat() * (bottom - top)

        val entryY = y(entryPrice)
        canvas.drawLine(left, entryY, right, entryY, entryPaint)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = dp(10f)
        textPaint.color = Color.parseColor("#D2A8FF")
        canvas.drawText("BUY  €${format(entryPrice)}", left, (entryY - dp(5f)).coerceAtLeast(top), textPaint)

        val positive = currentPrice >= entryPrice
        val color = Color.parseColor(if (positive) "#7EE787" else "#FF7B72")
        linePaint.color = color
        pointPaint.color = color
        fillPaint.color = Color.argb(30, Color.red(color), Color.green(color), Color.blue(color))

        val path = Path()
        values.forEachIndexed { index, value ->
            if (index == 0) path.moveTo(x(index), y(value)) else path.lineTo(x(index), y(value))
        }
        val area = Path(path).apply {
            lineTo(right, entryY)
            lineTo(left, entryY)
            close()
        }
        canvas.drawPath(area, fillPaint)
        canvas.drawPath(path, linePaint)
        canvas.drawCircle(left, y(values.first()), dp(5f), entryPaint)
        canvas.drawCircle(right, y(values.last()), dp(6f), pointPaint)

        textPaint.color = Color.parseColor("#8B949E")
        textPaint.textSize = dp(10f)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("ВХОД", left, height - dp(7f), textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("СЕЙЧАС", right, height - dp(7f), textPaint)
    }

    private fun format(value: Double): String = String.format(Locale.GERMANY, "%.8f", value)
    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private companion object {
        const val MAX_POINTS = 160
    }
}
