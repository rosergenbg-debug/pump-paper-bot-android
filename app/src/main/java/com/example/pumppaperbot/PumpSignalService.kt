package com.example.pumppaperbot

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class PumpSignalService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val shockExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val shockCheckQueuedOrRunning = AtomicBoolean(false)
    private val market = MarketSyncClient()
    private val eventRadar = EventRadarClient()
    private val pumpEcosystem = PumpEcosystemClient()
    private val normalCycleIntervalMillis = TimeUnit.MINUTES.toMillis(2)
    private val cycleQueuedOrRunning = AtomicBoolean(false)
    private lateinit var microImpulse: MicroImpulseStream
    @Volatile private var destroyed = false

    private val loop = object : Runnable {
        override fun run() {
            checkNow()
            handler.postDelayed(this, nextCycleIntervalMillis())
        }
    }

    override fun onCreate() {
        super.onCreate()
        destroyed = false
        PumpAlert.ensureChannels(this)
        microImpulse = MicroImpulseStream(this) { requestFastShockCheck() }
        startForeground(
            PumpAlert.monitorId(),
            PumpAlert.monitorNotification(
                this,
                "V${BuildConfig.VERSION_NAME}: V6 execution intelligence работает в SHADOW; " +
                    "четыре Pump Machine и Fusion сохраняют независимые решения."
            )
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        PumpBotEngine.setRunning(this, true)
        microImpulse.start()
        handler.removeCallbacks(loop)
        handler.post(loop)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // android:stopWithTask=false + START_STICKY: removing the UI task must not be
        // interpreted as turning the monitor off. A real user Stop still calls stopService().
        GeminiPaperStore.recordActivity(
            this,
            "ФОН",
            "HOLD",
            "Окно приложения закрыто/смахнуто; foreground-монитор продолжает работу"
        )
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        destroyed = true
        handler.removeCallbacks(loop)
        microImpulse.stop()
        executor.shutdownNow()
        shockExecutor.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun recordIndependentPumpFailure(stage: String, error: Throwable) {
        GeminiPaperStore.recordActivity(
            this,
            stage,
            "ERROR",
            error.message ?: error.javaClass.simpleName
        )
    }

    private fun observeV6(trigger: String, now: Long = System.currentTimeMillis()) {
        runCatching { ScalpExecutionIntelligenceStoreV600.observe(this, trigger, now) }
            .onFailure { error ->
                UnifiedResearchLog.record(
                    this,
                    "V6_EXECUTION_SHADOW",
                    "ERROR",
                    "Изолированная ошибка shadow-надстройки: ${error.javaClass.simpleName}: ${error.message.orEmpty().take(160)}",
                    now
                )
            }
    }

    private fun requestFastShockCheck() {
        if (!shockCheckQueuedOrRunning.compareAndSet(false, true)) return
        shockExecutor.execute {
            try {
                val now = System.currentTimeMillis()
                // When Serge owns a position, this produces only evidence-based exit warnings;
                // entry chatter and virtual-agent trade sounds are handled separately below.
                FastPositionWarningStore.sync(this, now)

                val pumpMachineFast = PumpMachineStore.state(this)
                val pumpMachine2Fast = PumpMachine2Store.state(this)
                val pumpRetestFast = PumpMachineRetestStore.state(this)
                val pumpSafeFast = PumpMachineSafeStore.state(this)
                val fusionFast = FusionSimStore.state(this)
                val entryObservationFast = SharedFusionEntryObservationStore.snapshot(this, now)
                val fastCandidates = PumpFastCandidatePolicyV537.evaluate(entryObservationFast)
                // Fusion is a different strategy, but once its own short-flow hypothesis is alive
                // it must not be physically delayed by the generic ~2 minute app cycle.
                val fusionFastCandidate = !fusionFast.inPosition &&
                    SharedFusionEntryPolicy.directionalCandidate(entryObservationFast.frame)
                if (pumpMachineFast.inPosition || pumpMachine2Fast.inPosition || pumpRetestFast.inPosition ||
                    pumpSafeFast.inPosition || fusionFast.inPosition || fastCandidates.any || fusionFastCandidate) {
                    val venue = BitpandaFusionStore.state(this)
                    if (!venue.fresh(now) || now - venue.lastSuccess >= 15_000L) {
                        BitpandaFusionClient().sync(this, force = true)
                    }
                    val fastNow = System.currentTimeMillis()
                    val trigger = buildList {
                        if (fastCandidates.pump2) add("PM1_CAND")
                        if (fastCandidates.pump3) add("PM2_CAND")
                        if (fastCandidates.retest) add("PM3_RETEST_CAND")
                        if (fastCandidates.safe) add("PM4_SAFE_CAND")
                        if (fusionFastCandidate) add("FUSION_CAND")
                        if (pumpMachine2Fast.inPosition) add("PM1_POS")
                        if (pumpMachineFast.inPosition) add("PM2_POS")
                        if (pumpRetestFast.inPosition) add("PM3_POS")
                        if (pumpSafeFast.inPosition) add("PM4_POS")
                        if (fusionFast.inPosition) add("FUSION_POS")
                    }.joinToString("+").ifBlank { "FAST" }
                    observeV6(trigger, fastNow)
                    if (pumpMachineFast.inPosition || fastCandidates.pump3) {
                        runCatching { PumpMachineStore.sync(this, fastNow) }
                            .onFailure { recordIndependentPumpFailure("PUMP_MACHINE_FAST", it) }
                    }
                    if (pumpMachine2Fast.inPosition || fastCandidates.pump2) {
                        runCatching { PumpMachine2Store.sync(this, fastNow) }
                            .onFailure { recordIndependentPumpFailure("PUMP_MACHINE_2_FAST", it) }
                    }
                    if (pumpRetestFast.inPosition || fastCandidates.retest) {
                        runCatching { PumpMachineRetestStore.sync(this, fastNow) }
                            .onFailure { recordIndependentPumpFailure("PUMP_MACHINE_RETEST_FAST", it) }
                    }
                    if (pumpSafeFast.inPosition || fastCandidates.safe) {
                        runCatching { PumpMachineSafeStore.sync(this, fastNow) }
                            .onFailure { recordIndependentPumpFailure("PUMP_MACHINE_SAFE_FAST", it) }
                    }
                    if (fusionFast.inPosition || fusionFastCandidate) {
                        runCatching { FusionSimStore.sync(this, DeepSeekPrimaryStore.state(this), fastNow) }
                            .onFailure { recordIndependentPumpFailure("FUSION_FAST", it) }
                    }
                }

                val shock = ShockReboundStore.state(this)
                if (!shock.active || !shock.fresh(now)) return@execute
                val fusion = FusionSimStore.state(this)
                if (!shock.ready && !fusion.inPosition && !pumpMachineFast.inPosition && !pumpMachine2Fast.inPosition &&
                    !pumpRetestFast.inPosition && !pumpSafeFast.inPosition) return@execute

                // A fast rebound cannot wait for the 1-3 minute full cycle. Refresh only the
                // read-only execution book and run the local paper engines; no AI call is made.
                BitpandaFusionClient().sync(this, force = true)
                val shockNow = System.currentTimeMillis()
                observeV6("SHOCK_REBOUND", shockNow)
                FusionSimStore.sync(this, DeepSeekPrimaryStore.state(this), shockNow)
                runCatching { PumpMachineStore.sync(this, shockNow) }
                    .onFailure { recordIndependentPumpFailure("PUMP_MACHINE_SHOCK", it) }
                runCatching { PumpMachine2Store.sync(this, shockNow) }
                    .onFailure { recordIndependentPumpFailure("PUMP_MACHINE_2_SHOCK", it) }
                runCatching { PumpMachineRetestStore.sync(this, shockNow) }
                    .onFailure { recordIndependentPumpFailure("PUMP_MACHINE_RETEST_SHOCK", it) }
                runCatching { PumpMachineSafeStore.sync(this, shockNow) }
                    .onFailure { recordIndependentPumpFailure("PUMP_MACHINE_SAFE_SHOCK", it) }
            } finally {
                shockCheckQueuedOrRunning.set(false)
            }
        }
    }

    private fun checkNow() {
        if (!cycleQueuedOrRunning.compareAndSet(false, true)) {
            GeminiPaperStore.recordActivity(
                this,
                "ЦИКЛ",
                "WAIT",
                "МОНИТОР 2 МИН: новый запуск не поставлен в очередь, предыдущий ещё выполняется"
            )
            return
        }
        executor.execute {
            val cycleIntervalMillis = nextCycleIntervalMillis()
            val fusionPriorityAtStart = FusionPriorityPolicy.plan(FusionSimStore.state(this))
            val source = if (fusionPriorityAtStart.active) {
                fusionPriorityAtStart.label
            } else if (cycleIntervalMillis <= PositionSupervisorPolicy.PRO_RECHECK_INTERVAL) {
                "МОНИТОР 1 МИН • УСИЛЕННАЯ ПОЗИЦИЯ"
            } else {
                "МОНИТОР 2 МИН"
            }
            if (!GeminiCycleGuard.tryEnter()) {
                GeminiPaperStore.recordActivity(
                    this,
                    "ЦИКЛ",
                    "WAIT",
                    "$source: пропущен, потому что предыдущая проверка ещё выполняется"
                )
                cycleQueuedOrRunning.set(false)
                return@execute
            }
            val startedAt = System.currentTimeMillis()
            GeminiPaperStore.beginCycle(this, source, cycleIntervalMillis, startedAt)
            try {
                market.sync(this)
                CycleStageGuard.run(this, "HUMAN_FACTOR_1M", { Unit }) {
                    ChartMarketClient().sync(this, ChartInterval.ONE_MINUTE)
                }
                val marketSnapshot = PumpBotEngine.snapshot(this)
                val evidenceNow = System.currentTimeMillis()
                PaperExecutionPolicy.freshLivePrice(marketSnapshot, evidenceNow)?.let { freshPrice ->
                    CycleStageGuard.run(this, "EVIDENCE_OUTCOMES", { Unit }) {
                        DeepSeekEvidenceMemory.updateOutcomes(this, freshPrice, evidenceNow)
                    }
                }
                CycleStageGuard.run(this, "PUMP_ECOSYSTEM", { PumpEcosystemStore.state(this) }) {
                    pumpEcosystem.sync(this)
                }
                CycleStageGuard.run(this, "BITPANDA_FUSION", { BitpandaFusionStore.state(this) }) {
                    BitpandaFusionClient().sync(this)
                }
                CycleStageGuard.run(this, "V6_EXECUTION_SHADOW", { ScalpExecutionIntelligenceStoreV600.current(this) }) {
                    ScalpExecutionIntelligenceStoreV600.observe(this, "BASELINE")
                }
                val eventState = CycleStageGuard.run(this, "EVENT_RADAR", { EventRadarStore.state(this) }) {
                    eventRadar.sync(this)
                }
                val personalGuard = CycleStageGuard.run(
                    this, "PERSONAL_GUARD", {
                        PersonalPositionGuardOutcome(PersonalPositionGuardStore.state(this))
                    }
                ) { PersonalPositionGuardStore.sync(this) }
                CycleStageGuard.run(this, "FUSION_ACTIVATION", { Unit }) {
                    FusionSimStore.activate(this, DeepSeekPrimaryStore.state(this).lastSuccess)
                }
                CycleStageGuard.run(this, "DEEPSIG_OWNERSHIP", { Unit }) {
                    DeepSeekTradeOwnership.activate(this, DeepSeekPrimaryStore.state(this).lastSuccess)
                }
                val deepSeek = CycleStageGuard.run(
                    this, "DEEPSIG_PRIMARY", { DeepSeekPrimaryStore.state(this) }
                ) { DeepSeekPrimaryAnalyst().sync(this) }
                CycleStageGuard.run(this, "POSITION_SUPERVISOR", { PositionSupervisorStore.state(this) }) {
                    PositionSupervisorClient().sync(this, forceCritical = personalGuard.forceCriticalAi)
                }
                CycleStageGuard.run(this, "GEMINI_POSITION", { GeminiPositionAdvisorStore.state(this) }) {
                    GeminiPositionAdvisorClient().sync(this, forceCritical = personalGuard.forceCriticalAi)
                }
                CycleStageGuard.run(this, "CYCLE_STATUS", { Unit }) {
                    GeminiPaperStore.markDataReady(this, source, startedAt)
                }
                val snapshot = PumpBotEngine.snapshot(this)
                val userPositionOpen = snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0
                val appTrade = CycleStageGuard.run(
                    this, "APP_PAPER", { AppPaperSyncResult(AppPaperStore.state(this), false) }
                ) { AppPaperStore.syncWithAlerts(this) }
                CycleStageGuard.run(this, "DEEPSIGX_PAPER", { GeminiExitExperimentStore.state(this) }) {
                    DeepSigXRuntimeV610.sync(this, deepSeek)
                }
                val pumpMachine = CycleStageGuard.run(
                    this, "PUMP_MACHINE", {
                        PumpMachineSyncResult(PumpMachineStore.state(this), "ошибка Pump 3 изолирована", 0.0)
                    }
                ) { PumpMachineStore.sync(this) }
                CycleStageGuard.run(
                    this, "PUMP_MACHINE_2", {
                        PumpMachine2SyncResult(PumpMachine2Store.state(this), "ошибка Pump 2 изолирована", 0.0)
                    }
                ) { PumpMachine2Store.sync(this) }
                CycleStageGuard.run(this, "PUMP_MACHINE_RETEST", {
                    PumpVariantSyncResult(PumpMachineRetestStore.state(this), "ошибка Pump Retest изолирована", 0.0)
                }) { PumpMachineRetestStore.sync(this) }
                CycleStageGuard.run(this, "PUMP_MACHINE_SAFE", {
                    PumpVariantSyncResult(PumpMachineSafeStore.state(this), "ошибка Pump Safe изолирована", 0.0)
                }) { PumpMachineSafeStore.sync(this) }
                CycleStageGuard.run(this, "FUSION_SIM", { FusionSimStore.state(this) }) {
                    FusionSimStore.sync(this, deepSeek)
                }
                CycleStageGuard.run(this, "VWAP_3265_AUTO", { Vwap3265AutoStore.state(this) }) {
                    Vwap3265AutoStore.sync(this)
                }
                CycleStageGuard.run(this, "HUMAN_FACTOR", { HumanFactorStore.state(this) }) {
                    HumanFactorStore.sync(this)
                }
                CycleStageGuard.run(this, "ENTRY_GATE_AUDIT", { EntryOpportunityAuditStore.latest(this) }) {
                    EntryOpportunityAuditStore.capture(this)
                }
                val rapidDropAlerted = CycleStageGuard.run(this, "RAPID_DROP_ALERT", { false }) {
                    if (PumpBotEngine.shouldAlertRapidDrop(this, snapshot)) {
                        PumpAlert.showRapidDrop(this, snapshot)
                        PumpBotEngine.markRapidDropAlerted(this, snapshot)
                        true
                    } else false
                }
                val signalAlerted = CycleStageGuard.run(this, "SIGNAL_ALERT", { false }) {
                    if (!rapidDropAlerted && userPositionOpen && snapshot.sellSignal &&
                        PumpBotEngine.shouldAlert(this, snapshot)
                    ) {
                        PumpAlert.showSignal(this, snapshot)
                        PumpBotEngine.markAlerted(this, snapshot)
                        true
                    } else false
                }
                CycleStageGuard.run(this, "ENTRY_REMINDER", { Unit }) {
                    EntryAlertReminderStore.flush(this)
                }
                val finishedAt = System.currentTimeMillis()
                CycleStageGuard.run(this, "PERFORMANCE_LEDGER", { Unit }) {
                    ResearchPerformanceLedger.capture(this)
                }
                CycleStageGuard.run(this, "UNIFIED_LOG", { Unit }) {
                    UnifiedResearchLog.captureCycle(this, source, finishedAt)
                }
                GeminiPaperStore.finishCycle(
                    this,
                    source,
                    startedAt,
                    finishedAt + cycleIntervalMillis,
                    "проверка завершена; V6 execution=SHADOW; DeepSeek аналитик: ${deepSeek.action}; Pump Machine: ${pumpMachine.status}; Gemini контролирует только открытую позицию Сержа",
                    finishedAt
                )
            } catch (error: Exception) {
                val failedAt = System.currentTimeMillis()
                GeminiPaperStore.failCycle(
                    this,
                    source,
                    startedAt,
                    failedAt + cycleIntervalMillis,
                    error.message ?: error.javaClass.simpleName,
                    failedAt
                )
            } finally {
                GeminiCycleGuard.exit()
                cycleQueuedOrRunning.set(false)
                if (!destroyed) {
                    handler.removeCallbacks(loop)
                    handler.postDelayed(loop, nextCycleIntervalMillis())
                }
            }
        }
    }

    private fun nextCycleIntervalMillis(): Long {
        val snapshot = PumpBotEngine.snapshot(this)
        if (snapshot.waitMode != "SELL" || snapshot.entryPrice <= 0.0) {
            val level = DeepSeekActionLevelPolicy.fromMarket(
                snapshot,
                DeepSeekPrimaryStore.state(this),
                MicroImpulseStore.state(this)
            )
            return if (level.intensive) {
                DeepSeekActionLevelPolicy.INTENSIVE_INTERVAL_MILLIS
            } else normalCycleIntervalMillis
        }
        val plan = PositionSupervisorPolicy.supportPlan(
            snapshot = snapshot,
            state = PositionSupervisorStore.state(this),
            guard = PersonalPositionGuardStore.state(this),
            micro = MicroImpulseStore.state(this),
            forceCritical = false,
            now = System.currentTimeMillis()
        )
        return PositionSupervisorPolicy.foregroundCycleInterval(plan)
    }
}
