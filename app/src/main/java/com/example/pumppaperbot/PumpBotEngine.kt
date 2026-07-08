package com.example.pumppaperbot

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PumpCandle(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long
)

data class IndicatorSeries(
    val fast: List<Double?>,
    val slow: List<Double?>,
    val adx: List<Double?>,
    val atr: List<Double?>,
    val volumeAverage: List<Double?>
)

data class TradeEvent(
    val time: Long,
    val action: String,
    val price: Double,
    val amount: Double,
    val fee: Double,
    val equity: Double,
    val pnl: Double,
    val coins: Double
)

data class StrategyState(
    val id: String,
    val title: String,
    val subtitle: String,
    val cash: Double,
    val coins: Double,
    val entryPrice: Double,
    val positionCost: Double,
    val highestClose: Double,
    val lastProcessedCandle: Long,
    val buys: Int,
    val sells: Int,
    val lastAction: String,
    val lastReason: String,
    val lastPrice: Double,
    val lastUpdated: Long,
    val trades: List<TradeEvent>
)

data class StrategyResult(
    val state: StrategyState,
    val equity: Double,
    val profit: Double,
    val profitPercent: Double,
    val totalFees: Double,
    val tradeCount: Int
)

data class ChartBundle(
    val candles: List<PumpCandle>,
    val fast: List<Double?>,
    val slow: List<Double?>,
    val trades: List<TradeEvent>,
    val subtitle: String
)

data class BotSnapshot(
    val running: Boolean,
    val startedAt: Long,
    val lastSync: Long,
    val primary: StrategyResult,
    val experimental: StrategyResult,
    val primaryChart: ChartBundle,
    val experimentalChart: ChartBundle
)

object PumpBotEngine {
    const val symbol = "PUMPUSDT"
    const val uniqueWorkName = "pump_paper_bot_periodic_monitor"
    const val startBalance = 1000.0
    const val appVersionName = "0.7"

    private const val prefsName = "PumpPaperBotV2"
    private const val algorithmVersion = 7
    private const val keyAlgorithmVersion = "algorithm_version"
    private const val keyRunning = "running"
    private const val keyStartedAt = "started_at"
    private const val keyLastSync = "last_sync"
    private const val keyMarket4h = "market_primary_json"
    private const val keyMarket2h = "market_experiment_json"
    private const val primaryFastPeriod = 50
    private const val primarySlowPeriod = 200
    private const val experimentFastPeriod = 34
    private const val experimentSlowPeriod = 200
    private const val adxPeriod = 14
    private const val primaryRsiPeriod = 14
    private const val primaryBuyRsi = 30.0
    private const val primarySellRsi = 62.0
    private const val experimentRsiPeriod = 14
    private const val experimentSupertrendPeriod = 14
    private const val experimentSupertrendMultiplier = 2.5
    private const val experimentMinRsi = 45.0
    const val feeRate = 0.0015
    private const val slippage = 0.0005
    private const val primaryStopLoss = 0.08
    private const val primaryTrailingStop = 0.04
    private const val experimentStopLoss = 0.04
    private const val experimentTrailingStop = 0.04

    fun klineUrl(interval: String): String {
        return "https://data-api.binance.vision/api/v3/klines?symbol=$symbol&interval=$interval&limit=500"
    }

    fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    fun ensureInitialized(context: Context) {
        val p = prefs(context)
        if (!p.contains("primary_cash") || p.getInt(keyAlgorithmVersion, 0) != algorithmVersion) reset(context)
    }

    fun reset(context: Context) {
        prefs(context).edit()
            .clear()
            .putInt(keyAlgorithmVersion, algorithmVersion)
            .putBoolean(keyRunning, false)
            .putLong(keyStartedAt, 0L)
            .putStrategy(defaultPrimary())
            .putStrategy(defaultExperimental())
            .putLong(keyLastSync, 0L)
            .apply()
    }

