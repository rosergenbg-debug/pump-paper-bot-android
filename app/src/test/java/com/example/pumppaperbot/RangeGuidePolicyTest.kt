package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RangeGuidePolicyTest {
    @Test
    fun `grid uses exact integer percentages one through four`() {
        val levels = RangeGuidePolicy.levels(100.0)!!
        assertEquals(listOf(1, 2, 3, 4), levels.levels.map { it.percent })
        assertEquals(listOf(101.0, 102.0, 103.0, 104.0), levels.levels.map { it.upper })
        assertEquals(listOf(99.0, 98.0, 97.0, 96.0), levels.levels.map { it.lower })
    }

    @Test
    fun `invalid reference produces no guide`() {
        assertNull(RangeGuidePolicy.levels(0.0))
        assertNull(RangeGuidePolicy.levels(Double.NaN))
    }
}
