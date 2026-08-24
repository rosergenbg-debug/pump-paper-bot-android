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

    private fun observation(frame: FusionFlowFrame?, shock: Boolean, now: Long) =
        SharedFusionEntryObservation(
            frame = frame,
            shockReady = shock,
            sampledAt = now,
            sampleBucket = now / 15_000L,
            micro = MicroImpulseSnapshot(
                connected = true,
                updatedAt = now,
                trades60s = 100,
                buyNotional5m = 420_000.0,
                sellNotional5m = 180_000.0,
                buyNotional15m = 720_000.0,
                sellNotional15m = 680_000.0,
                flowHistorySeconds = 3_600L,
                largeFlow = LargeFlowFingerprint(
                    mode = LargeFlowMode.BUY_SERIES,
                    confidence = 75,
                    largeBuyUsdt = 150_000.0,
                    largeSellUsdt = 30_000.0
                )
            ),
            executionAsk = 1.0,
            bookBidNotional = 90_000.0,
            bookAskNotional = 60_000.0,
            bookSpreadPercent = 0.08,
            capitalFlow = CapitalFlowProxy(mode = CapitalFlowMode.MIXED, score = 20, confidence = 90)
        )

    @Test
    fun `same observation and same independent state produce same entry result`() {
        val t0 = 1_000_000L
        val observation = observation(buyFrame, false, t0)

        val fusionFirst = SharedFusionEntryPolicy.evaluate(FusionStabilityState(), observation, t0)
        val machineFirst = SharedFusionEntryPolicy.evaluate(FusionStabilityState(), observation, t0)
        assertNull(fusionFirst.action)
        assertNull(machineFirst.action)
        assertEquals(fusionFirst.nextState.entryStreak, machineFirst.nextState.entryStreak)
        assertEquals(fusionFirst.nextState.entryCandidateAt, machineFirst.nextState.entryCandidateAt)

        val later = t0 + FusionStabilityPolicy.ENTRY_CONFIRM_MIN_MILLIS
        val laterObservation = observation.copy(sampledAt = later, sampleBucket = 2L, executionAsk = 1.0015)
        val fusionBuy = SharedFusionEntryPolicy.evaluate(fusionFirst.nextState, laterObservation, later)
        val machineBuy = SharedFusionEntryPolicy.evaluate(machineFirst.nextState, laterObservation, later)
        assertEquals("BUY", fusionBuy.action)
        assertEquals("BUY", machineBuy.action)
    }

    @Test
    fun `cooldowns remain independent even with one shared entry brain`() {
        val now = 2_000_000L
        val observation = observation(buyFrame, false, now)
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
        val observation = observation(null, true, now)
        val result = SharedFusionEntryPolicy.evaluate(FusionStabilityState(), observation, now)
        assertEquals("BUY", result.action)
    }
}
