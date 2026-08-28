package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompetitionAccountSpecTest {
    @Test
    fun comparisonContainsFourPumpMachinesFirstThenFusionAndLegacyAccounts() {
        assertEquals(10, CompetitionAccountSpec.COUNT)
        assertEquals(10, CompetitionAccountSpec.ORDER.size)
        assertEquals(listOf("PUMP 2% NET", "PUMP 3% NET", "PUMP RETEST", "PUMP SAFE"),
            CompetitionAccountSpec.ORDER.take(4))
        assertEquals("DEEPSIG FUSION", CompetitionAccountSpec.ORDER[4])
        assertTrue(CompetitionAccountSpec.ORDER.contains("DEEPSIGX"))
        assertTrue(CompetitionAccountSpec.ORDER.contains("APP"))
        assertTrue(CompetitionAccountSpec.ORDER.contains("DEEPSIG FUSION"))
        assertTrue(CompetitionAccountSpec.ORDER.contains("СЕРЖ"))
        assertEquals("VWAP 32,65 AUTO", CompetitionAccountSpec.ORDER[8])
        assertEquals("ЧЕЛОВЕЧЕСКИЙ ФАКТОР", CompetitionAccountSpec.ORDER[9])
    }
}
