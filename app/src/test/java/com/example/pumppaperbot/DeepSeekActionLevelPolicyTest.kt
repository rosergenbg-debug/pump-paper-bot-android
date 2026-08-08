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

    @Test fun `strong sustained PUMP can lead or lag a weak Bitcoin minute`() {
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
            bitcoinChange60sPercent = -0.25,
            breathing5m = 38,
            breathing15m = 31
        ))

        assertEquals(DeepSeekActionBand.GREEN, result.band)
    }

    @Test fun `persistent Bitcoin weakness still caps unconfirmed PUMP`() {
        val result = DeepSeekActionLevelPolicy.entry(entryEvidence(
            aiAction = "BUY",
            aiDirection = 80,
            aiConfidence = 80,
            aiEntryReadiness = 10,
            appReadiness = 20,
            microFresh = true,
            microPhase = "CALM",
            pumpBuyerPercent60s = 49.0,
            pumpChange60sPercent = -0.05,
            bitcoinBuyerPercent60s = 37.0,
            bitcoinChange60sPercent = -0.30,
            breathing5m = -12,
            breathing15m = -18
        ))

        assertTrue(result.level <= 5)
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

    @Test fun `isolated APP sell is warning not false nine of ten emergency`() {
        val result = DeepSeekActionLevelPolicy.exit(DeepSeekExitLevelEvidence(
            deepSeekDanger = 3,
            exitAdvised = false,
            localSellSignal = true,
            rapidDrop = false,
            localGuardCritical = false,
            directionScore = -10,
            microFresh = true,
            pumpBuyerPercent60s = 51.0,
            pumpChange60sPercent = -0.03
        ))

        assertEquals(6, result.level)
        assertEquals(DeepSeekActionBand.YELLOW, result.band)
    }

    @Test fun `executed exits always notify while user is in position`() {
        assertTrue(VirtualTradeAlertPolicy.shouldNotify("SELL", userPositionOpen = true))
        assertTrue(VirtualTradeAlertPolicy.shouldNotify(StrategyV2.ACTION_SELL_HALF, userPositionOpen = true))
        assertTrue(VirtualTradeAlertPolicy.shouldNotify("BUY", userPositionOpen = true))
        assertTrue(VirtualTradeAlertPolicy.shouldNotify("BUY", userPositionOpen = false))
    }

    @Test fun `DeepSeek entry alerts start at seven and repeat on each ten percent improvement`() {
        assertEquals(
            DeepSeekActionLevelAlertPolicy.NONE,
            DeepSeekActionLevelAlertPolicy.next(DeepSeekActionLevelAlertPolicy.NONE, 6)
        )
        assertEquals(
            7,
            DeepSeekActionLevelAlertPolicy.next(DeepSeekActionLevelAlertPolicy.NONE, 7)
        )
        assertEquals(
            8,
            DeepSeekActionLevelAlertPolicy.next(7, 8)
        )
        assertEquals(9, DeepSeekActionLevelAlertPolicy.next(8, 9))
        assertEquals(10, DeepSeekActionLevelAlertPolicy.next(9, 10))
        assertEquals(DeepSeekActionLevelAlertPolicy.NONE, DeepSeekActionLevelAlertPolicy.next(9, 9))
    }

    @Test fun `only urgent personal exit bypasses the ringing schedule`() {
        assertFalse(AlertDeliveryPolicy.shouldRing(withinSchedule = false, urgentPersonalExit = false))
        assertTrue(AlertDeliveryPolicy.shouldRing(withinSchedule = true, urgentPersonalExit = false))
        assertTrue(AlertDeliveryPolicy.shouldRing(withinSchedule = false, urgentPersonalExit = true))
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
        bitcoinChange60sPercent: Double = 0.0,
        breathing5m: Int? = null,
        breathing15m: Int? = null
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
        bitcoinChange60sPercent,
        breathing5m,
        breathing15m
    )
}
