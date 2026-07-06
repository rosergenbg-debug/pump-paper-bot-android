package com.example.pumppaperbot

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PumpCandle(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long
)

data class PumpBotState(
    val running: Boolean,
    val cash: Double,
    val coins: Double,
    val entryPrice: Double,
    val highestClose: Double,
    val lastCandleTime: Long,
    val buys: Int,
    val sells: Int,
    val lastAction: String,
    val lastReason: String,
    val lastPrice: Double,
    val lastUpdated: Long,
    val tradeLog: String
)

data class PumpBotResult(
    val state: PumpBotState,
    val equity: Double,
    val profit: Double,
    val profitPercent: Double
)

object PumpBotEngine {
    const val uniqueWorkName = "pump_paper_bot_periodic_monitor"
    const val symbol = "PUMPUSDT"
    const val interval = "4h"
    const val startBalance = 1000.0
    const val klineUrl = "https://data-api.binance.vision/api/v3/klines?symbol=$symbol&interval=$interval&limit=500"

    private const val prefsName = "PumpPaperBot"
    private const val fastPeriod = 25
    private const val slowPeriod = 99
    private const val adxThreshold = 20.0
    private const val minAtrPercent = 1.2
    private const val volumeFactor = 1.0
    private const val stopLoss = 0.08
    private const val trailingStop = 0.10
    private const val feeRate = 0.001
    private const val slippage = 0.0005

    private const val keyRunning = "running"
    private const val keyCash = "cash"
    private const val keyCoins = "coins"
    private const val keyEntry = "entry"
    private const val keyHighest = "highest"
    private const val keyLastCandle = "last_candle"
    private const val keyBuys = "buys"
    private const val keySells = "sells"
    private const val keyAction = "action"
    private const val keyReason = "reason"
    private const val keyLastPrice = "last_price"
    private const val keyUpdated = "updated"
    private const val keyTradeLog = "trade_log"

    fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    fun ensureInitialized(context: Context) {
        if (!prefs(context).contains(keyCash)) reset(context)
    }

    fun reset(context: Context) {
        prefs(context).edit()
            .clear()
            .putBoolean(keyRunning, false)
            .putDouble(keyCash, startBalance)
            .putDouble(keyCoins, 0.0)
            .putDouble(keyEntry, 0.0)
            .putDouble(keyHighest, 0.0)
            .putLong(keyLastCandle, 0L)
            .putInt(keyBuys, 0)
            .putInt(keySells, 0)
            .putString(keyAction, "RESET")
            .putString(keyReason, "Virtual account reset to 1000 USDT")
            .putDouble(keyLastPrice, 0.0)
            .putLong(keyUpdated, System.currentTimeMillis())
            .putString(keyTradeLog, "")
            .apply()
    }

    fun setRunning(context: Context, running: Boolean) {
        ensureInitialized(context)
        prefs(context).edit()
            .putBoolean(keyRunning, running)
            .putLong(keyUpdated, System.currentTimeMillis())
            .putString(keyAction, if (running) "START" else "STOP")
            .putString(keyReason, if (running) "Monitoring enabled" else "Monitoring stopped")
            .apply()
    }

    fun load(context: Context): PumpBotState {
        ensureInitialized(context)
        val p = prefs(context)
        return PumpBotState(
            running = p.getBoolean(keyRunning, false),
            cash = p.getDouble(keyCash, startBalance),
            coins = p.getDouble(keyCoins, 0.0),
            entryPrice = p.getDouble(keyEntry, 0.0),
            highestClose = p.getDouble(keyHighest, 0.0),
            lastCandleTime = p.getLong(keyLastCandle, 0L),
            buys = p.getInt(keyBuys, 0),
            sells = p.getInt(keySells, 0),
            lastAction = p.getString(keyAction, "WAIT").orEmpty(),
            lastReason = p.getString(keyReason, "No data yet").orEmpty(),
            lastPrice = p.getDouble(keyLastPrice, 0.0),
            lastUpdated = p.getLong(keyUpdated, 0L),
            tradeLog = p.getString(keyTradeLog, "").orEmpty()
        )
    }

    fun save(context: Context, state: PumpBotState) {
        prefs(context).edit()
            .putBoolean(keyRunning, state.running)
            .putDouble(keyCash, state.cash)
            .putDouble(keyCoins, state.coins)
            .putDouble(keyEntry, state.entryPrice)
            .putDouble(keyHighest, state.highestClose)
            .putLong(keyLastCandle, state.lastCandleTime)
            .putInt(keyBuys, state.buys)
            .putInt(keySells, state.sells)
            .putString(keyAction, state.lastAction)
            .putString(keyReason, state.lastReason)
            .putDouble(keyLastPrice, state.lastPrice)
            .putLong(keyUpdated, state.lastUpdated)
            .putString(keyTradeLog, state.tradeLog)
            .apply()
    }

