package com.example.pumppaperbot

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MarketSyncClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun sync(context: Context) {
        val pool = Executors.newFixedThreadPool(8)
        try {
            val pump = pool.submit<String> { fetch(PumpBotEngine.klineUrl(PumpBotEngine.pumpSymbol, "30m")) }
            val eur = pool.submit<String> { fetch(PumpBotEngine.klineUrl(PumpBotEngine.eurSymbol, "30m")) }
            val btc = pool.submit<String> { fetch(PumpBotEngine.klineUrl(PumpBotEngine.btcSymbol, "30m")) }
            val eth = pool.submit<String> { fetch(PumpBotEngine.klineUrl(PumpBotEngine.ethSymbol, "30m")) }
            val sol = pool.submit<String> { fetch(PumpBotEngine.klineUrl(PumpBotEngine.solSymbol, "30m")) }
            val futures = pool.submit<String> { fetch(PumpBotEngine.futuresKlineUrl()) }
            val premium = pool.submit<String> { fetch(PumpBotEngine.premiumKlineUrl()) }
            val funding = pool.submit<String> { fetch(PumpBotEngine.fundingUrl()) }
            PumpBotEngine.syncMarket(
                context,
                pump.get(), eur.get(), btc.get(), eth.get(), sol.get(),
                futures.get(), premium.get(), funding.get()
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
