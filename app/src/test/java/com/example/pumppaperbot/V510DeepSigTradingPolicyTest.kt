package com.example.pumppaperbot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V510DeepSigTradingPolicyTest {
    @Test fun `high readiness watch becomes executable independent buy when live flow confirms`() {
        val result = AppLedHybridPolicy.entry(
            AppLedEntryEvidence(
                aiFresh = true,
                aiAction = "WATCH",
                aiDirection = 68,
                aiConfidence = 74,
                aiReadiness = 9,
                appReadiness = 22,
                appBuySignal = false,
                appSellSignal = false,
                hardVeto = false,
                microFresh = true,
                pumpBuyerPercent60s = 61.0,
                pumpChange60sPercent = 0.16,
                bitcoinBuyerPercent60s = 50.0,
                bitcoinChange60sPercent = 0.01,
                breathing5m = 31,
                breathing15m = 18,
                breathing30m = 4,
                breathing60m = 2
            )
        )

        assertTrue(result.independentDeepSeekSetup)
        assertTrue(result.level >= 8)
        assertTrue(result.reason.contains("WATCH") || result.reason.contains("BUY"))
    }

    @Test fun `one strong independent setup reaches verifier in v510`() {
        val decision = DeepSeekPersistencePolicy.update(
            previousEntryStreak = 0,
            previousExitStreak = 0,
            previousEvaluationAt = 0L,
            independentEntrySetup = true,
            independentExitSetup = false,
            now = 120_000L
        )

        assertTrue(DeepSeekPersistencePolicy.REQUIRED_CONFIRMATIONS == 1)
        assertTrue(decision.confirmIndependentBuy)
    }

    @Test fun `profitable fade permits earlier deepsig exit before slow layers turn negative`() {
        val result = AppLedHybridPolicy.exit(
            AppLedExitEvidence(
                modelRequestsExit = true,
                appExitSignal = false,
                rapidDropUnrecovered = false,
                currentReturnPercent = 3.4,
                positionAgeMillis = AppLedHybridPolicy.MIN_ORDINARY_HOLD_MILLIS,
                microFresh = true,
                pumpBuyerPercent15s = 43.0,
                pumpBuyerPercent60s = 42.0,
                pumpBuyerPercent5m = 45.0,
                pumpChange60sPercent = -0.18,
                breathing5m = -16,
                breathing15m = -8,
                breathing30m = 9,
                breathing60m = 14
            )
        )

        assertTrue(result.allowExit)
        assertTrue(result.independentDeepSeekSetup)
        assertFalse(result.emergency)
    }

    @Test fun `flat trade still needs real structural weakness for ordinary independent exit`() {
        val result = AppLedHybridPolicy.exit(
            AppLedExitEvidence(
                modelRequestsExit = true,
                appExitSignal = false,
                rapidDropUnrecovered = false,
                currentReturnPercent = 0.2,
                positionAgeMillis = AppLedHybridPolicy.MIN_ORDINARY_HOLD_MILLIS,
                microFresh = true,
                pumpBuyerPercent15s = 43.0,
                pumpBuyerPercent60s = 42.0,
                pumpBuyerPercent5m = 45.0,
                pumpChange60sPercent = -0.18,
                breathing5m = -16,
                breathing15m = -8,
                breathing30m = 9,
                breathing60m = 14
            )
        )

        assertFalse(result.allowExit)
        assertFalse(result.independentDeepSeekSetup)
    }
}
