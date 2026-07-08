package com.example.pumppaperbot

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class PumpBotWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override fun doWork(): Result {
        return try {
            val json = fetch(PumpBotEngine.klineUrl(PumpBotEngine.pumpSymbol, "30m"))
            PumpBotEngine.syncPump(applicationContext, json)
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

    private fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "PumpRsiRiskBotAndroid/${PumpBotEngine.appVersionName}")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }
}
