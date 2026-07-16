package com.example.pumppaperbot

import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class FundingPoint(val time: Long, val rate: Double)

data class V2EntrySignal(
    val mode: String,
    val reason: String,
    val rsi: Double,
    val ema200: Double,
    val funding: Double,
    val candleTime: Long,
    val trendReadiness: Int = 0,
    val shockReadiness: Int = 0,
    val marketGateActive: Boolean = false,
    val marketGateBlockedEntry: Boolean = false,
    val lateEntryBlocked: Boolean = false,
    val breathing: MarketBreathingSnapshot = MarketBreathingSnapshot.unknown()
) {
    val active: Boolean get() = mode == StrategyV2.MODE_TREND ||
        mode == StrategyV2.MODE_SHOCK || mode == StrategyV2.MODE_EXHAUSTION
}

data class MarketOverheatGate(
    val active: Boolean,
    val pumpReturn: Double = 0.0,
    val btcReturn: Double = 0.0,
    val solReturn: Double = 0.0,
    val historySamples: Int = 0
)

data class V2ExitSignal(
    val action: String,
    val reason: String,
    val highestHigh: Double,
    val readiness: Int = 0
)

/**
 * Four-stage PUMP/EUR strategy.
 *
 * The original trend and single-shock routes remain as the stable baseline.  The
 * new exhaustion route is deliberately armed only after three separate declines,
 * then requires price recovery, actual buyer flow and a broad-market risk check.
 * Every decision uses a closed 30-minute candle; a backtest enters at the next
 * candle open, so future information cannot leak into an entry.
 */
object StrategyV2 {
    const val MODE_NONE = "NONE"
    const val MODE_TREND = "TREND"
    const val MODE_SHOCK = "SHOCK"
    const val MODE_EXHAUSTION = "EXHAUSTION"
    const val ACTION_WAIT = "WAIT"
    const val ACTION_SELL = "SELL"
    // Kept for saved-state compatibility.  V1.7 sells 40%, not half.
    const val ACTION_SELL_HALF = "SELL_HALF"

    const val PRICE_STOP = 0.044
    const val TREND_TARGET = 0.08
    const val SHOCK_FIRST_TARGET = 0.06
    const val EXHAUSTION_FIRST_TARGET = 0.07
    const val EXHAUSTION_SECOND_TARGET = 0.14
    const val EXHAUSTION_PARTIAL_FRACTION = 0.40
    const val RUNNER_TRAIL = 0.04
    const val RUNNER_FLOOR = 0.004
    const val BASE_MAX_HOLD_BARS = 48
    const val EXHAUSTION_MAX_HOLD_BARS = 96
    const val LOSS_COOLDOWN_BARS = 12
    const val BASE_MAX_HOLD_MILLIS = 24L * 60L * 60L * 1000L
    const val EXHAUSTION_MAX_HOLD_MILLIS = 48L * 60L * 60L * 1000L
    const val MARKET_GATE_LOOKBACK_BARS = 30 * 48
    const val MARKET_GATE_MIN_HISTORY = 360
    const val MARKET_GATE_HORIZON_BARS = 2
    const val MARKET_GATE_RETURN_QUANTILE = 0.90
    const val MARKET_GATE_VOLUME_FLOOR = 1.15

    private const val RSI_PERIOD = 14
    private const val EMA_FAST = 50
    private const val EMA_SLOW = 200
    private const val TREND_ARM_BARS = 24
    private const val SHOCK_ARM_BARS = 36
    private const val EXHAUSTION_ARM_BARS = 12

    fun synthesizeEur(asset: List<PumpCandle>, eurUsdt: List<PumpCandle>): List<PumpCandle> {
        val eurByClose = eurUsdt.associateBy { it.closeTime }
        return asset.mapNotNull { coin ->
            val eur = eurByClose[coin.closeTime] ?: return@mapNotNull null
            if (eur.open <= 0.0 || eur.high <= 0.0 || eur.low <= 0.0 || eur.close <= 0.0) return@mapNotNull null
            PumpCandle(
                openTime = coin.openTime,
                open = coin.open / eur.open,
                high = coin.high / eur.low,
                low = coin.low / eur.high,
                close = coin.close / eur.close,
                volume = coin.volume,
                closeTime = coin.closeTime,
                quoteVolume = coin.quoteVolume,
                tradeCount = coin.tradeCount,
                takerBuyVolume = coin.takerBuyVolume
            )
        }.sortedBy { it.closeTime }
    }

    fun latestEntrySignal(
        pumpEur: List<PumpCandle>,
        btcUsdt: List<PumpCandle>,
        funding: List<FundingPoint>,
        aggressive: Boolean = false,
        ethUsdt: List<PumpCandle> = emptyList(),
        solUsdt: List<PumpCandle> = emptyList(),
        pumpFutures: List<PumpCandle> = emptyList(),
        premium: List<PumpCandle> = emptyList(),
        orderBook: OrderBookMetrics? = null,
        openInterest: Double? = null,
        openInterestChangePercent: Double? = null
    ): V2EntrySignal {
        if (pumpEur.size < EMA_SLOW + SHOCK_ARM_BARS || btcUsdt.size < EMA_SLOW + 7) {
            return V2EntrySignal(MODE_NONE, "Ждем достаточно закрытых 30-минутных свечей", 0.0, 0.0, 0.0, 0L)
        }
        val indicators = indicators(pumpEur, btcUsdt, ethUsdt, solUsdt, pumpFutures, premium)
        val breathing = MarketBreathingAnalyzer.analyzeAt(
            pumpEur.lastIndex,
            pumpEur,
            btcUsdt,
            solUsdt,
            pumpFutures,
            indicators.rsi,
            indicators.ema20,
            orderBook,
            openInterest,
            openInterestChangePercent
        )
        return entrySignalAt(
            pumpEur.lastIndex, pumpEur, btcUsdt, solUsdt, funding, indicators, aggressive, breathing
        )
    }

