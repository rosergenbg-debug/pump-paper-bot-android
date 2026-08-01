package com.example.pumppaperbot

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val refreshUi = object : Runnable {
        override fun run() {
            updateUi()
            handler.postDelayed(this, 5000)
        }
    }

    private var tvStatus: TextView? = null
    private var tvLatestSignal: TextView? = null
    private var tvBuySignal: TextView? = null
    private var tvSellSignal: TextView? = null
    private var tvMode: TextView? = null
    private var tvReadiness: TextView? = null
    private var tvRapidDrop: TextView? = null
    private var tvBreathingState: TextView? = null
    private var tvEnergy: TextView? = null
    private var tvDirection: TextView? = null
    private var tvConfidence: TextView? = null
    private var tvLateRisk: TextView? = null
    private var tvMicrostructure: TextView? = null
    private var tvPrice: TextView? = null
    private var tvReason: TextView? = null
    private var tvPosition: TextView? = null
    private var tvManualPnl: TextView? = null
    private var tvAlertStatus: TextView? = null
    private var chart: StrategyChartView? = null
    private var manualPositionChart: ManualPositionChartView? = null
    private var btnRisk30: Button? = null
    private var btnRisk35: Button? = null
    private var btnStart: Button? = null
    private var btnCheck: Button? = null
    private var btnStop: Button? = null
    private var btnReset: Button? = null
    private var btnManualBuy: Button? = null
    private var btnManualSell: Button? = null
    private var btnManualHistory: Button? = null
    private var btnBacktest: Button? = null
    private var btnAlertSettings: Button? = null
    private var btnAppPaper: Button? = null
    private var btnGeminiExperiment: Button? = null
    private var btnGeminiExitExperiment: Button? = null
    private var btnUserPaper: Button? = null
    private var btnCompetition: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.tvAppTitle).text = "PUMP Сигнал V${BuildConfig.VERSION_NAME}"

        tvStatus = findViewById(R.id.tvStatus)
        tvLatestSignal = findViewById(R.id.tvLatestSignal)
        tvBuySignal = findViewById(R.id.tvBuySignal)
        tvSellSignal = findViewById(R.id.tvSellSignal)
        tvMode = findViewById(R.id.tvMode)
        tvReadiness = findViewById(R.id.tvReadiness)
        tvRapidDrop = findViewById(R.id.tvRapidDrop)
        tvBreathingState = findViewById(R.id.tvBreathingState)
        tvEnergy = findViewById(R.id.tvEnergy)
        tvDirection = findViewById(R.id.tvDirection)
        tvConfidence = findViewById(R.id.tvConfidence)
        tvLateRisk = findViewById(R.id.tvLateRisk)
        tvMicrostructure = findViewById(R.id.tvMicrostructure)
        tvPrice = findViewById(R.id.tvPrice)
        tvReason = findViewById(R.id.tvReason)
        tvPosition = findViewById(R.id.tvPosition)
        tvManualPnl = findViewById(R.id.tvManualPnl)
        tvAlertStatus = findViewById(R.id.tvAlertStatus)
        chart = findViewById(R.id.chart)
        manualPositionChart = findViewById(R.id.manualPositionChart)
        btnRisk30 = findViewById(R.id.btnRisk30)
        btnRisk35 = findViewById(R.id.btnRisk35)
        btnStart = findViewById(R.id.btnStart)
        btnCheck = findViewById(R.id.btnCheck)
        btnStop = findViewById(R.id.btnStop)
        btnReset = findViewById(R.id.btnReset)
        btnManualBuy = findViewById(R.id.btnManualBuy)
        btnManualSell = findViewById(R.id.btnManualSell)
        btnManualHistory = findViewById(R.id.btnManualHistory)
        btnBacktest = findViewById(R.id.btnBacktest)
        btnAlertSettings = findViewById(R.id.btnAlertSettings)
        btnAppPaper = findViewById(R.id.btnAppPaper)
        btnGeminiExperiment = findViewById(R.id.btnGeminiExperiment)
        btnGeminiExitExperiment = findViewById(R.id.btnGeminiExitExperiment)
        btnUserPaper = findViewById(R.id.btnUserPaper)
        btnCompetition = findViewById(R.id.btnCompetition)

        PumpBotEngine.ensureInitialized(this)
        requestNotificationPermission()
        if (PumpBotEngine.snapshot(this).running) {
            ContextCompat.startForegroundService(this, Intent(this, PumpSignalService::class.java))
            schedulePeriodicMonitor()
        }

        btnRisk30?.setOnClickListener {
            PumpBotEngine.setAggressive(this, false)
            updateUi()
            checkNow()
        }
        btnRisk35?.setOnClickListener {
            PumpBotEngine.setAggressive(this, true)
            updateUi()
            checkNow()
        }
        btnStart?.setOnClickListener { startMonitor() }
        btnCheck?.setOnClickListener { checkNow() }
        btnStop?.setOnClickListener {
            confirm("Остановить монитор?", "Проверка PUMP и звуковые сигналы будут остановлены.") {
                stopMonitor()
            }
        }
        btnReset?.setOnClickListener {
            confirm("Сбросить состояние?", "Очистится режим ожидания, цена входа и сохраненные данные графика.") {
                resetAll()
            }
        }
        btnManualBuy?.setOnClickListener { confirmManualBuy() }
        btnManualSell?.setOnClickListener { confirmManualSell() }
        btnManualHistory?.setOnClickListener { showManualHistory() }
        btnBacktest?.setOnClickListener { startActivity(Intent(this, BacktestActivity::class.java)) }
        btnAlertSettings?.setOnClickListener { startActivity(Intent(this, AlertSettingsActivity::class.java)) }
        btnAppPaper?.setOnClickListener {
            startActivity(Intent(this, AppPaperActivity::class.java))
        }
        btnGeminiExperiment?.setOnClickListener {
            startActivity(Intent(this, GeminiExperimentActivity::class.java))
        }
        btnGeminiExitExperiment?.setOnClickListener {
            startActivity(Intent(this, GeminiExitExperimentActivity::class.java))
        }
        btnUserPaper?.setOnClickListener {
            startActivity(Intent(this, AppPaperActivity::class.java))
        }
        btnCompetition?.setOnClickListener {
            startActivity(Intent(this, CompetitionActivity::class.java))
        }
        chart?.setOnClickListener { startActivity(Intent(this, ChartDetailActivity::class.java)) }

        updateUi()
        checkNow()
    }

    override fun onResume() {
        super.onResume()
        updateUi()
        handler.post(refreshUi)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshUi)
        super.onPause()
    }

    private fun startMonitor() {
        PumpBotEngine.setRunning(this, true)
        PumpAlert.ensureChannels(this)
        ContextCompat.startForegroundService(this, Intent(this, PumpSignalService::class.java))
        schedulePeriodicMonitor()
        updateUi()
    }

    private fun schedulePeriodicMonitor() {
        val request = PeriodicWorkRequestBuilder<PumpBotWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraints())
            .setInputData(workDataOf(
                PumpBotWorker.INPUT_CYCLE_SOURCE to "ANDROID РЕЗЕРВ 15 МИН",
                PumpBotWorker.INPUT_CYCLE_INTERVAL to TimeUnit.MINUTES.toMillis(15)
            ))
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PumpBotEngine.uniqueWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun stopMonitor() {
        PumpBotEngine.setRunning(this, false)
        stopService(Intent(this, PumpSignalService::class.java))
        WorkManager.getInstance(this).cancelUniqueWork(PumpBotEngine.uniqueWorkName)
        updateUi()
    }

    private fun resetAll() {
        stopService(Intent(this, PumpSignalService::class.java))
        WorkManager.getInstance(this).cancelUniqueWork(PumpBotEngine.uniqueWorkName)
        PumpBotEngine.reset(this)
        UserPaperStore.discardOpenPosition(this)
        ManualPositionStore.discardOpenPosition(this)
        updateUi()
        checkNow()
    }

    private fun checkNow() {
        val interval = if (PumpBotEngine.snapshot(this).running) {
            TimeUnit.MINUTES.toMillis(2)
        } else {
            TimeUnit.MINUTES.toMillis(15)
        }
        val request = OneTimeWorkRequestBuilder<PumpBotWorker>()
            .setConstraints(networkConstraints())
            .setInputData(workDataOf(
                PumpBotWorker.INPUT_CYCLE_SOURCE to "РУЧНАЯ ПРОВЕРКА",
                PumpBotWorker.INPUT_CYCLE_INTERVAL to interval
            ))
            .build()
        WorkManager.getInstance(this).enqueue(request)
        handler.postDelayed({ updateUi() }, 2000)
        handler.postDelayed({ updateUi() }, 6000)
    }

    private fun confirmManualBuy() {
        val snapshot = PumpBotEngine.snapshot(this)
        if (snapshot.waitMode != "BUY") return
        confirm("Я купил", "Запомнить текущую цену как цену покупки?") {
            if (PumpBotEngine.snapshot(this).waitMode != "BUY") return@confirm
            PumpBotEngine.confirmBought(this)
            val opened = PumpBotEngine.snapshot(this)
            ManualPositionStore.recordBuy(
                this,
                opened.entryPrice,
                opened.entryTime.takeIf { it > 0L } ?: System.currentTimeMillis()
            )
            UserPaperStore.recordBuy(
                this,
                opened.entryPrice,
                opened.entryTime.takeIf { it > 0L } ?: System.currentTimeMillis()
            )
            if (!opened.running) startMonitor()
            updateUi()
        }
    }

    private fun confirmManualSell() {
        val snapshot = PumpBotEngine.snapshot(this)
        if (snapshot.waitMode != "SELL") return
        confirm("Я продал", "Закрыть позицию полностью и снова ждать покупку?") {
            val current = PumpBotEngine.snapshot(this)
            if (current.waitMode != "SELL") return@confirm
            val sellPrice = current.lastPrice.takeIf { it > 0.0 } ?: current.entryPrice
            val soldAt = System.currentTimeMillis()
            ManualPositionStore.recordSell(this, sellPrice, soldAt)
            UserPaperStore.recordSell(this, sellPrice, soldAt)
            PumpBotEngine.confirmSold(this)
            updateUi()
        }
    }

    private fun showManualHistory() {
        val trades = ManualPositionStore.trades(this)
        val text = if (trades.isEmpty()) {
            "За последние 6 месяцев ручных покупок и продаж нет."
        } else {
            trades.asReversed().joinToString("\n\n") { trade ->
                if (trade.closed) {
                    String.format(
                        Locale.GERMANY,
                        "%s  BUY €%.8f\n%s  SELL €%.8f\nИтог %+.2f%%",
                        PumpBotEngine.formatDate(trade.boughtAt),
                        trade.buyPrice,
                        PumpBotEngine.formatDate(trade.soldAt),
                        trade.sellPrice,
                        trade.profitPercent
                    )
                } else {
                    String.format(
                        Locale.GERMANY,
                        "%s  BUY €%.8f\nПОЗИЦИЯ ОТКРЫТА",
                        PumpBotEngine.formatDate(trade.boughtAt),
                        trade.buyPrice
                    )
                }
            }
        }
        AlertDialog.Builder(this)
            .setTitle("Мои сделки • последние 6 месяцев")
            .setMessage(text)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    private fun confirm(title: String, message: String, action: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Подтвердить") { _, _ -> action() }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateUi() {
        val snapshot = PumpBotEngine.snapshot(this)
        val accountPrice = snapshot.lastPrice
        val appAccount = AppPaperStore.state(this)
        val geminiAccount = GeminiPaperStore.state(this).portfolio
        val geminiExitExperiment = GeminiExitExperimentStore.state(this)?.portfolio
            ?: geminiAccount
        val sergeAccount = UserPaperStore.markToMarket(this, accountPrice)
        btnAppPaper?.text = accountButtonText(
            "APP",
            appAccount.value(accountPrice),
            appAccount.profitPercent(accountPrice)
        )
        btnGeminiExperiment?.text = accountButtonText(
            "GEMINI",
            geminiAccount.value(accountPrice),
            geminiAccount.profitPercent(accountPrice)
        )
        btnGeminiExitExperiment?.text = accountButtonText(
            "GEMINI‑ЭКСП.",
            geminiExitExperiment.value(accountPrice),
            geminiExitExperiment.profitPercent(accountPrice)
        )
        btnUserPaper?.text = accountButtonText(
            "СЕРЖ",
            sergeAccount.value(accountPrice),
            sergeAccount.profitPercent(accountPrice)
        )
        tvStatus?.text = if (snapshot.running) {
            "Монитор включён • обновлено ${PumpBotEngine.formatTime(snapshot.lastSync)}"
        } else {
            "Монитор остановлен • последнее обновление ${PumpBotEngine.formatTime(snapshot.lastSync)}"
        }
        renderLatestSignal()
        tvMode?.text = if (snapshot.rapidDrop.active) {
            String.format(Locale.GERMANY, "АВАРИЙНОЕ ПАДЕНИЕ −%.1f%% — ПРОВЕРЬТЕ РЫНОК", snapshot.rapidDrop.dropPercent)
        } else if (snapshot.lateEntryBlocked && snapshot.waitMode == "BUY") {
            "ВХОД ЗАБЛОКИРОВАН — ЦЕНА УЖЕ ВЫСОКО"
        } else if (snapshot.marketGateActive && snapshot.waitMode == "BUY") {
            "РЫНОК ПЕРЕГРЕТ — НЕ ДОГОНЯЕМ ЦЕНУ"
        } else if (snapshot.waitMode == "BUY") {
            "ЖДЁМ НОВЫЙ ВХОД СНИЗУ"
        } else {
            "МОНЕТА КУПЛЕНА — ЖДЁМ ВЫХОД"
        }
        tvMode?.setTextColor(Color.parseColor("#F0F6FC"))
        tvMode?.setBackgroundColor(
            Color.parseColor(if (snapshot.rapidDrop.active || snapshot.marketGateActive || snapshot.lateEntryBlocked) "#9E2A2B" else "#30363D")
        )

        renderRapidDrop(snapshot)
        renderReadiness(snapshot)
        renderBreathing(snapshot)

        renderStrategyButtons(snapshot.aggressive)
        renderSignalBox(tvBuySignal, "BUY", snapshot.buySignal, snapshot.waitMode == "BUY")
        renderSignalBox(tvSellSignal, "SELL", snapshot.sellSignal, snapshot.waitMode == "SELL")

        tvPrice?.text = String.format(
            Locale.US,
            "Цена €%.8f • свеча %s\nRSI %.1f • EMA200 %.8f • funding %+.5f%%",
            snapshot.lastPrice,
            PumpBotEngine.formatTime(snapshot.lastCandle),
            snapshot.lastRsi,
            snapshot.lastEma200,
            snapshot.fundingRate * 100.0
        )
        tvReason?.text = if (snapshot.weekRhythm.caution) {
            "${snapshot.signalReason}\n${snapshot.weekRhythm.title}. ${snapshot.weekRhythm.explanation}"
        } else {
            snapshot.signalReason
        }
        tvReason?.setTextColor(
            when {
                snapshot.marketGateActive -> Color.parseColor("#F0B72F")
                snapshot.signalReason.startsWith("СЕЙЧАС НЕ ПОКУПАТЬ") -> Color.parseColor("#FF7B72")
                snapshot.readinessScore >= 95 -> Color.parseColor("#7EE787")
                snapshot.readinessScore <= -95 -> Color.parseColor("#FF7B72")
                else -> Color.parseColor("#C9D1D9")
            }
        )

        tvPosition?.text = if (snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0) {
            String.format(
                Locale.US,
                "Позиция: %s | вход %.8f EUR | максимум %.8f | частично продано: %s",
                friendlyMode(snapshot.strategyMode),
                snapshot.entryPrice,
                snapshot.highestClose,
                if (snapshot.partialTaken) "да" else "нет"
            )
        } else {
            "Сделка: не открыта"
        }
        renderManualPosition(snapshot)
        tvAlertStatus?.text = AlertSchedule.statusText(this)

        btnStart?.isEnabled = !snapshot.running
        btnStart?.alpha = if (snapshot.running) 0.45f else 1f
        btnStop?.isEnabled = snapshot.running
        btnStop?.alpha = if (snapshot.running) 1f else 0.65f
        val controls = ManualPositionControlPolicy.forWaitMode(snapshot.waitMode)
        btnManualBuy?.isEnabled = controls.buyEnabled
        btnManualBuy?.alpha = if (controls.buyEnabled) 1f else 0.35f
        btnManualSell?.isEnabled = controls.sellEnabled
        btnManualSell?.alpha = if (controls.sellEnabled) 1f else 0.35f
        val now = System.currentTimeMillis()
        val radar = EventRadarStore.state(this)
        val appCombinedDirection = radar.combinedDirection(snapshot.directionScore, now)
        val geminiState = GeminiPaperStore.state(this)
        val currentGeminiDecision = GeminiGaugePolicy.currentDecision(geminiState, now)
        chart?.setData(
            "PUMP/EUR • ДЫХАНИЕ РЫНКА",
            snapshot.chart.copy(
                directionScore = appCombinedDirection,
                showGeminiGauge = true,
                geminiDirectionScore = currentGeminiDecision?.directionScore,
                geminiConfidenceScore = currentGeminiDecision?.confidence ?: 0,
                geminiAction = currentGeminiDecision?.requestedAction.orEmpty(),
                geminiStatus = GeminiHourlyRetryPolicy.visibleStatus(geminiState, now)
            )
        )
    }

    private fun renderManualPosition(snapshot: LiveSnapshot) {
        val active = snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0
        if (active) {
            ManualPositionStore.ensureOpenPosition(
                this,
                snapshot.entryPrice,
                snapshot.entryTime.takeIf { it > 0L } ?: snapshot.lastCandle
            )
        }
        val trade = ManualPositionStore.openTrade(this)
        val entry = trade?.buyPrice?.takeIf { it > 0.0 } ?: snapshot.entryPrice
        val boughtAt = trade?.boughtAt
            ?: snapshot.entryTime.takeIf { it > 0L }
            ?: snapshot.lastCandle
        if (active && entry > 0.0 && snapshot.lastPrice > 0.0) {
            val pnl = (snapshot.lastPrice / entry - 1.0) * 100.0
            val color = if (pnl >= 0.0) "#7EE787" else "#FF7B72"
            tvManualPnl?.text = String.format(
                Locale.GERMANY,
                "МОЯ ПОЗИЦИЯ  %+.2f%%\n€%.8f → €%.8f",
                pnl,
                entry,
                snapshot.lastPrice
            )
            tvManualPnl?.setTextColor(Color.parseColor(color))
        } else {
            tvManualPnl?.text = "МОЯ ПОЗИЦИЯ НЕ ОТКРЫТА"
            tvManualPnl?.setTextColor(Color.parseColor("#79C0FF"))
        }
        manualPositionChart?.setPosition(
            candles = snapshot.chart.candles,
            boughtAt = boughtAt,
            buyPrice = entry,
            currentPrice = snapshot.lastPrice
        )
    }

    private fun networkConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private fun accountButtonText(name: String, value: Double, percent: Double): String =
        String.format(
            Locale.GERMANY,
            "%s\n€%,.2f\n%+.2f%%",
            name,
            value,
            percent
        )

    private fun renderLatestSignal() {
        val latest = SignalAttributionStore.latest(this)
        if (latest == null) {
            tvLatestSignal?.text = "ПОСЛЕДНИЙ СИГНАЛ\nПока подписанных сигналов нет"
            tvLatestSignal?.setTextColor(Color.parseColor("#8B949E"))
            return
        }
        val tradeMark = if (latest.executedTrade) " • СДЕЛКА ВЫПОЛНЕНА" else " • БЕЗ СДЕЛКИ"
        tvLatestSignal?.text = "${latest.source} • ${latest.kind}$tradeMark\n" +
            "${latest.reason}\n${PumpBotEngine.formatTime(latest.at)}"
        tvLatestSignal?.setTextColor(
            Color.parseColor(if (latest.executedTrade) "#7EE787" else "#DDE7F7")
        )
    }

    private fun renderRapidDrop(snapshot: LiveSnapshot) {
        val drop = snapshot.rapidDrop
        if (!drop.active) {
            tvRapidDrop?.visibility = View.GONE
            return
        }
        tvRapidDrop?.visibility = View.VISIBLE
        val action = when {
            snapshot.waitMode == "SELL" -> "ПОЗИЦИЯ ОТКРЫТА — СРОЧНО ПРОВЕРЬТЕ СТОП И ЦЕНУ ВЫХОДА"
            drop.recoveryConfirmed -> String.format(
                Locale.GERMANY,
                "ОТСКОК +%.1f%% ЕСТЬ, НО ЖДЁМ ОБЫЧНЫЙ СИГНАЛ 99/100",
                drop.reboundPercent
            )
            else -> "ПАДЕНИЕ НЕ ОСТАНОВИЛОСЬ — НЕ ПОКУПАТЬ АВТОМАТИЧЕСКИ"
        }
        tvRapidDrop?.text = String.format(
            Locale.GERMANY,
            "РЕЗКОЕ ПАДЕНИЕ −%.1f%%\nмаксимум €%.8f → сейчас €%.8f\n%s",
            drop.dropPercent,
            drop.peakPrice,
            drop.currentPrice,
            action
        )
    }

    private fun renderBreathing(snapshot: LiveSnapshot) {
        val radar = EventRadarStore.state(this)
        val information = radar.informationAdjustment()
        val combined = radar.combinedDirection(snapshot.directionScore)
        tvBreathingState?.text = "ДЫХАНИЕ: ${snapshot.breathingState}\n${snapshot.marketRelation}"
        tvBreathingState?.setTextColor(
            Color.parseColor(
                when {
                    snapshot.lateEntryBlocked -> "#FF7B72"
                    combined >= 25 -> "#7EE787"
                    combined <= -25 -> "#FF7B72"
                    else -> "#79C0FF"
                }
            )
        )
        tvEnergy?.text = "АКТИВНОСТЬ\n${snapshot.energyScore}/100\nсжатие ${snapshot.compressionScore}"
        tvDirection?.text = "ОБЩИЙ ФОН ${signed(combined)}/100\n" +
            "алг ${signed(snapshot.directionScore)} • ИИ ${signed(information)}\n" +
            if (information == 0) "новость не влияет" else "поправка наблюдения"
        tvDirection?.setTextColor(Color.parseColor(if (combined >= 20) "#7EE787" else if (combined <= -20) "#FF7B72" else "#C9D1D9"))
        tvConfidence?.text = "СОГЛАСОВАНО\n${snapshot.breathingConfidence}/100\nне шанс прибыли"
        tvLateRisk?.text = "ПОЗДНИЙ ВХОД\n${snapshot.lateEntryRisk}/100\n${if (snapshot.lateEntryBlocked) "ЗАПРЕЩЁН" else "допустимо"}"
        tvLateRisk?.setTextColor(Color.parseColor(if (snapshot.lateEntryBlocked) "#FF7B72" else if (snapshot.lateEntryRisk >= 45) "#F0B72F" else "#7EE787"))

        val book = snapshot.bookImbalance?.let {
            val side = if (it >= 0.0) "покупатели" else "продавцы"
            "стакан: $side ${String.format(Locale.GERMAN, "%+.0f%%", it * 100.0)}"
        } ?: "стакан: нет данных"
        val spread = snapshot.spreadPercent?.let { "spread ${String.format(Locale.GERMAN, "%.3f%%", it)}" } ?: "spread —"
        val oi = snapshot.openInterestChangePercent?.let { "OI ${String.format(Locale.GERMAN, "%+.2f%%", it)} с прошлой проверки" }
            ?: snapshot.openInterest?.let { "OI собирается для сравнения" }
            ?: "OI: нет данных"
        val impulse = ImpulseRadarStore.state(this)
        val impulseValues = if (impulse.candleTime > 0L) {
            val volume = impulse.volumeRatio?.let { String.format(Locale.GERMAN, "объём ×%.1f", it) } ?: "объём —"
            val flow = impulse.spotTakerRatio?.let { String.format(Locale.GERMAN, "spot %.0f%%", it * 100.0) } ?: "spot —"
            val oi10 = impulse.openInterestChange10m?.let { String.format(Locale.GERMAN, "OI10 %+.2f%%", it * 100.0) } ?: "OI10 —"
            "5m SHADOW ${impulse.readiness}/100 • $volume • $flow • $oi10"
        } else {
            "5m SHADOW: ждём первую синхронизацию"
        }
        tvMicrostructure?.text = "$book • $spread • $oi\n$impulseValues\n${impulse.status}"
    }

    private fun signed(value: Int): String = if (value >= 0) "+$value" else "−${kotlin.math.abs(value)}"

    private fun renderReadiness(snapshot: LiveSnapshot) {
        val score = snapshot.readinessScore
        if (snapshot.rapidDrop.active && snapshot.waitMode == "BUY" && !snapshot.rapidDrop.recoveryConfirmed) {
            tvReadiness?.text = "АВАРИЙНЫЙ РЕЖИМ\nПОКУПКА ЕЩЁ НЕ ПОДТВЕРЖДЕНА\nждём остановку падения"
            tvReadiness?.setTextColor(Color.parseColor("#FFFFFF"))
            tvReadiness?.setBackgroundColor(Color.parseColor("#4A1418"))
            return
        }
        if (snapshot.lateEntryBlocked && snapshot.waitMode == "BUY") {
            tvReadiness?.text = "ПОКУПКА ЗАПРЕЩЕНА\nРИСК ПОЗДНЕГО ВХОДА ${snapshot.lateEntryRisk}/100\nждём новый вход снизу"
            tvReadiness?.setTextColor(Color.parseColor("#FF7B72"))
            tvReadiness?.setBackgroundColor(Color.parseColor("#321A1D"))
            return
        }
        if (snapshot.marketGateActive) {
            tvReadiness?.text = "ПАУЗА ПОКУПКИ\nPUMP + BTC + SOL резко выросли за 1 час\nждём новую закрытую свечу"
            tvReadiness?.setTextColor(Color.parseColor("#F0B72F"))
            tvReadiness?.setBackgroundColor(Color.parseColor("#2D240F"))
            return
        }
        tvReadiness?.text = when {
            score >= 100 -> "+100  ПОКУПАТЬ\nУСЛОВИЯ ПОДТВЕРЖДЕНЫ"
            score == 99 -> "+99  ЗВОНОК: ПРИГОТОВИТЬСЯ\nдо покупки остался 1 балл"
            score >= 95 -> "+$score  СИГНАЛ ПРИБЛИЖАЕТСЯ\nзвонок будет на 99"
            score <= -100 -> "−100  ПРОДАВАТЬ\nУСЛОВИЯ ПОДТВЕРЖДЕНЫ"
            score == -99 -> "−99  ЗВОНОК: ПРИГОТОВИТЬСЯ\nдо продажи остался 1 балл"
            score <= -95 -> "−${kotlin.math.abs(score)}  СИГНАЛ ПРИБЛИЖАЕТСЯ\nзвонок будет на −99"
            score < 0 -> "ПРОДАЖА НЕ ПОДТВЕРЖДЕНА\nготовность ${kotlin.math.abs(score)}/100"
            else -> "ПОКУПКА НЕ ПОДТВЕРЖДЕНА\nготовность $score/100"
        }
        tvReadiness?.setTextColor(
            when {
                score >= 95 -> Color.parseColor("#7EE787")
                score <= -95 -> Color.parseColor("#FF7B72")
                else -> Color.parseColor("#F0B72F")
            }
        )
        tvReadiness?.setBackgroundColor(if (kotlin.math.abs(score) >= 95) Color.parseColor("#202A22") else Color.parseColor("#161B22"))
    }

    private fun renderStrategyButtons(aggressive: Boolean) {
        btnRisk30?.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (!aggressive) "#238636" else "#30363D"))
        btnRisk35?.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (aggressive) "#B62324" else "#30363D"))
        btnRisk30?.alpha = if (!aggressive) 1f else 0.72f
        btnRisk35?.alpha = if (aggressive) 1f else 0.72f
        btnRisk30?.text = "ОСТОРОЖНЫЙ\nБЛИЖЕ КО ДНУ\nМЕНЬШЕ ВХОДОВ"
        btnRisk35?.text = "АКТИВНЫЙ\nЛОВИТ РАЗВОРОТ\nБОЛЬШЕ ВХОДОВ"
    }

    private fun friendlyMode(mode: String): String {
        return when (mode) {
            StrategyV2.MODE_EXHAUSTION -> "4-этапный разворот"
            StrategyV2.MODE_SHOCK -> "импульс после падения"
            StrategyV2.MODE_TREND -> "трендовый вход"
            else -> "ожидание"
        }
    }

    private fun renderSignalBox(view: TextView?, label: String, signal: Boolean, selectedMode: Boolean) {
        val color = when {
            signal && label == "BUY" -> "#238636"
            signal && label == "SELL" -> "#DA3633"
            selectedMode -> "#30363D"
            else -> "#161B22"
        }
        view?.setBackgroundColor(Color.parseColor(color))
        val display = if (label == "BUY") "Покупка" else "Продажа"
        view?.text = if (signal) "$display: СИГНАЛ" else "$display: нет"
        view?.setTextColor(if (signal) Color.WHITE else Color.parseColor("#8B949E"))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 350)
        }
    }
}
