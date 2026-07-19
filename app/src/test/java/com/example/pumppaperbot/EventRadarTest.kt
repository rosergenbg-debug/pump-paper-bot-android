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
        aiConfigured = false
    )
}
