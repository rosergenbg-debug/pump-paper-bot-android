package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPaperTraderTest {
    @Test
    fun buyAndSellUseSameFeesAsOtherCompetitors() {
        val opened = UserPaperTrader.buy(UserPaperPortfolio(), 2.0, 1_000L)
        assertTrue(opened.inPosition)
        assertEquals(499.25, opened.pumpAmount, 0.000001)
        assertEquals(1.5, opened.totalFeesEur, 0.000001)

        val closed = UserPaperTrader.sell(opened, 2.2, 2_000L)
        assertFalse(closed.inPosition)
        assertEquals(1096.702475, closed.cashEur, 0.000001)
        assertEquals(2, closed.trades.size)
        assertEquals(96.702475, closed.profit(2.2), 0.000001)
    }

    @Test
    fun duplicateActionsCannotOpenOrCloseTwice() {
        val opened = UserPaperTrader.buy(UserPaperPortfolio(), 1.0, 1_000L)
        assertEquals(opened, UserPaperTrader.buy(opened, 1.1, 2_000L))
        val closed = UserPaperTrader.sell(opened, 1.1, 3_000L)
        assertEquals(closed, UserPaperTrader.sell(closed, 1.2, 4_000L))
    }
}
