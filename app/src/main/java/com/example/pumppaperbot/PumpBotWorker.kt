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

    override fun doWork(): Result {
        val source = inputData.getString(INPUT_CYCLE_SOURCE) ?: "ANDROID РЕЗЕРВ 15 МИН"
        val interval = inputData.getLong(
            INPUT_CYCLE_INTERVAL,
            TimeUnit.MINUTES.toMillis(15)
        )
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
            val eventState = eventRadar.sync(applicationContext)
            GeminiPaperStore.markDataReady(
                applicationContext,
                source,
                startedAt
            )
            val snapshot = PumpBotEngine.snapshot(applicationContext)
            val appTrade = AppPaperStore.syncWithAlerts(applicationContext)
            val gemini = GeminiExperimentClient().sync(
                applicationContext,
                source = source
            )
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
            val finishedAt = System.currentTimeMillis()
            GeminiPaperStore.finishCycle(
                applicationContext,
                source,
                startedAt,
                finishedAt + interval,
                "проверка завершена; Gemini: ${gemini.status}",
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
    }
}