    fun evaluateExit(
        candle: PumpCandle,
        positionMode: String,
        entryPrice: Double,
        entryTime: Long,
        partialTaken: Boolean,
        storedHighestHigh: Double
    ): V2ExitSignal {
        if (entryPrice <= 0.0 || entryTime <= 0L) {
            return V2ExitSignal(ACTION_WAIT, "Нет подтвержденной позиции", storedHighestHigh)
        }
        val exhaustion = positionMode == MODE_EXHAUSTION
        val maxHoldMillis = if (exhaustion) EXHAUSTION_MAX_HOLD_MILLIS else BASE_MAX_HOLD_MILLIS
        val highestBefore = max(storedHighestHigh, entryPrice)
        val highestAfter = max(highestBefore, candle.high)
        val hardStopPrice = entryPrice * (1.0 - PRICE_STOP)
        val timedOut = candle.closeTime - entryTime >= maxHoldMillis
        val timeReadiness = ((candle.closeTime - entryTime).toDouble() / maxHoldMillis * 100.0)
            .roundToInt().coerceIn(0, 99)
        val stopReadiness = ((entryPrice - candle.close) / (entryPrice * PRICE_STOP) * 100.0)
            .roundToInt().coerceIn(0, 99)

        if (exhaustion) {
            if (!partialTaken) {
                return when {
                    candle.low <= hardStopPrice -> V2ExitSignal(
                        ACTION_SELL,
                        String.format(Locale.US, "СТОП: цена коснулась %.8f (−4,4%%)", hardStopPrice),
                        highestAfter
                    )
                    candle.high >= entryPrice * (1.0 + EXHAUSTION_FIRST_TARGET) -> V2ExitSignal(
                        ACTION_SELL_HALF,
                        "РАЗВОРОТ: продать 40% при +7%; 60% оставить до +14% или трейлинга",
                        highestAfter
                    )
                    timedOut -> V2ExitSignal(ACTION_SELL, "РАЗВОРОТ: прошло 48 часов — закрыть позицию", highestAfter)
                    else -> {
                        val target = ((candle.close / entryPrice - 1.0) / EXHAUSTION_FIRST_TARGET * 100.0)
                            .roundToInt().coerceIn(0, 99)
                        V2ExitSignal(ACTION_WAIT, "Ждем +7%, стоп −4,4% или 48 часов", highestAfter, max(max(target, stopReadiness), timeReadiness))
                    }
                }
            }
            val runnerStop = max(entryPrice * (1.0 + RUNNER_FLOOR), highestBefore * (1.0 - RUNNER_TRAIL))
            return when {
                candle.low <= runnerStop -> V2ExitSignal(
                    ACTION_SELL,
                    String.format(Locale.US, "ОСТАТОК 60%%: защитный уровень %.8f", runnerStop),
                    highestAfter
                )
                candle.high >= entryPrice * (1.0 + EXHAUSTION_SECOND_TARGET) -> V2ExitSignal(
                    ACTION_SELL,
                    "ОСТАТОК 60%: цель +14% достигнута",
                    highestAfter
                )
                timedOut -> V2ExitSignal(ACTION_SELL, "ОСТАТОК 60%: прошло 48 часов — закрыть", highestAfter)
                else -> {
                    val runnerRange = max(highestBefore - runnerStop, entryPrice * 0.001)
                    val readiness = ((highestBefore - candle.close) / runnerRange * 100.0).roundToInt().coerceIn(0, 99)
                    V2ExitSignal(ACTION_WAIT, "ОСТАТОК 60%: трейлинг 4%, защищено минимум +0,4%", highestAfter, max(readiness, timeReadiness))
                }
            }
        }

        if (positionMode == MODE_SHOCK) {
            if (!partialTaken) {
                return when {
                    candle.low <= hardStopPrice -> V2ExitSignal(ACTION_SELL, "СТОП −4,4%", highestAfter)
                    candle.high >= entryPrice * (1.0 + SHOCK_FIRST_TARGET) -> V2ExitSignal(
                        ACTION_SELL_HALF, "ИМПУЛЬС: продать 50% при +6%, остаток вести трейлингом 4%", highestAfter
                    )
                    timedOut -> V2ExitSignal(ACTION_SELL, "ИМПУЛЬС: прошло 24 часа — закрыть", highestAfter)
                    else -> {
                        val target = ((candle.close / entryPrice - 1.0) / SHOCK_FIRST_TARGET * 100.0).roundToInt().coerceIn(0, 99)
                        V2ExitSignal(ACTION_WAIT, "Импульс: ждем +6% или защитный стоп", highestAfter, max(max(target, stopReadiness), timeReadiness))
                    }
                }
            }
            val runnerStop = max(entryPrice, highestBefore * (1.0 - RUNNER_TRAIL))
            return when {
                candle.low <= runnerStop -> V2ExitSignal(ACTION_SELL, "ОСТАТОК 50%: трейлинг/безубыток", highestAfter)
                timedOut -> V2ExitSignal(ACTION_SELL, "ОСТАТОК 50%: прошло 24 часа — закрыть", highestAfter)
                else -> V2ExitSignal(ACTION_WAIT, "ОСТАТОК 50%: действует трейлинг 4%", highestAfter, timeReadiness)
            }
        }

        return when {
            candle.low <= hardStopPrice -> V2ExitSignal(ACTION_SELL, "СТОП −4,4%", highestAfter)
            candle.high >= entryPrice * (1.0 + TREND_TARGET) -> V2ExitSignal(ACTION_SELL, "ТРЕНД: цель +8% достигнута", highestAfter)
            timedOut -> V2ExitSignal(ACTION_SELL, "ТРЕНД: прошло 24 часа — закрыть", highestAfter)
            else -> {
                val target = ((candle.close / entryPrice - 1.0) / TREND_TARGET * 100.0).roundToInt().coerceIn(0, 99)
                V2ExitSignal(ACTION_WAIT, "ТРЕНД: ждем +8% или защитный стоп", highestAfter, max(max(target, stopReadiness), timeReadiness))
            }
        }
    }

