package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartSpeedPolicyTest {
    @Test fun `new position selects one minute exactly once`() {
        assertTrue(ChartSpeedPolicy.shouldAutoSelectFast(entryTime = 200L, handledEntryTime = 0L))
        assertFalse(ChartSpeedPolicy.shouldAutoSelectFast(entryTime = 200L, handledEntryTime = 200L))
        assertFalse(ChartSpeedPolicy.shouldAutoSelectFast(entryTime = 0L, handledEntryTime = 0L))
    }

    @Test fun `all requested chart speeds are available`() {
        assertEquals(
            listOf("1m", "5m", "15m", "30m", "1h"),
            ChartInterval.entries.map { it.code }
        )
    }

    @Test fun `live price updates the current minute instead of waiting for candle close`() {
        val minute = ChartInterval.ONE_MINUTE.durationMillis
        val now = 10L * minute + 15_000L
        val current = PumpCandle(
            openTime = 10L * minute,
            open = 100.0,
            high = 101.0,
            low = 99.0,
            close = 100.5,
            volume = 1.0,
            closeTime = 11L * minute - 1L
        )

        val updated = ChartSpeedPresentation.withLiveEdge(
            listOf(current), ChartInterval.ONE_MINUTE, livePrice = 102.0, now = now
        )

        assertEquals(1, updated.size)
        assertEquals(102.0, updated.last().close, 0.000001)
        assertEquals(102.0, updated.last().high, 0.000001)
    }

    @Test fun `live price appends a new minute when cached candle is older`() {
        val minute = ChartInterval.ONE_MINUTE.durationMillis
        val old = PumpCandle(
            openTime = 9L * minute,
            open = 100.0,
            high = 101.0,
            low = 99.0,
            close = 100.5,
            volume = 1.0,
            closeTime = 10L * minute - 1L
        )

        val updated = ChartSpeedPresentation.withLiveEdge(
            listOf(old), ChartInterval.ONE_MINUTE, livePrice = 101.5, now = 10L * minute + 1_000L
        )

        assertEquals(2, updated.size)
        assertEquals(10L * minute, updated.last().openTime)
        assertEquals(101.5, updated.last().close, 0.000001)
    }
}
