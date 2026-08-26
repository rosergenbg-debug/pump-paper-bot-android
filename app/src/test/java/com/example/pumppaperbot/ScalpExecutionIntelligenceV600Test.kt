package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScalpExecutionIntelligenceV600Test {
    private fun market(
        bids: List<FusionBookLevel>,
        asks: List<FusionBookLevel>,
        feeRate: Double = 0.0025,
        now: Long = 1_000L
    ): FusionMarketSnapshot {
        val bid = bids.first().price
        val ask = asks.first().price
        val mid = (bid + ask) / 2.0
        return FusionMarketSnapshot(
            configured = true,
            connected = true,
            bid = bid,
            ask = ask,
            mid = mid,
            spreadPercent = (ask - bid) / mid * 100.0,
            bidDepthEur = bids.sumOf { it.notionalEur },
            askDepthEur = asks.sumOf { it.notionalEur },
            bidLevels = bids,
            askLevels = asks,
            feeRate = feeRate,
            feeTier = "Tier test",
            lastSuccess = now
        )
    }

    private fun breathing() = LiveMarketBreathingSnapshot(
        fresh = true,
        instantScore = 12,
        horizons = listOf(
            LiveBreathingHorizon(5, 8, null, null, 70, 20),
            LiveBreathingHorizon(15, 6, null, null, 70, 60),
            LiveBreathingHorizon(30, 4, null, null, 70, 120)
        )
    )

    private fun positiveMicro() = MicroImpulseSnapshot(
        connected = true,
        aggressiveBuyPercent15s = 64.0,
        aggressiveBuyPercent60s = 58.0,
        tradeAcceleration = 1.8,
        priceChange60sPercent = 0.18
    )

    @Test fun `confirmed agreement needs Binance strength and supportive Fusion book`() {
        val bids = listOf(
            FusionBookLevel(1.0000, 900.0),
            FusionBookLevel(0.9998, 700.0),
            FusionBookLevel(0.9996, 600.0),
            FusionBookLevel(0.9994, 500.0),
            FusionBookLevel(0.9992, 400.0)
        )
        val asks = listOf(
            FusionBookLevel(1.0005, 500.0),
            FusionBookLevel(1.0007, 450.0),
            FusionBookLevel(1.0009, 400.0),
            FusionBookLevel(1.0011, 350.0),
            FusionBookLevel(1.0013, 300.0)
        )
        val result = ScalpExecutionPolicyV600.evaluate(
            market(bids, asks), positiveMicro(), breathing(), null, "PM1_CAND", 1_000L
        ).first
        assertEquals("CONFIRMED", result.agreement)
        assertTrue(result.executionScore > 50)
        assertTrue(result.shadowOnly)
        assertNotNull(result.costFloorBps)
        assertTrue(result.costFloorBps!! > 50.0)
    }

    @Test fun `divergence is detected when Binance buys but Fusion book is ask heavy`() {
        val bids = listOf(
            FusionBookLevel(1.0000, 300.0),
            FusionBookLevel(0.9998, 300.0),
            FusionBookLevel(0.9996, 300.0),
            FusionBookLevel(0.9994, 300.0),
            FusionBookLevel(0.9992, 300.0)
        )
        val asks = listOf(
            FusionBookLevel(1.0005, 1500.0),
            FusionBookLevel(1.0007, 1400.0),
            FusionBookLevel(1.0009, 1300.0),
            FusionBookLevel(1.0011, 1200.0),
            FusionBookLevel(1.0013, 1100.0)
        )
        val result = ScalpExecutionPolicyV600.evaluate(
            market(bids, asks), positiveMicro(), breathing(), null, "PM1_CAND", 1_000L
        ).first
        assertEquals("DIVERGENT", result.agreement)
        assertTrue((result.imbalance5 ?: 0.0) < 0.0)
    }

    @Test fun `wide Fusion spread is classified as bad execution`() {
        val bids = listOf(FusionBookLevel(1.0000, 2_000.0))
        val asks = listOf(FusionBookLevel(1.0100, 2_000.0))
        val result = ScalpExecutionPolicyV600.evaluate(
            market(bids, asks), positiveMicro(), breathing(), null, "PM1_CAND", 1_000L
        ).first
        assertEquals("BAD_EXECUTION", result.agreement)
        assertTrue(result.spreadBps > 50.0)
    }

    @Test fun `depth slippage is zero on one deep level and unavailable when depth is insufficient`() {
        assertEquals(
            0.0,
            ScalpExecutionPolicyV600.buySlippageBps(
                listOf(FusionBookLevel(1.0, 1_000.0)), 500.0, 1.0
            )!!,
            0.000001
        )
        assertNull(
            ScalpExecutionPolicyV600.buySlippageBps(
                listOf(FusionBookLevel(1.0, 100.0)), 500.0, 1.0
            )
        )
    }

    @Test fun `book memory only affects depth change while it is fresh`() {
        val bids = listOf(FusionBookLevel(1.0000, 2_000.0))
        val asks = listOf(FusionBookLevel(1.0005, 2_000.0))
        val current = market(bids, asks, now = 100_000L)
        val freshPrior = FusionBookMemoryV600(
            at = 50_000L,
            top5BidEur = 500.0,
            top5AskEur = 3_000.0
        )
        val withFresh = ScalpExecutionPolicyV600.evaluate(
            current, positiveMicro(), breathing(), freshPrior, "FAST", 100_000L
        ).first
        assertNotNull(withFresh.bidDepthChangePercent)
        assertNotNull(withFresh.askDepthChangePercent)

        val stalePrior = freshPrior.copy(at = 1_000L)
        val withStale = ScalpExecutionPolicyV600.evaluate(
            current, positiveMicro(), breathing(), stalePrior, "FAST", 100_000L
        ).first
        assertNull(withStale.bidDepthChangePercent)
        assertNull(withStale.askDepthChangePercent)
    }
}
