package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class WeekRhythmTest {
    @Test
    fun `monday morning uses Berlin time`() {
        val time = berlinTime(2026, Calendar.JULY, 20, 8)
        val context = WeekRhythm.at(time)

        assertEquals("MONDAY_EXHALE", context.code)
        assertTrue(context.caution)
    }

    @Test
    fun `weekend is quiet context not a trade ban`() {
        val time = berlinTime(2026, Calendar.JULY, 18, 14)
        val context = WeekRhythm.at(time)

        assertEquals("WEEKEND_QUIET", context.code)
        assertTrue(context.caution)
    }

    @Test
    fun `ordinary Wednesday is neutral`() {
        val time = berlinTime(2026, Calendar.JULY, 22, 14)
        val context = WeekRhythm.at(time)

        assertEquals("NEUTRAL", context.code)
        assertFalse(context.caution)
    }

    private fun berlinTime(year: Int, month: Int, day: Int, hour: Int): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin")).apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis
    }
}
