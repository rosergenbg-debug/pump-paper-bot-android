package com.example.pumppaperbot

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal V6.9-style home: existing local position state and monitoring stay intact;
 * ONLY authenticated Bitpanda Fusion quotes/candles appear on this screen.
 * No order placement, no private key in GitHub or logs.
 */
class FusionDashboardActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val fetching = AtomicBoolean(false)
    private val market = FusionLiveMarket()
    private var quote: FusionLiveMarket.Quote? = null
    private var fetchError: String = ""
    private var interval = "30m"

    private lateinit var status: TextView
    private lateinit var position: TextView
    private lateinit var pumpInfo: TextView
    private lateinit var btcInfo: TextView
    private lateinit var pumpChart: FusionCandleChartView
    private lateinit var btcChart: FusionCandleChartView
    private lateinit var buyButton: Button
    private lateinit var sellButton: Button
    private val speedButtons = linkedMapOf<String, MutableList<Button>>()
    private lateinit var flowPanel: FusionMoneyFlowPanel

    private val ticker = object : Runnable {
        override fun run() {
            FusionRefreshState.heartbeat(this@FusionDashboardActivity)
            refreshMarket()
            handler.postDelayed(this, FusionRefreshState.FOREGROUND_INTERVAL_MS)
        }
    }
    private val flowTicker = object : Runnable {
        override fun run() {
            if (::flowPanel.isInitialized) {
                flowPanel.render(FusionFlowViewModel.from(
                    MicroImpulseStore.state(this@FusionDashboardActivity),
                    System.currentTimeMillis()
                ))
            }
            handler.postDelayed(this, 5_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        interval = getSharedPreferences("fusion_compact_display_v1", MODE_PRIVATE)
            .getString("interval", "30m")?.takeIf { it in listOf("5m", "15m", "30m", "1h") } ?: "30m"
        PumpBotEngine.ensureInitialized(this)
        if (!PumpBotEngine.snapshot(this).running) {
            PumpBotEngine.setRunning(this, true)
            PumpAlert.ensureChannels(this)
            ContextCompat.startForegroundService(this, Intent(this, PumpSignalService::class.java))
        }
        val work = PeriodicWorkRequestBuilder<PumpBotWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf(
                PumpBotWorker.INPUT_CYCLE_SOURCE to "ANDROID РЕЗЕРВ 15 МИН",
                PumpBotWorker.INPUT_CYCLE_INTERVAL to TimeUnit.MINUTES.toMillis(15)
            )).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PumpBotEngine.uniqueWorkName, ExistingPeriodicWorkPolicy.KEEP, work
        )
        buildScreen()
        render()
    }

    override fun onResume() {
        super.onResume()
        FusionRefreshState.foreground(this, true)
        // A background refresh may already have updated the shared non-secret quote cache.
        FusionQuoteCache.read(this, interval)?.let { cached ->
            val old = quote
            if (old == null || cached.updatedAt > old.updatedAt) quote = cached
        }
        render()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
        handler.removeCallbacks(flowTicker)
        handler.post(flowTicker)
    }

    override fun onPause() {
        FusionRefreshState.foreground(this, false)
        handler.removeCallbacks(ticker)
        handler.removeCallbacks(flowTicker)
        super.onPause()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0D1117"))
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(18), dp(14), dp(25))
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(label("PUMP Сигнал • FUSION", 26, "#F0F6FC", true))
        status = label("Ожидаем Bitpanda…", 12, "#9EAAB8", false)
        root.addView(status)

        val tools = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val key = button("BITPANDA • КЛЮЧ READ", "#126E7D")
        key.setOnClickListener { startActivity(Intent(this, BitpandaFusionActivity::class.java)) }
        tools.addView(key, LinearLayout.LayoutParams(0, dp(56), 2f))
        val update = button("ОБНОВИТЬ", "#176028")
        update.setOnClickListener { refreshMarket() }
        tools.addView(update, LinearLayout.LayoutParams(0, dp(56), 1f).apply { leftMargin = dp(7) })
        root.addView(tools, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(13) })

        root.addView(label("МОЯ ПОЗИЦИЯ", 22, "#7EE787", true), topMargin(18))
        position = label("Позиция не открыта", 23, "#7EE787", true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(18), dp(8), dp(18))
            setBackgroundColor(Color.parseColor("#10231C"))
        }
        root.addView(position, topMargin(7))

        val trade = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buyButton = button("Я КУПИЛ", "#166534")
        buyButton.setOnClickListener { recordTrade(buy = true) }
        sellButton = button("Я ПРОДАЛ", "#A82222")
        sellButton.setOnClickListener { recordTrade(buy = false) }
        trade.addView(buyButton, LinearLayout.LayoutParams(0, dp(58), 1f))
        trade.addView(sellButton, LinearLayout.LayoutParams(0, dp(58), 1f).apply { leftMargin = dp(9) })
        root.addView(trade, topMargin(11))

        root.addView(label("BITCOIN / EUR • СВЕЧИ", 20, "#FFB347", true), topMargin(24))
        btcInfo = label("ВЕРХ —  НИЗ —  СЕЙЧАС —", 15, "#F0F6FC", true)
        root.addView(btcInfo, topMargin(5))
        root.addView(intervalControls(), topMargin(10))
        btcChart = FusionCandleChartView(this)
        root.addView(btcChart, LinearLayout.LayoutParams(-1, dp(265)).apply { topMargin = dp(8) })

        root.addView(label("PUMP / EUR • BITPANDA FUSION", 20, "#3BDD8C", true), topMargin(23))
        pumpInfo = label("ВЕРХ —  НИЗ —  СЕЙЧАС —", 15, "#F0F6FC", true)
        root.addView(pumpInfo, topMargin(5))

        root.addView(intervalControls(), topMargin(10))
        pumpChart = FusionCandleChartView(this)
        root.addView(pumpChart, LinearLayout.LayoutParams(-1, dp(315)).apply { topMargin = dp(8) })
        root.addView(label("Синяя линия — цена входа. Свечи и котировки: Bitpanda Fusion. Стратегия V6.9 работает отдельно.", 11, "#8B949E", false), topMargin(8))
        flowPanel = FusionMoneyFlowPanel(this)
        root.addView(flowPanel, topMargin(16))
        updateSpeedButtons()
    }

    /** The two sets of interval buttons deliberately control a single shared 24h window. */
    private fun intervalControls(): LinearLayout {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("5m" to "5 МИН", "15m" to "15 МИН",
            "30m" to "30 МИН", "1h" to "1 ЧАС"
        ).forEachIndexed { i, (code, label) ->
            val button = button(label, "#27313B").apply { textSize = 12f }
            button.setOnClickListener { chooseInterval(code) }
            row.addView(button, LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                if (i > 0) leftMargin = dp(4)
            })
            speedButtons.getOrPut(code) { mutableListOf() }.add(button)
        }
        return row
    }

    private fun chooseInterval(code: String) {
        if (interval == code) return
        interval = code
        quote = null
        render()
        getSharedPreferences("fusion_compact_display_v1", MODE_PRIVATE).edit()
            .putString("interval", interval).apply()
        updateSpeedButtons()
        refreshMarket()
    }

    private fun updateSpeedButtons() {
        speedButtons.forEach { (code, buttons) ->
            buttons.forEach { button ->
                button.setBackgroundColor(Color.parseColor(if (code == interval) "#1B6F40" else "#27313B"))
            }
        }
    }

    private fun refreshMarket() {
        if (!fetching.compareAndSet(false, true)) return
        val currentInterval = interval
        if (BitpandaFusionSecureKeyStore.read(this).isBlank()) {
            quote = null
            fetchError = "Введите Fusion API-ключ с разрешением Read"
            fetching.set(false)
            render()
            return
        }
        executor.execute {
            val current = quote.takeIf { currentInterval == interval }
            val result = runCatching {
                market.load(applicationContext, currentInterval, current)
            }
            runOnUiThread {
                fetching.set(false)
                if (isFinishing || isDestroyed) return@runOnUiThread
                result.onSuccess {
                    if (currentInterval == interval) {
                        quote = it
                        fetchError = ""
                    } else refreshMarket()
                }.onFailure {
                    // Never log tokens or API response bodies.
                    fetchError = it.message?.take(120) ?: "Ошибка получения данных Bitpanda"
                }
                render()
            }
        }
    }

    private fun render() {
        val q = quote?.takeIf { it.fresh() }
        status.text = when {
            BitpandaFusionSecureKeyStore.read(this).isBlank() ->
                "Ключ не введён • нажмите BITPANDA • КЛЮЧ READ"
            q != null -> "Bitpanda Fusion • 5 с на экране / 60 с в фоне • " +
                java.text.SimpleDateFormat("HH:mm:ss", Locale.GERMANY).format(java.util.Date(q.updatedAt))
            else -> "ДАННЫЕ НЕАКТУАЛЬНЫ • ${fetchError.ifBlank { "ожидание ответа Bitpanda" }}"
        }
        status.setTextColor(Color.parseColor(if (q != null) "#7EE787" else "#FFB86C"))
        val snap = PumpBotEngine.snapshot(this)
        val inPosition = snap.waitMode == "SELL" && snap.entryPrice > 0.0
        if (inPosition) {
            val currentBid = q?.pumpBid
            val change = currentBid?.let {
                FusionPositionFees.pnlPercent(snap.entryPrice, it)
            }
            position.text = if (change != null)
                String.format(Locale.GERMANY,
                    "%+.2f%%\nВход €%.8f → BID €%.8f\nВключены комиссии: 0,25%% покупка + 0,25%% продажа",
                    change, snap.entryPrice, currentBid)
            else String.format(Locale.GERMANY, "ПОЗИЦИЯ ОТКРЫТА\nВход €%.8f\nЖдём свежую цену Bitpanda", snap.entryPrice)
            position.setTextColor(Color.parseColor(if (change == null || change >= 0.0) "#7EE787" else "#FF7B72"))
        } else {
            position.text = "МОЯ ПОЗИЦИЯ НЕ ОТКРЫТА"
            position.setTextColor(Color.parseColor("#7EE787"))
        }
        buyButton.isEnabled = q != null && !inPosition
        sellButton.isEnabled = q != null && inPosition
        buyButton.alpha = if (buyButton.isEnabled) 1f else .45f
        sellButton.alpha = if (sellButton.isEnabled) 1f else .45f

        if (q == null) {
            btcInfo.text = "ВЕРХ —   НИЗ —   СЕЙЧАС —"
            pumpInfo.text = "ВЕРХ —   НИЗ —   СЕЙЧАС —"
            val now = System.currentTimeMillis()
            btcChart.render(emptyList(), 30L * 60_000L, now, isBitcoin = true)
            pumpChart.render(emptyList(), 30L * 60_000L, now, isBitcoin = false)
            return
        }
        val now = System.currentTimeMillis()
        val width = when (interval) {
            "5m" -> 5L * 60_000L
            "15m" -> 15L * 60_000L
            "1h" -> 60L * 60_000L
            else -> 30L * 60_000L
        }
        val dayStart = now - 48L * width
        val btc24 = q.btc.filter { it.timestamp >= dayStart }
        val pump24 = q.pump.filter { it.timestamp >= dayStart }
        if (btc24.isEmpty() || pump24.isEmpty()) {
            btcInfo.text = "Свечи Bitpanda за последние сутки недоступны"
            pumpInfo.text = "Свечи Bitpanda за последние сутки недоступны"
            return
        }
        val pumpHigh = pump24.maxOf { it.high }
        val pumpLow = pump24.minOf { it.low }
        val btcHigh = btc24.maxOf { it.high }
        val btcLow = btc24.minOf { it.low }
        btcInfo.text = String.format(
            Locale.GERMANY,
            "ИНТЕРВАЛ %s   ВЕРХ €%,.0f   НИЗ €%,.0f\nСЕЙЧАС €%,.2f",
            interval, btcHigh, btcLow, q.btcMid
        )
        pumpInfo.text = String.format(
            Locale.GERMANY,
            "ИНТЕРВАЛ %s   ВЕРХ €%.8f   НИЗ €%.8f\nСЕЙЧАС €%.8f  ·  BID €%.8f / ASK €%.8f",
            interval, pumpHigh, pumpLow, q.pumpMid, q.pumpBid, q.pumpAsk
        )
        btcChart.render(btc24, width, now, isBitcoin = true,
            currentPrice = q.btcMid)
        pumpChart.render(pump24, width, now, isBitcoin = false,
            entryPrice = snap.entryPrice.takeIf { inPosition },
            currentPrice = q.pumpMid)
    }

    private fun recordTrade(buy: Boolean) {
        val q = quote?.takeIf { it.fresh() } ?: run {
            Toast.makeText(this, "Нет свежих котировок Bitpanda — сделка не записана", Toast.LENGTH_LONG).show()
            refreshMarket()
            return
        }
        val snap = PumpBotEngine.snapshot(this)
        if ((snap.waitMode == "BUY") != buy) {
            render()
            return
        }
        val suggested = if (buy) q.pumpAsk else q.pumpBid
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true)
            setText(String.format(Locale.US, "%.8f", suggested))
            selectAll()
        }
        AlertDialog.Builder(this)
            .setTitle(if (buy) "Я КУПИЛ • PUMP/EUR" else "Я ПРОДАЛ • PUMP/EUR")
            .setMessage("Введите фактическую цену исполнения с Bitpanda. Предложена котировка ${if (buy) "ASK" else "BID"}; она может отличаться от цены сделки.")
            .setView(input)
            .setPositiveButton("СОХРАНИТЬ", null)
            .setNegativeButton("ОТМЕНА", null)
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val value = input.text.toString().trim().replace(',', '.').toDoubleOrNull()
                        if (value == null || !value.isFinite() || value <= 0.0) {
                            input.error = "Введите правильную цену"
                            return@setOnClickListener
                        }
                        val current = PumpBotEngine.snapshot(this)
                        if ((current.waitMode == "BUY") != buy) {
                            dialog.dismiss()
                            render()
                            return@setOnClickListener
                        }
                        val at = System.currentTimeMillis()
                        if (buy) {
                            PumpBotEngine.confirmBought(this, value, at)
                            PersonalPositionGuardStore.open(this, value, at)
                            ManualPositionStore.recordBuy(this, value, at)
                            UserPaperStore.recordBuy(this, value, at)
                            chooseInterval("30m")
                        } else {
                            ManualPositionStore.recordSell(this, value, at)
                            UserPaperStore.recordSell(this, value, at)
                            PumpBotEngine.confirmSold(this)
                            PositionSupervisorStore.clearPosition(this)
                            GeminiPositionAdvisorStore.clearPosition(this)
                            PersonalPositionGuardStore.clear(this)
                            PumpAlert.clearPersonalPositionAlerts(this)
                        }
                        dialog.dismiss()
                        render()
                    }
                }
                dialog.show()
            }
    }

    private fun topMargin(value: Int) =
        LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(value) }

    private fun label(value: String, size: Int, color: String, bold: Boolean) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(Color.parseColor(color))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    private fun button(value: String, color: String) =
        Button(this).apply {
            text = value
            setAllCaps(false)
            setTextColor(Color.WHITE)
            textSize = 14f
            setBackgroundColor(Color.parseColor(color))
        }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
