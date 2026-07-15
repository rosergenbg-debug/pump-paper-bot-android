package com.example.pumppaperbot

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class PumpBotWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    private val market = MarketSyncClient()

    override fun doWork(): Result {
        return try {
            market.sync(applicationContext)
            val snapshot = PumpBotEngine.snapshot(applicationContext)
            if (PumpBotEngine.shouldAlert(applicationContext, snapshot)) {
                PumpAlert.showSignal(applicationContext, snapshot)
                PumpBotEngine.markAlerted(applicationContext, snapshot)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

}
