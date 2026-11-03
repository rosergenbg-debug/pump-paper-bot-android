package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Test

class FusionMarketMetricsTest {
    @Test fun spreadComputedFromExecutableBidAsk() {
        val bid = 0.003719
        val ask = 0.003723
        assertEquals(0.000004, FusionMarketMetrics.spreadEuro(bid, ask), 1e-12)
        assertEquals((ask - bid) / ((ask + bid) / 2.0) * 100.0,
            FusionMarketMetrics.spreadPercent(bid, ask), 1e-9)
    }

    @Test fun spreadZeroWhenBidEqualsAsk() {
        assertEquals(0.0, FusionMarketMetrics.spreadPercent(1.0, 1.0), 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invertedBookIsRejected() {
        FusionMarketMetrics.spreadPercent(1.1, 1.0)
    }
}
