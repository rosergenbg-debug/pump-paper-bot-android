package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartDisplayLogicTest {
    @Test
    fun `zoom always performs a clear two-times step`() {
        assertEquals(60, nextChartVisibleBarLimit(120))
        assertEquals(30, nextChartVisibleBarLimit(60))
        assertEquals(120, nextChartVisibleBarLimit(30))
        assertEquals(120, nextChartVisibleBarLimit(240))
    }

    @Test
    fun `zoom button always describes the next action`() {
        assertEquals("УВЕЛИЧИТЬ ×2", chartZoomActionText(120))
        assertEquals("ЕЩЁ ×2", chartZoomActionText(60))
        assertEquals("ВЕРНУТЬ ОБЗОР", chartZoomActionText(30))
    }
}
