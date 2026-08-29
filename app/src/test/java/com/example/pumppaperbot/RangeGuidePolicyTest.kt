package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RangeGuidePolicyTest {
    @Test
    fun `range is exactly three percent around reference`() {
        val levels = RangeGuidePolicy.levels(100.0)!!
        assertEquals(101.5, levels.upper, 0.000000001)
        assertEquals(98.5, levels.lower, 0.000000001)
        assertEquals(3.0, levels.upper - levels.lower, 0.000000001)
    }

    @Test
    fun `invalid reference produces no guide`() {
        assertNull(RangeGuidePolicy.levels(0.0))
        assertNull(RangeGuidePolicy.levels(Double.NaN))
    }
}
