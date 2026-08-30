package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RangeGuidePolicyTest {
    @Test
    fun `inner and outer ranges are exactly one and one point five percent`() {
        val levels = RangeGuidePolicy.levels(100.0)!!
        assertEquals(101.5, levels.outerUpper, 0.000000001)
        assertEquals(101.0, levels.innerUpper, 0.000000001)
        assertEquals(99.0, levels.innerLower, 0.000000001)
        assertEquals(98.5, levels.outerLower, 0.000000001)
        assertEquals(3.0, levels.outerUpper - levels.outerLower, 0.000000001)
        assertEquals(2.0, levels.innerUpper - levels.innerLower, 0.000000001)
    }

    @Test
    fun `invalid reference produces no guide`() {
        assertNull(RangeGuidePolicy.levels(0.0))
        assertNull(RangeGuidePolicy.levels(Double.NaN))
    }
}
