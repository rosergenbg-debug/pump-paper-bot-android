package com.example.pumppaperbot

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tanh

data class OrderBookMetrics(
    val imbalance: Double,
    val spreadPercent: Double,
    val bidNotional: Double,
    val askNotional: Double
)

data class MarketBreathingSnapshot(
    val state: String,
    val energyScore: Int,
    val compressionScore: Int,
    val directionScore: Int,
    val confidenceScore: Int,
    val lateEntryRisk: Int,
    val marketRelation: String,
    val explanation: String,
    val bookImbalance: Double? = null,
    val spreadPercent: Double? = null,
    val openInterest: Double? = null,
    val openInterestChangePercent: Double? = null
) {
    companion object {
        fun unknown(lateEntryRisk: Int = 0) = MarketBreathingSnapshot(
            state = "НЕДОСТАТОЧНО ДАННЫХ",
            energyScore = 0,
            compressionScore = 0,
            directionScore = 0,
            confidenceScore = 0,
            lateEntryRisk = lateEntryRisk,
            marketRelation = "Связь с BTC/SOL ещё не рассчитана",
            explanation = "Ждём достаточно закрытых свечей"
        )
    }
}

/**
 * Causal, descriptive market-state analysis.
 *
 * Energy and compression are trailing percentile ranks. Direction describes the
 * agreement of price and taker flow; it is not a probability of the next candle.
 * Every calculation at [index] uses candles with an index <= [index].
 */
object MarketBreathingAnalyzer {
    private const val HISTORY_BARS = 30 * 48
    private const val MIN_HISTORY = 96
    private const val HOUR_BARS = 2
    private const val SIX_HOUR_BARS = 12

