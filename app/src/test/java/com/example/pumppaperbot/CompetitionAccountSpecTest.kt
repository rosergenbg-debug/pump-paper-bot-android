package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompetitionAccountSpecTest {
    @Test
    fun comparisonContainsAllSixActiveAccountsIncludingPm2() {
        assertEquals(6, CompetitionAccountSpec.COUNT)
        assertEquals(6, CompetitionAccountSpec.ORDER.size)
        assertEquals("PUMP 3% NET", CompetitionAccountSpec.ORDER[0])
        assertEquals("PUMP 2% NET", CompetitionAccountSpec.ORDER[1])
        assertTrue(CompetitionAccountSpec.ORDER.contains("DEEPSIGX"))
        assertTrue(CompetitionAccountSpec.ORDER.contains("APP"))
        assertTrue(CompetitionAccountSpec.ORDER.contains("DEEPSIG FUSION"))
        assertTrue(CompetitionAccountSpec.ORDER.contains("СЕРЖ"))
    }
}
