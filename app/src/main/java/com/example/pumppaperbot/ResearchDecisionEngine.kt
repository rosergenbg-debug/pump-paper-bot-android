package com.example.pumppaperbot

import kotlin.math.abs
import kotlin.math.max

enum class ResearchMarketRegime {
    RANGE,
    TREND_UP,
    IMPULSE,
    STRESS,
    UNCERTAIN
}

enum class ResearchSetup {
    NO_TRADE,
    TREND_PULLBACK,
    RANGE_RECLAIM,
    BREAKOUT_RETEST
}

enum class ResearchSignalStatus {
    NO_TRADE,
    SHADOW_CANDIDATE,
    ACTIONABLE
}

/**
 * Evidence produced outside the decision engine.  The engine must never turn its own
 * score into "confidence": only frozen, out-of-sample and forward results may unlock
 * an actionable signal.
 */
data class ResearchValidationEvidence(
    val completedSignals: Int = 0,
    val walkForwardFolds: Int = 0,
    val netExpectancyPercent: Double = 0.0,
    val lower95ExpectancyPercent: Double = 0.0,
    val profitFactor: Double = 0.0,
    val maxDrawdownPercent: Double = Double.POSITIVE_INFINITY,
    val stableParameterFoldsPercent: Double = 0.0,
    val shadowDays: Int = 0,
    val medianFillErrorBps: Double = Double.POSITIVE_INFINITY
)

data class ResearchValidationResult(
    val passed: Boolean,
    val failures: List<String>
)

/** Frozen before the first validation run; these values must not be tuned on test data. */
object ResearchValidationGate {
    const val MIN_COMPLETED_SIGNALS = 60
    const val MIN_WALK_FORWARD_FOLDS = 4
    const val MIN_PROFIT_FACTOR = 1.20
    const val MAX_DRAWDOWN_PERCENT = 12.0
    const val MIN_STABLE_FOLDS_PERCENT = 75.0
    const val MIN_SHADOW_DAYS = 14
    const val MAX_MEDIAN_FILL_ERROR_BPS = 25.0

    fun evaluate(evidence: ResearchValidationEvidence?): ResearchValidationResult {
        if (evidence == null) {
            return ResearchValidationResult(false, listOf("нет независимой валидации"))
        }
        val failures = buildList {
            if (evidence.completedSignals < MIN_COMPLETED_SIGNALS) add("меньше 60 завершённых сигналов")
            if (evidence.walkForwardFolds < MIN_WALK_FORWARD_FOLDS) add("меньше 4 walk-forward окон")
            if (evidence.netExpectancyPercent <= 0.0) add("матожидание после расходов не положительное")
            if (evidence.lower95ExpectancyPercent <= 0.0) add("нижняя 95% граница матожидания не выше нуля")
            if (evidence.profitFactor < MIN_PROFIT_FACTOR) add("profit factor ниже 1,20")
            if (evidence.maxDrawdownPercent > MAX_DRAWDOWN_PERCENT) add("просадка выше 12%")
            if (evidence.stableParameterFoldsPercent < MIN_STABLE_FOLDS_PERCENT) add("параметры нестабильны между окнами")
            if (evidence.shadowDays < MIN_SHADOW_DAYS) add("меньше 14 дней теневой проверки")
            if (evidence.medianFillErrorBps > MAX_MEDIAN_FILL_ERROR_BPS) add("слишком большая ошибка модели исполнения")
        }
        return ResearchValidationResult(failures.isEmpty(), failures)
    }
}

data class ResearchDecision(
    val status: ResearchSignalStatus,
    val regime: ResearchMarketRegime,
    val setup: ResearchSetup,
    val reason: String,
    val candleTime: Long,
    val entryZoneLow: Double? = null,
    val entryZoneHigh: Double? = null,
    val invalidationPrice: Double? = null,
    val atrPercent: Double? = null,
    val buyerShare: Double? = null,
    val validationFailures: List<String> = emptyList()
)

enum class ResearchExitStatus {
    HOLD,
    SHADOW_EXIT,
    ACTIONABLE_EXIT
}

data class ResearchPositionState(
    val setup: ResearchSetup,
    val entryPrice: Double,
    val entryAtr: Double,
    val invalidationPrice: Double,
    val entryCandleIndex: Int,
    val peakPrice: Double = entryPrice
)

