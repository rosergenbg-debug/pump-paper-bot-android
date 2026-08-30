package com.example.pumppaperbot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import java.util.Locale
import kotlin.math.abs
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
    val fresh: Boolean = false
)

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
        if (valid.size < 35) {
            return BtcPumpReleaseGaugeData(
                phase = "НАКАПЛИВАЕМ 45 МИНУТ КОНТЕКСТА",
                detail = "Есть ${valid.size} мин. • нужно около 45 мин. • не BUY-сигнал",
                fresh = fresh
            )
        }

        fun atOrBefore(target: Long): LiveBreathingSample? =
            valid.lastOrNull { it.at <= target }

        val t45 = atOrBefore(last.at - 45L * 60L * 1_000L) ?: valid.first()
        val t15 = atOrBefore(last.at - 15L * 60L * 1_000L) ?: return BtcPumpReleaseGaugeData(fresh = fresh)
        val recent15 = valid.filter { it.at >= last.at - 15L * 60L * 1_000L }
        val prior5 = valid.filter { it.at in (last.at - 10L * 60L * 1_000L)..(last.at - 5L * 60L * 1_000L) }
        val last5 = valid.filter { it.at >= last.at - 5L * 60L * 1_000L }

        fun change(from: Double, to: Double): Double =
            if (from > 0.0) (to / from - 1.0) * 100.0 else 0.0
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

        val btcImpulse = change(t45.bitcoinPriceUsdt, t15.bitcoinPriceUsdt)
        val pumpImpulse = change(t45.priceUsdt, t15.priceUsdt)
        val lag = pumpImpulse - btcImpulse
        val btcRecent = change(t15.bitcoinPriceUsdt, last.bitcoinPriceUsdt)
        val pumpRecent = change(t15.priceUsdt, last.priceUsdt)
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
        // Sequential caps stop a high late component from pretending that earlier stages happened.
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
            "BTC30 %+,.2f%% • PUMP30 %+,.2f%% • лаг %+,.2f п.п. • BTC15 %+,.2f%% • PUMP15 %+,.2f%% • BUY %.1f%%\n60д: фон +2%%/60м ≈15,5%%; узкие совпадения доходили до ~27,5%%, но неустойчиво • НЕ BUY-СИГНАЛ",
            btcImpulse, pumpImpulse, lag, btcRecent, pumpRecent, recentBuyer
        )
        return BtcPumpReleaseGaugeData(
            patternScore = pattern,
            btcImpulseScore = btcImpulseScore,
            pumpHoldScore = pumpHoldScore,
            btcStableScore = btcStableScore,
            releaseScore = releaseScore,
            phase = phase,
            detail = detail,
            fresh = fresh
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
    private val phasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#79C0FF"); textSize = sp(11f); isFakeBoldText = true
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9D1D9"); textSize = sp(9.5f)
    }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#30363D") }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B949E"); textSize = sp(8.5f)
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
        canvas.drawText("BTC → PUMP • УДЕРЖАНИЕ / ОТПУСКАНИЕ", dp(8f), dp(18f), title)
        phasePaint.color = scoreColor(d.patternScore, d.fresh)
        canvas.drawText("${d.phase} • ПАТТЕРН ${d.patternScore}/100", dp(8f), dp(37f), phasePaint)

        val top = dp(50f)
        drawBar(canvas, "BTC РАЗГОН", d.btcImpulseScore, top)
        drawBar(canvas, "PUMP УДЕРЖАНИЕ", d.pumpHoldScore, top + dp(32f))
        drawBar(canvas, "BTC СТАБИЛЬНОСТЬ", d.btcStableScore, top + dp(64f))
        drawBar(canvas, "ОТПУСКАНИЕ / FLOW", d.releaseScore, top + dp(96f))

        val mainTop = top + dp(133f)
        canvas.drawText("СОВПАДЕНИЕ С ПАТТЕРНОМ • НЕ ВЕРОЯТНОСТЬ", dp(8f), mainTop, label)
        drawTrack(canvas, d.patternScore, mainTop + dp(7f), dp(17f), strong = true)

        val lines = d.detail.split('\n')
        lines.take(2).forEachIndexed { index, text ->
            canvas.drawText(text, dp(8f), mainTop + dp(39f + index * 13f), detailPaint)
        }
    }

    private fun drawBar(canvas: Canvas, text: String, value: Int, top: Float) {
        label.color = Color.parseColor("#C9D1D9")
        canvas.drawText("$text  $value/100", dp(8f), top, label)
        drawTrack(canvas, value, top + dp(6f), dp(12f), strong = false)
    }

    private fun drawTrack(canvas: Canvas, value: Int, top: Float, height: Float, strong: Boolean) {
        val left = dp(8f)
        val right = width - dp(8f)
        val rect = RectF(left, top, right, top + height)
        canvas.drawRoundRect(rect, height / 2f, height / 2f, track)
        val fraction = value.coerceIn(0, 100) / 100f
        if (fraction <= 0f) return
        fill.color = scoreColor(value, data.fresh)
        val fillRect = RectF(left, top, left + (right - left) * fraction, top + height)
        canvas.drawRoundRect(fillRect, height / 2f, height / 2f, fill)
        if (strong) {
            val marker = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F0F6FC"); strokeWidth = dp(1f) }
            listOf(60, 70, 80).forEach { mark ->
                val x = left + (right - left) * mark / 100f
                canvas.drawLine(x, top, x, top + height, marker)
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
