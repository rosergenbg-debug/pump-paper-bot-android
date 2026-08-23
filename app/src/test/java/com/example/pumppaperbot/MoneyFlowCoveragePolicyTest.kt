package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyFlowCoveragePolicyTest {
    @Test
    fun `continuous minute history survives socket reconnect semantics`() {
        val currentKey = 20_000L
        val now = currentKey * 60_000L + 30_000L
        val keys = (currentKey - 12L..currentKey).toList()

        val seconds = MoneyFlowCoveragePolicy.continuousSeconds(keys, now)

        assertEquals(12L * 60L + 30L, seconds)
        assertTrue(seconds >= 12L * 60L)
    }

    @Test
    fun `real gap resets coverage instead of pretending full fifteen minutes`() {
        val currentKey = 30_000L
        val now = currentKey * 60_000L + 20_000L
        val keys = listOf(currentKey, currentKey - 1L, currentKey - 3L, currentKey - 4L, currentKey - 5L)

        assertEquals(80L, MoneyFlowCoveragePolicy.continuousSeconds(keys, now))
    }

    @Test
    fun `previous completed minute can anchor restored history before first new trade`() {
        val currentKey = 40_000L
        val now = currentKey * 60_000L + 5_000L
        val keys = (currentKey - 5L..currentKey - 1L).toList()

        assertEquals(5L * 60L, MoneyFlowCoveragePolicy.continuousSeconds(keys, now))
    }
}
