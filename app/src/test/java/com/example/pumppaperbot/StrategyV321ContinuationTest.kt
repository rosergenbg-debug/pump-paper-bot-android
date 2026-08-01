package com.example.pumppaperbot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrategyV321ContinuationTest {
    @Test
    fun `small positive funding is neutral but overheated funding is not`() {
        assertTrue(StrategyV2.EntrySensitivity.fundingIsNeutral(0.00005))
        assertTrue(StrategyV2.EntrySensitivity.fundingIsNeutral(0.0001))
        assertFalse(StrategyV2.EntrySensitivity.fundingIsNeutral(0.000101))
    }

    @Test
    fun `continuation requires two closed rising candles above a rising ema20`() {
        assertTrue(
            StrategyV2.EntrySensitivity.twoClosedCandlesConfirmTrend(
                currentClose = 1.03,
                previousClose = 1.02,
                twoBarsAgoClose = 1.00,
                ema20Now = 1.01,
                ema20Previous = 1.005
            )
        )
        assertFalse(
            StrategyV2.EntrySensitivity.twoClosedCandlesConfirmTrend(
                currentClose = 1.03,
                previousClose = 0.99,
                twoBarsAgoClose = 1.00,
                ema20Now = 1.01,
                ema20Previous = 1.005
            )
        )
    }

    @Test
    fun `continuation buyer confirmation accepts aligned markets or strong spot relative strength`() {
        assertTrue(StrategyV2.EntrySensitivity.continuationBuyerFlowReady(0.01, 0.02, -0.01))
        assertTrue(StrategyV2.EntrySensitivity.continuationBuyerFlowReady(0.03, null, 0.01))
        assertFalse(StrategyV2.EntrySensitivity.continuationBuyerFlowReady(-0.01, 0.03, 0.02))
    }

    @Test
    fun `continuation avoids overbought RSI`() {
        assertTrue(StrategyV2.EntrySensitivity.continuationRsiReady(55.0, 52.0))
        assertFalse(StrategyV2.EntrySensitivity.continuationRsiReady(61.0, 58.0))
    }
}
