package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class T32NetworkV660Test {
    @Test
    fun fixedTargetReallyProducesTwoPointFiveNetAfterBothFees() {
        val entry = 0.0035
        val target = T32CostPolicyV660.targetPrice(entry)
        assertEquals(2.5, T32CostPolicyV660.netPercent(entry, target), 1e-9)
    }

    @Test
    fun frozenEconomicCoreMatchesProtectedXDecision() {
        assertEquals(0.0021, T32CostPolicyV660.FEE_RATE, 0.0)
        assertEquals(2.5, T32CostPolicyV660.TARGET_NET_PERCENT, 0.0)
        assertEquals(-1.2, T32CostPolicyV660.STOP_NET_PERCENT, 0.0)
        assertEquals(120L * 60L * 1_000L, T32CostPolicyV660.MAX_HOLD_MILLIS)
        assertEquals(2, T32CostPolicyV660.MAX_ENTRIES_PER_UTC_DAY)
        assertEquals(0.001, T32CostPolicyV660.LIMIT_DISCOUNT, 0.0)
    }

    @Test
    fun btcGuardBlocksStrongUpButCoreDoesNot() {
        val setup = setup(btcStrongUp = true, rel6 = 0.8)
        assertTrue(T32V660Profile.CORE.allows(setup))
        assertFalse(T32V660Profile.BTC_GUARD.allows(setup))
    }

    @Test
    fun solSelectRequiresRelativeStrengthThreshold() {
        assertFalse(T32V660Profile.SOL_SELECT.allows(setup(rel6 = 0.39)))
        assertTrue(T32V660Profile.SOL_SELECT.allows(setup(rel6 = 0.40)))
        assertTrue(T32V660Profile.SOL_SELECT.allows(setup(rel6 = 0.65)))
    }

    private fun setup(
        btcStrongUp: Boolean = false,
        rel6: Double? = null
    ) = T32V660Setup(
        readiness = 82,
        exactCore = true,
        vwap = 1.0,
        signalClose = 0.99,
        deviationPercent = -0.5,
        buyShare = 0.55,
        buyShareDelta = 0.02,
        drawdown12hPercent = -4.5,
        belowFour12h = true,
        btcStrongUpRecent = btcStrongUp,
        btcHourlyReturns = emptyList(),
        solBtcRelativeLag6 = rel6,
        reason = "test"
    )
}
