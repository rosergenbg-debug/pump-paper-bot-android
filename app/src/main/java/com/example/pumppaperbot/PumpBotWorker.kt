package com.example.pumppaperbot

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class PumpBotWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    private val market = MarketSyncClient()
    private val eventRadar = EventRadarClient()

    override fun doWork(): Result {
        return try {
            market.sync(applicationContext)
            val eventState = eventRadar.sync(applicationContext)
            val snapshot = PumpBotEngine.snapshot(applicationContext)
            val rapidDropAlerted = if (PumpBotEngine.shouldAlertRapidDrop(applicationContext, snapshot)) {
                PumpAlert.showRapidDrop(applicationContext, snapshot)
                PumpBotEngine.markRapidDropAlerted(applicationContext, snapshot)
                true
            } else false
            val signalAlerted = if (!rapidDropAlerted && PumpBotEngine.shouldAlert(applicationContext, snapshot)) {
                PumpAlert.showSignal(applicationContext, snapshot)
                PumpBotEngine.markAlerted(applicationContext, snapshot)
                true
            } else false
            if (!rapidDropAlerted && !signalAlerted && EventRadarStore.shouldAlert(applicationContext, eventState)) {
                PumpAlert.showEventRadar(applicationContext, eventState, snapshot)
                EventRadarStore.markAlerted(applicationContext, eventState)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

}