    fun backtest(
        pumpEur: List<PumpCandle>,
        btcUsdt: List<PumpCandle>,
        funding: List<FundingPoint>,
        startTime: Long,
        aggressive: Boolean = false,
        ethUsdt: List<PumpCandle> = emptyList(),
        solUsdt: List<PumpCandle> = emptyList(),
        pumpFutures: List<PumpCandle> = emptyList(),
        premium: List<PumpCandle> = emptyList()
    ): BacktestResult {
        if (pumpEur.isEmpty()) return BacktestResult.empty(startTime)
        val indicators = indicators(pumpEur, btcUsdt, ethUsdt, solUsdt, pumpFutures, premium)
        var cash = PumpBotEngine.startBalance
        val trades = ArrayList<TradeEvent>()
        val closedEquity = arrayListOf(cash)
        var wins = 0
        var stops = 0
        var roundTrips = 0
        var overheatBlocks = 0
        var i = max(EMA_SLOW + SHOCK_ARM_BARS, RSI_PERIOD + 2)
        var cooldownUntil = -1

        while (i < pumpEur.lastIndex) {
            val candle = pumpEur[i]
            if (candle.closeTime < startTime || i < cooldownUntil) {
                i++
                continue
            }
            val signal = entrySignalAt(
                i,
                pumpEur,
                btcUsdt,
                solUsdt,
                funding,
                indicators,
                aggressive,
                MarketBreathingAnalyzer.riskOnlyAt(i, pumpEur, indicators.rsi, indicators.ema20)
            )
            if (!signal.active) {
                if (signal.marketGateBlockedEntry) overheatBlocks++
                i++
                continue
            }

            val entryIndex = i + 1
            val entryCandle = pumpEur[entryIndex]
            val initialCash = cash
            val entryPrice = entryCandle.open * (1.0 + PumpBotEngine.slippage)
            val buyFee = cash * PumpBotEngine.feeRate
            var coins = (cash - buyFee) / entryPrice
            cash = 0.0
            val profileWord = if (aggressive) "Активный" else "Осторожный"
            val buyReason = when (signal.mode) {
                MODE_EXHAUSTION -> "$profileWord вход после 3 падений: разворот + поток покупателей"
                MODE_SHOCK -> "Базовый вход после сильного импульса вниз"
                else -> "Базовый трендовый вход"
            }
            trades += TradeEvent(entryCandle.openTime, "BUY", entryPrice, initialCash, buyFee, coins * entryPrice, 0.0, coins, buyReason)

            val exhaustion = signal.mode == MODE_EXHAUSTION
            val maxHold = if (exhaustion) EXHAUSTION_MAX_HOLD_BARS else BASE_MAX_HOLD_BARS
            val firstTarget = when (signal.mode) {
                MODE_EXHAUSTION -> EXHAUSTION_FIRST_TARGET
                MODE_SHOCK -> SHOCK_FIRST_TARGET
                else -> TREND_TARGET
            }
            val partialFraction = when (signal.mode) {
                MODE_EXHAUSTION -> EXHAUSTION_PARTIAL_FRACTION
                MODE_SHOCK -> 0.50
                else -> 1.0
            }
            var partialTaken = false
            var highestAfterPartial = entryPrice
            var exitIndex = minOf(entryIndex + maxHold, pumpEur.lastIndex)
            var exitReason = if (exhaustion) "48 часов" else "24 часа"
            var j = entryIndex
            while (j <= exitIndex) {
                val future = pumpEur[j]
                val hardStop = entryPrice * (1.0 - PRICE_STOP)
                if (!partialTaken) {
                    // Conservative intrabar ordering: if stop and target occur in one candle, stop wins.
                    if (future.low <= hardStop) {
                        val sellPrice = hardStop * (1.0 - PumpBotEngine.slippage)
                        val gross = coins * sellPrice
                        val fee = gross * PumpBotEngine.feeRate
                        cash += gross - fee
                        trades += TradeEvent(future.closeTime, "SELL", sellPrice, gross, fee, cash, cash - initialCash, 0.0, "Стоп −4,4%")
                        coins = 0.0
                        stops++
                        exitIndex = j
                        exitReason = "stop"
                        break
                    }
                    if (future.high >= entryPrice * (1.0 + firstTarget)) {
                        val sellPrice = entryPrice * (1.0 + firstTarget) * (1.0 - PumpBotEngine.slippage)
                        if (partialFraction >= 0.999) {
                            val gross = coins * sellPrice
                            val fee = gross * PumpBotEngine.feeRate
                            cash += gross - fee
                            trades += TradeEvent(future.closeTime, "SELL", sellPrice, gross, fee, cash, cash - initialCash, 0.0, "Цель +8%")
                            coins = 0.0
                            exitIndex = j
                            exitReason = "target"
                            break
                        }
                        val soldCoins = coins * partialFraction
                        val gross = soldCoins * sellPrice
                        val fee = gross * PumpBotEngine.feeRate
                        cash += gross - fee
                        coins -= soldCoins
                        val percent = if (exhaustion) 40 else 50
                        trades += TradeEvent(
                            future.closeTime, "SELL_HALF", sellPrice, gross, fee,
                            cash + coins * sellPrice, cash + coins * sellPrice - initialCash, coins,
                            "$percent% при +${(firstTarget * 100).roundToInt()}%"
                        )
                        partialTaken = true
                        highestAfterPartial = max(entryPrice * (1.0 + firstTarget), future.high)
                        j++
                        continue
                    }
                } else {
                    val floor = if (exhaustion) entryPrice * (1.0 + RUNNER_FLOOR) else entryPrice
                    val runnerStop = max(floor, highestAfterPartial * (1.0 - RUNNER_TRAIL))
                    if (future.low <= runnerStop) {
                        val sellPrice = runnerStop * (1.0 - PumpBotEngine.slippage)
                        val gross = coins * sellPrice
                        val fee = gross * PumpBotEngine.feeRate
                        cash += gross - fee
                        trades += TradeEvent(future.closeTime, "SELL", sellPrice, gross, fee, cash, cash - initialCash, 0.0, "Трейлинг остатка 4%")
                        coins = 0.0
                        exitIndex = j
                        exitReason = "runner"
                        break
                    }
                    if (exhaustion && future.high >= entryPrice * (1.0 + EXHAUSTION_SECOND_TARGET)) {
                        val sellPrice = entryPrice * (1.0 + EXHAUSTION_SECOND_TARGET) * (1.0 - PumpBotEngine.slippage)
                        val gross = coins * sellPrice
                        val fee = gross * PumpBotEngine.feeRate
                        cash += gross - fee
                        trades += TradeEvent(future.closeTime, "SELL", sellPrice, gross, fee, cash, cash - initialCash, 0.0, "Остаток 60% при +14%")
                        coins = 0.0
                        exitIndex = j
                        exitReason = "target14"
                        break
                    }
                    highestAfterPartial = max(highestAfterPartial, future.high)
                }
                j++
            }

            if (coins > 0.0) {
                val exitCandle = pumpEur[exitIndex]
                val sellPrice = exitCandle.close * (1.0 - PumpBotEngine.slippage)
                val gross = coins * sellPrice
                val fee = gross * PumpBotEngine.feeRate
                cash += gross - fee
                trades += TradeEvent(exitCandle.closeTime, "SELL", sellPrice, gross, fee, cash, cash - initialCash, 0.0, exitReason)
            }
            val tradeReturn = cash / initialCash - 1.0
            if (tradeReturn > 0.0) wins++
            if (tradeReturn < 0.0 && exhaustion) cooldownUntil = exitIndex + LOSS_COOLDOWN_BARS
            roundTrips++
            closedEquity += cash
            i = exitIndex + 1
        }

        var peak = closedEquity.firstOrNull() ?: PumpBotEngine.startBalance
        var maxDrawdown = 0.0
        closedEquity.forEach { equity ->
            peak = max(peak, equity)
            if (peak > 0.0) maxDrawdown = minOf(maxDrawdown, equity / peak - 1.0)
        }
        val profit = cash - PumpBotEngine.startBalance
        return BacktestResult(
            assetName = "PUMP",
            symbol = "PUMP/EUR",
            strategyName = if (aggressive) {
                "Активный: 4 этапа + защита от часового перегрева PUMP/BTC/SOL"
            } else {
                "Осторожный: 4 этапа + защита от часового перегрева PUMP/BTC/SOL"
            },
            equity = cash,
            profit = profit,
            profitPercent = profit / PumpBotEngine.startBalance * 100.0,
            totalFees = trades.sumOf { it.fee },
            trades = trades,
            firstCandleTime = pumpEur.firstOrNull()?.openTime ?: 0L,
            lastCandleTime = pumpEur.lastOrNull()?.closeTime ?: 0L,
            roundTrips = roundTrips,
            winRatePercent = if (roundTrips > 0) wins.toDouble() / roundTrips * 100.0 else 0.0,
            maxDrawdownPercent = maxDrawdown * 100.0,
            stopCount = stops,
            blockedOverheatCount = overheatBlocks
        )
    }

