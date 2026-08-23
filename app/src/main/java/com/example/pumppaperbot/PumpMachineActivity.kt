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
            text = "FUSION ↔ PUMP 3% ↔ PUMP 2% • LAB"
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
        root.addView(pairSummary, LinearLayout.LayoutParams(-1, dp(166)).apply { topMargin = dp(6) })

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
        val pumpMachine2 = PumpMachine2Store.state(this)
        val fusion = FusionSimStore.state(this)
        val market = BitpandaFusionStore.state(this)
        val bid = market.bid.takeIf { market.fresh(now) }
            ?: pumpMachine2.entryPrice.takeIf { it > 0.0 }
            ?: pumpMachine.entryPrice.takeIf { it > 0.0 }
            ?: fusion.entryPrice

        val pumpValue = PumpMachinePolicy.netLiquidationValue(pumpMachine, bid, market.feeRate)
        val pumpTotal = (pumpValue / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0
        val pumpTradeNet = PumpMachinePolicy.tradeNetPercent(pumpMachine, bid, market.feeRate)
        val pump2Value = PumpMachine2Policy.netLiquidationValue(pumpMachine2, bid, market.feeRate)
        val pump2Total = (pump2Value / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0
        val pump2TradeNet = PumpMachine2Policy.tradeNetPercent(pumpMachine2, bid, market.feeRate)

        val fusionMetrics = FusionPriorityPolicy.metrics(
            portfolio = fusion,
            markPriceEur = bid,
            feeRate = market.feeRate,
            venueFresh = market.fresh(now)
        )
        val fusionValue = fusionMetrics.netLiquidationValueEur
        val fusionTotal = fusionMetrics.netPnlPercent

        val pmLast = pumpMachine.trades.lastOrNull()
        val pm2Last = pumpMachine2.trades.lastOrNull()
        val fusionLast = fusion.trades.lastOrNull()
        pairSummary.text = buildString {
            append("ОБЩИЙ ВХОД: Shared Fusion Entry Engine • один 15-секундный снимок\n")
            append(String.format(Locale.GERMANY, "FUSION   €%.2f  %+.2f%%  • %s", fusionValue, fusionTotal, if (fusion.inPosition) "В PUMP" else "В EUR"))
            append("\n")
            append(String.format(Locale.GERMANY, "PUMP 3%%  €%.2f  %+.2f%%  • %s", pumpValue, pumpTotal, if (pumpMachine.inPosition) "В PUMP" else "В EUR"))
            append("\n")
            append(String.format(Locale.GERMANY, "PUMP 2%%  €%.2f  %+.2f%%  • %s", pump2Value, pump2Total, if (pumpMachine2.inPosition) "В PUMP" else "В EUR"))
            append("\n")
            append("Последнее: FUSION ${lastTradeLabel(fusionLast)} • PM3 ${lastTradeLabel(pmLast)} • PM2 ${lastTradeLabel(pm2Last)}")
        }

        summary.text = buildString {
            append(String.format(Locale.GERMANY, "PM3  €%.2f  %+.2f%%", pumpValue, pumpTotal))
            if (pumpMachine.inPosition) append(String.format(Locale.GERMANY, " • сделка %+.2f%% NET", pumpTradeNet))
            append("\n")
            append(String.format(Locale.GERMANY, "PM2  €%.2f  %+.2f%%", pump2Value, pump2Total))
            if (pumpMachine2.inPosition) append(String.format(Locale.GERMANY, " • сделка %+.2f%% NET", pump2TradeNet))
            append("\nPM3: TP +3,00% / SL −1,30% NET • PM2: TP +2,00% / SL −1,10% NET")
        }
        summary.setTextColor(Color.parseColor(if (pump2Total >= 0.0) "#7EE787" else "#FF7B72"))

        status.text = "ВХОДНОЙ МОЗГ ОДИН: Shared Fusion Entry. После собственного выхода cooldown независимый.\n" +
            "PM3: ${PumpMachineStore.lastStatus(this)}\nPM2: ${PumpMachine2Store.lastStatus(this)}"

        data class PairEvent(val source: String, val trade: FusionSimTrade)
        val events = buildList {
            pumpMachine.trades.takeLast(60).forEach { add(PairEvent("PM3", it)) }
            pumpMachine2.trades.takeLast(60).forEach { add(PairEvent("PM2", it)) }
            fusion.trades.takeLast(60).forEach { add(PairEvent("FUSION", it)) }
        }.sortedByDescending { it.trade.time }.take(120)

        trades.text = if (events.isEmpty()) {
            "Сделок пока нет. Fusion, PM3 и PM2 получают один общий входной снимок рынка, но ведут позиции и выходы независимо."
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
