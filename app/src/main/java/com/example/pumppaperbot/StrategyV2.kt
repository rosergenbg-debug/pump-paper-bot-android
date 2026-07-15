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
    val shockReadiness: Int = 0
) {
    val active: Boolean get() = mode == StrategyV2.MODE_TREND || mode == StrategyV2.MODE_SHOCK
}

data class V2ExitSignal(
    val action: String,
    val reason: String,
    val highestHigh: Double,
    val readiness: Int = 0
)

object StrategyV2 {
    const val MODE_NONE = "NONE"
    const val MODE_TREND = "TREND"
    const val MODE_SHOCK = "SHOCK"
    const val ACTION_WAIT = "WAIT"
    const val ACTION_SELL = "SELL"
    const val ACTION_SELL_HALF = "SELL_HALF"

    const val PRICE_STOP = 0.044
    const val TREND_TARGET = 0.08
    const val SHOCK_FIRST_TARGET = 0.06
    const val RUNNER_TRAIL = 0.04
    const val MAX_HOLD_BARS = 48
    const val MAX_HOLD_MILLIS = 24L * 60L * 60L * 1000L

    private const val RSI_PERIOD = 14
    private const val EMA_FAST = 50
    private const val EMA_SLOW = 200
    private const val TREND_ARM_BARS = 24
    private const val SHOCK_ARM_BARS = 36

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
                closeTime = coin.closeTime
            )
        }.sortedBy { it.closeTime }
    }

    fun latestEntrySignal(
        pumpEur: List<PumpCandle>,
        btcUsdt: List<PumpCandle>,
        funding: List<FundingPoint>,
        aggressive: Boolean = true
    ): V2EntrySignal {
        if (pumpEur.size < EMA_SLOW + SHOCK_ARM_BARS || btcUsdt.size < EMA_SLOW + 7) {
            return V2EntrySignal(MODE_NONE, "Ждем достаточно 30-минутных свечей", 0.0, 0.0, 0.0, 0L)
        }
        val indicators = indicators(pumpEur, btcUsdt)
        return entrySignalAt(pumpEur.lastIndex, pumpEur, funding, indicators, aggressive)
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
        val highestBefore = max(storedHighestHigh, entryPrice)
        val highestAfter = max(highestBefore, candle.high)
        val hardStopPrice = entryPrice * (1.0 - PRICE_STOP)
        val timedOut = candle.closeTime - entryTime >= MAX_HOLD_MILLIS
        val timeReadiness = ((candle.closeTime - entryTime).toDouble() / MAX_HOLD_MILLIS * 100.0)
            .roundToInt().coerceIn(0, 99)
        val stopReadiness = ((entryPrice - candle.close) / (entryPrice * PRICE_STOP) * 100.0)
            .roundToInt().coerceIn(0, 99)

        if (positionMode == MODE_SHOCK) {
            if (!partialTaken) {
                return when {
                    candle.low <= hardStopPrice -> V2ExitSignal(
                        ACTION_SELL,
                        String.format(Locale.US, "СТОП: цена коснулась %.8f (−4,4%%)", hardStopPrice),
                        highestAfter
                    )
                    candle.high >= entryPrice * (1.0 + SHOCK_FIRST_TARGET) -> V2ExitSignal(
                        ACTION_SELL_HALF,
                        "ШОК: зафиксировать 50% при +6%, остаток оставить с трейлингом 4%",
                        highestAfter
                    )
                    timedOut -> V2ExitSignal(ACTION_SELL, "ШОК: прошло 24 часа — закрыть позицию", highestAfter)
                    else -> {
                        val targetReadiness = ((candle.close / entryPrice - 1.0) / SHOCK_FIRST_TARGET * 100.0)
                            .roundToInt().coerceIn(0, 99)
                        val readiness = max(max(targetReadiness, stopReadiness), timeReadiness)
                        V2ExitSignal(ACTION_WAIT, "АГРЕССИВНЫЙ: ждем +6% или защитный стоп", highestAfter, readiness)
                    }
                }
            }
            val runnerStop = max(entryPrice, highestBefore * (1.0 - RUNNER_TRAIL))
            return when {
                candle.low <= runnerStop -> V2ExitSignal(
                    ACTION_SELL,
                    String.format(Locale.US, "ОСТАТОК: трейлинг/безубыток %.8f", runnerStop),
                    highestAfter
                )
                timedOut -> V2ExitSignal(ACTION_SELL, "ОСТАТОК: прошло 24 часа — закрыть", highestAfter)
                else -> {
                    val runnerRange = max(highestBefore - runnerStop, entryPrice * 0.001)
                    val runnerReadiness = ((highestBefore - candle.close) / runnerRange * 100.0)
                        .roundToInt().coerceIn(0, 99)
                    V2ExitSignal(
                        ACTION_WAIT,
                        "ОСТАТОК 50%: действует трейлинг 4%",
                        highestAfter,
                        max(runnerReadiness, timeReadiness)
                    )
                }
            }
        }

        return when {
            candle.low <= hardStopPrice -> V2ExitSignal(
                ACTION_SELL,
                String.format(Locale.US, "СТОП: цена коснулась %.8f (−4,4%%)", hardStopPrice),
                highestAfter
            )
            candle.high >= entryPrice * (1.0 + TREND_TARGET) -> V2ExitSignal(
                ACTION_SELL,
                "ТРЕНД: цель +8% достигнута — закрыть полностью",
                highestAfter
            )
            timedOut -> V2ExitSignal(ACTION_SELL, "ТРЕНД: прошло 24 часа — закрыть позицию", highestAfter)
            else -> {
                val targetReadiness = ((candle.close / entryPrice - 1.0) / TREND_TARGET * 100.0)
                    .roundToInt().coerceIn(0, 99)
                val readiness = max(max(targetReadiness, stopReadiness), timeReadiness)
                V2ExitSignal(ACTION_WAIT, "ОСТОРОЖНЫЙ: ждем +8% или защитный стоп", highestAfter, readiness)
            }
        }
    }

    fun backtest(
        pumpEur: List<PumpCandle>,
        btcUsdt: List<PumpCandle>,
        funding: List<FundingPoint>,
        startTime: Long,
        aggressive: Boolean = true
    ): BacktestResult {
        if (pumpEur.isEmpty()) return BacktestResult.empty(startTime)
        val indicators = indicators(pumpEur, btcUsdt)
        var cash = PumpBotEngine.startBalance
        val trades = ArrayList<TradeEvent>()
        val closedEquity = arrayListOf(cash)
        var wins = 0
        var stops = 0
        var roundTrips = 0
        var i = max(EMA_SLOW + SHOCK_ARM_BARS, RSI_PERIOD + 2)

        while (i < pumpEur.lastIndex) {
            val candle = pumpEur[i]
            if (candle.closeTime < startTime) {
                i++
                continue
            }
            val signal = entrySignalAt(i, pumpEur, funding, indicators, aggressive)
            if (!signal.active) {
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
            trades += TradeEvent(
                time = entryCandle.openTime,
                action = "BUY",
                price = entryPrice,
                amount = initialCash,
                fee = buyFee,
                equity = coins * entryPrice,
                pnl = 0.0,
                coins = coins,
                reason = if (signal.mode == MODE_SHOCK) "Агрессивный вход после шока" else "Осторожный трендовый вход"
            )

            var partialTaken = false
            var highestAfterPartial = entryPrice
            var exitIndex = minOf(entryIndex + MAX_HOLD_BARS, pumpEur.lastIndex)
            var exitReason = "24 часа"
            var j = entryIndex
            while (j <= exitIndex) {
                val future = pumpEur[j]
                val hardStop = entryPrice * (1.0 - PRICE_STOP)
                if (!partialTaken) {
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
                    val target = if (signal.mode == MODE_SHOCK) SHOCK_FIRST_TARGET else TREND_TARGET
                    if (future.high >= entryPrice * (1.0 + target)) {
                        val sellPrice = entryPrice * (1.0 + target) * (1.0 - PumpBotEngine.slippage)
                        if (signal.mode == MODE_TREND) {
                            val gross = coins * sellPrice
                            val fee = gross * PumpBotEngine.feeRate
                            cash += gross - fee
                            trades += TradeEvent(future.closeTime, "SELL", sellPrice, gross, fee, cash, cash - initialCash, 0.0, "Цель +8%")
                            coins = 0.0
                            exitIndex = j
                            exitReason = "target"
                            break
                        }
                        val soldCoins = coins * 0.5
                        val gross = soldCoins * sellPrice
                        val fee = gross * PumpBotEngine.feeRate
                        cash += gross - fee
                        coins -= soldCoins
                        trades += TradeEvent(
                            future.closeTime,
                            "SELL_HALF",
                            sellPrice,
                            gross,
                            fee,
                            cash + coins * sellPrice,
                            cash + coins * sellPrice - initialCash,
                            coins,
                            "50% при +6%"
                        )
                        partialTaken = true
                        highestAfterPartial = max(entryPrice * (1.0 + SHOCK_FIRST_TARGET), future.high)
                        j++
                        continue
                    }
                } else {
                    val runnerStop = max(entryPrice, highestAfterPartial * (1.0 - RUNNER_TRAIL))
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
            strategyName = if (aggressive) "Агрессивный: тренд + шок" else "Осторожный: только тренд",
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
            stopCount = stops
        )
    }

    private data class Indicators(
        val rsi: List<Double?>,
        val ema200: List<Double?>,
        val ret1: List<Double>,
        val volumeRatio: List<Double>,
        val btcAbove200: Map<Long, Boolean>,
        val btcSlopePositive: Map<Long, Boolean>
    )

    private fun indicators(pump: List<PumpCandle>, btc: List<PumpCandle>): Indicators {
        val closes = pump.map { it.close }
        val rsi = rsi(closes, RSI_PERIOD)
        val ema200 = ema(closes, EMA_SLOW)
        val ret1 = closes.mapIndexed { index, value -> if (index == 0 || closes[index - 1] <= 0.0) 0.0 else value / closes[index - 1] - 1.0 }
        val volumeRatio = pump.mapIndexed { index, candle ->
            if (index < 19) 0.0 else {
                val window = pump.subList(index - 19, index + 1).map { it.volume }.sorted()
                val median = (window[9] + window[10]) / 2.0
                if (median > 0.0) candle.volume / median else 0.0
            }
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
        return Indicators(rsi, ema200, ret1, volumeRatio, btcAbove, btcSlope)
    }

    private fun entrySignalAt(
        index: Int,
        pump: List<PumpCandle>,
        funding: List<FundingPoint>,
        indicators: Indicators,
        aggressive: Boolean
    ): V2EntrySignal {
        val candle = pump.getOrNull(index) ?: return V2EntrySignal(MODE_NONE, "Нет свечи", 0.0, 0.0, 0.0, 0L)
        val rsiNow = indicators.rsi.getOrNull(index) ?: 0.0
        val rsiPrevious = indicators.rsi.getOrNull(index - 1) ?: 0.0
        val emaNow = indicators.ema200.getOrNull(index) ?: 0.0
        val btcAbove = indicators.btcAbove200[candle.closeTime] == true
        val btcSlope = indicators.btcSlopePositive[candle.closeTime] == true
        val rate = fundingRateAt(funding, candle.closeTime)
        val extension = if (emaNow > 0.0) candle.close / emaNow - 1.0 else Double.POSITIVE_INFINITY
        val recentReturn6 = pump.getOrNull(index - 6)?.close?.takeIf { it > 0.0 }
            ?.let { candle.close / it - 1.0 } ?: 0.0
        val noChase = indicators.ret1.getOrElse(index) { 0.0 } < 0.04 &&
            recentReturn6 < 0.08 && extension <= 0.035

        var trendArmed = false
        for (j in max(0, index - TREND_ARM_BARS + 1)..index) {
            if ((indicators.rsi.getOrNull(j) ?: 100.0) <= 40.0) trendArmed = true
        }
        val rsiCrossedNow = rsiNow in 45.0..55.0 && rsiPrevious < 45.0
        val priceReady = extension in 0.0..0.035
        val trend = trendArmed && rsiCrossedNow && priceReady &&
            btcAbove && btcSlope && rate <= 0.0 && noChase

        val rsiApproach = when {
            rsiNow > 55.0 -> 0
            rsiNow >= 45.0 -> if (rsiPrevious < 45.0) 20 else 8
            rsiNow >= 35.0 && rsiNow > rsiPrevious ->
                (((rsiNow - 35.0) / 10.0) * 20.0).roundToInt().coerceIn(0, 20)
            rsiNow >= 35.0 -> (((rsiNow - 35.0) / 10.0) * 10.0).roundToInt().coerceIn(0, 10)
            else -> 0
        }
        val priceApproach = when {
            extension > 0.035 -> 0
            extension >= 0.0 -> 20
            extension >= -0.02 -> (((extension + 0.02) / 0.02) * 20.0).roundToInt().coerceIn(0, 20)
            else -> 0
        }

        var shockArmed = false
        for (j in max(1, index - SHOCK_ARM_BARS + 1)..index) {
            if (indicators.ret1[j] <= -0.03 && indicators.volumeRatio[j] >= 3.0 &&
                (indicators.rsi.getOrNull(j) ?: 100.0) <= 40.0
            ) shockArmed = true
        }
        val ready = rsiNow in 45.0..55.0 && priceReady
        val previousEma = indicators.ema200.getOrNull(index - 1) ?: 0.0
        val previousClose = pump.getOrNull(index - 1)?.close ?: 0.0
        val previousExtension = if (previousEma > 0.0) previousClose / previousEma - 1.0 else Double.POSITIVE_INFINITY
        val previousReady = rsiPrevious in 45.0..55.0 && previousExtension in 0.0..0.035
        val shock = shockArmed && ready && !previousReady && btcAbove && noChase

        val trendRaw = (if (trendArmed) 20 else 0) + rsiApproach + priceApproach +
            (if (btcAbove) 15 else 0) + (if (btcSlope) 10 else 0) +
            (if (rate <= 0.0) 5 else 0) + (if (noChase) 10 else 0)
        val trendPreparing = trendArmed && !trend && rsiNow >= 42.0 && rsiNow < 45.0 &&
            rsiNow > rsiPrevious && extension in -0.01..0.035 &&
            btcAbove && btcSlope && rate <= 0.0 && noChase
        val trendReadiness = when {
            trend -> 100
            trendPreparing -> 95 + (((rsiNow - 42.0) / 3.0) * 4.0).roundToInt().coerceIn(0, 4)
            else -> minOf(trendRaw, 94)
        }

        val shockRaw = if (aggressive) {
            (if (shockArmed) 25 else 0) + rsiApproach + priceApproach +
                (if (btcAbove) 15 else 0) + (if (noChase) 20 else 0)
        } else 0
        val shockPreparing = aggressive && shockArmed && !shock && !previousReady &&
            rsiNow in 42.0..55.0 && extension in -0.01..0.035 &&
            btcAbove && noChase && (rsiNow < 45.0 || extension < 0.0)
        val shockReadiness = when {
            !aggressive -> 0
            shock -> 100
            shockPreparing -> {
                val rsiProgress = (((rsiNow.coerceAtMost(45.0) - 42.0) / 3.0) * 4.0).roundToInt()
                val priceProgress = (((extension + 0.01) / 0.01) * 4.0).roundToInt()
                95 + max(rsiProgress, priceProgress).coerceIn(0, 4)
            }
            else -> minOf(shockRaw, 94)
        }

        return when {
            aggressive && shock -> V2EntrySignal(
                MODE_SHOCK,
                "ПОКУПКА АГРЕССИВНАЯ: падение ≥3% на объеме ≥3x, затем возврат выше EMA200; BTC выше EMA200",
                rsiNow,
                emaNow,
                rate,
                candle.closeTime,
                trendReadiness,
                100
            )
            trend -> V2EntrySignal(
                MODE_TREND,
                "ПОКУПКА ОСТОРОЖНАЯ: RSI восстановился выше 45; PUMP и BTC в восходящем режиме; funding ≤ 0",
                rsiNow,
                emaNow,
                rate,
                candle.closeTime,
                100,
                shockReadiness
            )
            else -> V2EntrySignal(
                MODE_NONE,
                when {
                    extension > 0.035 -> String.format(
                        Locale.US,
                        "СЕЙЧАС НЕ ПОКУПАТЬ: цена уже на %.1f%% выше EMA200. Ждем откат и новое подтверждение снизу.",
                        extension * 100.0
                    )
                    rsiNow > 55.0 -> String.format(
                        Locale.US,
                        "СЕЙЧАС НЕ ПОКУПАТЬ: RSI %.1f слишком высокий. Ждем охлаждение рынка и новый вход снизу.",
                        rsiNow
                    )
                    recentReturn6 >= 0.08 -> String.format(
                        Locale.US,
                        "СЕЙЧАС НЕ ПОКУПАТЬ: за 3 часа цена уже выросла на %.1f%%. Не догоняем движение.",
                        recentReturn6 * 100.0
                    )
                    aggressive -> {
                    String.format(
                        Locale.US,
                        "Покупка не подтверждена. Осторожный %d/100; после шока %d/100. RSI %.1f.",
                        trendReadiness,
                        shockReadiness,
                        rsiNow
                    )
                    }
                    else -> {
                    String.format(
                        Locale.US,
                        "Покупка не подтверждена. Осторожный вход %d/100; RSI %.1f.",
                        trendReadiness,
                        rsiNow
                    )
                    }
                },
                rsiNow,
                emaNow,
                rate,
                candle.closeTime,
                trendReadiness,
                shockReadiness
            )
        }
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