data class ResearchExitDecision(
    val status: ResearchExitStatus,
    val reason: String,
    val updatedPosition: ResearchPositionState,
    val exitLevel: Double? = null,
    val validationFailures: List<String> = emptyList()
)

/**
 * Position decisions are separated from entry discovery.  A percentage pullback by
 * itself is never an exit: the original invalidation, an activated ATR trail, or a
 * two-candle structural break must be observed on closed candles.
 */
object ResearchPositionEngine {
    private const val MAX_HOLD_BARS = 96

    fun evaluate(
        candles: List<PumpCandle>,
        index: Int,
        position: ResearchPositionState,
        validation: ResearchValidationEvidence? = null
    ): ResearchExitDecision {
        require(index in candles.indices) { "index outside candle history" }
        require(position.entryAtr > 0.0) { "entry ATR must be positive" }
        val current = candles[index]
        val updated = position.copy(peakPrice = max(position.peakPrice, current.high))
        val gate = ResearchValidationGate.evaluate(validation)

        fun exit(reason: String, level: Double): ResearchExitDecision = ResearchExitDecision(
            status = if (gate.passed) ResearchExitStatus.ACTIONABLE_EXIT else ResearchExitStatus.SHADOW_EXIT,
            reason = reason,
            updatedPosition = updated,
            exitLevel = level,
            validationFailures = gate.failures
        )

        if (current.close <= position.invalidationPrice) {
            return exit("закрытая свеча нарушила исходный уровень invalidation", position.invalidationPrice)
        }

        val favorableExcursion = updated.peakPrice - position.entryPrice
        if (favorableExcursion >= position.entryAtr * 2.0) {
            val trailingLevel = max(position.entryPrice, updated.peakPrice - position.entryAtr * 1.5)
            if (current.close <= trailingLevel) {
                return exit("ATR-трейлинг активирован только после движения не меньше 2 ATR", trailingLevel)
            }
        }

        if (index >= 1) {
            val ema20Now = ResearchDecisionEngine.emaAt(candles, index, 20)
            val ema20Previous = ResearchDecisionEngine.emaAt(candles, index - 1, 20)
            val currentBuyers = ResearchDecisionEngine.buyerShare(candles[index])
            val previousBuyers = ResearchDecisionEngine.buyerShare(candles[index - 1])
            val structuralBreak = candles[index].close < ema20Now &&
                candles[index - 1].close < ema20Previous &&
                currentBuyers != null && previousBuyers != null &&
                currentBuyers < 0.47 && previousBuyers < 0.47
            if (structuralBreak) {
                return exit("две закрытые свечи ниже EMA20 подтверждены продавцами", ema20Now)
            }
        }

        if (index - position.entryCandleIndex >= MAX_HOLD_BARS) {
            return exit("истёк максимальный 48-часовой исследовательский горизонт", current.close)
        }
        return ResearchExitDecision(
            status = ResearchExitStatus.HOLD,
            reason = "гипотеза не нарушена; обычное процентное колебание не является выходом",
            updatedPosition = updated
        )
    }
}

/**
 * V5 research-only candidate generator.
 *
 * It deliberately has no DeepSeek input and no arbitrary 1..10 confidence.  Every
 * calculation is made from candles at or before [index], so the same function can be
 * called by live evaluation and historical replay.
 */
object ResearchDecisionEngine {
    private const val MIN_BARS = 240
    private const val ATR_PERIOD = 14
    private const val REGIME_LOOKBACK = 48
    private const val VOLATILITY_LOOKBACK = 30 * 48

