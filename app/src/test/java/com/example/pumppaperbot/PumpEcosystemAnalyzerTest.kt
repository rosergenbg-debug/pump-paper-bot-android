package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PumpEcosystemAnalyzerTest {
    @Test fun `accelerating migrations and improving fundamentals produce positive background`() {
        val now = 2_000_000_000_000L
        val migrations = buildList {
            repeat(12) { add(now - it * 4L * 60L * 1000L) }
            repeat(12) { add(now - (70L + it * 20L) * 60L * 1000L) }
        }
        val result = PumpEcosystemAnalyzer.analyze(
            now = now,
            migrationTimes = migrations,
            dexVolume24hUsd = 100_000_000.0,
            dexChange1d = 25.0,
            revenue24hUsd = 800_000.0,
            revenueChange1d = 20.0,
            buyback24hUsd = 350_000.0,
            buybackChange1d = 15.0,
            burnedPump24h = 80_000_000.0,
            successfulSources = 4,
            sourceStatus = "4/4"
        )
        assertEquals(12, result.migrations1h)
        assertEquals(100, result.dataQuality)
        assertTrue((result.migrationAcceleration ?: 0.0) > 1.0)
        assertTrue((result.score ?: 0) > 20)
    }

    @Test fun `missing sources remain missing rather than invented as zero`() {
        val result = PumpEcosystemAnalyzer.analyze(
            now = 10_000L,
            migrationTimes = emptyList(),
            dexVolume24hUsd = null,
            dexChange1d = null,
            revenue24hUsd = null,
            revenueChange1d = null,
            buyback24hUsd = null,
            buybackChange1d = null,
            burnedPump24h = null,
            successfulSources = 0,
            sourceStatus = "0/4"
        )
        assertEquals(null, result.score)
        assertEquals(null, result.revenue24hUsd)
        assertEquals(0, result.dataQuality)
    }
}
