package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekFreshMarketContextTest {
    @Test fun `fresh live quote is preferred over closed candle`() {
        val now = 1_000_000L
        val snapshot = snapshot(livePrice = 0.0042, livePriceAt = now - 30_000L)

        assertEquals(0.0042, DeepSeekFreshMarketContext.analysisPrice(snapshot, now), 0.0)
    }

    @Test fun `stale live quote falls back to closed candle`() {
        val now = 1_000_000L
        val snapshot = snapshot(
            livePrice = 0.0042,
            livePriceAt = now - DeepSeekFreshMarketContext.LIVE_PRICE_MAX_AGE - 1L
        )

        assertEquals(snapshot.lastPrice, DeepSeekFreshMarketContext.analysisPrice(snapshot, now), 0.0)
    }

    @Test fun `future and missing timestamps are never fresh`() {
        assertFalse(DeepSeekFreshMarketContext.isFresh(0L, 1_000L, 500L))
        assertFalse(DeepSeekFreshMarketContext.isFresh(1_001L, 1_000L, 500L))
        assertTrue(DeepSeekFreshMarketContext.isFresh(500L, 1_000L, 500L))
    }

    private fun snapshot(livePrice: Double?, livePriceAt: Long) = LiveSnapshot(
        running = true,
        waitMode = "BUY",
        buyRsi = 45.0,
        lastSync = 900_000L,
        lastCandle = 800_000L,
        lastPrice = 0.0038,
        lastRsi = 50.0,
        lastEma200 = 0.0030,
        fundingRate = 0.0,
        strategyMode = StrategyV2.MODE_NONE,
        aggressive = false,
        readinessScore = 0,
        trendReadiness = 0,
        shockReadiness = 0,
        partialTaken = false,
        buySignal = false,
        sellSignal = false,
        signalAction = "WAIT",
        signalReason = "test",
        entryPrice = 0.0,
        entryTime = 0L,
        highestClose = 0.0,
        chart = ChartBundle(emptyList(), emptyList(), emptyList(), emptyList(), "test"),
        livePrice = livePrice,
        livePriceAt = livePriceAt
    )
}