    private data class Indicators(
        val rsi: List<Double?>,
        val ema20: List<Double?>,
        val ema200: List<Double?>,
        val ret1: List<Double>,
        val volumeRatio: List<Double>,
        val atrPct: List<Double?>,
        val drawdown36h: List<Double>,
        val shockCount18h: List<Int>,
        val spotImbalance3: List<Double?>,
        val futuresImbalance3: Map<Long, Double>,
        val relativeStrengthSlope: List<Double?>,
        val marketReturn3h: Map<Long, Double>,
        val premiumClose: Map<Long, Double>,
        val recoveryBreakout: List<Boolean>,
        val btcAbove200: Map<Long, Boolean>,
        val btcSlopePositive: Map<Long, Boolean>
    )

    private fun indicators(
        pump: List<PumpCandle>,
        btc: List<PumpCandle>,
        eth: List<PumpCandle>,
        sol: List<PumpCandle>,
        futures: List<PumpCandle>,
        premium: List<PumpCandle>
    ): Indicators {
        val closes = pump.map { it.close }
        val rsi = rsi(closes, RSI_PERIOD)
        val ema20 = ema(closes, 20)
        val ema200 = ema(closes, EMA_SLOW)
        val ret1 = returns(closes)
        val volumeRatio = pump.mapIndexed { index, candle ->
            if (index < 19) 0.0 else {
                val window = pump.subList(index - 19, index + 1).map { it.volume }.sorted()
                val median = (window[9] + window[10]) / 2.0
                if (median > 0.0) candle.volume / median else 0.0
            }
        }
        val atrPct = MutableList<Double?>(pump.size) { null }
        val trueRanges = pump.mapIndexed { index, candle ->
            if (index == 0) candle.high - candle.low else max(
                candle.high - candle.low,
                max(kotlin.math.abs(candle.high - pump[index - 1].close), kotlin.math.abs(candle.low - pump[index - 1].close))
            )
        }
        for (i in 13 until pump.size) atrPct[i] = trueRanges.subList(i - 13, i + 1).average() / pump[i].close

        val drawdown = MutableList(pump.size) { 0.0 }
        val shockCount = MutableList(pump.size) { 0 }
        val breakout = MutableList(pump.size) { false }
        for (i in pump.indices) {
            val maxHigh = pump.subList(max(0, i - 71), i + 1).maxOf { it.high }
            drawdown[i] = if (maxHigh > 0.0) pump[i].close / maxHigh - 1.0 else 0.0
            shockCount[i] = ret1.subList(max(0, i - 35), i + 1).count { it <= -0.02 }
            if (i >= 6) breakout[i] = pump[i].close >= pump.subList(i - 6, i).maxOf { it.high }
        }
        val spotImbalance3 = rollingImbalance(pump)
        val futuresImbalance = futures.mapIndexedNotNull { index, candle ->
            val value = rollingImbalanceAt(futures, index) ?: return@mapIndexedNotNull null
            candle.closeTime to value
        }.toMap()

        val btcByTime = btc.associateBy { it.closeTime }
        val ethByTime = eth.associateBy { it.closeTime }
        val solByTime = sol.associateBy { it.closeTime }
        val marketReturn = HashMap<Long, Double>()
        pump.forEachIndexed { index, candle ->
            if (index < 6) return@forEachIndexed
            val oldTime = pump[index - 6].closeTime
            val returns = listOfNotNull(
                pairedReturn(btcByTime, oldTime, candle.closeTime),
                pairedReturn(ethByTime, oldTime, candle.closeTime),
                pairedReturn(solByTime, oldTime, candle.closeTime)
            )
            if (returns.size == 3) marketReturn[candle.closeTime] = returns.average()
        }

        val beta = MutableList<Double?>(pump.size) { null }
        val btcReturnsAligned = pump.mapIndexed { index, candle ->
            val current = btcByTime[candle.closeTime]?.close
            val previous = if (index >= 1) btcByTime[pump[index - 1].closeTime]?.close else null
            if (current != null && previous != null && previous > 0.0) current / previous - 1.0 else Double.NaN
        }
        for (i in 95 until pump.size) {
            val xs = btcReturnsAligned.subList(i - 95, i + 1)
            val ys = ret1.subList(i - 95, i + 1)
            if (xs.any { !it.isFinite() }) continue
            val meanX = xs.average()
            val meanY = ys.average()
            var covariance = 0.0
            var variance = 0.0
            for (j in xs.indices) {
                covariance += (xs[j] - meanX) * (ys[j] - meanY)
                variance += (xs[j] - meanX) * (xs[j] - meanX)
            }
            if (variance > 0.0) beta[i] = (covariance / variance).coerceIn(-5.0, 10.0)
        }
        val relative = MutableList<Double?>(pump.size) { null }
        for (i in 6 until pump.size) {
            val currentBtc = btcByTime[pump[i].closeTime]?.close ?: continue
            val oldBtc = btcByTime[pump[i - 6].closeTime]?.close ?: continue
            val b = beta[i] ?: continue
            relative[i] = pump[i].close / pump[i - 6].close - 1.0 - b * (currentBtc / oldBtc - 1.0)
        }
        val relativeSlope = MutableList<Double?>(pump.size) { null }
        for (i in 11 until pump.size) {
            val values = relative.subList(i - 5, i + 1)
            if (values.any { it == null }) continue
            val ys = values.map { it!! }
            val meanY = ys.average()
            var numerator = 0.0
            for (j in 0..5) numerator += (j - 2.5) * (ys[j] - meanY)
            relativeSlope[i] = numerator / 17.5
        }

        val btcCloses = btc.map { it.close }
        val btcEma50 = ema(btcCloses, EMA_FAST)
        val btcEma200 = ema(btcCloses, EMA_SLOW)
        val btcAbove = HashMap<Long, Boolean>()
        val btcSlope = HashMap<Long, Boolean>()
        btc.indices.forEach { index ->
            val slow = btcEma200.getOrNull(index)
            val fast = btcEma50.getOrNull(index)
            val priorFast = btcEma50.getOrNull(index - 6)
            btcAbove[btc[index].closeTime] = slow != null && btc[index].close > slow
            btcSlope[btc[index].closeTime] = fast != null && priorFast != null && fast > priorFast
        }
        return Indicators(
            rsi, ema20, ema200, ret1, volumeRatio, atrPct, drawdown, shockCount,
            spotImbalance3, futuresImbalance, relativeSlope, marketReturn,
            premium.associate { it.closeTime to it.close }, breakout, btcAbove, btcSlope
        )
    }

