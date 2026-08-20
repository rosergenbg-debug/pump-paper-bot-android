package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LargeFlowFingerprintTest {
    @Test fun `repeated similar large buys form anonymous buy series`() {
        val now = 1_000_000L
        val normal = (0 until 80).map { index ->
            MicroTrade(now - 250_000L + index * 2_000L, 1.0, 1_000.0, index % 2 == 0)
        }
        val slices = (0 until 6).map { index ->
            MicroTrade(now - 60_000L + index * 6_000L, 1.0 + index * 0.0005, 20_000.0, true)
        }
        val result = LargeFlowFingerprintPolicy.evaluate(normal + slices, now, 1.01)
        assertEquals(LargeFlowMode.BUY_SERIES, result.mode)
        assertTrue(result.buySlices >= 4)
        assertTrue(result.largeBuyUsdt >= 100_000.0)
        assertTrue(result.fingerprint.contains("BUY-серия"))
    }

    @Test fun `large buys without price response are labelled absorbed`() {
        val now = 1_000_000L
        val normal = (0 until 50).map { index ->
            MicroTrade(now - 200_000L + index * 2_000L, 1.0, 500.0, index % 2 == 0)
        }
        val buys = (0 until 5).map { index ->
            MicroTrade(now - 50_000L + index * 5_000L, 1.0, 15_000.0, true)
        }
        val result = LargeFlowFingerprintPolicy.evaluate(normal + buys, now, 1.0)
        assertEquals(LargeFlowMode.BUY_ABSORBED, result.mode)
        assertTrue(result.explanation.contains("не BUY"))
    }

    @Test fun `visible fingerprint never claims a known owner`() {
        val text = LargeFlowFingerprintText.describe(LargeFlowFingerprint())
        assertTrue(text.contains("не показывает имя владельца"))
        assertTrue(text.contains("не доказательство"))
    }
}
