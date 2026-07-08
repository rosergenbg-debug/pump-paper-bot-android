package com.example.pumppaperbot

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class BacktestActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var startTime: Long = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)
    private var strategyId = "primary"
    private lateinit var status: TextView
    private lateinit var resultBox: LinearLayout
    private lateinit var dateButton: Button
    private lateinit var primaryButton: Button
    private lateinit var experimentButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        root.addView(label("BACKTEST", 24, "#F0F6FC", true))
        root.addView(label("Choose a historical start date and strategy.", 13, "#8B949E", false))

        dateButton = button("START: ${PumpBotEngine.formatTime(startTime)}", "#30363D")
        dateButton.setOnClickListener { pickDate() }
        root.addView(dateButton)

        val strategyRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        primaryButton = button("4H PRIMARY", "#1F6FEB")
        experimentButton = button("2H EXPERIMENT", "#30363D")
        primaryButton.setOnClickListener {
            strategyId = "primary"
            updateStrategyButtons()
        }
        experimentButton.setOnClickListener {
            strategyId = "experiment"
            updateStrategyButtons()
        }
        strategyRow.addView(primaryButton, LinearLayout.LayoutParams(0, dp(48), 1f))
        strategyRow.addView(experimentButton, LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin = dp(8) })
        root.addView(strategyRow)

        val run = button("RUN BACKTEST", "#238636")
        run.setOnClickListener { runBacktest() }
        root.addView(run)

        status = label("Ready.", 13, "#8B949E", false)
        root.addView(status)

        val scroll = ScrollView(this)
        resultBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(24))
        }
        scroll.addView(resultBox)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun pickDate() {
        val cal = Calendar.getInstance().apply { timeInMillis = startTime }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val picked = Calendar.getInstance()
                picked.set(year, month, day, 0, 0, 0)
                picked.set(Calendar.MILLISECOND, 0)
                startTime = picked.timeInMillis
                dateButton.text = "START: ${PumpBotEngine.formatTime(startTime)}"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateStrategyButtons() {
        primaryButton.setBackgroundColor(Color.parseColor(if (strategyId == "primary") "#1F6FEB" else "#30363D"))
        experimentButton.setBackgroundColor(Color.parseColor(if (strategyId == "experiment") "#1F6FEB" else "#30363D"))
    }

    private fun runBacktest() {
        status.text = "Loading historical candles..."
        resultBox.removeAllViews()
        thread {
            try {
                val warmupStart = startTime - TimeUnit.DAYS.toMillis(35)
                val now = System.currentTimeMillis()
                val candles4h = fetchCandles("4h", warmupStart, now)
                val candles2h = fetchCandles("2h", warmupStart, now)
                val firstData = listOfNotNull(candles4h.firstOrNull()?.openTime, candles2h.firstOrNull()?.openTime).minOrNull()
                val result = PumpBotEngine.backtest(strategyId, candles4h, candles2h, startTime)
                runOnUiThread {
                    val availability = if (firstData != null && startTime < firstData) {
                        " Requested date is before PUMP data; first available candle: ${PumpBotEngine.formatTime(firstData)}."
                    } else {
                        ""
                    }
                    status.text = "Done. Candles: 4H ${candles4h.size}, 2H ${candles2h.size}.$availability"
                    showResult(result)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Backtest failed: ${e.message}"
                }
            }
        }
    }

    private fun fetchCandles(interval: String, start: Long, end: Long): List<PumpCandle> {
        val all = ArrayList<PumpCandle>()
        var cursor = start
        while (cursor < end) {
            val url = "https://data-api.binance.vision/api/v3/klines?symbol=${PumpBotEngine.symbol}&interval=$interval&startTime=$cursor&endTime=$end&limit=1000"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "PumpPaperBotAndroid/0.3")
                .build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                response.body?.string().orEmpty()
            }
            val batch = PumpBotEngine.parseCandles(body)
            if (batch.isEmpty()) break
            all.addAll(batch)
            val next = batch.last().closeTime + 1L
            if (next <= cursor) break
            cursor = next
        }
        return all.distinctBy { it.closeTime }.sortedBy { it.closeTime }
    }

    private fun showResult(result: StrategyResult) {
        resultBox.removeAllViews()
        resultBox.addView(summary(result))
        if (result.state.trades.isEmpty()) {
            resultBox.addView(label("No trades in selected period.", 14, "#8B949E", false))
        } else {
            result.state.trades.reversed().forEach { trade ->
                resultBox.addView(greenDivider())
                resultBox.addView(tradeRow(trade))
            }
        }
    }

    private fun summary(result: StrategyResult): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.parseColor("#161B22"))
            addView(label(result.state.title, 18, "#F0F6FC", true))
            addView(label("Start: ${PumpBotEngine.formatTime(startTime)}", 13, "#8B949E", false))
            addView(label(String.format(Locale.US, "Invested: %.2f USDT", PumpBotEngine.startBalance), 14, "#C9D1D9", false))
            addView(label(String.format(Locale.US, "Now: %.2f USDT | P/L %+.2f USDT (%+.2f%%)", result.equity, result.profit, result.profitPercent), 17, if (result.profit >= 0) "#32C789" else "#FF4D6D", true))
            addView(label(String.format(Locale.US, "Trades: %d | Fees paid: %.2f USDT | Fee per operation: %.2f%%", result.tradeCount, result.totalFees, PumpBotEngine.feeRate * 100.0), 14, "#F0F6FC", false))
        }
    }

    private fun tradeRow(trade: TradeEvent): TextView {
        val color = if (trade.action == "BUY") "#32C789" else "#FF4D6D"
        val pnl = if (trade.action == "SELL") String.format(Locale.US, " | P/L %+.2f", trade.pnl) else ""
        return label(
            String.format(Locale.US, "%s %s\nprice %.8f | amount %.2f | fee %.2f | balance %.2f%s\ncoins %.2f", PumpBotEngine.formatTime(trade.time), trade.action, trade.price, trade.amount, trade.fee, trade.equity, pnl, trade.coins),
            13,
            color,
            false
        ).apply {
            setBackgroundColor(Color.parseColor("#101820"))
            setPadding(dp(10), dp(9), dp(10), dp(9))
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

    private fun button(text: String, color: String): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(color))
            textSize = 13f
        }
    }

    private fun label(text: String, size: Int, color: String, bold: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(Color.parseColor(color))
            setPadding(0, dp(5), 0, dp(5))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
