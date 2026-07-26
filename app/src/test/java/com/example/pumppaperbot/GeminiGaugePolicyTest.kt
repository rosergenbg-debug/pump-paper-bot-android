package com.example.pumppaperbot

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class GeminiGaugePolicyTest {
    private fun decision(at: Long) = GeminiHourlyDecision(
        id = 1L,
        decidedAt = at,
        candleTime = at,
        price = 0.01,
        requestedAction = "BUY",
        execution = "Виртуальная покупка",
        directionScore = 64,
        confidence = 72,
        horizonHours = 3,
        reason = "Тест",
        risks = emptyList(),
        model = "gemini-test",
        positionAfter = true,
        portfolioValueAfter = 1_000.0
    )

    @Test
    fun `fresh successful decision is visible on both gauges`() {
        val now = 10_000_000L
        val value = decision(now - 60_000L)

        assertSame(
            value,
            GeminiGaugePolicy.currentDecision(value, lastSuccess = now, lastFailure = 0L, now = now)
        )
    }

    @Test
    fun `failed or old decision is not shown as current`() {
        val now = 10_000_000L
        val fresh = decision(now - 60_000L)
        val old = decision(now - GeminiGaugePolicy.DECISION_FRESH_MILLIS - 1L)

        assertNull(
            GeminiGaugePolicy.currentDecision(fresh, lastSuccess = now - 2L, lastFailure = now, now = now)
        )
        assertNull(
            GeminiGaugePolicy.currentDecision(old, lastSuccess = now, lastFailure = 0L, now = now)
        )
    }

    @Test
    fun `missing decision stays missing`() {
        assertNull(
            GeminiGaugePolicy.currentDecision(
                null,
                lastSuccess = 0L,
                lastFailure = 0L,
                now = 1L
            )
        )
    }
}
