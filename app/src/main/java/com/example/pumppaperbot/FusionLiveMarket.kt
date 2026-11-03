package com.example.pumppaperbot

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Isolated read-only Bitpanda Fusion market feed for the compact dashboard.
 * NEVER logs, persists, exports or shares the user's API key.
 * Does not change the existing paper-research/AI trading algorithms.
 */
internal class FusionLiveMarket {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class Candle(
        val timestamp: Long,
        val open: Double,
        val high: Double,
        val low: Double,
        val close: Double
    )

    data class Quote(
        val pumpMid: Double,
        val pumpBid: Double,
        val pumpAsk: Double,
        val btcMid: Double,
        val pump: List<Candle>,
        val btc: List<Candle>,
        val updatedAt: Long,
        val candlesUpdatedAt: Long = updatedAt,
        val pumpHigh24h: Double? = null,
        val pumpLow24h: Double? = null
    ) {
        fun fresh(now: Long = System.currentTimeMillis()) =
            updatedAt in 1L..now && now - updatedAt <= 90_000L
    }

    fun load(context: Context, interval: String, previous: Quote? = null): Quote {
        require(interval in setOf("5m", "15m", "30m", "1h"))
        val key = BitpandaFusionSecureKeyStore.read(context)
        require(key.isNotBlank()) { "Не введён Bitpanda Fusion API-ключ Read" }
        val delay = FusionRefreshState.retryDelaySeconds(context)
        require(delay <= 0L) { "Bitpanda: пауза после ошибки API, ещё ${delay} с" }
        val now = System.currentTimeMillis()

        // The ticker's 'price' is the reference mid price, not a guaranteed fill.
        val tickers = JSONArray(get(context, "v1/tickers?pair=PUMP-EUR%2CBTC-EUR", key))
        fun ticker(pair: String): Double {
            for (i in 0 until tickers.length()) {
                val t = tickers.optJSONObject(i) ?: continue
                if (t.optString("pair") == pair) return positive(t, "price")
            }
            error("Bitpanda не вернула $pair")
        }
        val pumpMid = ticker("PUMP-EUR")
        val btcMid = ticker("BTC-EUR")
        val pumpTicker = (0 until tickers.length()).mapNotNull { tickers.optJSONObject(it) }
            .firstOrNull { it.optString("pair") == "PUMP-EUR" }
        // Official Fusion 24h ticker high/low. Optional fields never break live charts.
        fun optionalPositive(field: String): Double? =
            pumpTicker?.optString(field)?.toDoubleOrNull()
                ?.takeIf { it.isFinite() && it > 0.0 }
        val high24h = optionalPositive("high")
        val low24h = optionalPositive("low")
        val book = JSONObject(get(context, "v1/orderbook/PUMP-EUR?depth=1", key))
        fun firstPrice(side: String): Double {
            val first = book.optJSONArray(side)?.optJSONObject(0)
                ?: error("Нет стороны $side в стакане Bitpanda")
            return positive(first, "price")
        }
        val bid = firstPrice("bids")
        val ask = firstPrice("asks")
        require(ask >= bid) { "Некорректные котировки Bitpanda" }
        // Candles change on bar boundaries, not on every ticker refresh.
        // Reuse cached OHLCV for up to one minute while refreshing bid/ask every 15 sec.
        val cached = previous ?: FusionQuoteCache.read(context, interval)
        val reuseCandles = cached != null &&
            cached.candlesUpdatedAt in 1L..now &&
            now - cached.candlesUpdatedAt < 60_000L
        val pumpCandles = if (reuseCandles) cached!!.pump else
            parseCandles(get(context, "v1/candles/PUMP-EUR?interval=$interval&limit=60", key))
        val btcCandles = if (reuseCandles) cached!!.btc else
            parseCandles(get(context, "v1/candles/BTC-EUR?interval=$interval&limit=60", key))
        require(pumpCandles.size >= 2 && btcCandles.size >= 2) { "Bitpanda вернула недостаточно свечей" }
        val result = Quote(
            pumpMid = pumpMid,
            pumpBid = bid,
            pumpAsk = ask,
            btcMid = btcMid,
            pump = pumpCandles,
            btc = btcCandles,
            updatedAt = System.currentTimeMillis(),
            candlesUpdatedAt = if (reuseCandles) cached!!.candlesUpdatedAt else System.currentTimeMillis(),
            pumpHigh24h = high24h,
            pumpLow24h = low24h
        )
        FusionQuoteCache.save(context, interval, result)
        FusionRefreshState.success(context)
        return result
    }

    private fun get(context: Context, path: String, key: String): String {
        val url = "https://api.fusion.bitpanda.com/$path"
        val request = Request.Builder()
            .url(url)
            .header("x-api-key", key)
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val code = response.code
                val retryAfter = response.header("Retry-After")?.toLongOrNull()
                if (code == 429 || code == 401 || code == 403 || code >= 500)
                    FusionRefreshState.blocked(context, code, retryAfter)
                error("Ошибка Bitpanda HTTP ${code}")
            }
            return response.body?.string() ?: error("Пустой ответ Bitpanda")
        }
    }

    private fun parseCandles(raw: String): List<Candle> {
        val array = JSONArray(raw)
        val seen = linkedMapOf<Long, Candle>()
        for (i in 0 until array.length()) {
            val row = array.optJSONObject(i) ?: continue
            val seconds = row.optLong("timestamp")
            if (seconds <= 0L) continue
            val candle = Candle(
                timestamp = seconds * 1000L,
                open = positive(row, "open"),
                high = positive(row, "high"),
                low = positive(row, "low"),
                close = positive(row, "close")
            )
            if (candle.high < candle.low) continue
            seen[candle.timestamp] = candle
        }
        return seen.values.sortedBy { it.timestamp }
    }

    private fun positive(obj: JSONObject, field: String): Double {
        val number = obj.optString(field).toDoubleOrNull()
            ?: error("Отсутствует $field в ответе Bitpanda")
        require(number.isFinite() && number > 0.0) { "Недопустимое $field в ответе Bitpanda" }
        return number
    }
}
