package com.example.pumppaperbot

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
    private val executor = Executors.newFixedThreadPool(8)

    private lateinit var chart: StrategyChartView
    private lateinit var status: TextView
    private lateinit var profileStatus: TextView
    private lateinit var pointStatus: TextView
    private lateinit var range: TextView
    private lateinit var seek: SeekBar
    private lateinit var cautiousButton: Button
    private lateinit var aggressiveButton: Button
    private lateinit var zoomButton: Button
    private lateinit var criteriaButton: Button
    private var adjustingSeek = false
    private var aggressive = false
    private var startTime = 0L
    private var allCandles: List<PumpCandle> = emptyList()
    private var btcCandles: List<PumpCandle> = emptyList()
    private var ethCandles: List<PumpCandle> = emptyList()
    private var solCandles: List<PumpCandle> = emptyList()
    private var futuresCandles: List<PumpCandle> = emptyList()
    private var premiumCandles: List<PumpCandle> = emptyList()
    private var funding: List<FundingPoint> = emptyList()
    private var signalPoints: List<TradeEvent> = emptyList()
    private var completedTrades: List<TradeConnection> = emptyList()
    private var activeTradeIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aggressive = PumpBotEngine.isAggressive(this)
        render()
        loadSixMonths()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }

        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        nav.addView(button("← НАЗАД", "#30363D").apply { setOnClickListener { finish() } }, LinearLayout.LayoutParams(0, dp(44), 1f))
        nav.addView(button("КРУПНО: ПОТОК ±100", "#1F6FEB").apply {
            setOnClickListener { SignalGaugeDialog.show(this@ChartDetailActivity, PumpBotEngine.snapshot(this@ChartDetailActivity)) }
        }, LinearLayout.LayoutParams(0, dp(44), 1.35f).apply { leftMargin = dp(8) })
        root.addView(nav)

        status = label("Загружаю историю PUMP/EUR за 6 месяцев…", 14, "#F0F6FC", true)
        root.addView(status)

        val profiles = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        cautiousButton = button("ОСТОРОЖНЫЙ", "#30363D").apply { setOnClickListener { selectProfile(false) } }
        aggressiveButton = button("АКТИВНЫЙ", "#30363D").apply { setOnClickListener { selectProfile(true) } }
        profiles.addView(cautiousButton, LinearLayout.LayoutParams(0, dp(44), 1f))
        profiles.addView(aggressiveButton, LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(8) })
        root.addView(profiles)
        profileStatus = label("", 13, "#C9D1D9", true)
        root.addView(profileStatus)
        renderProfileButtons()

        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(button("← СДЕЛКА", "#1F6FEB").apply { setOnClickListener { moveTrade(-1) } }, LinearLayout.LayoutParams(0, dp(44), 1f))
        zoomButton = button("УВЕЛИЧИТЬ ×2", "#30363D").apply { setOnClickListener { cycleZoom() } }
        controls.addView(zoomButton, LinearLayout.LayoutParams(0, dp(44), 1.15f).apply { leftMargin = dp(6) })
        controls.addView(button("СДЕЛКА →", "#1F6FEB").apply { setOnClickListener { moveTrade(1) } }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(6) })
        root.addView(controls, LinearLayout.LayoutParams(-1, dp(44)).apply { topMargin = dp(2) })

        val actionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        criteriaButton = button("ПОЧЕМУ ВХОД / ВЫХОД", "#8250DF").apply {
            setOnClickListener { showActiveTradeCriteria() }
        }
        actionRow.addView(criteriaButton, LinearLayout.LayoutParams(0, dp(38), 1.25f))
        actionRow.addView(button("ПОСЛЕДНИЕ СВЕЧИ", "#30363D").apply {
            setOnClickListener {
                activeTradeIndex = -1
                chart.setHistoryOffsetBars(0)
                pointStatus.text = "Последние свечи • удерживайте палец на свече для подробностей"
            }
        }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(6) })
        root.addView(actionRow, LinearLayout.LayoutParams(-1, dp(38)).apply { topMargin = dp(2) })

        pointStatus = label("После загрузки откроется последняя историческая сделка.", 13, "#58A6FF", true).apply {
            maxLines = 3
            setPadding(dp(8), dp(5), dp(8), dp(5))
            background = roundedBackground("#161B22", 8)
        }
        root.addView(pointStatus)

        chart = StrategyChartView(this).apply { setVisibleBarLimit(120) }
        root.addView(chart, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(1) })

        range = label("Период на экране: —", 13, "#C9D1D9", true)
        root.addView(range)
        seek = SeekBar(this).apply {
            isEnabled = false
            max = 1
            progress = 1
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser && !adjustingSeek) {
                        activeTradeIndex = -1
                        chart.setHistoryOffsetBars(chart.maxHistoryOffset() - progress)
                        pointStatus.text = "Период выбран вручную\nСиние стрелки показывают входы и выходы сделок"
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        root.addView(seek, LinearLayout.LayoutParams(-1, dp(28)))

        chart.setOnHistoryWindowChanged { offset, start, end ->
            val format = SimpleDateFormat("dd.MM HH:mm", Locale.GERMAN)
            range.text = "На экране: ${format.format(Date(start))} — ${format.format(Date(end))}"
            if (seek.isEnabled) {
                adjustingSeek = true
                seek.progress = (chart.maxHistoryOffset() - offset).coerceIn(0, seek.max)
                adjustingSeek = false
            }
        }
        setContentView(root)
    }

    private fun selectProfile(value: Boolean) {
        aggressive = value
        PumpBotEngine.setAggressive(this, value)
        renderProfileButtons()
        if (allCandles.isNotEmpty()) renderHistory(focusLastSignal = true)
    }

    private fun renderProfileButtons() {
        cautiousButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (!aggressive) "#238636" else "#30363D"))
        aggressiveButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (aggressive) "#B62324" else "#30363D"))
        profileStatus.text = if (aggressive) {
            "Активный: более позднее подтверждение после падений"
        } else {
            "Осторожный: вход только пока цена близко ко дну"
        }
    }

    private fun cycleZoom() {
        val next = nextChartVisibleBarLimit(chart.currentVisibleBarLimit())
        chart.setVisibleBarLimit(next)
        completedTrades.getOrNull(activeTradeIndex)?.let { connection ->
            chart.centerOnTime(connection.entry.time + (connection.exit.time - connection.entry.time) / 2L)
        }
        updateZoomButton(next)
    }

    private fun moveTrade(delta: Int) {
        if (completedTrades.isEmpty()) {
            pointStatus.text = "Для выбранного профиля завершённых сделок нет."
            return
        }
        activeTradeIndex = if (activeTradeIndex !in completedTrades.indices) {
            if (delta < 0) completedTrades.lastIndex else 0
        } else {
            (activeTradeIndex + delta).coerceIn(0, completedTrades.lastIndex)
        }
        focusActiveTrade()
    }

    private fun focusActiveTrade() {
        val connection = completedTrades.getOrNull(activeTradeIndex) ?: return
        updateZoomButton(chart.focusOnTimeRange(connection.entry.time, connection.exit.time))
        pointStatus.text = String.format(
            Locale.GERMAN,
            "СДЕЛКА %d/%d   %+.2f%%  •  %+.2f EUR  •  %s\nВХОД  %s  €%.8f\nВЫХОД %s  €%.8f",
            activeTradeIndex + 1,
            completedTrades.size,
            connection.profitPercent,
            connection.profitEur,
            formatDuration(connection.durationMillis),
            formatCompactDate(connection.entry.time),
            connection.entry.price,
            formatCompactDate(connection.exit.time),
            connection.exit.price
        )
    }

    private fun showActiveTradeCriteria() {
        val connection = completedTrades.getOrNull(activeTradeIndex)
        if (connection == null) {
            pointStatus.text = "Сначала выберите сделку кнопками ← СДЕЛКА / СДЕЛКА →"
            return
        }
        val partial = if (connection.partialExits.isEmpty()) {
            "Частичной продажи не было."
        } else {
            connection.partialExits.joinToString("\n") { "• ${formatCompactDate(it.time)} — ${it.reason} по €${String.format(Locale.GERMAN, "%.8f", it.price)}" }
        }
        val message = buildString {
            append("ПОЧЕМУ ВОШЛИ\n")
            append("✓ Сигнал сформирован на закрытой 30-минутной свече.\n")
            append("✓ ${connection.entry.reason}\n")
            append("✓ Покупка исполнена на открытии следующей свечи.\n\n")
            append("ПОЧЕМУ ВЫШЛИ\n")
            append("✓ ${connection.exit.reason}\n")
            append("✓ Продажа исполнена после подтверждения, с учётом проскальзывания 0,05%.\n\n")
            append("ЧАСТИЧНАЯ ФИКСАЦИЯ\n$partial\n\n")
            append(String.format(Locale.GERMAN, "ИТОГ  %+.2f%%  •  %+.2f EUR\nДеньги работали: %s", connection.profitPercent, connection.profitEur, formatDuration(connection.durationMillis)))
        }
        AlertDialog.Builder(this)
            .setTitle("Критерии сделки ${activeTradeIndex + 1}")
            .setMessage(message)
            .setPositiveButton("ПОНЯТНО", null)
            .show()
    }

    private fun loadSixMonths() {
        startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(183)
        val warmupStart = startTime - TimeUnit.DAYS.toMillis(45)
        val endTime = System.currentTimeMillis()
        val pumpFuture = executor.submit<List<PumpCandle>> { fetchCandles(PumpBotEngine.pumpSymbol, warmupStart, endTime) }
        val eurFuture = executor.submit<List<PumpCandle>> { fetchCandles(PumpBotEngine.eurSymbol, warmupStart, endTime) }
        val btcFuture = executor.submit<List<PumpCandle>> { fetchCandles(PumpBotEngine.btcSymbol, warmupStart, endTime) }
        val ethFuture = executor.submit<List<PumpCandle>> { fetchCandles(PumpBotEngine.ethSymbol, warmupStart, endTime) }
        val solFuture = executor.submit<List<PumpCandle>> { fetchCandles(PumpBotEngine.solSymbol, warmupStart, endTime) }
        val futuresFuture = executor.submit<List<PumpCandle>> { fetchDerivativeCandles(warmupStart, endTime, false) }
        val premiumFuture = executor.submit<List<PumpCandle>> { fetchDerivativeCandles(warmupStart, endTime, true) }
        val fundingFuture = executor.submit<List<FundingPoint>> { fetchFunding(warmupStart, endTime) }

        executor.execute {
            try {
                allCandles = StrategyV2.synthesizeEur(pumpFuture.get(), eurFuture.get())
                btcCandles = btcFuture.get()
                ethCandles = ethFuture.get()
                solCandles = solFuture.get()
                futuresCandles = futuresFuture.get()
                premiumCandles = premiumFuture.get()
                funding = fundingFuture.get()
                runOnUiThread { renderHistory(focusLastSignal = true) }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Не удалось загрузить историю: ${e.message ?: "ошибка сети"}"
                    status.setTextColor(Color.parseColor("#FF7B72"))
                }
            }
        }
    }

    private fun renderHistory(focusLastSignal: Boolean) {
        val result = StrategyV2.backtest(
            allCandles, btcCandles, funding, startTime, aggressive,
            ethCandles, solCandles, futuresCandles, premiumCandles
        )
        val closes = allCandles.map { it.close }
        val fast = ema(closes, PumpBotEngine.emaFastPeriod)
        val slow = ema(closes, PumpBotEngine.emaSlowPeriod)
        val first = allCandles.indexOfFirst { it.closeTime >= startTime }.let { if (it < 0) 0 else it }
        val displayCandles = allCandles.drop(first)
        signalPoints = result.trades.sortedBy { it.time }
        completedTrades = completedTradeConnections(signalPoints)
        val bundle = ChartBundle(
            candles = displayCandles,
            fast = fast.drop(first),
            slow = slow.drop(first),
            trades = signalPoints,
            subtitle = "6 месяцев • 30 минут • EMA50/EMA200",
            aggressive = aggressive,
            showReadinessGauge = false
        )
        status.text = String.format(
            Locale.GERMAN,
            "PUMP/EUR • %d сделок • %+.2f%% (%+.2f EUR) • защита %d",
            result.roundTrips,
            result.profitPercent,
            result.profit,
            result.blockedOverheatCount
        )
        chart.setData("PUMP/EUR — ВХОДЫ И ВЫХОДЫ", bundle)
        seek.max = chart.maxHistoryOffset().coerceAtLeast(1)
        seek.progress = seek.max
        seek.isEnabled = chart.maxHistoryOffset() > 0
        if (focusLastSignal && completedTrades.isNotEmpty()) {
            activeTradeIndex = completedTrades.lastIndex
            focusActiveTrade()
        } else if (completedTrades.isEmpty()) {
            activeTradeIndex = -1
            pointStatus.text = "За выбранный период завершённых сделок не найдено."
        }
    }

    private fun fetchCandles(symbol: String, start: Long, end: Long): List<PumpCandle> {
        val all = ArrayList<PumpCandle>()
        var cursor = start
        while (cursor < end && !Thread.currentThread().isInterrupted) {
            val batch = PumpBotEngine.parseCandles(request(PumpBotEngine.historicalKlineUrl(symbol, "30m", cursor, end)))
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

    private fun fetchDerivativeCandles(start: Long, end: Long, premium: Boolean): List<PumpCandle> {
        val all = ArrayList<PumpCandle>()
        var cursor = start
        while (cursor < end && !Thread.currentThread().isInterrupted) {
            val url = if (premium) {
                PumpBotEngine.historicalPremiumKlineUrl(PumpBotEngine.pumpSymbol, "30m", cursor, end)
            } else {
                PumpBotEngine.historicalFuturesKlineUrl(PumpBotEngine.pumpSymbol, "30m", cursor, end)
            }
            val batch = PumpBotEngine.parseCandles(request(url))
            if (batch.isEmpty()) break
            all.addAll(batch)
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
        textSize = 13f
        maxLines = 2
        isAllCaps = false
        setPadding(dp(3), 0, dp(3), 0)
    }

    private fun updateZoomButton(limit: Int) {
        zoomButton.text = chartZoomActionText(limit)
    }

    private fun formatDuration(durationMillis: Long): String {
        val totalMinutes = durationMillis / 60_000L
        val days = totalMinutes / (24L * 60L)
        val hours = totalMinutes / 60L % 24L
        val minutes = totalMinutes % 60L
        return when {
            days > 0L -> "${days} д ${hours} ч ${minutes} мин"
            hours > 0L -> "${hours} ч ${minutes} мин"
            else -> "${minutes} мин"
        }
    }

    private fun formatCompactDate(time: Long): String =
        SimpleDateFormat("dd.MM HH:mm", Locale.GERMAN).format(Date(time))

    private fun label(text: String, size: Int, color: String, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size.coerceAtLeast(13).toFloat()
        setTextColor(Color.parseColor(color))
        setPadding(0, dp(2), 0, dp(2))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun roundedBackground(color: String, radiusDp: Int) = GradientDrawable().apply {
        setColor(Color.parseColor(color))
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