    fun setRunning(context: Context, running: Boolean) {
        ensureInitialized(context)
        val p = prefs(context)
        val editor = p.edit()
            .putBoolean(keyRunning, running)
            .putLong(keyLastSync, System.currentTimeMillis())
        if (running && p.getLong(keyStartedAt, 0L) == 0L) {
            editor.putLong(keyStartedAt, System.currentTimeMillis())
        }
        editor.apply()
    }

    fun isRunning(context: Context): Boolean {
        ensureInitialized(context)
        return prefs(context).getBoolean(keyRunning, false)
    }

    fun sync(context: Context, json4h: String, json2h: String, allowTrading: Boolean) {
        ensureInitialized(context)
        val candles4h = parseCandles(json4h)
        val candles2h = parseCandles(json2h)
        val p = prefs(context)

        val primary = p.getStrategy("primary", defaultPrimary())
        val experimental = p.getStrategy("experiment", defaultExperimental())
        val nextPrimary = if (allowTrading) runPrimary(primary, candles4h) else primary.withMarketOnly(candles4h)
        val nextExperimental = if (allowTrading) runExperimental(experimental, candles2h, candles4h) else experimental.withMarketOnly(candles2h)

        p.edit()
            .putString(keyMarket4h, json4h)
            .putString(keyMarket2h, json2h)
            .putLong(keyLastSync, System.currentTimeMillis())
            .putStrategy(nextPrimary)
            .putStrategy(nextExperimental)
            .apply()
    }

    fun snapshot(context: Context): BotSnapshot {
        ensureInitialized(context)
        val p = prefs(context)
        val candles4h = parseSavedCandles(p.getString(keyMarket4h, "").orEmpty())
        val candles2h = parseSavedCandles(p.getString(keyMarket2h, "").orEmpty())
        val primary = p.getStrategy("primary", defaultPrimary()).withMarketOnly(candles4h)
        val experimental = p.getStrategy("experiment", defaultExperimental()).withMarketOnly(candles2h)
        val ind4h = indicators(candles4h, primaryFastPeriod, primarySlowPeriod)
        val ind2h = indicators(candles2h, experimentFastPeriod, experimentSlowPeriod)

        return BotSnapshot(
            running = p.getBoolean(keyRunning, false),
            startedAt = p.getLong(keyStartedAt, 0L),
            lastSync = p.getLong(keyLastSync, 0L),
            primary = result(primary),
            experimental = result(experimental),
            primaryChart = ChartBundle(candles4h, ind4h.fast, ind4h.slow, primary.trades, "30m RSI Recovery: Yellow EMA50 / Purple EMA200"),
            experimentalChart = ChartBundle(candles2h, ind2h.fast, ind2h.slow, experimental.trades, "2h Supertrend Trend: Yellow EMA34 / Purple EMA200")
        )
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
                        openTime = row.getLong(0),
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

        return candles.sortedBy { it.closeTime }
    }

    fun result(state: StrategyState): StrategyResult {
        val equity = if (state.coins > 0.0 && state.lastPrice > 0.0) {
            state.coins * state.lastPrice * (1 - feeRate)
        } else {
            state.cash
        }
        val profit = equity - startBalance
        val totalFees = state.trades.sumOf { it.fee }
        return StrategyResult(state, equity, profit, profit / startBalance * 100.0, totalFees, state.trades.size)
    }

    fun backtest(strategyId: String, candles4h: List<PumpCandle>, candles2h: List<PumpCandle>, startTime: Long): StrategyResult {
        val state = if (strategyId == "experiment") {
            defaultExperimental().copy(
                lastProcessedCandle = startTime,
                lastReason = "Backtest from ${formatTime(startTime)}"
            )
        } else {
            defaultPrimary().copy(
                lastProcessedCandle = startTime,
                lastReason = "Backtest from ${formatTime(startTime)}"
            )
        }
        val next = if (strategyId == "experiment") {
            runExperimental(state, candles2h, candles4h)
        } else {
            runPrimary(state, candles4h)
        }
        return result(next)
    }

