package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class GeminiRequestBudgetTest {
    @Test fun `only position advisor receives the full remaining daily quota`() {
        assertEquals(12, GeminiRequestBudget.activeLimit(positionOpen = false))
        assertEquals(12, GeminiRequestBudget.activeLimit(positionOpen = true, positionPriority = false))
        assertEquals(25, GeminiRequestBudget.activeLimit(positionOpen = true, positionPriority = true))
    }

    @Test fun `quota day follows Pacific midnight`() {
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val beforePacificMidnight = parser.parse("2026-07-25 06:59:59")!!.time
        val afterPacificMidnight = parser.parse("2026-07-25 07:00:01")!!.time

        assertEquals("2026-07-24", GeminiRequestBudget.pacificDayKey(beforePacificMidnight))
        assertEquals("2026-07-25", GeminiRequestBudget.pacificDayKey(afterPacificMidnight))
        assertTrue(GeminiRequestBudget.nextPacificReset(beforePacificMidnight) > beforePacificMidnight)
    }

    @Test fun `daily quota errors are distinguished from minute limits`() {
        assertTrue(GeminiRequestBudget.isDailyQuotaMessage("GenerateRequestsPerModelPerDay-FreeTier"))
        assertTrue(GeminiRequestBudget.isDailyQuotaMessage("Requests per day quota exceeded"))
    }
}
