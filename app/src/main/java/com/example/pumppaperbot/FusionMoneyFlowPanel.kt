package com.example.pumppaperbot

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

/** Binance PUMPUSDT taker-side turnover, NOT exchange deposits or withdrawals. */
internal class FusionMoneyFlowPanel(context: Context) : LinearLayout(context) {
    private val title: TextView
    private val status: TextView
    private val bars: LinearLayout

    init {
        orientation = VERTICAL
        setPadding(dp(9), dp(12), dp(9), dp(12))
        setBackgroundColor(Color.rgb(17, 26, 34))
        title = text("PUMP • АГРЕССИВНЫЕ ПОКУПКИ / ПРОДАЖИ (USDT)", 14, "#E8EEF6", true)
        addView(title)
        status = text("Поток подключается", 11, "#B5C1CD", false)
        addView(status, LayoutParams(-1, -2).apply { topMargin = dp(4) })
        bars = LinearLayout(context).apply { orientation = VERTICAL }
        addView(bars, LayoutParams(-1, -2).apply { topMargin = dp(6) })
        addView(text("Рыночные сделки Binance PUMP/USDT. БАЛАНС = покупки − продажи taker; НЕ пополнение/вывод средств и НЕ котировка Bitpanda.", 10, "#9EAAB8", false),
            LayoutParams(-1, -2).apply { topMargin = dp(10) })
    }

    fun render(model: FusionFlowViewModel) {
        status.text = model.status
        status.setTextColor(Color.parseColor(if (model.fresh) "#8DDCA8" else "#FF8B82"))
        bars.removeAllViews()
        if (!model.fresh) {
            bars.addView(text("Нет живых данных — старые суммы скрыты", 13, "#FF8B82", false),
                LayoutParams(-1, dp(44)))
            return
        }
        for (flow in model.rows) {
            val covered = model.secondsCovered >= flow.minutes * 60L
            val name = if (flow.minutes == 1) "1 МИН" else "${flow.minutes} МИН"
            val tag = if (covered) "" else " • не полное окно"
            val row = LinearLayout(context).apply { orientation = VERTICAL }
            row.addView(text(
                name + tag + "   ВОШЛО " + format(flow.buyUsdt) + "  •  ВЫШЛО " + format(flow.sellUsdt),
                11, "#E8EEF6", false
            ))
            val bar = LinearLayout(context).apply {
                orientation = HORIZONTAL
                setBackgroundColor(Color.parseColor("#333D49"))
            }
            val all = flow.buyUsdt + flow.sellUsdt
            if (all > 0) {
                val buyWeight = flow.buyRatio.coerceAtLeast(.001f)
                val sellWeight = (1f - flow.buyRatio).coerceAtLeast(.001f)
                bar.addView(LinearLayout(context).apply {
                    setBackgroundColor(Color.parseColor("#258E53"))
                }, LayoutParams(0, dp(13), buyWeight))
                bar.addView(LinearLayout(context).apply {
                    setBackgroundColor(Color.parseColor("#B8424D"))
                }, LayoutParams(0, dp(13), sellWeight))
            }
            row.addView(bar, LayoutParams(-1, dp(13)).apply { topMargin = dp(4) })
            val balance = flow.netUsdt
            val textColor = if (balance >= 0.0) "#7EE787" else "#FF7B72"
            row.addView(text("БАЛАНС " + (if (balance >= 0) "+" else "−") + format(kotlin.math.abs(balance)), 11, textColor, true),
                LayoutParams(-1, -2).apply { topMargin = dp(3) })
            bars.addView(row, LayoutParams(-1, -2).apply { topMargin = dp(11) })
        }
    }

    private fun format(value: Double): String {
        if (!value.isFinite()) return "—"
        val abs = kotlin.math.abs(value)
        return when {
            abs >= 1_000_000 -> String.format(Locale.GERMANY, "%.2f млн", value / 1_000_000)
            abs >= 1_000 -> String.format(Locale.GERMANY, "%.1f тыс.", value / 1000)
            else -> String.format(Locale.GERMANY, "%.0f", value)
        }
    }

    private fun text(value: String, size: Int, color: String, bold: Boolean): TextView =
        TextView(context).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(Color.parseColor(color))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.START
        }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
}
