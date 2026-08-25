package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekEntryCoachTest {
    @Test
    fun `automatic tuning requires evidence and high confidence`() {
        val noHistory = DeepSeekEntryTuningPolicy.apply(
            DeepSeekEntryTuning(), "pm2_score_offset", 1, 95, 5, 100_000_000L, "test"
        )
        assertFalse(noHistory.applied)

        val weakConfidence = DeepSeekEntryTuningPolicy.apply(
            DeepSeekEntryTuning(), "pm2_score_offset", 1, 84, 10, 100_000_000L, "test"
        )
        assertFalse(weakConfidence.applied)
    }

    @Test
    fun `one bounded soft step is allowed per day`() {
        val first = DeepSeekEntryTuningPolicy.apply(
            DeepSeekEntryTuning(), "pm2_score_offset", 1, 90, 8, 100_000_000L, "late entries"
        )
        assertTrue(first.applied)
        assertEquals(1, first.tuning.pm2ScoreOffset)
        assertEquals(1, first.tuning.revision)

        val second = DeepSeekEntryTuningPolicy.apply(
            first.tuning, "chase_tightening", 1, 95, 9, 100_001_000L, "same day"
        )
        assertFalse(second.applied)
        assertEquals(0, second.tuning.chaseTighteningBps)
    }

    @Test
    fun `soft controls stay inside their safe bounds`() {
        val current = DeepSeekEntryTuning(pm2ScoreOffset = 6)
        val outcome = DeepSeekEntryTuningPolicy.apply(
            current, "pm2_score_offset", 1, 95, 12, 100_000_000L, "boundary"
        )
        assertFalse(outcome.applied)
        assertEquals(6, outcome.tuning.pm2ScoreOffset)
    }
}
