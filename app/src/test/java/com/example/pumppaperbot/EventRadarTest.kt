package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class EventRadarTest {
    @Test
    fun emergencyRateHikeIsRiskOffAndImportant() {
        val event = EventRadarClassifier.classify(
            RawMarketEvent(
                source = "ФРС",
                sourceUrl = "https://example.test/feed",
                title = "Federal Reserve announces emergency rate hike",
                summary = "Monetary policy response to inflation accelerating faster than expected.",
                link = "https://example.test/one",
                publishedAt = 1000L
            ),
            now = 2000L
        )

        assertTrue(event.directionScore <= -60)
        assertTrue(event.importance >= 75)
        assertTrue(event.confidence >= 60)
        assertFalse(event.aiAnalyzed)
    }

    @Test
    fun rateCutIsRiskOnButNotATradeCommand() {
        val event = EventRadarClassifier.classify(
            RawMarketEvent(
                source = "ЕЦБ",
                sourceUrl = "https://example.test/feed",
                title = "ECB announces emergency rate cut and liquidity facility",
                summary = "Monetary easing starts immediately.",
                link = "https://example.test/two",
                publishedAt = 1000L
            ),
            now = 2000L
        )

        assertTrue(event.directionScore >= 60)
        assertTrue(event.importance >= 75)
        assertEquals("Рынок подтверждает движение вверх", state(event).confirmation(35, 70))
        assertEquals("Рынок идёт против положительного события", state(event).confirmation(-35, 70))
    }

    @Test
    fun parserReadsRssAndResolvesRelativeLink() {
        val dateParser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val now = dateParser.parse("2026-07-19 12:00:00")!!.time
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"><channel><item>
              <title>Federal Reserve keeps interest rate unchanged</title>
              <description><![CDATA[Monetary policy statement & outlook.]]></description>
              <link>/newsevents/pressreleases/test.htm</link>
              <pubDate>Sat, 18 Jul 2026 14:00:00 GMT</pubDate>
            </item></channel></rss>
        """.trimIndent().toByteArray()

        val result = EventFeedParser.parse(
            xml,
            EventFeedSource("ФРС", "https://www.federalreserve.gov/feeds/press_monetary.xml"),
            now
        )

        assertEquals(1, result.size)
        assertEquals("Federal Reserve keeps interest rate unchanged", result.single().title)
        assertEquals(
            "https://www.federalreserve.gov/newsevents/pressreleases/test.htm",
            result.single().link
        )
        assertTrue(result.single().summary.contains("Monetary policy statement & outlook"))
    }

    @Test
    fun geminiResponseParserReadsAnalysisUsageAndGrounding() {
        val original = EventRadarClassifier.classify(
            RawMarketEvent("ФРС", "https://example.test", "Rate decision", "Policy text", "", 1000L),
            now = 2000L
        )
        val response = """
            {
              "candidates": [{
                "content": {"parts": [{"text": "{\"direction\":-32,\"importance\":77,\"confidence\":68,\"category\":\"СТАВКИ\",\"summary_ru\":\"Давление на рискованные активы.\",\"detailed_analysis_ru\":\"Подробный разбор.\",\"evidence\":[\"Ставка выше\"],\"risks\":[\"Рынок уже учёл\"],\"horizon_hours\":12}"}]},
                "groundingMetadata": {"groundingChunks": [{"web": {"title": "Federal Reserve", "uri": "https://federalreserve.gov"}}]}
              }],
              "usageMetadata": {"promptTokenCount": 120, "candidatesTokenCount": 30, "totalTokenCount": 150},
              "modelVersion": "gemini-3-flash-preview"
            }
        """.trimIndent()

        val parsed = GeminiResponseParser.parse(response, original)

        assertEquals(-32, parsed.event.directionScore)
        assertEquals(77, parsed.event.importance)
        assertTrue(parsed.event.aiAnalyzed)
        assertEquals(150, parsed.totalTokens)
        assertEquals(listOf("Federal Reserve"), parsed.webTitles)
        assertEquals("Подробный разбор.", parsed.detailedAnalysis)
        assertEquals(listOf("Ставка выше"), parsed.evidence)
        assertEquals(listOf("Рынок уже учёл"), parsed.risks)
        assertEquals(12, parsed.horizonHours)
    }

    @Test
    fun informationAdjustmentRequiresRealRecentAiEventAndIsCapped() {
        val event = EventRadarClassifier.classify(
            RawMarketEvent("PUMP НОВОСТИ", "", "PUMP event", "", "", 1_000L),
            now = 2_000L
        ).copy(aiAnalyzed = true)
        val state = EventRadarState(
            enabled = true,
            lastAttempt = 0L,
            lastSuccess = 2_000L,
            sourceCount = 7,
            latest = event,
            alertCandidate = null,
            recent = listOf(event),
            error = "",
            aiEnabled = true,
            aiConfigured = true,
            fetchBytes = 0,
            parsedEntries = 1,
            newEvents = 1,
            sourceChecks = emptyList(),
            gemini = GeminiDiagnostics(
                lastSuccess = 2_000L,
                status = "РАБОТАЕТ",
                inputTitle = event.title,
                directionScore = 100,
                importance = 100,
                confidence = 100
            )
        )

        assertEquals(12, state.informationAdjustment(now = 2_000L))
        assertEquals(100, state.combinedDirection(95, now = 2_000L))
        assertEquals(0, state.copy(recent = emptyList()).informationAdjustment(now = 2_000L))
        assertEquals(0, state.informationAdjustment(now = 25L * 60L * 60L * 1000L))
    }

    @Test
    fun emptyGeminiCandidateExplainsTokenExhaustion() {
        val event = EventRadarClassifier.classify(
            RawMarketEvent("ТЕСТ", "", "API test", "", "", 1_000L),
            now = 2_000L
        )
        val error = runCatching {
            GeminiResponseParser.parse(
                """{"candidates":[{"finishReason":"MAX_TOKENS"}],"usageMetadata":{"thoughtsTokenCount":305}}""",
                event
            )
        }.exceptionOrNull()

        assertTrue(error is GeminiApiException)
        assertTrue(error?.message.orEmpty().contains("MAX_TOKENS"))
        assertTrue(error?.message.orEmpty().contains("305"))
    }

    private fun state(event: MarketEvent) = EventRadarState(
        enabled = true,
        lastAttempt = 0L,
        lastSuccess = 0L,
        sourceCount = 1,
        latest = event,
        alertCandidate = null,
        recent = listOf(event),
        error = "",
        aiEnabled = false,
        aiConfigured = false,
        fetchBytes = 0,
        parsedEntries = 0,
        newEvents = 0,
        sourceChecks = emptyList(),
        gemini = GeminiDiagnostics()
    )
}
