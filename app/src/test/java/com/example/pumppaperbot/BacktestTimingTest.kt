package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Test

class BacktestTimingTest {
    @Test
    fun `24 hours means exactly 48 thirty minute candles`() {
        val window = backtestHoldWindow(entryIndex = 100, holdBars = 48, lastIndex = 1_000)

        assertEquals(147, window.lastSignalIndex)
        assertEquals(148, window.executionIndex)
    }

    @Test
    fun `48 hours means exactly 96 thirty minute candles`() {
        val window = backtestHoldWindow(entryIndex = 100, holdBars = 96, lastIndex = 1_000)

        assertEquals(195, window.lastSignalIndex)
        assertEquals(196, window.executionIndex)
    }

    @Test
    fun `window never reads past last closed candle`() {
        val window = backtestHoldWindow(entryIndex = 98, holdBars = 48, lastIndex = 100)

        assertEquals(99, window.lastSignalIndex)
        assertEquals(100, window.executionIndex)
    }

    @Test
    fun `live timeout uses wall clock and fires at exactly 24 hours`() {
        val entryTime = 1_700_000_000_000L
        val candle = PumpCandle(
            openTime = entryTime + 23L * 60L * 60L * 1000L,
            open = 100.0,
            high = 101.0,
            low = 99.0,
            close = 100.0,
            volume = 1_000.0,
            closeTime = entryTime + 23L * 60L * 60L * 1000L + 30L * 60L * 1000L - 1L
        )

        val before = StrategyV2.evaluateExit(
            candle, StrategyV2.MODE_TREND, 100.0, entryTime,
            partialTaken = false, storedHighestHigh = 100.0,
            evaluationTime = entryTime + StrategyV2.BASE_MAX_HOLD_MILLIS - 1L
        )
        val onTime = StrategyV2.evaluateExit(
            candle, StrategyV2.MODE_TREND, 100.0, entryTime,
            partialTaken = false, storedHighestHigh = 100.0,
            evaluationTime = entryTime + StrategyV2.BASE_MAX_HOLD_MILLIS
        )

        assertEquals(StrategyV2.ACTION_WAIT, before.action)
        assertEquals(StrategyV2.ACTION_SELL, onTime.action)
    }
}
