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
        microImpulse = MicroImpulseStream(this)
        startForeground(
            PumpAlert.monitorId(),
            PumpAlert.monitorNotification(
                this,
                "V5: APP, DeepSig и DeepSigX ведут отдельные виртуальные счета; " +
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

    override fun onDestroy() {
        destroyed = true
        handler.removeCallbacks(loop)
        microImpulse.stop()
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
                    DeepSeekEvidenceMemory.updateOutcomes(this, freshPrice, evidenceNow)
                }
                runCatching { pumpEcosystem.sync(this) }
                BitpandaFusionClient().sync(this)
                val eventState = eventRadar.sync(this)
                val personalGuard = PersonalPositionGuardStore.sync(this)
                FusionSimStore.activate(this, DeepSeekPrimaryStore.state(this).lastSuccess)
                DeepSeekTradeOwnership.activate(this, DeepSeekPrimaryStore.state(this).lastSuccess)
                val deepSeek = DeepSeekPrimaryAnalyst().sync(this)
                PositionSupervisorClient().sync(this, forceCritical = personalGuard.forceCriticalAi)
                GeminiPositionAdvisorClient().sync(this, forceCritical = personalGuard.forceCriticalAi)
                GeminiPaperStore.markDataReady(this, source, startedAt)
                val snapshot = PumpBotEngine.snapshot(this)
                val appTrade = AppPaperStore.syncWithAlerts(this)
                val deepSeekPaper = DeepSeekPaperCoordinator().sync(this, deepSeek, source)
                FusionSimStore.sync(this, deepSeek)
                val rapidDropAlerted = if (PumpBotEngine.shouldAlertRapidDrop(this, snapshot)) {
                    PumpAlert.showRapidDrop(this, snapshot)
                    PumpBotEngine.markRapidDropAlerted(this, snapshot)
                    true
                } else false
                val signalAlerted = if (!rapidDropAlerted && !appTrade.tradeAlerted && PumpBotEngine.shouldAlert(this, snapshot)) {
                    PumpAlert.showSignal(this, snapshot)
                    PumpBotEngine.markAlerted(this, snapshot)
                    true
                } else false
                if (!rapidDropAlerted && !appTrade.tradeAlerted && !signalAlerted &&
                    EventRadarStore.shouldAlert(this, eventState)
                ) {
                    PumpAlert.showEventRadar(this, eventState, snapshot)
                    EventRadarStore.markAlerted(this, eventState)
                }
                EntryAlertReminderStore.flush(this)
                val finishedAt = System.currentTimeMillis()
                UnifiedResearchLog.captureCycle(this, source, finishedAt)
                GeminiPaperStore.finishCycle(
                    this,
                    source,
                    startedAt,
                    finishedAt + cycleIntervalMillis,
                    "проверка завершена; DeepSig: ${deepSeek.action}; виртуальный счёт: ${deepSeekPaper.status}; Gemini контролирует только открытую позицию Сержа",
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
