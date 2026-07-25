package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImpulseRadarTest {
    @Test
    fun identifiesResearchCandidateButDoesNotCreateTradingAction() {
        val pump = candles(pump = true, finalVolume = 4_000.0)
        val peers = candles(pump = false, finalVolume = 1_000.0)
        val futures = candles(pump = true, finalVolume = 4_000.0, takerRatio = 0.70)

        val result = ImpulseRadarAnalyzer.analyze(pump, peers, peers, futures, "[]")

        assertTrue(result.candidate)
        assertEquals(100, result.readiness)
        assertTrue(result.status.contains("BUY ЗАПРЕЩЁН"))
    }

    @Test
    fun ordinaryVolumeStaysInShadowObservation() {
        val pump = candles(pump = true, finalVolume = 1_000.0)
        val peers = candles(pump = false, finalVolume = 1_000.0)
        val futures = candles(pump = true, finalVolume = 1_000.0, takerRatio = 0.70)

        val result = ImpulseRadarAnalyzer.analyze(pump, peers, peers, futures, "[]")

        assertFalse(result.candidate)
        assertTrue(result.readiness < 100)
        assertTrue(result.status.contains("ТОРГОВЫЙ СИГНАЛ НЕ МЕНЯЕТСЯ"))
    }

    private fun candles(
        pump: Boolean,
        finalVolume: Double,
        takerRatio: Double = 0.75
    ): List<PumpCandle> {
        val start = 1_700_000_000_000L
        val interval = 5L * 60L * 1000L
        return (0 until 140).map { index ->
            val flat = index >= 125
            val close = when {
                !pump -> 100.0
                index == 139 -> 100.8
                flat -> 100.0 + (index % 2) * 0.01
                else -> 100.0 + (index % 4) * 0.35
            }
            val volume = if (index == 139) finalVolume else 1_000.0
            val flow = if (index >= 138) takerRatio else 0.52
            PumpCandle(
                openTime = start + index * interval,
                open = if (index == 139) 100.1 else close,
                high = if (index == 139) 100.9 else close + if (flat) 0.01 else 0.25,
                low = close - if (flat) 0.01 else 0.25,
                close = close,
                volume = volume,
                closeTime = start + (index + 1) * interval - 1L,
                quoteVolume = close * volume,
                tradeCount = 100,
                takerBuyVolume = volume * flow
            )
        }
    }
}
