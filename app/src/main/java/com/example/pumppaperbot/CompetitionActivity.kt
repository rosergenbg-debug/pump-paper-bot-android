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
        root.addView(archiveStatus, LinearLayout.LayoutParams(-1, dp(54)).apply {
            topMargin = dp(3)
        })
        repeat(CompetitionAccountSpec.COUNT) { index ->
            val chart = CompetitionChartView(this).apply {
                contentDescription = "Открыть подробный график ${index + 1}"
                setOnClickListener { showChartDetail(index) }
            }
            charts += chart
            root.addView(chart, LinearLayout.LayoutParams(-1, 0, 1f).apply {
                topMargin = dp(3)
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
        val archive = ResearchHistoryArchive.summary(this)
        val ledger = runCatching { ResearchPerformanceLedger.summary(this) }
            .getOrDefault(ResearchLedgerSummary(0, 0, 0))
        archiveStatus.text = archive.compactText() +
            "\nНЕПРЕРЫВНЫЙ ЖУРНАЛ V4→V5+: ${ledger.trades} сделок, ${ledger.decisions} решений. • Нажмите график для деталей"
        val now = System.currentTimeMillis()
        val snapshot = PumpBotEngine.snapshot(this)
        val price = PaperExecutionPolicy.displayPrice(snapshot, now)
        val app = AppPaperStore.state(this)
        val pumpMachine = PumpMachineStore.state(this)
        val pumpMachine2 = PumpMachine2Store.state(this)
        val geminiExitExperiment = GeminiExitExperimentStore.state(this)?.portfolio
            ?: GeminiPaperPortfolio()
        val user = UserPaperStore.markToMarket(this, price)
        val fusionMarket = BitpandaFusionStore.state(this)
        val fusionPrice = fusionMarket.bid.takeIf { fusionMarket.fresh(now) } ?: price
        val fusion = FusionSimStore.state(this)
        val closedCandles = if (historicalCandles.isNotEmpty()) {
            (historicalCandles + snapshot.chart.candles)
                .distinctBy { it.closeTime }
                .sortedBy { it.closeTime }
        } else {
            snapshot.chart.candles
        }
        val candles = CompetitionChartPresentation.withLiveEdge(closedCandles, price, now)

        val pumpMachineValue = PumpMachinePolicy.netLiquidationValue(
            pumpMachine,
            fusionPrice,
            fusionMarket.feeRate
        )
        setChart(0, CompetitionDataset(
            "PUMP 3% NET • V5.28 НЕЗАВИСИМЫЙ ВХОД • TP +3% / SL −1,3%",
            summary(
                pumpMachineValue,
                (pumpMachineValue / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0,
                pumpMachine.inPosition
            ),
            candles,
            pumpMachine.trades.map { CompetitionMarker(it.time, it.action, it.price, it.pnlEur) },
            FusionTradingCosts.FEE_RATE
        ))

        val pumpMachine2Value = PumpMachine2Policy.netLiquidationValue(
            pumpMachine2,
            fusionPrice,
            fusionMarket.feeRate
        )
        setChart(1, CompetitionDataset(
            "PUMP 2% NET • V5.28 НЕЗАВИСИМЫЙ ВХОД • TP +2% / SL −1,1%",
            summary(
                pumpMachine2Value,
                (pumpMachine2Value / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0,
                pumpMachine2.inPosition
            ),
            candles,
            pumpMachine2.trades.map { CompetitionMarker(it.time, it.action, it.price, it.pnlEur) },
            FusionTradingCosts.FEE_RATE
        ))
        setChart(2, CompetitionDataset(
            "DEEPSIGX",
            summary(
                geminiExitExperiment.value(price),
                geminiExitExperiment.profitPercent(price),
                geminiExitExperiment.inPosition
            ),
            candles,
            geminiExitExperiment.trades.map { CompetitionMarker(it.time, it.action, it.price, it.pnlEur) },
            0.0015
        ))
        setChart(3, CompetitionDataset(
            "APP",
            summary(app.value(price), app.profitPercent(price), app.inPosition),
            candles,
            app.trades.map { CompetitionMarker(it.candleTime, it.action, it.price, it.pnlEur) },
            0.0015
        ))
        setChart(4, CompetitionDataset(
            "DEEPSIG FUSION",
            summary(
                fusion.value(fusionPrice),
                fusion.profit(fusionPrice) / FusionSimPortfolio.START_BALANCE * 100.0,
                fusion.inPosition
            ),
            candles,
            fusion.trades.map { CompetitionMarker(it.time, it.action, it.price, it.pnlEur) },
            FusionTradingCosts.FEE_RATE
        ))
        setChart(5, CompetitionDataset(
            "СЕРЖ",
            summary(user.value(price), user.profitPercent(price), user.inPosition),
            candles,
            user.trades.map { CompetitionMarker(it.time, it.action, it.price) },
            0.0015
        ))
    }

    private fun setChart(index: Int, dataset: CompetitionDataset) {
        datasets[index] = dataset
        charts[index].setData(
            dataset.title,
            dataset.subtitle,
            dataset.candles,
            dataset.markers,
            dataset.feeRate
        )
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
            text = "Проведите пальцем по графику для просмотра прошлого. Ниже — точные события, чтобы метки не перекрывали друг друга."
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
        val tradeScroll = ScrollView(this).apply { addView(tradeText) }
        body.addView(tradeScroll, LinearLayout.LayoutParams(-1, dp(220)).apply { topMargin = dp(6) })

        AlertDialog.Builder(this)
            .setView(body)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    private fun detailTradeText(dataset: CompetitionDataset): String {
        if (dataset.markers.isEmpty()) return "Сделок пока нет."
        return dataset.markers.takeLast(120).asReversed().joinToString("\n\n") { marker ->
            buildString {
                append(PumpBotEngine.formatDate(marker.time))
                append("  •  ")
                append(when {
                    marker.action.startsWith("BUY") -> "ВХОД"
                    marker.action == "SELL_HALF" -> "½ ВЫХОД"
                    else -> "ВЫХОД"
                })
                append(String.format(Locale.US, "  •  €%.8f", marker.price))
                if (kotlin.math.abs(marker.pnlEur) >= 0.005) {
                    append(String.format(Locale.GERMANY, "  •  %+.2f €", marker.pnlEur))
                }
            }
        }
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