    fun analyzeAt(
        index: Int,
        pump: List<PumpCandle>,
        btc: List<PumpCandle>,
        sol: List<PumpCandle>,
        futures: List<PumpCandle>,
        rsi: List<Double?>,
        ema20: List<Double?>,
        orderBook: OrderBookMetrics? = null,
        openInterest: Double? = null,
        openInterestChangePercent: Double? = null
    ): MarketBreathingSnapshot {
        val lateRisk = lateEntryRiskAt(index, pump, rsi, ema20)
        if (index < MIN_HISTORY || index !in pump.indices) {
            return MarketBreathingSnapshot.unknown(lateRisk)
        }

        val start = max(SIX_HOUR_BARS, index - HISTORY_BARS)
        val currentHour = hourMetricsAt(index, pump) ?: return MarketBreathingSnapshot.unknown(lateRisk)
        val currentActivity = activityAt(index, pump) ?: return MarketBreathingSnapshot.unknown(lateRisk)
        val hourAbsReturns = ArrayList<Double>()
        val hourRanges = ArrayList<Double>()
        val hourVolumes = ArrayList<Double>()
        val hourTrades = ArrayList<Double>()
        val activityRv = ArrayList<Double>()
        val activityRange = ArrayList<Double>()
        val activityVolume = ArrayList<Double>()
        for (sampleIndex in start until index) {
            hourMetricsAt(sampleIndex, pump)?.let { sample ->
                hourAbsReturns += sample.absoluteReturn
                hourRanges += sample.range
                hourVolumes += sample.volume
                if (sample.trades > 0.0) hourTrades += sample.trades
            }
            activityAt(sampleIndex, pump)?.let { sample ->
                activityRv += sample.realizedVolatility
                activityRange += sample.averageRange
                activityVolume += sample.volume
            }
        }
        if (hourAbsReturns.size < MIN_HISTORY || activityRv.size < MIN_HISTORY) {
            return MarketBreathingSnapshot.unknown(lateRisk)
        }

        val energyParts = arrayListOf(
            percentileRank(currentHour.absoluteReturn, hourAbsReturns),
            percentileRank(currentHour.range, hourRanges),
            percentileRank(currentHour.volume, hourVolumes)
        )
        if (currentHour.trades > 0.0 && hourTrades.size >= MIN_HISTORY / 2) {
            energyParts += percentileRank(currentHour.trades, hourTrades)
        }
        val energy = energyParts.average().roundToInt().coerceIn(0, 100)
        val activityRank = listOf(
            percentileRank(currentActivity.realizedVolatility, activityRv),
            percentileRank(currentActivity.averageRange, activityRange),
            percentileRank(currentActivity.volume, activityVolume)
        ).average()
        val compression = (100.0 - activityRank).roundToInt().coerceIn(0, 100)

        val components = ArrayList<WeightedComponent>()
        val spotFlow = rollingImbalance(pump, index)
        if (spotFlow != null) components += WeightedComponent(0.25, spotFlow.coerceIn(-1.0, 1.0))
        val futuresByTime = futures.associateBy { it.closeTime }
        val futuresFlow = rollingAlignedImbalance(pump, futuresByTime, index)
        if (futuresFlow != null) components += WeightedComponent(0.20, futuresFlow.coerceIn(-1.0, 1.0))
        val return1h = returnOver(index, HOUR_BARS, pump)
        val return3h = returnOver(index, 6, pump)
        components += WeightedComponent(0.20, tanh(return1h / 0.020))
        components += WeightedComponent(0.20, tanh(return3h / 0.040))
        val currentEma = ema20.getOrNull(index)
        val oldEma = ema20.getOrNull(index - 6)
        if (currentEma != null && oldEma != null && oldEma > 0.0) {
            components += WeightedComponent(0.15, tanh((currentEma / oldEma - 1.0) / 0.020))
        }
        if (orderBook != null) {
            // A single book snapshot can be spoofed, so it is deliberately a small
            // descriptive component and never the only reason to permit an entry.
            components += WeightedComponent(0.10, orderBook.imbalance.coerceIn(-1.0, 1.0))
        }
        val totalWeight = components.sumOf { it.weight }.coerceAtLeast(0.01)
        val direction = (components.sumOf { it.weight * it.value } / totalWeight * 100.0)
            .roundToInt().coerceIn(-100, 100)

        val decisive = components.filter { abs(it.value) >= 0.12 }
        val positiveWeight = decisive.filter { it.value > 0.0 }.sumOf { it.weight }
        val negativeWeight = decisive.filter { it.value < 0.0 }.sumOf { it.weight }
        val agreement = if (positiveWeight + negativeWeight > 0.0) {
            max(positiveWeight, negativeWeight) / (positiveWeight + negativeWeight)
        } else 0.0
        val completeness = (totalWeight / 1.10).coerceIn(0.0, 1.0)
        val clarity = max(max(energy, compression), abs(direction)) / 100.0
        val confidence = (40.0 * completeness + 35.0 * agreement + 25.0 * clarity)
            .roundToInt().coerceIn(0, 95)

        val relation = marketRelation(index, pump, btc, sol)
        val state = when {
            lateRisk >= 65 && direction > 10 -> "ПОЗДНИЙ ВХОД — НЕ ДОГОНЯТЬ"
            energy >= 78 && direction >= 30 -> "УСКОРЕННЫЙ ВЫДОХ ВВЕРХ"
            energy >= 65 && direction >= 18 -> "НАЧАЛО ВЫДОХА ВВЕРХ"
            energy >= 78 && direction <= -30 -> "УСКОРЕННЫЙ ВЫДОХ ВНИЗ"
            energy >= 65 && direction <= -18 -> "НАЧАЛО ВЫДОХА ВНИЗ"
            compression >= 72 -> "СЖАТИЕ / ЗАДЕРЖКА"
            energy <= 35 && abs(direction) <= 20 -> "СПОКОЙНОЕ ДЫХАНИЕ"
            abs(direction) <= 15 -> "НЕОПРЕДЕЛЁННОЕ ДЫХАНИЕ"
            else -> "ПЕРЕХОДНОЕ СОСТОЯНИЕ"
        }
        val explanation = String.format(
            Locale.GERMAN,
            "Активность %d/100, сжатие %d/100, поток %+d/100, риск позднего входа %d/100. %s",
            energy, compression, direction, lateRisk, relation
        )
        return MarketBreathingSnapshot(
            state = state,
            energyScore = energy,
            compressionScore = compression,
            directionScore = direction,
            confidenceScore = confidence,
            lateEntryRisk = lateRisk,
            marketRelation = relation,
            explanation = explanation,
            bookImbalance = orderBook?.imbalance,
            spreadPercent = orderBook?.spreadPercent,
            openInterest = openInterest,
            openInterestChangePercent = openInterestChangePercent
        )
    }

