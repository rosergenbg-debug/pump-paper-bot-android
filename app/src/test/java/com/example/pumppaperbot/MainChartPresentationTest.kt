package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainChartPresentationTest {
    private fun candle(low: Double, high: Double) = PumpCandle(
        openTime = 1L,
        open = (low + high) / 2.0,
        high = high,
        low = low,
        close = (low + high) / 2.0,
        volume = 0.0,
        closeTime = 2L
    )

    @Test fun `main window is based on candle prices only`() {
        val window = MainChartViewportPolicy.candleWindow(
            listOf(candle(0.00310, 0.00340), candle(0.00320, 0.00344))
        )
        assertNotNull(window)
        window!!
        assertTrue(window.minPrice > 0.0029)
        assertTrue(window.maxPrice < 0.0037)
        // A hypothetical EMA200 at 0.00268 therefore cannot compress the candles.
        assertTrue(0.00268 < window.minPrice)
    }

    @Test fun `vertical pan moves window without changing zoom`() {
        val candles = listOf(candle(0.00310, 0.00344))
        val normal = MainChartViewportPolicy.candleWindow(candles, 0f)!!
        val shifted = MainChartViewportPolicy.candleWindow(candles, 0.5f)!!
        assertEquals(normal.span, shifted.span, 0.0000000001)
        assertTrue(shifted.minPrice > normal.minPrice)
        assertTrue(shifted.maxPrice > normal.maxPrice)
    }

    @Test fun `four bars use exact instant 5 15 and 30 minute upper values`() {
        val snapshot = LiveMarketBreathingSnapshot(
            fresh = true,
            instantScore = -43,
            horizons = listOf(
                LiveBreathingHorizon(5, 10, null, null, 55, 20),
                LiveBreathingHorizon(15, 49, null, null, 68, 57),
                LiveBreathingHorizon(20, 32, null, null, 74, 77),
                LiveBreathingHorizon(30, 12, null, null, 60, 116)
            )
        )
        val bars = MainChartFlowPresentation.from(snapshot)
        assertTrue(bars.fresh)
        assertEquals(-43, bars.instant)
        assertEquals(10, bars.fiveMinutes)
        assertEquals(49, bars.fifteenMinutes)
        assertEquals(12, bars.thirtyMinutes)
    }

    @Test fun `stale breathing renders neutral bars`() {
        val bars = MainChartFlowPresentation.from(
            LiveMarketBreathingSnapshot(fresh = false, instantScore = 90)
        )
        assertFalse(bars.fresh)
        assertEquals(null, bars.instant)
        assertEquals(null, bars.fiveMinutes)
        assertEquals(null, bars.fifteenMinutes)
        assertEquals(null, bars.thirtyMinutes)
    }
}
