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
        val updatedAt: Long
    ) {
        fun fresh(now: Long = System.currentTimeMillis()) =
            updatedAt in 1L..now && now - updatedAt <= 90_000L
    }

    fun load(context: Context, interval: String): Quote {
        require(interval in setOf("5m", "15m", "30m", "1h"))
        val key = BitpandaFusionSecureKeyStore.read(context)
        require(key.isNotBlank()) { "Не введён Bitpanda Fusion API-ключ Read" }

        // The ticker's 'price' is the reference mid price, not a guaranteed fill.
        val tickers = JSONArray(get("v1/tickers?pair=PUMP-EUR%2CBTC-EUR", key))
        fun ticker(pair: String): Double {
            for (i in 0 until tickers.length()) {
                val t = tickers.optJSONObject(i) ?: continue
                if (t.optString("pair") == pair) return positive(t, "price")
            }
            error("Bitpanda не вернула $pair")
        }
        val pumpMid = ticker("PUMP-EUR")
        val btcMid = ticker("BTC-EUR")
        val book = JSONObject(get("v1/orderbook/PUMP-EUR?depth=1", key))
        fun firstPrice(side: String): Double {
            val first = book.optJSONArray(side)?.optJSONObject(0)
                ?: error("Нет стороны $side в стакане Bitpanda")
            return positive(first, "price")
        }
        val bid = firstPrice("bids")
        val ask = firstPrice("asks")
        require(ask >= bid) { "Некорректные котировки Bitpanda" }
        val pumpCandles = parseCandles(get("v1/candles/PUMP-EUR?interval=$interval&limit=60", key))
        val btcCandles = parseCandles(get("v1/candles/BTC-EUR?interval=$interval&limit=60", key))
        require(pumpCandles.size >= 2 && btcCandles.size >= 2) { "Bitpanda вернула недостаточно свечей" }
        return Quote(pumpMid, bid, ask, btcMid, pumpCandles, btcCandles, System.currentTimeMillis())
    }

    private fun get(path: String, key: String): String {
        val url = "https://api.fusion.bitpanda.com/$path"
        val request = Request.Builder()
            .url(url)
            .header("x-api-key", key)
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Ошибка Bitpanda HTTP ${response.code}")
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
