package com.example.pumppaperbot

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class MarketSyncClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun sync(context: Context) {
        val pump = fetch(PumpBotEngine.klineUrl(PumpBotEngine.pumpSymbol, "30m"))
        val eur = fetch(PumpBotEngine.klineUrl(PumpBotEngine.eurSymbol, "30m"))
        val btc = fetch(PumpBotEngine.klineUrl(PumpBotEngine.btcSymbol, "30m"))
        val funding = fetch(PumpBotEngine.fundingUrl())
        PumpBotEngine.syncMarket(context, pump, eur, btc, funding)
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
