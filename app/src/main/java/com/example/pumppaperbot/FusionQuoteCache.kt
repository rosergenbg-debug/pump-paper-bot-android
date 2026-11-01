package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Non-sensitive Bitpanda market data only; never persists the API key.
 * Cache is shared by the visible screen and the already-running monitor service.
 */
internal object FusionQuoteCache {
    private const val PREFS = "fusion_quote_cache_v1"
    private const val KEY_JSON = "quote"

    fun save(context: Context, interval: String, quote: FusionLiveMarket.Quote) {
        val obj = JSONObject()
            .put("interval", interval)
            .put("updatedAt", quote.updatedAt)
            .put("candlesUpdatedAt", quote.candlesUpdatedAt)
            .put("pumpMid", quote.pumpMid).put("pumpBid", quote.pumpBid)
            .put("pumpAsk", quote.pumpAsk).put("btcMid", quote.btcMid)
            .put("pump", candles(quote.pump)).put("btc", candles(quote.btc))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_JSON, obj.toString()).apply()
    }

    fun read(context: Context, interval: String): FusionLiveMarket.Quote? = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_JSON, null) ?: return@runCatching null
        val obj = JSONObject(raw)
        if (obj.optString("interval") != interval) return@runCatching null
        val updatedAt = obj.optLong("updatedAt")
        val candleAt = obj.optLong("candlesUpdatedAt")
        val now = System.currentTimeMillis()
        if (updatedAt !in 1L..now || now - updatedAt > 300_000L) return@runCatching null
        val pump = readCandles(obj.getJSONArray("pump"))
        val btc = readCandles(obj.getJSONArray("btc"))
        if (pump.size < 2 || btc.size < 2) return@runCatching null
        FusionLiveMarket.Quote(
            pumpMid = obj.getDouble("pumpMid"),
            pumpBid = obj.getDouble("pumpBid"),
            pumpAsk = obj.getDouble("pumpAsk"),
            btcMid = obj.getDouble("btcMid"),
            pump = pump, btc = btc,
            updatedAt = updatedAt, candlesUpdatedAt = candleAt
        )
    }.getOrNull()

    private fun candles(items: List<FusionLiveMarket.Candle>) = JSONArray().apply {
        items.forEach { add ->
            put(JSONArray().put(add.timestamp).put(add.open)
                .put(add.high).put(add.low).put(add.close))
        }
    }

    private fun readCandles(array: JSONArray): List<FusionLiveMarket.Candle> =
        (0 until array.length()).mapNotNull { i ->
            val row = array.optJSONArray(i) ?: return@mapNotNull null
            if (row.length() != 5) return@mapNotNull null
            FusionLiveMarket.Candle(
                row.getLong(0), row.getDouble(1), row.getDouble(2),
                row.getDouble(3), row.getDouble(4)
            ).takeIf { it.open > 0.0 && it.close > 0.0 && it.low > 0.0 &&
                it.high >= it.low }
        }
}
