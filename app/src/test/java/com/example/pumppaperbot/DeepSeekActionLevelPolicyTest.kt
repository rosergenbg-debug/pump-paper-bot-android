package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekActionLevelPolicyTest {
    @Test fun `entry veto always forces red one of ten`() {
        val result = DeepSeekActionLevelPolicy.entry(entryEvidence(hardVeto = true))

        assertEquals(1, result.level)
        assertEquals(DeepSeekActionBand.RED, result.band)
        assertFalse(result.intensive)
    }

    @Test fun `unconfirmed optimism cannot become green`() {
        val result = DeepSeekActionLevelPolicy.entry(entryEvidence(
            aiAction = "WATCH",
            aiDirection = 80,
            aiConfidence = 85,
            aiEntryReadiness = 10,
            appReadiness = 25,
            microFresh = true,
            microPhase = "CALM",
            pumpBuyerPercent60s = 50.0,
            pumpChange60sPercent = 0.0
        ))

        assertTrue(result.level <= 7)
        assertFalse(result.band == DeepSeekActionBand.GREEN)
    }

    @Test fun `verified DeepSeek buy with independent flow reaches green ten`() {
        val result = DeepSeekActionLevelPolicy.entry(entryEvidence(
            aiAction = "BUY",
            aiDirection = 82,
            aiConfidence = 84,
            aiEntryReadiness = 10,
            appReadiness = 78,
            microFresh = true,
            microPhase = "CONFIRMATION",
            pumpBuyerPercent60s = 61.0,
            pumpChange60sPercent = 0.22,
            bitcoinBuyerPercent60s = 54.0,
            bitcoinChange60sPercent = 0.06
        ))

        assertEquals(10, result.level)
        assertEquals(DeepSeekActionBand.GREEN, result.band)
        assertTrue(result.intensive)
        assertTrue(result.proPreferred)
    }

    @Test fun `weak Bitcoin caps entry before green`() {
        val result = DeepSeekActionLevelPolicy.entry(entryEvidence(
            aiAction = "BUY",
            aiDirection = 82,
            aiConfidence = 84,
            aiEntryReadiness = 10,
            appReadiness = 78,
            microFresh = true,
            microPhase = "CONFIRMATION",
            pumpBuyerPercent60s = 61.0,
            pumpChange60sPercent = 0.22,
            bitcoinBuyerPercent60s = 39.0,
            bitcoinChange60sPercent = -0.25
        ))

        assertTrue(result.level <= 5)
        assertFalse(result.band == DeepSeekActionBand.GREEN)
    }

    @Test fun `exit scale reverses colors from safe green to dangerous red`() {
        val safe = DeepSeekActionLevelPolicy.exit(DeepSeekExitLevelEvidence(
            deepSeekDanger = 2,
            exitAdvised = false,
            localSellSignal = false,
            rapidDrop = false,
            localGuardCritical = false,
            directionScore = 10,
            microFresh = true,
            pumpBuyerPercent60s = 54.0,
            pumpChange60sPercent = 0.05
        ))
        val danger = DeepSeekActionLevelPolicy.exit(DeepSeekExitLevelEvidence(
            deepSeekDanger = 5,
            exitAdvised = true,
            localSellSignal = false,
            rapidDrop = false,
            localGuardCritical = false,
            directionScore = -20,
            microFresh = true,
            pumpBuyerPercent60s = 44.0,
            pumpChange60sPercent = -0.20
        ))

        assertEquals(DeepSeekActionBand.GREEN, safe.band)
        assertEquals(2, safe.level)
        assertEquals(DeepSeekActionBand.RED, danger.band)
        assertEquals(9, danger.level)
    }

    @Test fun `executed exits always notify while user is in position`() {
        assertTrue(VirtualTradeAlertPolicy.shouldNotify("SELL", userPositionOpen = true))
        assertTrue(VirtualTradeAlertPolicy.shouldNotify(StrategyV2.ACTION_SELL_HALF, userPositionOpen = true))
        assertFalse(VirtualTradeAlertPolicy.shouldNotify("BUY", userPositionOpen = true))
        assertTrue(VirtualTradeAlertPolicy.shouldNotify("BUY", userPositionOpen = false))
    }

    @Test fun `DeepSeek preparation and ready alerts only fire on upward crossing`() {
        assertEquals(
            DeepSeekActionLevelAlertPolicy.PREPARE,
            DeepSeekActionLevelAlertPolicy.next(DeepSeekActionLevelAlertPolicy.NONE, 6)
        )
        assertEquals(
            DeepSeekActionLevelAlertPolicy.READY,
            DeepSeekActionLevelAlertPolicy.next(DeepSeekActionLevelAlertPolicy.PREPARE, 9)
        )
        assertEquals(
            DeepSeekActionLevelAlertPolicy.NONE,
            DeepSeekActionLevelAlertPolicy.next(DeepSeekActionLevelAlertPolicy.READY, 9)
        )
    }

    private fun entryEvidence(
        freshAi: Boolean = true,
        aiAction: String = "WATCH",
        aiDirection: Int = 20,
        aiConfidence: Int = 50,
        aiEntryReadiness: Int = 3,
        appReadiness: Int = 20,
        hardVeto: Boolean = false,
        microFresh: Boolean = false,
        microPhase: String = "CALM",
        pumpBuyerPercent60s: Double = 50.0,
        pumpChange60sPercent: Double = 0.0,
        bitcoinBuyerPercent60s: Double = 50.0,
        bitcoinChange60sPercent: Double = 0.0
    ) = DeepSeekEntryLevelEvidence(
        freshAi,
        aiAction,
        aiDirection,
        aiConfidence,
        aiEntryReadiness,
        appReadiness,
        hardVeto,
        microFresh,
        microPhase,
        pumpBuyerPercent60s,
        pumpChange60sPercent,
        bitcoinBuyerPercent60s,
        bitcoinChange60sPercent
    )
}
