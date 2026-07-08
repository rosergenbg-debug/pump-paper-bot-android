package com.example.pumppaperbot

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PumpSignalService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

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
                val request = Request.Builder()
                    .url(PumpBotEngine.klineUrl(PumpBotEngine.pumpSymbol, "30m"))
                    .header("Accept", "application/json")
                    .header("User-Agent", "PumpRsiRiskBotAndroid/${PumpBotEngine.appVersionName}")
                    .build()
                val json = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    response.body?.string().orEmpty()
                }
                PumpBotEngine.syncPump(this, json)
                val snapshot = PumpBotEngine.snapshot(this)
                if (PumpBotEngine.shouldAlert(this, snapshot)) {
                    PumpAlert.showSignal(this, snapshot)
                    PumpBotEngine.markAlerted(this, snapshot)
                }
            } catch (_: Exception) {
                // Следующая проверка попробует еще раз.
            }
        }
    }
}
