package com.example.pumppaperbot

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class PumpBotWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    private val market = MarketSyncClient()
    private val eventRadar = EventRadarClient()
    private val pumpEcosystem = PumpEcosystemClient()

    override fun doWork(): Result {
        val requestedSource = inputData.getString(INPUT_CYCLE_SOURCE) ?: "ANDROID РЕЗЕРВ 15 МИН"
        val fusionPriorityAtStart = FusionPriorityPolicy.plan(FusionSimStore.state(applicationContext))
        val source = if (fusionPriorityAtStart.active) {
            "${fusionPriorityAtStart.label} • $requestedSource"
        } else requestedSource
        val interval = inputData.getLong(
            INPUT_CYCLE_INTERVAL,
            TimeUnit.MINUTES.toMillis(15)
        )
        val forcePositionPro = inputData.getBoolean(INPUT_FORCE_POSITION_PRO, false)
        val forcePrimaryDeepSeek = inputData.getBoolean(INPUT_FORCE_PRIMARY_DEEPSEEK, false)
        if (!GeminiCycleGuard.tryEnter()) {
            GeminiPaperStore.recordActivity(
                applicationContext,
                "ЦИКЛ",
                "WAIT",
                "$source: пропущен, потому что предыдущая проверка ещё выполняется"
            )
            return Result.success()
        }
        val startedAt = System.currentTimeMillis()
        GeminiPaperStore.beginCycle(applicationContext, source, interval, startedAt)
        return try {
            market.sync(applicationContext)
            val marketSnapshot = PumpBotEngine.snapshot(applicationContext)
            val evidenceNow = System.currentTimeMillis()
            PaperExecutionPolicy.freshLivePrice(marketSnapshot, evidenceNow)?.let { freshPrice ->
                CycleStageGuard.run(applicationContext, "EVIDENCE_OUTCOMES", { Unit }) {
                    DeepSeekEvidenceMemory.updateOutcomes(applicationContext, freshPrice, evidenceNow)
                }
            }
            CycleStageGuard.run(applicationContext, "PUMP_ECOSYSTEM", {
                PumpEcosystemStore.state(applicationContext)
            }) { pumpEcosystem.sync(applicationContext) }
            CycleStageGuard.run(applicationContext, "BITPANDA_FUSION", {
                BitpandaFusionStore.state(applicationContext)
            }) { BitpandaFusionClient().sync(applicationContext) }
            val eventState = CycleStageGuard.run(applicationContext, "EVENT_RADAR", {
                EventRadarStore.state(applicationContext)
            }) { eventRadar.sync(applicationContext) }
            val personalGuard = CycleStageGuard.run(applicationContext, "PERSONAL_GUARD", {
                PersonalPositionGuardOutcome(PersonalPositionGuardStore.state(applicationContext))
            }) { PersonalPositionGuardStore.sync(applicationContext) }
            CycleStageGuard.run(applicationContext, "FUSION_ACTIVATION", { Unit }) {
                FusionSimStore.activate(
                    applicationContext,
                    DeepSeekPrimaryStore.state(applicationContext).lastSuccess
                )
            }
            CycleStageGuard.run(applicationContext, "DEEPSIG_OWNERSHIP", { Unit }) {
                DeepSeekTradeOwnership.activate(
                    applicationContext,
                    DeepSeekPrimaryStore.state(applicationContext).lastSuccess
                )
            }
            val deepSeek = CycleStageGuard.run(applicationContext, "DEEPSIG_PRIMARY", {
                DeepSeekPrimaryStore.state(applicationContext)
            }) {
                DeepSeekPrimaryAnalyst().sync(
                    applicationContext,
                    force = forcePositionPro || forcePrimaryDeepSeek
                )
            }
            CycleStageGuard.run(applicationContext, "POSITION_SUPERVISOR", {
                PositionSupervisorStore.state(applicationContext)
            }) {
                PositionSupervisorClient().sync(
                    applicationContext,
                    forceCritical = forcePositionPro || personalGuard.forceCriticalAi
                )
            }
            CycleStageGuard.run(applicationContext, "GEMINI_POSITION", {
                GeminiPositionAdvisorStore.state(applicationContext)
            }) {
                GeminiPositionAdvisorClient().sync(
                    applicationContext,
                    forceCritical = forcePositionPro || personalGuard.forceCriticalAi
                )
            }
            CycleStageGuard.run(applicationContext, "CYCLE_STATUS", { Unit }) {
                GeminiPaperStore.markDataReady(applicationContext, source, startedAt)
            }
            val snapshot = PumpBotEngine.snapshot(applicationContext)
            val userPositionOpen = snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0
            val appTrade = CycleStageGuard.run(applicationContext, "APP_PAPER", {
                AppPaperSyncResult(AppPaperStore.state(applicationContext), false)
            }) { AppPaperStore.syncWithAlerts(applicationContext) }
            val pumpMachine = CycleStageGuard.run(applicationContext, "PUMP_MACHINE", {
                PumpMachineSyncResult(
                    PumpMachineStore.state(applicationContext),
                    "ошибка Pump 3 изолирована",
                    0.0
                )
            }) { PumpMachineStore.sync(applicationContext) }
            CycleStageGuard.run(applicationContext, "PUMP_MACHINE_2", {
                PumpMachine2SyncResult(
                    PumpMachine2Store.state(applicationContext),
                    "ошибка Pump 2 изолирована",
                    0.0
                )
            }) { PumpMachine2Store.sync(applicationContext) }
            CycleStageGuard.run(applicationContext, "PUMP_MACHINE_RETEST", {
                PumpVariantSyncResult(PumpMachineRetestStore.state(applicationContext), "ошибка Pump Retest изолирована", 0.0)
            }) { PumpMachineRetestStore.sync(applicationContext) }
            CycleStageGuard.run(applicationContext, "PUMP_MACHINE_SAFE", {
                PumpVariantSyncResult(PumpMachineSafeStore.state(applicationContext), "ошибка Pump Safe изолирована", 0.0)
            }) { PumpMachineSafeStore.sync(applicationContext) }
            CycleStageGuard.run(applicationContext, "FUSION_SIM", {
                FusionSimStore.state(applicationContext)
            }) { FusionSimStore.sync(applicationContext, deepSeek) }
            CycleStageGuard.run(applicationContext, "ENTRY_GATE_AUDIT", {
                EntryOpportunityAuditStore.latest(applicationContext)
            }) { EntryOpportunityAuditStore.capture(applicationContext) }
            val rapidDropAlerted = CycleStageGuard.run(applicationContext, "RAPID_DROP_ALERT", { false }) {
                if (PumpBotEngine.shouldAlertRapidDrop(applicationContext, snapshot)) {
                    PumpAlert.showRapidDrop(applicationContext, snapshot)
                    PumpBotEngine.markRapidDropAlerted(applicationContext, snapshot)
                    true
                } else false
            }
            val signalAlerted = CycleStageGuard.run(applicationContext, "SIGNAL_ALERT", { false }) {
                if (!rapidDropAlerted && !appTrade.tradeAlerted &&
                    (!userPositionOpen || snapshot.sellSignal) && PumpBotEngine.shouldAlert(applicationContext, snapshot)
                ) {
                    PumpAlert.showSignal(applicationContext, snapshot)
                    PumpBotEngine.markAlerted(applicationContext, snapshot)
                    true
                } else false
            }
            if (!userPositionOpen && !rapidDropAlerted && !appTrade.tradeAlerted && !signalAlerted &&
                EventRadarStore.shouldAlert(applicationContext, eventState)
            ) {
                CycleStageGuard.run(applicationContext, "EVENT_ALERT", { Unit }) {
                    PumpAlert.showEventRadar(applicationContext, eventState, snapshot)
                    EventRadarStore.markAlerted(applicationContext, eventState)
                }
            }
            CycleStageGuard.run(applicationContext, "ENTRY_REMINDER", { Unit }) {
                EntryAlertReminderStore.flush(applicationContext)
            }
            val finishedAt = System.currentTimeMillis()
            CycleStageGuard.run(applicationContext, "PERFORMANCE_LEDGER", { Unit }) {
                ResearchPerformanceLedger.capture(applicationContext)
            }
            CycleStageGuard.run(applicationContext, "UNIFIED_LOG", { Unit }) {
                UnifiedResearchLog.captureCycle(applicationContext, source, finishedAt)
            }
            GeminiPaperStore.finishCycle(
                applicationContext,
                source,
                startedAt,
                finishedAt + interval,
                "проверка завершена; DeepSeek аналитик: ${deepSeek.action}; Pump Machine: ${pumpMachine.status}; Gemini контролирует только открытую позицию Сержа",
                finishedAt
            )
            Result.success()
        } catch (e: Exception) {
            val failedAt = System.currentTimeMillis()
            GeminiPaperStore.failCycle(
                applicationContext,
                source,
                startedAt,
                failedAt + interval,
                e.message ?: e.javaClass.simpleName,
                failedAt
            )
            Result.retry()
        } finally {
            GeminiCycleGuard.exit()
        }
    }

    companion object {
        const val INPUT_CYCLE_SOURCE = "cycle_source"
        const val INPUT_CYCLE_INTERVAL = "cycle_interval"
        const val INPUT_FORCE_POSITION_PRO = "force_position_pro"
        const val INPUT_FORCE_PRIMARY_DEEPSEEK = "force_primary_deepseek"
    }
}
