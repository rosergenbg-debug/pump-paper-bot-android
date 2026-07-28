package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V315RetentionTest {
    @Test
    fun `manual trade journal keeps six months and an open position`() {
        val now = ManualPositionStore.RETENTION_MILLIS + 10_000L
        val oldClosed = ManualTrade(1L, 1.0, 2L, 1.1)
        val recentClosed = ManualTrade(
            now - ManualPositionStore.RETENTION_MILLIS + 1L,
            1.0,
            now - ManualPositionStore.RETENTION_MILLIS + 2L,
            1.1
        )
        val open = ManualTrade(3L, 1.0)

        val kept = ManualPositionStore.retained(listOf(oldClosed, recentClosed, open), now)

        assertFalse(kept.contains(oldClosed))
        assertTrue(kept.contains(recentClosed))
        assertTrue(kept.contains(open))
    }

    @Test
    fun `technical csv retains only last twenty four hours`() {
        val cutoff = 100L
        val lines = listOf(
            "observed_at_ms,value",
            "99,old",
            "100,boundary",
            "101,new"
        )

        assertEquals(
            listOf("observed_at_ms,value", "100,boundary", "101,new"),
            RollingCsvRetention.retain(lines, cutoff)
        )
    }

    @Test
    fun `weak Gemini buy becomes hold while strong buy passes`() {
        val weak = GeminiHourlyRecommendation(
            "BUY", 29, 90, 1, "Слабое направление", emptyList(), "test"
        )
        val strong = weak.copy(directionScore = 40, confidence = 70)

        assertEquals("HOLD", GeminiExecutionPolicy.apply(weak, false).action)
        assertEquals("BUY", GeminiExecutionPolicy.apply(strong, false).action)
        assertEquals("BUY", GeminiExecutionPolicy.apply(weak, true).action)
    }

    @Test
    fun `urgent personal exit bypasses quiet hours but buy does not`() {
        assertTrue(PersonalExitAlertPolicy.bypassesQuietHours("SELL", -99))
        assertTrue(PersonalExitAlertPolicy.bypassesQuietHours("SELL", -100))
        assertFalse(PersonalExitAlertPolicy.bypassesQuietHours("SELL", -98))
        assertFalse(PersonalExitAlertPolicy.bypassesQuietHours("BUY", 100))
    }

    @Test
    fun `manual buy and sell controls are always mutually exclusive`() {
        val waitingForBuy = ManualPositionControlPolicy.forWaitMode("BUY")
        assertTrue(waitingForBuy.buyEnabled)
        assertFalse(waitingForBuy.sellEnabled)

        val positionOpen = ManualPositionControlPolicy.forWaitMode("SELL")
        assertFalse(positionOpen.buyEnabled)
        assertTrue(positionOpen.sellEnabled)
    }
}
