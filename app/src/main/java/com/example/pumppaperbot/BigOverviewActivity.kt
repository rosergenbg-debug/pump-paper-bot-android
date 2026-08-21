package com.example.pumppaperbot

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max

class BigOverviewActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_OPEN_ZOOMED = "open_zoomed"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()
    private val executor = Executors.newFixedThreadPool(8)
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var status: TextView
    private lateinit var profileStatus: TextView
    private lateinit var pointStatus: TextView
    private lateinit var range: TextView
    private lateinit var chart: StrategyChartView
    private lateinit var zoomButton: Button
    private lateinit var cautiousButton: Button
    private lateinit var aggressiveButton: Button
    private lateinit var flowBars: BigFlowBarsView
    private lateinit var flowClock: FlowClockView
    private lateinit var flowText: TextView
    private lateinit var moneyText: TextView

    private var aggressive = false
    private var startTime = 0L
    private var initialZoomed = false
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

    private val liveRefresh = object : Runnable {
        override fun run() {
            renderLiveFlow()
            handler.postDelayed(this, 2_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aggressive = PumpBotEngine.isAggressive(this)
        initialZoomed = intent.getBooleanExtra(EXTRA_OPEN_ZOOMED, false)
        renderScreen()
        loadSixMonths()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(liveRefresh)
        handler.post(liveRefresh)
    }

    override fun onPause() {
        handler.removeCallbacks(liveRefresh)
        super.onPause()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun renderScreen() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(7), dp(10), dp(24))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }

        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        nav.addView(button("← НАЗАД", "#30363D").apply { setOnClickListener { finish() } }, weighted(1f, 48))
        zoomButton = button("УВЕЛИЧИТЬ ×2", "#1F6FEB").apply { setOnClickListener { cycleZoom() } }
        nav.addView(zoomButton, weighted(1.25f, 48).apply { leftMargin = dp(7) })
        content.addView(nav)

        status = label("Загружаю 6 месяцев PUMP/EUR…", 16, "#F0F6FC", true)
        content.addView(status, fullWrap(7))

        val profiles = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        cautiousButton = button("ОСТОРОЖНЫЙ", "#30363D").apply { setOnClickListener { selectProfile(false) } }
        aggressiveButton = button("АКТИВНЫЙ", "#30363D").apply { setOnClickListener { selectProfile(true) } }
        profiles.addView(cautiousButton, weighted(1f, 46))
        profiles.addView(aggressiveButton, weighted(1f, 46).apply { leftMargin = dp(7) })
        content.addView(profiles, fullWrap(7))
        profileStatus = label("", 13, "#C9D1D9", true)
        content.addView(profileStatus)
        renderProfileButtons()

        val tradeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tradeRow.addView(button("← СДЕЛКА", "#1F6FEB").apply { setOnClickListener { moveTrade(-1) } }, weighted(1f, 48))
        tradeRow.addView(button("ПОСЛЕДНИЕ СВЕЧИ", "#30363D").apply { setOnClickListener { showLatest() } }, weighted(1.25f, 48).apply { leftMargin = dp(6) })
        tradeRow.addView(button("СДЕЛКА →", "#1F6FEB").apply { setOnClickListener { moveTrade(1) } }, weighted(1f, 48).apply { leftMargin = dp(6) })
        content.addView(tradeRow, fullWrap(7))

        val criteria = button("ПОЧЕМУ ВХОД / ВЫХОД", "#8250DF").apply { setOnClickListener { showActiveTradeCriteria() } }
        content.addView(criteria, fullHeight(46, 5))

        pointStatus = label(
            "ПОСЛЕДНИЕ СВЕЧИ • живой край справа • свечи задают масштаб, EMA не сжимают цену",
            14,
            "#79C0FF",
            true
        ).apply {
            setPadding(dp(9), dp(8), dp(9), dp(8))
            setBackgroundColor(Color.parseColor("#161B22"))
        }
        content.addView(pointStatus, fullWrap(6))

        chart = StrategyChartView(this).apply {
            setMainViewportMode(true)
            setVisibleBarLimit(if (initialZoomed) 30 else 60)
        }
        content.addView(chart, fullHeight(if (initialZoomed) 680 else 520, 5))

        range = label("На экране: —", 13, "#C9D1D9", true)
        content.addView(range, fullWrap(4))
        chart.setOnHistoryWindowChanged { _, start, end ->
            val format = SimpleDateFormat("dd.MM HH:mm", Locale.GERMAN)
            range.text = "На экране: ${format.format(Date(start))} — ${format.format(Date(end))}"
        }
        updateZoomUi()

        content.addView(label("ПОТОК РЫНКА • МГНОВЕННО / 5 / 15 / 30 МИН", 17, "#F0F6FC", true), fullWrap(12))
        content.addView(label(
            "Ноль посередине. Зелёное вверх — давление покупателей, красное вниз — продавцов. Это наблюдение потока, не самостоятельная команда BUY/EXIT.",
            12,
            "#8B949E",
            false
        ))
        flowBars = BigFlowBarsView(this)
        content.addView(flowBars, fullHeight(270, 5))

        content.addView(label("ДУГИ ДЕНЕЖНОГО ПОТОКА • 5 / 15 / 30 / 60 МИН", 17, "#F0F6FC", true), fullWrap(12))
        flowClock = FlowClockView(this)
        content.addView(flowClock, fullHeight(390, 5))
        flowText = panel(13, true)
        content.addView(flowText, fullWrap(5))

        content.addView(label("КРУПНЫЙ ВИДИМЫЙ ПОТОК", 17, "#F0F6FC", true), fullWrap(12))
        moneyText = panel(14, true)
        content.addView(moneyText, fullWrap(5))

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        })
    }

    private fun selectProfile(value: Boolean) {
        aggressive = value
        PumpBotEngine.setAggressive(this, value)
        renderProfileButtons()
        if (allCandles.isNotEmpty()) renderHistory()
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
        val next = if (chart.currentVisibleBarLimit() <= 30) 60 else 30
        chart.setVisibleBarLimit(next)
        if (activeTradeIndex >= 0) {
            completedTrades.getOrNull(activeTradeIndex)?.let { connection ->
                chart.centerOnTime(connection.entry.time + (connection.exit.time - connection.entry.time) / 2L)
            }
        } else {
            chart.setHistoryOffsetBars(0)
        }
        updateZoomUi()
    }

    private fun updateZoomUi() {
        val zoomed = chart.currentVisibleBarLimit() <= 30
        zoomButton.text = if (zoomed) "ВЕРНУТЬ ОБЗОР" else "УВЕЛИЧИТЬ ×2"
        val params = chart.layoutParams as? LinearLayout.LayoutParams ?: return
        val wanted = dp(if (zoomed) 680 else 520)
        if (params.height != wanted) {
            params.height = wanted
            chart.layoutParams = params
        }
    }

    private fun showLatest() {
        activeTradeIndex = -1
        chart.setVisibleBarLimit(if (initialZoomed) 30 else 60)
        chart.resetVerticalViewport()
        chart.setHistoryOffsetBars(0)
        pointStatus.text = "ПОСЛЕДНИЕ СВЕЧИ • живой край справа • тяните ↑↓, если хотите найти EMA"
        updateZoomUi()
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
        chart.resetVerticalViewport()
        chart.focusOnTimeRange(connection.entry.time, connection.exit.time)
        pointStatus.text = String.format(
            Locale.GERMAN,
            "СДЕЛКА %d/%d • %+.2f%% • %+.2f EUR\nВХОД %s €%.8f → ВЫХОД %s €%.8f",
            activeTradeIndex + 1,
            completedTrades.size,
            connection.profitPercent,
            connection.profitEur,
            formatCompactDate(connection.entry.time),
            connection.entry.price,
            formatCompactDate(connection.exit.time),
            connection.exit.price
        )
        updateZoomUi()
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
            connection.partialExits.joinToString("\n") {
                "• ${formatCompactDate(it.time)} — ${it.reason} по €${String.format(Locale.GERMAN, "%.8f", it.price)}"
            }
        }
        AlertDialog.Builder(this)
            .setTitle("Критерии сделки ${activeTradeIndex + 1}")
            .setMessage(buildString {
                append("ПОЧЕМУ ВОШЛИ\n✓ ${connection.entry.reason}\n\n")
                append("ПОЧЕМУ ВЫШЛИ\n✓ ${connection.exit.reason}\n\n")
                append("ЧАСТИЧНАЯ ФИКСАЦИЯ\n$partial\n\n")
                append(String.format(Locale.GERMAN, "ИТОГ %+.2f%% • %+.2f EUR", connection.profitPercent, connection.profitEur))
            })
            .setPositiveButton("ПОНЯТНО", null)
            .show()
    }

    private fun renderLiveFlow() {
        if (!::flowBars.isInitialized) return
        val now = System.currentTimeMillis()
        val breathing = LiveMarketBreathingStore.snapshot(this, now)
        val micro = MicroImpulseStore.state(this)
        val scores = MainChartFlowPresentation.from(breathing)
        chart.setFlowScores(scores)
        flowBars.setData(scores)
        flowClock.setData(breathing)
        flowText.text = ContinuousFlowWaveText.describe(breathing)
        moneyText.text = runCatching { BigOverviewMoneyText.describe(micro) }
            .getOrElse { error -> "КРУПНЫЙ ПОТОК: ошибка отображения (${error.javaClass.simpleName}). Остальной обзор продолжает работать." }
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
                runOnUiThread { renderHistory() }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Не удалось загрузить историю: ${e.message ?: "ошибка сети"}"
                    status.setTextColor(Color.parseColor("#FF7B72"))
                }
            }
        }
    }

    private fun renderHistory() {
        if (allCandles.isEmpty()) return
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
            subtitle = "6 месяцев • 30 минут • EMA50/EMA200 • свечи задают вертикальный масштаб",
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
        chart.setData("PUMP/EUR — ЖИВОЙ ОБЗОР", bundle)
        activeTradeIndex = -1
        chart.setVisibleBarLimit(if (initialZoomed) 30 else 60)
        chart.resetVerticalViewport()
        chart.setHistoryOffsetBars(0)
        pointStatus.text = "ПОСЛЕДНИЕ СВЕЧИ • открыт самый свежий участок рынка, а не последняя историческая сделка"
        updateZoomUi()
        renderLiveFlow()
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

    private fun formatCompactDate(time: Long): String =
        SimpleDateFormat("dd.MM HH:mm", Locale.GERMAN).format(Date(time))

    private fun panel(size: Int, bold: Boolean) = label("", size, "#C9D1D9", bold).apply {
        setBackgroundColor(Color.parseColor("#161B22"))
        setPadding(dp(10), dp(10), dp(10), dp(10))
    }

    private fun label(text: String, size: Int, color: String, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.parseColor(color))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun button(text: String, color: String) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor(color))
        textSize = 13f
        maxLines = 2
        isAllCaps = false
        gravity = Gravity.CENTER
        setPadding(dp(3), 0, dp(3), 0)
    }

    private fun weighted(weight: Float, height: Int) = LinearLayout.LayoutParams(0, dp(height), weight)
    private fun fullHeight(height: Int, top: Int = 0) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(height)).apply { topMargin = dp(top) }
    private fun fullWrap(top: Int = 0) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top) }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