    fun evaluate(
        candles: List<PumpCandle>,
        index: Int = candles.lastIndex,
        validation: ResearchValidationEvidence? = null
    ): ResearchDecision {
        if (index !in candles.indices || index < MIN_BARS) {
            return noTrade(ResearchMarketRegime.UNCERTAIN, "недостаточно закрытых свечей", 0L)
        }
        val history = candles.subList(0, index + 1)
        if (history.any { it.open <= 0.0 || it.high < it.low || it.close <= 0.0 || it.volume < 0.0 }) {
            return noTrade(ResearchMarketRegime.UNCERTAIN, "повреждённые OHLCV-данные", history.last().closeTime)
        }

        val current = history.last()
        val previous = history[history.lastIndex - 1]
        val atr = atrAt(history, history.lastIndex, ATR_PERIOD)
            ?: return noTrade(ResearchMarketRegime.UNCERTAIN, "ATR ещё не рассчитан", current.closeTime)
        if (atr <= 0.0) return noTrade(ResearchMarketRegime.UNCERTAIN, "нулевая волатильность", current.closeTime)

        val ema20 = emaAt(history, history.lastIndex, 20)
        val ema50 = emaAt(history, history.lastIndex, 50)
        val ema20SixBarsAgo = emaAt(history, history.lastIndex - 6, 20)
        val buyerShare = buyerShare(current)
            ?: return noTrade(ResearchMarketRegime.UNCERTAIN, "нет taker-buy объёма; подтверждение потока невозможно", current.closeTime)
        val atrPercent = atr / current.close * 100.0
        val atrPercentile = volatilityPercentile(history, history.lastIndex, atrPercent)
        val threeBarMove = (current.close - history[history.lastIndex - 3].close) / atr
        val emaSlope = (ema20 - ema20SixBarsAgo) / atr
        val regime = when {
            threeBarMove <= -2.5 || (atrPercentile >= 0.95 && current.close < ema20 && buyerShare < 0.45) ->
                ResearchMarketRegime.STRESS
            threeBarMove >= 2.5 || (atrPercentile >= 0.90 && current.close > ema20) ->
                ResearchMarketRegime.IMPULSE
            ema20 > ema50 && emaSlope >= 0.15 && current.close > ema50 -> ResearchMarketRegime.TREND_UP
            abs(ema20 - ema50) <= atr * 0.80 -> ResearchMarketRegime.RANGE
            else -> ResearchMarketRegime.UNCERTAIN
        }

        if (regime == ResearchMarketRegime.STRESS || regime == ResearchMarketRegime.UNCERTAIN) {
            return noTrade(regime, "режим рынка не допускает новый риск", current.closeTime, atrPercent, buyerShare)
        }

        val shortReturns = normalizedMoves(history, history.lastIndex)
        val currentMove = (current.close / history[history.lastIndex - 3].close - 1.0)
        val movePercentile = percentileRank(shortReturns, currentMove)
        val distanceFromEma = (current.close - ema20) / atr
        if (movePercentile >= 0.85 || distanceFromEma > 1.0) {
            return noTrade(regime, "анти-погоня: цена уже слишком далеко от нормального диапазона", current.closeTime, atrPercent, buyerShare)
        }

        val rangeStart = max(0, history.lastIndex - REGIME_LOOKBACK)
        val rangeBeforeCurrent = history.subList(rangeStart, history.lastIndex)
        val rangeLow = rangeBeforeCurrent.minOf { it.low }
        val rangeHigh = rangeBeforeCurrent.maxOf { it.high }
        val trendPullback = regime == ResearchMarketRegime.TREND_UP &&
            previous.close <= ema20 + atr * 0.10 &&
            current.close > ema20 && current.close > previous.close && current.close > current.open &&
            distanceFromEma in -0.10..0.75 && buyerShare >= 0.52
        val rangeReclaim = regime == ResearchMarketRegime.RANGE &&
            previous.low <= rangeLow + atr * 0.50 &&
            current.close > previous.close && current.close >= previous.open &&
            current.close <= rangeLow + atr * 2.0 && buyerShare >= 0.53
        val retestLevel = recentBreakoutLevel(history, history.lastIndex, atr)
        val breakoutRetest = retestLevel != null &&
            regime in setOf(ResearchMarketRegime.TREND_UP, ResearchMarketRegime.IMPULSE) &&
            current.low <= retestLevel + atr * 0.40 && current.close >= retestLevel &&
            current.close <= retestLevel + atr * 0.80 && buyerShare >= 0.52

        val setup = when {
            trendPullback -> ResearchSetup.TREND_PULLBACK
            rangeReclaim -> ResearchSetup.RANGE_RECLAIM
            breakoutRetest -> ResearchSetup.BREAKOUT_RETEST
            else -> ResearchSetup.NO_TRADE
        }
        if (setup == ResearchSetup.NO_TRADE) {
            return noTrade(regime, "нет подтверждённого отката, возврата границы или ретеста", current.closeTime, atrPercent, buyerShare)
        }

        val gate = ResearchValidationGate.evaluate(validation)
        val status = if (gate.passed) ResearchSignalStatus.ACTIONABLE else ResearchSignalStatus.SHADOW_CANDIDATE
        val anchor = when (setup) {
            ResearchSetup.RANGE_RECLAIM -> rangeLow
            ResearchSetup.BREAKOUT_RETEST -> retestLevel ?: current.low
            else -> ema20
        }
        val invalidation = minOf(previous.low, anchor - atr * 0.75)
        return ResearchDecision(
            status = status,
            regime = regime,
            setup = setup,
            reason = when (setup) {
                ResearchSetup.TREND_PULLBACK -> "откат к EMA20 завершён возвратом цены и покупательского потока"
                ResearchSetup.RANGE_RECLAIM -> "нижняя граница диапазона протестирована и возвращена покупателями"
                ResearchSetup.BREAKOUT_RETEST -> "пробой подтверждён обратным тестом без погони за первой свечой"
                ResearchSetup.NO_TRADE -> error("unreachable")
            },
            candleTime = current.closeTime,
            entryZoneLow = max(anchor, current.close - atr * 0.25),
            entryZoneHigh = current.close + atr * 0.15,
            invalidationPrice = invalidation,
            atrPercent = atrPercent,
            buyerShare = buyerShare,
            validationFailures = gate.failures
        )
    }