    private fun runPrimary(initial: StrategyState, candles: List<PumpCandle>): StrategyState {
        if (candles.size < primarySlowPeriod + 10) return initial.withMarketOnly(candles, "Waiting for enough 30m candles")

        val closes = candles.map { it.close }
        val ema200 = ema(closes, primarySlowPeriod)
        val rsi14 = rsi(closes, primaryRsiPeriod)
        var state = initial.withMarketOnly(candles)
        val first = maxOf(primarySlowPeriod + 4, adxPeriod * 2)

        for (i in first until candles.size) {
            val candle = candles[i]
            if (candle.closeTime <= state.lastProcessedCandle) continue

            val rsiNow = value(rsi14, i)
            val emaNow = value(ema200, i)
            val trendOk = candle.close > emaNow
            val recovered = rsiNow >= primarySellRsi
            val trendLost = candle.close < emaNow
            val stop = stopHit(state, candle.close, primaryStopLoss, primaryTrailingStop)
            state = applySignal(
                state = state,
                candle = candle,
                shouldBuy = state.coins <= 0.0 && trendOk && rsiNow <= primaryBuyRsi,
                shouldSell = state.coins > 0.0 && (recovered || trendLost || stop),
                buyReason = String.format(Locale.US, "30m RSI recovery entry: RSI %.1f <= %.0f and price above EMA200", rsiNow, primaryBuyRsi),
                sellReason = when {
                    stop -> "30m recovery exit: stop/trailing protection"
                    recovered -> String.format(Locale.US, "30m recovery exit: RSI %.1f >= %.0f", rsiNow, primarySellRsi)
                    trendLost -> "30m recovery exit: price lost EMA200"
                    else -> "No exit"
                }
            )
        }

        return state
    }

    private fun runExperimental(initial: StrategyState, candles2h: List<PumpCandle>, candles4h: List<PumpCandle>): StrategyState {
        if (candles2h.size < experimentSlowPeriod + 10) {
            return initial.withMarketOnly(candles2h, "Waiting for enough 2h candles")
        }

        val closes = candles2h.map { it.close }
        val ema34 = ema(closes, experimentFastPeriod)
        val ema200 = ema(closes, experimentSlowPeriod)
        val rsi14 = rsi(closes, experimentRsiPeriod)
        val st = supertrendTrend(candles2h, experimentSupertrendPeriod, experimentSupertrendMultiplier)
        var state = initial.withMarketOnly(candles2h)
        val first = maxOf(experimentSlowPeriod + 4, adxPeriod * 2)

        for (i in first until candles2h.size) {
            val candle = candles2h[i]
            if (candle.closeTime <= state.lastProcessedCandle) continue

            val trendUp = value(ema34, i) > value(ema200, i)
            val stTurnedUp = st.getOrElse(i) { 0 } > 0 && st.getOrElse(i - 1) { 0 } < 0
            val stDown = st.getOrElse(i) { 0 } < 0
            val rsiOk = value(rsi14, i) >= experimentMinRsi
            val stop = stopHit(state, candle.close, experimentStopLoss, experimentTrailingStop)

            state = applySignal(
                state = state,
                candle = candle,
                shouldBuy = state.coins <= 0.0 && stTurnedUp && trendUp && rsiOk,
                shouldSell = state.coins > 0.0 && (stDown || !trendUp || stop),
                buyReason = String.format(Locale.US, "2h Supertrend turned up, EMA34 > EMA200, RSI %.1f", value(rsi14, i)),
                sellReason = when {
                    stop -> "2h Supertrend exit: stop/trailing protection"
                    stDown -> "2h Supertrend exit: trend flipped down"
                    !trendUp -> "2h Supertrend exit: EMA34 fell below EMA200"
                    else -> "No exit"
                }
            )
        }

        return state
    }

