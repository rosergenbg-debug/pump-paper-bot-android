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
    private lateinit var pairSummary: TextView
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
            text = "FUSION ↔ PUMP MACHINE • 24H LAB"
            textSize = 21f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(8), dp(6), dp(8))
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, dp(62)))

        pairSummary = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#C9D1D9"))
            setBackgroundColor(Color.parseColor("#111827"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        root.addView(pairSummary, LinearLayout.LayoutParams(-1, dp(138)).apply { topMargin = dp(6) })

        summary = TextView(this).apply {
            textSize = 17f
            setTextColor(Color.parseColor("#7EE787"))
            setBackgroundColor(Color.parseColor("#101820"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        root.addView(summary, LinearLayout.LayoutParams(-1, dp(106)).apply { topMargin = dp(6) })

        status = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#FFF3BF"))
            setBackgroundColor(Color.parseColor("#2B2410"))
            setPadding(dp(12), dp(9), dp(12), dp(9))
        }
        root.addView(status, LinearLayout.LayoutParams(-1, dp(104)).apply { topMargin = dp(7) })

        root.addView(TextView(this).apply {
            text = "ПАРНЫЙ ЖУРНАЛ • один входной мозг, разные выходы"
            textSize = 13f
            setTextColor(Color.parseColor("#58A6FF"))
            setPadding(dp(4), dp(10), dp(4), dp(6))
        })

        trades = TextView(this).apply {
            textSize = 12.5f
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
        val pumpMachine = PumpMachineStore.state(this)
        val fusion = FusionSimStore.state(this)
        val market = BitpandaFusionStore.state(this)
        val bid = market.bid.takeIf { market.fresh(now) }
            ?: pumpMachine.entryPrice.takeIf { it > 0.0 }
            ?: fusion.entryPrice

        val pumpValue = PumpMachinePolicy.netLiquidationValue(pumpMachine, bid, market.feeRate)
        val pumpTotal = (pumpValue / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0
        val pumpTradeNet = PumpMachinePolicy.tradeNetPercent(pumpMachine, bid, market.feeRate)

        val fusionMetrics = FusionPriorityPolicy.metrics(
            portfolio = fusion,
            markPriceEur = bid,
            feeRate = market.feeRate,
            venueFresh = market.fresh(now)
        )
        val fusionValue = fusionMetrics.netLiquidationValueEur
        val fusionTotal = fusionMetrics.netPnlPercent

        val pmLast = pumpMachine.trades.lastOrNull()
        val fusionLast = fusion.trades.lastOrNull()
        pairSummary.text = buildString {
            append("ОБЩИЙ ВХОД: Shared Fusion Entry Engine • один 15-секундный снимок\n")
            append(String.format(Locale.GERMANY, "FUSION   €%.2f  %+.2f%%  • %s", fusionValue, fusionTotal, if (fusion.inPosition) "В PUMP" else "В EUR"))
            append("\n")
            append(String.format(Locale.GERMANY, "MACHINE  €%.2f  %+.2f%%  • %s", pumpValue, pumpTotal, if (pumpMachine.inPosition) "В PUMP" else "В EUR"))
            append("\n")
            append("Последнее: FUSION ${lastTradeLabel(fusionLast)} • MACHINE ${lastTradeLabel(pmLast)}")
        }

        summary.text = buildString {
            append(String.format(Locale.GERMANY, "PUMP MACHINE • СЧЁТ €%.2f • ВСЕГО %+.2f%%", pumpValue, pumpTotal))
            append("\n")
            if (pumpMachine.inPosition) {
                append(String.format(Locale.GERMANY, "ТЕКУЩАЯ СДЕЛКА %+.2f%% NET", pumpTradeNet))
                append(" • TP +3,00% • SL −1,50%")
            } else {
                append("В EUR • ждём следующий независимый общий Fusion-вход")
            }
        }
        summary.setTextColor(Color.parseColor(if (pumpTotal >= 0.0) "#7EE787" else "#FF7B72"))

        status.text = "ВХОД У ОБОИХ ФИЗИЧЕСКИ ОДИНАКОВЫЙ; состояния подтверждения и cooldown независимы.\n" +
            "MACHINE: ${PumpMachineStore.lastStatus(this)}"

        data class PairEvent(val source: String, val trade: FusionSimTrade)
        val events = buildList {
            pumpMachine.trades.takeLast(60).forEach { add(PairEvent("MACHINE", it)) }
            fusion.trades.takeLast(60).forEach { add(PairEvent("FUSION", it)) }
        }.sortedByDescending { it.trade.time }.take(120)

        trades.text = if (events.isEmpty()) {
            "Сделок пока нет. После V5.22 оба участника получают один и тот же входной снимок рынка, но ведут позиции независимо."
        } else {
            events.joinToString("\n\n") { event ->
                val trade = event.trade
                val pnl = if (trade.action == "SELL") {
                    String.format(Locale.GERMANY, " • PnL %+.2f €", trade.pnlEur)
                } else ""
                "${PumpBotEngine.formatDate(trade.time)} • ${event.source} • ${trade.action} • " +
                    String.format(Locale.GERMANY, "€%.8f", trade.price) + pnl +
                    "\n${trade.reason}"
            }
        }
    }

    private fun lastTradeLabel(trade: FusionSimTrade?): String {
        if (trade == null) return "—"
        return "${trade.action} ${PumpBotEngine.formatDate(trade.time)}"
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
