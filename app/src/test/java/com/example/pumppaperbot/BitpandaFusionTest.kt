package com.example.pumppaperbot

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BitpandaFusionTest {
    private fun upperBarSnapshot(
        instant: Int,
        five: Int,
        fifteen: Int,
        twenty: Int,
        thirty: Int,
        fresh: Boolean = true
    ) = LiveMarketBreathingSnapshot(
        fresh = fresh,
        instantScore = instant,
        horizons = listOf(
            LiveBreathingHorizon(5, five, null, null, 70, 20),
            LiveBreathingHorizon(15, fifteen, null, null, 70, 60),
            LiveBreathingHorizon(20, twenty, null, null, 70, 80),
            LiveBreathingHorizon(30, thirty, null, null, 70, 120)
        )
    )

    @Test fun `Fusion flow buys only when instant 5 15 and 30 are positive`() {
        assertEquals("BUY", FusionFlowPolicy.decide(
            false, upperBarSnapshot(8, 5, 3, -2, 1)
        )?.action)
        assertEquals(null, FusionFlowPolicy.decide(
            false, upperBarSnapshot(8, 5, 3, 2, 0)
        ))
    }

    @Test fun `Fusion exits early when instant 5 15 and exact 20m are negative`() {
        val exit = FusionFlowPolicy.decide(true, upperBarSnapshot(-5, -4, -3, -1, 20))
        assertEquals("EXIT", exit?.action)
        assertEquals(20, exit?.score30m)
        assertEquals(null, FusionFlowPolicy.decide(
            true, upperBarSnapshot(-5, -4, -3, 1, -20)
        ))
    }

    @Test fun `Fusion flow refuses stale data`() {
        assertEquals(null, FusionFlowPolicy.decide(
            false, upperBarSnapshot(10, 10, 10, 10, 10, fresh = false)
        ))
    }

    @Test fun `Fusion ignores contradictory lower flow clock values`() {
        val upper = upperBarSnapshot(12, 9, 7, 5, 4).copy(
            flowWave = LiveFlowWave(points = listOf(LiveFlowWavePoint(
                at = 1L,
                score15m = -80,
                score30m = -80,
                score60m = -80,
                score180m = -80,
                score360m = -80,
                score5m = -80,
                score20m = -80
            )))
        )

        assertEquals("BUY", FusionFlowPolicy.decide(false, upper)?.action)
    }

    @Test fun `Fusion waits until every required upper bar exists`() {
        val missing20 = upperBarSnapshot(-8, -7, -6, -5, 20).copy(
            horizons = upperBarSnapshot(-8, -7, -6, -5, 20).horizons
                .filterNot { it.minutes == 20 }
        )

        assertEquals(null, FusionFlowPolicy.decide(true, missing20))
    }

    @Test fun `orderbook parser uses best bid ask and computes spread and depth`() {
        val snapshot = BitpandaFusionClient.parseOrderbook(
            JSONObject("""{
                "pair":"PUMP-EUR",
                "bids":[{"price":"0.0020","quantity":"100000"},{"price":"0.0019","quantity":"50000"}],
                "asks":[{"price":"0.0022","quantity":"80000"},{"price":"0.0023","quantity":"40000"}]
            }"""),
            now = 1234L
        )
        assertEquals(0.0020, snapshot.bid, 0.00000001)
        assertEquals(0.0022, snapshot.ask, 0.00000001)
        assertEquals(0.0021, snapshot.mid, 0.00000001)
        assertEquals(9.5238095, snapshot.spreadPercent, 0.0001)
        assertEquals(295.0, snapshot.bidDepthEur, 0.0001)
        assertEquals(268.0, snapshot.askDepthEur, 0.0001)
        assertTrue(snapshot.connected)
    }

    @Test fun `fusion sim buys at ask sells at bid and charges both fees`() {
        val bought = FusionSimTrader.apply(
            FusionSimPortfolio(), 10L, "BUY", bid = 0.0020, ask = 0.0022,
            feeRate = 0.0015, reason = "test", now = 100L
        )
        assertTrue(bought.inPosition)
        assertEquals(0.0022, bought.trades.single().price, 0.0)
        assertEquals(0.0, bought.cashEur, 0.0)

        val sold = FusionSimTrader.apply(
            bought, 11L, "SELL", bid = 0.0021, ask = 0.0023,
            feeRate = 0.0015, reason = "test", now = 200L
        )
        assertFalse(sold.inPosition)
        assertEquals(0.0021, sold.trades.last().price, 0.0)
        assertTrue(sold.cashEur < 1000.0)
        assertTrue(sold.totalFeesEur > 1.5)
    }

    @Test fun `duplicate decision can never execute twice`() {
        val bought = FusionSimTrader.apply(
            FusionSimPortfolio(), 10L, "BUY", 0.0020, 0.0021, 0.0015, "test", 100L
        )
        val duplicate = FusionSimTrader.apply(
            bought, 10L, "SELL", 0.0030, 0.0031, 0.0015, "test", 200L
        )
        assertEquals(bought, duplicate)
    }

    @Test fun `open Fusion position enables one minute Pro priority only`() {
        val inactive = FusionPriorityPolicy.plan(FusionSimPortfolio())
        assertFalse(inactive.active)
        assertFalse(inactive.forcePro)
        assertEquals(120_000L, inactive.intervalMillis)

        val open = FusionSimPortfolio(pumpAmount = 10.0, entryPrice = 1.0, entryCostEur = 10.0)
        val active = FusionPriorityPolicy.plan(open)
        assertTrue(active.active)
        assertTrue(active.forcePro)
        assertEquals(60_000L, active.intervalMillis)
        assertTrue(active.label.contains("МАКСИМАЛЬНЫЙ КОНТРОЛЬ"))

        val now = 1_000_000L
        assertFalse(DeepSeekPrimaryPolicy.shouldRun(
            DeepSeekPrimaryState(lastAttempt = now - 59_999L), true, false, now,
            intervalMillis = active.intervalMillis
        ))
        assertTrue(DeepSeekPrimaryPolicy.shouldRun(
            DeepSeekPrimaryState(lastAttempt = now - 60_000L), true, false, now,
            intervalMillis = active.intervalMillis
        ))
    }

    @Test fun `Fusion priority PnL includes hypothetical exit fee and peak pullback`() {
        val bought = FusionSimTrader.apply(
            FusionSimPortfolio(), 10L, "BUY", bid = 0.0021, ask = 0.0022,
            feeRate = 0.0015, reason = "test", now = 100L
        ).copy(peakValueEur = 1_050.0)
        val metrics = FusionPriorityPolicy.metrics(
            bought, markPriceEur = 0.0022, feeRate = 0.0015, venueFresh = true
        )
        assertTrue(metrics.estimatedExitFeeEur > 0.0)
        assertTrue(metrics.netPnlEur < 0.0)
        assertTrue(metrics.pullbackFromPeakPercent > 4.0)
        assertTrue(metrics.venueFresh)
    }
}