    private fun applySignal(
        state: StrategyState,
        candle: PumpCandle,
        shouldBuy: Boolean,
        shouldSell: Boolean,
        buyReason: String,
        sellReason: String
    ): StrategyState {
        var next = state.copy(
            lastProcessedCandle = candle.closeTime,
            lastPrice = candle.close,
            lastUpdated = System.currentTimeMillis()
        )

        if (shouldBuy) {
            val buyPrice = candle.close * (1 + slippage)
            val amount = next.cash
            val fee = amount * feeRate
            val coins = (amount - fee) / buyPrice
            next = next.copy(
                cash = 0.0,
                coins = coins,
                entryPrice = buyPrice,
                positionCost = amount,
                highestClose = candle.close,
                buys = next.buys + 1,
                lastAction = "BUY",
                lastReason = buyReason
            ).addTrade(candle.closeTime, "BUY", buyPrice, amount, fee, coins * buyPrice, 0.0, coins)
        } else if (shouldSell) {
            val sellPrice = candle.close * (1 - slippage)
            val gross = next.coins * sellPrice
            val fee = gross * feeRate
            val cash = gross - fee
            val pnl = cash - next.positionCost
            next = next.copy(
                cash = cash,
                coins = 0.0,
                entryPrice = 0.0,
                positionCost = 0.0,
                highestClose = 0.0,
                sells = next.sells + 1,
                lastAction = "SELL",
                lastReason = sellReason
            ).addTrade(candle.closeTime, "SELL", sellPrice, gross, fee, cash, pnl, 0.0)
        } else if (next.coins > 0.0) {
            next = next.copy(
                highestClose = maxOf(next.highestClose, candle.close),
                lastAction = "HOLD",
                lastReason = "Position stays open"
            )
        } else {
            next = next.copy(
                lastAction = "WAIT",
                lastReason = "No valid entry signal"
            )
        }

        return next
    }

    private fun defaultPrimary(): StrategyState {
        return StrategyState(
            id = "primary",
            title = "30m RSI Recovery",
            subtitle = "Buy RSI pullbacks above EMA200; sell on recovery or trailing stop",
            cash = startBalance,
            coins = 0.0,
            entryPrice = 0.0,
            positionCost = 0.0,
            highestClose = 0.0,
            lastProcessedCandle = 0L,
            buys = 0,
            sells = 0,
            lastAction = "WAIT",
            lastReason = "Press START to begin",
            lastPrice = 0.0,
            lastUpdated = 0L,
            trades = emptyList()
        )
    }

    private fun defaultExperimental(): StrategyState {
        return StrategyState(
            id = "experiment",
            title = "2h Supertrend Trend",
            subtitle = "Buy Supertrend flips with EMA34/EMA200 trend confirmation",
            cash = startBalance,
            coins = 0.0,
            entryPrice = 0.0,
            positionCost = 0.0,
            highestClose = 0.0,
            lastProcessedCandle = 0L,
            buys = 0,
            sells = 0,
            lastAction = "WAIT",
            lastReason = "Press START to begin",
            lastPrice = 0.0,
            lastUpdated = 0L,
            trades = emptyList()
        )
    }

    private fun StrategyState.withMarketOnly(candles: List<PumpCandle>, reason: String? = null): StrategyState {
        val latest = candles.lastOrNull()?.close ?: lastPrice
        return copy(
            lastPrice = latest,
            lastUpdated = if (latest > 0.0) System.currentTimeMillis() else lastUpdated,
            lastReason = reason ?: lastReason
        )
    }

    private data class FilterInfo(
        val trendOk: Boolean,
        val directionOk: Boolean,
        val text: String
    )

