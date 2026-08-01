package com.example.pumppaperbot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrategyV319EntrySensitivityTest {
    @Test
    fun `cautious trend accepts a slightly faster RSI recovery`() {
        assertTrue(StrategyV2.EntrySensitivity.trendRecovered(56.5, 44.5, 42.0, false))
        assertFalse(StrategyV2.EntrySensitivity.trendRecovered(58.0, 44.5, 42.0, false))
    }

    @Test
    fun `active trend keeps recovery valid for one additional closed candle`() {
        assertTrue(StrategyV2.EntrySensitivity.trendRecovered(54.0, 47.0, 44.0, true))
        assertFalse(StrategyV2.EntrySensitivity.trendRecovered(54.0, 47.0, 44.0, false))
    }

    @Test
    fun `active shock threshold is more responsive but still requires strong volume`() {
        assertTrue(2.6 >= StrategyV2.EntrySensitivity.shockArmVolumeRatio(true))
        assertFalse(2.6 >= StrategyV2.EntrySensitivity.shockArmVolumeRatio(false))
        assertTrue(StrategyV2.EntrySensitivity.shockRsiReady(43.5, true))
        assertFalse(StrategyV2.EntrySensitivity.shockRsiReady(43.5, false))
    }
}
