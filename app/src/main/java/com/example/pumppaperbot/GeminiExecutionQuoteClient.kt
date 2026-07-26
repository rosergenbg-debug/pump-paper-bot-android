package com.example.pumppaperbot

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class GeminiExecutionQuote(
    val priceEur: Double,
    val receivedAt: Long
)

/**
 * Obtains a new PUMP/EUR execution quote only after the Gemini response has
 * been fully received and parsed.
 */
class GeminiExecutionQuoteClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    fun fetch(): GeminiExecutionQuote {
        val pool = Executors.newFixedThreadPool(2)
        return try {
            val pump = pool.submit<Double> { fetchPrice(PumpBotEngine.tickerUrl(PumpBotEngine.pumpSymbol)) }
            val eur = pool.submit<Double> { fetchPrice(PumpBotEngine.tickerUrl(PumpBotEngine.eurSymbol)) }
            val pumpUsdt = pump.get()
            val eurUsdt = eur.get()
            require(pumpUsdt > 0.0 && eurUsdt > 0.0) { "Некорректная свежая котировка" }
            GeminiExecutionQuote(
                priceEur = pumpUsdt / eurUsdt,
                receivedAt = System.currentTimeMillis()
            )
        } finally {
            pool.shutdownNow()
        }
    }

    private fun fetchPrice(url: String): Double {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "PumpSignalAndroid/${PumpBotEngine.appVersionName}")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Котировка HTTP ${response.code}")
            PumpBotEngine.parseTickerPrice(response.body?.string().orEmpty())
                ?: error("Пустая котировка")
        }
    }
}