    private fun entrySignalAt(
        index: Int,
        pump: List<PumpCandle>,
        btc: List<PumpCandle>,
        sol: List<PumpCandle>,
        funding: List<FundingPoint>,
        indicators: Indicators,
        aggressive: Boolean,
        breathing: MarketBreathingSnapshot
    ): V2EntrySignal {
        val candle = pump.getOrNull(index) ?: return V2EntrySignal(MODE_NONE, "Нет свечи", 0.0, 0.0, 0.0, 0L)
        val rsiNow = indicators.rsi.getOrNull(index) ?: 0.0
        val rsiPrevious = indicators.rsi.getOrNull(index - 1) ?: 0.0
        val emaNow = indicators.ema200.getOrNull(index) ?: 0.0
        val ema20Now = indicators.ema20.getOrNull(index) ?: 0.0
        val ema20Previous = indicators.ema20.getOrNull(index - 1) ?: 0.0
        val previousClose = pump.getOrNull(index - 1)?.close ?: 0.0
        val btcAbove = indicators.btcAbove200[candle.closeTime] == true
        val btcSlope = indicators.btcSlopePositive[candle.closeTime] == true
        val rate = fundingRateAt(funding, candle.closeTime)
        val extension = if (emaNow > 0.0) candle.close / emaNow - 1.0 else Double.POSITIVE_INFINITY
        val recentReturn6 = pump.getOrNull(index - 6)?.close?.takeIf { it > 0.0 }?.let { candle.close / it - 1.0 } ?: 0.0
        val noChase = indicators.ret1.getOrElse(index) { 0.0 } < 0.04 && recentReturn6 < 0.08 && extension <= 0.035
        val priceReady = extension in 0.0..0.035

        var trendArmed = false
        for (j in max(0, index - TREND_ARM_BARS + 1)..index) {
            if ((indicators.rsi.getOrNull(j) ?: 100.0) <= 40.0) trendArmed = true
        }
        val rsiCrossed = rsiNow in 45.0..55.0 && rsiPrevious < 45.0
        val trend = trendArmed && rsiCrossed && priceReady && btcAbove && btcSlope && rate <= 0.0 && noChase

        var baseShockArmed = false
        for (j in max(1, index - SHOCK_ARM_BARS + 1)..index) {
            if (indicators.ret1[j] <= -0.03 && indicators.volumeRatio[j] >= 3.0 &&
                (indicators.rsi.getOrNull(j) ?: 100.0) <= 40.0
            ) baseShockArmed = true
        }
        val previousEma = indicators.ema200.getOrNull(index - 1) ?: 0.0
        val previousExtension = if (previousEma > 0.0) previousClose / previousEma - 1.0 else Double.POSITIVE_INFINITY
        val previousReady = rsiPrevious in 45.0..55.0 && previousExtension in 0.0..0.035
        val baseShock = baseShockArmed && rsiNow in 45.0..55.0 && priceReady && !previousReady && btcAbove && noChase

        var exhaustionArmed = false
        for (j in max(0, index - EXHAUSTION_ARM_BARS + 1)..index) {
            if (indicators.drawdown36h[j] <= -0.06 && indicators.shockCount18h[j] >= 3) exhaustionArmed = true
        }
        val ema20Reclaim = ema20Now > 0.0 && ema20Previous > 0.0 && candle.close >= ema20Now && previousClose < ema20Previous
        val rsiCross55 = rsiNow >= 55.0 && rsiPrevious < 55.0
        val priceConfirmation = ema20Reclaim || (indicators.recoveryBreakout[index] && rsiCross55)
        val spotFlow = indicators.spotImbalance3.getOrNull(index)
        val futuresFlow = indicators.futuresImbalance3[candle.closeTime]
        val relativeSlope = indicators.relativeStrengthSlope.getOrNull(index)
        val marketReturn = indicators.marketReturn3h[candle.closeTime]
        val premium = indicators.premiumClose[candle.closeTime]
        val atr = indicators.atrPct.getOrNull(index)
        val currentDrawdownLimit = if (aggressive) -0.03 else -0.06
        val flowReady = spotFlow != null && futuresFlow != null && relativeSlope != null &&
            spotFlow >= 0.0 && futuresFlow >= 0.03 && relativeSlope >= 0.0
        val riskReady = marketReturn != null && premium != null && atr != null &&
            marketReturn >= -0.02 && premium <= 0.0001 && atr in 0.007..0.035 && extension <= 0.04
        val exhaustion = exhaustionArmed && priceConfirmation && rsiNow in 43.0..58.0 &&
            flowReady && riskReady && indicators.drawdown36h[index] <= currentDrawdownLimit

        val trendReadiness = baselineReadiness(
            trend, trendArmed, rsiNow, rsiPrevious, extension, btcAbove, btcSlope, rate <= 0.0, noChase
        )
        val exhaustionReadiness = exhaustionReadiness(
            active = exhaustion,
            armed = exhaustionArmed,
            priceConfirmation = priceConfirmation,
            rsiNow = rsiNow,
            flowReady = flowReady,
            spotFlow = spotFlow,
            futuresFlow = futuresFlow,
            relativeSlope = relativeSlope,
            riskReady = riskReady,
            currentDrawdown = indicators.drawdown36h[index],
            currentDrawdownLimit = currentDrawdownLimit
        )
        val baseShockReadiness = if (baseShock) 100 else if (baseShockArmed && rsiNow in 42.0..55.0 && btcAbove && noChase) 94 else 0
        val shockReadiness = max(baseShockReadiness, exhaustionReadiness)
        val entryWouldBeActive = exhaustion || baseShock || trend
        val lateRiskLimit = if (aggressive) 70 else 60
        val lateEntryBlocked = breathing.lateEntryRisk >= lateRiskLimit &&
            (entryWouldBeActive || max(trendReadiness, shockReadiness) >= 90)
        if (lateEntryBlocked) {
            return V2EntrySignal(
                MODE_NONE,
                String.format(
                    Locale.US,
                    "СЕЙЧАС НЕ ПОКУПАТЬ: риск позднего входа %d/100. Цена находится высоко в суточном диапазоне и/или импульс уже прошёл. Ждём новый вход снизу.",
                    breathing.lateEntryRisk
                ),
                rsiNow,
                emaNow,
                rate,
                candle.closeTime,
                trendReadiness.coerceAtMost(70),
                shockReadiness.coerceAtMost(70),
                marketGateBlockedEntry = entryWouldBeActive,
                lateEntryBlocked = true,
                breathing = breathing
            )
        }
        val marketGate = if (entryWouldBeActive || max(trendReadiness, shockReadiness) >= 95) {
            marketOverheatGateAt(index, pump, btc, sol)
        } else {
            MarketOverheatGate(false)
        }

        if (marketGate.active) {
            return V2EntrySignal(
                MODE_NONE,
                String.format(
                    Locale.US,
                    "ПАУЗА ПОКУПКИ: за 1 час одновременно резко выросли PUMP %+.1f%%, BTC %+.1f%% и SOL %+.1f%%. Не догоняем цену; ждем следующую закрытую свечу и новый безопасный вход.",
                    marketGate.pumpReturn * 100.0,
                    marketGate.btcReturn * 100.0,
                    marketGate.solReturn * 100.0
                ),
                rsiNow,
                emaNow,
                rate,
                candle.closeTime,
                trendReadiness.coerceAtMost(90),
                shockReadiness.coerceAtMost(90),
                marketGateActive = true,
                marketGateBlockedEntry = entryWouldBeActive,
                breathing = breathing
            )
        }

        return when {
            exhaustion -> V2EntrySignal(
                MODE_EXHAUSTION,
                "ПОКУПКА: 3 падения подтверждены разворотом, spot/futures-покупателями и безопасным режимом BTC+ETH+SOL",
                rsiNow, emaNow, rate, candle.closeTime, trendReadiness, 100,
                breathing = breathing
            )
            baseShock -> V2EntrySignal(
                MODE_SHOCK,
                "ПОКУПКА: базовый импульс после сильного падения; PUMP вернулся выше EMA200, BTC не слабый",
                rsiNow, emaNow, rate, candle.closeTime, trendReadiness, 100,
                breathing = breathing
            )
            trend -> V2EntrySignal(
                MODE_TREND,
                "ПОКУПКА: RSI восстановился, PUMP и BTC подтвердили восходящий режим",
                rsiNow, emaNow, rate, candle.closeTime, 100, shockReadiness,
                breathing = breathing
            )
            else -> V2EntrySignal(
                MODE_NONE,
                waitingReason(
                    rsiNow, extension, recentReturn6, trendReadiness, shockReadiness,
                    exhaustionArmed, priceConfirmation, flowReady, riskReady,
                    indicators.drawdown36h[index], currentDrawdownLimit
                ),
                rsiNow, emaNow, rate, candle.closeTime, trendReadiness, shockReadiness,
                breathing = breathing
            )
        }
    }

