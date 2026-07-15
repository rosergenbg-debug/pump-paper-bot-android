package com.example.pumppaperbot

import android.content.Intent
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
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class BacktestResultActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private lateinit var status: TextView
    private lateinit var resultBox: LinearLayout
    private var startTime = 0L
    private var aggressive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startTime = intent.getLongExtra(extraStartTime, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(365))
        aggressive = intent.getBooleanExtra(extraAggressive, false)
        render()
        runBacktest()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }

        val navRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val backButton = button("< К выбору", "#30363D")
        backButton.setOnClickListener { finish() }
        val mainButton = button("<< Главное", "#30363D")
        mainButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
        navRow.addView(backButton, LinearLayout.LayoutParams(0, dp(44), 1f))
        navRow.addView(mainButton, LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(8) })
        root.addView(navRow)

        root.addView(label("РЕЗУЛЬТАТ ПРОВЕРКИ", 23, "#F0F6FC", true))
        val profile = if (aggressive) "Агрессивный: 2 входа" else "Осторожный: 1 вход"
        root.addView(label("PUMP/EUR | $profile | старт ${PumpBotEngine.formatDate(startTime)}", 13, "#8B949E", false))

        status = label("Загружаю свечи 30 минут...", 14, "#8B949E", false)
        root.addView(status)

        val scroll = ScrollView(this).apply { isFillViewport = true }
        resultBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(24))
        }
        scroll.addView(resultBox)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun runBacktest() {
        status.text = "Загружаю PUMP, EUR, BTC и funding..."
        thread {
            try {
                val warmupStart = startTime - TimeUnit.DAYS.toMillis(14)
                val end = System.currentTimeMillis()
                val pump = fetchCandles(PumpBotEngine.pumpSymbol, warmupStart, end)
                val eur = fetchCandles(PumpBotEngine.eurSymbol, warmupStart, end)
                val btc = fetchCandles(PumpBotEngine.btcSymbol, warmupStart, end)
                val funding = fetchFunding(warmupStart, end)
                val candles = StrategyV2.synthesizeEur(pump, eur)
                val result = StrategyV2.backtest(candles, btc, funding, startTime, aggressive)
                runOnUiThread {
                    val note = if (result.firstCandleTime > 0L && startTime < result.firstCandleTime) {
                        " Первые данные: ${PumpBotEngine.formatDate(result.firstCandleTime)}."
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

    private fun fetchFunding(start: Long, end: Long): List<FundingPoint> {
        val all = ArrayList<FundingPoint>()
        var cursor = start
        while (cursor < end) {
            val request = Request.Builder()
                .url(PumpBotEngine.fundingUrl(PumpBotEngine.pumpSymbol, cursor, end))
                .header("Accept", "application/json")
                .header("User-Agent", "PumpSignalAndroid/${PumpBotEngine.appVersionName}")
                .build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Funding HTTP ${response.code}")
                response.body?.string().orEmpty()
            }
            val batch = PumpBotEngine.parseFunding(body)
            if (batch.isEmpty()) break
            all.addAll(batch)
            val next = batch.last().time + 1L
            if (next <= cursor || batch.size < 1000) break
            cursor = next
        }
        return all.distinctBy { it.time }.sortedBy { it.time }
    }

    private fun showResult(result: BacktestResult) {
        resultBox.removeAllViews()
        resultBox.addView(summary(result))
        if (result.trades.isEmpty()) {
            resultBox.addView(label("За выбранный период сделок не было.", 15, "#8B949E", false))
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
            addView(label("${result.assetName} ${result.symbol}", 19, "#F0F6FC", true))
            addView(label(result.strategyName, 14, "#F0F6FC", true))
            addView(label("Старт: ${PumpBotEngine.formatDate(startTime)}", 13, "#8B949E", false))
            addView(label(String.format(Locale.US, "Вложили: %.2f EUR", PumpBotEngine.startBalance), 15, "#C9D1D9", false))
            addView(label(String.format(Locale.US, "Сейчас: %.2f EUR | результат %+.2f EUR (%+.2f%%)", result.equity, result.profit, result.profitPercent), 19, if (result.profit >= 0) "#32C789" else "#FF4D6D", true))
            addView(label(String.format(Locale.US, "Сделок: %d | прибыльных %.1f%% | полных стопов: %d", result.roundTrips, result.winRatePercent, result.stopCount), 14, "#F0F6FC", false))
            addView(label(String.format(Locale.US, "Закрытая просадка: %.2f%% | комиссии: %.2f EUR | 0,15%% за операцию", result.maxDrawdownPercent, result.totalFees), 14, "#F0F6FC", false))
            addView(label("Результат исторический и не гарантирует будущую прибыль.", 13, "#F0B72F", true))
            addView(label("Данные до: ${PumpBotEngine.formatDate(result.lastCandleTime)}", 13, "#8B949E", false))
        }
    }

    private fun tradeRow(trade: TradeEvent): TextView {
        val color = if (trade.action == "BUY") "#32C789" else "#FF4D6D"
        val action = when (trade.action) {
            "BUY" -> "ПОКУПКА"
            "SELL_HALF" -> "ПРОДАЖА 50%"
            else -> "ПРОДАЖА"
        }
        val pnl = if (trade.action != "BUY") String.format(Locale.US, " | результат %+.2f", trade.pnl) else ""
        return label(
            String.format(Locale.US, "%s %s — %s\nцена %.8f EUR | сумма %.2f | комиссия %.2f | баланс %.2f%s\nмонет %.2f", PumpBotEngine.formatTime(trade.time), action, trade.reason, trade.price, trade.amount, trade.fee, trade.equity, pnl, trade.coins),
            14,
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

    companion object {
        const val extraStartTime = "start_time"
        const val extraAggressive = "aggressive"
    }
}
