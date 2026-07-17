package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `tooltip stays opposite the finger and inside the plot`() {
        val fromBottomRight = chartTooltipPosition(
            touchX = 850f,
            touchY = 700f,
            plotLeft = 20f,
            plotRight = 900f,
            plotTop = 100f,
            plotBottom = 800f,
            tooltipWidth = 300f,
            tooltipHeight = 150f,
            clearance = 60f
        )
        assertTrue(fromBottomRight.left + 300f < 850f)
        assertTrue(fromBottomRight.top + 150f < 700f)

        val fromTopLeft = chartTooltipPosition(
            touchX = 80f,
            touchY = 140f,
            plotLeft = 20f,
            plotRight = 900f,
            plotTop = 100f,
            plotBottom = 800f,
            tooltipWidth = 300f,
            tooltipHeight = 150f,
            clearance = 60f
        )
        assertTrue(fromTopLeft.left > 80f)
        assertTrue(fromTopLeft.top > 140f)
        assertTrue(fromTopLeft.left + 300f <= 900f)
        assertTrue(fromTopLeft.top + 150f <= 800f)
    }
}
