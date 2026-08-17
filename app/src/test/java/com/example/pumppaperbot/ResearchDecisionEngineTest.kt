package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResearchDecisionEngineTest {
    @Test
    fun candidateCannotBecomeActionableWithoutIndependentEvidence() {
        val candles = trendWithPullback()
        val decision = ResearchDecisionEngine.evaluate(candles)

        assertEquals(ResearchSetup.TREND_PULLBACK, decision.setup)
        assertEquals(ResearchSignalStatus.SHADOW_CANDIDATE, decision.status)
        assertTrue(decision.validationFailures.isNotEmpty())
    }

    @Test
    fun validationRequiresPositiveLowerConfidenceBound() {
        val result = ResearchValidationGate.evaluate(passingEvidence().copy(lower95ExpectancyPercent = -0.01))

        assertFalse(result.passed)
        assertTrue(result.failures.any { it.contains("95%") })
    }

    @Test
    fun frozenValidationGateCanUnlockOnlyAnExistingSetup() {
        val decision = ResearchDecisionEngine.evaluate(trendWithPullback(), validation = passingEvidence())

        assertEquals(ResearchSetup.TREND_PULLBACK, decision.setup)
        assertEquals(ResearchSignalStatus.ACTIONABLE, decision.status)
    }

    @Test
    fun expansionTopIsRejectedByAntiChase() {
        val candles = trendWithPullback().toMutableList()
        val previous = candles.last()
        candles += candle(
            index = candles.size,
            open = previous.close,
            close = previous.close * 1.12,
            low = previous.close * 0.995,
            high = previous.close * 1.13,
            buyerShare = 0.70
        )

        val decision = ResearchDecisionEngine.evaluate(candles, validation = passingEvidence())

        assertEquals(ResearchSignalStatus.NO_TRADE, decision.status)
        assertEquals(ResearchSetup.NO_TRADE, decision.setup)
        assertTrue(decision.reason.contains("анти-погоня") || decision.regime == ResearchMarketRegime.STRESS)
    }

    @Test
    fun futureCandlesCannotChangePastDecision() {
        val base = trendWithPullback()
        val before = ResearchDecisionEngine.evaluate(base)
        val extended = base + List(20) { offset ->
            candle(base.size + offset, 70.0, 60.0, 72.0, 55.0, 0.20)
        }
        val after = ResearchDecisionEngine.evaluate(extended, index = base.lastIndex)

        assertEquals(before, after)
    }

    @Test
    fun missingBuyerFlowForcesNoTradeInsteadOfInventedConfidence() {
        val candles = trendWithPullback().toMutableList()
        candles[candles.lastIndex] = candles.last().copy(takerBuyVolume = 0.0)

        val decision = ResearchDecisionEngine.evaluate(candles, validation = passingEvidence())

        assertEquals(ResearchSignalStatus.NO_TRADE, decision.status)
        assertTrue(decision.reason.contains("taker-buy"))
    }

    @Test
    fun ordinaryTwoPercentPullbackDoesNotForceExit() {
        val candles = flatCandles(245, 100.0).toMutableList()
        candles += candle(245, 100.0, 98.0, 97.5, 100.5, 0.50)
        val position = ResearchPositionState(
            setup = ResearchSetup.RANGE_RECLAIM,
            entryPrice = 100.0,
            entryAtr = 3.0,
            invalidationPrice = 94.0,
            entryCandleIndex = 240,
            peakPrice = 101.0
        )

        val decision = ResearchPositionEngine.evaluate(candles, candles.lastIndex, position)

        assertEquals(ResearchExitStatus.HOLD, decision.status)
    }

    @Test
    fun closedInvalidationProducesOnlyShadowExitBeforeValidation() {
        val candles = flatCandles(245, 100.0).toMutableList()
        candles += candle(245, 96.0, 93.5, 93.0, 96.5, 0.40)
        val position = ResearchPositionState(
            setup = ResearchSetup.RANGE_RECLAIM,
            entryPrice = 100.0,
            entryAtr = 3.0,
            invalidationPrice = 94.0,
            entryCandleIndex = 240
        )

        val decision = ResearchPositionEngine.evaluate(candles, candles.lastIndex, position)

        assertEquals(ResearchExitStatus.SHADOW_EXIT, decision.status)
        assertTrue(decision.validationFailures.isNotEmpty())
    }

    @Test
    fun replayUsesCandidateAndPositionEnginesWithNextOpenFills() {
        val base = trendWithPullback()
        val lastPrice = base.last().close
        val continuation = List(100) { offset ->
            val index = base.size + offset
            candle(index, lastPrice, lastPrice, lastPrice - 0.002, lastPrice + 0.002, 0.50)
        }

        val result = ResearchReplayEngine.run(base + continuation)

        assertTrue(result.shadowCandidates >= 1)
        assertTrue(result.completedTrades >= 1)
        assertEquals(base.last().close * 1.001, result.trades.first().entryPrice, 0.0000001)
    }

    @Test
    fun replayDoesNotChaseGapAboveCandidateEntryZone() {
        val base = trendWithPullback()
        val last = base.last()
        val gap = candle(base.size, last.close * 1.10, last.close * 1.11, last.close * 1.09, last.close * 1.12, 0.65)

        val result = ResearchReplayEngine.run(base + gap)

        assertEquals(1, result.shadowCandidates)
        assertEquals(1, result.unfilledCandidates)
        assertEquals(0, result.completedTrades)
    }

    private fun passingEvidence() = ResearchValidationEvidence(
        completedSignals = 80,
        walkForwardFolds = 5,
        netExpectancyPercent = 0.8,
        lower95ExpectancyPercent = 0.1,
        profitFactor = 1.35,
        maxDrawdownPercent = 8.0,
        stableParameterFoldsPercent = 80.0,
        shadowDays = 21,
        medianFillErrorBps = 12.0
    )

    private fun trendWithPullback(): List<PumpCandle> {
        val candles = ArrayList<PumpCandle>()
        repeat(245) { i ->
            val close = 1.0 + i * 0.00023
            candles += candle(i, close - 0.0001, close, close - 0.004, close + 0.004, 0.54)
        }
        val anchor = candles.last().close
        candles += candle(245, anchor, anchor - 0.002, anchor - 0.003, anchor + 0.0004, 0.48)
        candles += candle(246, anchor - 0.002, anchor + 0.0001, anchor - 0.0022, anchor + 0.001, 0.57)
        return candles
    }

    private fun flatCandles(count: Int, price: Double): List<PumpCandle> = List(count) { index ->
        candle(index, price, price, price - 1.5, price + 1.5, 0.50)
    }

    private fun candle(
        index: Int,
        open: Double,
        close: Double,
        low: Double,
        high: Double,
        buyerShare: Double
    ): PumpCandle {
        val volume = 1_000.0
        return PumpCandle(
            openTime = index * 1_800_000L,
            open = open,
            high = high,
            low = low,
            close = close,
            volume = volume,
            closeTime = (index + 1L) * 1_800_000L - 1L,
            quoteVolume = volume * close,
            tradeCount = 100,
            takerBuyVolume = volume * buyerShare
        )
    }
}
