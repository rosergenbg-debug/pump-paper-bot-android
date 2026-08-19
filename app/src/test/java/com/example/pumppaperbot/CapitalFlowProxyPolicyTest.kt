package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapitalFlowProxyPolicyTest {
    private fun breathing(short: Int = 35, absorption: Int = 20) = LiveMarketBreathingSnapshot(
        fresh = true,
        flowWave = LiveFlowWave(points = listOf(LiveFlowWavePoint(1L, short, short, 20, 10, 5))),
        buyerBreath = BuyerBreathSnapshot(absorptionRisk = absorption, efficiencyScore = 35)
    )

    @Test fun `price buys and rising open interest classify new longs`() {
        val result = CapitalFlowProxyPolicy.evaluate(
            ImpulseSnapshot(
                candleTime = 1L,
                spotTakerRatio = 0.68,
                futuresTakerRatio = 0.66,
                return15m = 0.018,
                return60m = 0.03,
                openInterestChange10m = 0.012
            ),
            breathing(),
            now = 1L
        )

        assertEquals(CapitalFlowMode.NEW_LONGS, result.mode)
        assertTrue(result.confidence >= 80)
    }

    @Test fun `rising price with falling open interest is short covering not smart money`() {
        val result = CapitalFlowProxyPolicy.evaluate(
            ImpulseSnapshot(
                candleTime = 1L,
                spotTakerRatio = 0.62,
                futuresTakerRatio = 0.61,
                return15m = 0.014,
                openInterestChange10m = -0.014
            ),
            breathing(),
            now = 1L
        )

        assertEquals(CapitalFlowMode.SHORT_COVERING, result.mode)
        assertTrue(result.identityNote.contains("анонимен"))
    }

    @Test fun `heavy buying without price response is labelled absorption`() {
        val result = CapitalFlowProxyPolicy.evaluate(
            ImpulseSnapshot(
                candleTime = 1L,
                spotTakerRatio = 0.72,
                futuresTakerRatio = 0.69,
                return15m = 0.0005,
                openInterestChange10m = 0.001
            ),
            breathing(short = 45, absorption = 80),
            now = 1L
        )

        assertEquals(CapitalFlowMode.ACCUMULATION, result.mode)
    }
}
