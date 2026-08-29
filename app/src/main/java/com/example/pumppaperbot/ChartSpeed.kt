package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import kotlin.math.max
import kotlin.math.min

enum class ChartInterval(
    val code: String,
    val buttonLabel: String,
    val subtitleLabel: String,
    val durationMillis: Long,
    val refreshMillis: Long
) {
    ONE_MINUTE("1m", "1 МИНУТА", "1 минута", 60_000L, 30_000L),
    FIVE_MINUTES("5m", "5 МИНУТ", "5 минут", 5L * 60_000L, 60_000L),
    FIFTEEN_MINUTES("15m", "15 МИНУТ", "15 минут", 15L * 60_000L, 60_000L),
    THIRTY_MINUTES("30m", "30 МИНУТ", "30 минут", 30L * 60_000L, 120_000L),
    ONE_HOUR("1h", "1 ЧАС", "1 час", 60L * 60_000L, 120_000L);

    companion object {
        fun fromCode(code: String?): ChartInterval = entries.firstOrNull { it.code == code }
            ?: THIRTY_MINUTES
    }
}

internal object ChartSpeedPolicy {
    fun shouldAutoSelectFast(entryTime: Long, handledEntryTime: Long): Boolean =
        entryTime > 0L && entryTime != handledEntryTime
}

object ChartSpeedStore {
    private const val PREFS = "chart_speed_v410"
    private const val KEY_INTERVAL = "selected_interval"
    private const val KEY_HANDLED_ENTRY = "handled_position_entry"
    private const val KEY_UPDATED_PREFIX = "updated_"
    private const val KEY_PUMP_PREFIX = "pump_"
    private const val KEY_EUR_PREFIX = "eur_"
    private const val KEY_ERROR_PREFIX = "error_"

    fun selected(context: Context): ChartInterval = ChartInterval.fromCode(
        prefs(context).getString(KEY_INTERVAL, ChartInterval.THIRTY_MINUTES.code)
    )

    fun select(context: Context, interval: ChartInterval) {
        prefs(context).edit().putString(KEY_INTERVAL, interval.code).apply()
    }

    /**
     * Switches once for each newly opened Serge position. After that one switch the user may
     * manually choose any interval without the UI forcing one minute again on every refresh.
     */
    fun selectFastForNewPosition(context: Context, entryTime: Long): Boolean {
        val p = prefs(context)
        if (!ChartSpeedPolicy.shouldAutoSelectFast(entryTime, p.getLong(KEY_HANDLED_ENTRY, 0L))) {
            return false
        }
        p.edit()
            .putString(KEY_INTERVAL, ChartInterval.ONE_MINUTE.code)
            .putLong(KEY_HANDLED_ENTRY, entryTime)
            .apply()
        return true
    }

    fun save(
        context: Context,
        interval: ChartInterval,
        pumpJson: String,
        eurJson: String,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        prefs(context).edit()
            .putString(KEY_PUMP_PREFIX + interval.code, pumpJson)
            .putString(KEY_EUR_PREFIX + interval.code, eurJson)
            .putLong(KEY_UPDATED_PREFIX + interval.code, updatedAt)
            .remove(KEY_ERROR_PREFIX + interval.code)
            .apply()
    }

    fun recordError(context: Context, interval: ChartInterval, message: String) {
        prefs(context).edit()
            .putString(KEY_ERROR_PREFIX + interval.code, message.take(180))
            .apply()
    }

    fun updatedAt(context: Context, interval: ChartInterval): Long =
        prefs(context).getLong(KEY_UPDATED_PREFIX + interval.code, 0L)

    fun error(context: Context, interval: ChartInterval): String =
        prefs(context).getString(KEY_ERROR_PREFIX + interval.code, "").orEmpty()

    fun candles(context: Context, interval: ChartInterval): List<PumpCandle> {
        val p = prefs(context)
        val pump = parseChartCandles(p.getString(KEY_PUMP_PREFIX + interval.code, "").orEmpty())
        val eur = parseChartCandles(p.getString(KEY_EUR_PREFIX + interval.code, "").orEmpty())
        return StrategyV2.synthesizeEur(pump, eur)
    }

    /**
     * Raw Binance PUMP/USDT candles used by the protected X/T32 replay lineage.
     * Trading execution remains PUMP/EUR on Bitpanda; keeping the signal market raw avoids
     * silently changing the historical evaluator through EUR conversion.
     */
    fun pumpUsdtCandles(context: Context, interval: ChartInterval): List<PumpCandle> {
        val raw = prefs(context).getString(KEY_PUMP_PREFIX + interval.code, "").orEmpty()
        return parseChartCandles(raw)
    }

