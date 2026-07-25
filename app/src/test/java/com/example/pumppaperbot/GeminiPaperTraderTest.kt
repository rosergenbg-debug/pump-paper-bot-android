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
        val gradedPortfolio = GeminiPaperTrader.gradeCompletedHours(
            bought,
            listOf(GeminiHourOutcome(50L, closePrice = 0.00208, highPrice = 0.00210))
        )
        val held = GeminiPaperTrader.applyDecision(
            gradedPortfolio, 0.00208, 51L, 200L, recommendation("HOLD", 20, 60), 201L
        )
        val graded = held.decisions.first()
        assertEquals(4.0, graded.evaluatedReturnPercent ?: 0.0, 0.0001)
        assertEquals(5.0, graded.peakReturnPercent ?: 0.0, 0.0001)
        assertEquals(true, graded.directionCorrect)
        assertEquals(true, graded.surgeOpportunity)
        assertEquals(true, graded.surgeCaptured)
        assertEquals(100.0, held.surgeCapturePercent, 0.0001)
    }

    @Test fun `missed api hour can still be graded later`() {
        val first = GeminiPaperTrader.applyDecision(
            GeminiPaperPortfolio(), 0.002, 50L, 100L, recommendation("HOLD", 40, 70), 101L
        )
        val graded = GeminiPaperTrader.gradeCompletedHours(
            first,
            listOf(GeminiHourOutcome(50L, closePrice = 0.00201, highPrice = 0.00208))
        )
        assertEquals(0.5, graded.decisions.first().evaluatedReturnPercent ?: 0.0, 0.0001)
        assertEquals(true, graded.decisions.first().surgeOpportunity)
        assertEquals(false, graded.decisions.first().surgeCaptured)
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

    @Test fun `first automatic attempt is allowed for a new closed hour`() {
        val decision = GeminiHourlyRetryPolicy.automaticDecision(
            frameHourId = 20L,
            lastAttemptHour = 19L,
            attemptsThisHour = 3,
            lastAttempt = 1_000L,
            now = 2_000L
        )
        assertTrue(decision.allowed)
    }

    @Test fun `failed hour waits five minutes before retry`() {
        val now = 1_000_000L
        val waiting = GeminiHourlyRetryPolicy.automaticDecision(
            frameHourId = 20L,
            lastAttemptHour = 20L,
            attemptsThisHour = 1,
            lastAttempt = now,
            now = now + 4L * 60L * 1000L
        )
        assertFalse(waiting.allowed)
        assertEquals(now + 5L * 60L * 1000L, waiting.nextAttemptAt)

        val ready = GeminiHourlyRetryPolicy.automaticDecision(
            frameHourId = 20L,
            lastAttemptHour = 20L,
            attemptsThisHour = 1,
            lastAttempt = now,
            now = now + 5L * 60L * 1000L
        )
        assertTrue(ready.allowed)
    }

    @Test fun `automatic retries stop after three attempts in one hour`() {
        val blocked = GeminiHourlyRetryPolicy.automaticDecision(
            frameHourId = 20L,
            lastAttemptHour = 20L,
            attemptsThisHour = 3,
            lastAttempt = 1_000L,
            now = 2_000L
        )
        assertFalse(blocked.allowed)
        assertTrue(blocked.status.contains("3/3"))
    }

    @Test fun `activity event preserves observable request details`() {
        val original = GeminiActivityEvent(
            at = 123_456L,
            stage = "GEMINI API",
            result = "OK",
            detail = "Ответ получен",
            durationMillis = 4_321L,
            model = "gemini-test",
            hourId = 77L,
            attempt = 2
        )
        assertEquals(original, GeminiActivityEvent.fromJson(original.toJson()))
    }

    @Test fun `cycle guard blocks overlapping checks and recovers`() {
        assertTrue(GeminiCycleGuard.tryEnter())
        assertFalse(GeminiCycleGuard.tryEnter())
        GeminiCycleGuard.exit()
        assertTrue(GeminiCycleGuard.tryEnter())
        GeminiCycleGuard.exit()
    }
}
