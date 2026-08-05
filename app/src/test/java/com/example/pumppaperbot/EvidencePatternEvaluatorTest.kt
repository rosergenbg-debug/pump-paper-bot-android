package com.example.pumppaperbot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidencePatternEvaluatorTest {
    @Test fun `pattern is promoted only after independent profitable lift and walk forward confirmation`() {
        val now = 2_000_000_000_000L
        val spacing = 16L * 60L * 1000L
        val points = List(36) { index ->
            EvidenceOutcomePoint(
                observedAt = now - (36L - index) * spacing,
                predictedDirection = 1,
                baselineDirection = if (index % 2 == 0) 1 else -1,
                returnPercent = if (index % 5 == 0) -0.6 else 1.1
            )
        }
        val result = EvidencePatternEvaluator.evaluate("режим", 15, points, now)
        assertTrue(result.independentCases >= 30)
        assertTrue(result.precisionPercent >= 60.0)
        assertTrue(result.validationPrecisionPercent >= 60.0)
        assertTrue(result.netExpectancyPercent > 0.0)
        assertTrue(result.liftPercent > 0.0)
        assertTrue(result.promoted)
    }

    @Test fun `many overlapping predictions do not fake thirty independent cases`() {
        val now = 2_000_000_000_000L
        val points = List(100) { index ->
            EvidenceOutcomePoint(
                observedAt = now - (100L - index) * 60_000L,
                predictedDirection = 1,
                baselineDirection = 0,
                returnPercent = 1.0
            )
        }
        val result = EvidencePatternEvaluator.evaluate("режим", 60, points, now)
        assertTrue(result.independentCases < 30)
        assertFalse(result.promoted)
    }

    @Test fun `sixty percent accuracy is insufficient when fees erase expectancy`() {
        val now = 2_000_000_000_000L
        val points = List(32) { index ->
            EvidenceOutcomePoint(
                observedAt = now - (32L - index) * 16L * 60L * 1000L,
                predictedDirection = 1,
                baselineDirection = if (index % 2 == 0) 1 else -1,
                returnPercent = if (index % 4 == 0) -0.2 else 0.2
            )
        }
        val result = EvidencePatternEvaluator.evaluate("режим", 15, points, now)
        assertTrue(result.precisionPercent >= 60.0)
        assertTrue(result.netExpectancyPercent <= 0.0)
        assertFalse(result.promoted)
    }
}
