package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Test

class CompetitionAccountSpecTest {
    @Test
    fun comparisonContainsFourT32BranchesPlusOwnerAndApp() {
        assertEquals(6, CompetitionAccountSpec.COUNT)
        assertEquals(
            listOf(
                "T32 ORIGINAL",
                "T32 +1,5% NET",
                "T32 +2,0% NET",
                "HUMAN +2,0% NET",
                "СЕРЖ",
                "APP"
            ),
            CompetitionAccountSpec.ORDER
        )
    }
}
