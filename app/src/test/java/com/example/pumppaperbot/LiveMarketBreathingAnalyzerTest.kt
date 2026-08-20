package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LiveMarketBreathingAnalyzerTest {
    @Test fun `upper overview bars include exact 20 minute retrospective window`() {
        val now = 2_000_000L
        val samples = (0..80).map { index ->
            LiveBreathingSample(
                at = now - (80 - index) * 15_000L,
                priceUsdt = 1.0 + index * 0.0001,
                pumpBuyerPercent = 56.0,
                pumpChange60sPercent = 0.02,
                bookImbalance = 0.08,
                bitcoinBuyerPercent = 52.0,
                bitcoinChange60sPercent = 0.01
            )
        }

        val result = LiveMarketBreathingAnalyzer.analyze(samples, now)

        assertTrue(result.horizons.any { it.minutes == 20 && it.score != null })
    }

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

    @Test fun `continuous wave keeps history and fades gradually when feed pauses`() {
        val last = 90L * 60L * 1000L
        val samples = (0..89).map { minute ->
            LiveBreathingSample(
                at = last - (89 - minute) * 60_000L,
                priceUsdt = 1.0 + minute * 0.001,
                pumpBuyerPercent = 68.0,
                pumpChange60sPercent = 0.12,
                bookImbalance = 0.12,
                bitcoinBuyerPercent = 50.0,
                bitcoinChange60sPercent = 0.0
            )
        }
        val fresh = LiveMarketBreathingAnalyzer.analyze(samples, last).flowWave
        val paused = LiveMarketBreathingAnalyzer.analyze(samples, last + 5L * 60_000L).flowWave

        assertTrue(paused.points.size >= fresh.points.size)
        assertTrue((paused.latest?.score15m ?: 0) > 0)
        assertTrue((paused.latest?.score15m ?: 0) < (fresh.latest?.score15m ?: 0))
        assertTrue((paused.latest?.score360m ?: 0) >= (paused.latest?.score15m ?: 0))
        assertTrue(paused.state.contains("ПРИОСТАНОВЛЕНА"))
    }

    @Test fun `three hour layer is present and one bad minute does not erase long waves`() {
        val now = 4L * 60L * 60L * 1000L
        val samples = (0..239).map { minute ->
            LiveBreathingSample(
                at = now - (239 - minute) * 60_000L,
                priceUsdt = 1.0 + minute * 0.0004,
                pumpBuyerPercent = if (minute == 239) 8.0 else 61.0,
                pumpChange60sPercent = if (minute == 239) -1.2 else 0.06,
                bookImbalance = if (minute == 239) -0.9 else 0.08,
                bitcoinBuyerPercent = 51.0,
                bitcoinChange60sPercent = 0.0
            )
        }

        val latest = LiveMarketBreathingAnalyzer.analyze(samples, now).flowWave.latest
        assertNotNull(latest)
        assertTrue(latest!!.score180m > 0)
        assertTrue(latest.score360m > 0)
    }

    @Test fun `bitcoin context is capped and cannot manufacture pump wave`() {
        val pulse = LiveMarketBreathingAnalyzer.flowPulse(LiveBreathingSample(
            at = 1L,
            priceUsdt = 1.0,
            pumpBuyerPercent = 50.0,
            pumpChange60sPercent = 0.0,
            bookImbalance = 0.0,
            bitcoinBuyerPercent = 100.0,
            bitcoinChange60sPercent = 10.0
        ))

        assertTrue(abs(pulse) <= 5.01)
    }
}
