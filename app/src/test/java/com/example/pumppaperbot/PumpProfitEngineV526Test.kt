package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PumpProfitEngineV526Test {
    private val fee = 0.0025
    private val entryCost = 1000.0
    private val amount = (entryCost - entryCost * fee) / 1.0
    private val portfolio = FusionSimPortfolio(
        cashEur = 0.0,
        pumpAmount = amount,
        entryPrice = 1.0,
        entryCostEur = entryCost
    )

    private fun observation(
        phase: BuyerBreathPhase = BuyerBreathPhase.IGNITION,
        instant: Int = 16,
        score5: Int = 9,
        score15: Int = 1,
        score30: Int = -2,
        buyer5: Double = 66.0,
        absorption: Int = 20,
        efficiency: Int = 45,
        activity: Double = 1.8,
        move: Double = 0.45,
        at: Long = 1_000_000L
    ): SharedFusionEntryObservation {
        val breath = BuyerBreathSnapshot(
            phase = phase,
            fresh = true,
            pressureScore = 45,
            efficiencyScore = efficiency,
            absorptionRisk = absorption,
            confidence = 80,
            buyerPercent5m = buyer5,
            buyerPercent15m = 57.0,
            priceChange5mPercent = move,
            priceChange15mPercent = move,
            activityRatio = activity,
            moveSincePhaseStartPercent = move
        )
        val breathing = LiveMarketBreathingSnapshot(
            updatedAt = at,
            fresh = true,
            historyMinutes = 120,
            instantScore = instant,
            normalScore = 5,
            horizons = listOf(
                LiveBreathingHorizon(5, score5, move, buyer5, 70, 5),
                LiveBreathingHorizon(15, score15, move, 57.0, 60, 15),
                LiveBreathingHorizon(20, score15, move, 56.0, 60, 20),
                LiveBreathingHorizon(30, score30, move, 55.0, 55, 30)
            ),
            buyerBreath = breath
        )
        return SharedFusionEntryObservation(
            frame = FusionFlowFrame(instant, score5, score15, score15, score30),
            shockReady = false,
            sampledAt = at,
            sampleBucket = at / 15_000L,
            breathing = breathing
        )
    }

    private fun bidForNet(netPercent: Double): Double =
        (1.0 + netPercent / 100.0) * entryCost / (amount * (1.0 - fee))

    @Test
    fun `pm2 enters early ignition after short causal confirmation`() {
        val first = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2,
            FusionStabilityState(),
            observation(),
            1_000_000L
        )
        assertNull(first.action)
        assertEquals(1, first.nextState.entryStreak)

        val second = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2,
            first.nextState,
            observation(at = 1_015_000L),
            1_015_000L
        )
        assertEquals("BUY", second.action)
        assertTrue(second.reason.contains("EARLY_ENTRY"))
    }

    @Test
    fun `pm2 and pm3 use the same strict entry gate`() {
        val weakerSetup = observation(
            instant = 9,
            score5 = 3,
            score15 = -3,
            score30 = -7,
            buyer5 = 57.0,
            absorption = 60,
            efficiency = -20,
            activity = 1.06
        )
        val pm2 = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2,
            FusionStabilityState(),
            weakerSetup,
            1_000_000L
        )
        val pm3 = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_3,
            FusionStabilityState(),
            weakerSetup,
            1_000_000L
        )

        assertEquals(pm3.action, pm2.action)
        assertEquals(pm3.nextState, pm2.nextState)
        assertEquals(pm3.reason, pm2.reason)
        assertNull(pm2.action)
    }

    @Test
    fun `mature pump is rejected as no fomo`() {
        val decision = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_3,
            FusionStabilityState(),
            observation(phase = BuyerBreathPhase.MATURE),
            1_000_000L
        )
        assertNull(decision.action)
        assertTrue(decision.reason.contains("NO_FOMO"))
    }

    @Test
    fun `absorbed early pump is rejected`() {
        val decision = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2,
            FusionStabilityState(),
            observation(absorption = 88),
            1_000_000L
        )
        assertNull(decision.action)
        assertTrue(decision.reason.contains("ABSORPTION"))
    }

    @Test
    fun `pm2 hard stop is tighter than legacy one point five`() {
        val decision = PumpProfitEngineV526.evaluatePosition(
            PumpProfitModeV526.PUMP_2,
            portfolio,
            FusionStabilityState(),
            observation(),
            bidForNet(-1.11),
            fee,
            3L * 60L * 1000L
        )
        assertEquals("EXIT", decision.action)
        assertTrue(decision.reason!!.contains("HARD_STOP_PM2"))
    }

    @Test
    fun `pm2 breakeven protects already earned net profit`() {
        val armed = PumpProfitEngineV526.evaluatePosition(
            PumpProfitModeV526.PUMP_2,
            portfolio,
            FusionStabilityState(),
            observation(),
            bidForNet(1.00),
            fee,
            5L * 60L * 1000L
        )
        assertNull(armed.action)
        assertTrue(armed.nextState.profitDefenseArmed)

        val protected = PumpProfitEngineV526.evaluatePosition(
            PumpProfitModeV526.PUMP_2,
            portfolio,
            armed.nextState,
            observation(),
            bidForNet(0.05),
            fee,
            6L * 60L * 1000L
        )
        assertEquals("EXIT", protected.action)
        assertTrue(protected.reason!!.contains("BREAKEVEN_PM2"))
    }

    @Test
    fun `pm2 releases dead capital after twenty minutes`() {
        val weak = observation(
            phase = BuyerBreathPhase.SELLER_TAKEOVER,
            instant = -4,
            score5 = -5,
            score15 = -3,
            score30 = -2
        )
        val decision = PumpProfitEngineV526.evaluatePosition(
            PumpProfitModeV526.PUMP_2,
            portfolio,
            FusionStabilityState(),
            weak,
            bidForNet(0.0),
            fee,
            21L * 60L * 1000L
        )
        assertEquals("EXIT", decision.action)
        assertTrue(decision.reason!!.contains("TIMEOUT_PM2"))
    }

    @Test
    fun `pm3 keeps constructive winner below target`() {
        val decision = PumpProfitEngineV526.evaluatePosition(
            PumpProfitModeV526.PUMP_3,
            portfolio,
            FusionStabilityState(),
            observation(instant = 18, score5 = 12, score15 = 7, score30 = 4),
            bidForNet(2.10),
            fee,
            12L * 60L * 1000L
        )
        assertNull(decision.action)
        assertTrue(decision.nextState.profitDefenseArmed)
    }
}