    private fun noTrade(
        regime: ResearchMarketRegime,
        reason: String,
        candleTime: Long,
        atrPercent: Double? = null,
        buyerShare: Double? = null
    ) = ResearchDecision(
        ResearchSignalStatus.NO_TRADE,
        regime,
        ResearchSetup.NO_TRADE,
        reason,
        candleTime,
        atrPercent = atrPercent,
        buyerShare = buyerShare
    )

    internal fun buyerShare(candle: PumpCandle): Double? =
        candle.takerBuyVolume.takeIf { candle.volume > 0.0 && it > 0.0 }
            ?.let { (it / candle.volume).coerceIn(0.0, 1.0) }

    internal fun atrAt(candles: List<PumpCandle>, index: Int, period: Int): Double? {
        if (index < period) return null
        return (index - period + 1..index).map { i ->
            val previousClose = candles[i - 1].close
            maxOf(
                candles[i].high - candles[i].low,
                abs(candles[i].high - previousClose),
                abs(candles[i].low - previousClose)
            )
        }.average()
    }

    internal fun emaAt(candles: List<PumpCandle>, index: Int, period: Int): Double {
        val alpha = 2.0 / (period + 1.0)
        var value = candles.first().close
        for (i in 1..index) value = alpha * candles[i].close + (1.0 - alpha) * value
        return value
    }

    private fun volatilityPercentile(candles: List<PumpCandle>, index: Int, current: Double): Double {
        val start = max(ATR_PERIOD, index - VOLATILITY_LOOKBACK)
        val history = (start until index).mapNotNull { i ->
            atrAt(candles, i, ATR_PERIOD)?.let { it / candles[i].close * 100.0 }
        }
        return percentileRank(history, current)
    }

    private fun normalizedMoves(candles: List<PumpCandle>, index: Int): List<Double> {
        val start = max(3, index - VOLATILITY_LOOKBACK)
        return (start until index).map { i -> candles[i].close / candles[i - 3].close - 1.0 }
    }

    private fun percentileRank(values: List<Double>, value: Double): Double {
        if (values.isEmpty()) return 0.5
        return values.count { it <= value }.toDouble() / values.size
    }

    private fun recentBreakoutLevel(candles: List<PumpCandle>, index: Int, atr: Double): Double? {
        for (breakoutIndex in index - 2 downTo max(REGIME_LOOKBACK, index - 6)) {
            val baseStart = breakoutIndex - REGIME_LOOKBACK
            if (baseStart < 0) continue
            val level = candles.subList(baseStart, breakoutIndex).maxOf { it.high }
            if (candles[breakoutIndex].close >= level + atr * 0.20) return level
        }
        return null
    }
}
