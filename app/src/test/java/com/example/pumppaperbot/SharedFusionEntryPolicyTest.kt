package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedFusionEntryPolicyTest {
    private val buyFrame = FusionFlowFrame(
        instant = 30,
        score5m = 24,
        score15m = 18,
        score20m = 7,
        score30m = 12
    )

    @Test
    fun `same observation and same independent state produce same entry result`() {
        val t0 = 1_000_000L
        val observation = SharedFusionEntryObservation(buyFrame, false, t0, 1L)

        val fusionFirst = SharedFusionEntryPolicy.evaluate(FusionStabilityState(), observation, t0)
        val machineFirst = SharedFusionEntryPolicy.evaluate(FusionStabilityState(), observation, t0)
        assertNull(fusionFirst.action)
        assertNull(machineFirst.action)
        assertEquals(fusionFirst.nextState.entryStreak, machineFirst.nextState.entryStreak)
        assertEquals(fusionFirst.nextState.entryCandidateAt, machineFirst.nextState.entryCandidateAt)

        val later = t0 + FusionStabilityPolicy.ENTRY_CONFIRM_MIN_MILLIS
        val laterObservation = observation.copy(sampledAt = later, sampleBucket = 2L)
        val fusionBuy = SharedFusionEntryPolicy.evaluate(fusionFirst.nextState, laterObservation, later)
        val machineBuy = SharedFusionEntryPolicy.evaluate(machineFirst.nextState, laterObservation, later)
        assertEquals("BUY", fusionBuy.action)
        assertEquals("BUY", machineBuy.action)
    }

    @Test
    fun `cooldowns remain independent even with one shared entry brain`() {
        val now = 2_000_000L
        val observation = SharedFusionEntryObservation(buyFrame, false, now, 10L)
        val free = SharedFusionEntryPolicy.evaluate(FusionStabilityState(), observation, now)
        val blocked = SharedFusionEntryPolicy.evaluate(
            FusionStabilityState(cooldownUntil = now + 120_000L),
            observation,
            now
        )
        assertEquals(1, free.nextState.entryStreak)
        assertEquals(0, blocked.nextState.entryStreak)
        assertNull(blocked.action)
    }

    @Test
    fun `confirmed shock rebound uses the same immediate entry lane`() {
        val now = 3_000_000L
        val observation = SharedFusionEntryObservation(null, true, now, 20L)
        val result = SharedFusionEntryPolicy.evaluate(FusionStabilityState(), observation, now)
        assertEquals("BUY", result.action)
    }
}
