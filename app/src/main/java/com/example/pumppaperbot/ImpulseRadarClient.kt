package com.example.pumppaperbot

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ImpulseRadarClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun sync(context: Context) {
        val now = System.currentTimeMillis()
        if (!ImpulseRadarStore.shouldSync(context, now)) return
        ImpulseRadarStore.markAttempt(context, now)
        val saved = ImpulseRadarStore.payloads(context)
        val pool = Executors.newFixedThreadPool(5)
        try {
            val pump = pool.submit<String> {
                updateKlines(saved.pumpJson, false, PumpBotEngine.pumpSymbol, now)
            }
            val btc = pool.submit<String> {
                updateKlines(saved.btcJson, false, PumpBotEngine.btcSymbol, now)
            }
            val sol = pool.submit<String> {
                updateKlines(saved.solJson, false, PumpBotEngine.solSymbol, now)
            }
            val futures = pool.submit<String> {
                updateKlines(saved.futuresJson, true, PumpBotEngine.pumpSymbol, now)
            }
            val openInterest = pool.submit<String> {
                fetch(PumpBotEngine.openInterestHistoryUrl())
            }
            val payloads = ImpulseRadarPayloads(
                pumpJson = pump.get(),
                btcJson = btc.get(),
                solJson = sol.get(),
                futuresJson = futures.get(),
                openInterestJson = openInterest.get()
            )
            val snapshot = ImpulseRadarAnalyzer.analyze(
                pump = PumpBotEngine.parseCandles(payloads.pumpJson),
                btc = PumpBotEngine.parseCandles(payloads.btcJson),
                sol = PumpBotEngine.parseCandles(payloads.solJson),
                futures = PumpBotEngine.parseCandles(payloads.futuresJson),
                openInterestJson = payloads.openInterestJson
            )
            ImpulseRadarStore.save(context, payloads, snapshot)
        } catch (error: Exception) {
            ImpulseRadarStore.saveFailure(context, error.message ?: "5m shadow sync failed")
        } finally {
            pool.shutdownNow()
        }
    }

    private fun updateKlines(
        existingJson: String,
        futures: Boolean,
        symbol: String,
        now: Long
    ): String {
        val lastClosed = IncrementalMarketHistory.lastClosedKlineTime(existingJson, now)
        if (!IncrementalMarketHistory.needsKlineRefresh(lastClosed, now, fiveMinutesMillis)) {
            return existingJson
        }
        val fresh = if (lastClosed == null) {
            fetch(
                if (futures) {
                    PumpBotEngine.futuresKlineUrl(symbol, "5m", historyBars)
                } else {
                    PumpBotEngine.klineUrl(symbol, "5m", historyBars)
                }
            )
        } else {
            fetch(
                if (futures) {
                    PumpBotEngine.historicalFuturesKlineUrl(symbol, "5m", lastClosed + 1L, now)
                } else {
                    PumpBotEngine.historicalKlineUrl(symbol, "5m", lastClosed + 1L, now)
                }
            )
        }
        return IncrementalMarketHistory.mergeKlines(existingJson, fresh, historyBars, now)
    }

    private fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "PumpSignalAndroid/${PumpBotEngine.appVersionName}")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("5m HTTP ${response.code}")
            val text = response.body?.string().orEmpty()
            JSONArray(text)
            return text
        }
    }

    private companion object {
        const val historyBars = 1000
        const val fiveMinutesMillis = 5L * 60L * 1000L
    }
}
