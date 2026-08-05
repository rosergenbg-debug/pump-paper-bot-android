package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LiveMarketBreathingAnalyzerTest {
    @Test fun `single violent tick cannot overturn sustained positive breathing`() {
        val now = 20L * 60L * 1000L
        val samples = (0..79).map { index ->
            LiveBreathingSample(
                at = now - (79 - index) * 15_000L,
                priceUsdt = 1.0 + index * 0.0008,
                pumpBuyerPercent = 58.0,
                pumpChange60sPercent = 0.08,
                bookImbalance = 0.08,
                bitcoinBuyerPercent = 53.0,
                bitcoinChange60sPercent = 0.01
            )
        }.toMutableList()
        samples[samples.lastIndex] = samples.last().copy(
            pumpBuyerPercent = 20.0,
            pumpChange60sPercent = -0.8,
            bookImbalance = -0.8
        )

        val result = LiveMarketBreathingAnalyzer.analyze(samples, now)

        assertTrue((result.instantScore ?: 0) < 0)
        assertTrue((result.normalScore ?: 0) > 0)
        assertTrue(result.regime.contains("ПОКУПАТЕЛЕЙ") || result.regime.contains("УЛУЧШЕНИЕ"))
    }

    @Test fun `experiment stays nervous but never exceeds fifteen point gap`() {
        val now = 40L * 60L * 1000L
        val samples = (0..119).map { index ->
            LiveBreathingSample(
                at = now - (119 - index) * 15_000L,
                priceUsdt = 1.0 + index * 0.0002,
                pumpBuyerPercent = if (index == 119) 82.0 else 53.0,
                pumpChange60sPercent = if (index == 119) 0.9 else 0.03,
                bookImbalance = 0.04,
                bitcoinBuyerPercent = 51.0,
                bitcoinChange60sPercent = 0.0
            )
        }

        val result = LiveMarketBreathingAnalyzer.analyze(samples, now)

        assertNotNull(result.normalScore)
        assertNotNull(result.experimentScore)
        assertTrue(abs(result.experimentScore!! - result.normalScore!!) <= 15)
        assertTrue(result.experimentScore!! >= result.normalScore!!)
    }

    @Test fun `observations older than twenty four hours are excluded`() {
        val now = 30L * 60L * 60L * 1000L
        val old = LiveBreathingSample(
            at = now - RollingCsvRetention.RETENTION_MILLIS - 1L,
            priceUsdt = 2.0,
            pumpBuyerPercent = 5.0,
            pumpChange60sPercent = -2.0,
            bookImbalance = -1.0,
            bitcoinBuyerPercent = 5.0,
            bitcoinChange60sPercent = -1.0
        )
        val current = listOf(
            old,
            old.copy(at = now - 15_000L, priceUsdt = 1.0, pumpBuyerPercent = 60.0),
            old.copy(at = now, priceUsdt = 1.01, pumpBuyerPercent = 60.0)
        )

        val result = LiveMarketBreathingAnalyzer.analyze(current, now)

        assertEquals(0, result.historyMinutes)
        assertTrue((result.normalScore ?: 0) > 0)
    }
}
