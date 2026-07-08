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

    private var startTime: Long = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(180)
    private var selectedCoinIndex = 0
    private var selectedRisk = 35.0
    private val coinButtons = ArrayList<Button>()
    private lateinit var risk30Button: Button
    private lateinit var risk35Button: Button
    private lateinit var status: TextView
    private lateinit var resultBox: LinearLayout
    private lateinit var dateButton: Button

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
        root.addView(label("ПРОВЕРКА НАЗАД", 24, "#F0F6FC", true))
        root.addView(label("Выберите PUMP или одну из популярных монет, риск RSI 30/35 и дату старта.", 13, "#8B949E", false))

        root.addView(label("Монета", 16, "#F0F6FC", true))
        addCoinGrid(root)

        root.addView(label("Риск стратегии", 16, "#F0F6FC", true))
        val riskRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        risk30Button = button("RSI 30\nОсторожно", "#30363D")
        risk35Button = button("RSI 35\nАктивно", "#238636")
        risk30Button.setOnClickListener {
            selectedRisk = 30.0
            updateRiskButtons()
        }
        risk35Button.setOnClickListener {
            selectedRisk = 35.0
            updateRiskButtons()
        }
        riskRow.addView(risk30Button, LinearLayout.LayoutParams(0, dp(54), 1f))
        riskRow.addView(risk35Button, LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(8) })
        root.addView(riskRow)

        dateButton = button("СТАРТ: ${PumpBotEngine.formatDate(startTime)}", "#30363D")
        dateButton.setOnClickListener { pickDate() }
        root.addView(dateButton, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(8) })

        val run = button("ЗАПУСТИТЬ ПРОВЕРКУ", "#238636")
        run.setOnClickListener { runBacktest() }
        root.addView(run, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(6) })

        status = label("Готово.", 13, "#8B949E", false)
        root.addView(status)

        val scroll = ScrollView(this)
        resultBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(24))
        }
        scroll.addView(resultBox)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        updateCoinButtons()
        updateRiskButtons()
    }

    private fun addCoinGrid(root: LinearLayout) {
        PumpBotEngine.coinOptions.chunked(2).forEachIndexed { rowIndex, rowCoins ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowCoins.forEach { coin ->
                val globalIndex = PumpBotEngine.coinOptions.indexOf(coin)
                val coinButton = button("${coin.name}\n${coin.symbol}", "#30363D")
                coinButton.setOnClickListener {
                    selectedCoinIndex = globalIndex
                    updateCoinButtons()
                }
                coinButtons.add(coinButton)
                val params = LinearLayout.LayoutParams(0, dp(54), 1f)
                if (row.childCount > 0) params.leftMargin = dp(8)
                row.addView(coinButton, params)
            }
            if (rowCoins.size == 1) {
                row.addView(View(this), LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(8) })
            }
            val params = LinearLayout.LayoutParams(-1, dp(54))
            if (rowIndex > 0) params.topMargin = dp(5)
            root.addView(row, params)
        }
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
                dateButton.text = "СТАРТ: ${PumpBotEngine.formatDate(startTime)}"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun runBacktest() {
        val selected = PumpBotEngine.coinOptions[selectedCoinIndex]
        status.text = "Загружаю ${selected.symbol}, свечи 30 минут..."
        resultBox.removeAllViews()
        thread {
            try {
                val warmupStart = startTime - TimeUnit.DAYS.toMillis(14)
                val candles = fetchCandles(selected.symbol, warmupStart, System.currentTimeMillis())
                val result = PumpBotEngine.backtest(selected, candles, startTime, selectedRisk)
                runOnUiThread {
                    val note = if (result.firstCandleTime > 0L && startTime < result.firstCandleTime) {
                        " Первые доступные данные: ${PumpBotEngine.formatDate(result.firstCandleTime)}."
                    } else {
                        ""
                    }
                    status.text = "Готово. Свечей: ${candles.size}.$note"
                    showResult(result)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Проверка не удалась: ${e.message}"
                }
            }
        }
    }

    private fun fetchCandles(symbol: String, start: Long, end: Long): List<PumpCandle> {
        val all = ArrayList<PumpCandle>()
        var cursor = start
        while (cursor < end) {
            val request = Request.Builder()
                .url(PumpBotEngine.historicalKlineUrl(symbol, "30m", cursor, end))
                .header("Accept", "application/json")
                .header("User-Agent", "PumpRsiBotAndroid/${PumpBotEngine.appVersionName}")
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

    private fun showResult(result: BacktestResult) {
        resultBox.removeAllViews()
        resultBox.addView(summary(result))
        if (result.trades.isEmpty()) {
            resultBox.addView(label("За выбранный период сделок не было.", 14, "#8B949E", false))
        } else {
            result.trades.reversed().forEach { trade ->
                resultBox.addView(greenDivider())
                resultBox.addView(tradeRow(trade))
            }
        }
    }

    private fun summary(result: BacktestResult): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.parseColor("#161B22"))
            addView(label("${result.assetName} ${result.symbol}", 18, "#F0F6FC", true))
            addView(label("Риск: RSI <= ${result.buyRsi.toInt()}", 14, "#F0F6FC", true))
            addView(label("Старт: ${PumpBotEngine.formatDate(startTime)}", 13, "#8B949E", false))
            addView(label(String.format(Locale.US, "Вложили: %.2f USDT", PumpBotEngine.startBalance), 14, "#C9D1D9", false))
            addView(label(String.format(Locale.US, "Сейчас: %.2f USDT | результат %+.2f USDT (%+.2f%%)", result.equity, result.profit, result.profitPercent), 18, if (result.profit >= 0) "#32C789" else "#FF4D6D", true))
            addView(label(String.format(Locale.US, "Сделок: %d | комиссии: %.2f USDT | комиссия за операцию: %.2f%%", result.trades.size, result.totalFees, PumpBotEngine.feeRate * 100.0), 14, "#F0F6FC", false))
            addView(label("Данные до: ${PumpBotEngine.formatDate(result.lastCandleTime)}", 13, "#8B949E", false))
        }
    }

    private fun tradeRow(trade: TradeEvent): TextView {
        val color = if (trade.action == "BUY") "#32C789" else "#FF4D6D"
        val action = if (trade.action == "BUY") "ПОКУПКА" else "ПРОДАЖА"
        val pnl = if (trade.action == "SELL") String.format(Locale.US, " | результат %+.2f", trade.pnl) else ""
        return label(
            String.format(Locale.US, "%s %s\nцена %.8f | сумма %.2f | комиссия %.2f | баланс %.2f%s\nмонет %.2f", PumpBotEngine.formatTime(trade.time), action, trade.price, trade.amount, trade.fee, trade.equity, pnl, trade.coins),
            13,
            color,
            false
        ).apply {
            setBackgroundColor(Color.parseColor("#101820"))
            setPadding(dp(10), dp(9), dp(10), dp(9))
        }
    }

    private fun updateCoinButtons() {
        coinButtons.forEachIndexed { index, button ->
            button.setBackgroundColor(Color.parseColor(if (index == selectedCoinIndex) "#1F6FEB" else "#30363D"))
        }
    }

    private fun updateRiskButtons() {
        risk30Button.setBackgroundColor(Color.parseColor(if (selectedRisk <= 30.0) "#238636" else "#30363D"))
        risk35Button.setBackgroundColor(Color.parseColor(if (selectedRisk > 30.0) "#238636" else "#30363D"))
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
            textSize = 12f
            isAllCaps = false
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