    fun parseCandles(json: String): List<PumpCandle> {
        val rows = JSONArray(json)
        val now = System.currentTimeMillis()
        val candles = ArrayList<PumpCandle>()

        for (i in 0 until rows.length()) {
            val row = rows.getJSONArray(i)
            val closeTime = row.getLong(6)
            if (closeTime < now) {
                candles.add(
                    PumpCandle(
                        open = row.getString(1).toDouble(),
                        high = row.getString(2).toDouble(),
                        low = row.getString(3).toDouble(),
                        close = row.getString(4).toDouble(),
                        volume = row.getString(5).toDouble(),
                        closeTime = closeTime
                    )
                )
            }
        }

        return candles
    }

    fun evaluate(previous: PumpBotState, candles: List<PumpCandle>): PumpBotResult {
        if (candles.size < slowPeriod + 10) {
            val price = candles.lastOrNull()?.close ?: previous.lastPrice
            return result(previous.copy(lastPrice = price, lastReason = "Not enough candles for WMA99"))
        }

        val last = candles.last()
        if (last.closeTime <= previous.lastCandleTime) {
            return result(
                previous.copy(
                    lastPrice = last.close,
                    lastUpdated = System.currentTimeMillis(),
                    lastReason = "No new closed 4H candle"
                )
            )
        }

        val closes = candles.map { it.close }
        val volumes = candles.map { it.volume }
        val fast = wma(closes, fastPeriod)
        val slow = wma(closes, slowPeriod)
        val adx = adx(candles, 14)
        val atr = atr(candles, 14)
        val volumeAverage = sma(volumes, 20)
        val i = candles.lastIndex

        val slowNow = value(slow, i)
        val fastNow = value(fast, i)
        val slowPrev = value(slow, i - 1)
        val fastPrev = value(fast, i - 1)
        val slowSlope = slowNow - value(slow, i - 3)
        val atrPercent = if (value(atr, i) > 0.0) value(atr, i) / last.close * 100.0 else 0.0
        val volumeOk = value(volumeAverage, i) > 0.0 && last.volume >= value(volumeAverage, i) * volumeFactor
        val trendOk = value(adx, i) >= adxThreshold &&
            slowSlope > 0.0 &&
            last.close > slowNow &&
            volumeOk &&
            atrPercent >= minAtrPercent
        val crossedUp = fastPrev <= slowPrev && fastNow > slowNow
        val crossedDown = fastPrev >= slowPrev && fastNow < slowNow
        val filterText = String.format(
            Locale.US,
            "ADX %.1f, ATR %.2f%%, volume %s, WMA slope %s",
            value(adx, i),
            atrPercent,
            if (volumeOk) "ok" else "weak",
            if (slowSlope > 0.0) "up" else "flat/down"
        )

        var next = previous.copy(
            lastCandleTime = last.closeTime,
            lastPrice = last.close,
            lastUpdated = System.currentTimeMillis()
        )

        if (previous.coins <= 0.0 && crossedUp && trendOk) {
            val buyPrice = last.close * (1 + slippage)
            val coins = previous.cash * (1 - feeRate) / buyPrice
            next = next.copy(
                cash = 0.0,
                coins = coins,
                entryPrice = buyPrice,
                highestClose = last.close,
                buys = previous.buys + 1,
                lastAction = "BUY",
                lastReason = "WMA crossed up. $filterText"
            ).withTrade("BUY", buyPrice, coins * buyPrice)
        } else if (previous.coins > 0.0) {
            val highest = maxOf(previous.highestClose, last.close)
            val stopPrice = previous.entryPrice * (1 - stopLoss)
            val trailPrice = highest * (1 - trailingStop)
            val shouldSell = crossedDown || last.close <= stopPrice || last.close <= trailPrice

            if (shouldSell) {
                val sellPrice = last.close * (1 - slippage)
                val cash = previous.coins * sellPrice * (1 - feeRate)
                val reason = when {
                    last.close <= stopPrice -> "Stop-loss"
                    last.close <= trailPrice -> "Trailing stop"
                    else -> "WMA crossed down"
                }
                next = next.copy(
                    cash = cash,
                    coins = 0.0,
                    entryPrice = 0.0,
                    highestClose = 0.0,
                    sells = previous.sells + 1,
                    lastAction = "SELL",
                    lastReason = "$reason. $filterText"
                ).withTrade("SELL", sellPrice, cash)
            } else {
                next = next.copy(
                    highestClose = highest,
                    lastAction = "HOLD",
                    lastReason = "Position remains open. $filterText"
                )
            }
        } else {
            next = next.copy(
                lastAction = "WAIT",
                lastReason = if (crossedUp) "Signal rejected. $filterText" else "No entry signal. $filterText"
            )
        }

        return result(next)
    }

