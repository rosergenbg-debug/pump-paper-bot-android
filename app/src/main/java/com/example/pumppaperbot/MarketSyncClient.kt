package com.example.pumppaperbot

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.TreeMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MarketSyncClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun sync(context: Context) {
        val pool = Executors.newFixedThreadPool(10)
        try {
            val pump = pool.submit<String> { fetchRecentSpot(PumpBotEngine.pumpSymbol) }
            val eur = pool.submit<String> { fetchRecentSpot(PumpBotEngine.eurSymbol) }
            val btc = pool.submit<String> { fetchRecentSpot(PumpBotEngine.btcSymbol) }
            val eth = pool.submit<String> { fetch(PumpBotEngine.klineUrl(PumpBotEngine.ethSymbol, "30m")) }
            val sol = pool.submit<String> { fetchRecentSpot(PumpBotEngine.solSymbol) }
            val futures = pool.submit<String> { fetch(PumpBotEngine.futuresKlineUrl()) }
            val premium = pool.submit<String> { fetch(PumpBotEngine.premiumKlineUrl()) }
            val funding = pool.submit<String> { fetch(PumpBotEngine.fundingUrl()) }
            val depth = pool.submit<String> { runCatching { fetch(PumpBotEngine.depthUrl()) }.getOrDefault("") }
            val openInterest = pool.submit<String> { runCatching { fetch(PumpBotEngine.openInterestUrl()) }.getOrDefault("") }
            PumpBotEngine.syncMarket(
                context,
                pump.get(), eur.get(), btc.get(), eth.get(), sol.get(),
                futures.get(), premium.get(), funding.get(), depth.get(), openInterest.get()
            )
        } finally {
            pool.shutdownNow()
        }
    }

    private fun fetchRecentSpot(symbol: String): String {
        val end = System.currentTimeMillis()
        val start = end - (recentSpotBars + 20L) * candleMillis
        val rowsByClose = TreeMap<Long, String>()
        var cursor = start
        while (cursor < end) {
            val body = fetch(PumpBotEngine.historicalKlineUrl(symbol, "30m", cursor, end))
            val batch = JSONArray(body)
            if (batch.length() == 0) break
            var lastClose = cursor
            for (index in 0 until batch.length()) {
                val row = batch.getJSONArray(index)
                val closeTime = row.getLong(6)
                lastClose = maxOf(lastClose, closeTime)
                if (closeTime < end) rowsByClose[closeTime] = row.toString()
            }
            val next = lastClose + 1L
            if (next <= cursor) break
            cursor = next
        }
        val selected = rowsByClose.entries.toList().takeLast(recentSpotBars)
        return JSONArray().apply {
            selected.forEach { put(JSONArray(it.value)) }
        }.toString()
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

    private companion object {
        const val recentSpotBars = 1500
        const val candleMillis = 30L * 60L * 1000L
    }
}
