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
            val json4h = fetch(PumpBotEngine.klineUrl("30m"))
            val json2h = fetch(PumpBotEngine.klineUrl("2h"))
            PumpBotEngine.sync(
                context = applicationContext,
                json4h = json4h,
                json2h = json2h,
                allowTrading = PumpBotEngine.isRunning(applicationContext)
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "PumpPaperBotAndroid/${PumpBotEngine.appVersionName}")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }
}
