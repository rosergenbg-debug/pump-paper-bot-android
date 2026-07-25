package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiPaperTraderTest {
    @Test fun buysOnceForFreshStrongGeminiDecision() {
        val bought = GeminiPaperTrader.evaluate(
            GeminiPaperPortfolio(), 0.002, 10L, 70, 75, 30, 100L
        )
        assertTrue(bought.inPosition)
        assertEquals(1, bought.trades.size)
        assertEquals(998.5 / 0.002, bought.pumpAmount, 0.0001)
        assertEquals(bought, GeminiPaperTrader.evaluate(bought, 0.0021, 10L, 90, 90, 10))
    }

    @Test fun refusesWeakOrLateEntry() {
        assertFalse(GeminiPaperTrader.evaluate(
            GeminiPaperPortfolio(), 0.002, 1L, 54, 90, 10
        ).inPosition)
        assertFalse(GeminiPaperTrader.evaluate(
            GeminiPaperPortfolio(), 0.002, 2L, 90, 90, 66
        ).inPosition)
    }

    @Test fun takesProfitAndIncludesBothFees() {
        val bought = GeminiPaperTrader.evaluate(
            GeminiPaperPortfolio(), 0.002, 1L, 80, 80, 20, 100L
        )
        val sold = GeminiPaperTrader.evaluate(bought, 0.00213, 2L, 20, 70, 20, 200L)
        assertFalse(sold.inPosition)
        assertEquals(2, sold.trades.size)
        assertTrue(sold.cashEur > 1059.0)
        assertTrue(sold.cashEur < 1063.0)
    }
}
