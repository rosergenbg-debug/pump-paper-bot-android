package com.example.pumppaperbot

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class StatsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val snapshot = PumpBotEngine.snapshot(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D1117"))
        }

        root.addView(summaryPanel(snapshot))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(24))
        }
        content.addView(section("PRIMARY 4H", snapshot.primary))
        content.addView(section("EXPERIMENT 2H + 4H", snapshot.experimental))
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun summaryPanel(snapshot: BotSnapshot): View {
        val totalTrades = snapshot.primary.tradeCount + snapshot.experimental.tradeCount
        val totalFees = snapshot.primary.totalFees + snapshot.experimental.totalFees
        val totalEquity = snapshot.primary.equity + snapshot.experimental.equity
        val totalProfit = totalEquity - PumpBotEngine.startBalance * 2
        val start = PumpBotEngine.formatTime(snapshot.startedAt)

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundColor(Color.parseColor("#161B22"))
            minimumHeight = dp(190)
            addView(label("STATISTICS", 22, "#F0F6FC", true))
            addView(label("Start: $start   Last sync: ${PumpBotEngine.formatTime(snapshot.lastSync)}", 13, "#8B949E", false))
            addView(label("Invested: 2000.00 USDT", 15, "#C9D1D9", false))
            addView(label(String.format(Locale.US, "Now: %.2f USDT", totalEquity), 20, if (totalProfit >= 0) "#32C789" else "#FF4D6D", true))
            addView(label(String.format(Locale.US, "P/L: %+.2f USDT   Trades: %d   Fees paid: %.2f USDT", totalProfit, totalTrades, totalFees), 15, "#F0F6FC", false))
            addView(label(String.format(Locale.US, "Fee per operation: %.2f%%", PumpBotEngine.feeRate * 100.0), 13, "#8B949E", false))
        }
    }

    private fun section(title: String, result: StrategyResult): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(16))
        }
        root.addView(label(title, 18, "#F0F6FC", true))
        root.addView(label(String.format(Locale.US, "Balance %.2f USDT | P/L %+.2f%% | Trades %d | Fees %.2f USDT", result.equity, result.profitPercent, result.tradeCount, result.totalFees), 14, "#C9D1D9", false))

        if (result.state.trades.isEmpty()) {
            root.addView(label("No trades yet.", 13, "#8B949E", false))
        } else {
            result.state.trades.reversed().forEach { trade ->
                root.addView(greenDivider())
                root.addView(tradeRow(trade))
            }
        }
        return root
    }

    private fun tradeRow(trade: TradeEvent): View {
        val color = if (trade.action == "BUY") "#32C789" else "#FF4D6D"
        val pnl = if (trade.action == "SELL") String.format(Locale.US, " | P/L %+.2f", trade.pnl) else ""
        return TextView(this).apply {
            setPadding(dp(10), dp(9), dp(10), dp(9))
            setBackgroundColor(Color.parseColor("#101820"))
            setTextColor(Color.parseColor("#F0F6FC"))
            textSize = 13f
            text = String.format(
                Locale.US,
                "%s  %s\nprice %.8f | amount %.2f | fee %.2f | balance %.2f%s\ncoins %.2f",
                PumpBotEngine.formatTime(trade.time),
                trade.action,
                trade.price,
                trade.amount,
                trade.fee,
                trade.equity,
                pnl,
                trade.coins
            )
            setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            setTextColor(Color.parseColor(color))
        }
    }

    private fun greenDivider(): View {
        return View(this).apply {
            setBackgroundColor(Color.parseColor("#32C789"))
            layoutParams = LinearLayout.LayoutParams(-1, dp(4)).apply {
                topMargin = dp(10)
                bottomMargin = dp(6)
            }
        }
    }

    private fun label(text: String, size: Int, color: String, bold: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(Color.parseColor(color))
            gravity = Gravity.START
            setPadding(0, dp(3), 0, dp(3))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
