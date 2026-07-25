package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiPaperTraderTest {
    private fun recommendation(
        action: String,
        direction: Int = 70,
        confidence: Int = 75
    ) = GeminiHourlyRecommendation(
        action = action,
        directionScore = direction,
        confidence = confidence,
        horizonHours = 1,
        reason = "Тестовое решение",
        risks = listOf("Рынок может развернуться"),
        model = "gemini-test"
    )

    @Test fun `buys once for a fresh hourly decision`() {
        val bought = GeminiPaperTrader.applyDecision(
            GeminiPaperPortfolio(),
            price = 0.002,
            decisionId = 10L,
            candleTime = 100L,
            recommendation = recommendation("BUY"),
            now = 101L
        )
        assertTrue(bought.inPosition)
        assertEquals(1, bought.trades.size)
        assertEquals(998.5 / 0.002, bought.pumpAmount, 0.0001)

        val duplicate = GeminiPaperTrader.applyDecision(
            bought,
            price = 0.0021,
            decisionId = 10L,
            candleTime = 100L,
            recommendation = recommendation("SELL"),
            now = 102L
        )
        assertEquals(bought, duplicate)
    }

    @Test fun `sell closes the virtual position and includes both fees`() {
        val bought = GeminiPaperTrader.applyDecision(
            GeminiPaperPortfolio(), 0.002, 1L, 100L, recommendation("BUY"), 101L
        )
        val sold = GeminiPaperTrader.applyDecision(
            bought, 0.0022, 2L, 200L, recommendation("SELL", -60, 80), 201L
        )
        assertFalse(sold.inPosition)
        assertEquals(2, sold.trades.size)
        assertTrue(sold.cashEur > 1095.0)
        assertEquals(1, sold.closedTrades)
        assertEquals(1, sold.winningTrades)
        assertTrue(sold.totalFeesEur > 3.0)
    }

    @Test fun `next hour grades direction and captured surge`() {
        val bought = GeminiPaperTrader.applyDecision(
            GeminiPaperPortfolio(), 0.002, 50L, 100L, recommendation("BUY", 80, 80), 101L
        )
        val held = GeminiPaperTrader.applyDecision(
            bought, 0.00208, 51L, 200L, recommendation("HOLD", 20, 60), 201L
        )
        val graded = held.decisions.first()
        assertEquals(4.0, graded.evaluatedReturnPercent ?: 0.0, 0.0001)
        assertEquals(true, graded.directionCorrect)
        assertEquals(true, graded.surgeOpportunity)
        assertEquals(true, graded.surgeCaptured)
        assertEquals(100.0, held.surgeCapturePercent, 0.0001)
    }

    @Test fun `only full hour candle close is accepted`() {
        val fullHourClose = 60L * 60L * 1000L - 1L
        val halfHourClose = 30L * 60L * 1000L - 1L
        assertTrue(GeminiMarketFrame.isFullHourClose(fullHourClose))
        assertFalse(GeminiMarketFrame.isFullHourClose(halfHourClose))
    }

    @Test fun `parses structured Gemini hourly answer`() {
        val response = """
            {
              "modelVersion":"gemini-test",
              "candidates":[{"content":{"parts":[{"text":"{\"action\":\"HOLD\",\"direction\":12,\"confidence\":61,\"horizon_hours\":1,\"reason_ru\":\"Сигналы смешанные.\",\"risks\":[\"Объём слабый\"]}"}]}}],
              "usageMetadata":{"promptTokenCount":100,"candidatesTokenCount":40,"totalTokenCount":140}
            }
        """.trimIndent()
        val parsed = GeminiHourlyResponseParser.parse(response, "fallback")
        assertEquals("HOLD", parsed.recommendation.action)
        assertEquals(12, parsed.recommendation.directionScore)
        assertEquals("gemini-test", parsed.recommendation.model)
        assertEquals(140, parsed.totalTokens)
    }
}
