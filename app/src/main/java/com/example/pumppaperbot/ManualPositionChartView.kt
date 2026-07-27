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

/** Compact percentage chart for the position confirmed by the user. */
class ManualPositionChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#30363D")
        strokeWidth = dp(1f)
    }
    private val entry = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#58A6FF")
        strokeWidth = dp(1.5f)
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val point = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B949E")
        textSize = dp(10f)
    }

    private var percentages: List<Double> = emptyList()
    private var active = false

    fun setPosition(candles: List<PumpCandle>, boughtAt: Long, buyPrice: Double, currentPrice: Double) {
        active = boughtAt > 0L && buyPrice > 0.0 && currentPrice > 0.0
        percentages = if (active) {
            val points = candles
                .filter { it.closeTime >= boughtAt && it.close > 0.0 }
                .map { (it.close / buyPrice - 1.0) * 100.0 }
                .takeLast(MAX_POINTS)
                .toMutableList()
            if (points.isEmpty() || abs(points.first()) > 0.000001) points.add(0, 0.0)
            val live = (currentPrice / buyPrice - 1.0) * 100.0
            if (points.isEmpty() || abs(points.last() - live) > 0.000001) points.add(live)
            points.takeLast(MAX_POINTS)
        } else emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#101820"))
        val left = dp(12f)
        val right = width - dp(12f)
        val top = dp(16f)
        val bottom = height - dp(22f)
        if (right <= left || bottom <= top) return

        if (!active || percentages.size < 2) {
            text.textAlign = Paint.Align.CENTER
            text.textSize = dp(14f)
            text.color = Color.parseColor("#79C0FF")
            canvas.drawText("График появится после «Я купил»", width / 2f, height / 2f, text)
            return
        }

        var low = min(percentages.minOrNull() ?: 0.0, 0.0)
        var high = max(percentages.maxOrNull() ?: 0.0, 0.0)
        val padding = max((high - low) * 0.15, 0.15)
        low -= padding
        high += padding
        val range = (high - low).coerceAtLeast(0.1)
        fun x(index: Int) = left + (right - left) * index / (percentages.size - 1).toFloat()
        fun y(value: Double) = bottom - ((value - low) / range).toFloat() * (bottom - top)

        repeat(4) { index ->
            val y = top + (bottom - top) * index / 3f
            canvas.drawLine(left, y, right, y, grid)
        }
        val zeroY = y(0.0)
        canvas.drawLine(left, zeroY, right, zeroY, entry)

        val positive = percentages.last() >= 0.0
        val color = Color.parseColor(if (positive) "#7EE787" else "#FF7B72")
        line.color = color
        point.color = color
        fill.color = Color.argb(32, Color.red(color), Color.green(color), Color.blue(color))
        val path = Path()
        percentages.forEachIndexed { index, value ->
            if (index == 0) path.moveTo(x(index), y(value)) else path.lineTo(x(index), y(value))
        }
        val area = Path(path).apply {
            lineTo(right, zeroY)
            lineTo(left, zeroY)
            close()
        }
        canvas.drawPath(area, fill)
        canvas.drawPath(path, line)
        canvas.drawCircle(right, y(percentages.last()), dp(6f), point)

        text.textSize = dp(10f)
        text.color = Color.parseColor("#8B949E")
        text.textAlign = Paint.Align.LEFT
        canvas.drawText("ПОКУПКА  0%", left, height - dp(6f), text)
        text.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            String.format(Locale.GERMANY, "СЕЙЧАС  %+.2f%%", percentages.last()),
            right,
            height - dp(6f),
            text
        )
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density

    private companion object {
        const val MAX_POINTS = 240
    }
}
