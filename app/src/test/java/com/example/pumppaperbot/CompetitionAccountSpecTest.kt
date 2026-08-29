package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Test

class CompetitionAccountSpecTest {
    @Test
    fun comparisonContainsThreeAutosHumanSergeAndApp() {
        assertEquals(6, CompetitionAccountSpec.COUNT)
        assertEquals(
            listOf(
                "AUTO CORE",
                "AUTO BTC GUARD",
                "AUTO SOL/BTC SELECT",
                "HUMAN SELECT",
                "СЕРЖ",
                "APP"
            ),
            CompetitionAccountSpec.ORDER
        )
    }
}
