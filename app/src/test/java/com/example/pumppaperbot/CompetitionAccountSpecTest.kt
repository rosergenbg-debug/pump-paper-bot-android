package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Test

class CompetitionAccountSpecTest {
    @Test
    fun comparisonContainsTwoXAutosHumanSergeAndApp() {
        assertEquals(5, CompetitionAccountSpec.COUNT)
        assertEquals(
            listOf(
                "AUTO X ECONOMY",
                "AUTO X52 SELECT",
                "HUMAN +2,0% NET",
                "СЕРЖ",
                "APP"
            ),
            CompetitionAccountSpec.ORDER
        )
    }
}
