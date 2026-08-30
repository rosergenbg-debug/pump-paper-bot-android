package com.example.pumppaperbot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Visual-only V5.18 money-flow surface. It never creates a trading decision.
 * The numbers are executed PUMP/USDT taker notional, not capital ownership or market cap.
 */
data class MoneyFlowWindowUi(
    val label: String,
    val minutes: Int,
    val buyUsdt: Double,
    val sellUsdt: Double,
    val ready: Boolean
) {
    val totalUsdt: Double get() = buyUsdt + sellUsdt
    val netUsdt: Double get() = buyUsdt - sellUsdt
    val buyerShare: Double
        get() = if (totalUsdt > 0.0) (buyUsdt / totalUsdt).coerceIn(0.0, 1.0) else 0.5
}

data class MoneyFlowPanelData(
    val fresh: Boolean,
    val oneMinute: MoneyFlowWindowUi,
    val fiveMinutes: MoneyFlowWindowUi,
    val fifteenMinutes: MoneyFlowWindowUi,
    val thirtyMinutes: MoneyFlowWindowUi,
    val sixtyMinutes: MoneyFlowWindowUi,
    val activityRatio: Double?,
    val flowScore15m: Int?,
    val state: String
) {
    val windows: List<MoneyFlowWindowUi>
        get() = listOf(oneMinute, fiveMinutes, fifteenMinutes, thirtyMinutes, sixtyMinutes)
}

object MoneyFlowPresentation {
    fun from(
        micro: MicroImpulseSnapshot,
        breathing: LiveMarketBreathingSnapshot,
        now: Long = System.currentTimeMillis()
    ): MoneyFlowPanelData {
        val age = (now - micro.updatedAt).coerceAtLeast(0L)
        val fresh = micro.connected && micro.updatedAt > 0L && age <= 90_000L
        val history = micro.flowHistorySeconds
        val one = MoneyFlowWindowUi(
            label = "СЕЙЧАС",
            minutes = 1,
            buyUsdt = micro.buyNotional60s.coerceAtLeast(0.0),
            sellUsdt = micro.sellNotional60s.coerceAtLeast(0.0),
            ready = fresh && history >= 55L
        )
        val five = MoneyFlowWindowUi(
            label = "5 МИН",
            minutes = 5,
            buyUsdt = micro.buyNotional5m.coerceAtLeast(0.0),
            sellUsdt = micro.sellNotional5m.coerceAtLeast(0.0),
            ready = fresh && history >= 4L * 60L
        )
        val fifteen = MoneyFlowWindowUi(
            label = "15 МИН",
            minutes = 15,
            buyUsdt = micro.buyNotional15m.coerceAtLeast(0.0),
            sellUsdt = micro.sellNotional15m.coerceAtLeast(0.0),
            ready = fresh && history >= 12L * 60L
        )
        val thirty = MoneyFlowWindowUi(
            label = "30 МИН",
            minutes = 30,
            buyUsdt = micro.buyNotional30m.coerceAtLeast(0.0),
            sellUsdt = micro.sellNotional30m.coerceAtLeast(0.0),
            ready = fresh && history >= 27L * 60L
        )
        val sixty = MoneyFlowWindowUi(
            label = "1 ЧАС",
            minutes = 60,
            buyUsdt = micro.buyNotional60m.coerceAtLeast(0.0),
            sellUsdt = micro.sellNotional60m.coerceAtLeast(0.0),
            ready = fresh && history >= 57L * 60L
        )
        val activityRatio = if (five.ready && fifteen.ready && fifteen.totalUsdt > 0.0) {
            val recentPerMinute = five.totalUsdt / 5.0
            val backgroundPerMinute = fifteen.totalUsdt / 15.0
            if (backgroundPerMinute > 0.0) (recentPerMinute / backgroundPerMinute).coerceIn(0.0, 4.0) else null
        } else null
        val flowScore15m = breathing.horizons.firstOrNull { it.minutes == 15 }?.score
        val state = when {
            !fresh -> "ДЕНЕЖНЫЙ ПОТОК НЕ СВЕЖИЙ"
            !five.ready -> "НАКАПЛИВАЕМ ДЕНЕЖНЫЙ ПОТОК"
            five.buyerShare >= 0.60 && five.netUsdt > 0.0 -> "СИЛЬНЫЙ ПЕРЕВЕС ПОКУПОК"
            five.buyerShare <= 0.40 && five.netUsdt < 0.0 -> "СИЛЬНЫЙ ПЕРЕВЕС ПРОДАЖ"
            activityRatio != null && activityRatio >= 1.55 -> "МАССА ДЕНЕГ РЕЗКО УСКОРИЛАСЬ"
            abs(five.buyerShare - 0.5) <= 0.04 -> "БОЛЬШАЯ БОРЬБА • ПЕРЕВЕС МАЛ"
            five.netUsdt > 0.0 -> "ДЕНЬГИ УМЕРЕННО ДАВЯТ В ПОКУПКУ"
            five.netUsdt < 0.0 -> "ДЕНЬГИ УМЕРЕННО ДАВЯТ В ПРОДАЖУ"
            else -> "ПОТОК СБАЛАНСИРОВАН"
        }
        return MoneyFlowPanelData(
            fresh, one, five, fifteen, thirty, sixty, activityRatio, flowScore15m, state
        )
    }

