package com.example.pumppaperbot

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
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
    private var tvBuyerBreathSummary: TextView? = null
    private var tvHumanFactor: TextView? = null
    private var humanFactorActions: View? = null
    private var btnHumanApprove: Button? = null
    private var btnHumanReject: Button? = null
    private var chart: StrategyChartView? = null
    private var manualPositionChart: ManualPositionChartView? = null
    private var btnRisk30: Button? = null
    private var btnRisk35: Button? = null
    private var btnStart: Button? = null
    private var btnCheck: Button? = null
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
    private var btnFusionSim: Button? = null
    private var btnPumpMachine2: Button? = null
    private var btnCompetition: Button? = null
    private var btnCriticalOverview: Button? = null
    private var btnDeepSeekApi: Button? = null
    private var btnGeminiApi: Button? = null
    private var btnBitpandaFusion: Button? = null
    private var btnUnifiedLog: Button? = null
    private var btnRecentLog: Button? = null
    private var evidenceMemoryDialogVisible = false
    private var backgroundPersistencePromptVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { ResearchHistoryArchive.ensureCaptured(this) }
        runCatching { ResearchPerformanceLedger.capture(this) }
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
        tvBuyerBreathSummary = findViewById(R.id.tvBuyerBreathSummary)
        tvHumanFactor = findViewById(R.id.tvHumanFactor)
        humanFactorActions = findViewById(R.id.humanFactorActions)
        btnHumanApprove = findViewById(R.id.btnHumanApprove)
        btnHumanReject = findViewById(R.id.btnHumanReject)
        chart = findViewById(R.id.chart)
        manualPositionChart = findViewById(R.id.manualPositionChart)
        btnRisk30 = findViewById(R.id.btnRisk30)
        btnRisk35 = findViewById(R.id.btnRisk35)
        btnStart = findViewById(R.id.btnStart)
        btnCheck = findViewById(R.id.btnCheck)
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
        btnFusionSim = findViewById(R.id.btnFusionSim)
        btnPumpMachine2 = findViewById(R.id.btnPumpMachine2)
        btnCompetition = findViewById(R.id.btnCompetition)
        btnCriticalOverview = findViewById(R.id.btnCriticalOverview)
        btnDeepSeekApi = findViewById(R.id.btnDeepSeekApi)
        btnGeminiApi = findViewById(R.id.btnGeminiApi)
        btnBitpandaFusion = findViewById(R.id.btnBitpandaFusion)
        btnUnifiedLog = findViewById(R.id.btnUnifiedLog)
        btnRecentLog = findViewById(R.id.btnRecentLog)

        PumpBotEngine.ensureInitialized(this)
        requestNotificationPermission()
        // V5.30 is an always-on paper monitor. Opening the app repairs an accidental
        // previous STOP state without touching any account or historical record.
        startMonitor()

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
        btnCheck?.setOnClickListener { checkNow(forceDeepSeek = true) }
        btnReset?.setOnClickListener {
            confirm("Сбросить состояние?", "Очистится режим ожидания, цена входа и сохраненные данные графика.") {
                resetAll()
            }
        }
        btnManualBuy?.setOnClickListener { confirmManualBuy() }
        btnManualSell?.setOnClickListener { confirmManualSell() }
        btnManualHistory?.setOnClickListener { showManualHistory() }
        btnHumanApprove?.setOnClickListener {
            if (HumanFactorStore.approve(this)) Toast.makeText(this,"Виртуальная покупка подтверждена",Toast.LENGTH_LONG).show()
            else Toast.makeText(this,"Сигнал уже изменился или цена устарела",Toast.LENGTH_LONG).show()
            updateUi()
        }
        btnHumanReject?.setOnClickListener { HumanFactorStore.reject(this); updateUi() }
        btnChartSpeed?.setOnClickListener { showChartSpeedDialog() }
        btnBacktest?.setOnClickListener { startActivity(Intent(this, BacktestActivity::class.java)) }
        btnAlertSettings?.setOnClickListener { startActivity(Intent(this, AlertSettingsActivity::class.java)) }
        btnAppPaper?.setOnClickListener {
            startActivity(Intent(this, PumpMachineActivity::class.java))
        }
        btnGeminiExperiment?.setOnClickListener {
            startActivity(Intent(this, PumpMachineActivity::class.java))
        }
        btnGeminiExitExperiment?.setOnClickListener {
            startActivity(Intent(this, PumpMachineActivity::class.java))
        }
        btnUserPaper?.setOnClickListener {
            startActivity(Intent(this, PumpMachineActivity::class.java))
        }
        btnFusionSim?.setOnClickListener {
            startActivity(Intent(this, BitpandaFusionActivity::class.java))
        }
        btnPumpMachine2?.setOnClickListener {
            startActivity(Intent(this, CompetitionActivity::class.java))
        }
        btnCompetition?.setOnClickListener {
            startActivity(Intent(this, CompetitionActivity::class.java))
        }
        btnCriticalOverview?.setOnClickListener {
            startActivity(Intent(this, CriticalOverviewActivity::class.java))
        }
        tvBuyerBreathSummary?.setOnClickListener {
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
        btnBitpandaFusion?.setOnClickListener {
            startActivity(Intent(this, BitpandaFusionActivity::class.java))
        }
        btnUnifiedLog?.setOnClickListener {
            runCatching { UnifiedResearchLog.share(this) }
                .onFailure { Toast.makeText(this, it.message ?: "Ошибка экспорта лога", Toast.LENGTH_LONG).show() }
        }
        btnRecentLog?.setOnClickListener {
            runCatching { UnifiedResearchLog.shareRecent24h(this) }
                .onFailure { Toast.makeText(this, it.message ?: "Ошибка экспорта 24-часового лога", Toast.LENGTH_LONG).show() }
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
        handler.postDelayed({ maybeEnsureBackgroundPersistence() }, 500L)
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

    private fun resetAll() {
        runCatching { ResearchPerformanceLedger.capture(this) }
        stopService(Intent(this, PumpSignalService::class.java))
        WorkManager.getInstance(this).cancelUniqueWork(PumpBotEngine.uniqueWorkName)
        PumpBotEngine.reset(this)
        UserPaperStore.discardOpenPosition(this)
        ManualPositionStore.discardOpenPosition(this)
        PositionSupervisorStore.clearPosition(this)
        GeminiPositionAdvisorStore.clearPosition(this)
        PersonalPositionGuardStore.clear(this)
        PumpAlert.clearPersonalPositionAlerts(this)
        updateUi()
        checkNow()
    }

    private fun checkNow(forceDeepSeek: Boolean = false) {
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
                PumpBotWorker.INPUT_FORCE_PRIMARY_DEEPSEEK to forceDeepSeek
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
            PumpAlert.clearPersonalPositionAlerts(this)
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
        val pumpMachineAccount = PumpMachineStore.state(this)
        val pumpMachine2Account = PumpMachine2Store.state(this)
        val geminiExitExperiment = GeminiExitExperimentStore.state(this)?.portfolio
            ?: GeminiPaperPortfolio()
        val sergeAccount = UserPaperStore.markToMarket(this, accountPrice)
        val human = HumanFactorStore.state(this)
        val auto3265 = Vwap3265AutoStore.state(this)
        val fusionMarket = BitpandaFusionStore.state(this)
        val fusionAccount = FusionSimStore.state(this)
        val fusionPriority = FusionPriorityPolicy.plan(fusionAccount)
        val fusionMark = fusionMarket.bid.takeIf { fusionMarket.fresh(now) } ?: accountPrice
        val pumpMachine2Value = PumpMachine2Policy.netLiquidationValue(
            pumpMachine2Account,
            fusionMarket.bid.takeIf { fusionMarket.fresh(now) } ?: accountPrice,
            fusionMarket.feeRate
        )
        btnAppPaper?.text = accountButtonText(
            "PUMP MACHINE 1 • 2%",
            pumpMachine2Value,
            (pumpMachine2Value / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0
        )
        val pumpMachineValue = PumpMachinePolicy.netLiquidationValue(
            pumpMachineAccount,
            fusionMarket.bid.takeIf { fusionMarket.fresh(now) } ?: accountPrice,
            fusionMarket.feeRate
        )
        btnGeminiExperiment?.text = accountButtonText(
            "PUMP MACHINE 2 • 3%",
            pumpMachineValue,
            (pumpMachineValue / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0
        )
        val retestValue = PumpMachineRetestStore.netValue(this, now)
        btnGeminiExitExperiment?.text = accountButtonText(
            "PUMP MACHINE 3 • RETEST ${PumpBotEngine.formatTime(PumpMachineRetestStore.lastStatusAt(this))}",
            retestValue,
            (retestValue / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0
        )
        val safeValue = PumpMachineSafeStore.netValue(this, now)
        btnUserPaper?.text = accountButtonText(
            "PUMP MACHINE 4 • SAFE ${PumpBotEngine.formatTime(PumpMachineSafeStore.lastStatusAt(this))}",
            safeValue,
            (safeValue / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0
        )
        btnFusionSim?.text = accountButtonText(
            "FUSION • ЛОКАЛЬНЫЙ ПОТОК",
            fusionAccount.value(fusionMark),
            fusionAccount.profit(fusionMark) / FusionSimPortfolio.START_BALANCE * 100.0
        )
        btnPumpMachine2?.text = "ЕЩЁ 5 СЧЕТОВ\nAPP • DEEPSIGX • СЕРЖ • AUTO • ЧЕЛОВЕК\nоткрыть сравнение"
        tvStatus?.text = if (snapshot.running) {
            "V${BuildConfig.VERSION_NAME} PAPER‑ТЕСТ • монитор включён" +
                (if (fusionPriority.active) " • FUSION: локальная защита" else "") +
                " • обновлено ${PumpBotEngine.formatTime(snapshot.lastSync)}"
        } else {
            "V${BuildConfig.VERSION_NAME} PAPER‑ТЕСТ • монитор остановлен • последнее обновление ${PumpBotEngine.formatTime(snapshot.lastSync)}"
        }
        tvHumanFactor?.text = buildString {
            append("ЧЕЛОВЕЧЕСКИЙ ФАКТОР • VWAP 32,65\n")
            append(if(human.inPosition) String.format(Locale.GERMANY,"ПОЗИЦИЯ • €%.2f",human.value(accountPrice)) else "ГОТОВНОСТЬ ${human.readiness}/100")
            append("\n${human.reason}")
            append(String.format(Locale.GERMANY,"\nAUTO 32,65: €%.2f • %s",auto3265.value(accountPrice),if(auto3265.inPosition)"В PUMP" else "В EUR"))
        }
        humanFactorActions?.visibility = if(human.pending&&!human.inPosition) View.VISIBLE else View.GONE
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
        tvMode?.text = if (ResearchModePolicy.ENABLED) {
            "V5 АНАЛИТИКА + PAPER‑ТЕСТ • APP | DEEPSIG | DEEPSIGX"
        } else if (snapshot.rapidDrop.active) {
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
        renderBuyerBreath()

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
        val researchAppReason = appAccount.decisions.lastOrNull()?.reason
        tvReason?.text = if (ResearchModePolicy.ENABLED) {
            researchAppReason?.let { "APP V5 • последний причинный расчёт\n$it" }
                ?: "APP V5 ждёт достаточную историю и первый причинный расчёт. Старый индикатор не является торговой командой."
        } else if (snapshot.weekRhythm.caution) {
            "${snapshot.signalReason}\n${snapshot.weekRhythm.title}. ${snapshot.weekRhythm.explanation}"
        } else {
            snapshot.signalReason
        }
        tvReason?.setTextColor(
            when {
                ResearchModePolicy.ENABLED -> Color.parseColor("#79C0FF")
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
        val alertsEnabled = ResearchModePolicy.alertsEnabled(this)
        tvAlertStatus?.text = if (alertsEnabled) {
            "ЗВОНКИ ВКЛЮЧЕНЫ\n${AlertSchedule.statusText(this)}"
        } else {
            "ЗВОНКИ ВЫКЛЮЧЕНЫ • аналитика и виртуальные сделки продолжаются"
        }
        btnAlertSettings?.text = if (alertsEnabled) {
            "ЗВОНКИ ВКЛЮЧЕНЫ • РАСПИСАНИЕ"
        } else {
            "ЗВОНКИ ВЫКЛЮЧЕНЫ • НАСТРОЙКИ"
        }
        btnAlertSettings?.backgroundTintList = ColorStateList.valueOf(
            Color.parseColor(if (alertsEnabled) "#238636" else "#8E1519")
        )

        btnStart?.isEnabled = !snapshot.running
        btnStart?.alpha = if (snapshot.running) 0.45f else 1f
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
                    else -> "DEEPSIG РАБОТАЕТ"
                }
            )
        )
    }

    private fun renderBuyerBreath() {
        val cycle = LiveMarketBreathingStore.snapshot(this, System.currentTimeMillis()).buyerBreath
        tvBuyerBreathSummary?.text = BuyerBreathText.compact(cycle)
        val colors = when (cycle.phase) {
            BuyerBreathPhase.IGNITION, BuyerBreathPhase.EXPANSION -> "#D7FBE0" to "#14351F"
            BuyerBreathPhase.MATURE, BuyerBreathPhase.QUIET -> "#FFF3BF" to "#3A300F"
            BuyerBreathPhase.EXHAUSTION, BuyerBreathPhase.SELLER_TAKEOVER, BuyerBreathPhase.SHOCK ->
                "#FFD7D5" to "#3A171A"
            BuyerBreathPhase.STALE -> "#8B949E" to "#161B22"
        }
        tvBuyerBreathSummary?.setTextColor(Color.parseColor(colors.first))
        tvBuyerBreathSummary?.setBackgroundColor(Color.parseColor(colors.second))
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
            tvPositionSupervisor?.setBackgroundColor(Color.parseColor("#201522"))
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
        val regime = BtcPumpRegimeSnapshot(
            type = BtcPumpRegimeType.MIXED_OR_SIDEWAYS,
            title = state.btcPumpRegimeTitle,
            explanation = state.btcPumpRegimeExplanation,
            confidence = 0,
            exitRiskAdjustment = 0
        )
        val adviser = PersonalPositionAdvisorPolicy.render(
            state,
            regime,
            supportPlan,
            intervalMinutes,
            LiveMarketBreathingStore.snapshot(this, System.currentTimeMillis()).buyerBreath
        )
        tvPositionSupervisor?.text = buildString {
            append(adviser.text)
            append("\n\nКОНТРОЛЬ: ${state.model.ifBlank { "DeepSeek ещё не вызывался" }} • до ${intervalMinutes} мин")
            if (state.lastSuccess > 0L) append(" • ответ ${PumpBotEngine.formatTime(state.lastSuccess)}")
            append("\n\nВТОРОЕ МНЕНИЕ: ${GeminiPositionAdvisorPolicy.statusText(gemini)}")
            append("\n${gemini.model.ifBlank { "Gemini ещё не вызывался" }}")
        }
        val colors = when (adviser.severity) {
            PersonalAdvisorSeverity.EXIT -> "#FFFFFF" to "#5A171C"
            PersonalAdvisorSeverity.WATCH -> "#FFF3BF" to "#493A0F"
            PersonalAdvisorSeverity.CALM -> "#D7FBE0" to "#14351F"
        }
        tvPositionSupervisor?.setTextColor(Color.parseColor(colors.first))
        tvPositionSupervisor?.setBackgroundColor(Color.parseColor(colors.second))
    }

    private fun renderDeepSeekActionLevel(
        snapshot: LiveSnapshot,
        primary: DeepSeekPrimaryState,
        now: Long
    ) {
        if (ResearchModePolicy.ENABLED) {
            val inPosition = PumpMachineStore.state(this).inPosition
            val fresh = DeepSeekPrimaryPolicy.isFreshSignal(primary, now)
            val card = DeepSeekResearchCardPolicy.render(primary, inPosition, fresh)
            tvDeepSeekActionLevel?.text = card.text
            val color = when (card.tone) {
                DeepSeekResearchCardTone.SAFE -> "#FFB4AB" to "#3A171A"
                DeepSeekResearchCardTone.WATCH -> "#FFD866" to "#3A300F"
                DeepSeekResearchCardTone.READY -> "#7EE787" to "#15351F"
                DeepSeekResearchCardTone.DANGER -> "#FFFFFF" to "#5A171C"
                DeepSeekResearchCardTone.STALE -> "#8B949E" to "#161B22"
            }
            tvDeepSeekActionLevel?.setTextColor(Color.parseColor(color.first))
            tvDeepSeekActionLevel?.setBackgroundColor(Color.parseColor(color.second))
            return
        }
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
        tvDeepSeekActionLevel?.text = "DEEPSIG • $phase\n${level.level}/10 • ${level.label}\n${level.detail}"
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
        if (ResearchModePolicy.ENABLED) {
            data class PaperEvent(val source: String, val action: String, val at: Long, val reason: String)
            val events = buildList {
                AppPaperStore.state(this@MainActivity).trades.lastOrNull()?.let {
                    add(PaperEvent("APP", it.action, it.time, it.reason))
                }
                PumpMachineStore.state(this@MainActivity).trades.lastOrNull()?.let {
                    add(PaperEvent("PUMP MACHINE", it.action, it.time, it.reason))
                }
                GeminiExitExperimentStore.state(this@MainActivity)?.portfolio?.trades?.lastOrNull()?.let {
                    add(PaperEvent("DEEPSIGX", it.action, it.time, it.reason))
                }
            }
            val latestPaper = events.maxByOrNull { it.at }
            tvLatestSignal?.text = if (latestPaper == null) {
                "ТИХИЙ PAPER‑ЖУРНАЛ\nТри системы ждут первую виртуальную сделку"
            } else {
                "${latestPaper.source} • ВИРТУАЛЬНО ${latestPaper.action}\n" +
                    "${latestPaper.reason.take(260)}\n${PumpBotEngine.formatTime(latestPaper.at)}"
            }
            tvLatestSignal?.setTextColor(Color.parseColor("#79C0FF"))
            return
        }
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
        if (ResearchModePolicy.ENABLED) {
            val latest = AppPaperStore.state(this).decisions.lastOrNull()
            tvReadiness?.text = if (latest == null) {
                "APP V5 • АНАЛИТИК\nЖДЁМ ПЕРВЫЙ КАНДИДАТ\nреальные сделки только вручную"
            } else {
                val action = when (latest.action) {
                    "BUY" -> "КАНДИДАТ ВХОДА • APP КУПИЛ ВИРТУАЛЬНО"
                    StrategyV2.ACTION_SELL, StrategyV2.ACTION_SELL_HALF -> "КАНДИДАТ ВЫХОДА • APP ПРОДАЛ ВИРТУАЛЬНО"
                    else -> "NO TRADE / НАБЛЮДЕНИЕ"
                }
                "APP V5 • $action\n${PumpBotEngine.formatTime(latest.time)}\nпроверьте причины перед ручным действием"
            }
            tvReadiness?.setTextColor(Color.parseColor("#79C0FF"))
            tvReadiness?.setBackgroundColor(Color.parseColor("#101820"))
            return
        }
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
            score >= 95 -> "+$score  КАНДИДАТ ПРИБЛИЖАЕТСЯ\nзапись в журнал на 99"
            score <= -100 -> "−100  ПРОДАВАТЬ\nУСЛОВИЯ ПОДТВЕРЖДЕНЫ"
            score == -99 -> "−99  ЗВОНОК: ПРИГОТОВИТЬСЯ\nдо продажи остался 1 балл"
            score <= -95 -> "−${kotlin.math.abs(score)}  РИСК ПРИБЛИЖАЕТСЯ\nзапись в журнал на −99"
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
        if (ResearchModePolicy.ENABLED) {
            btnRisk30?.isEnabled = false
            btnRisk35?.isEnabled = false
            btnRisk30?.alpha = 0.45f
            btnRisk35?.alpha = 0.45f
            btnRisk30?.text = "СТАРАЯ НАСТРОЙКА\nНЕ ИСПОЛЬЗУЕТСЯ APP V5"
            btnRisk35?.text = "СТАРАЯ НАСТРОЙКА\nНЕ ИСПОЛЬЗУЕТСЯ APP V5"
            return
        }
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
        val fusion = BitpandaFusionStore.state(this)
        val fusionPriority = FusionPriorityPolicy.plan(FusionSimStore.state(this))
        btnBitpandaFusion?.text = when {
            !fusion.configured -> "BITPANDA FUSION\nКЛЮЧ READ НЕ ВВЕДЁН\nоткрыть центр"
            fusion.fresh() -> "BITPANDA • READ-ONLY\n${fusion.pair} • СПРЕД ${String.format(Locale.US, "%.3f", fusion.spreadPercent)}%\n" +
                (if (fusionPriority.active) "ЛОКАЛЬНАЯ ЗАЩИТА • БЕЗ УСКОРЕНИЯ PRO" else "FUSIONSIM РАБОТАЕТ")
            else -> "BITPANDA • READ-ONLY\nНЕТ СВЕЖИХ ДАННЫХ\nпроверить соединение"
        }
    }

    private fun maybeEnsureBackgroundPersistence() {
        if (backgroundPersistencePromptVisible || !PumpBotEngine.snapshot(this).running) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val power = getSystemService(PowerManager::class.java)
        if (power.isIgnoringBatteryOptimizations(packageName)) return

        backgroundPersistencePromptVisible = true
        AlertDialog.Builder(this)
            .setTitle("PUMP • РАБОТА В ФОНЕ")
            .setMessage(
                "Чтобы поток 1/5/15 минут, Fusion и предупреждения продолжали работать, когда открыт YouTube или другое приложение, " +
                    "разрешите PUMP работать без оптимизации батареи. Постоянное уведомление монитора останется в шторке, пока монитор включён."
            )
            .setPositiveButton("РАЗРЕШИТЬ ВСЕГДА") { _, _ ->
                backgroundPersistencePromptVisible = false
                requestBatteryOptimizationExemption()
            }
            .setNegativeButton("ПОЗЖЕ") { _, _ -> backgroundPersistencePromptVisible = false }
            .setOnCancelListener { backgroundPersistencePromptVisible = false }
            .show()
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val direct = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName")
        )
        runCatching { startActivity(direct) }
            .onFailure {
                runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
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
