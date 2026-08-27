package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Synthetic market lifecycles based on recurring PUMP scalping shapes rather than one isolated
 * score tick. They intentionally feed the same causal flow/book/price evidence to the independent
 * strategies so future tuning cannot fix one bot by silently breaking the others.
 */
class V620SyntheticCandleScenariosTest {
    private fun observation(
        phase: BuyerBreathPhase = BuyerBreathPhase.EXPANSION,
        instant: Int = 24,
        score5: Int = 16,
        score15: Int = 8,
        score30: Int = 4,
        buy15s: Double = 68.0,
        buy60s: Double = 64.0,
        buy5mPct: Double = 62.0,
        price60: Double = 0.24,
        price5: Double = 0.38,
        move: Double = 0.42,
        tradeAcceleration: Double = 1.60,
        absorption: Int = 25,
        efficiency: Int = 50,
        activity: Double = 1.80,
        bookBid: Double = 100_000.0,
        bookAsk: Double = 50_000.0,
        spread: Double = 0.08,
        executionAsk: Double = 1.0,
        capitalMode: CapitalFlowMode = CapitalFlowMode.MIXED,
        at: Long = 1_000_000L
    ): SharedFusionEntryObservation {
        val breath = BuyerBreathSnapshot(
            phase = phase,
            fresh = true,
            pressureScore = 50,
            efficiencyScore = efficiency,
            absorptionRisk = absorption,
            confidence = 85,
            buyerPercent5m = buy5mPct,
            buyerPercent15m = 58.0,
            priceChange5mPercent = price5,
            priceChange15mPercent = price5,
            activityRatio = activity,
            moveSincePhaseStartPercent = move
        )
        val breathing = LiveMarketBreathingSnapshot(
            updatedAt = at,
            fresh = true,
            historyMinutes = 120,
            instantScore = instant,
            normalScore = 20,
            horizons = listOf(
                LiveBreathingHorizon(5, score5, price5, buy5mPct, 75, 5),
                LiveBreathingHorizon(15, score15, price5, 58.0, 65, 15),
                LiveBreathingHorizon(20, score15, price5, 57.0, 60, 20),
                LiveBreathingHorizon(30, score30, price5, 56.0, 55, 30)
            ),
            buyerBreath = breath
        )
        val total5 = 250_000.0
        val buy5 = total5 * buy5mPct / 100.0
        val sell5 = total5 - buy5
        val micro = MicroImpulseSnapshot(
            connected = true,
            updatedAt = at,
            trades60s = 160,
            tradeAcceleration = tradeAcceleration,
            aggressiveBuyPercent5s = buy15s,
            aggressiveBuyPercent15s = buy15s,
            aggressiveBuyPercent60s = buy60s,
            aggressiveBuyPercent5m = buy5mPct,
            priceChange60sPercent = price60,
            spreadPercent = spread,
            topBookImbalance = (bookBid - bookAsk) / (bookBid + bookAsk),
            buyNotional5m = buy5,
            sellNotional5m = sell5,
            buyNotional15m = buy5 + 180_000.0,
            sellNotional15m = sell5 + 160_000.0,
            flowHistorySeconds = 900L,
            largeFlow = LargeFlowFingerprint(
                mode = LargeFlowMode.MIXED,
                confidence = 35,
                largeBuyUsdt = 35_000.0,
                largeSellUsdt = 25_000.0
            )
        )
        return SharedFusionEntryObservation(
            frame = FusionFlowFrame(instant, score5, score15, score15, score30),
            shockReady = false,
            sampledAt = at,
            sampleBucket = at / 15_000L,
            breathing = breathing,
            micro = micro,
            executionAsk = executionAsk,
            bookBidNotional = bookBid,
            bookAskNotional = bookAsk,
            bookSpreadPercent = spread,
            capitalFlow = CapitalFlowProxy(mode = capitalMode, score = 25, confidence = 85)
        )
    }

