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

/**
 * V6.6.1 focused foreground monitor.
 * Three new V6.6 autos + HUMAN run alongside the permanent preserved APP paper account.
 * Other legacy automatic research engines remain dormant. SERGE is the owner's manual account.
 */
class PumpSignalService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val queuedOrRunning = AtomicBoolean(false)
    private val market = MarketSyncClient()
    private val ecosystem = PumpEcosystemClient()
    private val fastIntervalMillis = TimeUnit.SECONDS.toMillis(30)
    private val fullMarketIntervalMillis = TimeUnit.MINUTES.toMillis(2)
    @Volatile private var destroyed = false
    @Volatile private var lastFullMarketSyncAt = 0L

    private val loop = object : Runnable {
        override fun run() {
            checkNow()
            if (!destroyed) handler.postDelayed(this, fastIntervalMillis)
        }
    }

    override fun onCreate() {
        super.onCreate()
        destroyed = false
        PumpAlert.ensureChannels(this)
        startForeground(
            PumpAlert.monitorId(),
            PumpAlert.monitorNotification(
                this,
                "V${BuildConfig.VERSION_NAME}: 3 X AUTO + HUMAN + preserved SERGE/APP. Paper-only."
            )
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        PumpBotEngine.setRunning(this, true)
        handler.removeCallbacks(loop)
        handler.post(loop)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        destroyed = true
        handler.removeCallbacks(loop)
        executor.shutdownNow()
        HumanFactorAlarmV650.cancel(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkNow() {
        if (!queuedOrRunning.compareAndSet(false, true)) return
        executor.execute {
            val startedAt = System.currentTimeMillis()
            try {
                if (lastFullMarketSyncAt == 0L || startedAt - lastFullMarketSyncAt >= fullMarketIntervalMillis) {
                    runCatching { market.sync(this) }
                        .onSuccess { lastFullMarketSyncAt = System.currentTimeMillis() }
                        .onFailure { logError("MARKET_30M", it) }
                    runCatching { ecosystem.sync(this) }.onFailure { logError("PUMP_ECOSYSTEM", it) }
                }

                // 30-second live path feeds the progressive 0-100 readiness gauge.
                runCatching { ChartMarketClient().sync(this, ChartInterval.ONE_MINUTE) }
                    .onFailure { logError("MARKET_1M", it) }
                runCatching { BitpandaFusionClient().sync(this, force = false) }
                    .onFailure { logError("BITPANDA", it) }

                val now = System.currentTimeMillis()
                runCatching { T32NetworkV660.syncAll(this, now) }
                    .onFailure { logError("V660_AUTOS", it) }
                runCatching { HumanFactorStore.sync(this, now) }
                    .onFailure { logError("V660_HUMAN", it) }

                // APP is a permanent owner-facing paper account. Keep its original prefs/history
                // and original StrategyV2 engine alive. This does NOT reactivate other V6.5 engines.
                // Its alert delivery already honours the global master switch and schedule.
                runCatching { AppPaperStore.syncWithAlerts(this) }
                    .onFailure { logError("APP_PAPER", it) }

                // Absolute sound gate for surviving personal safety-alert routes.
                // HUMAN has the same master/schedule checks internally, including while a position is open.
                val canRingNow = ResearchModePolicy.alertsEnabled(this) && AlertSchedule.isAllowedNow(this)
                if (canRingNow) {
                    runCatching { FastPositionWarningStore.sync(this, now) }
                        .onFailure { logError("PERSONAL_WARNING", it) }

                    val snapshot = PumpBotEngine.snapshot(this)
                    val userPositionOpen = snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0
                    if (userPositionOpen && snapshot.sellSignal && PumpBotEngine.shouldAlert(this, snapshot)) {
                        runCatching {
                            PumpAlert.showSignal(this, snapshot)
                            PumpBotEngine.markAlerted(this, snapshot)
                        }.onFailure { logError("PERSONAL_SIGNAL", it) }
                    }
                    if (PumpBotEngine.shouldAlertRapidDrop(this, snapshot)) {
                        runCatching {
                            PumpAlert.showRapidDrop(this, snapshot)
                            PumpBotEngine.markRapidDropAlerted(this, snapshot)
                        }.onFailure { logError("RAPID_DROP", it) }
                    }
                    runCatching { EntryAlertReminderStore.flush(this) }
                } else {
                    runCatching { EntryAlertReminderStore.clear(this) }
                    runCatching { PumpAlert.silenceUserAlerts(this) }
                    if (!ResearchModePolicy.alertsEnabled(this)) {
                        runCatching { HumanFactorAlarmV650.cancel(this) }
                    }
                }

                runCatching { UnifiedResearchLog.captureCycle(this, "V661_FOCUSED_30S", now) }
            } catch (error: Throwable) {
                logError("V661_CYCLE", error)
            } finally {
                queuedOrRunning.set(false)
            }
        }
    }

    private fun logError(stage: String, error: Throwable) {
        UnifiedResearchLog.record(
            this,
            stage,
            "ERROR",
            "${error.javaClass.simpleName}: ${error.message.orEmpty().take(180)}",
            System.currentTimeMillis()
        )
    }
}
