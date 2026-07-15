package com.example.pumppaperbot

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ChartDetailActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()
    private val executor = Executors.newFixedThreadPool(4)

    private lateinit var chart: StrategyChartView
    private lateinit var status: TextView
    private lateinit var range: TextView
    private lateinit var seek: SeekBar
    private lateinit var rotateButton: Button
    private var adjustingSeek = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
        loadSixMonths()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        rotateButton.text = if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            "ВЕРТИКАЛЬНО"
        } else {
            "ПОВЕРНУТЬ БОКОМ"
        }
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }

        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        nav.addView(button("← НАЗАД", "#30363D").apply { setOnClickListener { finish() } }, LinearLayout.LayoutParams(0, dp(46), 1f))
        rotateButton = button("ПОВЕРНУТЬ БОКОМ", "#1F6FEB").apply { setOnClickListener { rotateChart() } }
        nav.addView(rotateButton, LinearLayout.LayoutParams(0, dp(46), 1f).apply { leftMargin = dp(8) })
        root.addView(nav)

        val aggressive = PumpBotEngine.isAggressive(this)
        val profile = if (aggressive) "Агрессивный: зелёные и красные входы" else "Осторожный: только зелёные входы"
        status = label("Загружаю историю PUMP/EUR за 6 месяцев…", 14, "#F0F6FC", true)
        root.addView(status)
        root.addView(label(profile, 13, if (aggressive) "#FF7B72" else "#7EE787", true))
        root.addView(label("● В — вход: зелёный осторожный, красный после шока   ▲ П — продажа", 12, "#C9D1D9", false))

        chart = StrategyChartView(this)
        root.addView(chart, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(4) })

        range = label("Период на экране: —", 13, "#C9D1D9", true)
        root.addView(range)
        seek = SeekBar(this).apply {
            isEnabled = false
            max = 1
            progress = 1
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser && !adjustingSeek) chart.setHistoryOffsetBars(chart.maxHistoryOffset() - progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        root.addView(seek, LinearLayout.LayoutParams(-1, dp(42)))
        root.addView(label("Слева — 6 месяцев назад, справа — последние свечи. График можно также тянуть пальцем.", 12, "#8B949E", false))

        chart.setOnHistoryWindowChanged { offset, start, end ->
            val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
            range.text = "На экране: ${format.format(Date(start))} — ${format.format(Date(end))}"
            if (seek.isEnabled) {
                adjustingSeek = true
                seek.progress = (chart.maxHistoryOffset() - offset).coerceIn(0, seek.max)
                adjustingSeek = false
            }
        }
        setContentView(root)
    }

    private fun rotateChart() {
        requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    private fun loadSixMonths() {
        val startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(183)
        val warmupStart = startTime - TimeUnit.DAYS.toMillis(14)
        val endTime = System.currentTimeMillis()
        val aggressive = PumpBotEngine.isAggressive(this)

        val pumpFuture = executor.submit<List<PumpCandle>> { fetchCandles(PumpBotEngine.pumpSymbol, warmupStart, endTime) }
        val eurFuture = executor.submit<List<PumpCandle>> { fetchCandles(PumpBotEngine.eurSymbol, warmupStart, endTime) }
        val btcFuture = executor.submit<List<PumpCandle>> { fetchCandles(PumpBotEngine.btcSymbol, warmupStart, endTime) }
        val fundingFuture = executor.submit<List<FundingPoint>> { fetchFunding(warmupStart, endTime) }

        executor.execute {
            try {
                val allCandles = StrategyV2.synthesizeEur(pumpFuture.get(), eurFuture.get())
                val result = StrategyV2.backtest(allCandles, btcFuture.get(), fundingFuture.get(), startTime, aggressive)
                val live = PumpBotEngine.snapshot(this)
                val closes = allCandles.map { it.close }
                val fast = ema(closes, PumpBotEngine.emaFastPeriod)
                val slow = ema(closes, PumpBotEngine.emaSlowPeriod)
                val first = allCandles.indexOfFirst { it.closeTime >= startTime }.let { if (it < 0) 0 else it }
                val displayCandles = allCandles.drop(first)
                val bundle = ChartBundle(
                    candles = displayCandles,
                    fast = fast.drop(first),
                    slow = slow.drop(first),
                    trades = result.trades,
                    subtitle = "6 месяцев • 30 минут • EMA50/EMA200",
                    readinessScore = live.readinessScore,
                    trendReadiness = live.trendReadiness,
                    shockReadiness = live.shockReadiness,
                    aggressive = aggressive
                )
                runOnUiThread {
                    status.text = "PUMP/EUR • ${displayCandles.size} закрытых свечей • ${result.roundTrips} исторических сделок"
                    chart.setData("PUMP/EUR — ИСТОРИЯ СИГНАЛОВ", bundle)
                    seek.max = chart.maxHistoryOffset().coerceAtLeast(1)
                    seek.progress = seek.max
                    seek.isEnabled = chart.maxHistoryOffset() > 0
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Не удалось загрузить историю: ${e.message ?: "ошибка сети"}"
                    status.setTextColor(Color.parseColor("#FF7B72"))
                }
            }
        }
    }

    private fun fetchCandles(symbol: String, start: Long, end: Long): List<PumpCandle> {
        val all = ArrayList<PumpCandle>()
        var cursor = start
        while (cursor < end && !Thread.currentThread().isInterrupted) {
            val body = request(PumpBotEngine.historicalKlineUrl(symbol, "30m", cursor, end))
            val batch = PumpBotEngine.parseCandles(body)
            if (batch.isEmpty()) break
            all.addAll(batch)
            val next = batch.last().closeTime + 1L
            if (next <= cursor || batch.size < 1000) break
            cursor = next
        }
        return all.distinctBy { it.closeTime }.sortedBy { it.closeTime }
    }

    private fun fetchFunding(start: Long, end: Long): List<FundingPoint> {
        val all = ArrayList<FundingPoint>()
        var cursor = start
        while (cursor < end && !Thread.currentThread().isInterrupted) {
            val batch = PumpBotEngine.parseFunding(request(PumpBotEngine.fundingUrl(PumpBotEngine.pumpSymbol, cursor, end)))
            if (batch.isEmpty()) break
            all.addAll(batch)
            val next = batch.last().time + 1L
            if (next <= cursor || batch.size < 1000) break
            cursor = next
        }
        return all.distinctBy { it.time }.sortedBy { it.time }
    }

    private fun request(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "PumpSignalAndroid/${PumpBotEngine.appVersionName}")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
    }

    private fun ema(values: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(values.size) { null }
        if (values.size < period) return result
        var previous = values.take(period).average()
        result[period - 1] = previous
        val multiplier = 2.0 / (period + 1.0)
        for (i in period until values.size) {
            previous = values[i] * multiplier + previous * (1.0 - multiplier)
            result[i] = previous
        }
        return result
    }

    private fun button(text: String, color: String) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor(color))
        textSize = 12f
        isAllCaps = false
        setPadding(dp(4), 0, dp(4), 0)
    }

    private fun label(text: String, size: Int, color: String, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.parseColor(color))
        setPadding(0, dp(3), 0, dp(3))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
