package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketBreathingAnalyzerTest {
    @Test
    fun lateEntryRiskIsHighNearPeakAfterFastRise() {
        val candles = candles(count = 120) { index ->
            if (index < 113) 100.0 else 100.0 + (index - 112) * 1.5
        }
        val rsi = MutableList<Double?>(candles.size) { 50.0 }.apply { this[lastIndex] = 68.0 }
        val ema20 = MutableList<Double?>(candles.size) { 100.0 }

        val risk = MarketBreathingAnalyzer.lateEntryRiskAt(candles.lastIndex, candles, rsi, ema20)

        assertTrue("peak risk was $risk", risk >= 70)
    }

    @Test
    fun lateEntryRiskStaysLowNearBottom() {
        val candles = candles(count = 120) { index ->
            when {
                index < 80 -> 110.0
                else -> 110.0 - (index - 79) * 0.45
            }
        }
        val rsi = MutableList<Double?>(candles.size) { 38.0 }
        val ema20 = MutableList<Double?>(candles.size) { 100.0 }

        val risk = MarketBreathingAnalyzer.lateEntryRiskAt(candles.lastIndex, candles, rsi, ema20)

        assertTrue("bottom risk was $risk", risk <= 25)
    }

    @Test
    fun breathingCalculationDoesNotReadFutureCandles() {
        val full = candles(count = 240) { index ->
            when {
                index <= 210 -> 100.0 + index * 0.01
                else -> 180.0 + index
            }
        }
        val index = 210
        val truncated = full.take(index + 1)
        val rsiFull = MutableList<Double?>(full.size) { 52.0 }
        val emaFull = MutableList<Double?>(full.size) { 100.0 }
        val rsiShort = rsiFull.take(index + 1)
        val emaShort = emaFull.take(index + 1)

        val fromFull = MarketBreathingAnalyzer.analyzeAt(
            index, full, full, full, full, rsiFull, emaFull
        )
        val fromTruncated = MarketBreathingAnalyzer.analyzeAt(
            index, truncated, truncated, truncated, truncated, rsiShort, emaShort
        )

        assertEquals(fromTruncated.energyScore, fromFull.energyScore)
        assertEquals(fromTruncated.compressionScore, fromFull.compressionScore)
        assertEquals(fromTruncated.directionScore, fromFull.directionScore)
        assertEquals(fromTruncated.lateEntryRisk, fromFull.lateEntryRisk)
        assertEquals(fromTruncated.state, fromFull.state)
    }

    private fun candles(count: Int, closeAt: (Int) -> Double): List<PumpCandle> {
        val start = 1_700_000_000_000L
        val interval = 30L * 60L * 1000L
        return List(count) { index ->
            val close = closeAt(index)
            val volume = if (index >= count - 2) 5_000.0 else 1_000.0
            PumpCandle(
                openTime = start + index * interval,
                open = close,
                high = close * 1.001,
                low = close * 0.999,
                close = close,
                volume = volume,
                closeTime = start + (index + 1) * interval - 1L,
                quoteVolume = close * volume,
                tradeCount = if (index >= count - 2) 1_000 else 100,
                takerBuyVolume = volume * 0.70
            )
        }
    }
}
