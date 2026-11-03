package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Test

class FusionPercentLevelsTest {
    @Test fun pumpTargetsOnePercentFromCurrentBidIndependentReference() {
        val now = 0.00377800
        assertEquals(0.00381578, FusionPercentLevels.price(now, 1), 0.00000000001)
        assertEquals(0.00374022, FusionPercentLevels.price(now, -1), 0.00000000001)
    }

    @Test fun pumpTargetUsesEightDecimalsForStopLossPriceEntry() {
        val oneAbove = FusionPercentLevels.price(0.00377800, 1)
        assertEquals("0,00381578", FusionPercentLevels.format(oneAbove, false))
        assertEquals("0,00374022", FusionPercentLevels.format(
            FusionPercentLevels.price(0.00377800, -1), false))
    }

    @Test fun bitcoinTargetsDisplayEuroCents() {
        assertEquals("70.700,00", FusionPercentLevels.format(
            FusionPercentLevels.price(70_000.0, 1), true
        ))
    }
}
