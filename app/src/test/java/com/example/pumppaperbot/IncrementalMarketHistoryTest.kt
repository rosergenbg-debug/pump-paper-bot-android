package com.example.pumppaperbot

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncrementalMarketHistoryTest {
    @Test
    fun doesNotRequestHistoryBeforeNextThirtyMinuteCandleCanClose() {
        val lastClose = 1_000_000L

        assertFalse(
            IncrementalMarketHistory.needsKlineRefresh(
                lastClose,
                lastClose + 30L * 60L * 1000L
            )
        )
        assertTrue(
            IncrementalMarketHistory.needsKlineRefresh(
                lastClose,
                lastClose + 30L * 60L * 1000L + 1L
            )
        )
    }

    @Test
    fun requestsInitialHistoryWhenCacheIsEmpty() {
        assertTrue(IncrementalMarketHistory.needsKlineRefresh(null, 2_000_000L))
        assertNull(IncrementalMarketHistory.lastClosedKlineTime("", 2_000_000L))
    }

    @Test
    fun appendsOnlyNewClosedCandlesAndKeepsConfiguredLimit() {
        val existing = JSONArray()
            .put(kline(closeTime = 100L, close = "1.00"))
            .put(kline(closeTime = 200L, close = "2.00"))
            .put(kline(closeTime = 300L, close = "3.00"))
            .toString()
        val fresh = JSONArray()
            .put(kline(closeTime = 300L, close = "3.25"))
            .put(kline(closeTime = 400L, close = "4.00"))
            .put(kline(closeTime = 600L, close = "6.00"))
            .toString()

        val merged = JSONArray(IncrementalMarketHistory.mergeKlines(existing, fresh, 3, now = 500L))

        assertEquals(3, merged.length())
        assertEquals(listOf(200L, 300L, 400L), (0 until merged.length()).map { merged.getJSONArray(it).getLong(6) })
        assertEquals("3.25", merged.getJSONArray(1).getString(4))
    }

    @Test
    fun ignoresStillOpenCandle() {
        val json = JSONArray()
            .put(kline(closeTime = 100L, close = "1.00"))
            .put(kline(closeTime = 600L, close = "6.00"))
            .toString()

        assertEquals(100L, IncrementalMarketHistory.lastClosedKlineTime(json, now = 500L))
    }

    @Test
    fun mergesFundingWithoutDuplicatingOldPoints() {
        val existing = JSONArray()
            .put(funding(time = 100L, rate = "0.0001"))
            .put(funding(time = 200L, rate = "0.0002"))
            .toString()
        val fresh = JSONArray()
            .put(funding(time = 200L, rate = "0.0003"))
            .put(funding(time = 300L, rate = "0.0004"))
            .toString()

        val merged = JSONArray(IncrementalMarketHistory.mergeFunding(existing, fresh, 2, now = 400L))

        assertEquals(2, merged.length())
        assertEquals(200L, merged.getJSONObject(0).getLong("fundingTime"))
        assertEquals("0.0003", merged.getJSONObject(0).getString("fundingRate"))
        assertEquals(300L, IncrementalMarketHistory.lastFundingTime(merged.toString()))
    }

    private fun kline(closeTime: Long, close: String): JSONArray {
        return JSONArray()
            .put(closeTime - 99L)
            .put(close)
            .put(close)
            .put(close)
            .put(close)
            .put("100")
            .put(closeTime)
            .put("100")
            .put(10)
            .put("50")
            .put("0")
            .put("0")
    }

    private fun funding(time: Long, rate: String): JSONObject {
        return JSONObject()
            .put("fundingTime", time)
            .put("fundingRate", rate)
    }
}