    /**
     * Rare late-entry veto found in the historical research.  The current closed
     * 30-minute candle is compared with one-hour return and volume distributions
     * built only from earlier candles.  No future candle participates.
     */
    internal fun marketOverheatGateAt(
        index: Int,
        pump: List<PumpCandle>,
        btc: List<PumpCandle>,
        sol: List<PumpCandle>
    ): MarketOverheatGate {
        if (index < MARKET_GATE_MIN_HISTORY + MARKET_GATE_HORIZON_BARS) {
            return MarketOverheatGate(false)
        }
        val btcByTime = btc.associateBy { it.closeTime }
        val solByTime = sol.associateBy { it.closeTime }
        val start = max(MARKET_GATE_HORIZON_BARS, index - MARKET_GATE_LOOKBACK_BARS)
        val pumpReturns = ArrayList<Double>()
        val btcReturns = ArrayList<Double>()
        val solReturns = ArrayList<Double>()
        val pumpVolumes = ArrayList<Double>()
        val btcVolumes = ArrayList<Double>()
        val solVolumes = ArrayList<Double>()

        for (sampleIndex in start until index) {
            val sample = alignedHourSample(sampleIndex, pump, btcByTime, solByTime) ?: continue
            pumpReturns += sample.pumpReturn
            btcReturns += sample.btcReturn
            solReturns += sample.solReturn
            pumpVolumes += sample.pumpVolume
            btcVolumes += sample.btcVolume
            solVolumes += sample.solVolume
        }
        if (pumpReturns.size < MARKET_GATE_MIN_HISTORY) return MarketOverheatGate(false)
        val current = alignedHourSample(index, pump, btcByTime, solByTime)
            ?: return MarketOverheatGate(false)
        val pumpHot = isHotAsset(current.pumpReturn, current.pumpVolume, pumpReturns, pumpVolumes)
        val btcHot = isHotAsset(current.btcReturn, current.btcVolume, btcReturns, btcVolumes)
        val solHot = isHotAsset(current.solReturn, current.solVolume, solReturns, solVolumes)
        return MarketOverheatGate(
            active = pumpHot && btcHot && solHot,
            pumpReturn = current.pumpReturn,
            btcReturn = current.btcReturn,
            solReturn = current.solReturn,
            historySamples = pumpReturns.size
        )
    }

