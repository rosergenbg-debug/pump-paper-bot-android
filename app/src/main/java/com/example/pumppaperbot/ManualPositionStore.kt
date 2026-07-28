package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ManualTrade(
    val boughtAt: Long,
    val buyPrice: Double,
    val soldAt: Long = 0L,
    val sellPrice: Double = 0.0
) {
    val closed: Boolean get() = soldAt > 0L && sellPrice > 0.0
    val profitPercent: Double
        get() = if (closed && buyPrice > 0.0) (sellPrice / buyPrice - 1.0) * 100.0 else 0.0

    fun toJson(): JSONObject = JSONObject()
        .put("boughtAt", boughtAt)
        .put("buyPrice", buyPrice)
        .put("soldAt", soldAt)
        .put("sellPrice", sellPrice)

    companion object {
        fun fromJson(value: JSONObject) = ManualTrade(
            boughtAt = value.optLong("boughtAt"),
            buyPrice = value.optDouble("buyPrice"),
            soldAt = value.optLong("soldAt"),
            sellPrice = value.optDouble("sellPrice")
        )
    }
}

/** Small personal trade journal. No amounts or account data are stored. */
object ManualPositionStore {
    private const val PREFS = "manual_position_v315"
    private const val KEY_TRADES = "trades"
    internal const val RETENTION_MILLIS = 183L * 24L * 60L * 60L * 1000L
    private const val MAX_TRADES = 5_000

    fun recordBuy(
        context: Context,
        price: Double,
        at: Long = System.currentTimeMillis()
    ) {
        if (price <= 0.0 || !price.isFinite()) return
        val trades = retained(read(context), at).toMutableList()
        if (trades.lastOrNull()?.closed == false) return
        trades += ManualTrade(at, price)
        write(context, trades)
    }

    fun recordSell(
        context: Context,
        price: Double,
        at: Long = System.currentTimeMillis()
    ) {
        if (price <= 0.0 || !price.isFinite()) return
        val trades = retained(read(context), at).toMutableList()
        val index = trades.indexOfLast { !it.closed }
        if (index < 0) return
        trades[index] = trades[index].copy(soldAt = at, sellPrice = price)
        write(context, trades)
    }

    fun ensureOpenPosition(context: Context, price: Double, at: Long) {
        if (openTrade(context) != null || price <= 0.0 || !price.isFinite()) return
        recordBuy(context, price, at.takeIf { it > 0L } ?: System.currentTimeMillis())
    }

    fun discardOpenPosition(context: Context) {
        val trades = read(context)
        val retained = trades.filter { it.closed }
        if (retained != trades) write(context, retained)
    }

    fun trades(context: Context, now: Long = System.currentTimeMillis()): List<ManualTrade> {
        val all = read(context)
        val current = retained(all, now)
        if (current != all) write(context, current)
        return current
    }

    fun openTrade(context: Context): ManualTrade? = trades(context).lastOrNull { !it.closed }

    internal fun retained(values: List<ManualTrade>, now: Long): List<ManualTrade> {
        val cutoff = now - RETENTION_MILLIS
        return values.filter { !it.closed || it.soldAt >= cutoff }.takeLast(MAX_TRADES)
    }

    private fun read(context: Context): List<ManualTrade> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TRADES, "[]").orEmpty()
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let(ManualTrade::fromJson)
        }.filter { it.boughtAt > 0L && it.buyPrice > 0.0 }
    }.getOrDefault(emptyList())

    private fun write(context: Context, values: List<ManualTrade>) {
        val array = JSONArray()
        values.forEach { array.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TRADES, array.toString())
            .apply()
    }
}
