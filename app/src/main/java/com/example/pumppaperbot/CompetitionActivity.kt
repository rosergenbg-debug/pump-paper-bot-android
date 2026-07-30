package com.example.pumppaperbot

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CompetitionActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val charts = ArrayList<CompetitionChartView>()
    private val executor = Executors.newSingleThreadExecutor()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()
    private var historicalCandles: List<PumpCandle> = emptyList()
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
            setBackgroundColor(Color.parseColor("#0D1117"))
            setPadding(dp(8), dp(6), dp(8), dp(8))
        }
        val back = Button(this).apply {
            text = "←  СРАВНЕНИЕ ТРЁХ"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#30363D"))
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }
        root.addView(back, LinearLayout.LayoutParams(-1, dp(44)))
        repeat(3) {
            val chart = CompetitionChartView(this)
            charts += chart
            root.addView(chart, LinearLayout.LayoutParams(-1, 0, 1f).apply {
                topMargin = dp(5)
            })
        }
        charts.forEach { source ->
            source.setOnOffsetChanged { offset ->
                charts.filterNot { it === source }.forEach { it.setSynchronizedOffset(offset) }
            }
        }
        setContentView(root)
        render()
        loadSixMonths()
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

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun render() {
        val snapshot = PumpBotEngine.snapshot(this)
        val price = snapshot.lastPrice
        val app = AppPaperStore.state(this)
        val gemini = GeminiPaperStore.state(this).portfolio
        val user = UserPaperStore.markToMarket(this, price)
        val candles = if (historicalCandles.isNotEmpty()) {
            (historicalCandles + snapshot.chart.candles)
                .distinctBy { it.closeTime }
                .sortedBy { it.closeTime }
        } else {
            snapshot.chart.candles
        }

        charts[0].setData(
            "GEMINI",
            summary(gemini.value(price), gemini.profitPercent(price), gemini.inPosition),
            candles,
            gemini.trades.map { CompetitionMarker(it.time, it.action, it.price, it.pnlEur) }
        )
        charts[1].setData(
            "APP",
            summary(app.value(price), app.profitPercent(price), app.inPosition),
            candles,
            app.trades.map { CompetitionMarker(it.candleTime, it.action, it.price, it.pnlEur) }
        )
        charts[2].setData(
            "СЕРЖ",
            summary(user.value(price), user.profitPercent(price), user.inPosition),
            candles,
            user.trades.map { CompetitionMarker(it.time, it.action, it.price) }
        )
    }

    private fun summary(value: Double, percent: Double, inPosition: Boolean): String =
        String.format(
            Locale.GERMANY,
            "€%.2f  •  %+.2f%%  •  %s",
            value,
            percent,
            if (inPosition) "В PUMP" else "В ЕВРО"
        )

    private fun loadSixMonths() {
        executor.execute {
            runCatching {
                val end = System.currentTimeMillis()
                val start = end - TimeUnit.DAYS.toMillis(183)
                val pump = fetchCandles(PumpBotEngine.pumpSymbol, start, end)
                val eur = fetchCandles(PumpBotEngine.eurSymbol, start, end)
                StrategyV2.synthesizeEur(pump, eur)
            }.onSuccess {
                historicalCandles = it
                runOnUiThread { render() }
            }
        }
    }

    private fun fetchCandles(symbol: String, start: Long, end: Long): List<PumpCandle> {
        val all = ArrayList<PumpCandle>()
        var cursor = start
        while (cursor < end && !Thread.currentThread().isInterrupted) {
            val json = request(PumpBotEngine.historicalKlineUrl(symbol, "30m", cursor, end))
            val batch = PumpBotEngine.parseCandles(json)
            if (batch.isEmpty()) break
            all += batch
            val next = batch.last().closeTime + 1L
            if (next <= cursor || batch.size < 1000) break
            cursor = next
        }
        return all.distinctBy { it.closeTime }.sortedBy { it.closeTime }
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

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