    private data class AlignedHourSample(
        val pumpReturn: Double,
        val btcReturn: Double,
        val solReturn: Double,
        val pumpVolume: Double,
        val btcVolume: Double,
        val solVolume: Double
    )

    private fun alignedHourSample(
        index: Int,
        pump: List<PumpCandle>,
        btcByTime: Map<Long, PumpCandle>,
        solByTime: Map<Long, PumpCandle>
    ): AlignedHourSample? {
        if (index < MARKET_GATE_HORIZON_BARS) return null
        val currentPump = pump.getOrNull(index) ?: return null
        val previousPump = pump.getOrNull(index - 1) ?: return null
        val oldPump = pump.getOrNull(index - MARKET_GATE_HORIZON_BARS) ?: return null
        if (oldPump.close <= 0.0) return null
        val currentBtc = btcByTime[currentPump.closeTime] ?: return null
        val previousBtc = btcByTime[previousPump.closeTime] ?: return null
        val oldBtc = btcByTime[oldPump.closeTime] ?: return null
        val currentSol = solByTime[currentPump.closeTime] ?: return null
        val previousSol = solByTime[previousPump.closeTime] ?: return null
        val oldSol = solByTime[oldPump.closeTime] ?: return null
        if (oldBtc.close <= 0.0 || oldSol.close <= 0.0) return null
        return AlignedHourSample(
            pumpReturn = currentPump.close / oldPump.close - 1.0,
            btcReturn = currentBtc.close / oldBtc.close - 1.0,
            solReturn = currentSol.close / oldSol.close - 1.0,
            pumpVolume = currentPump.volume + previousPump.volume,
            btcVolume = currentBtc.volume + previousBtc.volume,
            solVolume = currentSol.volume + previousSol.volume
        )
    }

    private fun isHotAsset(
        currentReturn: Double,
        currentVolume: Double,
        historicalReturns: List<Double>,
        historicalVolumes: List<Double>
    ): Boolean {
        if (currentReturn <= 0.0 || currentVolume <= 0.0) return false
        val returnThreshold = percentile(historicalReturns, MARKET_GATE_RETURN_QUANTILE)
        val volumeMedian = percentile(historicalVolumes, 0.50)
        return currentReturn >= returnThreshold &&
            volumeMedian > 0.0 && currentVolume >= MARKET_GATE_VOLUME_FLOOR * volumeMedian
    }

    private fun percentile(values: List<Double>, quantile: Double): Double {
        if (values.isEmpty()) return Double.NaN
        val sorted = values.sorted()
        val position = (sorted.size - 1) * quantile.coerceIn(0.0, 1.0)
        val lower = kotlin.math.floor(position).toInt()
        val upper = kotlin.math.ceil(position).toInt()
        if (lower == upper) return sorted[lower]
        val fraction = position - lower
        return sorted[lower] * (1.0 - fraction) + sorted[upper] * fraction
    }

    private fun baselineReadiness(
        active: Boolean,
        armed: Boolean,
        rsiNow: Double,
        rsiPrevious: Double,
        extension: Double,
        btcAbove: Boolean,
        btcSlope: Boolean,
        fundingReady: Boolean,
        noChase: Boolean
    ): Int {
        if (active) return 100
        var raw = if (armed) 20 else 0
        raw += when {
            rsiNow in 45.0..55.0 && rsiPrevious < 45.0 -> 20
            rsiNow in 42.0..45.0 && rsiNow > rsiPrevious -> 16
            rsiNow in 35.0..55.0 -> 8
            else -> 0
        }
        raw += if (extension in 0.0..0.035) 20 else if (extension in -0.01..0.04) 10 else 0
        raw += if (btcAbove) 15 else 0
        raw += if (btcSlope) 10 else 0
        raw += if (fundingReady) 5 else 0
        raw += if (noChase) 10 else 0
        val preparing = armed && rsiNow in 42.0..45.0 && rsiNow > rsiPrevious &&
            extension in -0.01..0.035 && btcAbove && btcSlope && fundingReady && noChase
        return if (preparing) {
            95 + (((rsiNow - 42.0) / 3.0) * 4.0).roundToInt().coerceIn(0, 4)
        } else minOf(raw, 94)
    }

