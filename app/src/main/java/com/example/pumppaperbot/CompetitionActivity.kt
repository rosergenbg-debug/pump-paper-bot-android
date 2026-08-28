package com.example.pumppaperbot

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
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

private data class CompetitionDataset(
    val title: String,
    val subtitle: String,
    val candles: List<PumpCandle>,
    val markers: List<CompetitionMarker>,
    val feeRate: Double
)

class CompetitionActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val charts = ArrayList<CompetitionChartView>()
    private val datasets = arrayOfNulls<CompetitionDataset>(CompetitionAccountSpec.COUNT)
    private val executor = Executors.newSingleThreadExecutor()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()
    private var historicalCandles: List<PumpCandle> = emptyList()
    private lateinit var archiveStatus: TextView
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
            text = CompetitionAccountSpec.SCREEN_TITLE
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#30363D"))
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }
        root.addView(back, LinearLayout.LayoutParams(-1, dp(44)))
        archiveStatus = TextView(this).apply {
            setTextColor(Color.parseColor("#C9D1D9"))
            setBackgroundColor(Color.parseColor("#161B22"))
            setPadding(dp(8), dp(6), dp(8), dp(6))
            textSize = 11f
        }
        root.addView(archiveStatus, LinearLayout.LayoutParams(-1, dp(64)).apply { topMargin = dp(3) })
        val chartColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        repeat(CompetitionAccountSpec.COUNT) { index ->
            val chart = CompetitionChartView(this).apply {
                contentDescription = "Открыть подробный график ${index + 1}"
                setOnClickListener { showChartDetail(index) }
            }
            charts += chart
            chartColumn.addView(chart, LinearLayout.LayoutParams(-1, dp(230)).apply { topMargin = dp(3) })
        }
        root.addView(ScrollView(this).apply {
            isFillViewport = true
            addView(chartColumn)
        }, LinearLayout.LayoutParams(-1, 0, 1f))
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
        archiveStatus.text = "V6.6 • новые счета стартуют с €1000 и нулевой историей. Старые V6.5 данные не удаляются физически, но больше не участвуют в этой сети.\n" +
            "CORE / BTC GUARD / SOL-BTC SELECT: TP +2,5% • STOP -1,2% • TIME 120m • max2/day"
        val now = System.currentTimeMillis()
        val snapshot = PumpBotEngine.snapshot(this)
        val displayPrice = PaperExecutionPolicy.displayPrice(snapshot, now)
        val market = BitpandaFusionStore.state(this)
        val mark = market.bid.takeIf { market.fresh(now) } ?: displayPrice
        val closedCandles = if (historicalCandles.isNotEmpty()) {
            (historicalCandles + snapshot.chart.candles)
                .distinctBy { it.closeTime }
                .sortedBy { it.closeTime }
        } else snapshot.chart.candles
        val candles = CompetitionChartPresentation.withLiveEdge(closedCandles, displayPrice, now)

        val core = V660CoreStore.state(this)
        setChart(0, CompetitionDataset(
            "AUTO CORE • X BASE",
            summary(core.value(mark), (core.value(mark) / 1000.0 - 1.0) * 100.0, core.inPosition, core.readiness, core.reason),
            candles,
            core.trades.map { CompetitionMarker(it.time, it.action, it.price, it.pnlEur) },
            T32CostPolicyV660.FEE_RATE
        ))

        val btc = V660BtcGuardStore.state(this)
        setChart(1, CompetitionDataset(
            "AUTO BTC GUARD • БЛОК STRONG-UP",
            summary(btc.value(mark), (btc.value(mark) / 1000.0 - 1.0) * 100.0, btc.inPosition, btc.readiness, btc.reason),
            candles,
            btc.trades.map { CompetitionMarker(it.time, it.action, it.price, it.pnlEur) },
            T32CostPolicyV660.FEE_RATE
        ))

        val sol = V660SolSelectStore.state(this)
        setChart(2, CompetitionDataset(
            "AUTO SOL/BTC SELECT • REL6 ≥ +0,40 п.п.",
            summary(sol.value(mark), (sol.value(mark) / 1000.0 - 1.0) * 100.0, sol.inPosition, sol.readiness, sol.reason),
            candles,
            sol.trades.map { CompetitionMarker(it.time, it.action, it.price, it.pnlEur) },
            T32CostPolicyV660.FEE_RATE
        ))

        val human = HumanFactorStore.state(this)
        setChart(3, CompetitionDataset(
            "HUMAN SELECT • РУЧНОЙ ВХОД / AUTO EXIT",
            summary(human.value(mark), (human.value(mark) / 1000.0 - 1.0) * 100.0, human.inPosition, human.readiness, human.reason),
            candles,
            human.trades.map { CompetitionMarker(it.time, it.action, it.price, it.pnlEur) },
            T32CostPolicyV660.FEE_RATE
        ))
    }

    private fun setChart(index: Int, dataset: CompetitionDataset) {
        datasets[index] = dataset
        charts[index].setData(dataset.title, dataset.subtitle, dataset.candles, dataset.markers, dataset.feeRate)
    }

    private fun showChartDetail(index: Int) {
        val dataset = datasets.getOrNull(index) ?: return
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        body.addView(TextView(this).apply {
            text = "${dataset.title}\n${dataset.subtitle}"
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(dp(8), dp(4), dp(8), dp(6))
        })
        val detailChart = CompetitionChartView(this).apply {
            setVisibleBars(48)
            setData(dataset.title, dataset.subtitle, dataset.candles, dataset.markers, dataset.feeRate)
        }
        body.addView(detailChart, LinearLayout.LayoutParams(-1, dp(330)))
        body.addView(TextView(this).apply {
            text = "Проведите пальцем по графику для прошлого. Ниже — точные события счёта."
            setTextColor(Color.parseColor("#8B949E"))
            textSize = 12f
            setPadding(dp(8), dp(6), dp(8), dp(6))
        })
        val tradeText = TextView(this).apply {
            setTextColor(Color.parseColor("#C9D1D9"))
            setBackgroundColor(Color.parseColor("#161B22"))
            textSize = 13f
            setPadding(dp(10), dp(8), dp(10), dp(8))
            text = detailTradeText(dataset)
        }
        body.addView(ScrollView(this).apply { addView(tradeText) }, LinearLayout.LayoutParams(-1, dp(220)).apply { topMargin = dp(6) })
        AlertDialog.Builder(this).setView(body).setPositiveButton("Закрыть", null).show()
    }

    private fun detailTradeText(dataset: CompetitionDataset): String {
        if (dataset.markers.isEmpty()) return "Сделок пока нет — счёт стартовал с нуля."
        return dataset.markers.takeLast(120).asReversed().joinToString("\n\n") { marker ->
            buildString {
                append(PumpBotEngine.formatDate(marker.time))
                append("  •  ")
                append(if (marker.action.startsWith("BUY")) "ВХОД" else "ВЫХОД")
                append(String.format(Locale.US, "  •  €%.8f", marker.price))
                if (kotlin.math.abs(marker.pnlEur) >= 0.005) append(String.format(Locale.GERMANY, "  •  %+.2f €", marker.pnlEur))
            }
        }
    }

    private fun summary(value: Double, percent: Double, inPosition: Boolean, readiness: Int, reason: String): String =
        String.format(Locale.GERMANY, "€%.2f • %+.2f%% • %s • готовность %d/100\n%s", value, percent, if (inPosition) "В PUMP" else "В ЕВРО", readiness, reason.take(180))

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
        val request = Request.Builder().url(url).header("Accept", "application/json")
            .header("User-Agent", "PumpSignalAndroid/${PumpBotEngine.appVersionName}").build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
