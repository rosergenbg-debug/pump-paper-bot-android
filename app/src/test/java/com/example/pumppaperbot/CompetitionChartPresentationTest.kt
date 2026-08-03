package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompetitionChartPresentationTest {
    @Test
    fun `fresh manual exit is inside display range before 30 minute candle closes`() {
        val currentOpen = 12L * 30L * 60L * 1_000L
        val exitAt = currentOpen + 10L * 60L * 1_000L
        val previousClose = currentOpen - 1L
        val previous = candle(
            openTime = currentOpen - 30L * 60L * 1_000L,
            closeTime = previousClose,
            close = 0.0020
        )

        val result = CompetitionChartPresentation.withLiveEdge(
            listOf(previous),
            livePrice = 0.0021,
            now = exitAt
        )

        assertEquals(2, result.size)
        assertEquals(currentOpen, result.last().openTime)
        assertTrue(exitAt in result.first().openTime..result.last().closeTime)
        assertEquals(0.0021, result.last().close, 0.0)
    }

    @Test
    fun `live edge updates current display candle without changing closed history`() {
        val currentOpen = 20L * 30L * 60L * 1_000L
        val currentClose = currentOpen + 30L * 60L * 1_000L - 1L
        val current = candle(currentOpen, currentClose, close = 0.0020)

        val result = CompetitionChartPresentation.withLiveEdge(
            listOf(current),
            livePrice = 0.0022,
            now = currentOpen + 5_000L
        )

        assertEquals(1, result.size)
        assertEquals(0.0022, result.single().close, 0.0)
        assertEquals(0.0022, result.single().high, 0.0)
    }

    private fun candle(openTime: Long, closeTime: Long, close: Double) = PumpCandle(
        openTime = openTime,
        open = close,
        high = close,
        low = close,
        close = close,
        volume = 1.0,
        closeTime = closeTime
    )
}
