package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Test

class CompetitionAccountSpecTest {
    @Test
    fun comparisonContainsOnlyFourPrimaryAccountsInOwnerOrder() {
        assertEquals(4, CompetitionAccountSpec.COUNT)
        assertEquals(
            listOf("T32", "ЧЕЛОВЕЧЕСКИЙ ФАКТОР", "СЕРЖ", "APP"),
            CompetitionAccountSpec.ORDER
        )
    }
}
