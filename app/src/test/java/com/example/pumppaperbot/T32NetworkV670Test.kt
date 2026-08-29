package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class T32NetworkV670Test {
    @Test
    fun economicCoreMatchesProtectedXCheckpoint() {
        assertEquals(0.0021, T32CostPolicyV670.FEE_RATE, 0.0)
        assertEquals(-1.2, T32CostPolicyV670.STOP_NET_PERCENT, 0.0)
        assertEquals(2.5, T32CostPolicyV670.ECONOMY_TARGET_NET_PERCENT, 0.0)
        assertEquals(120L * 60L * 1_000L, T32CostPolicyV670.ECONOMY_MAX_HOLD_MILLIS)
        assertEquals(90L * 60L * 1_000L, T32CostPolicyV670.X52_MAX_HOLD_MILLIS)
        assertEquals(2, T32CostPolicyV670.MAX_ENTRIES_PER_UTC_DAY)
        assertEquals(120_000L, T32CostPolicyV670.LIMIT_TTL_MILLIS)
    }

    @Test
    fun fixedTargetIsTwoPointFiveNetAfterBothFees() {
        val entry = 0.0035
        val target = T32CostPolicyV670.economyTargetPrice(entry)
        assertEquals(2.5, T32CostPolicyV670.netPercent(entry, target), 1e-9)
    }

    @Test
    fun exactCoreRequiresEveryHistoricalT32AndDrop4Condition() {
        assertTrue(T32V670Policy.isExactCore(-0.40, true, 0.50, 0.49, -4.0))
        assertFalse(T32V670Policy.isExactCore(-0.39, true, 0.50, 0.49, -4.0))
        assertFalse(T32V670Policy.isExactCore(-0.40, false, 0.50, 0.49, -4.0))
        assertFalse(T32V670Policy.isExactCore(-0.40, true, 0.49, 0.48, -4.0))
        assertFalse(T32V670Policy.isExactCore(-0.40, true, 0.50, 0.50, -4.0))
        assertFalse(T32V670Policy.isExactCore(-0.40, true, 0.50, 0.49, -3.99))
    }

    @Test
    fun selectiveProfileRequiresProtectedSolBtcThresholdButEconomyDoesNot() {
        val below = setup(relativeLag6 = 0.39)
        val atThreshold = setup(relativeLag6 = 0.40)
        assertTrue(T32V670Profile.ECONOMY.allowsSignal(below))
        assertFalse(T32V670Profile.X52_SELECT.allowsSignal(below))
        assertTrue(T32V670Profile.X52_SELECT.allowsSignal(atThreshold))
    }

    private fun setup(relativeLag6: Double?) = T32V670Setup(
        readiness = 100,
        exactCore = true,
        signalOpenTime = 1L,
        signalCloseTime = 2L,
        signalCloseUsdt = 1.0,
        signalVwapUsdt = 1.01,
        liveOpenTime = 3L,
        liveOpenUsdt = 0.999,
        liveHighUsdt = 1.0,
        liveLowUsdt = 0.998,
        liveCloseUsdt = 0.999,
        drawdown12hPercent = -4.5,
        solBtcRelativeLag6 = relativeLag6,
        reason = "test"
    )
}
