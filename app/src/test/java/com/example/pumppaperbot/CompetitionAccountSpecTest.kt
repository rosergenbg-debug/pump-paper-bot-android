package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Test

class CompetitionAccountSpecTest {
    @Test
    fun comparisonContainsExactlyThreeAutosPlusHuman() {
        assertEquals(4, CompetitionAccountSpec.COUNT)
        assertEquals(
            listOf(
                "AUTO CORE",
                "AUTO BTC GUARD",
                "AUTO SOL/BTC SELECT",
                "HUMAN SELECT"
            ),
            CompetitionAccountSpec.ORDER
        )
    }
}
