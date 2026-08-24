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
        score15: Int = 8,
        score30: Int = 5,
        buyer5: Double = 66.0,
        absorption: Int = 20,
        efficiency: Int = 45,
        activity: Double = 1.8,
        move: Double = 0.45,
        fiveMinuteBuy: Double = 420_000.0,
        fiveMinuteSell: Double = 180_000.0,
        fifteenMinuteTotal: Double = 1_400_000.0,
        microAvailable: Boolean = true,
        largeMode: LargeFlowMode = LargeFlowMode.BUY_SERIES,
        largeConfidence: Int = 70,
        ask: Double = 1.0,
        bookBid: Double = 90_000.0,
        bookAsk: Double = 60_000.0,
        capitalMode: CapitalFlowMode = CapitalFlowMode.MIXED,
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
        val micro = if (microAvailable) MicroImpulseSnapshot(
            connected = true,
            updatedAt = at,
            trades60s = 120,
            buyNotional5m = fiveMinuteBuy,
            sellNotional5m = fiveMinuteSell,
            buyNotional15m = fiveMinuteBuy + (fifteenMinuteTotal - fiveMinuteBuy - fiveMinuteSell) * 0.58,
            sellNotional15m = fiveMinuteSell + (fifteenMinuteTotal - fiveMinuteBuy - fiveMinuteSell) * 0.42,
            flowHistorySeconds = 3_600L,
            largeFlow = LargeFlowFingerprint(
                mode = largeMode,
                confidence = largeConfidence,
                thresholdUsdt = 15_000.0,
                largeBuyUsdt = 150_000.0,
                largeSellUsdt = 40_000.0
            )
        ) else null
        return SharedFusionEntryObservation(
            frame = FusionFlowFrame(instant, score5, score15, score15, score30),
            shockReady = false,
            sampledAt = at,
            sampleBucket = at / 15_000L,
            breathing = breathing,
            micro = micro,
            executionAsk = ask,
            bookBidNotional = bookBid,
            bookAskNotional = bookAsk,
            bookSpreadPercent = 0.08,
            capitalFlow = CapitalFlowProxy(mode = capitalMode, score = 20, confidence = 90)
        )
    }

    private fun bidForNet(netPercent: Double): Double =
        (1.0 + netPercent / 100.0) * entryCost / (amount * (1.0 - fee))

    @Test
    fun `pm2 enters only after ninety second price acceptance`() {
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
            observation(at = 1_090_000L, ask = 1.0015),
            1_090_000L
        )
        assertEquals("BUY", second.action)
        assertTrue(second.reason.contains("CAPITAL_ACCEPTED"))
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
    fun `pm2 can confirm a new entry while pm3 remains in its own cooldown`() {
        val firstPm2 = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2,
            FusionStabilityState(),
            observation(),
            1_000_000L
        )
        val secondPm2 = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2,
            firstPm2.nextState,
            observation(at = 1_090_000L, ask = 1.0015),
            1_090_000L
        )
        val pm3 = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_3,
            FusionStabilityState(cooldownUntil = 1_120_000L),
            observation(at = 1_090_000L, ask = 1.0015),
            1_090_000L
        )

        assertEquals("BUY", secondPm2.action)
        assertNull(pm3.action)
        assertTrue(pm3.reason.contains("COOLDOWN"))
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
    fun `quiet low capital tape cannot arm pm2 or pm3`() {
        val empty = observation(
            fiveMinuteBuy = 38_000.0,
            fiveMinuteSell = 31_000.0,
            fifteenMinuteTotal = 220_000.0
        )
        listOf(PumpProfitModeV526.PUMP_2, PumpProfitModeV526.PUMP_3).forEach { mode ->
            val decision = PumpProfitEngineV526.evaluateEntry(
                mode, FusionStabilityState(), empty, 1_000_000L
            )
            assertNull(decision.action)
            assertEquals(0, decision.nextState.entryStreak)
            assertTrue(decision.reason.contains("CAPITAL_WAIT"))
        }
    }

    @Test
    fun `missing real trade tape fails closed`() {
        val decision = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2, FusionStabilityState(),
            observation(microAvailable = false), 1_000_000L
        )
        assertNull(decision.action)
        assertTrue(decision.reason.contains("CAPITAL_WAIT"))
    }

    @Test
    fun `broad turnover without a large buy series cannot arm`() {
        val decision = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2,
            FusionStabilityState(),
            observation(
                fiveMinuteBuy = 250_000.0,
                fiveMinuteSell = 110_000.0,
                largeMode = LargeFlowMode.MIXED,
                largeConfidence = 100
            ),
            1_000_000L
        )
        assertNull(decision.action)
        assertEquals(0, decision.nextState.entryStreak)
        assertTrue(decision.reason.contains("крупных BUY"))
    }

    @Test
    fun `ask heavy book blocks an otherwise strong capital setup`() {
        val decision = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_3,
            FusionStabilityState(),
            observation(bookBid = 45_000.0, bookAsk = 90_000.0),
            1_000_000L
        )
        assertNull(decision.action)
        assertEquals(0, decision.nextState.entryStreak)
        assertTrue(decision.reason.contains("стакан"))
    }

    @Test
    fun `capital that does not lift price remains armed but cannot buy`() {
        val first = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2, FusionStabilityState(), observation(), 1_000_000L
        )
        val later = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2, first.nextState,
            observation(at = 1_090_000L, ask = 0.9995), 1_090_000L
        )
        assertNull(later.action)
        assertTrue(later.reason.contains("не приняла капитал"))
    }

    @Test
    fun `late one point twenty five move is rejected`() {
        val decision = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_3, FusionStabilityState(), observation(move = 1.25), 1_000_000L
        )
        assertNull(decision.action)
        assertTrue(decision.reason.contains("NO_FOMO"))
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

    @Test
    fun `safe variant rejects a setup accepted by ordinary pump gate`() {
        val ordinary = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_2, FusionStabilityState(), observation(), 1_000_000L
        )
        val safe = PumpProfitEngineV526.evaluateEntry(
            PumpProfitModeV526.PUMP_SAFE, FusionStabilityState(),
            observation(instant = 12, score5 = 6, buyer5 = 60.0, activity = 1.12), 1_000_000L
        )
        assertEquals(1, ordinary.nextState.entryStreak)
        assertNull(safe.action)
        assertEquals(0, safe.nextState.entryStreak)
    }

    @Test
    fun `safe variant takes one point fifteen net target`() {
        val decision = PumpProfitEngineV526.evaluatePosition(
            PumpProfitModeV526.PUMP_SAFE, portfolio, FusionStabilityState(), observation(),
            bidForNet(1.16), fee, 5L * 60L * 1000L
        )
        assertEquals("EXIT", decision.action)
        assertTrue(decision.reason!!.contains("TAKE_PROFIT_PM SAFE"))
    }
}
