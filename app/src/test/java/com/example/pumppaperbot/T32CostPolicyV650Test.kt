package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class T32CostPolicyV650Test {
    @Test
    fun feeIsTwentyOneBasisPointsPerSide() {
        assertEquals(0.0021, T32CostPolicyV650.FEE_RATE, 0.0)
    }

    @Test
    fun onePointFiveTargetIsExactlyNetAfterBothFees() {
        val entry = 1.234567
        val target = T32CostPolicyV650.targetPrice(entry, 1.5)
        assertEquals(1.5, T32CostPolicyV650.netPercent(entry, target), 1e-9)

        val fill = T32CostPolicyV650.buyAllCash(1000.0, entry)
        val gross = fill.coins * target
        val finalCash = gross - T32CostPolicyV650.sellFee(fill.coins, target)
        assertEquals(1015.0, finalCash, 1e-7)
    }

    @Test
    fun twoPercentTargetIsExactlyNetAfterBothFees() {
        val entry = 0.0042
        val target = T32CostPolicyV650.targetPrice(entry, 2.0)
        assertEquals(2.0, T32CostPolicyV650.netPercent(entry, target), 1e-9)

        val fill = T32CostPolicyV650.buyAllCash(1000.0, entry)
        val gross = fill.coins * target
        val finalCash = gross - T32CostPolicyV650.sellFee(fill.coins, target)
        assertEquals(1020.0, finalCash, 1e-7)
    }

    @Test
    fun humanAlarmRepeatsWhileDecisionIsPending() {
        val t0 = 1_000_000L
        assertTrue(HumanFactorAlertPolicyV650.shouldRing(true, 0L, t0))
        assertFalse(HumanFactorAlertPolicyV650.shouldRing(true, t0, t0 + 59_999L))
        assertTrue(HumanFactorAlertPolicyV650.shouldRing(true, t0, t0 + 60_000L))
        assertFalse(HumanFactorAlertPolicyV650.shouldRing(false, t0, t0 + 120_000L))
    }
}
