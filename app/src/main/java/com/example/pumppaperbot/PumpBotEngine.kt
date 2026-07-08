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

    private const val prefsName = "PumpPaperBotV2"
    private const val keyRunning = "running"
    private const val keyStartedAt = "started_at"
    private const val keyLastSync = "last_sync"
    private const val keyMarket4h = "market_4h_json"
    private const val keyMarket2h = "market_2h_json"
    private const val primaryFastPeriod = 25
    private const val primarySlowPeriod = 99
    private const val experimentFastPeriod = 34
    private const val experimentSlowPeriod = 99
    private const val adxPeriod = 14
    private const val primaryAdxTrendMin = 14.0
    private const val experimentAdxTrendMin = 14.0
    private const val primaryMinAtrPercent = 0.45
    private const val experimentMinAtrPercent = 0.25
    private const val primaryVolumeFactor = 0.65
    private const val experimentVolumeFactor = 0.55
    const val feeRate = 0.0015
    private const val slippage = 0.0005
    private const val primaryStopLoss = 0.08
    private const val experimentStopLoss = 0.05
    private const val primaryTrailingStop = 0.10
    private const val experimentTrailingStop = 0.05

    fun klineUrl(interval: String): String {
        return "https://data-api.binance.vision/api/v3/klines?symbol=$symbol&interval=$interval&limit=500"
    }

    fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    fun ensureInitialized(context: Context) {
        val p = prefs(context)
        if (!p.contains("primary_cash")) reset(context)
    }

    fun reset(context: Context) {
        prefs(context).edit()
            .clear()
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
            primaryChart = ChartBundle(candles4h, ind4h.fast, ind4h.slow, primary.trades, "Primary 4H WMA25/WMA99"),
            experimentalChart = ChartBundle(candles2h, ind2h.fast, ind2h.slow, experimental.trades, "Experiment 2H WMA34/WMA99 with 4H fast-line filter")
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
        if (candles.size < primarySlowPeriod + 10) return initial.withMarketOnly(candles, "Waiting for enough 4H candles")

        val ind = indicators(candles, primaryFastPeriod, primarySlowPeriod)
        var state = initial.withMarketOnly(candles)
        val first = maxOf(primarySlowPeriod + 4, adxPeriod * 2)

        for (i in first until candles.size) {
            val candle = candles[i]
            if (candle.closeTime <= state.lastProcessedCandle) continue

            val info = filterInfo(candles, ind, i, primaryAdxTrendMin, primaryMinAtrPercent, primaryVolumeFactor)
            val crossedUp = crossedUp(ind.fast, ind.slow, i)
            val crossedDown = crossedDown(ind.fast, ind.slow, i)
            val resumeUp = bullishResume(candles, ind, i)
            val peakExit = bearishPeakExit(candles, ind, i)
            state = applySignal(
                state = state,
                candle = candle,
                shouldBuy = state.coins <= 0.0 && info.trendOk && (crossedUp || resumeUp),
                shouldSell = state.coins > 0.0 && (crossedDown || peakExit || stopHit(state, candle.close, primaryStopLoss, primaryTrailingStop) || trendFaded(info)),
                buyReason = if (crossedUp) "4H WMA crossed up. ${info.text}" else "4H pullback resumed inside trend. ${info.text}",
                sellReason = sellReason(state, candle.close, crossedDown, peakExit, info, primaryStopLoss, primaryTrailingStop)
            )
        }

        return state
    }

    private fun runExperimental(initial: StrategyState, candles2h: List<PumpCandle>, candles4h: List<PumpCandle>): StrategyState {
        if (candles2h.size < experimentSlowPeriod + 10 || candles4h.size < primarySlowPeriod + 10) {
            return initial.withMarketOnly(candles2h, "Waiting for enough 2H/4H candles")
        }

        val ind2h = indicators(candles2h, experimentFastPeriod, experimentSlowPeriod)
        val ind4h = indicators(candles4h, primaryFastPeriod, primarySlowPeriod)
        var state = initial.withMarketOnly(candles2h)
        val first = maxOf(experimentSlowPeriod + 4, adxPeriod * 2)

        for (i in first until candles2h.size) {
            val candle = candles2h[i]
            if (candle.closeTime <= state.lastProcessedCandle) continue

            val twoHour = filterInfo(candles2h, ind2h, i, experimentAdxTrendMin, experimentMinAtrPercent, experimentVolumeFactor)
            val fourIndex = candles4h.indexOfLast { it.closeTime <= candle.closeTime }
            val fourHourFirst = maxOf(primarySlowPeriod + 4, adxPeriod * 2)
            val fourHour = if (fourIndex >= fourHourFirst) {
                filterInfo(candles4h, ind4h, fourIndex, primaryAdxTrendMin, experimentMinAtrPercent, 0.4)
            } else {
                null
            }
            val fourHourTrendOk = fourHour != null && candles4h[fourIndex].close > value(ind4h.fast, fourIndex)
            val fourHourTrendWeak = fourHour == null || value(ind4h.fast, fourIndex) < value(ind4h.slow, fourIndex)
            val crossedUp2h = crossedUp(ind2h.fast, ind2h.slow, i)
            val crossedDown2h = crossedDown(ind2h.fast, ind2h.slow, i)
            val resumeUp2h = bullishResume(candles2h, ind2h, i)
            val peakExit2h = bearishPeakExit(candles2h, ind2h, i)

            state = applySignal(
                state = state,
                candle = candle,
                shouldBuy = state.coins <= 0.0 && twoHour.trendOk && fourHourTrendOk && (crossedUp2h || resumeUp2h),
                shouldSell = state.coins > 0.0 && (crossedDown2h || peakExit2h || stopHit(state, candle.close, experimentStopLoss, experimentTrailingStop) || fourHourTrendWeak),
                buyReason = if (crossedUp2h) {
                    "2H WMA crossed up with 4H fast-line confirmation. 2H ${twoHour.text}; 4H ${fourHour?.text ?: "not ready"}"
                } else {
                    "2H pullback resumed with 4H fast-line confirmation. 2H ${twoHour.text}; 4H ${fourHour?.text ?: "not ready"}"
                },
                sellReason = when {
                    stopHit(state, candle.close, experimentStopLoss, experimentTrailingStop) -> sellReason(state, candle.close, false, peakExit2h, twoHour, experimentStopLoss, experimentTrailingStop)
                    crossedDown2h -> "2H WMA crossed down. ${twoHour.text}"
                    peakExit2h -> "2H local peak exit. ${twoHour.text}"
                    fourHourTrendWeak -> "4H trend filter weakened. ${fourHour?.text ?: "4H not ready"}"
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
            title = "Primary 4H",
            subtitle = "Buy and sell by 4H WMA25/WMA99",
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
            title = "Experiment 2H + 4H",
            subtitle = "2H WMA34/WMA99 entries with 4H fast-line confirmation",
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
            fast = wma(closes, fastPeriod),
            slow = wma(closes, slowPeriod),
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

    private fun atr(candles: List<PumpCandle>, period: Int): List<Double?> {
        val ranges = candles.mapIndexed { index, candle ->
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
