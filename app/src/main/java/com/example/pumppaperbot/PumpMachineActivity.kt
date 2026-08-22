package com.example.pumppaperbot

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class PumpMachineActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var summary: TextView
    private lateinit var status: TextView
    private lateinit var trades: TextView

    private val refresh = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 5_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(12))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        root.addView(TextView(this).apply {
            text = "PUMP MACHINE • +3% NET / −1,5% NET"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(8), dp(6), dp(8))
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, dp(62)))

        summary = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.parseColor("#7EE787"))
            setBackgroundColor(Color.parseColor("#101820"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        root.addView(summary, LinearLayout.LayoutParams(-1, dp(112)).apply { topMargin = dp(6) })

        status = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#FFF3BF"))
            setBackgroundColor(Color.parseColor("#2B2410"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        root.addView(status, LinearLayout.LayoutParams(-1, dp(118)).apply { topMargin = dp(7) })

        root.addView(TextView(this).apply {
            text = "СДЕЛКИ PUMP MACHINE • новая чистая история V5.21"
            textSize = 13f
            setTextColor(Color.parseColor("#58A6FF"))
            setPadding(dp(4), dp(10), dp(4), dp(6))
        })

        trades = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#C9D1D9"))
            setBackgroundColor(Color.parseColor("#161B22"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val scroll = ScrollView(this).apply { addView(trades) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        render()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refresh)
        handler.post(refresh)
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun render() {
        val now = System.currentTimeMillis()
        val portfolio = PumpMachineStore.state(this)
        val market = BitpandaFusionStore.state(this)
        val bid = market.bid.takeIf { market.fresh(now) } ?: portfolio.entryPrice
        val value = PumpMachinePolicy.netLiquidationValue(portfolio, bid, market.feeRate)
        val total = (value / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0
        val tradeNet = PumpMachinePolicy.tradeNetPercent(portfolio, bid, market.feeRate)
        summary.text = buildString {
            append(String.format(Locale.GERMANY, "СЧЁТ €%.2f  •  ВСЕГО %+.2f%%", value, total))
            append("\n")
            if (portfolio.inPosition) {
                append(String.format(Locale.GERMANY, "В PUMP • ТЕКУЩАЯ СДЕЛКА %+.2f%% NET", tradeNet))
                append("\nЦЕЛЬ +3,00% • STOP −1,50%")
            } else {
                append("В ЕВРО • ждём следующий Fusion-вход")
                append("\nЦЕЛЬ СДЕЛКИ +3,00% NET • STOP −1,50% NET")
            }
        }
        summary.setTextColor(Color.parseColor(if (total >= 0.0) "#7EE787" else "#FF7B72"))

        status.text = "ЛОГИКА: тот же вход/системный выход, что у Fusion; максимум сделки +3% net.\n" +
            PumpMachineStore.lastStatus(this)

        val events = portfolio.trades.takeLast(120).asReversed()
        trades.text = if (events.isEmpty()) {
            "Сделок пока нет. Счёт V5.21 начинается с €1000 и не смешивается со старым DeepSig."
        } else {
            events.joinToString("\n\n") { trade ->
                val label = if (trade.action == "BUY") "BUY" else "SELL"
                val pnl = if (trade.action == "SELL") {
                    String.format(Locale.GERMANY, " • PnL %+.2f €", trade.pnlEur)
                } else ""
                "${PumpBotEngine.formatDate(trade.time)} • $label • " +
                    String.format(Locale.GERMANY, "€%.8f", trade.price) + pnl +
                    "\n${trade.reason}"
            }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