    fun chartBundle(
        context: Context,
        snapshot: LiveSnapshot,
        now: Long = System.currentTimeMillis()
    ): ChartBundle {
        val interval = selected(context)
        val base = snapshot.chart
        val livePrice = freshestVisiblePrice(context, snapshot, now)
        val sourceCandles = if (interval == ChartInterval.THIRTY_MINUTES) {
            base.candles
        } else {
            val p = prefs(context)
            val pump = parseChartCandles(p.getString(KEY_PUMP_PREFIX + interval.code, "").orEmpty())
            val eur = parseChartCandles(p.getString(KEY_EUR_PREFIX + interval.code, "").orEmpty())
            StrategyV2.synthesizeEur(pump, eur)
        }
        if (sourceCandles.isEmpty()) {
            val fallback = ChartSpeedPresentation.withLiveEdge(
                base.candles,
                ChartInterval.THIRTY_MINUTES,
                livePrice,
                now
            )
            return base.copy(
                candles = fallback,
                fast = chartEma(fallback.map { it.close }, PumpBotEngine.emaFastPeriod),
                slow = chartEma(fallback.map { it.close }, PumpBotEngine.emaSlowPeriod),
                subtitle = "${interval.subtitleLabel}: загружается • временно показаны 30 минут • стратегия APP остаётся 30 минут"
            )
        }
        val visible = ChartSpeedPresentation.withLiveEdge(sourceCandles, interval, livePrice, now)
        val age = updatedAt(context, interval).takeIf { it > 0L }?.let {
            ((now - it).coerceAtLeast(0L) / 1000L)
        }
        val freshness = if (interval == ChartInterval.ONE_MINUTE) {
            "живой край ≈15 сек"
        } else {
            age?.let { "данные ${it}с назад" } ?: "живой край"
        }
        return base.copy(
            candles = visible,
            fast = chartEma(visible.map { it.close }, PumpBotEngine.emaFastPeriod),
            slow = chartEma(visible.map { it.close }, PumpBotEngine.emaSlowPeriod),
            subtitle = "${interval.subtitleLabel} • $freshness • EMA50/EMA200 • стратегия APP остаётся 30 минут"
        )
    }

    private fun freshestVisiblePrice(context: Context, snapshot: LiveSnapshot, now: Long): Double? {
        val micro = MicroImpulseStore.state(context)
        if (micro.connected && micro.priceUsdt > 0.0 && micro.updatedAt in 1L..now &&
            now - micro.updatedAt <= 45_000L
        ) {
            val eur = PumpBotEngine.parseCandles(PumpBotEngine.savedMarketPayloads(context).eurJson)
                .lastOrNull()?.close
            if (eur != null && eur > 0.0) return micro.priceUsdt / eur
        }
        return PaperExecutionPolicy.displayPrice(snapshot, now).takeIf { it > 0.0 }
    }

    internal fun parseChartCandles(json: String): List<PumpCandle> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val rows = JSONArray(json)
            (0 until rows.length()).mapNotNull { index ->
                val row = rows.optJSONArray(index) ?: return@mapNotNull null
                val openTime = row.optLong(0, 0L)
                val closeTime = row.optLong(6, 0L)
                val open = row.optString(1).toDoubleOrNull() ?: return@mapNotNull null
                val high = row.optString(2).toDoubleOrNull() ?: return@mapNotNull null
                val low = row.optString(3).toDoubleOrNull() ?: return@mapNotNull null
                val close = row.optString(4).toDoubleOrNull() ?: return@mapNotNull null
                if (openTime <= 0L || closeTime <= openTime || min(open, min(high, min(low, close))) <= 0.0) {
                    return@mapNotNull null
                }
                PumpCandle(
                    openTime = openTime,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = row.optString(5).toDoubleOrNull() ?: 0.0,
                    closeTime = closeTime,
                    quoteVolume = row.optString(7).toDoubleOrNull() ?: 0.0,
                    tradeCount = row.optInt(8, 0),
                    takerBuyVolume = row.optString(9).toDoubleOrNull() ?: 0.0,
                    takerBuyQuoteVolume = row.optString(10).toDoubleOrNull() ?: 0.0
                )
            }.distinctBy { it.openTime }.sortedBy { it.openTime }
        }.getOrDefault(emptyList())
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

internal object ChartSpeedPresentation {
    fun withLiveEdge(
        candles: List<PumpCandle>,
        interval: ChartInterval,
        livePrice: Double?,
        now: Long
    ): List<PumpCandle> {
        if (candles.isEmpty() || livePrice == null || !livePrice.isFinite() || livePrice <= 0.0) {
            return candles
        }
        val currentOpen = now / interval.durationMillis * interval.durationMillis
        val currentClose = currentOpen + interval.durationMillis - 1L
        val result = candles.sortedBy { it.openTime }.toMutableList()
        val last = result.last()
        when {
            last.openTime == currentOpen -> result[result.lastIndex] = last.copy(
                high = max(last.high, livePrice),
                low = min(last.low, livePrice),
                close = livePrice,
                closeTime = currentClose
            )
            last.openTime < currentOpen -> result += PumpCandle(
                openTime = currentOpen,
                open = last.close,
                high = max(last.close, livePrice),
                low = min(last.close, livePrice),
                close = livePrice,
                volume = 0.0,
                closeTime = currentClose
            )
        }
        return result.takeLast(360)
    }
}

internal fun chartEma(values: List<Double>, period: Int): List<Double?> {
    if (values.isEmpty() || period <= 0) return List(values.size) { null }
    val result = MutableList<Double?>(values.size) { null }
    if (values.size < period) return result
    var sum = 0.0
    for (index in 0 until period) sum += values[index]
    var previous = sum / period
    result[period - 1] = previous
    val multiplier = 2.0 / (period + 1.0)
    for (index in period until values.size) {
        previous = (values[index] - previous) * multiplier + previous
        result[index] = previous
    }
    return result
}
