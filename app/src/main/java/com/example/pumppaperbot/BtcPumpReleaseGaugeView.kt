package com.example.pumppaperbot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import java.io.File
import java.io.RandomAccessFile
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Presentation/research-only context. It never participates in entry, exit, alerts or account state.
 * Score 0..100 means causal pattern similarity, NOT probability of a future move.
 */
data class BtcPumpReleaseGaugeData(
    val patternScore: Int = 0,
    val btcImpulseScore: Int = 0,
    val pumpHoldScore: Int = 0,
    val btcStableScore: Int = 0,
    val releaseScore: Int = 0,
    val phase: String = "НАКАПЛИВАЕМ ДАННЫЕ",
    val detail: String = "0–100 = совпадение с паттерном, не вероятность",
    val fresh: Boolean = false,
    val btcPoints: List<Pair<Long, Double>> = emptyList(),
    val btcPrice: Double? = null,
    val btc15m: Double? = null,
    val btc30m: Double? = null,
    val btc60m: Double? = null
)

/** Reads only the tail of the existing 24h breathing CSV, so the UI never re-downloads market data. */
internal object BtcPumpReleaseLiveSource {
    private const val FILE_NAME = "pump_live_breathing_v415.csv"
    private const val MAX_TAIL_BYTES = 2_500_000L

    fun recentMinuteSamples(
        context: Context,
        now: Long = System.currentTimeMillis(),
        minutes: Int = 90
    ): List<LiveBreathingSample> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists() || file.length() <= 0L) return emptyList()
        val cutoff = now - minutes.coerceAtLeast(45) * 60_000L
        val byMinute = LinkedHashMap<Long, LiveBreathingSample>()
        runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val start = (raf.length() - MAX_TAIL_BYTES).coerceAtLeast(0L)
                raf.seek(start)
                if (start > 0L) raf.readLine() // discard a possible partial first row
                while (true) {
                    val line = raf.readLine() ?: break
                    val sample = parse(line) ?: continue
                    if (sample.at < cutoff || sample.at > now) continue
                    byMinute[sample.at / 60_000L] = sample
                }
            }
        }
        return byMinute.values.sortedBy { it.at }
    }

    private fun parse(line: String): LiveBreathingSample? {
        if (line.startsWith("observed_at_ms")) return null
        val v = line.split(',')
        if (v.size < 7) return null
        return LiveBreathingSample(
            at = v[0].toLongOrNull() ?: return null,
            priceUsdt = v[1].toDoubleOrNull() ?: return null,
            pumpBuyerPercent = v[2].toDoubleOrNull() ?: return null,
            pumpChange60sPercent = v[3].toDoubleOrNull() ?: return null,
            bookImbalance = v[4].toDoubleOrNull(),
            bitcoinBuyerPercent = v[5].toDoubleOrNull() ?: return null,
            bitcoinChange60sPercent = v[6].toDoubleOrNull() ?: return null,
            pumpBuyNotional60s = v.getOrNull(7)?.toDoubleOrNull() ?: 0.0,
            pumpSellNotional60s = v.getOrNull(8)?.toDoubleOrNull() ?: 0.0,
            pumpTrades60s = v.getOrNull(9)?.toIntOrNull() ?: 0,
            tradeAcceleration = v.getOrNull(10)?.toDoubleOrNull() ?: 0.0,
            bitcoinPriceUsdt = v.getOrNull(11)?.toDoubleOrNull() ?: 0.0
        )
    }
}

internal object BtcPumpReleasePolicy {
    private const val FRESH_MILLIS = 90_000L

