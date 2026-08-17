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
                DeepSeekEvidenceMemory.updateOutcomes(applicationContext, freshPrice, evidenceNow)
            }
            runCatching { pumpEcosystem.sync(applicationContext) }
            BitpandaFusionClient().sync(applicationContext)
            val eventState = eventRadar.sync(applicationContext)
            val personalGuard = PersonalPositionGuardStore.sync(applicationContext)
            FusionSimStore.activate(
                applicationContext,
                DeepSeekPrimaryStore.state(applicationContext).lastSuccess
            )
            DeepSeekTradeOwnership.activate(
                applicationContext,
                DeepSeekPrimaryStore.state(applicationContext).lastSuccess
            )
            val deepSeek = DeepSeekPrimaryAnalyst().sync(
                applicationContext,
                force = forcePositionPro || forcePrimaryDeepSeek
            )
            PositionSupervisorClient().sync(
                applicationContext,
                forceCritical = forcePositionPro || personalGuard.forceCriticalAi
            )
            GeminiPositionAdvisorClient().sync(
                applicationContext,
                forceCritical = forcePositionPro || personalGuard.forceCriticalAi
            )
            GeminiPaperStore.markDataReady(
                applicationContext,
                source,
                startedAt
            )
            val snapshot = PumpBotEngine.snapshot(applicationContext)
            val appTrade = AppPaperStore.syncWithAlerts(applicationContext)
            val deepSeekPaper = DeepSeekPaperCoordinator().sync(
                applicationContext, deepSeek, source
            )
            FusionSimStore.sync(applicationContext, deepSeek)
            val rapidDropAlerted = if (PumpBotEngine.shouldAlertRapidDrop(applicationContext, snapshot)) {
                PumpAlert.showRapidDrop(applicationContext, snapshot)
                PumpBotEngine.markRapidDropAlerted(applicationContext, snapshot)
                true
            } else false
            val signalAlerted = if (!rapidDropAlerted && !appTrade.tradeAlerted && PumpBotEngine.shouldAlert(applicationContext, snapshot)) {
                PumpAlert.showSignal(applicationContext, snapshot)
                PumpBotEngine.markAlerted(applicationContext, snapshot)
                true
            } else false
            if (!rapidDropAlerted && !appTrade.tradeAlerted && !signalAlerted &&
                EventRadarStore.shouldAlert(applicationContext, eventState)
            ) {
                PumpAlert.showEventRadar(applicationContext, eventState, snapshot)
                EventRadarStore.markAlerted(applicationContext, eventState)
            }
            EntryAlertReminderStore.flush(applicationContext)
            val finishedAt = System.currentTimeMillis()
            UnifiedResearchLog.captureCycle(applicationContext, source, finishedAt)
            GeminiPaperStore.finishCycle(
                applicationContext,
                source,
                startedAt,
                finishedAt + interval,
                "проверка завершена; DeepSeek: ${deepSeek.action}; виртуальный счёт: ${deepSeekPaper.status}; Gemini контролирует только открытую позицию Сержа",
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