    fun result(state: PumpBotState): PumpBotResult {
        val equity = if (state.coins > 0.0 && state.lastPrice > 0.0) {
            state.coins * state.lastPrice * (1 - feeRate)
        } else {
            state.cash
        }
        val profit = equity - startBalance
        return PumpBotResult(state, equity, profit, profit / startBalance * 100.0)
    }

    private fun PumpBotState.withTrade(action: String, price: Double, equity: Double): PumpBotState {
        val stamp = SimpleDateFormat("dd.MM HH:mm", Locale.GERMAN).format(Date())
        val line = String.format(Locale.US, "%s %s %.8f equity %.2f", stamp, action, price, equity)
        val merged = (line + "\n" + tradeLog).lines().filter { it.isNotBlank() }.take(12).joinToString("\n")
        return copy(tradeLog = merged)
    }

    private fun value(values: List<Double?>, index: Int): Double {
        return values.getOrNull(index) ?: 0.0
    }

    private fun wma(values: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(values.size) { null }
        val divisor = period * (period + 1) / 2.0

        for (i in period - 1 until values.size) {
            var weighted = 0.0
            for (j in 0 until period) weighted += values[i - j] * (period - j)
            result[i] = weighted / divisor
        }

        return result
    }

    private fun sma(values: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(values.size) { null }
        var sum = 0.0

        for (i in values.indices) {
            sum += values[i]
            if (i >= period) sum -= values[i - period]
            if (i >= period - 1) result[i] = sum / period
        }

        return result
    }

    private fun atr(candles: List<PumpCandle>, period: Int): List<Double?> {
        val ranges = candles.mapIndexed { index, candle ->
            if (index == 0) candle.high - candle.low else {
                val previousClose = candles[index - 1].close
                maxOf(candle.high - candle.low, kotlin.math.abs(candle.high - previousClose), kotlin.math.abs(candle.low - previousClose))
            }
        }
        return sma(ranges, period)
    }

    private fun adx(candles: List<PumpCandle>, period: Int): List<Double?> {
        val plusDm = MutableList(candles.size) { 0.0 }
        val minusDm = MutableList(candles.size) { 0.0 }
        val ranges = MutableList(candles.size) { 0.0 }

        for (i in 1 until candles.size) {
            val upMove = candles[i].high - candles[i - 1].high
            val downMove = candles[i - 1].low - candles[i].low
            plusDm[i] = if (upMove > downMove && upMove > 0.0) upMove else 0.0
            minusDm[i] = if (downMove > upMove && downMove > 0.0) downMove else 0.0
            ranges[i] = maxOf(candles[i].high - candles[i].low, kotlin.math.abs(candles[i].high - candles[i - 1].close), kotlin.math.abs(candles[i].low - candles[i - 1].close))
        }

        val tr = sma(ranges, period)
        val plus = sma(plusDm, period)
        val minus = sma(minusDm, period)
        val dx = MutableList(candles.size) { 0.0 }

        for (i in candles.indices) {
            val trValue = tr[i] ?: continue
            if (trValue <= 0.0) continue
            val plusDi = 100.0 * ((plus[i] ?: 0.0) / trValue)
            val minusDi = 100.0 * ((minus[i] ?: 0.0) / trValue)
            val total = plusDi + minusDi
            dx[i] = if (total == 0.0) 0.0 else 100.0 * kotlin.math.abs(plusDi - minusDi) / total
        }

        return sma(dx, period)
    }

    private fun SharedPreferences.Editor.putDouble(key: String, value: Double): SharedPreferences.Editor {
        return putLong(key, java.lang.Double.doubleToRawLongBits(value))
    }

    private fun SharedPreferences.getDouble(key: String, default: Double): Double {
        return if (contains(key)) java.lang.Double.longBitsToDouble(getLong(key, 0L)) else default
    }
}
