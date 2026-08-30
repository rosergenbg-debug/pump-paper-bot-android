package com.example.pumppaperbot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import java.util.Locale
import kotlin.math.max

data class BtcMiniChartData(
    val points: List<Pair<Long, Double>>,
    val currentPrice: Double?,
    val change2h: Double?,
    val change6h: Double?,
    val change24h: Double?,
    val fresh: Boolean
)

internal object BtcMiniPresentation {
    fun from(
        candles: List<PumpCandle>,
        livePrice: Double?,
        liveUpdatedAt: Long,
        now: Long = System.currentTimeMillis()
    ): BtcMiniChartData {
        val cutoff = now - 24L * 60L * 60L * 1_000L
        val closed = candles.asSequence()
            .filter { it.closeTime in cutoff..now && it.close.isFinite() && it.close > 0.0 }
            .sortedBy { it.closeTime }
            .map { it.closeTime to it.close }
            .toMutableList()
        val liveFresh = livePrice != null && livePrice.isFinite() && livePrice > 0.0 &&
            liveUpdatedAt > 0L && now - liveUpdatedAt in 0L..90_000L
        if (liveFresh) {
            if (closed.lastOrNull()?.first == now) closed[closed.lastIndex] = now to livePrice!!
            else closed += now to livePrice!!
        }
        val current = closed.lastOrNull()?.second
        fun change(hours: Int): Double? {
            val end = current ?: return null
            val target = now - hours * 60L * 60L * 1_000L
            val prior = candles.asSequence()
                .filter { it.closeTime <= target && it.close.isFinite() && it.close > 0.0 }
                .maxByOrNull { it.closeTime }
                ?.close ?: return null
            return (end / prior - 1.0) * 100.0
        }
        val newestClosedAt = candles.maxOfOrNull { it.closeTime } ?: 0L
        val fresh = liveFresh || (newestClosedAt > 0L && now - newestClosedAt <= 75L * 60L * 1_000L)
        return BtcMiniChartData(closed, current, change(2), change(6), change(24), fresh)
    }

    fun signedPercent(value: Double?): String = value?.let {
        String.format(Locale.GERMANY, "%+.2f%%", it)
    } ?: "—"
}

class BtcMiniChartView(context: Context) : View(context) {
    companion object { const val VIEW_TAG = "v690_btc_mini_chart" }

    private val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#101820") }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#30363D"); strokeWidth = dp(1f)
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0B72F"); style = Paint.Style.STROKE; strokeWidth = dp(2f)
    }
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0F6FC"); textSize = sp(11f); isFakeBoldText = true
    }
    private val metric = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9D1D9"); textSize = sp(9.5f); textAlign = Paint.Align.LEFT
    }
    private val stale = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF7B72"); textSize = sp(9f); textAlign = Paint.Align.RIGHT
    }
    private var data: BtcMiniChartData? = null

    init { tag = VIEW_TAG }

    fun setData(value: BtcMiniChartData) {
        data = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(background.color)
        val d = data
        val price = d?.currentPrice?.let { String.format(Locale.GERMANY, "$%,.0f", it) } ?: "$—"
        canvas.drawText("BITCOIN • 24 ЧАСА • $price", dp(8f), dp(17f), title)
        if (d != null) {
            drawMetric(canvas, "2ч", d.change2h, dp(39f))
            drawMetric(canvas, "6ч", d.change6h, dp(58f))
            drawMetric(canvas, "24ч", d.change24h, dp(77f))
            if (!d.fresh) {
                stale.textAlign = Paint.Align.LEFT
                canvas.drawText("НЕ СВЕЖИЕ", dp(8f), height - dp(9f), stale)
            }
        }
        val points = d?.points.orEmpty()
        if (points.size < 2) return
        val left = dp(82f)
        val right = width - dp(8f)
        val top = dp(28f)
        val bottom = height - dp(8f)
        val low = points.minOf { it.second }
        val high = points.maxOf { it.second }
        val span = max(high - low, high * 0.0005)
        canvas.drawLine(left, (top + bottom) / 2f, right, (top + bottom) / 2f, grid)
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = left + (right - left) * index / max(1, points.lastIndex)
            val y = bottom - ((point.second - low) / span).toFloat() * (bottom - top)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        line.color = when {
            (d?.change24h ?: 0.0) > 0.0 -> Color.parseColor("#2EA043")
            (d?.change24h ?: 0.0) < 0.0 -> Color.parseColor("#DA3633")
            else -> Color.parseColor("#F0B72F")
        }
        canvas.drawPath(path, line)
    }

    private fun drawMetric(canvas: Canvas, period: String, value: Double?, baseline: Float) {
        metric.color = when {
            value == null -> Color.parseColor("#8B949E")
            value > 0.0 -> Color.parseColor("#3FB950")
            value < 0.0 -> Color.parseColor("#F85149")
            else -> Color.parseColor("#C9D1D9")
        }
        canvas.drawText("$period ${BtcMiniPresentation.signedPercent(value)}", dp(8f), baseline, metric)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity
}
