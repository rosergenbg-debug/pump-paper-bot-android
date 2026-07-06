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
        val current = PumpBotEngine.load(applicationContext)
        if (!current.running) return Result.success()

        return try {
            val request = Request.Builder()
                .url(PumpBotEngine.klineUrl)
                .header("Accept", "application/json")
                .header("User-Agent", "PumpPaperBotAndroid/0.1")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return Result.retry()
                val json = response.body?.string().orEmpty()
                val candles = PumpBotEngine.parseCandles(json)
                val result = PumpBotEngine.evaluate(current, candles)
                PumpBotEngine.save(applicationContext, result.state)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
