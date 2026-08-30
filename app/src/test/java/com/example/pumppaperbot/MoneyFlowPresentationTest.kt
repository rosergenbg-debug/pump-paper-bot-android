package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyFlowPresentationTest {
    private val breathing = LiveMarketBreathingSnapshot(
        fresh = true,
        horizons = listOf(
            LiveBreathingHorizon(
                minutes = 15,
                score = 22,
                priceChangePercent = 0.4,
                buyerPercent = 56.0,
                persistencePercent = 70,
                samples = 15
            )
        )
    )

    @Test fun `fresh mature snapshot exposes exact money through one hour`() {
        val now = 1_000_000L
        val data = MoneyFlowPresentation.from(
            MicroImpulseSnapshot(
                connected = true,
                updatedAt = now,
                buyNotional60s = 120.0,
                sellNotional60s = 80.0,
                buyNotional5m = 600.0,
                sellNotional5m = 400.0,
                buyNotional15m = 1_200.0,
                sellNotional15m = 1_800.0,
                buyNotional30m = 3_500.0,
                sellNotional30m = 3_000.0,
                buyNotional60m = 8_000.0,
                sellNotional60m = 7_000.0,
                flowHistorySeconds = 3_600L
            ),
            breathing,
            now
        )

        assertTrue(data.oneMinute.ready)
        assertTrue(data.fiveMinutes.ready)
        assertTrue(data.fifteenMinutes.ready)
        assertEquals(40.0, data.oneMinute.netUsdt, 0.000001)
        assertEquals(200.0, data.fiveMinutes.netUsdt, 0.000001)
        assertEquals(-600.0, data.fifteenMinutes.netUsdt, 0.000001)
        assertEquals(500.0, data.thirtyMinutes.netUsdt, 0.000001)
        assertEquals(1_000.0, data.sixtyMinutes.netUsdt, 0.000001)
        assertEquals(1.0, data.activityRatio ?: 0.0, 0.000001)
        assertEquals(22, data.flowScore15m)
    }

    @Test fun `fifteen minute money stays in accumulation until enough live history`() {
        val now = 2_000_000L
        val data = MoneyFlowPresentation.from(
            MicroImpulseSnapshot(
                connected = true,
                updatedAt = now,
                buyNotional60s = 100.0,
                sellNotional60s = 90.0,
                buyNotional5m = 500.0,
                sellNotional5m = 450.0,
                buyNotional15m = 1_500.0,
                sellNotional15m = 1_400.0,
                flowHistorySeconds = 11L * 60L
            ),
            breathing,
            now
        )

        assertTrue(data.oneMinute.ready)
        assertTrue(data.fiveMinutes.ready)
        assertFalse(data.fifteenMinutes.ready)
        assertFalse(data.thirtyMinutes.ready)
        assertFalse(data.sixtyMinutes.ready)
        assertEquals(null, data.activityRatio)
    }

    @Test fun `stale micro stream disables all money windows`() {
        val now = 3_000_000L
        val data = MoneyFlowPresentation.from(
            MicroImpulseSnapshot(
                connected = true,
                updatedAt = now - 91_000L,
                buyNotional60s = 100.0,
                sellNotional60s = 20.0,
                buyNotional5m = 500.0,
                sellNotional5m = 100.0,
                buyNotional15m = 1_500.0,
                sellNotional15m = 300.0,
                flowHistorySeconds = 900L
            ),
            breathing,
            now
        )

        assertFalse(data.fresh)
        assertTrue(data.windows.none { it.ready })
        assertTrue(data.state.contains("НЕ СВЕЖИЙ"))
    }

    @Test fun `compact formatter preserves net sign`() {
        assertTrue(MoneyFlowPresentation.compactUsd(12_500.0, true).startsWith("+"))
        assertTrue(MoneyFlowPresentation.compactUsd(-2_000_000.0, true).startsWith("−"))
    }
}