    fun riskOnlyAt(
        index: Int,
        pump: List<PumpCandle>,
        rsi: List<Double?>,
        ema20: List<Double?>
    ): MarketBreathingSnapshot = MarketBreathingSnapshot.unknown(lateEntryRiskAt(index, pump, rsi, ema20))

    internal fun lateEntryRiskAt(
        index: Int,
        pump: List<PumpCandle>,
        rsi: List<Double?>,
        ema20: List<Double?>
    ): Int {
        val candle = pump.getOrNull(index) ?: return 0
        if (index < 6 || candle.close <= 0.0) return 0
        val start = max(0, index - 47)
        val recent = pump.subList(start, index + 1)
        val low = recent.minOf { it.low }
        val high = recent.maxOf { it.high }
        val location = if (high > low) ((candle.close - low) / (high - low)).coerceIn(0.0, 1.0) else 0.5
        val locationRisk = ((location - 0.50) / 0.50).coerceIn(0.0, 1.0)
        val rsiRisk = (((rsi.getOrNull(index) ?: 50.0) - 50.0) / 18.0).coerceIn(0.0, 1.0)
        val return3hRisk = (returnOver(index, 6, pump) / 0.070).coerceIn(0.0, 1.0)
        val return1hRisk = (returnOver(index, HOUR_BARS, pump) / 0.040).coerceIn(0.0, 1.0)
        val ema = ema20.getOrNull(index)
        val emaRisk = if (ema != null && ema > 0.0) {
            ((candle.close / ema - 1.0) / 0.035).coerceIn(0.0, 1.0)
        } else 0.0
        return (100.0 * (
            0.30 * locationRisk +
                0.20 * rsiRisk +
                0.20 * return3hRisk +
                0.15 * return1hRisk +
                0.15 * emaRisk
            )).roundToInt().coerceIn(0, 100)
    }

    private data class HourMetrics(
        val absoluteReturn: Double,
        val range: Double,
        val volume: Double,
        val trades: Double
    )

    private data class ActivityMetrics(
        val realizedVolatility: Double,
        val averageRange: Double,
        val volume: Double
    )

    private data class WeightedComponent(val weight: Double, val value: Double)

    private fun hourMetricsAt(index: Int, candles: List<PumpCandle>): HourMetrics? {
        if (index < HOUR_BARS || index !in candles.indices) return null
        val old = candles[index - HOUR_BARS]
        val pair = candles.subList(index - HOUR_BARS + 1, index + 1)
        if (old.close <= 0.0 || pair.first().open <= 0.0) return null
        return HourMetrics(
            absoluteReturn = abs(candles[index].close / old.close - 1.0),
            range = (pair.maxOf { it.high } - pair.minOf { it.low }) / pair.first().open,
            volume = pair.sumOf { it.volume },
            trades = pair.sumOf { it.tradeCount }.toDouble()
        )
    }

