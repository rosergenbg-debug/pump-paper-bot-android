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
        // Choose bid so liquidation after both simulated fees is just above +3% net.
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
        assertTrue(decision.reason.startsWith("TAKE_PROFIT_3_NET"))
    }

    @Test
    fun `minus one point five percent net stop exits immediately`() {
        val p = openPortfolio()
        val bid = 0.989
        val net = PumpMachinePolicy.tradeNetPercent(p, bid, fee)
        assertTrue(net <= -1.5)
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
        assertTrue(decision.reason.startsWith("STOP_LOSS_1_5_NET"))
    }

    @Test
    fun `ordinary fusion entry still needs two observations and sixty seconds`() {
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
        val secondTooSoon = PumpMachinePolicy.evaluate(
            portfolio = FusionSimPortfolio(),
            previous = first.nextState,
            frame = frame,
            bid = 1.0,
            feeRate = fee,
            now = 30_000L,
            shockReady = false,
            shockFailed = false,
            shockEntry = false,
            positionAgeMillis = Long.MAX_VALUE
        )
        assertNull(secondTooSoon.action)
        val confirmed = PumpMachinePolicy.evaluate(
            portfolio = FusionSimPortfolio(),
            previous = secondTooSoon.nextState,
            frame = frame,
            bid = 1.0,
            feeRate = fee,
            now = 62_000L,
            shockReady = false,
            shockFailed = false,
            shockEntry = false,
            positionAgeMillis = Long.MAX_VALUE
        )
        assertEquals("BUY", confirmed.action)
    }

    @Test
    fun `confirmed shock rebound can use the same fusion fast entry lane`() {
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
        assertEquals("BUY", decision.action)
        assertTrue(decision.reason.startsWith("SHOCK_REBOUND_ENTRY"))
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