    private fun filterInfo(
        candles: List<PumpCandle>,
        ind: IndicatorSeries,
        i: Int,
        adxMin: Double,
        minAtrPercent: Double,
        volumeFactor: Double
    ): FilterInfo {
        val candle = candles[i]
        val slowNow = value(ind.slow, i)
        val slowSlope = slowNow - value(ind.slow, i - 3)
        val atrPercent = if (value(ind.atr, i) > 0.0) value(ind.atr, i) / candle.close * 100.0 else 0.0
        val volumeOk = value(ind.volumeAverage, i) > 0.0 && candle.volume >= value(ind.volumeAverage, i) * volumeFactor
        val directionOk = slowSlope > 0.0 && candle.close > slowNow
        val trendOk = value(ind.adx, i) >= adxMin && directionOk && volumeOk && atrPercent >= minAtrPercent
        val text = String.format(
            Locale.US,
            "ADX %.1f, ATR %.2f%%, volume %s, slow WMA %s",
            value(ind.adx, i),
            atrPercent,
            if (volumeOk) "ok" else "weak",
            if (slowSlope > 0.0) "up" else "flat/down"
        )
        return FilterInfo(trendOk, directionOk, text)
    }

    private fun sellReason(
        state: StrategyState,
        close: Double,
        crossedDown: Boolean,
        peakExit: Boolean,
        info: FilterInfo,
        stopLoss: Double,
        trailingStop: Double
    ): String {
        return when {
            close <= state.entryPrice * (1 - stopLoss) -> "Stop-loss. ${info.text}"
            close <= state.highestClose * (1 - trailingStop) -> "Trailing stop. ${info.text}"
            crossedDown -> "WMA crossed down. ${info.text}"
            peakExit -> "Local peak exit: fast WMA turned down and price fell below it. ${info.text}"
            trendFaded(info) -> "Trend faded into sideways/weak market. ${info.text}"
            else -> "No exit"
        }
    }

    private fun trendFaded(info: FilterInfo): Boolean {
        return !info.directionOk
    }

    private fun stopHit(state: StrategyState, close: Double, stopLoss: Double, trailingStop: Double): Boolean {
        return state.coins > 0.0 &&
            (close <= state.entryPrice * (1 - stopLoss) || close <= state.highestClose * (1 - trailingStop))
    }

    private fun StrategyState.addTrade(
        time: Long,
        action: String,
        price: Double,
        amount: Double,
        fee: Double,
        equity: Double,
        pnl: Double,
        coins: Double
    ): StrategyState {
        val nextTrades = trades + TradeEvent(time, action, price, amount, fee, equity, pnl, coins)
        return copy(trades = nextTrades)
    }

    private fun indicators(candles: List<PumpCandle>, fastPeriod: Int, slowPeriod: Int): IndicatorSeries {
        val closes = candles.map { it.close }
        val volumes = candles.map { it.volume }
        return IndicatorSeries(
            fast = ema(closes, fastPeriod),
            slow = ema(closes, slowPeriod),
            adx = adx(candles, adxPeriod),
            atr = atr(candles, adxPeriod),
            volumeAverage = sma(volumes, 20)
        )
    }

