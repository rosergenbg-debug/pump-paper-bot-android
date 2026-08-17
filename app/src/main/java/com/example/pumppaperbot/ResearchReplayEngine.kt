package com.example.pumppaperbot

import kotlin.math.max

data class ResearchReplayTrade(
    val setup: ResearchSetup,
    val entryTime: Long,
    val exitTime: Long,
    val entryPrice: Double,
    val exitPrice: Double,
    val netReturnPercent: Double,
    val exitReason: String
)

data class ResearchReplayResult(
    val initialCapital: Double,
    val finalCapital: Double,
    val netReturnPercent: Double,
    val completedTrades: Int,
    val shadowCandidates: Int,
    val unfilledCandidates: Int,
    val expectancyPercent: Double,
    val profitFactor: Double,
    val maxDrawdownPercent: Double,
    val trades: List<ResearchReplayTrade>
)

/**
 * Causal replay for the V5 research engine.  Both entries and exits call the same
 * pure functions intended for live use, and an order is filled only at the next
 * candle open.  This is a first bar-level simulator, not a substitute for later
 * spread/order-book/latency stress tests.
 */
object ResearchReplayEngine {
    fun run(
        candles: List<PumpCandle>,
        startIndex: Int = 240,
        initialCapital: Double = 1_000.0,
        feeRate: Double = 0.0015,
        slippageRate: Double = 0.0010
    ): ResearchReplayResult {
        require(initialCapital > 0.0)
        require(feeRate in 0.0..0.05)
        require(slippageRate in 0.0..0.05)
        if (candles.size < startIndex + 2) return empty(initialCapital)

        var cash = initialCapital
        var coins = 0.0
        var position: ResearchPositionState? = null
        var entryCapital = 0.0
        var entryTime = 0L
        var shadowCandidates = 0
        var unfilledCandidates = 0
        val trades = ArrayList<ResearchReplayTrade>()
        val equity = ArrayList<Double>()
        var index = startIndex.coerceAtLeast(240)

        while (index < candles.lastIndex) {
            val active = position
            if (active == null) {
                val candidate = ResearchDecisionEngine.evaluate(candles, index)
                if (candidate.status == ResearchSignalStatus.SHADOW_CANDIDATE) {
                    shadowCandidates++
                    val execution = candles[index + 1]
                    val zoneLow = candidate.entryZoneLow ?: error("candidate without entry zone")
                    val zoneHigh = candidate.entryZoneHigh ?: error("candidate without entry zone")
                    val fill = execution.open * (1.0 + slippageRate)
                    if (fill !in zoneLow..zoneHigh) {
                        unfilledCandidates++
                        equity += cash
                        index++
                        continue
                    }
                    entryCapital = cash
                    coins = cash * (1.0 - feeRate) / fill
                    cash = 0.0
                    entryTime = execution.openTime
                    val entryAtr = candidate.atrPercent
                        ?.let { it / 100.0 * fill }
                        ?.takeIf { it > 0.0 }
                        ?: error("candidate without ATR")
                    position = ResearchPositionState(
                        setup = candidate.setup,
                        entryPrice = fill,
                        entryAtr = entryAtr,
                        invalidationPrice = candidate.invalidationPrice ?: error("candidate without invalidation"),
                        entryCandleIndex = index + 1,
                        peakPrice = fill
                    )
                    index++
                    equity += coins * candles[index].close * (1.0 - feeRate)
                    continue
                }
            } else {
                val decision = ResearchPositionEngine.evaluate(candles, index, active)
                position = decision.updatedPosition
                if (decision.status == ResearchExitStatus.SHADOW_EXIT) {
                    val execution = candles[index + 1]
                    val fill = execution.open * (1.0 - slippageRate)
                    val exitCapital = coins * fill * (1.0 - feeRate)
                    trades += ResearchReplayTrade(
                        setup = active.setup,
                        entryTime = entryTime,
                        exitTime = execution.openTime,
                        entryPrice = active.entryPrice,
                        exitPrice = fill,
                        netReturnPercent = (exitCapital / entryCapital - 1.0) * 100.0,
                        exitReason = decision.reason
                    )
                    cash = exitCapital
                    coins = 0.0
                    position = null
                    index++
                    equity += cash
                    continue
                }
            }
            equity += if (position == null) cash else coins * candles[index].close * (1.0 - feeRate)
            index++
        }

        val finalCapital = if (position == null) cash else coins * candles.last().close * (1.0 - feeRate)
        equity += finalCapital
        val returns = trades.map { it.netReturnPercent }
        val grossProfit = returns.filter { it > 0.0 }.sum()
        val grossLoss = -returns.filter { it < 0.0 }.sum()
        val profitFactor = when {
            grossLoss > 0.0 -> grossProfit / grossLoss
            grossProfit > 0.0 -> Double.POSITIVE_INFINITY
            else -> 0.0
        }
        return ResearchReplayResult(
            initialCapital = initialCapital,
            finalCapital = finalCapital,
            netReturnPercent = (finalCapital / initialCapital - 1.0) * 100.0,
            completedTrades = trades.size,
            shadowCandidates = shadowCandidates,
            unfilledCandidates = unfilledCandidates,
            expectancyPercent = returns.averageOrZero(),
            profitFactor = profitFactor,
            maxDrawdownPercent = maxDrawdown(equity),
            trades = trades
        )
    }

    private fun empty(capital: Double) = ResearchReplayResult(
        capital, capital, 0.0, 0, 0, 0, 0.0, 0.0, 0.0, emptyList()
    )

    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

    private fun maxDrawdown(equity: List<Double>): Double {
        var peak = equity.firstOrNull() ?: return 0.0
        var worst = 0.0
        equity.forEach { value ->
            peak = max(peak, value)
            if (peak > 0.0) worst = max(worst, (peak - value) / peak * 100.0)
        }
        return worst
    }
}
