package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PumpMachinePolicyTest {
    private val fee = FusionTradingCosts.FEE_RATE

    private fun openPortfolio(ask: Double = 1.0, allocation: Double = 1000.0): FusionSimPortfolio {
        val buyFee = allocation * fee
        val amount = (allocation - buyFee) / ask
        return FusionSimPortfolio(
            cashEur = 0.0,
            pumpAmount = amount,
            entryPrice = ask,
            entryCostEur = allocation,
            peakValueEur = allocation,
            trades = listOf(
                FusionSimTrade(
                    time = 1_000L,
                    decisionId = 1_000L,
                    action = "BUY",
                    price = ask,
                    amount = amount,
                    feeEur = buyFee,
                    pnlEur = 0.0,
                    reason = "ENTRY_CONFIRMED"
                )
            )
        )
    }

    @Test
    fun `three percent net target exits immediately`() {
        val p = openPortfolio()
        val bid = 1.036
        val net = PumpMachinePolicy.tradeNetPercent(p, bid, fee)
        assertTrue(net >= 3.0)
        val decision = PumpMachinePolicy.evaluate(
            portfolio = p,
            previous = FusionStabilityState(peakBid = bid),
            frame = null,
            bid = bid,
            feeRate = fee,
            now = 700_000L,
            shockReady = false,
            shockFailed = false,
            shockEntry = false,
            positionAgeMillis = 699_000L
        )
        assertEquals("EXIT", decision.action)
        assertTrue(decision.reason.startsWith("V610_TAKE_PROFIT_PM3"))
    }

    @Test
    fun `v610 minus one point three percent net stop exits immediately`() {
        val p = openPortfolio()
        val bid = 0.989
        val net = PumpMachinePolicy.tradeNetPercent(p, bid, fee)
        assertTrue(net <= -1.3)
        val decision = PumpMachinePolicy.evaluate(
            portfolio = p,
            previous = FusionStabilityState(peakBid = 1.0),
            frame = null,
            bid = bid,
            feeRate = fee,
            now = 80_000L,
            shockReady = false,
            shockFailed = false,
            shockEntry = false,
            positionAgeMillis = 79_000L
        )
        assertEquals("EXIT", decision.action)
        assertTrue(decision.reason.startsWith("V610_HARD_STOP_PM3"))
    }

    @Test
    fun `legacy fusion frame alone no longer opens pm3`() {
        val frame = FusionFlowFrame(instant = 20, score5m = 18, score15m = 15, score20m = 12, score30m = 10)
        val first = PumpMachinePolicy.evaluate(
            portfolio = FusionSimPortfolio(),
            previous = FusionStabilityState(),
            frame = frame,
            bid = 1.0,
            feeRate = fee,
            now = 1_000L,
            shockReady = false,
            shockFailed = false,
            shockEntry = false,
            positionAgeMillis = Long.MAX_VALUE
        )
        assertNull(first.action)
        assertTrue(first.reason.contains("live breathing snapshot"))
    }

    @Test
    fun `shock rebound without real capital tape is rejected`() {
        val decision = PumpMachinePolicy.evaluate(
            portfolio = FusionSimPortfolio(),
            previous = FusionStabilityState(),
            frame = null,
            bid = 1.0,
            feeRate = fee,
            now = 100_000L,
            shockReady = true,
            shockFailed = false,
            shockEntry = false,
            positionAgeMillis = Long.MAX_VALUE
        )
        assertNull(decision.action)
        assertTrue(decision.reason.startsWith("V526_PM3_NO_FOMO") || decision.reason.startsWith("V610_PM3_NO_FOMO"))
    }

    @Test
    fun `system exit can close before three percent target`() {
        val p = openPortfolio()
        val frame = FusionFlowFrame(instant = -20, score5m = -18, score15m = -15, score20m = -12, score30m = 8)
        val first = PumpMachinePolicy.evaluate(
            portfolio = p,
            previous = FusionStabilityState(peakBid = 1.01),
            frame = frame,
            bid = 1.005,
            feeRate = fee,
            now = 700_000L,
            shockReady = false,
            shockFailed = false,
            shockEntry = false,
            positionAgeMillis = 699_000L
        )
        assertNull(first.action)
        val second = PumpMachinePolicy.evaluate(
            portfolio = p,
            previous = first.nextState,
            frame = frame,
            bid = 0.999,
            feeRate = fee,
            now = 820_000L,
            shockReady = false,
            shockFailed = false,
            shockEntry = false,
            positionAgeMillis = 819_000L
        )
        assertEquals("EXIT", second.action)
        assertTrue(second.reason.startsWith("SYSTEM_EXIT"))
    }
}