    private fun parseSavedCandles(json: String): List<PumpCandle> {
        if (json.isBlank()) return emptyList()
        return try {
            parseCandles(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun crossedUp(fast: List<Double?>, slow: List<Double?>, i: Int): Boolean {
        return value(fast, i - 1) <= value(slow, i - 1) && value(fast, i) > value(slow, i)
    }

    private fun crossedDown(fast: List<Double?>, slow: List<Double?>, i: Int): Boolean {
        return value(fast, i - 1) >= value(slow, i - 1) && value(fast, i) < value(slow, i)
    }

    private fun bullishResume(candles: List<PumpCandle>, ind: IndicatorSeries, i: Int): Boolean {
        val fastNow = value(ind.fast, i)
        val fastPrev = value(ind.fast, i - 1)
        val slowNow = value(ind.slow, i)
        val closeNow = candles[i].close
        val closePrev = candles[i - 1].close
        return fastNow > slowNow &&
            fastNow > fastPrev &&
            closeNow > fastNow &&
            closePrev <= fastPrev
    }

    private fun bearishPeakExit(candles: List<PumpCandle>, ind: IndicatorSeries, i: Int): Boolean {
        val fastNow = value(ind.fast, i)
        val fastPrev = value(ind.fast, i - 1)
        val closeNow = candles[i].close
        return fastNow < fastPrev && closeNow < fastNow
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

    private fun ema(values: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(values.size) { null }
        val multiplier = 2.0 / (period + 1.0)
        var previous: Double? = null

        for (i in values.indices) {
            if (i == period - 1) {
                var sum = 0.0
                for (j in 0 until period) sum += values[j]
                previous = sum / period
                result[i] = previous
            } else if (i >= period && previous != null) {
                previous = values[i] * multiplier + previous * (1 - multiplier)
                result[i] = previous
            }
        }

        return result
    }

    private fun rsi(values: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(values.size) { null }
        var averageGain = 0.0
        var averageLoss = 0.0

        for (i in 1 until values.size) {
            val change = values[i] - values[i - 1]
            val gain = maxOf(change, 0.0)
            val loss = maxOf(-change, 0.0)
            if (i <= period) {
                averageGain += gain
                averageLoss += loss
                if (i == period) {
                    averageGain /= period
                    averageLoss /= period
                    result[i] = if (averageLoss == 0.0) 100.0 else 100.0 - 100.0 / (1.0 + averageGain / averageLoss)
                }
            } else {
                averageGain = (averageGain * (period - 1) + gain) / period
                averageLoss = (averageLoss * (period - 1) + loss) / period
                result[i] = if (averageLoss == 0.0) 100.0 else 100.0 - 100.0 / (1.0 + averageGain / averageLoss)
            }
        }

        return result
    }

    private fun supertrendTrend(candles: List<PumpCandle>, period: Int, multiplier: Double): List<Int> {
        val trend = MutableList(candles.size) { 0 }
        val upper = MutableList<Double?>(candles.size) { null }
        val lower = MutableList<Double?>(candles.size) { null }
        val range = trueRanges(candles)
        val averageRange = ema(range, period)

        for (i in candles.indices) {
            val atrValue = averageRange[i] ?: continue
            val midpoint = (candles[i].high + candles[i].low) / 2.0
            val basicUpper = midpoint + multiplier * atrValue
            val basicLower = midpoint - multiplier * atrValue
            if (i == 0 || upper[i - 1] == null || lower[i - 1] == null) {
                upper[i] = basicUpper
                lower[i] = basicLower
                trend[i] = 1
                continue
            }

            val previousUpper = upper[i - 1] ?: basicUpper
            val previousLower = lower[i - 1] ?: basicLower
            upper[i] = if (basicUpper < previousUpper || candles[i - 1].close > previousUpper) basicUpper else previousUpper
            lower[i] = if (basicLower > previousLower || candles[i - 1].close < previousLower) basicLower else previousLower
            trend[i] = when {
                trend[i - 1] < 0 && candles[i].close > (upper[i] ?: basicUpper) -> 1
                trend[i - 1] > 0 && candles[i].close < (lower[i] ?: basicLower) -> -1
                else -> trend[i - 1]
            }
        }

        return trend
    }

    private fun atr(candles: List<PumpCandle>, period: Int): List<Double?> {
        return sma(trueRanges(candles), period)
    }

    private fun trueRanges(candles: List<PumpCandle>): List<Double> {
        return candles.mapIndexed { index, candle ->
            if (index == 0) {
                candle.high - candle.low
            } else {
                val previousClose = candles[index - 1].close
                maxOf(
                    candle.high - candle.low,
                    kotlin.math.abs(candle.high - previousClose),
                    kotlin.math.abs(candle.low - previousClose)
                )
            }
        }
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
            ranges[i] = maxOf(
                candles[i].high - candles[i].low,
                kotlin.math.abs(candles[i].high - candles[i - 1].close),
                kotlin.math.abs(candles[i].low - candles[i - 1].close)
            )
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

    private fun SharedPreferences.Editor.putStrategy(state: StrategyState): SharedPreferences.Editor {
        val prefix = "${state.id}_"
        putString("${prefix}title", state.title)
        putString("${prefix}subtitle", state.subtitle)
        putDouble("${prefix}cash", state.cash)
        putDouble("${prefix}coins", state.coins)
        putDouble("${prefix}entry", state.entryPrice)
        putDouble("${prefix}position_cost", state.positionCost)
        putDouble("${prefix}highest", state.highestClose)
        putLong("${prefix}last_candle", state.lastProcessedCandle)
        putInt("${prefix}buys", state.buys)
        putInt("${prefix}sells", state.sells)
        putString("${prefix}action", state.lastAction)
        putString("${prefix}reason", state.lastReason)
        putDouble("${prefix}last_price", state.lastPrice)
        putLong("${prefix}updated", state.lastUpdated)
        putString("${prefix}trades", tradesToJson(state.trades))
        return this
    }

    private fun SharedPreferences.getStrategy(id: String, fallback: StrategyState): StrategyState {
        val prefix = "${id}_"
        return StrategyState(
            id = id,
            title = getString("${prefix}title", fallback.title).orEmpty(),
            subtitle = getString("${prefix}subtitle", fallback.subtitle).orEmpty(),
            cash = getDouble("${prefix}cash", fallback.cash),
            coins = getDouble("${prefix}coins", fallback.coins),
            entryPrice = getDouble("${prefix}entry", fallback.entryPrice),
            positionCost = getDouble("${prefix}position_cost", fallback.positionCost),
            highestClose = getDouble("${prefix}highest", fallback.highestClose),
            lastProcessedCandle = getLong("${prefix}last_candle", fallback.lastProcessedCandle),
            buys = getInt("${prefix}buys", fallback.buys),
            sells = getInt("${prefix}sells", fallback.sells),
            lastAction = getString("${prefix}action", fallback.lastAction).orEmpty(),
            lastReason = getString("${prefix}reason", fallback.lastReason).orEmpty(),
            lastPrice = getDouble("${prefix}last_price", fallback.lastPrice),
            lastUpdated = getLong("${prefix}updated", fallback.lastUpdated),
            trades = jsonToTrades(getString("${prefix}trades", "[]").orEmpty())
        )
    }

    private fun tradesToJson(trades: List<TradeEvent>): String {
        val array = JSONArray()
        trades.forEach { trade ->
            array.put(
                JSONObject()
                    .put("time", trade.time)
                    .put("action", trade.action)
                    .put("price", trade.price)
                    .put("amount", trade.amount)
                    .put("fee", trade.fee)
                    .put("equity", trade.equity)
                    .put("pnl", trade.pnl)
                    .put("coins", trade.coins)
            )
        }
        return array.toString()
    }

    private fun jsonToTrades(json: String): List<TradeEvent> {
        return try {
            val array = JSONArray(json)
            val result = ArrayList<TradeEvent>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    TradeEvent(
                        time = obj.getLong("time"),
                        action = obj.getString("action"),
                        price = obj.getDouble("price"),
                        amount = obj.optDouble("amount", obj.optDouble("equity", 0.0)),
                        fee = obj.optDouble("fee", 0.0),
                        equity = obj.getDouble("equity"),
                        pnl = obj.optDouble("pnl", 0.0),
                        coins = obj.optDouble("coins", 0.0)
                    )
                )
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun formatTime(time: Long): String {
        if (time <= 0L) return "-"
        return SimpleDateFormat("dd.MM HH:mm", Locale.GERMAN).format(Date(time))
    }

    private fun SharedPreferences.Editor.putDouble(key: String, value: Double): SharedPreferences.Editor {
        return putLong(key, java.lang.Double.doubleToRawLongBits(value))
    }

    private fun SharedPreferences.getDouble(key: String, default: Double): Double {
        return if (contains(key)) java.lang.Double.longBitsToDouble(getLong(key, 0L)) else default
    }
}
