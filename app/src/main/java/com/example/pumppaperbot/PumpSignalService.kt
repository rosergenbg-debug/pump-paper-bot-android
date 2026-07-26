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
    private val cycleIntervalMillis = TimeUnit.MINUTES.toMillis(2)
    private val cycleQueuedOrRunning = AtomicBoolean(false)

    private val loop = object : Runnable {
        override fun run() {
            checkNow()
            handler.postDelayed(this, cycleIntervalMillis)
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
            val source = "МОНИТОР 2 МИН"
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
                val eventState = eventRadar.sync(this)
                GeminiPaperStore.markDataReady(this, source, startedAt)
                val snapshot = PumpBotEngine.snapshot(this)
                val gemini = GeminiExperimentClient().sync(this, source = source)
                val rapidDropAlerted = if (PumpBotEngine.shouldAlertRapidDrop(this, snapshot)) {
                    PumpAlert.showRapidDrop(this, snapshot)
                    PumpBotEngine.markRapidDropAlerted(this, snapshot)
                    true
                } else false
                val signalAlerted = if (!rapidDropAlerted && PumpBotEngine.shouldAlert(this, snapshot)) {
                    PumpAlert.showSignal(this, snapshot)
                    PumpBotEngine.markAlerted(this, snapshot)
                    true
                } else false
                if (!rapidDropAlerted && !signalAlerted && EventRadarStore.shouldAlert(this, eventState)) {
                    PumpAlert.showEventRadar(this, eventState, snapshot)
                    EventRadarStore.markAlerted(this, eventState)
                }
                val finishedAt = System.currentTimeMillis()
                GeminiPaperStore.finishCycle(
                    this,
                    source,
                    startedAt,
                    finishedAt + cycleIntervalMillis,
                    "проверка завершена; Gemini: ${gemini.status}",
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
            }
        }
    }
}