    private fun activityAt(index: Int, candles: List<PumpCandle>): ActivityMetrics? {
        if (index < SIX_HOUR_BARS || index !in candles.indices) return null
        var squared = 0.0
        var ranges = 0.0
        var volume = 0.0
        for (i in index - SIX_HOUR_BARS + 1..index) {
            val previous = candles.getOrNull(i - 1) ?: return null
            val candle = candles[i]
            if (previous.close <= 0.0 || candle.open <= 0.0) return null
            val change = candle.close / previous.close - 1.0
            squared += change * change
            ranges += (candle.high - candle.low) / candle.open
            volume += candle.volume
        }
        return ActivityMetrics(sqrt(squared), ranges / SIX_HOUR_BARS, volume)
    }

    private fun rollingImbalance(candles: List<PumpCandle>, index: Int): Double? {
        if (index < 2) return null
        val values = candles.subList(index - 2, index + 1).mapNotNull { candle ->
            if (candle.volume > 0.0 && candle.takerBuyVolume >= 0.0) {
                2.0 * candle.takerBuyVolume / candle.volume - 1.0
            } else null
        }
        return if (values.size == 3) values.average() else null
    }

    private fun rollingAlignedImbalance(
        pump: List<PumpCandle>,
        futuresByTime: Map<Long, PumpCandle>,
        index: Int
    ): Double? {
        if (index < 2) return null
        val values = (index - 2..index).mapNotNull { pumpIndex ->
            val candle = futuresByTime[pump[pumpIndex].closeTime] ?: return@mapNotNull null
            if (candle.volume > 0.0 && candle.takerBuyVolume >= 0.0) {
                2.0 * candle.takerBuyVolume / candle.volume - 1.0
            } else null
        }
        return if (values.size == 3) values.average() else null
    }

    private fun marketRelation(
        index: Int,
        pump: List<PumpCandle>,
        btc: List<PumpCandle>,
        sol: List<PumpCandle>
    ): String {
        if (index < 6) return "Недостаточно данных BTC/SOL"
        val btcByTime = btc.associateBy { it.closeTime }
        val solByTime = sol.associateBy { it.closeTime }
        val oldTime = pump[index - 6].closeTime
        val nowTime = pump[index].closeTime
        val btcOld = btcByTime[oldTime]?.close ?: return "BTC/SOL не синхронизированы по времени"
        val btcNow = btcByTime[nowTime]?.close ?: return "BTC/SOL не синхронизированы по времени"
        val solOld = solByTime[oldTime]?.close ?: return "BTC/SOL не синхронизированы по времени"
        val solNow = solByTime[nowTime]?.close ?: return "BTC/SOL не синхронизированы по времени"
        if (btcOld <= 0.0 || solOld <= 0.0) return "Недостаточно данных BTC/SOL"
        val pumpReturn = returnOver(index, 6, pump)
        val marketReturn = ((btcNow / btcOld - 1.0) + (solNow / solOld - 1.0)) / 2.0
        val detachment = pumpReturn - marketReturn
        return when {
            abs(detachment) >= 0.030 -> String.format(
                Locale.GERMAN,
                "PUMP отделился от BTC/SOL на %+.1f%%",
                detachment * 100.0
            )
            pumpReturn * marketReturn > 0.0 && abs(marketReturn) >= 0.002 -> "PUMP движется синхронно с BTC/SOL"
            else -> "Связь с BTC/SOL сейчас смешанная"
        }
    }

    private fun returnOver(index: Int, bars: Int, candles: List<PumpCandle>): Double {
        val current = candles.getOrNull(index)?.close ?: return 0.0
        val old = candles.getOrNull(index - bars)?.close ?: return 0.0
        return if (old > 0.0) current / old - 1.0 else 0.0
    }

    private fun percentileRank(current: Double, history: List<Double>): Double {
        if (history.isEmpty() || !current.isFinite()) return 50.0
        val valid = history.filter { it.isFinite() }
        if (valid.isEmpty()) return 50.0
        val belowOrEqual = valid.count { it <= current }
        return belowOrEqual.toDouble() / valid.size * 100.0
    }
}
