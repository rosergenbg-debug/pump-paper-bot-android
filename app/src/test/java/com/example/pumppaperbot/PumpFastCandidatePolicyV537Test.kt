package com.example.pumppaperbot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PumpFastCandidatePolicyV537Test {
    private fun responsivePm2Observation(at: Long = 1_000_000L): SharedFusionEntryObservation {
        val breath = BuyerBreathSnapshot(
            phase = BuyerBreathPhase.IGNITION,
            fresh = true,
            pressureScore = 45,
            efficiencyScore = 10,
            absorptionRisk = 35,
            confidence = 80,
            buyerPercent5m = 60.0,
            buyerPercent15m = 57.0,
            priceChange5mPercent = 0.45,
            priceChange15mPercent = 0.45,
            activityRatio = 1.20,
            moveSincePhaseStartPercent = 0.45
        )
        val breathing = LiveMarketBreathingSnapshot(
            updatedAt = at,
            fresh = true,
            historyMinutes = 120,
            instantScore = 12,
            normalScore = 5,
            horizons = listOf(
                LiveBreathingHorizon(5, 6, 0.45, 60.0, 70, 5),
                LiveBreathingHorizon(15, -1, 0.45, 57.0, 60, 15),
                LiveBreathingHorizon(20, -1, 0.45, 56.0, 60, 20),
                LiveBreathingHorizon(30, -3, 0.45, 55.0, 55, 30)
            ),
            buyerBreath = breath
        )
        val micro = MicroImpulseSnapshot(
            connected = true,
            updatedAt = at,
            trades60s = 120,
            buyNotional5m = 60_000.0,
            sellNotional5m = 40_000.0,
            buyNotional15m = 580_000.0,
            sellNotional15m = 420_000.0,
            flowHistorySeconds = 3_600L,
            largeFlow = LargeFlowFingerprint(
                mode = LargeFlowMode.BUY_SERIES,
                confidence = 70,
                thresholdUsdt = 15_000.0,
                largeBuyUsdt = 150_000.0,
                largeSellUsdt = 40_000.0
            )
        )
        return SharedFusionEntryObservation(
            frame = FusionFlowFrame(12, 6, -1, -1, -3),
            shockReady = false,
            sampledAt = at,
            sampleBucket = at / 15_000L,
            breathing = breathing,
            micro = micro,
            executionAsk = 1.0,
            bookBidNotional = 50_000.0,
            bookAskNotional = 50_000.0,
            bookSpreadPercent = 0.08,
            capitalFlow = CapitalFlowProxy(mode = CapitalFlowMode.MIXED, score = 20, confidence = 90)
        )
    }

    @Test
    fun `responsive pm2 can trigger fast cycle without pm3`() {
        val plan = PumpFastCandidatePolicyV537.evaluate(responsivePm2Observation())

        assertTrue(plan.pump2)
        assertFalse(plan.pump3)
        assertTrue(plan.any)
    }
}
