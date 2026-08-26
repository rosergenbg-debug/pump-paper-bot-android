package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V6ScalpReportTest {
    @Test fun `text report splits below the configured byte limit without losing rows`() {
        val summary = "HEADER\n[V6_EXECUTION_SAMPLES]\n"
        val rows = (1..120).map { index ->
            "$index\tPM1_CAND\tCONFIRMED\t80\t25.0\t5.0\t0.0\t0.0\t65.0\t0.1\t0.2\t1.0\tNA\tNA\t60\t55\t1.8\t0.2\t10\t8\t6\t4\t1.0\t1.1\tTier\t" + "x".repeat(80)
        }
        val parts = V6ScalpReportStore.split(summary, rows, maxBytes = 4_000)
        assertTrue(parts.size > 1)
        assertTrue(parts.all { it.toByteArray(Charsets.UTF_8).size <= 4_000 })
        val recovered = parts.sumOf { part ->
            part.lineSequence().count { line -> line.substringBefore('\t').toIntOrNull() != null }
        }
        assertEquals(rows.size, recovered)
    }
}
