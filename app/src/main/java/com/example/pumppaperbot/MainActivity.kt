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
import android.widget.Toast
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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val chartExecutor = Executors.newSingleThreadExecutor()
    private val chartSyncRunning = AtomicBoolean(false)
    private val refreshUi = object : Runnable {
        override fun run() {
            updateUi()
            handler.postDelayed(this, 5000)
        }
    }
    private val refreshChart = object : Runnable {
        override fun run() {
            requestChartSync()
            handler.postDelayed(this, ChartSpeedStore.selected(this@MainActivity).refreshMillis)
        }
    }

    private var tvStatus: TextView? = null
    private var tvLatestSignal: TextView? = null
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
    private var tvPositionSupervisor: TextView? = null
    private var tvDeepSeekPrimary: TextView? = null
    private var tvDeepSeekActionLevel: TextView? = null
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
    private var btnChartSpeed: Button? = null
    private var btnBacktest: Button? = null
    private var btnAlertSettings: Button? = null
    private var btnAppPaper: Button? = null
    private var btnGeminiExperiment: Button? = null
    private var btnGeminiExitExperiment: Button? = null
    private var btnUserPaper: Button? = null
    private var btnCompetition: Button? = null
    private var btnCriticalOverview: Button? = null
    private var btnDeepSeekApi: Button? = null
    private var btnGeminiApi: Button? = null
    private var evidenceMemoryDialogVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.tvAppTitle).text = "PUMP Сигнал V${BuildConfig.VERSION_NAME}"

        tvStatus = findViewById(R.id.tvStatus)
        tvLatestSignal = findViewById(R.id.tvLatestSignal)
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
        tvPositionSupervisor = findViewById(R.id.tvPositionSupervisor)
        tvDeepSeekPrimary = findViewById(R.id.tvDeepSeekPrimary)
        tvDeepSeekActionLevel = findViewById(R.id.tvDeepSeekActionLevel)
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
        btnChartSpeed = findViewById(R.id.btnChartSpeed)
        btnBacktest = findViewById(R.id.btnBacktest)
        btnAlertSettings = findViewById(R.id.btnAlertSettings)
        btnAppPaper = findViewById(R.id.btnAppPaper)
        btnGeminiExperiment = findViewById(R.id.btnGeminiExperiment)
        btnGeminiExitExperiment = findViewById(R.id.btnGeminiExitExperiment)
        btnUserPaper = findViewById(R.id.btnUserPaper)
        btnCompetition = findViewById(R.id.btnCompetition)
        btnCriticalOverview = findViewById(R.id.btnCriticalOverview)
        btnDeepSeekApi = findViewById(R.id.btnDeepSeekApi)
        btnGeminiApi = findViewById(R.id.btnGeminiApi)

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
        btnChartSpeed?.setOnClickListener { showChartSpeedDialog() }
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
        btnCriticalOverview?.setOnClickListener {
            startActivity(Intent(this, CriticalOverviewActivity::class.java))
        }
        btnDeepSeekApi?.setOnClickListener {
            startActivity(Intent(this, ApiCenterActivity::class.java).putExtra(
                ApiCenterActivity.EXTRA_PROVIDER, ApiCenterActivity.DEEPSEEK
            ))
        }
        btnGeminiApi?.setOnClickListener {
            startActivity(Intent(this, ApiCenterActivity::class.java).putExtra(
                ApiCenterActivity.EXTRA_PROVIDER, ApiCenterActivity.GEMINI
            ))
        }
        chart?.setOnClickListener { startActivity(Intent(this, ChartDetailActivity::class.java)) }
        chart?.setVisibleBarLimit(mainChartVisibleBarLimit())

        updateUi()
        checkNow()
    }

    override fun onResume() {
        super.onResume()
        updateUi()
        handler.post(refreshUi)
        handler.post(refreshChart)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshUi)
        handler.removeCallbacks(refreshChart)
        super.onPause()
    }

    override fun onDestroy() {
        chartExecutor.shutdownNow()
        super.onDestroy()
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
        PositionSupervisorStore.clearPosition(this)
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
                PumpBotWorker.INPUT_CYCLE_INTERVAL to interval,
                PumpBotWorker.INPUT_FORCE_PRIMARY_DEEPSEEK to true
            ))
            .build()
        WorkManager.getInstance(this).enqueue(request)
        handler.postDelayed({ updateUi() }, 2000)
        handler.postDelayed({ updateUi() }, 6000)
    }

    private fun confirmManualBuy() {
        val snapshot = PumpBotEngine.snapshot(this)
        if (snapshot.waitMode != "BUY") return
        val shownPrice = PaperExecutionPolicy.freshLivePrice(snapshot)
        if (shownPrice == null) {
            Toast.makeText(this, "Свежей цены нет — запускаю новую проверку рынка", Toast.LENGTH_LONG).show()
            checkNow()
            return
        }
        confirm(
            "Я купил",
            String.format(Locale.GERMANY, "Запомнить свежую цену €%.8f как цену покупки?", shownPrice)
        ) {
            val current = PumpBotEngine.snapshot(this)
            if (current.waitMode != "BUY") return@confirm
            val executionPrice = PaperExecutionPolicy.freshLivePrice(current)
            if (executionPrice == null) {
                Toast.makeText(this, "Цена успела устареть — повторите после проверки", Toast.LENGTH_LONG).show()
                checkNow()
                return@confirm
            }
            val confirmedAt = System.currentTimeMillis()
            PumpBotEngine.confirmBought(this, executionPrice, confirmedAt)
            EntryAlertReminderStore.clear(this)
            PersonalPositionGuardStore.open(this, executionPrice, confirmedAt)
            val opened = PumpBotEngine.snapshot(this)
            ChartSpeedStore.selectFastForNewPosition(this, opened.entryTime)
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
            schedulePositionCheck(forceCritical = true)
            requestChartSync()
            updateUi()
        }
    }

    private fun showChartSpeedDialog() {
        val intervals = ChartInterval.entries.toTypedArray()
        val labels = arrayOf(
            "1 МИНУТА — максимально быстро",
            "5 МИНУТ — быстрый обзор",
            "15 МИНУТ — средняя скорость",
            "30 МИНУТ — основной режим стратегии",
            "1 ЧАС — общий тренд"
        )
        val selected = intervals.indexOf(ChartSpeedStore.selected(this)).coerceAtLeast(0)
        val dialog = AlertDialog.Builder(this)
            .setTitle("СКОРОСТЬ ГРАФИКА")
            .setSingleChoiceItems(labels, selected, null)
            .setNegativeButton("Отмена", null)
            .create()
        dialog.setOnShowListener {
            dialog.listView.setOnItemClickListener { _, _, position, _ ->
                ChartSpeedStore.select(this, intervals[position])
                dialog.dismiss()
                updateUi()
                handler.removeCallbacks(refreshChart)
                handler.post(refreshChart)
            }
        }
        dialog.show()
    }

    private fun requestChartSync() {
        if (ChartSpeedStore.selected(this) == ChartInterval.THIRTY_MINUTES) return
        if (!chartSyncRunning.compareAndSet(false, true)) return
        val appContext = applicationContext
        chartExecutor.execute {
            try {
                ChartMarketClient().syncSelected(appContext)
            } finally {
                chartSyncRunning.set(false)
                runOnUiThread { updateUi() }
            }
        }
    }

    private fun confirmManualSell() {
        val snapshot = PumpBotEngine.snapshot(this)
        if (snapshot.waitMode != "SELL") return
        confirm("Я продал", "Закрыть позицию полностью и снова ждать покупку?") {
            val current = PumpBotEngine.snapshot(this)
            if (current.waitMode != "SELL") return@confirm
            val sellPrice = PaperExecutionPolicy.freshLivePrice(current)
                ?: PaperExecutionPolicy.displayPrice(current).takeIf { it > 0.0 }
                ?: current.entryPrice
            val soldAt = System.currentTimeMillis()
            ManualPositionStore.recordSell(this, sellPrice, soldAt)
            UserPaperStore.recordSell(this, sellPrice, soldAt)
            PumpBotEngine.confirmSold(this)
            PositionSupervisorStore.clearPosition(this)
            GeminiPositionAdvisorStore.clearPosition(this)
            PersonalPositionGuardStore.clear(this)
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
        if (snapshot.waitMode == "SELL" && snapshot.entryTime > 0L &&
            ChartSpeedStore.selectFastForNewPosition(this, snapshot.entryTime)
        ) {
            requestChartSync()
        }
        val now = System.currentTimeMillis()
        val chartInterval = ChartSpeedStore.selected(this)
        val displayChart = ChartSpeedStore.chartBundle(this, snapshot, now)
        btnChartSpeed?.text = buildString {
            append("СКОРОСТЬ ГРАФИКА • ${chartInterval.buttonLabel}\n")
            append(if (chartInterval == ChartInterval.ONE_MINUTE) "ЖИВОЙ КРАЙ ≈15 СЕК." else "НАЖМИТЕ, ЧТОБЫ ИЗМЕНИТЬ")
        }
        val accountPrice = PaperExecutionPolicy.displayPrice(snapshot, now)
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
            "DEEPSEEK",
            geminiAccount.value(accountPrice),
            geminiAccount.profitPercent(accountPrice)
        )
        btnGeminiExitExperiment?.text = accountButtonText(
            "DEEPSEEK‑ЭКСП.",
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
        val deepSeekPrimary = DeepSeekPrimaryStore.state(this)
        tvDeepSeekPrimary?.text = DeepSeekPrimaryPolicy.compactStatus(
            deepSeekPrimary,
            DeepSeekSecureKeyStore.read(this).isNotBlank()
        ) + DeepSeekDailyBudgetStore.costUsd(this, now).takeIf {
            DeepSeekCostWarningPolicy.warningReached(it)
        }?.let { String.format(Locale.GERMANY, "\nРАСХОД: $%.2f • предупреждение, анализ продолжается", it) }.orEmpty()
        tvDeepSeekPrimary?.setTextColor(Color.parseColor(
            when {
                deepSeekPrimary.error.isNotBlank() -> "#FF7B72"
                DeepSeekCostWarningPolicy.warningReached(DeepSeekDailyBudgetStore.costUsd(this, now)) -> "#FFD866"
                else -> "#7EE787"
            }
        ))
        renderDeepSeekActionLevel(snapshot, deepSeekPrimary, now)
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
        renderApiButtons()
        maybeShowEvidenceMemoryPrompt()

        val livePrice = PaperExecutionPolicy.freshLivePrice(snapshot, now)
        val priceAge = if (livePrice != null) ((now - snapshot.livePriceAt) / 1000L).coerceAtLeast(0L) else -1L
        tvPrice?.text = String.format(
            Locale.US,
            "%s €%.8f%s • свеча %s\nRSI %.1f • EMA200 %.8f • funding %+.5f%%",
            if (livePrice != null) "Живая цена" else "Закрытие 30м (живая цена недоступна)",
            accountPrice,
            if (priceAge >= 0L) " • возраст ${priceAge}с" else "",
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
        renderPositionSupervisor(snapshot)
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
        val radar = EventRadarStore.state(this)
        val appCombinedDirection = radar.combinedDirection(snapshot.directionScore, now)
        val deepSeekSignal = DeepSeekPrimaryStore.state(this)
        val deepSeekFresh = DeepSeekPrimaryPolicy.isFreshSignal(deepSeekSignal, now)
        chart?.setData(
            "PUMP/EUR • ${chartInterval.buttonLabel}",
            displayChart.copy(
                directionScore = appCombinedDirection,
                showGeminiGauge = true,
                geminiDirectionScore = deepSeekSignal.direction.takeIf { deepSeekFresh },
                geminiConfidenceScore = if (deepSeekFresh) deepSeekSignal.confidence else 0,
                geminiAction = if (deepSeekFresh) deepSeekSignal.action else "STALE",
                geminiStatus = when {
                    deepSeekSignal.error.isNotBlank() -> "ОШИБКА: ${deepSeekSignal.error}"
                    !deepSeekFresh -> "ПОСЛЕДНИЙ СИГНАЛ УСТАРЕЛ"
                    else -> "DEEPSEEK РАБОТАЕТ"
                }
            )
        )
    }

    private fun maybeShowEvidenceMemoryPrompt() {
        if (evidenceMemoryDialogVisible || !DeepSeekEvidenceMemory.shouldPrompt(this)) return
        evidenceMemoryDialogVisible = true
        val status = DeepSeekEvidenceMemory.status(this)
        AlertDialog.Builder(this)
            .setTitle("ПАМЯТЬ DEEPSEEK ПОЧТИ ЗАПОЛНЕНА")
            .setMessage(status.russianSummary() +
                "\n\nДобавить ещё 50 МБ или удалить старые слабые данные? Доказанные закономерности при очистке сохраняются.")
            .setPositiveButton("Добавить 50 МБ") { _, _ ->
                DeepSeekEvidenceMemory.allocateAnotherBlock(this)
                evidenceMemoryDialogVisible = false
                Toast.makeText(this, "Добавлено ещё 50 МБ памяти анализа", Toast.LENGTH_LONG).show()
            }
            .setNeutralButton("Очистить слабые") { _, _ ->
                val appContext = applicationContext
                chartExecutor.execute {
                    val deleted = DeepSeekEvidenceMemory.pruneLowValue(appContext)
                    runOnUiThread {
                        evidenceMemoryDialogVisible = false
                        Toast.makeText(this, "Удалено слабых записей: $deleted", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Решить позже") { _, _ ->
                DeepSeekEvidenceMemory.dismissPrompt(this)
                evidenceMemoryDialogVisible = false
            }
            .setOnCancelListener {
                DeepSeekEvidenceMemory.dismissPrompt(this)
                evidenceMemoryDialogVisible = false
            }
            .show()
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
        val currentPrice = PaperExecutionPolicy.displayPrice(snapshot)
        if (active && entry > 0.0 && currentPrice > 0.0) {
            val pnl = (currentPrice / entry - 1.0) * 100.0
            val color = if (pnl >= 0.0) "#7EE787" else "#FF7B72"
            tvManualPnl?.text = String.format(
                Locale.GERMANY,
                "МОЯ ПОЗИЦИЯ  %+.2f%%\n€%.8f → €%.8f",
                pnl,
                entry,
                currentPrice
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
            currentPrice = currentPrice
        )
    }

    private fun renderPositionSupervisor(snapshot: LiveSnapshot) {
        if (snapshot.waitMode != "SELL") {
            tvPositionSupervisor?.text = "DEEPSEEK • ожидает нажатия «Я купил»"
            tvPositionSupervisor?.setTextColor(Color.parseColor("#8B949E"))
            return
        }
        val state = PositionSupervisorStore.state(this)
        val gemini = GeminiPositionAdvisorStore.state(this)
        val supportPlan = PositionSupervisorPolicy.supportPlan(
            snapshot = snapshot,
            state = state,
            guard = PersonalPositionGuardStore.state(this),
            micro = MicroImpulseStore.state(this),
            forceCritical = false,
            now = System.currentTimeMillis()
        )
        val intervalMinutes = supportPlan.intervalMillis / TimeUnit.MINUTES.toMillis(1)
        tvPositionSupervisor?.text = buildString {
            append(PositionSupervisorPolicy.statusText(state))
            append("\n${state.model.ifBlank { "DeepSeek ещё не вызывался" }} • контроль до ${intervalMinutes} мин")
            if (state.lastSuccess > 0L) append(" • ответ ${PumpBotEngine.formatTime(state.lastSuccess)}")
            append("\n\n${GeminiPositionAdvisorPolicy.statusText(gemini)}")
            append("\n${gemini.model.ifBlank { "Gemini ещё не вызывался" }}")
        }
        val color = when {
            gemini.action == "EXIT" -> "#FF7B72"
            state.action == "CANCEL_EXIT" -> "#7EE787"
            state.exitAdvised -> "#FF7B72"
            else -> "#D2A8FF"
        }
        tvPositionSupervisor?.setTextColor(Color.parseColor(color))
    }

    private fun renderDeepSeekActionLevel(
        snapshot: LiveSnapshot,
        primary: DeepSeekPrimaryState,
        now: Long
    ) {
        val micro = MicroImpulseStore.state(this)
        val level = if (snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0) {
            DeepSeekActionLevelPolicy.fromPosition(
                snapshot,
                PositionSupervisorStore.state(this),
                PersonalPositionGuardStore.state(this),
                micro,
                now
            )
        } else {
            DeepSeekActionLevelPolicy.fromMarket(snapshot, primary, micro, now)
        }
        val phase = if (level.phase == DeepSeekActionPhase.ENTRY) {
            "ГОТОВНОСТЬ ВХОДА"
        } else {
            "ОПАСНОСТЬ ВЫХОДА"
        }
        tvDeepSeekActionLevel?.text = "DEEPSEEK • $phase\n${level.level}/10 • ${level.label}\n${level.detail}"
        val colors = when (level.band) {
            DeepSeekActionBand.RED -> "#FF7B72" to "#3A171A"
            DeepSeekActionBand.YELLOW -> "#FFD866" to "#3A300F"
            DeepSeekActionBand.GREEN -> "#7EE787" to "#15351F"
        }
        tvDeepSeekActionLevel?.setTextColor(Color.parseColor(colors.first))
        tvDeepSeekActionLevel?.setBackgroundColor(Color.parseColor(colors.second))
    }

    private fun schedulePositionCheck(forceCritical: Boolean) {
        val request = OneTimeWorkRequestBuilder<PumpBotWorker>()
            .setConstraints(networkConstraints())
            .setInputData(workDataOf(
                PumpBotWorker.INPUT_CYCLE_SOURCE to "СЕРЖ КУПИЛ • DEEPSEEK PRO",
                PumpBotWorker.INPUT_CYCLE_INTERVAL to TimeUnit.MINUTES.toMillis(2),
                PumpBotWorker.INPUT_FORCE_POSITION_PRO to forceCritical
            ))
            .build()
        WorkManager.getInstance(this).enqueue(request)
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
        val micro = MicroImpulseStore.state(this)
        val liveFlow = if (micro.connected && micro.updatedAt > 0L) {
            String.format(
                Locale.GERMAN,
                "LIVE PUMP: покупки 60с %.0f%% / 5м %.0f%% • BTC 60с %+.2f%%, покупки %.0f%%",
                micro.aggressiveBuyPercent60s,
                micro.aggressiveBuyPercent5m,
                micro.bitcoinPriceChange60sPercent,
                micro.bitcoinAggressiveBuyPercent60s
            )
        } else {
            "LIVE PUMP/BTC: поток подключается"
        }
        tvMicrostructure?.text = "$book • $spread • $oi\n$liveFlow\n$impulseValues\n${impulse.status}"
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

    private fun renderApiButtons() {
        val deep = DeepSeekPrimaryStore.state(this)
        btnDeepSeekApi?.text = if (DeepSeekSecureKeyStore.read(this).isBlank()) {
            "DEEPSEEK API\nКЛЮЧ НЕ ВВЕДЁН\nоткрыть центр"
        } else {
            val fresh = DeepSeekPrimaryPolicy.isFreshSignal(deep)
            "DEEPSEEK • ОСНОВНОЙ\n${if (fresh) deep.action else "УСТАРЕЛ"} ${if (fresh) signed(deep.direction) + "/100" else ""}\n${deep.successfulToday} OK • ${deep.failedToday} ERR"
        }
        val budget = GeminiRequestBudget.state(this)
        btnGeminiApi?.text = if (EventRadarStore.apiKey(this).isBlank()) {
            "GEMINI API\nКЛЮЧ НЕ ВВЕДЁН\nоткрыть центр"
        } else {
            "GEMINI • РУЧНОЕ МНЕНИЕ\nавтоматические запросы выключены\nосталось ${budget.remainingToday}"
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 350)
        }
    }
}
