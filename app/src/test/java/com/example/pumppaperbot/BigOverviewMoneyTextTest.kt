package com.example.pumppaperbot

import org.junit.Assert.assertTrue
import org.junit.Test

class BigOverviewMoneyTextTest {
    @Test
    fun `money overview formats positive net flow without runtime exception`() {
        val snapshot = MicroImpulseSnapshot(
            buyNotional60s = 125_000.0,
            sellNotional60s = 75_000.0,
            largeFlow = LargeFlowFingerprint(
                confidence = 82,
                thresholdUsdt = 20_000.0,
                largestBuyUsdt = 55_000.0,
                largestSellUsdt = 25_000.0,
                largeBuyUsdt = 110_000.0,
                largeSellUsdt = 30_000.0,
                buySlices = 4,
                sellSlices = 1,
                title = "ВИДНА СЕРИЯ КРУПНЫХ ПОКУПОК",
                explanation = "test",
                fingerprint = "BUY test"
            )
        )

        val text = BigOverviewMoneyText.describe(snapshot)

        assertTrue(text.contains("КРУПНЫЙ НЕТТО-ПОТОК В ПОКУПКУ"))
        assertTrue(text.contains("Чистый крупный поток 5 мин"))
        assertTrue(text.contains("Весь taker-поток 60 сек"))
    }

    @Test
    fun `money overview formats negative and zero values without runtime exception`() {
        val negative = MicroImpulseSnapshot(
            buyNotional60s = 10_000.0,
            sellNotional60s = 90_000.0,
            largeFlow = LargeFlowFingerprint(
                thresholdUsdt = 15_000.0,
                largeBuyUsdt = 5_000.0,
                largeSellUsdt = 75_000.0
            )
        )
        val zero = MicroImpulseSnapshot()

        assertTrue(BigOverviewMoneyText.describe(negative).contains("КРУПНЫЙ НЕТТО-ПОТОК В ПРОДАЖУ"))
        assertTrue(BigOverviewMoneyText.describe(zero).contains("СБАЛАНСИРОВАН"))
    }
}