    private fun exhaustionReadiness(
        active: Boolean,
        armed: Boolean,
        priceConfirmation: Boolean,
        rsiNow: Double,
        flowReady: Boolean,
        spotFlow: Double?,
        futuresFlow: Double?,
        relativeSlope: Double?,
        riskReady: Boolean,
        currentDrawdown: Double,
        currentDrawdownLimit: Double
    ): Int {
        if (active) return 100
        var raw = if (armed) 30 else 0
        raw += if (priceConfirmation && rsiNow in 43.0..58.0) 25 else if (rsiNow in 40.0..58.0) 12 else 0
        raw += if (flowReady) 20 else {
            var partial = 0
            if ((spotFlow ?: -1.0) >= 0.0) partial += 6
            if ((futuresFlow ?: -1.0) >= 0.02) partial += 6
            if ((relativeSlope ?: -1.0) >= 0.0) partial += 5
            partial
        }
        raw += if (riskReady) 15 else 0
        raw += if (currentDrawdown <= currentDrawdownLimit) 10 else 0

        val nearlyReady = armed && rsiNow in 43.0..58.0 && riskReady && currentDrawdown <= currentDrawdownLimit
        if (nearlyReady) {
            val futuresNear = futuresFlow != null && futuresFlow >= 0.02 && futuresFlow < 0.03
            val onlyFlowShort = priceConfirmation && (spotFlow ?: -1.0) >= 0.0 &&
                futuresNear && (relativeSlope ?: -1.0) >= 0.0
            if (onlyFlowShort) return 99
            val onlyPriceShort = !priceConfirmation && flowReady
            if (onlyPriceShort) return 98
            if (flowReady || priceConfirmation) return 96
            return 95
        }
        return minOf(raw, 94)
    }

    private fun waitingReason(
        rsi: Double,
        extension: Double,
        recentReturn6: Double,
        trendReadiness: Int,
        exhaustionReadiness: Int,
        armed: Boolean,
        priceReady: Boolean,
        flowReady: Boolean,
        riskReady: Boolean,
        currentDrawdown: Double,
        limit: Double
    ): String {
        if (extension > 0.04 || rsi > 58.0 || recentReturn6 >= 0.08) {
            return String.format(
                Locale.US,
                "СЕЙЧАС НЕ ПОКУПАТЬ: цена уже разогрета (RSI %.1f; 3 часа %+.1f%%; EMA200 %+.1f%%). Ждем новый вход снизу.",
                rsi, recentReturn6 * 100.0, extension * 100.0
            )
        }
        val stage = when {
            !armed -> "1/4: ждем три отдельных падения"
            currentDrawdown > limit -> "1/4 выполнен, но цена уже слишком далеко от дна"
            !priceReady -> "2/4: ждем подтвержденный разворот"
            !flowReady -> "3/4: ждем перевес покупателей spot и futures"
            !riskReady -> "4/4: общий рынок пока небезопасен"
            else -> "условия почти собраны"
        }
        return String.format(
            Locale.US,
            "Покупка не подтверждена. Базовый %d/100; 4-этапный %d/100. %s. RSI %.1f.",
            trendReadiness, exhaustionReadiness, stage, rsi
        )
    }

    private fun rollingImbalance(candles: List<PumpCandle>): List<Double?> {
        return candles.indices.map { rollingImbalanceAt(candles, it) }
    }

    private fun rollingImbalanceAt(candles: List<PumpCandle>, index: Int): Double? {
        if (index < 2) return null
        val values = candles.subList(index - 2, index + 1).mapNotNull { candle ->
            if (candle.volume > 0.0 && candle.takerBuyVolume >= 0.0) 2.0 * candle.takerBuyVolume / candle.volume - 1.0 else null
        }
        return if (values.size == 3) values.average() else null
    }

    private fun pairedReturn(byTime: Map<Long, PumpCandle>, oldTime: Long, currentTime: Long): Double? {
        val old = byTime[oldTime]?.close ?: return null
        val current = byTime[currentTime]?.close ?: return null
        return if (old > 0.0) current / old - 1.0 else null
    }

    private fun returns(values: List<Double>): List<Double> {
        return values.mapIndexed { index, value -> if (index == 0 || values[index - 1] <= 0.0) 0.0 else value / values[index - 1] - 1.0 }
    }

    private fun fundingRateAt(points: List<FundingPoint>, time: Long): Double {
        var answer: FundingPoint? = null
        for (point in points) {
            if (point.time > time) break
            answer = point
        }
        return answer?.rate ?: Double.POSITIVE_INFINITY
    }

    private fun ema(values: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(values.size) { null }
        if (values.size < period) return result
        var previous = values.take(period).average()
        result[period - 1] = previous
        val multiplier = 2.0 / (period + 1.0)
        for (i in period until values.size) {
            previous = values[i] * multiplier + previous * (1.0 - multiplier)
            result[i] = previous
        }
        return result
    }

    private fun rsi(values: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(values.size) { null }
        var averageGain = 0.0
        var averageLoss = 0.0
        for (i in 1 until values.size) {
            val change = values[i] - values[i - 1]
            val gain = max(change, 0.0)
            val loss = max(-change, 0.0)
            if (i <= period) {
                averageGain += gain
                averageLoss += loss
                if (i == period) {
                    averageGain /= period
                    averageLoss /= period
                }
            } else {
                averageGain = (averageGain * (period - 1) + gain) / period
                averageLoss = (averageLoss * (period - 1) + loss) / period
            }
            if (i >= period) result[i] = if (averageLoss == 0.0) 100.0 else 100.0 - 100.0 / (1.0 + averageGain / averageLoss)
        }
        return result
    }
}
