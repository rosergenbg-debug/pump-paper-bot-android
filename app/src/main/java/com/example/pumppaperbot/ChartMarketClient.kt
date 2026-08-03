package com.example.pumppaperbot

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ChartMarketClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    fun syncSelected(context: Context) {
        val interval = ChartSpeedStore.selected(context)
        if (interval == ChartInterval.THIRTY_MINUTES) return
        val pool = Executors.newFixedThreadPool(2)
        try {
            val pump = pool.submit<String> {
                fetch(PumpBotEngine.klineUrl(PumpBotEngine.pumpSymbol, interval.code, 360))
            }
            val eur = pool.submit<String> {
                fetch(PumpBotEngine.klineUrl(PumpBotEngine.eurSymbol, interval.code, 360))
            }
            val pumpJson = pump.get()
            val eurJson = eur.get()
            require(JSONArray(pumpJson).length() > 0 && JSONArray(eurJson).length() > 0) {
                "Binance не вернул свечи ${interval.subtitleLabel}"
            }
            ChartSpeedStore.save(context, interval, pumpJson, eurJson)
        } catch (error: Exception) {
            ChartSpeedStore.recordError(
                context,
                interval,
                error.cause?.message ?: error.message ?: "ошибка загрузки графика"
            )
        } finally {
            pool.shutdownNow()
        }
    }

    private fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "PumpSignalAndroid/${PumpBotEngine.appVersionName}")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}: ${response.request.url}")
            return response.body?.string().orEmpty()
        }
    }
}
