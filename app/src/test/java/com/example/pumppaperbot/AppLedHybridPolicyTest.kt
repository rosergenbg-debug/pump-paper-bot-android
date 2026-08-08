package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLedHybridPolicyTest {
    @Test fun `bearish DeepSeek cannot erase near ready APP during confirmed rebound`() {
        val result = AppLedHybridPolicy.entry(entryEvidence(
            aiAction = "WATCH",
            aiDirection = -75,
            aiConfidence = 85,
            aiReadiness = 1,
            appReadiness = 74,
            breathing5m = 28,
            breathing15m = 16
        ))

        assertTrue(result.level >= 7)
        assertEquals("WATCH", result.tradeAction)
        assertTrue(result.reboundConfirmed)
    }

    @Test fun `APP buy plus sustained buyers creates verified hybrid buy`() {
        val result = AppLedHybridPolicy.entry(entryEvidence(
            aiAction = "WATCH",
            aiDirection = 5,
            aiConfidence = 55,
            aiReadiness = 3,
            appReadiness = 100,
            appBuySignal = true,
            breathing5m = 32,
            breathing15m = 22
        ))

        assertEquals("BUY", result.tradeAction)
        assertEquals(10, result.level)
    }

    @Test fun `Bitcoin sideways permits independent PUMP approach`() {
        val result = AppLedHybridPolicy.entry(entryEvidence(
            appReadiness = 71,
            bitcoinBuyerPercent60s = 49.0,
            bitcoinChange60sPercent = 0.01,
            breathing5m = 22,
            breathing15m = 12
        ))

        assertTrue(result.level >= 7)
        assertTrue(result.reason.contains("APP") || result.reason.contains("Bitcoin"))
    }

    @Test fun `DeepSeek can arm an independent entry even when APP readiness is low`() {
        val result = AppLedHybridPolicy.entry(entryEvidence(
            aiAction = "BUY",
            aiDirection = 76,
            aiConfidence = 82,
            aiReadiness = 9,
            appReadiness = 24,
            breathing5m = 31,
            breathing15m = 19
        ))

        assertTrue(result.independentDeepSeekSetup)
        assertFalse(result.appConfirmedEntry)
        assertEquals("WATCH", result.tradeAction)
        assertTrue(result.level >= 8)
    }

    @Test fun `independent DeepSeek entry requires two separate AI cycles`() {
        val first = DeepSeekPersistencePolicy.update(0, 0, 0L, true, false, 120_000L)
        val duplicate = DeepSeekPersistencePolicy.update(
            first.entryStreak, first.exitStreak, first.lastEvaluationAt, true, false, 150_000L
        )
        val second = DeepSeekPersistencePolicy.update(
            duplicate.entryStreak, duplicate.exitStreak, duplicate.lastEvaluationAt,
            true, false, 240_000L
        )

        assertFalse(first.confirmIndependentBuy)
        assertEquals(1, duplicate.entryStreak)
        assertTrue(second.confirmIndependentBuy)
    }

    @Test fun `independent DeepSeek exit is armed before it is executable`() {
        val result = AppLedHybridPolicy.exit(exitEvidence(
            modelRequestsExit = true,
            appExitSignal = false,
            breathing5m = -25,
            breathing15m = -30,
            breathing30m = -25,
            breathing60m = -18
        ))

        assertTrue(result.allowExit)
        assertTrue(result.independentDeepSeekSetup)
        assertTrue(result.dangerCap <= 9)
    }

    @Test fun `model micro exit without APP or medium weakness is warning only`() {
        val result = AppLedHybridPolicy.exit(exitEvidence(
            modelRequestsExit = true,
            appExitSignal = false,
            breathing5m = -8,
            breathing15m = 1,
            breathing30m = 5,
            breathing60m = 4
        ))

        assertFalse(result.allowExit)
        assertTrue(result.dangerCap <= 6)
    }

    @Test fun `APP exit with persistent weakness is allowed after minimum hold`() {
        val result = AppLedHybridPolicy.exit(exitEvidence(
            modelRequestsExit = true,
            appExitSignal = true,
            positionAgeMillis = AppLedHybridPolicy.MIN_ORDINARY_HOLD_MILLIS,
            pumpBuyerPercent60s = 41.0,
            pumpBuyerPercent5m = 44.0,
            pumpChange60sPercent = -0.20,
            breathing5m = -25,
            breathing15m = -30,
            breathing30m = -25,
            breathing60m = -18
        ))

        assertTrue(result.allowExit)
    }

    @Test fun `strong buyer recovery blocks an ordinary exit`() {
        val result = AppLedHybridPolicy.exit(exitEvidence(
            modelRequestsExit = true,
            appExitSignal = true,
            pumpBuyerPercent15s = 70.0,
            pumpBuyerPercent60s = 63.0,
            pumpBuyerPercent5m = 57.0,
            pumpChange60sPercent = 0.20,
            breathing15m = -30,
            breathing30m = -25,
            breathing60m = -20
        ))

        assertFalse(result.allowExit)
        assertEquals(5, result.dangerCap)
    }

    @Test fun `ordinary exit is held during the first twenty minutes`() {
        val result = AppLedHybridPolicy.exit(exitEvidence(
            appExitSignal = true,
            positionAgeMillis = AppLedHybridPolicy.MIN_ORDINARY_HOLD_MILLIS - 1L,
            breathing5m = -25,
            breathing15m = -30,
            breathing30m = -25,
            breathing60m = -18
        ))

        assertFalse(result.allowExit)
        assertTrue(result.reason.contains("слишком новая"))
    }

    private fun entryEvidence(
        aiAction: String = "WATCH",
        aiDirection: Int = 10,
        aiConfidence: Int = 50,
        aiReadiness: Int = 3,
        appReadiness: Int = 20,
        appBuySignal: Boolean = false,
        bitcoinBuyerPercent60s: Double = 50.0,
        bitcoinChange60sPercent: Double = 0.0,
        breathing5m: Int? = null,
        breathing15m: Int? = null
    ) = AppLedEntryEvidence(
        aiFresh = true,
        aiAction = aiAction,
        aiDirection = aiDirection,
        aiConfidence = aiConfidence,
        aiReadiness = aiReadiness,
        appReadiness = appReadiness,
        appBuySignal = appBuySignal,
        appSellSignal = false,
        hardVeto = false,
        microFresh = true,
        pumpBuyerPercent60s = 60.0,
        pumpChange60sPercent = 0.12,
        bitcoinBuyerPercent60s = bitcoinBuyerPercent60s,
        bitcoinChange60sPercent = bitcoinChange60sPercent,
        breathing5m = breathing5m,
        breathing15m = breathing15m,
        breathing30m = 0,
        breathing60m = 0
    )

    private fun exitEvidence(
        modelRequestsExit: Boolean = true,
        appExitSignal: Boolean = false,
        positionAgeMillis: Long = AppLedHybridPolicy.MIN_ORDINARY_HOLD_MILLIS,
        pumpBuyerPercent15s: Double = 42.0,
        pumpBuyerPercent60s: Double = 43.0,
        pumpBuyerPercent5m: Double = 45.0,
        pumpChange60sPercent: Double = -0.15,
        breathing5m: Int? = null,
        breathing15m: Int? = null,
        breathing30m: Int? = null,
        breathing60m: Int? = null
    ) = AppLedExitEvidence(
        modelRequestsExit = modelRequestsExit,
        appExitSignal = appExitSignal,
        rapidDropUnrecovered = false,
        currentReturnPercent = -1.0,
        positionAgeMillis = positionAgeMillis,
        microFresh = true,
        pumpBuyerPercent15s = pumpBuyerPercent15s,
        pumpBuyerPercent60s = pumpBuyerPercent60s,
        pumpBuyerPercent5m = pumpBuyerPercent5m,
        pumpChange60sPercent = pumpChange60sPercent,
        breathing5m = breathing5m,
        breathing15m = breathing15m,
        breathing30m = breathing30m,
        breathing60m = breathing60m
    )
}
