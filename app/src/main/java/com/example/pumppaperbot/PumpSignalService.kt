package com.example.pumppaperbot

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PumpSignalService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val market = MarketSyncClient()

    private val loop = object : Runnable {
        override fun run() {
            checkNow()
            handler.postDelayed(this, TimeUnit.MINUTES.toMillis(2))
        }
    }

    override fun onCreate() {
        super.onCreate()
        PumpAlert.ensureChannels(this)
        startForeground(
            PumpAlert.monitorId(),
            PumpAlert.monitorNotification(this, "Проверяет PUMP примерно каждые 2 минуты.")
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        PumpBotEngine.setRunning(this, true)
        handler.removeCallbacks(loop)
        handler.post(loop)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(loop)
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkNow() {
        executor.execute {
            try {
                market.sync(this)
                val snapshot = PumpBotEngine.snapshot(this)
                val rapidDropAlerted = if (PumpBotEngine.shouldAlertRapidDrop(this, snapshot)) {
                    PumpAlert.showRapidDrop(this, snapshot)
                    PumpBotEngine.markRapidDropAlerted(this, snapshot)
                    true
                } else false
                if (!rapidDropAlerted && PumpBotEngine.shouldAlert(this, snapshot)) {
                    PumpAlert.showSignal(this, snapshot)
                    PumpBotEngine.markAlerted(this, snapshot)
                }
            } catch (_: Exception) {
                // Следующая проверка попробует еще раз.
            }
        }
    }
}
