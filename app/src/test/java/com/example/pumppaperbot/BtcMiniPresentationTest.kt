package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BtcMiniPresentationTest {
    @Test fun `two six and twenty four hour changes use causal earlier closes`() {
        val hour = 60L * 60L * 1_000L
        val now = 30L * hour
        val candles = (0..60).map { index ->
            val at = index * hour / 2L
            candle(at, 100.0 + index)
        }
        val data = BtcMiniPresentation.from(candles, 160.0, now, now)

        assertEquals((160.0 / 156.0 - 1.0) * 100.0, data.change2h!!, 0.000001)
        assertEquals((160.0 / 148.0 - 1.0) * 100.0, data.change6h!!, 0.000001)
        assertEquals((160.0 / 112.0 - 1.0) * 100.0, data.change24h!!, 0.000001)
        assertTrue(data.fresh)
    }

    @Test fun `old candles are visibly stale`() {
        val now = 10L * 60L * 60L * 1_000L
        val data = BtcMiniPresentation.from(listOf(candle(1L, 100.0)), null, 0L, now)
        assertFalse(data.fresh)
    }

    private fun candle(at: Long, close: Double) = PumpCandle(
        openTime = at - 1L,
        open = close,
        high = close,
        low = close,
        close = close,
        volume = 1.0,
        closeTime = at
    )
}
