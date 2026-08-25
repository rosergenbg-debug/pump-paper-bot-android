package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveTuningGuardV536Test {
    private val old = DeepSeekEntryTuning(revision = 2)
    private val changed = old.copy(pm2ScoreOffset = 1, revision = 3)
    private val start = 1_000_000L
    private fun row(t: Long, pnl: Double) = AdaptiveTuningClosedOutcome("PM2", t, pnl)

    @Test fun `trial waits and preserves old tuning`() {
        val base = (1..8).map { row(start - it, 2.0) }
        val trial = DeepSeekAdaptiveTuningPolicy.begin(old, "pm2_score_offset", base, start)
        assertEquals(old, trial.previousTuning)
        assertEquals("WAIT", DeepSeekAdaptiveTuningPolicy.evaluate(
            changed, trial, base + row(start + 1, 1.0), start + 2
        ).action)
    }

    @Test fun `six losses rollback`() {
        val base = (1..8).map { row(start - it, 1.5) }
        val trial = DeepSeekAdaptiveTuningPolicy.begin(old, "pm2_score_offset", base, start)
        val result = DeepSeekAdaptiveTuningPolicy.evaluate(
            changed, trial, base + (1..6).map { row(start + it, -1.0) }, start + 10
        )
        assertEquals("ROLLBACK", result.action)
        assertFalse(result.trial.active)
        assertEquals(old.pm2ScoreOffset, result.tuning.pm2ScoreOffset)
        assertTrue(result.tuning.revision > changed.revision)
    }

    @Test fun `six positive outcomes keep change`() {
        val base = (1..8).map { row(start - it, 0.25) }
        val trial = DeepSeekAdaptiveTuningPolicy.begin(old, "pm2_score_offset", base, start)
        val sample = listOf(2.0, 1.5, -0.4, 1.2, -0.2, 1.1)
            .mapIndexed { i, pnl -> row(start + i + 1, pnl) }
        assertEquals("KEEP", DeepSeekAdaptiveTuningPolicy.evaluate(
            changed, trial, base + sample, start + 10
        ).action)
    }
}