object BigOverviewMoneyText {
    fun describe(micro: MicroImpulseState): String {
        val f = micro.largeFlow
        val netLarge = f.largeBuyUsdt - f.largeSellUsdt
        val allNet60 = micro.buyNotional60s - micro.sellNotional60s
        val all60 = micro.buyNotional60s + micro.sellNotional60s
        val side = when {
            netLarge > max(5_000.0, f.thresholdUsdt * 0.35) -> "КРУПНЫЙ НЕТТО-ПОТОК В ПОКУПКУ"
            netLarge < -max(5_000.0, f.thresholdUsdt * 0.35) -> "КРУПНЫЙ НЕТТО-ПОТОК В ПРОДАЖУ"
            else -> "КРУПНЫЙ НЕТТО-ПОТОК СБАЛАНСИРОВАН"
        }
        return buildString {
            append(side)
            append(String.format(Locale.GERMANY, "\nЧистый крупный поток 5 мин: %+,.0f USDT", netLarge))
            append(String.format(Locale.GERMANY, "\nКрупные BUY $%,.0f • SELL $%,.0f", f.largeBuyUsdt, f.largeSellUsdt))
            append(String.format(Locale.GERMANY, "\nДинамический порог крупной заявки: $%,.0f", f.thresholdUsdt))
            append(String.format(Locale.GERMANY, "\nКрупнейшая BUY $%,.0f • SELL $%,.0f", f.largestBuyUsdt, f.largestSellUsdt))
            append("\nСерии частей: BUY ${f.buySlices} • SELL ${f.sellSlices}")
            append(String.format(Locale.GERMANY, "\n\nВесь taker-поток 60 сек: $%,.0f • нетто %+,.0f", all60, allNet60))
            append("\n${f.title} • уверенность ${f.confidence}/100")
            append("\n${f.explanation}")
            append("\nПочерк: ${f.fingerprint}")
            append("\n\nВажно: это объём прошедших сделок за окно, а не сумма денег, которая сейчас «сидит» в монете. Публичная лента не раскрывает владельца; серия похожих заявок — признак алгоритмического потока, а не доказательство конкретной компании.")
        }
    }
}