    private fun appEvidence(
        appBuySignal: Boolean,
        appReadiness: Int,
        aiAction: String,
        aiReadiness: Int,
        aiDirection: Int,
        aiConfidence: Int,
        buy60s: Double,
        price60: Double,
        score5: Int,
        score15: Int,
        score30: Int,
        score60: Int
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
        pumpBuyerPercent60s = buy60s,
        pumpChange60sPercent = price60,
        bitcoinBuyerPercent60s = 50.0,
        bitcoinChange60sPercent = 0.01,
        breathing5m = score5,
        breathing15m = score15,
        breathing30m = score30,
        breathing60m = score60
    )

    private fun portfolio(entryPrice: Double = 1.0): FusionSimPortfolio {
        val fee = FusionTradingCosts.FEE_RATE
        val entryCost = 1_000.0
        return FusionSimPortfolio(
            cashEur = 0.0,
            pumpAmount = (entryCost - entryCost * fee) / entryPrice,
            entryPrice = entryPrice,
            entryCostEur = entryCost
        )
    }

    private fun bidForNet(value: FusionSimPortfolio, netPercent: Double): Double {
        val fee = FusionTradingCosts.FEE_RATE
        return (1.0 + netPercent / 100.0) * value.entryCostEur /
            (value.pumpAmount * (1.0 - fee))
    }

