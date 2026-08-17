package com.example.pumppaperbot

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BitpandaFusionTest {
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
}
