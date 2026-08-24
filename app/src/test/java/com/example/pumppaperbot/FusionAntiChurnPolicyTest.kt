package com.example.pumppaperbot

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FusionAntiChurnPolicyTest {

    private val positive = FusionFlowFrame(
        instant = 9,
        score5m = 7,
        score15m = 6,
        score20m = 4,
        score30m = 4
    )

    @Test
    fun structuralStopStartsWideAndTrailsUpWithPeak() {
        val atEntry = FusionRiskPolicy.activeStopPrice(
            entryPrice = 100.0,
            peakBid = 100.0,
            feeRate = 0.0025,
            profitDefenseArmed = false
        )
        val afterRise = FusionRiskPolicy.activeStopPrice(
            entryPrice = 100.0,
            peakBid = 105.0,
            feeRate = 0.0025,
            profitDefenseArmed = false
        )

        assertEquals(98.25, atEntry, 0.000001)
        assertEquals(103.1625, afterRise, 0.000001)
        assertTrue(afterRise > atEntry)
    }

    @Test
    fun smallProfitAloneDoesNotTriggerOldPointSixPercentChurn() {
        val decision = FusionStabilityPolicy.evaluate(
            inPosition = true,
            entryPrice = 100.0,
            previous = FusionStabilityState(peakBid = 100.17),
            frame = positive,
            bid = 100.16,
            feeRate = 0.0025,
            now = 2_000_000L,
            positionAgeMillis = 20L * 60L * 1000L
        )

        assertNull(decision.action)
        assertFalse(decision.nextState.profitDefenseArmed)
        assertTrue(decision.activeStopPrice < 99.0)
    }

    @Test
    fun profitDefenseArmsOnlyWhenProfitExistsAndFlowDeteriorates() {
        val deterioration = FusionFlowFrame(
            instant = -18,
            score5m = -14,
            score15m = -12,
            score20m = -10,
            score30m = 4
        )
        val armed = FusionStabilityPolicy.evaluate(
            inPosition = true,
            entryPrice = 100.0,
            previous = FusionStabilityState(peakBid = 102.0),
            frame = deterioration,
            bid = 101.70,
            feeRate = 0.0025,
            now = 3_000_000L,
            positionAgeMillis = 20L * 60L * 1000L
        )

        assertNull(armed.action)
        assertTrue(armed.nextState.profitDefenseArmed)
        assertEquals(100.98, armed.activeStopPrice, 0.000001)

        val stopped = FusionStabilityPolicy.evaluate(
            inPosition = true,
            entryPrice = 100.0,
            previous = armed.nextState,
            frame = deterioration,
            bid = 100.97,
            feeRate = 0.0025,
            now = 3_060_000L,
            positionAgeMillis = 21L * 60L * 1000L
        )

        assertEquals("EXIT", stopped.action)
        assertTrue(stopped.reason.startsWith("PROFIT_DEFENSE_STOP"))
    }

    @Test
    fun shortDipDoesNotArmProfitDefenseWhileFifteenAndTwentyMinutesStayPositive() {
        val overnightLikeDip = FusionFlowFrame(
            instant = -37,
            score5m = -57,
            score15m = 22,
            score20m = 26,
            score30m = 37
        )
        val decision = FusionStabilityPolicy.evaluate(
            inPosition = true,
            entryPrice = 100.0,
            previous = FusionStabilityState(peakBid = 104.0),
            frame = overnightLikeDip,
            bid = 103.0,
            feeRate = 0.0025,
            now = 3_500_000L,
            positionAgeMillis = 30L * 60L * 1000L
        )

        assertNull(decision.action)
        assertFalse(decision.nextState.profitDefenseArmed)
        assertTrue(decision.activeStopPrice < 103.0)
    }

    @Test
    fun entryNeedsTwoObservationsAndAtLeastSixtySecondsEvenWhenStrong() {
        val t0 = 4_000_000L
        val first = FusionStabilityPolicy.evaluate(
            inPosition = false,
            entryPrice = 0.0,
            previous = FusionStabilityState(),
            frame = positive,
            bid = 100.0,
            feeRate = 0.0025,
            now = t0,
            entryObservation = capitalReadyObservation(positive, t0)
        )
        assertNull(first.action)
        assertEquals(1, first.nextState.entryStreak)

        val tooSoon = FusionStabilityPolicy.evaluate(
            inPosition = false,
            entryPrice = 0.0,
            previous = first.nextState,
            frame = positive,
            bid = 100.1,
            feeRate = 0.0025,
            now = t0 + 30_000L,
            entryObservation = capitalReadyObservation(positive, t0 + 30_000L, ask = 1.0005)
        )
        assertNull(tooSoon.action)
        assertEquals(2, tooSoon.nextState.entryStreak)

        val confirmed = FusionStabilityPolicy.evaluate(
            inPosition = false,
            entryPrice = 0.0,
            previous = tooSoon.nextState,
            frame = positive,
            bid = 100.2,
            feeRate = 0.0025,
            now = t0 + 61_000L,
            entryObservation = capitalReadyObservation(positive, t0 + 61_000L, ask = 1.0015)
        )
        assertEquals("BUY", confirmed.action)
    }

    @Test
    fun cooldownBlocksImmediateReEntryAndTwoRecentLossesExtendIt() {
        val now = 5_000_000L
        val previous = FusionStabilityState(
            lastLossExitAt = now - 10L * 60L * 1000L,
            lossExitStreak = 1
        )
        val cooldown = FusionStabilityPolicy.cooldownAfterExit(
            previous = previous,
            exitPnlEur = -3.0,
            wasProtectiveStop = true,
            now = now
        )

        assertEquals(2, cooldown.lossExitStreak)
        assertEquals(now + FusionStabilityPolicy.DOUBLE_LOSS_COOLDOWN_MILLIS, cooldown.cooldownUntil)

        val blocked = FusionStabilityPolicy.evaluate(
            inPosition = false,
            entryPrice = 0.0,
            previous = cooldown,
            frame = positive,
            bid = 100.0,
            feeRate = 0.0025,
            now = now + 5L * 60L * 1000L
        )
        assertNull(blocked.action)
        assertTrue(blocked.reason.startsWith("COOLDOWN"))
    }

    @Test
    fun tinyAllNegativeNoiseIsNotAFullSystemExit() {
        val noisy = FusionFlowFrame(-1, -1, -1, -1, 5)
        assertTrue(noisy.exitSignal)
        assertFalse(noisy.meaningfulExitSignal)
    }

    @Test
    fun oldStabilityJsonMigratesWithSafeDefaults() {
        val old = JSONObject()
            .put("entryStreak", 1)
            .put("exitStreak", 2)
            .put("exitArmedAt", 123L)
            .put("exitArmedBid", 99.0)
            .put("peakBid", 101.0)

        val migrated = FusionStabilityState.fromJson(old)
        assertEquals(0L, migrated.cooldownUntil)
        assertEquals(0, migrated.lossExitStreak)
        assertFalse(migrated.profitDefenseArmed)
        assertEquals(0L, migrated.entryCandidateAt)
    }
}
