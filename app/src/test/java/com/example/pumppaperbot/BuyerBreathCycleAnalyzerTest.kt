package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuyerBreathCycleAnalyzerTest {
    private val now = 40L * 60L * 1000L

    @Test fun `rising share activity and price detect beginning of inhale`() {
        val samples = series { index ->
            if (index >= 140) point(index, 58.0, 2_000.0, 1.0 + (index - 140) * 0.0002, 1.5)
            else point(index, 49.0, 500.0, 1.0)
        }

        val result = analyze(samples)

        assertEquals(BuyerBreathPhase.IGNITION, result.phase)
        assertTrue((result.pressureScore ?: 0) > 0)
        assertTrue(result.actionHint.contains("5–15"))
    }

    @Test fun `persistent buyers with price response detect expansion`() {
        val samples = series { index ->
            if (index >= 100) point(index, 60.0, 2_200.0, 1.0 + (index - 100) * 0.00018, 1.4)
            else point(index, 50.0, 500.0, 1.0)
        }

        val result = analyze(samples)

        assertEquals(BuyerBreathPhase.EXPANSION, result.phase)
        assertTrue((result.efficiencyScore ?: -100) > -20)
    }

    @Test fun `high buy share without price response is absorption not growth`() {
        val samples = series { index ->
            if (index >= 100) point(index, 66.0, 2_400.0, 1.01 - (index - 100) * 0.00012, 1.5, -0.12)
            else point(index, 50.0, 600.0, 1.01)
        }

        val result = analyze(samples)

        assertEquals(BuyerBreathPhase.EXHAUSTION, result.phase)
        assertTrue(result.absorptionRisk >= 65)
        assertTrue((result.efficiencyScore ?: 100) < 0)
    }

    @Test fun `seller dominance and falling price detect takeover`() {
        val samples = series { index ->
            if (index >= 100) point(index, 43.0, 1_800.0, 1.01 - (index - 100) * 0.00015, 1.3)
            else point(index, 50.0, 600.0, 1.01)
        }

        val result = analyze(samples)

        assertEquals(BuyerBreathPhase.SELLER_TAKEOVER, result.phase)
        assertTrue((result.pressureScore ?: 0) < 0)
    }

    @Test fun `shock bypasses the calm phase model`() {
        val samples = series { index -> point(index, 60.0, 2_000.0, 1.0 + index * 0.0001, 1.5) }
            .dropLast(1) + point(159, 62.0, 4_000.0, 1.04, 2.0, 0.90)

        val result = analyze(samples)

        assertEquals(BuyerBreathPhase.SHOCK, result.phase)
        assertTrue(result.actionHint.contains("аварийная", ignoreCase = true))
    }

    @Test fun `quiet phase keeps reference arc waiting at zero`() {
        val timing = BuyerBreathTimingPolicy.estimate(BuyerBreathPhase.QUIET, 1_373)

        assertFalse(timing.active)
        assertTrue(timing.forecastReliable)
        assertEquals(0, timing.progressPercent)
        assertEquals(35, timing.estimatedTotalMinutes)
    }

    @Test fun `expansion receives adaptive elapsed time and peak window`() {
        val timing = BuyerBreathTimingPolicy.estimate(BuyerBreathPhase.EXPANSION, 7)

        assertTrue(timing.active)
        assertTrue(timing.forecastReliable)
        assertEquals(7, timing.elapsedMinutes)
        assertTrue(timing.estimatedTotalMinutes in 20..65)
        assertTrue(timing.estimatedFlowPeakMinute in 7..25)
        assertEquals(3, timing.nextPhaseMinMinutes ?: -1)
        assertEquals(12, timing.nextPhaseMaxMinutes ?: -1)
    }

    @Test fun `exhaustion is placed on descending side of arc`() {
        val timing = BuyerBreathTimingPolicy.estimate(BuyerBreathPhase.EXHAUSTION, 12)

        assertTrue(timing.progressPercent >= 62)
        assertTrue(timing.elapsedMinutes < timing.estimatedTotalMinutes)
        assertTrue(timing.status.contains("Нисходящая"))
    }

    @Test fun `seller takeover closes arc without inventing another phase timer`() {
        val timing = BuyerBreathTimingPolicy.estimate(BuyerBreathPhase.SELLER_TAKEOVER, 15)

        assertTrue(timing.progressPercent >= 85)
        assertTrue(timing.nextPhaseMinMinutes == null)
        assertTrue(timing.status.contains("завершена"))
    }

    @Test fun `shock disables ordinary timing forecast`() {
        val timing = BuyerBreathTimingPolicy.estimate(BuyerBreathPhase.SHOCK, 2)

        assertTrue(timing.active)
        assertFalse(timing.forecastReliable)
        assertTrue(timing.status.contains("Шоковый"))
    }

    private fun analyze(samples: List<LiveBreathingSample>): BuyerBreathSnapshot {
        val horizons = LiveMarketBreathingAnalyzer.analyze(samples, now).horizons
        return BuyerBreathCycleAnalyzer.analyze(samples, horizons, fresh = true)
    }

    private fun series(block: (Int) -> LiveBreathingSample): List<LiveBreathingSample> =
        (0 until 160).map(block)

    private fun point(
        index: Int,
        buyPercent: Double,
        totalNotional: Double,
        price: Double,
        acceleration: Double = 1.0,
        priceChange60s: Double = 0.02
    ) = LiveBreathingSample(
        at = now - (159 - index) * 15_000L,
        priceUsdt = price,
        pumpBuyerPercent = buyPercent,
        pumpChange60sPercent = priceChange60s,
        bookImbalance = 0.0,
        bitcoinBuyerPercent = 50.0,
        bitcoinChange60sPercent = 0.0,
        pumpBuyNotional60s = totalNotional * buyPercent / 100.0,
        pumpSellNotional60s = totalNotional * (100.0 - buyPercent) / 100.0,
        pumpTrades60s = 100,
        tradeAcceleration = acceleration
    )
}