    @Test
    fun `scenario one first green bounce inside deep falling medium trend stays observation only`() {
        val bounce = observation(
            phase = BuyerBreathPhase.IGNITION,
            instant = 16,
            score5 = 5,
            score15 = -28,
            score30 = -32,
            buy15s = 64.0,
            buy60s = 58.0,
            buy5mPct = 54.0,
            price60 = 0.18,
            price5 = -0.70,
            move = 0.18,
            tradeAcceleration = 1.45,
            absorption = 38,
            efficiency = 8,
            activity = 1.25
        )

        val lifecycle = PrimaryImpulseLifecycleV620.assess(bounce)
        assertEquals(PrimaryImpulseRegimeV620.REVERSAL_SEED, lifecycle.regime)
        assertTrue(lifecycle.keepFastTracking)
        assertTrue(lifecycle.blockPrimaryEntry)

        val pm1 = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2, FusionStabilityState(), bounce, bounce.sampledAt
        )
        val pm2 = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_3, FusionStabilityState(), bounce, bounce.sampledAt
        )
        assertNull(pm1.action)
        assertNull(pm2.action)
        assertTrue(pm1.reason.contains("REVERSAL_SEED"))
        assertTrue(pm2.reason.contains("REVERSAL_SEED"))

        val retest = PumpVariantEntryPolicyV610.retestRebound(bounce, 0.35, 0.15)
        val safe = PumpVariantEntryPolicyV610.safeContinuation(bounce, appSupport = false)
        assertFalse(retest.allowed)
        assertFalse(safe.allowed)
        assertFalse(SharedFusionEntryPolicy.directionalCandidate(bounce.frame))

        val appAndDeepSig = AppLedHybridPolicy.entry(
            appEvidence(
                appBuySignal = false,
                appReadiness = 45,
                aiAction = "WATCH",
                aiReadiness = 8,
                aiDirection = 60,
                aiConfidence = 72,
                buy60s = 58.0,
                price60 = 0.18,
                score5 = 5,
                score15 = -28,
                score30 = -32,
                score60 = -18
            )
        )
        assertEquals("WATCH", appAndDeepSig.tradeAction)
        assertFalse(appAndDeepSig.independentDeepSeekSetup)
        assertTrue(appAndDeepSig.structuralWeakness)
    }

    @Test
    fun `scenario two aligned ignition becomes actionable across independent lanes without waiting for whole move`() {
        val first = observation()
        val second15s = observation(at = 1_015_000L, executionAsk = 1.0025)

        val pm1Armed = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2, FusionStabilityState(), first, first.sampledAt
        )
        val pm2Armed = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_3, FusionStabilityState(), first, first.sampledAt
        )
        assertEquals(1, pm1Armed.nextState.entryStreak)
        assertEquals(1, pm2Armed.nextState.entryStreak)

        val pm1Buy = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2, pm1Armed.nextState, second15s, second15s.sampledAt
        )
        val pm2Buy = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_3, pm2Armed.nextState, second15s, second15s.sampledAt
        )
        assertEquals("BUY", pm1Buy.action)
        assertEquals("BUY", pm2Buy.action)

        val safe = PumpVariantEntryPolicyV610.safeContinuation(first, appSupport = false)
        assertTrue(safe.reason, safe.allowed)

        val fusionFirst = SharedFusionEntryPolicy.evaluate(FusionStabilityState(), first, first.sampledAt)
        val fusionSecond = SharedFusionEntryPolicy.evaluate(
            fusionFirst.nextState,
            observation(at = 1_060_000L, executionAsk = 1.0030),
            1_060_000L
        )
        assertEquals("BUY", fusionSecond.action)

        val appAndDeepSig = AppLedHybridPolicy.entry(
            appEvidence(
                appBuySignal = true,
                appReadiness = 92,
                aiAction = "WATCH",
                aiReadiness = 9,
                aiDirection = 68,
                aiConfidence = 74,
                buy60s = 64.0,
                price60 = 0.24,
                score5 = 31,
                score15 = 18,
                score30 = 6,
                score60 = 4
            )
        )
        assertEquals("BUY", appAndDeepSig.tradeAction)
        assertTrue(appAndDeepSig.independentDeepSeekSetup)
    }

    @Test
    fun `scenario three shallow shakeout after correct entry is not mistaken for fee sized market failure`() {
        val value = portfolio()
        val transientShakeout = observation(
            phase = BuyerBreathPhase.SELLER_TAKEOVER,
            instant = -2,
            score5 = -1,
            score15 = 2,
            score30 = 4,
            buy15s = 47.0,
            buy60s = 50.0,
            buy5mPct = 52.0,
            price60 = -0.12,
            price5 = 0.05,
            move = 0.35,
            tradeAcceleration = 1.05,
            absorption = 50,
            efficiency = 5,
            activity = 1.0
        )
        val shakeoutBid = 0.9970 // market only -0.30%; NET is lower because two 0.21% fees exist.

        val pm1Hold = PumpProfitEngineV526.evaluatePosition(
            PumpProfitModeV526.PUMP_2,
            value,
            FusionStabilityState(),
            transientShakeout,
            shakeoutBid,
            FusionTradingCosts.FEE_RATE,
            2L * 60L * 1000L
        )
        val pm2Hold = PumpProfitEngineV526.evaluatePosition(
            PumpProfitModeV526.PUMP_3,
            value,
            FusionStabilityState(),
            transientShakeout,
            shakeoutBid,
            FusionTradingCosts.FEE_RATE,
            2L * 60L * 1000L
        )
        assertNull(pm1Hold.action)
        assertNull(pm2Hold.action)
        assertTrue(pm1Hold.tradeNetPercent < -0.60)
        assertEquals(-0.30, PumpProfitEngineV526.priceMovePercent(1.0, shakeoutBid), 0.001)

        val continuation = observation(
            phase = BuyerBreathPhase.EXPANSION,
            instant = 20,
            score5 = 13,
            score15 = 7,
            score30 = 4,
            buy15s = 65.0,
            buy60s = 61.0,
            buy5mPct = 59.0,
            price60 = 0.19,
            price5 = 0.30,
            move = 0.50
        )
        val pm1Target = PumpProfitEngineV526.evaluatePosition(
            PumpProfitModeV526.PUMP_2,
            value,
            pm1Hold.nextState,
            continuation,
            bidForNet(value, 2.01),
            FusionTradingCosts.FEE_RATE,
            6L * 60L * 1000L
        )
        val pm2Target = PumpProfitEngineV526.evaluatePosition(
            PumpProfitModeV526.PUMP_3,
            value,
            pm2Hold.nextState,
            continuation,
            bidForNet(value, 3.01),
            FusionTradingCosts.FEE_RATE,
            8L * 60L * 1000L
        )
        assertEquals("EXIT", pm1Target.action)
        assertEquals("EXIT", pm2Target.action)
        assertTrue(pm1Target.reason!!.contains("TAKE_PROFIT"))
        assertTrue(pm2Target.reason!!.contains("TAKE_PROFIT"))

        val retest = PumpVariantEntryPolicyV610.retestRebound(
            observation(
                phase = BuyerBreathPhase.MATURE,
                instant = 14,
                score5 = 8,
                score15 = 3,
                score30 = 1,
                buy15s = 63.0,
                buy60s = 58.0,
                buy5mPct = 57.0,
                price60 = 0.14,
                price5 = 0.18,
                move = 0.55
            ),
            pullbackPercent = 0.35,
            reboundPercent = 0.16
        )
        assertTrue(retest.reason, retest.allowed)

        val appExit = AppLedHybridPolicy.exit(
            AppLedExitEvidence(
                modelRequestsExit = true,
                appExitSignal = false,
                rapidDropUnrecovered = false,
                currentReturnPercent = -0.30,
                positionAgeMillis = 5L * 60L * 1000L,
                microFresh = true,
                pumpBuyerPercent15s = 47.0,
                pumpBuyerPercent60s = 49.0,
                pumpBuyerPercent5m = 51.0,
                pumpChange60sPercent = -0.12,
                breathing5m = -1,
                breathing15m = 2,
                breathing30m = 4,
                breathing60m = 5
            )
        )
        assertFalse(appExit.allowExit)
    }

    @Test
    fun `scenario four one stopped false start can rearm on a genuinely new aligned regime but two losses cannot`() {
        val now = 2_000_000L
        val strongNewRegime = observation(
            at = now,
            phase = BuyerBreathPhase.IGNITION,
            instant = 22,
            score5 = 14,
            score15 = 6,
            score30 = 3,
            buy15s = 67.0,
            buy60s = 62.0,
            buy5mPct = 60.0,
            price60 = 0.20,
            price5 = 0.28,
            move = 0.32,
            tradeAcceleration = 1.55,
            absorption = 28
        )
        val oneLoss = FusionStabilityState(
            cooldownUntil = now + 15L * 60L * 1000L,
            lastExitAt = now - 5L * 60L * 1000L,
            lastLossExitAt = now - 5L * 60L * 1000L,
            lossExitStreak = 1
        )
        assertTrue(PrimaryImpulseLifecycleV620.cooldownRescueEligible(
            PumpProfitModeV526.PUMP_2, oneLoss, strongNewRegime, now
        ))

        val armed = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2, oneLoss, strongNewRegime, now
        )
        assertNull(armed.action)
        assertEquals(1, armed.nextState.entryStreak)
        assertTrue(armed.nextState.cooldownUntil > now)
        assertTrue(armed.reason.contains("COOLDOWN_RESCUE"))

        val confirmed = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2,
            armed.nextState,
            observation(
                at = now + 15_000L,
                executionAsk = 1.0020,
                phase = BuyerBreathPhase.IGNITION,
                instant = 22,
                score5 = 14,
                score15 = 6,
                score30 = 3,
                buy15s = 67.0,
                buy60s = 62.0,
                buy5mPct = 60.0,
                price60 = 0.20,
                price5 = 0.28,
                move = 0.32,
                tradeAcceleration = 1.55,
                absorption = 28
            ),
            now + 15_000L
        )
        assertEquals("BUY", confirmed.action)
        assertEquals(0L, confirmed.nextState.cooldownUntil)

        val twoLosses = oneLoss.copy(lossExitStreak = 2)
        val blocked = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2, twoLosses, strongNewRegime, now
        )
        assertNull(blocked.action)
        assertEquals(0, blocked.nextState.entryStreak)
        assertTrue(blocked.reason.contains("COOLDOWN"))
        assertFalse(blocked.reason.contains("RESCUE"))
    }

    @Test
    fun `paper execution fee for V6_2 is point twenty one percent per side`() {
        assertEquals(0.0021, FusionTradingCosts.FEE_RATE, 0.0)
        assertTrue(FusionTradingCosts.FEE_TIER.contains("0,21%"))
    }
}
