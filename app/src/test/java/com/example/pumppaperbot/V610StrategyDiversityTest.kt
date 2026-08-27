package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V610StrategyDiversityTest {
    private fun observation(
        phase: BuyerBreathPhase = BuyerBreathPhase.EXPANSION,
        instant: Int = 18,
        score5: Int = 14,
        score15: Int = 8,
        score30: Int = 5,
        buy15s: Double = 62.0,
        buy60s: Double = 59.0,
        buy5mPct: Double = 60.0,
        price60: Double = 0.16,
        price5: Double = 0.35,
        move: Double = 0.45,
        absorption: Int = 25,
        appBookBid: Double = 90_000.0,
        appBookAsk: Double = 60_000.0,
        spread: Double = 0.08,
        capitalMode: CapitalFlowMode = CapitalFlowMode.MIXED,
        largeMode: LargeFlowMode = LargeFlowMode.MIXED,
        at: Long = 1_000_000L
    ): SharedFusionEntryObservation {
        val breath = BuyerBreathSnapshot(
            phase = phase,
            fresh = true,
            pressureScore = 45,
            efficiencyScore = 40,
            absorptionRisk = absorption,
            confidence = 80,
            buyerPercent5m = buy5mPct,
            buyerPercent15m = 57.0,
            priceChange5mPercent = price5,
            priceChange15mPercent = price5,
            activityRatio = 1.5,
            moveSincePhaseStartPercent = move
        )
        val breathing = LiveMarketBreathingSnapshot(
            updatedAt = at,
            fresh = true,
            historyMinutes = 30,
            instantScore = instant,
            normalScore = 20,
            horizons = listOf(
                LiveBreathingHorizon(5, score5, price5, buy5mPct, 70, 5),
                LiveBreathingHorizon(15, score15, price5, 57.0, 65, 15),
                LiveBreathingHorizon(20, score15, price5, 56.0, 60, 20),
                LiveBreathingHorizon(30, score30, price5, 55.0, 55, 30)
            ),
            buyerBreath = breath
        )
        val total5 = 200_000.0
        val buy5 = total5 * buy5mPct / 100.0
        val sell5 = total5 - buy5
        val micro = MicroImpulseSnapshot(
            connected = true,
            updatedAt = at,
            trades60s = 100,
            aggressiveBuyPercent15s = buy15s,
            aggressiveBuyPercent60s = buy60s,
            aggressiveBuyPercent5m = buy5mPct,
            priceChange60sPercent = price60,
            buyNotional5m = buy5,
            sellNotional5m = sell5,
            buyNotional15m = buy5 + 180_000.0,
            sellNotional15m = sell5 + 160_000.0,
            flowHistorySeconds = 900L,
            largeFlow = LargeFlowFingerprint(
                mode = largeMode,
                confidence = 20,
                largeBuyUsdt = 20_000.0,
                largeSellUsdt = 18_000.0
            )
        )
        return SharedFusionEntryObservation(
            frame = FusionFlowFrame(instant, score5, score15, score15, score30),
            shockReady = false,
            sampledAt = at,
            sampleBucket = at / 15_000L,
            breathing = breathing,
            micro = micro,
            executionAsk = 1.0,
            bookBidNotional = appBookBid,
            bookAskNotional = appBookAsk,
            bookSpreadPercent = spread,
            capitalFlow = CapitalFlowProxy(mode = capitalMode, score = 20, confidence = 80)
        )
    }

    @Test
    fun `retest rebound remains valid in mature phase after real pullback`() {
        val result = PumpVariantEntryPolicyV610.retestRebound(
            observation(
                phase = BuyerBreathPhase.MATURE,
                instant = 14,
                score5 = 8,
                score15 = 2,
                score30 = 0,
                buy15s = 63.0,
                buy60s = 58.0,
                buy5mPct = 58.0,
                price60 = 0.14,
                price5 = 0.20,
                move = 0.55
            ),
            pullbackPercent = 0.35,
            reboundPercent = 0.16
        )
        assertFalse(result.hardVeto)
        assertTrue(result.allowed)
    }

    @Test
    fun `safe can trade a strong local continuation without APP binary lock`() {
        val result = PumpVariantEntryPolicyV610.safeContinuation(
            observation(),
            appSupport = false
        )
        assertFalse(result.hardVeto)
        assertTrue(result.allowed)
    }

    @Test
    fun `safe still rejects seller takeover`() {
        val result = PumpVariantEntryPolicyV610.safeContinuation(
            observation(
                phase = BuyerBreathPhase.SELLER_TAKEOVER,
                instant = -14,
                score5 = -10,
                buy15s = 40.0,
                buy60s = 42.0,
                buy5mPct = 44.0
            ),
            appSupport = true
        )
        assertTrue(result.hardVeto)
        assertFalse(result.allowed)
    }

    @Test
    fun `flat PM flow scores lower than same flow with actual price response`() {
        val flat = AdaptiveBreathEntryPolicy.evaluate(
            PumpProfitModeV526.PUMP_2,
            observation(price60 = 0.0, price5 = 0.0, move = 0.0)
        )
        val moving = AdaptiveBreathEntryPolicy.evaluate(
            PumpProfitModeV526.PUMP_2,
            observation(price60 = 0.18, price5 = 0.22, move = 0.30)
        )
        assertTrue(moving.score > flat.score)
    }

    @Test
    fun `strict PM can recognise mature reacceleration without treating it as automatic late veto`() {
        val result = AdaptiveBreathEntryPolicy.evaluate(
            PumpProfitModeV526.PUMP_3,
            observation(
                phase = BuyerBreathPhase.MATURE,
                instant = 15,
                score5 = 13,
                score15 = 7,
                buy15s = 62.0,
                buy60s = 57.0,
                price60 = 0.18,
                price5 = 0.35,
                move = 0.60,
                absorption = 30
            )
        )
        assertFalse(result.hardVeto)
    }

    @Test
    fun `Fusion directional candidate does not require all medium horizons green`() {
        assertTrue(SharedFusionEntryPolicy.directionalCandidate(
            FusionFlowFrame(instant = 12, score5m = 8, score15m = -2, score20m = -1, score30m = -6)
        ))
        assertFalse(SharedFusionEntryPolicy.directionalCandidate(
            FusionFlowFrame(instant = 3, score5m = 1, score15m = 8, score20m = 8, score30m = 8)
        ))
    }

    @Test
    fun `Fusion capital gate does not require large BUY fingerprint`() {
        val result = CapitalParticipationGate.evaluate(
            observation(largeMode = LargeFlowMode.MIXED)
        )
        assertTrue(result.reason, result.allowed)
    }

    @Test
    fun `Fusion still rejects bearish capital mechanism`() {
        val result = CapitalParticipationGate.evaluate(
            observation(capitalMode = CapitalFlowMode.DISTRIBUTION)
        )
        assertFalse(result.allowed)
    }

    @Test
    fun `retest exits at advertised two percent NET target`() {
        val fee = 0.0025
        val entryCost = 1000.0
        val amount = (entryCost - entryCost * fee) / 1.0
        val portfolio = FusionSimPortfolio(
            cashEur = 0.0,
            pumpAmount = amount,
            entryPrice = 1.0,
            entryCostEur = entryCost
        )
        val targetBid = (1.0 + 2.01 / 100.0) * entryCost / (amount * (1.0 - fee))
        val decision = PumpProfitEngineV526.evaluatePosition(
            PumpProfitModeV526.PUMP_RETEST,
            portfolio,
            FusionStabilityState(),
            observation(),
            targetBid,
            fee,
            5L * 60L * 1000L
        )
        assertEquals("EXIT", decision.action)
        assertTrue(decision.reason!!.contains("TAKE_PROFIT_PM RETEST"))
    }
}
