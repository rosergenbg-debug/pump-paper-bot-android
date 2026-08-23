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
                "V${BuildConfig.VERSION_NAME}: APP, DeepSig и DeepSigX ведут отдельные виртуальные счета; " +
                    (if (ResearchModePolicy.alertsEnabled(this)) "звонки включены." else "звонки выключены.")
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
                val entryObservationFast = SharedFusionEntryObservationStore.snapshot(this, now)
                val pairFastCandidate = !pumpMachineFast.inPosition && !pumpMachine2Fast.inPosition &&
                    PumpProfitEngineV526.isFastCandidate(PumpProfitModeV526.PUMP_3, entryObservationFast)
                if (pumpMachineFast.inPosition || pumpMachine2Fast.inPosition || pairFastCandidate) {
                    val venue = BitpandaFusionStore.state(this)
                    if (!venue.fresh(now) || now - venue.lastSuccess >= 15_000L) {
                        BitpandaFusionClient().sync(this, force = true)
                    }
                    val fastNow = System.currentTimeMillis()
                    PumpMachinePairCoordinator.sync(this, fastNow)
                }

                val shock = ShockReboundStore.state(this)
                if (!shock.active || !shock.fresh(now)) return@execute
                val fusion = FusionSimStore.state(this)
                if (!shock.ready && !fusion.inPosition && !pumpMachineFast.inPosition && !pumpMachine2Fast.inPosition) return@execute

                // A fast rebound cannot wait for the 1-3 minute full cycle. Refresh only the
                // read-only execution book and run the local paper engines; no AI call is made.
                BitpandaFusionClient().sync(this, force = true)
                val shockNow = System.currentTimeMillis()
                FusionSimStore.sync(this, DeepSeekPrimaryStore.state(this), shockNow)
                PumpMachinePairCoordinator.sync(this, shockNow)
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
                val pumpPair = CycleStageGuard.run(
                    this, "PUMP_MACHINE_PAIR", {
                        PumpMachinePairSyncResult(
                            PumpMachineSyncResult(PumpMachineStore.state(this), "ошибка пары Pump изолирована", 0.0),
                            PumpMachine2SyncResult(PumpMachine2Store.state(this), "ошибка пары Pump изолирована", 0.0)
                        )
                    }
                ) { PumpMachinePairCoordinator.sync(this) }
                val pumpMachine = pumpPair.pump3
                CycleStageGuard.run(this, "FUSION_SIM", { FusionSimStore.state(this) }) {
                    FusionSimStore.sync(this, deepSeek)
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
                    if (!rapidDropAlerted && !appTrade.tradeAlerted &&
                        (!userPositionOpen || snapshot.sellSignal) && PumpBotEngine.shouldAlert(this, snapshot)
                    ) {
                        PumpAlert.showSignal(this, snapshot)
                        PumpBotEngine.markAlerted(this, snapshot)
                        true
                    } else false
                }
                if (!userPositionOpen && !rapidDropAlerted && !appTrade.tradeAlerted && !signalAlerted &&
                    EventRadarStore.shouldAlert(this, eventState)
                ) {
                    CycleStageGuard.run(this, "EVENT_ALERT", { Unit }) {
                        PumpAlert.showEventRadar(this, eventState, snapshot)
                        EventRadarStore.markAlerted(this, eventState)
                    }
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
                    "проверка завершена; DeepSeek аналитик: ${deepSeek.action}; Pump Machine: ${pumpMachine.status}; Gemini контролирует только открытую позицию Сержа",
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
        val fusionPriority = FusionPriorityPolicy.plan(FusionSimStore.state(this))
        if (fusionPriority.active) return fusionPriority.intervalMillis
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
