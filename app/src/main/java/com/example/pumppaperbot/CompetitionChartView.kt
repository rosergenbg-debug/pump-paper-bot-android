package com.example.pumppaperbot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class CompetitionMarker(
    val time: Long,
    val action: String,
    val price: Double,
    val pnlEur: Double = 0.0
)

class CompetitionChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#101820") }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#30363D")
        strokeWidth = dp(1f)
    }
    private val priceLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#79C0FF")
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }
    private val buy = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#32C789") }
    private val sell = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF4D6D") }
    private val tradeWin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#32C789")
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }
    private val tradeLoss = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4D6D")
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }
    private val resultText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(8.5f)
        isFakeBoldText = true
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(11f)
        isFakeBoldText = true
    }
    private val subText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9D1D9")
        textSize = sp(10f)
    }

    private var title = ""
    private var subtitle = ""
    private var candles: List<PumpCandle> = emptyList()
    private var markers: List<CompetitionMarker> = emptyList()
    private var offset = 0
    private var visibleBars = 96
    private var downX = 0f
    private var dragStartOffset = 0
    private var step = 1f
    private var offsetListener: ((Int) -> Unit)? = null

    fun setData(
        title: String,
        subtitle: String,
        candles: List<PumpCandle>,
        markers: List<CompetitionMarker>
    ) {
        this.title = title
        this.subtitle = subtitle
        this.candles = candles
        this.markers = markers
        offset = offset.coerceIn(0, maxOffset())
        invalidate()
    }

    fun setSynchronizedOffset(value: Int) {
        val next = value.coerceIn(0, maxOffset())
        if (next == offset) return
        offset = next
        invalidate()
    }

    fun setOnOffsetChanged(listener: ((Int) -> Unit)?) {
        offsetListener = listener
    }

    private fun maxOffset() = max(0, candles.size - min(visibleBars, candles.size))

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                dragStartOffset = offset
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                if (abs(dx) >= dp(3f)) {
                    val moved = (dx / max(step, 1f)).toInt()
                    val next = (dragStartOffset + moved).coerceIn(0, maxOffset())
                    if (next != offset) {
                        offset = next
                        invalidate()
                        offsetListener?.invoke(offset)
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(background.color)
        canvas.drawText(title, dp(10f), dp(18f), text)
        canvas.drawText(subtitle, dp(10f), dp(34f), subText)
        if (candles.size < 2) {
            canvas.drawText("Ждём данные рынка", dp(10f), height / 2f, subText)
            return
        }

        val count = min(visibleBars, candles.size)
        val end = (candles.size - offset).coerceAtLeast(count)
        val start = (end - count).coerceAtLeast(0)
        val visible = candles.subList(start, end)
        val left = dp(8f)
        val right = width - dp(8f)
        val top = dp(44f)
        val bottom = height - dp(12f)
        step = (right - left) / max(1, visible.size - 1)

        var low = visible.minOf { it.low }
        var high = visible.maxOf { it.high }
        val padding = max((high - low) * 0.08, high * 0.001)
        low -= padding
        high += padding
        val range = (high - low).coerceAtLeast(0.000000001)
        fun x(index: Int) = left + index * step
        fun y(price: Double) = bottom - ((price - low) / range).toFloat() * (bottom - top)

        repeat(3) { index ->
            val y = top + (bottom - top) * index / 2f
            canvas.drawLine(left, y, right, y, grid)
        }
        val path = Path()
        visible.forEachIndexed { index, candle ->
            if (index == 0) path.moveTo(x(index), y(candle.close))
            else path.lineTo(x(index), y(candle.close))
        }
        canvas.drawPath(path, priceLine)

        val firstTime = visible.first().openTime
        val lastTime = visible.last().closeTime
        val visibleMarkers = markers
            .filter { it.time in firstTime..lastTime && it.price > 0.0 }
            .sortedBy { it.time }

        fun markerPoint(marker: CompetitionMarker): Pair<Float, Float> {
            val index = visible.indices.minByOrNull {
                abs(visible[it].closeTime - marker.time)
            } ?: 0
            return x(index) to y(marker.price).coerceIn(top, bottom)
        }

        val completedTrades =
            ArrayList<Triple<CompetitionMarker, CompetitionMarker, Double>>()
        var openBuy: CompetitionMarker? = null
        var realizedEur = 0.0
        visibleMarkers.forEach { marker ->
            when {
                marker.action.startsWith("BUY") -> {
                    openBuy = marker
                    realizedEur = 0.0
                }
                marker.action == "SELL_HALF" && openBuy != null -> {
                    realizedEur += marker.pnlEur
                }
                marker.action == "SELL" && openBuy != null -> {
                    realizedEur += marker.pnlEur
                    completedTrades += Triple(openBuy!!, marker, realizedEur)
                    openBuy = null
                    realizedEur = 0.0
                }
            }
        }

        completedTrades.forEach { (entry, exit, pnlEur) ->
            val (entryX, entryY) = markerPoint(entry)
            val (exitX, exitY) = markerPoint(exit)
            val netPercent =
                ((exit.price / entry.price) *
                    (1.0 - 0.0015) *
                    (1.0 - 0.0015) - 1.0) * 100.0
            val positive = netPercent >= 0.0
            val connector = if (positive) tradeWin else tradeLoss
            val tradePath = Path().apply {
                moveTo(entryX, entryY)
                lineTo(exitX, entryY)
                lineTo(exitX, exitY)
            }
            canvas.drawPath(tradePath, connector)

            val result = if (abs(pnlEur) >= 0.005) {
                String.format(
                    java.util.Locale.GERMANY,
                    "%+.2f%%  %+.2f €",
                    netPercent,
                    pnlEur
                )
            } else {
                String.format(java.util.Locale.GERMANY, "%+.2f%%", netPercent)
            }
            resultText.color = if (positive) tradeWin.color else tradeLoss.color
            resultText.textAlign =
                if (exitX > width * 0.68f) Paint.Align.RIGHT else Paint.Align.LEFT
            val labelX =
                if (resultText.textAlign == Paint.Align.RIGHT) exitX - dp(4f) else exitX + dp(4f)
            val labelY = if (positive) {
                (entryY - dp(5f)).coerceAtLeast(top + dp(9f))
            } else {
                (entryY + dp(13f)).coerceAtMost(bottom)
            }
            canvas.drawText(result, labelX, labelY, resultText)
        }

        visibleMarkers.forEach { marker ->
            val (px, py) = markerPoint(marker)
            val isBuy = marker.action.startsWith("BUY")
            val paint = if (isBuy) buy else sell
            // BUY points upward from below; SELL points downward from above.
            val baseDirection = if (isBuy) 1f else -1f
            val markerPath = Path().apply {
                moveTo(px, py)
                lineTo(px - dp(7f), py + baseDirection * dp(12f))
                lineTo(px + dp(7f), py + baseDirection * dp(12f))
                close()
            }
            canvas.drawPath(markerPath, paint)
            resultText.color = paint.color
            resultText.textAlign = Paint.Align.CENTER
            val label = when {
                marker.action == "SELL_HALF" -> "½ ВЫХОД"
                isBuy -> "ВХОД"
                else -> "ВЫХОД"
            }
            val labelY = if (isBuy) {
                (py + dp(23f)).coerceAtMost(bottom)
            } else {
                (py - dp(15f)).coerceAtLeast(top + dp(8f))
            }
            canvas.drawText(label, px, labelY, resultText)
        }
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
    private fun sp(value: Float) = value * resources.displayMetrics.scaledDensity
}
