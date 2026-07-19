package com.example.pumppaperbot

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.TreeMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MarketSyncClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun sync(context: Context) {
        val now = System.currentTimeMillis()
        val saved = PumpBotEngine.savedMarketPayloads(context)
        val pool = Executors.newFixedThreadPool(12)
        try {
            val pump = pool.submit<String> {
                updateKlines(saved.pumpJson, recentSpotBars, now) { start, end ->
                    PumpBotEngine.historicalKlineUrl(PumpBotEngine.pumpSymbol, "30m", start, end)
                }
            }
            val eur = pool.submit<String> {
                updateKlines(saved.eurJson, recentSpotBars, now) { start, end ->
                    PumpBotEngine.historicalKlineUrl(PumpBotEngine.eurSymbol, "30m", start, end)
                }
            }
            val btc = pool.submit<String> {
                updateKlines(saved.btcJson, recentSpotBars, now) { start, end ->
                    PumpBotEngine.historicalKlineUrl(PumpBotEngine.btcSymbol, "30m", start, end)
                }
            }
            val eth = pool.submit<String> {
                updateKlines(saved.ethJson, recentDerivativeBars, now) { start, end ->
                    PumpBotEngine.historicalKlineUrl(PumpBotEngine.ethSymbol, "30m", start, end)
                }
            }
            val sol = pool.submit<String> {
                updateKlines(saved.solJson, recentSpotBars, now) { start, end ->
                    PumpBotEngine.historicalKlineUrl(PumpBotEngine.solSymbol, "30m", start, end)
                }
            }
            val futures = pool.submit<String> {
                updateKlines(saved.futuresJson, recentDerivativeBars, now) { start, end ->
                    PumpBotEngine.historicalFuturesKlineUrl(PumpBotEngine.pumpSymbol, "30m", start, end)
                }
            }
            val premium = pool.submit<String> {
                updateKlines(saved.premiumJson, recentDerivativeBars, now) { start, end ->
                    PumpBotEngine.historicalPremiumKlineUrl(PumpBotEngine.pumpSymbol, "30m", start, end)
                }
            }
            val funding = pool.submit<String> { updateFunding(saved.fundingJson, now) }
            val depth = pool.submit<String> { runCatching { fetch(PumpBotEngine.depthUrl()) }.getOrDefault("") }
            val openInterest = pool.submit<String> { runCatching { fetch(PumpBotEngine.openInterestUrl()) }.getOrDefault("") }
            val pumpTicker = pool.submit<String> { runCatching { fetch(PumpBotEngine.tickerUrl(PumpBotEngine.pumpSymbol)) }.getOrDefault("") }
            val eurTicker = pool.submit<String> { runCatching { fetch(PumpBotEngine.tickerUrl(PumpBotEngine.eurSymbol)) }.getOrDefault("") }
            PumpBotEngine.syncMarket(
                context,
                pump.get(), eur.get(), btc.get(), eth.get(), sol.get(),
                futures.get(), premium.get(), funding.get(), depth.get(), openInterest.get(),
                pumpTicker.get(), eurTicker.get()
            )
        } finally {
            pool.shutdownNow()
        }
    }

    private fun updateKlines(
        existingJson: String,
        limit: Int,
        now: Long,
        url: (Long, Long) -> String
    ): String {
        val lastClosed = IncrementalMarketHistory.lastClosedKlineTime(existingJson, now)
        if (!IncrementalMarketHistory.needsKlineRefresh(lastClosed, now)) return existingJson
        val start = lastClosed?.plus(1L)
            ?: (now - (limit + initialPaddingBars.toLong()) * candleMillis).coerceAtLeast(0L)
        val fresh = fetchKlineRange(start, now, url)
        val merged = IncrementalMarketHistory.mergeKlines(existingJson, fresh, limit, now)
        if (IncrementalMarketHistory.lastClosedKlineTime(merged, now) == null) {
            error("Binance не вернул закрытые свечи")
        }
        return merged
    }

    private fun fetchKlineRange(start: Long, end: Long, url: (Long, Long) -> String): String {
        val rows = JSONArray()
        var cursor = start
        while (cursor < end) {
            val body = fetch(url(cursor, end))
            val batch = JSONArray(body)
            if (batch.length() == 0) break
            var lastClose = cursor
            for (index in 0 until batch.length()) {
                val row = batch.getJSONArray(index)
                val closeTime = row.getLong(6)
                lastClose = maxOf(lastClose, closeTime)
                rows.put(row)
            }
            val next = lastClose + 1L
            if (next <= cursor) break
            cursor = next
        }
        return rows.toString()
    }

    private fun updateFunding(existingJson: String, now: Long): String {
        val lastFunding = IncrementalMarketHistory.lastFundingTime(existingJson)
        val body = if (lastFunding == null) {
            fetch(PumpBotEngine.fundingUrl())
        } else {
            fetch(PumpBotEngine.fundingUrl(PumpBotEngine.pumpSymbol, lastFunding + 1L, now))
        }
        return IncrementalMarketHistory.mergeFunding(existingJson, body, fundingLimit, now)
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
        const val recentDerivativeBars = 1000
        const val initialPaddingBars = 20
        const val fundingLimit = 1000
        const val candleMillis = 30L * 60L * 1000L
    }
}

internal object IncrementalMarketHistory {
    private const val candleMillis = 30L * 60L * 1000L

    fun lastClosedKlineTime(json: String, now: Long): Long? {
        return parseArray(json).mapNotNull { row ->
            row.optLong(6, 0L).takeIf { it > 0L && it < now }
        }.maxOrNull()
    }

    fun needsKlineRefresh(lastClosedTime: Long?, now: Long): Boolean {
        return lastClosedTime == null || now > lastClosedTime + candleMillis
    }

    fun mergeKlines(existingJson: String, freshJson: String, limit: Int, now: Long): String {
        val rowsByClose = TreeMap<Long, JSONArray>()
        (parseArray(existingJson) + parseArray(freshJson)).forEach { row ->
            val closeTime = row.optLong(6, 0L)
            if (closeTime > 0L && closeTime < now) rowsByClose[closeTime] = row
        }
        val selected = rowsByClose.values.toList().takeLast(limit.coerceAtLeast(1))
        return JSONArray().apply { selected.forEach { put(it) } }.toString()
    }

    fun lastFundingTime(json: String): Long? {
        return parseObjects(json).mapNotNull { point ->
            point.optLong("fundingTime", 0L).takeIf { it > 0L }
        }.maxOrNull()
    }

    fun mergeFunding(existingJson: String, freshJson: String, limit: Int, now: Long): String {
        val pointsByTime = TreeMap<Long, JSONObject>()
        (parseObjects(existingJson) + parseObjects(freshJson)).forEach { point ->
            val time = point.optLong("fundingTime", 0L)
            if (time > 0L && time <= now) pointsByTime[time] = point
        }
        val selected = pointsByTime.values.toList().takeLast(limit.coerceAtLeast(1))
        return JSONArray().apply { selected.forEach { put(it) } }.toString()
    }

    private fun parseArray(json: String): List<JSONArray> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val root = JSONArray(json)
            (0 until root.length()).mapNotNull { root.optJSONArray(it) }
        }.getOrDefault(emptyList())
    }

    private fun parseObjects(json: String): List<JSONObject> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val root = JSONArray(json)
            (0 until root.length()).mapNotNull { root.optJSONObject(it) }
        }.getOrDefault(emptyList())
    }
}