    fun evaluate(
        samples: List<LiveBreathingSample>,
        now: Long = System.currentTimeMillis()
    ): BtcPumpReleaseGaugeData {
        val valid = samples.asSequence()
            .filter { it.at <= now && it.priceUsdt > 0.0 && it.bitcoinPriceUsdt > 0.0 }
            .sortedBy { it.at }
            .toList()
        val last = valid.lastOrNull() ?: return BtcPumpReleaseGaugeData()
        val fresh = now - last.at in 0L..FRESH_MILLIS

        fun atOrBefore(target: Long): LiveBreathingSample? = valid.lastOrNull { it.at <= target }
        fun change(from: Double, to: Double): Double? =
            if (from > 0.0 && to > 0.0) (to / from - 1.0) * 100.0 else null
        fun btcChange(minutes: Int): Double? {
            val prior = atOrBefore(last.at - minutes * 60_000L) ?: return null
            return change(prior.bitcoinPriceUsdt, last.bitcoinPriceUsdt)
        }
        val baseChart = BtcPumpReleaseGaugeData(
            fresh = fresh,
            btcPoints = valid.takeLast(91).map { it.at to it.bitcoinPriceUsdt },
            btcPrice = last.bitcoinPriceUsdt,
            btc15m = btcChange(15),
            btc30m = btcChange(30),
            btc60m = btcChange(60)
        )
        if (valid.size < 35) {
            return baseChart.copy(
                phase = "НАКАПЛИВАЕМ 45 МИНУТ КОНТЕКСТА",
                detail = "Есть ${valid.size} мин. • нужно около 45 мин. • не BUY-сигнал"
            )
        }

        val t45 = atOrBefore(last.at - 45L * 60_000L) ?: valid.first()
        val t15 = atOrBefore(last.at - 15L * 60_000L) ?: return baseChart
        val recent15 = valid.filter { it.at >= last.at - 15L * 60_000L }
        val prior5 = valid.filter { it.at in (last.at - 10L * 60_000L)..(last.at - 5L * 60_000L) }
        val last5 = valid.filter { it.at >= last.at - 5L * 60_000L }

        fun requiredChange(from: Double, to: Double): Double = change(from, to) ?: 0.0
        fun scoreUp(value: Double, zeroAt: Double, fullAt: Double): Int {
            if (fullAt <= zeroAt) return 0
            return (((value - zeroAt) / (fullAt - zeroAt)) * 100.0)
                .roundToInt().coerceIn(0, 100)
        }
        fun scoreDown(value: Double, fullAt: Double, zeroAt: Double): Int {
            if (zeroAt <= fullAt) return 0
            return (((zeroAt - value) / (zeroAt - fullAt)) * 100.0)
                .roundToInt().coerceIn(0, 100)
        }

        // Mirrors the causal 60d research shape: prior 30m impulse ending 15m ago, then 15m settle/release.
        val btcImpulse = requiredChange(t45.bitcoinPriceUsdt, t15.bitcoinPriceUsdt)
        val pumpImpulse = requiredChange(t45.priceUsdt, t15.priceUsdt)
        val lag = pumpImpulse - btcImpulse
        val btcRecent = requiredChange(t15.bitcoinPriceUsdt, last.bitcoinPriceUsdt)
        val pumpRecent = requiredChange(t15.priceUsdt, last.priceUsdt)
        val btcRange15 = recent15.takeIf { it.size >= 2 }?.let { window ->
            val lo = window.minOf { it.bitcoinPriceUsdt }
            val hi = window.maxOf { it.bitcoinPriceUsdt }
            if (lo > 0.0) (hi / lo - 1.0) * 100.0 else 0.0
        } ?: 9.9

        val btcImpulseScore = scoreUp(btcImpulse, 0.10, 0.55)
        val flatness = scoreDown(abs(pumpImpulse), 0.20, 0.95)
        val lagScore = scoreDown(lag, -0.50, 0.05)
        val deepDropPenalty = if (pumpImpulse < -0.80) scoreUp(-pumpImpulse, 0.80, 1.80) else 0
        val pumpHoldScore = (
            flatness * 0.45 + lagScore * 0.55 - deepDropPenalty * 0.45
        ).roundToInt().coerceIn(0, 100)

        val stableClose = scoreDown(abs(btcRecent), 0.08, 0.50)
        val stableRange = scoreDown(btcRange15, 0.22, 0.85)
        val btcStableScore = (stableClose * 0.55 + stableRange * 0.45)
            .roundToInt().coerceIn(0, 100)

        val priceTurn = scoreUp(pumpRecent, -0.20, 0.35)
        val recentBuyer = last5.map { it.pumpBuyerPercent }.averageOrNull() ?: last.pumpBuyerPercent
        val previousBuyer = prior5.map { it.pumpBuyerPercent }.averageOrNull() ?: recentBuyer
        val buyerLevel = scoreUp(recentBuyer, 46.0, 56.0)
        val buyerTurn = scoreUp(recentBuyer - previousBuyer, -1.0, 6.0)
        val releaseScore = (priceTurn * 0.45 + buyerLevel * 0.25 + buyerTurn * 0.30)
            .roundToInt().coerceIn(0, 100)

        var pattern = (
            btcImpulseScore * 0.30 + pumpHoldScore * 0.27 +
                btcStableScore * 0.25 + releaseScore * 0.18
        ).roundToInt().coerceIn(0, 100)
        // Sequential caps stop a high late component from pretending earlier stages happened.
        if (btcImpulseScore < 35) pattern = pattern.coerceAtMost(44)
        if (pumpHoldScore < 35) pattern = pattern.coerceAtMost(54)
        if (btcStableScore < 35) pattern = pattern.coerceAtMost(69)

        val phase = when {
            !fresh -> "ДАННЫЕ УСТАРЕЛИ"
            btcImpulseScore < 35 -> "НЕТ BTC-РАЗГОНА"
            pumpHoldScore < 40 -> "BTC РАСТЁТ • PUMP НЕ СЖАТ"
            btcStableScore < 45 -> "PUMP УДЕРЖАН • BTC ЕЩЁ ДВИЖЕТСЯ"
            releaseScore < 45 -> "BTC УСПОКОИЛСЯ • ЖДЁМ ОТПУСКАНИЕ"
            pattern >= 72 -> "ОТПУСКАНИЕ ВОЗМОЖНО • НАБЛЮДАТЬ"
            else -> "ПАТТЕРН СОБИРАЕТСЯ"
        }
        val detail = String.format(
            Locale.GERMANY,
            "BTC30 %+.2f%% • PUMP30 %+.2f%% • лаг %+.2f п.п. • BTC15 %+.2f%% • PUMP15 %+.2f%% • BUY %.1f%%\n60д: фон +2%%/60м ≈15,5%%; узкие совпадения до ~27,5%%, но неустойчиво • НЕ BUY-СИГНАЛ",
            btcImpulse, pumpImpulse, lag, btcRecent, pumpRecent, recentBuyer
        )
        return baseChart.copy(
            patternScore = pattern,
            btcImpulseScore = btcImpulseScore,
            pumpHoldScore = pumpHoldScore,
            btcStableScore = btcStableScore,
            releaseScore = releaseScore,
            phase = phase,
            detail = detail
        )
    }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()
}

