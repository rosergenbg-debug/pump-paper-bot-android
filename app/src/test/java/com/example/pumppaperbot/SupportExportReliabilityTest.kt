package com.example.pumppaperbot

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class SupportExportReliabilityTest {
    @Test fun `pass one empty twenty four hour json is valid`() {
        val payloads = SupportLogSplitPolicy.split(base(), emptyList(), 16_000)
        val files = writeParts(payloads, "json")

        SupportExportValidator.validateJsonFiles(files)
        assertEquals(1, files.size)
    }

    @Test fun `pass two normal json preserves every journal row`() {
        val events = (1..500).map { index ->
            JSONObject().put("time", index.toLong()).put("agent", "APP").put("detail", "event-$index")
        }
        val payloads = SupportLogSplitPolicy.split(base(), events, 18_000)
        val files = writeParts(payloads, "json")

        SupportExportValidator.validateJsonFiles(files)
        val recovered = files.sumOf { JSONObject(it.readText()).getJSONArray("journal").length() }
        assertEquals(events.size, recovered)
    }

    @Test fun `pass three heavy txt splits safely without losing rows`() {
        val summary = "windowHours=24\nCONTAINS_API_KEYS=false\n"
        val rows = (1..8_000).map { "$it\t" + "данные".repeat(20) }
        val payloads = V6ScalpReportStore.split(summary, rows, 32_000)
        val files = writeParts(payloads, "txt")

        SupportExportValidator.validateTextFiles(files)
        val recovered = files.sumOf { file ->
            file.useLines { lines -> lines.count { it.substringBefore('\t').toIntOrNull() != null } }
        }
        assertEquals(rows.size, recovered)
        assertTrue(files.size > 3)
    }

    private fun base() = JSONObject()
        .put("windowHours", 24)
        .put("safety", JSONObject().put("containsApiKeys", false))

    private fun writeParts(payloads: List<String>, extension: String) =
        Files.createTempDirectory("pump-support-export").toFile().let { dir ->
            payloads.mapIndexed { index, payload ->
                dir.resolve("part-${index + 1}.$extension").apply { writeText(payload, Charsets.UTF_8) }
            }
        }
}