class BigFlowBarsView(context: Context) : View(context) {
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#101820") }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#30363D") }
    private val positive = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#32C789") }
    private val negative = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF4D6D") }
    private val neutral = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6E7681") }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        textSize = sp(12f)
    }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B949E")
        textAlign = Paint.Align.CENTER
        textSize = sp(10f)
    }
    private val rect = RectF()
    private var scores = MainChartFlowScores(null, null, null, null, fresh = false)

    fun setData(value: MainChartFlowScores) {
        scores = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(bg.color)
        val items = listOf(
            "МГН" to scores.instant,
            "5 МИН" to scores.fiveMinutes,
            "15 МИН" to scores.fifteenMinutes,
            "30 МИН" to scores.thirtyMinutes
        )
        val top = dp(34f)
        val bottom = height - dp(30f)
        val middle = (top + bottom) / 2f
        val slot = width / 4f
        val barWidth = slot * 0.50f
        val usable = (bottom - top) / 2f

        items.forEachIndexed { index, (label, raw) ->
            val centerX = slot * (index + 0.5f)
            val left = centerX - barWidth / 2f
            val right = centerX + barWidth / 2f
            rect.set(left, top, right, bottom)
            canvas.drawRoundRect(rect, dp(5f), dp(5f), track)
            canvas.drawRect(left, middle - dp(1f), right, middle + dp(1f), neutral)
            canvas.drawText(label, centerX, dp(20f), text)
            val score = raw?.coerceIn(-100, 100)
            if (score == null) {
                canvas.drawText("—", centerX, bottom + dp(19f), small)
                return@forEachIndexed
            }
            val amount = usable * abs(score) / 100f
            if (score >= 0) {
                rect.set(left + dp(5f), middle - amount, right - dp(5f), middle)
                canvas.drawRoundRect(rect, dp(3f), dp(3f), positive)
            } else {
                rect.set(left + dp(5f), middle, right - dp(5f), middle + amount)
                canvas.drawRoundRect(rect, dp(3f), dp(3f), negative)
            }
            text.color = if (score > 0) positive.color else if (score < 0) negative.color else Color.WHITE
            canvas.drawText(if (score > 0) "+$score" else score.toString(), centerX, bottom + dp(20f), text)
            text.color = Color.WHITE
        }
        small.textAlign = Paint.Align.LEFT
        canvas.drawText("+100", dp(5f), top + dp(8f), small)
        canvas.drawText("0", dp(5f), middle + dp(4f), small)
        canvas.drawText("−100", dp(5f), bottom, small)
        small.textAlign = Paint.Align.CENTER
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
    private fun sp(value: Float) = value * resources.displayMetrics.scaledDensity
}