    fun compactUsd(value: Double, signed: Boolean = false): String {
        val sign = when {
            signed && value > 0.0 -> "+"
            signed && value < 0.0 -> "−"
            else -> ""
        }
        val absolute = abs(value)
        val body = when {
            absolute >= 1_000_000_000.0 -> String.format(Locale.GERMANY, "%.1fB", absolute / 1_000_000_000.0)
            absolute >= 1_000_000.0 -> String.format(Locale.GERMANY, "%.2fM", absolute / 1_000_000.0)
            absolute >= 1_000.0 -> String.format(Locale.GERMANY, "%.1fk", absolute / 1_000.0)
            else -> String.format(Locale.GERMANY, "%.0f", absolute)
        }
        return "${sign}\$$body"
    }

    fun summary(data: MoneyFlowPanelData): String = buildString {
        append(data.state)
        data.windows.forEach { window ->
            if (window.ready) {
                append("\n${window.label}: нетто ${compactUsd(window.netUsdt, true)}")
                append(" • BUY ${compactUsd(window.buyUsdt)} • SELL ${compactUsd(window.sellUsdt)}")
                append(String.format(Locale.GERMANY, " • %.0f%% BUY", window.buyerShare * 100.0))
            } else {
                append("\n${window.label}: накопление")
            }
        }
        data.activityRatio?.let {
            append(String.format(Locale.GERMANY, "\nТемп оборота 5м к фону 15м: ×%.2f", it))
        }
        data.flowScore15m?.let { append(" • поток 15м $it/100") }
        append("\nЭто прошедший taker-оборот, а не сумма денег, принадлежащая держателям PUMP.")
    }
}

class MoneyFlowStripView(context: Context) : View(context) {
    companion object { const val VIEW_TAG = "v518_money_flow_strip" }

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#101820") }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#30363D") }
    private val buy = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2EA043") }
    private val sell = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#DA3633") }
    private val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D0D7DE"); strokeWidth = dp(1f) }
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0F6FC"); textSize = sp(11f); isFakeBoldText = true
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9D1D9"); textSize = sp(10f); isFakeBoldText = true
    }
    private val value = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = sp(10f); textAlign = Paint.Align.RIGHT; isFakeBoldText = true
    }
    private val note = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B949E"); textSize = sp(8f)
    }
    private val rect = RectF()
    private var data: MoneyFlowPanelData? = null

    init { tag = VIEW_TAG }

    fun setData(value: MoneyFlowPanelData) {
        data = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(bg.color)
        val d = data
        canvas.drawText("PUMP • ВОШЛО / ВЫШЛО / БАЛАНС (USDT)", dp(8f), dp(15f), title)
        if (d == null) return
        val rows = d.windows
        val left = dp(8f)
        val right = width - dp(8f)
        val barWidth = max(dp(80f), right - left)
        rows.forEachIndexed { index, window ->
            val y = dp(35f + index * 34f)
            canvas.drawText(window.label, left, y, label)
            value.textSize = sp(8.2f)
            value.color = if (!window.ready) Color.parseColor("#8B949E") else Color.WHITE
            val amounts = if (window.ready) {
                "ВОШЛО ${MoneyFlowPresentation.compactUsd(window.buyUsdt)}  •  " +
                    "ВЫШЛО ${MoneyFlowPresentation.compactUsd(window.sellUsdt)}  •  " +
                    "БАЛАНС ${MoneyFlowPresentation.compactUsd(window.netUsdt, true)}"
            } else "ДАННЫЕ НАКАПЛИВАЮТСЯ"
            canvas.drawText(amounts, right, y, value)
            rect.set(left, y + dp(7f), left + barWidth, y + dp(15f))
            canvas.drawRoundRect(rect, dp(3f), dp(3f), track)
            if (window.ready && window.totalUsdt > 0.0) {
                val split = left + (barWidth * window.buyerShare).toFloat()
                rect.set(left, y + dp(7f), split, y + dp(15f))
                canvas.drawRoundRect(rect, dp(3f), dp(3f), buy)
                rect.set(split, y + dp(7f), left + barWidth, y + dp(15f))
                canvas.drawRoundRect(rect, dp(3f), dp(3f), sell)
                canvas.drawLine(left + barWidth / 2f, y + dp(5f), left + barWidth / 2f, y + dp(17f), divider)
            }
        }
        canvas.drawText(
            "Исполненный taker-оборот; не сумма активов держателей.",
            left,
            height - dp(6f),
            note
        )
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
    private fun sp(v: Float): Float = v * resources.displayMetrics.scaledDensity
}

