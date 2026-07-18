package com.example.pumppaperbot

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.Executors
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
        val profile = if (aggressive) "Активный" else "Осторожный"
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
        status.text = "Загружаю PUMP, BTC, ETH, SOL, spot/futures и funding..."
        thread {
            val pool = Executors.newFixedThreadPool(8)
            try {
                val warmupStart = startTime - TimeUnit.DAYS.toMillis(45)
                val end = System.currentTimeMillis()
                val pump = pool.submit<List<PumpCandle>> { fetchCandles(PumpBotEngine.pumpSymbol, warmupStart, end) }
                val eur = pool.submit<List<PumpCandle>> { fetchCandles(PumpBotEngine.eurSymbol, warmupStart, end) }
                val btc = pool.submit<List<PumpCandle>> { fetchCandles(PumpBotEngine.btcSymbol, warmupStart, end) }
                val eth = pool.submit<List<PumpCandle>> { fetchCandles(PumpBotEngine.ethSymbol, warmupStart, end) }
                val sol = pool.submit<List<PumpCandle>> { fetchCandles(PumpBotEngine.solSymbol, warmupStart, end) }
                val futures = pool.submit<List<PumpCandle>> { fetchDerivativeCandles(warmupStart, end, false) }
                val premium = pool.submit<List<PumpCandle>> { fetchDerivativeCandles(warmupStart, end, true) }
                val funding = pool.submit<List<FundingPoint>> { fetchFunding(warmupStart, end) }
                val candles = StrategyV2.synthesizeEur(pump.get(), eur.get())
                val result = StrategyV2.backtest(
                    candles, btc.get(), funding.get(), startTime, aggressive,
                    eth.get(), sol.get(), futures.get(), premium.get()
                )
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
            } finally {
                pool.shutdownNow()
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

    private fun fetchDerivativeCandles(start: Long, end: Long, premium: Boolean): List<PumpCandle> {
        val all = ArrayList<PumpCandle>()
        var cursor = start
        while (cursor < end) {
            val url = if (premium) {
                PumpBotEngine.historicalPremiumKlineUrl(PumpBotEngine.pumpSymbol, "30m", cursor, end)
            } else {
                PumpBotEngine.historicalFuturesKlineUrl(PumpBotEngine.pumpSymbol, "30m", cursor, end)
            }
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "PumpSignalAndroid/${PumpBotEngine.appVersionName}")
                .build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Futures HTTP ${response.code}")
                response.body?.string().orEmpty()
            }
            val batch = PumpBotEngine.parseCandles(body)
            if (batch.isEmpty()) break
            all.addAll(batch)
            val next = batch.last().closeTime + 1L
            if (next <= cursor || batch.size < 1000) break
            cursor = next
        }
        return all.distinctBy { it.closeTime }.sortedBy { it.closeTime }
    }

    private fun showResult(result: BacktestResult) {
        resultBox.removeAllViews()
        resultBox.addView(summary(result))
        val connections = completedTradeConnections(result.trades)
        if (connections.isEmpty()) {
            resultBox.addView(label("За выбранный период сделок не было.", 15, "#8B949E", false))
        } else {
            resultBox.addView(label("СДЕЛКИ: ОДНА КАРТОЧКА = ВХОД + ВЫХОД", 15, "#F0F6FC", true))
            connections.asReversed().forEachIndexed { reversedIndex, connection ->
                val number = connections.size - reversedIndex
                resultBox.addView(tradeCard(connection, number, connections.size))
            }

            val technicalLog = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                result.trades.reversed().forEach { trade -> addView(tradeRow(trade)) }
            }
            resultBox.addView(button("ПОКАЗАТЬ ТЕХНИЧЕСКИЙ ЖУРНАЛ", "#30363D").apply {
                setOnClickListener {
                    technicalLog.visibility = if (technicalLog.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    text = if (technicalLog.visibility == View.VISIBLE) "СКРЫТЬ ТЕХНИЧЕСКИЙ ЖУРНАЛ" else "ПОКАЗАТЬ ТЕХНИЧЕСКИЙ ЖУРНАЛ"
                }
            }, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(10) })
            resultBox.addView(technicalLog)
        }
    }

    private fun summary(result: BacktestResult): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = roundedBackground("#161B22", 10)

            addView(label("${result.symbol} • ${if (aggressive) "АКТИВНЫЙ" else "ОСТОРОЖНЫЙ"}", 16, "#C9D1D9", true).apply { gravity = Gravity.CENTER })
            addView(label(String.format(Locale.US, "%+.2f%%", result.profitPercent), 36, if (result.profit >= 0) "#32C789" else "#FF4D6D", true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(2), 0, 0)
            })
            addView(label(String.format(Locale.US, "%+.2f EUR", result.profit), 20, if (result.profit >= 0) "#32C789" else "#FF4D6D", true).apply { gravity = Gravity.CENTER })
            addView(label(String.format(Locale.US, "1000,00 EUR  →  %.2f EUR", result.equity), 18, "#F0F6FC", true).apply {
                gravity = Gravity.CENTER
                background = roundedBackground("#0D1117", 8)
                setPadding(dp(8), dp(8), dp(8), dp(8))
            })

            val metrics = LinearLayout(this@BacktestResultActivity).apply { orientation = LinearLayout.HORIZONTAL }
            metrics.addView(metricCard("${result.roundTrips}", "СДЕЛОК", "#1F6FEB"), LinearLayout.LayoutParams(0, dp(72), 1f))
            metrics.addView(metricCard(String.format(Locale.US, "%.0f%%", result.winRatePercent), "ПРИБЫЛЬНЫХ", "#238636"), LinearLayout.LayoutParams(0, dp(72), 1f).apply { leftMargin = dp(6) })
            metrics.addView(metricCard(String.format(Locale.US, "%.2f%%", result.maxDrawdownPercent), "МАКС. ПАДЕНИЕ", "#B62324"), LinearLayout.LayoutParams(0, dp(72), 1f).apply { leftMargin = dp(6) })
            addView(metrics, LinearLayout.LayoutParams(-1, dp(72)).apply { topMargin = dp(10) })

            addView(label("✓ РУЧНОЕ ИСПОЛНЕНИЕ", 14, "#58A6FF", true).apply {
                text = "✓ РУЧНОЕ ИСПОЛНЕНИЕ\nСигнал после закрытия свечи → цена следующего открытия"
                background = roundedBackground("#10243D", 8)
                setPadding(dp(10), dp(8), dp(10), dp(8))
            })
            addView(label(String.format(Locale.US, "Полных стопов: %d   •   комиссии: %.2f EUR", result.stopCount, result.totalFees), 14, "#F0F6FC", true))
            if (result.maxDrawdownPercent < -5.0) {
                addView(label(String.format(Locale.US, "⚠ Внутри позиции цена падала до %.2f%%. Чтобы реально ограничить потерю около 5%%, защитный стоп −4,4%% нужно ставить на бирже сразу после покупки.", result.maxDrawdownPercent), 14, "#FF7B72", true).apply {
                    background = roundedBackground("#2B1418", 8, "#B62324")
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                })
            }
            addView(label("Защита от вершины отменила входов: ${result.blockedOverheatCount}", 14, "#F0B72F", true))
            addView(button("ОТКРЫТЬ ГРАФИК СДЕЛОК", "#1F6FEB").apply {
                setOnClickListener {
                    PumpBotEngine.setAggressive(this@BacktestResultActivity, aggressive)
                    startActivity(Intent(this@BacktestResultActivity, ChartDetailActivity::class.java))
                }
            }, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(6) })
            addView(label("Период: ${PumpBotEngine.formatDate(startTime)} — ${PumpBotEngine.formatDate(result.lastCandleTime)}", 13, "#8B949E", false))
            addView(label("Исторический результат не гарантирует будущую прибыль.", 13, "#F0B72F", true))
        }
    }

    private fun metricCard(value: String, caption: String, accent: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = roundedBackground("#0D1117", 8, accent)
            addView(label(value, 23, "#F0F6FC", true).apply { gravity = Gravity.CENTER })
            addView(label(caption, 13, "#8B949E", true).apply { gravity = Gravity.CENTER })
        }
    }

    private fun tradeCard(connection: TradeConnection, number: Int, total: Int): View {
        val positive = connection.profitEur >= 0.0
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(9), dp(10), dp(9))
            background = roundedBackground(if (positive) "#10251B" else "#2B1418", 9, if (positive) "#238636" else "#B62324")
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }

            val header = LinearLayout(this@BacktestResultActivity).apply { orientation = LinearLayout.HORIZONTAL }
            header.addView(label("СДЕЛКА $number ИЗ $total", 15, "#F0F6FC", true), LinearLayout.LayoutParams(0, -2, 1f))
            header.addView(label(String.format(Locale.US, "%+.2f%%", connection.profitPercent), 21, if (positive) "#32C789" else "#FF4D6D", true))
            addView(header)
            addView(label(String.format(Locale.US, "%+.2f EUR  •  деньги работали %s", connection.profitEur, formatDuration(connection.durationMillis)), 15, if (positive) "#32C789" else "#FF4D6D", true))
            addView(label(String.format(Locale.US, "ВХОД   %s   €%.8f", PumpBotEngine.formatTime(connection.entry.time), connection.entry.price), 14, "#58A6FF", true))
            addView(label(String.format(Locale.US, "ВЫХОД  %s   €%.8f", PumpBotEngine.formatTime(connection.exit.time), connection.exit.price), 14, "#D2A8FF", true))
            addView(button("ПОЧЕМУ ВОШЛИ / ПОЧЕМУ ВЫШЛИ", "#30363D").apply {
                setOnClickListener { showTradeCriteria(connection, number) }
            }, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(4) })
        }
    }

    private fun showTradeCriteria(connection: TradeConnection, number: Int) {
        val partial = if (connection.partialExits.isEmpty()) {
            "Частичной продажи не было."
        } else {
            connection.partialExits.joinToString("\n") { "• ${PumpBotEngine.formatTime(it.time)} — ${it.reason}" }
        }
        val message = """
            ПОЧЕМУ ВОШЛИ
            ✓ ${connection.entry.reason}
            ✓ Решение принято только по закрытой 30-минутной свече.
            ✓ Покупка — на следующем открытии.

            ПОЧЕМУ ВЫШЛИ
            ✓ ${connection.exit.reason}

            ЧАСТИЧНАЯ ПРОДАЖА
            $partial

            ИТОГ: ${String.format(Locale.US, "%+.2f%% / %+.2f EUR", connection.profitPercent, connection.profitEur)}
        """.trimIndent()
        AlertDialog.Builder(this)
            .setTitle("Критерии сделки $number")
            .setMessage(message)
            .setPositiveButton("ПОНЯТНО", null)
            .show()
    }

    private fun tradeRow(trade: TradeEvent): TextView {
        val color = if (trade.action == "BUY") "#32C789" else "#FF4D6D"
        val action = when (trade.action) {
            "BUY" -> "ПОКУПКА"
            "SELL_HALF" -> if (trade.reason.startsWith("40%")) "ПРОДАЖА 40%" else "ПРОДАЖА 50%"
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

    private fun button(text: String, color: String): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(color))
            textSize = 13f
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

    private fun roundedBackground(color: String, radiusDp: Int, stroke: String? = null) = GradientDrawable().apply {
        setColor(Color.parseColor(color))
        cornerRadius = dp(radiusDp).toFloat()
        if (stroke != null) setStroke(dp(1), Color.parseColor(stroke))
    }

    private fun formatDuration(durationMillis: Long): String {
        val totalMinutes = durationMillis / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return if (hours >= 24L) "${hours / 24L} д ${hours % 24L} ч" else "${hours} ч ${minutes} мин"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val extraStartTime = "start_time"
        const val extraAggressive = "aggressive"
    }
}
