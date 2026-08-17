package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPaperTraderTest {
    @Test
    fun `trend runner has no fixed profit target and holds the activation candle`() {
        val candle = PumpCandle(
            openTime = 2_000L,
            open = 1.01,
            high = 1.12,
            low = 1.00,
            close = 1.10,
            volume = 1_000.0,
            closeTime = 3_000L
        )

        val result = StrategyV2.evaluateExit(
            candle = candle,
            positionMode = StrategyV2.MODE_TREND,
            entryPrice = 1.0,
            entryTime = 1_000L,
            partialTaken = false,
            storedHighestHigh = 1.0,
            evaluationTime = 3_000L
        )

        assertEquals(StrategyV2.ACTION_WAIT, result.action)
        assertTrue(result.reason.contains("ПОБЕДИТЕЛЬ"))
        assertTrue(result.reason.contains("без фиксированной цели"))
        assertEquals(1.12, result.highestHigh, 0.000001)
    }

    @Test
    fun `trend winner exits only on a later confirmed pullback`() {
        val candle = PumpCandle(
            openTime = 3_000L,
            open = 1.10,
            high = 1.11,
            low = 1.06,
            close = 1.065,
            volume = 1_000.0,
            closeTime = 4_000L
        )

        val result = StrategyV2.evaluateExit(
            candle = candle,
            positionMode = StrategyV2.MODE_TREND,
            entryPrice = 1.0,
            entryTime = 1_000L,
            partialTaken = false,
            storedHighestHigh = 1.12,
            evaluationTime = 4_000L
        )

        assertEquals(StrategyV2.ACTION_SELL, result.action)
        assertTrue(result.reason.contains("подтверждён откат"))
    }

    @Test
    fun `app buys once and ignores same candle`() {
        val buy = AppPaperEvaluation(
            candleTime = 1_000L,
            price = 2.0,
            action = "BUY",
            reason = "confirmed",
            strategyMode = StrategyV2.MODE_TREND,
            highestClose = 2.0
        )

        val opened = AppPaperTrader.apply(AppPaperPortfolio(), buy, now = 2_000L)
        val duplicate = AppPaperTrader.apply(opened, buy, now = 3_000L)

        assertTrue(opened.inPosition)
        assertEquals(499.25, opened.pumpAmount, 0.000001)
        assertEquals(1.5, opened.totalFeesEur, 0.000001)
        assertEquals(1, opened.trades.size)
        assertEquals(opened, duplicate)
    }

    @Test
    fun `research APP stores entry risk contract and clears it on exit`() {
        val buy = AppPaperEvaluation(
            candleTime = 1_000L,
            price = 2.0,
            action = "BUY",
            reason = "research setup",
            strategyMode = "TREND_PULLBACK",
            highestClose = 2.0,
            entryAtr = 0.08,
            invalidationPrice = 1.84,
            entryZoneLow = 1.96,
            entryZoneHigh = 2.02
        )
        val opened = AppPaperTrader.apply(AppPaperPortfolio(), buy, now = 1_000L)
        val closed = AppPaperTrader.apply(
            opened,
            buy.copy(candleTime = 2_000L, action = StrategyV2.ACTION_SELL, reason = "invalidation"),
            now = 2_000L
        )

        assertEquals(0.08, opened.entryAtr, 0.000001)
        assertEquals(1.84, opened.invalidationPrice, 0.000001)
        assertEquals(0.0, closed.entryAtr, 0.0)
        assertEquals(0.0, closed.invalidationPrice, 0.0)
    }

    @Test
    fun `app supports half exit followed by full exit`() {
        val opened = AppPaperTrader.apply(
            AppPaperPortfolio(),
            AppPaperEvaluation(
                1_000L, 2.0, "BUY", "entry",
                StrategyV2.MODE_SHOCK, 2.0
            ),
            now = 1_000L
        )
        val half = AppPaperTrader.apply(
            opened,
            AppPaperEvaluation(
                2_000L, 2.2, StrategyV2.ACTION_SELL_HALF, "target one",
                StrategyV2.MODE_SHOCK, 2.2
            ),
            now = 2_000L
        )
        val closed = AppPaperTrader.apply(
            half,
            AppPaperEvaluation(
                3_000L, 2.3, StrategyV2.ACTION_SELL, "exit",
                StrategyV2.MODE_SHOCK, 2.3
            ),
            now = 3_000L
        )

        assertTrue(half.inPosition)
        assertTrue(half.partialTaken)
        assertEquals(opened.pumpAmount / 2.0, half.pumpAmount, 0.000001)
        assertFalse(closed.inPosition)
        assertEquals(0.0, closed.pumpAmount, 0.0)
        assertEquals(listOf("BUY", "SELL_HALF", "SELL"), closed.trades.map { it.action })
        assertEquals(1, closed.closedTrades)
        assertTrue(closed.profit(2.3) > 0.0)
    }

    @Test
    fun `trade history remains while old decision log expires`() {
        val oldTime = 1_000L
        val old = AppPaperPortfolio(
            trades = listOf(
                AppPaperTrade(oldTime, oldTime, "BUY", 1.0, 998.5, 1.5, 0.0, "old")
            ),
            decisions = listOf(
                AppPaperDecision(oldTime, oldTime, "BUY", 1.0, "old", true, 998.5)
            )
        )
        val now = oldTime + AppPaperTrader.DECISION_RETENTION_MILLIS + 10L
        val next = AppPaperTrader.apply(
            old,
            AppPaperEvaluation(
                2_000L, 1.0, "WAIT", "new",
                StrategyV2.MODE_NONE, 0.0
            ),
            now = now
        )

        assertEquals(1, next.trades.size)
        assertEquals(1, next.decisions.size)
        assertEquals("new", next.decisions.single().reason)
    }

    @Test
    fun `app trade alert policy returns only newly executed trades`() {
        val before = AppPaperPortfolio()
        val after = AppPaperTrader.apply(
            before,
            AppPaperEvaluation(
                1_000L, 2.0, "BUY", "entry",
                StrategyV2.MODE_TREND, 2.0
            ),
            now = 2_000L
        )

        assertEquals(listOf("BUY"), AppTradeAlertPolicy.newlyExecutedTrades(before, after).map { it.action })
        assertTrue(AppTradeAlertPolicy.newlyExecutedTrades(after, after).isEmpty())
    }
}