class MoneyMassDialView(context: Context) : View(context) {
    companion object { const val VIEW_TAG = "v518_money_mass_dial" }

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#101820") }
    private val base = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#30363D"); style = Paint.Style.STROKE; strokeCap = Paint.Cap.BUTT
    }
    private val buy = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2EA043"); style = Paint.Style.STROKE; strokeCap = Paint.Cap.BUTT
    }
    private val sell = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DA3633"); style = Paint.Style.STROKE; strokeCap = Paint.Cap.BUTT
    }
    private val center = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F0F6FC"); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8B949E"); textAlign = Paint.Align.CENTER }
    private val rect = RectF()
    private var data: MoneyFlowPanelData? = null

    init { tag = VIEW_TAG }

    fun setData(value: MoneyFlowPanelData) {
        data = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(bg.color)
        val d = data ?: return
        val primary = d.fiveMinutes
        val cx = width / 2f
        val cy = dp(118f)
        val radius = minOf(width * 0.31f, dp(94f))
        val activity = d.activityRatio?.coerceIn(0.55, 2.2) ?: 1.0
        val stroke = dp((18.0 + activity * 5.0).toFloat())
        base.strokeWidth = stroke
        buy.strokeWidth = stroke
        sell.strokeWidth = stroke
        rect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(rect, -90f, 360f, false, base)
        if (primary.ready && primary.totalUsdt > 0.0) {
            val buySweep = (360.0 * primary.buyerShare).toFloat()
            canvas.drawArc(rect, -90f, buySweep, false, buy)
            canvas.drawArc(rect, -90f + buySweep, 360f - buySweep, false, sell)
        }
        center.textSize = sp(12f)
        canvas.drawText("5 МИН", cx, cy - dp(26f), center)
        center.textSize = sp(25f)
        center.color = when {
            !primary.ready -> Color.parseColor("#8B949E")
            primary.netUsdt >= 0.0 -> buy.color
            else -> sell.color
        }
        canvas.drawText(
            if (primary.ready) MoneyFlowPresentation.compactUsd(primary.netUsdt, true) else "НАКОПЛЕНИЕ",
            cx,
            cy + dp(5f),
            center
        )
        small.textSize = sp(10f)
        small.color = Color.parseColor("#8B949E")
        val turnover = if (primary.ready) "оборот ${MoneyFlowPresentation.compactUsd(primary.totalUsdt)}" else "ждём 5 минут истории"
        canvas.drawText(turnover, cx, cy + dp(28f), small)

        center.textSize = sp(12f)
        center.color = Color.parseColor("#F0F6FC")
        canvas.drawText(d.state, cx, dp(235f), center)
        small.textSize = sp(10f)
        val ratio = d.activityRatio?.let { String.format(Locale.GERMANY, "темп ×%.2f к 15м", it) } ?: "темп: накопление фона"
        canvas.drawText(ratio, cx, dp(255f), small)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
    private fun sp(v: Float): Float = v * resources.displayMetrics.scaledDensity
}
