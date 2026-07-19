package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RapidDropDetectorTest {
    @Test
    fun `does not alarm before 25 percent`() {
        val candles = listOf(
            candle(0, 100.0, 100.0, 98.0, 99.0, buyerShare = 0.55),
            candle(1, 90.0, 91.0, 82.0, 84.0),
            candle(2, 80.0, 81.0, 75.0, 76.0)
        )

        assertFalse(RapidDropDetector.detect(candles, 75.01).active)
    }

    @Test
    fun `alarms at 25 percent and reports exact fall`() {
        val candles = listOf(
            candle(0, 100.0, 100.0, 98.0, 99.0),
            candle(1, 90.0, 91.0, 80.0, 82.0),
            candle(2, 80.0, 81.0, 74.0, 75.0)
        )

        val result = RapidDropDetector.detect(candles, 75.0)

        assertTrue(result.active)
        assertEquals(25.0, result.dropPercent, 0.0001)
        assertEquals(25, result.severityBand)
        assertFalse(result.recoveryConfirmed)
    }

    @Test
    fun `recovery needs rebound green candle and buyer flow`() {
        val candles = listOf(
            candle(0, 100.0, 100.0, 98.0, 99.0, buyerShare = 0.55),
            candle(1, 80.0, 82.0, 70.0, 72.0, buyerShare = 0.55),
            candle(2, 71.0, 74.0, 70.0, 73.0, buyerShare = 0.60)
        )

        val result = RapidDropDetector.detect(candles, 73.0)

        assertTrue(result.active)
        assertTrue(result.reboundPercent >= 3.0)
        assertTrue(result.recoveryConfirmed)
    }

    @Test
    fun `severity only rings again at meaningful levels`() {
        assertEquals(25, RapidDropDetector.severityBand(34.9))
        assertEquals(35, RapidDropDetector.severityBand(49.9))
        assertEquals(50, RapidDropDetector.severityBand(64.9))
        assertEquals(65, RapidDropDetector.severityBand(79.9))
        assertEquals(80, RapidDropDetector.severityBand(88.0))
    }

    @Test
    fun `same crash level is quiet for twelve hours but deeper fall rings`() {
        assertFalse(RapidDropDetector.shouldNotify(25, 25, 60L * 60L * 1000L))
        assertTrue(RapidDropDetector.shouldNotify(35, 25, 60L * 60L * 1000L))
        assertTrue(
            RapidDropDetector.shouldNotify(
                25,
                25,
                RapidDropDetector.SAME_LEVEL_COOLDOWN_MILLIS
            )
        )
    }

    private fun candle(
        index: Int,
        open: Double,
        high: Double,
        low: Double,
        close: Double,
        buyerShare: Double = 0.40
    ) = PumpCandle(
        openTime = index * 1_800_000L,
        open = open,
        high = high,
        low = low,
        close = close,
        volume = 1_000.0,
        closeTime = (index + 1) * 1_800_000L - 1L,
        takerBuyVolume = 1_000.0 * buyerShare
    )
}