class BtcPumpReleaseGaugeView(context: Context) : View(context) {
    companion object { const val VIEW_TAG = "v692_btc_pump_release_gauge" }

    private val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#101820") }
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0F6FC"); textSize = sp(13f); isFakeBoldText = true
    }
    private val metric = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = sp(9f) }
    private val phasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#79C0FF"); textSize = sp(11f); isFakeBoldText = true
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9D1D9"); textSize = sp(9.5f)
    }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#30363D") }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#30363D"); strokeWidth = dp(1f) }
    private val btcLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0B72F"); style = Paint.Style.STROKE; strokeWidth = dp(2f)
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B949E"); textSize = sp(8.3f)
    }
    private var data = BtcPumpReleaseGaugeData()

    init { tag = VIEW_TAG }

    fun setData(value: BtcPumpReleaseGaugeData) {
        data = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(background.color)
        val d = data
        val price = d.btcPrice?.let { String.format(Locale.GERMANY, "$%,.0f", it) } ?: "$—"
        canvas.drawText("BITCOIN • 90 МИН • $price", dp(8f), dp(18f), title)
        drawMetric(canvas, "15м", d.btc15m, dp(8f), dp(34f))
        drawMetric(canvas, "30м", d.btc30m, dp(68f), dp(34f))
        drawMetric(canvas, "60м", d.btc60m, dp(128f), dp(34f))
        drawBtcPath(canvas, d.btcPoints, dp(8f), dp(43f), width - dp(8f), dp(105f))

        phasePaint.color = scoreColor(d.patternScore, d.fresh)
        canvas.drawText("${d.phase} • ПАТТЕРН ${d.patternScore}/100", dp(8f), dp(124f), phasePaint)

        val top = dp(139f)
        drawBar(canvas, "BTC РАЗГОН", d.btcImpulseScore, top)
        drawBar(canvas, "PUMP УДЕРЖАНИЕ", d.pumpHoldScore, top + dp(30f))
        drawBar(canvas, "BTC СТАБИЛЬНОСТЬ", d.btcStableScore, top + dp(60f))
        drawBar(canvas, "ОТПУСКАНИЕ / FLOW", d.releaseScore, top + dp(90f))

        val mainTop = top + dp(125f)
        canvas.drawText("СОВПАДЕНИЕ С ПАТТЕРНОМ • НЕ ВЕРОЯТНОСТЬ", dp(8f), mainTop, label)
        drawTrack(canvas, d.patternScore, mainTop + dp(7f), dp(17f), strong = true)

        d.detail.split('\n').take(2).forEachIndexed { index, text ->
            canvas.drawText(text, dp(8f), mainTop + dp(39f + index * 13f), detailPaint)
        }
    }

    private fun drawBtcPath(canvas: Canvas, points: List<Pair<Long, Double>>, left: Float, top: Float, right: Float, bottom: Float) {
        if (points.size < 2 || right <= left || bottom <= top) return
        val low = points.minOf { it.second }
        val high = points.maxOf { it.second }
        val span = max(high - low, high * 0.00025)
        canvas.drawLine(left, (top + bottom) / 2f, right, (top + bottom) / 2f, grid)
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = left + (right - left) * index / max(1, points.lastIndex)
            val y = bottom - ((point.second - low) / span).toFloat() * (bottom - top)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        btcLine.color = when {
            (data.btc60m ?: 0.0) > 0.0 -> Color.parseColor("#3FB950")
            (data.btc60m ?: 0.0) < 0.0 -> Color.parseColor("#F85149")
            else -> Color.parseColor("#F0B72F")
        }
        canvas.drawPath(path, btcLine)
    }

    private fun drawMetric(canvas: Canvas, period: String, value: Double?, x: Float, y: Float) {
        metric.color = when {
            value == null -> Color.parseColor("#8B949E")
            value > 0.0 -> Color.parseColor("#3FB950")
            value < 0.0 -> Color.parseColor("#F85149")
            else -> Color.parseColor("#C9D1D9")
        }
        val text = value?.let { String.format(Locale.GERMANY, "%+.2f%%", it) } ?: "—"
        canvas.drawText("$period $text", x, y, metric)
    }

    private fun drawBar(canvas: Canvas, text: String, value: Int, top: Float) {
        label.color = Color.parseColor("#C9D1D9")
        canvas.drawText("$text  $value/100", dp(8f), top, label)
        drawTrack(canvas, value, top + dp(5f), dp(11f), strong = false)
    }

    private fun drawTrack(canvas: Canvas, value: Int, top: Float, height: Float, strong: Boolean) {
        val left = dp(8f)
        val right = width - dp(8f)
        val rect = RectF(left, top, right, top + height)
        canvas.drawRoundRect(rect, height / 2f, height / 2f, track)
        val fraction = value.coerceIn(0, 100) / 100f
        if (fraction > 0f) {
            fill.color = scoreColor(value, data.fresh)
            val fillRect = RectF(left, top, left + (right - left) * fraction, top + height)
            canvas.drawRoundRect(fillRect, height / 2f, height / 2f, fill)
        }
        if (strong) {
            listOf(60, 70, 80).forEach { mark ->
                val x = left + (right - left) * mark / 100f
                canvas.drawLine(x, top, x, top + height, grid)
            }
        }
    }

    private fun scoreColor(value: Int, fresh: Boolean): Int = when {
        !fresh -> Color.parseColor("#8B949E")
        value >= 75 -> Color.parseColor("#3FB950")
        value >= 55 -> Color.parseColor("#D29922")
        value >= 35 -> Color.parseColor("#58A6FF")
        else -> Color.parseColor("#8B949E")
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity
}

internal object BtcPumpReleasePanelInstaller {
    fun install(chart: StrategyChartView): BtcPumpReleaseGaugeView? {
        val parent = chart.parent as? ViewGroup ?: return null
        parent.findViewWithTag<BtcPumpReleaseGaugeView>(BtcPumpReleaseGaugeView.VIEW_TAG)?.let { return it }
        val view = BtcPumpReleaseGaugeView(chart.context)
        val height = (330f * chart.resources.displayMetrics.density).roundToInt()
        val margin = (8f * chart.resources.displayMetrics.density).roundToInt()
        val lp = if (parent is LinearLayout) {
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height).apply { topMargin = margin }
        } else {
            ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height).apply { topMargin = margin }
        }
        val chartIndex = parent.indexOfChild(chart)
        parent.addView(view, (chartIndex + 1).coerceAtMost(parent.childCount), lp)
        return view
    }
}
