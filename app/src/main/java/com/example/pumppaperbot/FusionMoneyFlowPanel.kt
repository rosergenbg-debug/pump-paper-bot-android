package com.example.pumppaperbot

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.math.abs

/**
 * Compact Binance PUMP/USDT taker flow. Five short rows, figures overlay the
 * green (taker buys) / red (taker sells) proportional bars. NOT deposits or withdrawals.
 */
internal class FusionMoneyFlowPanel(context: Context) : LinearLayout(context) {
    private val status: TextView
    private val bars: LinearLayout

    init {
        orientation = VERTICAL
        setPadding(dp(8), dp(9), dp(8), dp(10))
        setBackgroundColor(Color.rgb(17, 26, 34))

        addView(text("PUMP • ПОКУПКИ / ПРОДАЖИ (USDT)", 13f, "#E8EEF6", true))
        status = text("Binance: подключение потока…", 10.5f, "#B5C1CD", false).apply {
            maxLines = 2
            setPadding(0, dp(2), 0, dp(3))
        }
        addView(status)
        bars = LinearLayout(context).apply { orientation = VERTICAL }
        addView(bars)
        addView(text(
            "ВХ — покупки • ВЫХ — продажи • Δ — разница. Binance taker-сделки, не ввод/вывод денег.",
            9f, "#9EAAB8", false
        ), LayoutParams(-1, -2).apply { topMargin = dp(6) })
    }

    fun render(model: FusionFlowViewModel) {
        status.text = model.status
        status.setTextColor(Color.parseColor(if (model.fresh) "#8DDCA8" else "#FF8B82"))
        bars.removeAllViews()
        if (!model.fresh) {
            bars.addView(text("Поток временно неактуален — старые суммы скрыты", 11f, "#FF8B82", false),
                LayoutParams(-1, dp(24)))
            return
        }
        model.rows.forEach { line ->
            val complete = model.secondsCovered >= line.minutes * 60L
            bars.addView(buildRow(line, complete), LayoutParams(-1, dp(29)).apply {
                topMargin = dp(2)
            })
        }
    }

    private fun buildRow(line: FusionFlowLine, complete: Boolean): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(text(
            "${line.minutes}м" + (if (complete) "" else "*"),
            10.5f, if (complete) "#DDE6EF" else "#F5D68B", true
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
        }, LayoutParams(dp(39), -1))

        val fullBar = FrameLayout(context)
        val segments = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(Color.parseColor("#333D49"))
        }
        val all = line.buyUsdt + line.sellUsdt
        if (all > 0.0) {
            // Weighted segments; very small nonzero fractions remain visible.
            val buyFraction = line.buyRatio.coerceIn(0.001f, .999f)
            segments.addView(LinearLayout(context).apply {
                setBackgroundColor(Color.parseColor("#287A4E"))
            }, LayoutParams(0, -1, buyFraction))
            segments.addView(LinearLayout(context).apply {
                setBackgroundColor(Color.parseColor("#A43C47"))
            }, LayoutParams(0, -1, 1f - buyFraction))
        }
        fullBar.addView(segments, FrameLayout.LayoutParams(-1, -1))

        // An unobtrusive dark tint maintains legibility on both colored halves.
        fullBar.addView(LinearLayout(context).apply {
            setBackgroundColor(Color.argb(48, 0, 0, 0))
        }, FrameLayout.LayoutParams(-1, -1))
        val number = text(
            "ВХ ${compact(line.buyUsdt)}  ВЫХ ${compact(line.sellUsdt)}  Δ ${signed(line.netUsdt)}",
            10f, "#FFFFFF", true
        ).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            setShadowLayer(dp(2).toFloat(), 0f, dp(1).toFloat(), Color.BLACK)
            setPadding(dp(2), 0, dp(2), 0)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 9.5f)
        }
        fullBar.addView(number, FrameLayout.LayoutParams(-1, -1))
        row.addView(fullBar, LayoutParams(0, dp(24), 1f))
        return row
    }

    private fun compact(value: Double): String {
        if (!value.isFinite()) return "—"
        val a = abs(value)
        return when {
            a >= 1_000_000 -> String.format(Locale.GERMANY, "%.1fм", value / 1_000_000)
            a >= 1_000 -> String.format(Locale.GERMANY, "%.1fк", value / 1_000)
            else -> String.format(Locale.GERMANY, "%.0f", value)
        }
    }

    private fun signed(value: Double) =
        (if (value >= 0) "+" else "−") + compact(abs(value))

    private fun text(value: String, size: Float, color: String, bold: Boolean): TextView =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(Color.parseColor(color))
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.START
        }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
}
